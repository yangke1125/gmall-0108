package com.atguigu.gmall.index.utils;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class DistributedLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Timer timer;

    public Boolean lock(String lockName,String uuid,Integer expire){
        String script ="if(redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], ARGV[1]) == 1) " +
                "then " +
                    "redis.call('hincrby', KEYS[1], ARGV[1], 1) " +
                    "redis.call('expire', KEYS[1], ARGV[2]) return 1 " +
                "else " +
                    "return 0 " +
                "end";
        Boolean flag = redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expire.toString());
        if (!flag){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock(lockName,uuid,expire);
        }else {
            this.renewlock(lockName,uuid,expire);
        }
        return true;
    }

    public void unlock(String lockName,String uuid){
        String script ="if(redis.call('hexists', KEYS[1], ARGV[1]) == 0) " +
                "then " +
                    "return nil elseif(redis.call('hincrby', KEYS[1], ARGV[1], -1) == 0) " +
                "then " +
                    "return redis.call('del', KEYS[1]) " +
                "else " +
                    "return 0 " +
                "end";
        Long flag = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuid);
        if (flag==null){
            throw new IllegalMonitorStateException("你再尝试释放别人的锁");
        }else if (flag==1){
            this.timer.cancel();
        }
    }

    public void renewlock(String lockName,String uuid,Integer expire){
        String script="if(redis.call('hexists', KEYS[1], ARGV[1]) == 1) then redis.call('expire', KEYS[1], ARGV[2]) end";
        this.timer =new Timer();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expire.toString());
            }
        },expire*1000/3,expire*1000/3);

    }

    public static void main(String[] args) {
        BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), 20, 0.3);
        System.out.println(bloomFilter.put("1"));
        System.out.println(bloomFilter.put("2"));
        System.out.println(bloomFilter.put("3"));
        System.out.println(bloomFilter.put("4"));
        System.out.println(bloomFilter.put("5"));
        System.out.println(bloomFilter.put("6"));
        System.out.println(bloomFilter.put("7"));
        System.out.println(bloomFilter.put("8"));
        System.out.println("----------------");
        System.out.println(bloomFilter.mightContain("1"));
        System.out.println(bloomFilter.mightContain("2"));
        System.out.println(bloomFilter.mightContain("3"));
        System.out.println(bloomFilter.mightContain("4"));
        System.out.println(bloomFilter.mightContain("5"));
        System.out.println(bloomFilter.mightContain("6"));
        System.out.println(bloomFilter.mightContain("7"));
        System.out.println(bloomFilter.mightContain("8"));
        System.out.println(bloomFilter.mightContain("9"));
        System.out.println(bloomFilter.mightContain("10"));
        System.out.println(bloomFilter.mightContain("11"));
        System.out.println(bloomFilter.mightContain("12"));
        System.out.println(bloomFilter.mightContain("13"));
        System.out.println(bloomFilter.mightContain("14"));
        System.out.println(bloomFilter.mightContain("15"));
    }


}
