package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrValueVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {

    //http://localhost:18086/search?keyword=%E6%89%8B%E6%9C%BA&brandId=1,2&categoryId=225,250&priceFrom=1000&priceTo=5000&store=false&props=4:8G-12G&props=5:128G-256G&pageNum=2
//测试

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public SearchResponseVo search(SearchParamVo paramVo) {

        try {
            SearchResponse response = this.restHighLevelClient.search(new SearchRequest(new String[]{"goods"}, builder(paramVo)), RequestOptions.DEFAULT);

            System.out.println(response);
            SearchResponseVo responseVo = this.parseResult(response);
            responseVo.setPageNum(paramVo.getPageNum());
            responseVo.setPageSize(paramVo.getPageSize());
            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    private SearchResponseVo parseResult(SearchResponse response) {
        SearchResponseVo responseVo = new SearchResponseVo();

        SearchHits hits = response.getHits();
        responseVo.setTotal(hits.getTotalHits());

        SearchHit[] hitsHits = hits.getHits();
        List<Goods> goodsList = Arrays.stream(hitsHits).map(hitsHit -> {
            String json = hitsHit.getSourceAsString();
            Goods goods = JSON.parseObject(json, Goods.class);
            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("title");
            goods.setTitle(highlightField.getFragments()[0].string());
            return goods;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);

        //聚合
        Aggregations aggregations = response.getAggregations();
        //
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregations.get("brandIdAgg");
        List<? extends Terms.Bucket> brandBuckets = brandIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(brandBuckets)) {
            List<BrandEntity> brandEntities = brandBuckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                brandEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());

                Aggregations subBrandAgg = ((Terms.Bucket) bucket).getAggregations();
                ParsedStringTerms brandNameAgg = (ParsedStringTerms) subBrandAgg.get("brandNameAgg");
                List<? extends Terms.Bucket> buckets = brandNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(buckets)) {
                    brandEntity.setName(buckets.get(0).getKeyAsString());
                }

                Aggregations subLogoAgg = ((Terms.Bucket) bucket).getAggregations();
                ParsedStringTerms logoAgg = (ParsedStringTerms) subLogoAgg.get("logoAgg");
                List<? extends Terms.Bucket> logoBucket = logoAgg.getBuckets();
                if (!CollectionUtils.isEmpty(logoBucket)) {
                    brandEntity.setLogo(logoBucket.get(0).getKeyAsString());
                }
                return brandEntity;
            }).collect(Collectors.toList());
            responseVo.setBrands(brandEntities);
        }
        //
        ParsedLongTerms categoryAgg = (ParsedLongTerms) aggregations.get("categoryAgg");
        List<? extends Terms.Bucket> cateBuckets = categoryAgg.getBuckets();
        if (!CollectionUtils.isEmpty(cateBuckets)) {
            List<CategoryEntity> categoryEntities = cateBuckets.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                categoryEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                ParsedLongTerms categoryNameAgg = (ParsedLongTerms) ((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = categoryNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameAggBuckets)) {
                    categoryEntity.setName(nameAggBuckets.get(0).getKeyAsString());
                }
                return categoryEntity;
            }).collect(Collectors.toList());
            responseVo.setCategories(categoryEntities);
        }

        //
        ParsedNested attrAgg = (ParsedNested) aggregations.get("attrAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdBucketAgg = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(attrIdBucketAgg)) {
            List<SearchResponseAttrValueVo> attrValueVos = attrIdBucketAgg.stream().map(bucket -> {
                SearchResponseAttrValueVo attrValue = new SearchResponseAttrValueVo();
                long valueId = ((Terms.Bucket) bucket).getKeyAsNumber().longValue();
                attrValue.setAttrId(valueId);

                ParsedStringTerms attrNameAgg = (ParsedStringTerms) bucket.getAggregations().get("attrNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameAggBuckets)) {
                    attrValue.setAttrName(nameAggBuckets.get(0).getKeyAsString());
                }

                ParsedStringTerms attrValueAgg = (ParsedStringTerms) bucket.getAggregations().get("attrValueAgg");
                List<? extends Terms.Bucket> buckets = attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(buckets)) {
                    List<String> attrValues = buckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                    attrValue.setAttrValues(attrValues);
                }
                return attrValue;
            }).collect(Collectors.toList());
            responseVo.setFilters(attrValueVos);
        }


        return responseVo;
    }

    /**
     * 构建查询DSL语句
     *
     * @return
     */
    private SearchSourceBuilder builder(SearchParamVo paramVo) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        String keyword = paramVo.getKeyword();
        if (StringUtils.isBlank(keyword)) {
            //打广告
            return sourceBuilder;
        }
        // 1. 构建查询条件（bool查询）
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);
        // 1.1. 匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));

        // 1.2. 过滤
        // 1.2.1. 品牌过滤
        List<Long> brandId = paramVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandId));
        }
        // 1.2.2. 分类过滤
        List<Long> categoryId = paramVo.getCategoryId();
        if (!CollectionUtils.isEmpty(categoryId)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", categoryId));
        }
        // 1.2.3. 价格区间过滤
        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        if (priceFrom != null || priceTo != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            boolQueryBuilder.filter(rangeQuery);
            if (priceFrom != null) {
                rangeQuery.gte(priceFrom);
            }
            if (priceTo != null) {
                rangeQuery.lte(priceTo);
            }
        }

        // 1.2.4. 是否有货

        Boolean store = paramVo.getStore();
        if (store != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
        }

        // 1.2.5. 规格参数的过滤 props=5:高通-麒麟&props=6:骁龙865-硅谷1000

        List<String> props = paramVo.getProps();
        if (!CollectionUtils.isEmpty(props)) {
            props.forEach(prop -> {
                //每一个规格参数过滤的嵌套查询
                String[] attr = StringUtils.split(prop, ":");
                if (attr != null && attr.length == 2) {
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None));
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attr[0]));
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", StringUtils.split(attr[1], "-")));

                }
            });
        }
        // 2. 构建排序 0-默认，得分降序；1-按价格升序；2-按价格降序；3-按创建时间降序；4-按销量降序

        Integer sort = paramVo.getSort();
        switch (sort) {
            case 0:
                sourceBuilder.sort("_score", SortOrder.DESC);
                break;
            case 1:
                sourceBuilder.sort("price", SortOrder.DESC);
                break;
            case 2:
                sourceBuilder.sort("price", SortOrder.ASC);
                break;
            case 3:
                sourceBuilder.sort("sales", SortOrder.DESC);
                break;
            case 4:
                sourceBuilder.sort("createTime", SortOrder.DESC);
                break;
            default:
                throw new RuntimeException("你的搜索条件不合法");
        }
        // 3. 构建分页

        Integer pageNum = paramVo.getPageNum();
        Integer pageSize = paramVo.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);

        // 4. 构建高亮
        sourceBuilder.highlighter(
                new HighlightBuilder()
                        .field("title")
                        .preTags("<font style='color:red;'>")
                        .postTags("</font>")
        );
        // 5. 构建聚合

        // 5.1. 构建品牌聚合
        sourceBuilder.aggregation(
                AggregationBuilders.terms("brandIdAgg").field("brandId")
                        .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                        .subAggregation(AggregationBuilders.terms("logoAgg").field("logo"))
        );

        // 5.2. 构建分类聚合
        sourceBuilder.aggregation(
                AggregationBuilders.terms("categoryAgg").field("categoryId")
                        .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryId"))
        );
        // 5.3. 构建规格参数的嵌套聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue")))
        );

        System.out.println(sourceBuilder);
        return sourceBuilder;
    }

}
