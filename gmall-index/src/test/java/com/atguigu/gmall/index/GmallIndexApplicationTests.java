package com.atguigu.gmall.index;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class GmallIndexApplicationTests {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Test
    void contextLoads() {
//        this.redisTemplate.opsForValue().set("test1","liuyan");
//        System.out.println(redisTemplate.opsForValue().get("test1"));
        RBloomFilter<Object> bf = this.redissonClient.getBloomFilter("bf");
        bf.tryInit(20,0.3);
        bf.add("1");
        bf.add("2");
        bf.add("3");
        bf.add("4");
        bf.add("5");
        bf.add("6");
        bf.add("7");

        System.out.println(bf.contains("1"));
        System.out.println(bf.contains("2"));
        System.out.println(bf.contains("3"));
        System.out.println(bf.contains("4"));
        System.out.println(bf.contains("5"));
        System.out.println(bf.contains("6"));
        System.out.println(bf.contains("7"));
        System.out.println(bf.contains("8"));
        System.out.println(bf.contains("9"));
        System.out.println(bf.contains("10"));
        System.out.println(bf.contains("11"));
        System.out.println(bf.contains("12"));
        System.out.println(bf.contains("13"));
        System.out.println(bf.contains("14"));

    }

}
