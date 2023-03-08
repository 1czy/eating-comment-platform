package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPE_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPE_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Override
    public List<ShopType> getAllShopType() {
        String key = CACHE_SHOPTYPE_KEY;
        String shopTypeJSON = redisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(shopTypeJSON)) {
            log.info(JSONUtil.toList(shopTypeJSON,ShopType.class).toString());
           return JSONUtil.toList(shopTypeJSON,ShopType.class);
        }
        List<ShopType> list = this.query().orderByAsc("sort").list();
        String jsonStr = JSONUtil.toJsonStr(list);
        redisTemplate.opsForValue().set(key,jsonStr,CACHE_SHOPTYPE_TTL, TimeUnit.MINUTES);
        log.info(jsonStr);
        return list;
    }
}
