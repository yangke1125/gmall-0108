package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.*;

import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;

import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescMapper spuDescMapper;
    @Autowired
    private SpuAttrValueService spuAttrValueService;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private GmallSmsClient gmallSmsClient;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo queryCategoryById(PageParamVo pageParamVo, long categoryId) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        if (categoryId != 0) {
            wrapper.eq("category_id", categoryId);
        }
        String key = pageParamVo.getKey();
        if (StringUtils.isNotBlank(key)) {
            wrapper.and(t -> t.like("name", key).or().like("id", key));
        }
        IPage<SpuEntity> page = this.page(
                pageParamVo.getPage(),
                wrapper
        );
        return new PageResultVo(page);

    }

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @GlobalTransactional
    @Override
    public void bigSave(SpuVo spu) {

        Long spuId = savaSpu(spu);

        savaSpuDesc(spu, spuId);

        saveBaseAttr(spu, spuId);
        //2 Sku
        saveSku(spu, spuId);
        // int i =1/0;
        this.rabbitTemplate.convertAndSend("PMS_ITEM_EXCHANGE","item.insert",spuId);
    }

    public void saveSku(SpuVo spu, Long spuId) {
        List<SkuVo> skus = spu.getSkus();
        if (CollectionUtils.isEmpty(skus)) {
            return;
        }
        skus.forEach(skuVo -> {
            skuVo.setSpuId(spuId);
            skuVo.setCategoryId(spu.getCategoryId());
            skuVo.setBrandId(spu.getBrandId());
            List<String> images = skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)) {
                skuVo.setDefaultImage(StringUtils.isBlank(skuVo.getDefaultImage()) ? images.get(0) : skuVo.getDefaultImage());
            }
            this.skuMapper.insert(skuVo);

            Long skuId = skuVo.getId();
            if (!CollectionUtils.isEmpty(images)) {
                this.skuImagesService.saveBatch(images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(skuVo.getDefaultImage(), image) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)) {
                saleAttrs.forEach(skuAttrValueEntity -> {
                    skuAttrValueEntity.setSkuId(skuId);
                });
                this.skuAttrValueService.saveBatch(saleAttrs);
            }
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo, skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.gmallSmsClient.saleSales(skuSaleVo);
        });
    }

    public void saveBaseAttr(SpuVo spu, Long spuId) {
        List<SpuAttrValueVo> baseAttrs = spu.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrs.stream().filter(spuAttrValueVo -> spuAttrValueVo.getAttrValue() != null
            ).map(spuAttrValueVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(spuAttrValueVo, spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                return spuAttrValueEntity;
            }).collect(Collectors.toList());
            this.spuAttrValueService.saveBatch(spuAttrValueEntities);
        }
    }

    public void savaSpuDesc(SpuVo spu, Long spuId) {
        List<String> spuImages = spu.getSpuImages();
        if (!CollectionUtils.isEmpty(spuImages)) {
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            spuDescEntity.setSpuId(spuId);
            spuDescEntity.setDecript(StringUtils.join(spuImages, ","));
            this.spuDescMapper.insert(spuDescEntity);
        }
    }

    public Long savaSpu(SpuVo spu) {
        spu.setCreateTime(new Date());
        spu.setUpdateTime(spu.getCreateTime());
        this.save(spu);
        return spu.getId();
    }

//    public static void main(String[] args) {
//        List<User> users = Arrays.asList(
//                new User("liuyan1",20,false),
//                new User("liuyan2",21,false),
//                new User("liuyan3",22,true),
//                new User("liuyan4",23,false),
//                new User("liuyan5",24,false),
//                new User("liuyan6",25,true)
//        );
////        users.stream().map(user -> user.getName()).collect(Collectors.toList()).forEach(System.out::println);
////        users.stream().map(user -> {
////            Person person = new Person();
////            person.setAge(user.getAge());
////            person.setName(user.getName());
////            return person;
////        }).collect(Collectors.toList()).forEach(System.out::println);
//        //users.stream().filter(user -> user.getAge()>20).collect(Collectors.toList()).forEach(System.out::println);
//        users.stream().filter(User::getSex).collect(Collectors.toList()).forEach(System.out::println);
//        List<Integer> list = Arrays.asList(10,20,30,40);
//        System.out.println(list.stream().reduce((a, b) -> a + b).get());
//        System.out.println(users.stream().map(User::getAge).reduce((a, b) -> a + b).get());
//    }
}
//@Data
//@AllArgsConstructor
//@NoArgsConstructor
//class User{
//    private String name;
//    private Integer age;
//    private Boolean sex;
//}
//@Data
//@ToString
//class Person{
//    private String name;
//    private Integer age;
//}