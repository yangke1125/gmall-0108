package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private CartAsyncService asyncService;

    private static  final String KEY_PREFIX = "cart:info:";

    public void addCart(Cart cart) {
        String userId = getUserId();
        //用户id获取内存map
        BoundHashOperations<String, Object, Object> boundHashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();//新增数量
        if (boundHashOps.hasKey(skuId)){
            //更新数量
            String cartJson = boundHashOps.get(skuId).toString();
            cart  = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(cart.getCount().add(count));
            //写入数据库
            this.asyncService.updateCart(cart,userId,skuId);
        }else {
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity==null){
                throw new CartException("您加入的商品不存在");
            }
            cart.setDefaultImage(skuEntity.getDefaultImage());
            cart.setPrice(skuEntity.getPrice());
            cart.setTitle(skuEntity.getTitle());
            cart.setUserId(userId);
            ResponseVo<List<ItemSaleVo>> listResponseVo = this.smsClient.queryItemSalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = listResponseVo.getData();
            cart.setSales(JSON.toJSONString(itemSaleVos));
            ResponseVo<List<SkuAttrValueEntity>> listResponseVo1 = this.pmsClient.querySkuAttrValuesBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = listResponseVo1.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));
            //库存
            ResponseVo<List<WareSkuEntity>> listResponseVo2 = this.wmsClient.queryWareSkuByskuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = listResponseVo2.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()-wareSkuEntity.getStockLocked()>0));
            }
            cart.setCheck(true);
            //写入数据库
            this.asyncService.insertCart(cart);
        }
        boundHashOps.put(skuId,JSON.toJSONString(cart));
    }


    private String getUserId() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = userInfo.getUserKey();
        if (userInfo.getUserId()!=null){
            userId = userInfo.getUserId().toString();
        }
        return userId;
    }

    public Cart queryCartBySkuId(Cart cart) {
        //获取登录信息
        String userId = this.getUserId();

        BoundHashOperations<String, Object, Object> hashOperations = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if (!hashOperations.hasKey(cart.getSkuId().toString())){
            throw new CartException("当前用户没有对应的购物记录");
        }
        String cartJson = hashOperations.get(cart.getSkuId().toString()).toString();
        return JSON.parseObject(cartJson,Cart.class);
    }

    @Async
    public void exceutor1(){
        try {
            System.out.println("exceutor1开始执行");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("exceutor1执行结束");
        } catch (InterruptedException e) {
            //return AsyncResult.forExecutionException(e);
            e.printStackTrace();
        }
        //return AsyncResult.forValue("111");
    }

    @Async
    public void exceutor2(){
        try {
            System.out.println("exceutor2开始执行");
            TimeUnit.SECONDS.sleep(5);
            //int i =1/0;
            System.out.println("exceutor2执行结束");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //return AsyncResult.forValue("2222");
    }


    public List<Cart> queryCarts() {
        //获取userKey
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();

        BoundHashOperations<String, Object, Object> unloginHashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userKey);
        List<Object> unloginCartJsons = unloginHashOps.values();
        List<Cart> unloginCarts = null;
        if (!CollectionUtils.isEmpty(unloginCartJsons)){
            unloginCarts = unloginCartJsons.stream().map(cartJson -> JSON.parseObject(cartJson.toString(),Cart.class)).collect(Collectors.toList());
        }
        //3获取userId
        Long userId = userInfo.getUserId();
        if (userId==null){
            return unloginCarts;
        }
        //4合并未登录的购物车
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if (!CollectionUtils.isEmpty(unloginCarts)){
            unloginCarts.forEach(cart -> {
                String skuId = cart.getSkuId().toString();
                BigDecimal count = cart.getCount();
                if (loginHashOps.hasKey(skuId)){
                    String cartJson = loginHashOps.get(skuId).toString();
                    cart = JSON.parseObject(cartJson,Cart.class);
                    cart.setCount(cart.getCount().add(count));
                    //写入数据库
                    this.asyncService.updateCart(cart,userId.toString(),skuId);
                }else {
                    cart.setUserId(userId.toString());
                    this.asyncService.insertCart(cart);
                }
                loginHashOps.put(skuId,JSON.toJSONString(cart));
            });
            //删除未登录
            this.redisTemplate.delete(KEY_PREFIX+userKey);
            this.asyncService.deleteCartByUserId(userKey);
        }
        List<Object> loginCartJsons = loginHashOps.values();
        if (!CollectionUtils.isEmpty(loginCartJsons)){
            return loginCartJsons.stream().map(cartJson-> JSON.parseObject(cartJson.toString(),Cart.class)).collect(Collectors.toList());
        }
        return null;

    }

    public void updateNum(Cart cart) {
        String userId = this.getUserId();
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if (!hashOps.hasKey(cart.getSkuId().toString())){
            throw new CartException("您没有对应的购物车记录");
        }

        BigDecimal count = cart.getCount();
        String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
        cart = JSON.parseObject(cartJson, Cart.class);
        cart.setCount(count);
        hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        this.asyncService.updateCart(cart,userId,cart.getSkuId().toString());
    }


    public void deleteCart(Long skuId) {
        String userId = this.getUserId();
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        hashOps.delete(skuId.toString());
        this.asyncService.deleteCartByUserIdAndSkuId(userId,skuId);
    }
}
