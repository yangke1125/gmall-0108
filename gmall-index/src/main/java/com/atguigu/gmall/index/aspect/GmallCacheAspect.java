package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.config.GmallCache;
import com.google.common.hash.BloomFilter;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class GmallCacheAspect {
    @Autowired
    private RBloomFilter filter;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
//
//    @Pointcut("execution(* com.atguigu.gmall.index.service.*.*(..))")
//    public void pointcut(){}
//
//
//    /**
//     *
//     */
//    @Before("pointcut()")
//    public void before(JoinPoint joinPoint){
//        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
//        System.out.println("这是前置方法........."+joinPoint.getTarget().getClass().getName());
//        System.out.println("目标方法："+signature.getMethod().getName());
//        System.out.println("目标方法的参数："+joinPoint.getArgs());
//    }
//    @AfterReturning(value = "pointcut()",returning = "result")
//    public void afterReturning(JoinPoint joinPoint,Object result){
//        System.out.println("这是一个后置通知：");
//        ((List<CategoryEntity>)result).forEach(System.out::println);
//
//    }
//
//    @AfterThrowing(value = "pointcut()",throwing = "ex")
//    public void afterThrowing(Exception ex){
//
//    }
    @Around("@annotation(com.atguigu.gmall.index.config.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        Class returnType = signature.getReturnType();
        String prefix = gmallCache.prefix();
        List<Object> objects = Arrays.asList(joinPoint.getArgs());
        String key =prefix+objects;
        //bloom过滤器
        if (!this.filter.contains(key)){
            return null;
        }
        String s = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(s)){
            return JSON.parseObject(s,returnType);
        }
        RLock fairLock = this.redissonClient.getFairLock(gmallCache.lock() + objects);
        fairLock.lock();

        try {
            String s1 = redisTemplate.opsForValue().get(key);
            if (StringUtils.isNotBlank(s1)){
                return JSON.parseObject(s1,returnType);
            }
            Object result = joinPoint.proceed(joinPoint.getArgs());
            if (result!=null){
                int timeOut = gmallCache.timeout()+new Random().nextInt(gmallCache.random());
                this.redisTemplate.opsForValue().set(key,JSON.toJSONString(result),timeOut, TimeUnit.MINUTES);
            }
            return result;
        } finally {
            fairLock.unlock();
        }

    }


}
