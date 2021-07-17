package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.utils.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;

import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.security.Key;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public static final String KEY_PREFIX="index:cates:";
    public static final String LOCK_PREFIX="index:cates:lcok:";

    @Autowired
    private DistributedLock lock;

    @Autowired
    private RedissonClient redissonClient;


    public List<CategoryEntity> queryLvl1Categories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesByPid(0l);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        return categoryEntities;
    }

    @GmallCache(prefix = KEY_PREFIX,timeout = 259200,lock = LOCK_PREFIX)
    public List<CategoryEntity> queryLvl2WithSubsByPid(Long pid) {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryLvl2WithSubsByPid(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        return categoryEntities;
    }

    public List<CategoryEntity> queryLvl2WithSubsByPid2(Long pid) {
        //查询缓存,缓存不为空
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json)){
            return JSON.parseArray(json,CategoryEntity.class);
        }
        //缓存为空
        RLock fairLock = this.redissonClient.getFairLock(LOCK_PREFIX + pid);
        fairLock.lock();

        try {
            String json2 = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
            if (StringUtils.isNotBlank(json2)){
                return JSON.parseArray(json2,CategoryEntity.class);
            }
            ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryLvl2WithSubsByPid(pid);
            List<CategoryEntity> categoryEntities = listResponseVo.getData();
            //放入缓存
            if (CollectionUtils.isEmpty(categoryEntities)){
                this.redisTemplate.opsForValue().set(KEY_PREFIX+pid,JSON.toJSONString(categoryEntities),5, TimeUnit.MINUTES);
            }else {
                this.redisTemplate.opsForValue().set(KEY_PREFIX+pid,JSON.toJSONString(categoryEntities),180+new Random().nextInt(10), TimeUnit.DAYS);
            }
            return categoryEntities;
        } finally {
            fairLock.unlock();
        }

    }

    public  void testLock() {
        RLock lock = redissonClient.getLock("lock");
        lock.lock();
        try {
            String numString = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)){
                this.redisTemplate.opsForValue().set("num","1");
            }
            int num = Integer.parseInt(numString);
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));
            try {
                TimeUnit.SECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
         lock.unlock();
        }

    }
    public  void testLock3() {
        String uuid = UUID.randomUUID().toString();
        Boolean flag = this.lock.lock("lock", uuid, 30);
        if (flag){
            String numString = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)){
                this.redisTemplate.opsForValue().set("num","1");
            }
            int num = Integer.parseInt(numString);
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));
            try {
                TimeUnit.SECONDS.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //this.Sublcok("lock",uuid);
            lock.unlock("lock",uuid);
        }

    }
    public void Sublcok(String lockName,String uuid){
        lock.lock(lockName,uuid,30);
        System.out.println("测试可重入锁");
        lock.unlock(lockName,uuid);
    }


    public  void testLock2() {
        String uuid = UUID.randomUUID().toString();
        Boolean aBoolean = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid,3,TimeUnit.SECONDS);
        if (!aBoolean){
            try {
                Thread.sleep(100);
                testLock2();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
            String numString = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)){
                this.redisTemplate.opsForValue().set("num","1");
            }
            int num = Integer.parseInt(numString);
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));
            String script ="if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class), Arrays.asList("lock"),uuid);
//            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));
//            this.redisTemplate.delete("lock");
        }
    }

    public void testRead() {
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("rwlock");
        rwlock.readLock().lock(10,TimeUnit.SECONDS);
        System.out.println("测试读锁");
    }

    public void testWrite() {
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("rwlock");
        rwlock.writeLock().lock(10,TimeUnit.SECONDS);
        System.out.println("测试写锁");
    }

    public void testlatch() {
        try {
            RCountDownLatch wasd = redissonClient.getCountDownLatch("wasd");
            wasd.trySetCount(6);
            wasd.await();
            System.out.println("班长锁门");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void testCountDown() {
        RCountDownLatch wasd = redissonClient.getCountDownLatch("wasd");
        wasd.countDown();
        System.out.println("同学出来");
    }
}
