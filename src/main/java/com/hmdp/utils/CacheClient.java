package com.hmdp.utils;


import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * 缓存工具类
 */
@Slf4j
@Component
public class CacheClient {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 缓存穿透方法
     * @param keyPrefix
     * @param id
     * @param type
     * @param function
     * @param time
     * @param unit
     * @return R
     * @param <R>
     * @param <ID>
     */
    public<R,ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID,R> function,Long time,TimeUnit unit){
        String key = keyPrefix + id;
        //1.查询redis缓存是否有店铺详情
        //存在，返回；
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(shopJson)) {
            return  JSONUtil.toBean(shopJson,type);
        }
        //判断缓存是否命中
        if(shopJson != null){
            return null;
        }
           R r = function.apply(id);
            //查询数据库是否存在；
            if(r == null){
                //不存在，将null写入redis
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //存在，写入缓存；
            this.set(key,r,time,unit);
        //返回
        return r;
    }

    /**
     * 缓存穿透方法，永久过期
     * @param keyPrefix
     * @param id
     * @param type
     * @param function
     * @param time
     * @param unit
     * @return
     * @param <R>
     * @param <ID>
     */
    public<R,ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID,R> function,Long time,TimeUnit unit){
        String key = keyPrefix + id;
        //1.查询redis缓存是否有店铺详情
        //存在，返回；
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(json)) {
            return  JSONUtil.toBean(json,type);
        }
        //判断缓存是否命中
        if(json != null){
            return null;
        }
        //不存在，获取缓存重构
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        R r = null;
        try {
            boolean isLock = tryLock(lockKey);
            if(!isLock) {
                //失败，休眠重试；
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, function,time,unit);
            }
            //成功 连接数据库
             r = function.apply(id);
            //模拟缓存重建延时
            Thread.sleep(200);
            //查询数据库是否存在；
            if(r == null){
                //不存在，将null写入redis
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //存在，写入缓存；
           this.set(key,r,time,unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            unLock(lockKey);
        }
        //返回
        return r;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    public<R,ID> R queryWithLogicalExpire(String keyPrefix, ID id,Class<R> type,Function<ID,R> dbFallback,Long time,TimeUnit unit){
        String key = keyPrefix + id;
        //1.查询redis缓存是否有店铺详情
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isBlank(json)) {
            //返回；
            return  null;
        }
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())){
            //未过期，返回
            return r;
        }
        //判断缓存是否命中
        //不存在，获取缓存重构
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if(isLock) {
            //失败，休眠重试；
            CACHE_REBUILD_EXECUTOR.submit(() ->{
                try {  //实际中过期时间设置为30分钟
                    //先查数据库
                    R r1 = dbFallback.apply(id);
                    //重建缓存
                    this.setWithLogicalExpire(key,r1,time,unit);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    unLock(lockKey);
                }
            });
        }
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(r),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //返回
        return r;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "10", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

}
