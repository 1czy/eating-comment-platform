package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.entity.User;
import com.hmdp.service.IUserService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
public class testApplicationTest {
    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private IUserService userService;
    @Resource
    private CacheClient cacheClient;
    @Autowired
    private RedisIdWorker idWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private ExecutorService es = Executors.newFixedThreadPool(500);
    @Resource
    private RedissonClient redissonClient;
    @Test
    void testRedissonClient() throws InterruptedException {
        RLock lock = redissonClient.getLock("anyLock");
        boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
        if(isLock){
           try{
               System.out.println("执行业务成功");
           }finally {
               lock.unlock();
           }
        }

    }
    @Test
    public void testSaveShop() throws InterruptedException {
        for(int i = 0; i <10 ; i++){
            Long l = Long.valueOf(i+1);
            Shop shop = shopService.getById(l);
            cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY +l,shop,10L, TimeUnit.SECONDS);
        }

    }
    @Test
    void testRedisIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable tast = () -> {
            for(int i = 0; i < 100; i++){
                long id = idWorker.nextId("order");
                System.out.println("id=" + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++){
            es.submit(tast);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time=" + (end-begin));
    }
    @Test
    void testuse(){
        User byId = userService.getById(10L);
        System.out.println(byId);

    }

    @Test
    void loadShopData() {
        // 1.查询店铺信息
        List<Shop> list = shopService.list();
        // 2.把店铺分组，按照typeId分组，typeId一致的放到一个集合
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        // 3.分批完成写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // 3.1.获取类型id
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            // 3.2.获取同类型的店铺的集合
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            // 3.3.写入redis GEOADD key 经度 纬度 member
            for (Shop shop : value) {
                // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }

    /**
     *  uv统计
     */

    @Test
    void testHyperLogLog(){
        String[] users = new String[1000];
        int index = 0;
        for (int i = 1; i <= 1000000; i++){
            users[index++] = "user_" + i;
            if(i % 1000 == 0){
                index = 0;
                stringRedisTemplate.opsForHyperLogLog().add("hll1",users);
            }
        }
        Long size = stringRedisTemplate.opsForHyperLogLog().size("hll1");
        System.out.println("size = " + size);
    }

}