package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

/**
 * 接受页面传递过来的检索参数
 * search?keyword=小米&brandId=1,3&cid=225&props=5:高通-麒麟&props=6:骁龙865-硅谷1000&sort=1&priceFrom=1000&priceTo=6000&pageNum=1&store=true
 */
@Data
public class SearchParamVo {

    private String keyword;

    private List<Long> brandId;

    private List<Long> categoryId;

    private List<String> props;

    // 价格区间
    private Double priceFrom;
    private Double priceTo;

    private Boolean store; // 是否有货

    private Integer sort = 0;// 排序字段：0-默认，得分降序；1-按价格降序；2-按价格升序；3-销量；4-新品

    private Integer pageNum =1; // 页码
    private final Integer pageSize = 20; // 每页记录数


}
