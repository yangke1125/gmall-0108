package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.api.GmallWmsApi;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private GmallWmsApi gmallWmsApi;
    @Test
    void contextLoads() {
        if (!this.restTemplate.indexExists(Goods.class)){
            this.restTemplate.createIndex(Goods.class);
            this.restTemplate.putMapping(Goods.class);
        }
        Integer pageNum =1;
        Integer pageSize =100;
        do {
            PageParamVo pageParamVo = new PageParamVo(pageNum,pageSize,null);
            ResponseVo<List<SpuEntity>> responseVo = this.pmsClient.querySpuByPageJson(pageParamVo);
            List<SpuEntity> spuEntities = responseVo.getData();
            if (CollectionUtils.isEmpty(spuEntities)){
                return;
            }

            //遍历spu查看spu下的所有sku
            spuEntities.forEach(spuEntity -> {
                ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkuBySpuId(spuEntity.getId());
                List<SkuEntity> skuEntities = skuResponseVo.getData();
                if (!CollectionUtils.isEmpty(skuEntities)){

                    ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(spuEntity.getBrandId());
                    BrandEntity brandEntity = brandEntityResponseVo.getData();
                    ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(spuEntity.getCategoryId());
                    CategoryEntity categoryEntity = categoryEntityResponseVo.getData();


                    List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                        Goods goods = new Goods();
                        //sku信息
                        goods.setSkuId(skuEntity.getId());
                        goods.setDefaultImage(skuEntity.getDefaultImage());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setSubTitle(skuEntity.getSubtitle());
                        goods.setPrice(skuEntity.getPrice().doubleValue());

                        //创建时间
                        goods.setCreateTime(spuEntity.getCreateTime());

                        //销量库存
                        ResponseVo<List<WareSkuEntity>> wareResponseVo = this.gmallWmsApi.queryWareSkuByskuId(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
                        if (!CollectionUtils.isEmpty(wareSkuEntities)){
                            goods.setSales(wareSkuEntities.stream().mapToLong(WareSkuEntity::getSales).reduce((a,b)->a+b).getAsLong());
                            goods.setStock(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()-wareSkuEntity.getStockLocked() >0));
                        }

                        //品牌
                        if (brandEntity!=null){
                            goods.setBrandId(brandEntity.getId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }
                        //分类
                        if (categoryEntity!=null){
                            goods.setCategoryId(categoryEntity.getId());
                            goods.setCategoryName(categoryEntity.getName());
                        }
                        //检索
                        List<SearchAttrValueVo> attrValueVos = new ArrayList<>();

                        ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.pmsClient.querySearchAttrValuesBySkuId(skuEntity.getCategoryId(), skuEntity.getId());
                        List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
                        if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                            attrValueVos.addAll( skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                BeanUtils.copyProperties(skuAttrValueEntity,searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList()));
                        }
                        ResponseVo<List<SpuAttrValueEntity>> baseAttrResponseVo = this.pmsClient.querySearchAttrValuesBySpuId(skuEntity.getCategoryId(), spuEntity.getId());
                        List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrResponseVo.getData();
                        if (!CollectionUtils.isEmpty(spuAttrValueEntities)){
                            attrValueVos.addAll( spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                BeanUtils.copyProperties(spuAttrValueEntity,searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList()));
                        }

                        goods.setSearchAttrs(attrValueVos);

                        return goods;
                    }).collect(Collectors.toList());

                    this.goodsRepository.saveAll(goodsList);

                }
            });

            pageSize =spuEntities.size();
            pageNum++;
        }while (pageSize==100);

    }

}
