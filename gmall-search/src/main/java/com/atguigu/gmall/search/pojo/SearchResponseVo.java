package com.atguigu.gmall.search.pojo;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponseVo {

    private List<BrandEntity> brands;

    private List<CategoryEntity> categories;

    private List<SearchResponseAttrValueVo> filters;

    //分页参数
    private Long total;
    private Integer pageNum;
    private Integer pageSize;

    private List<Goods> goodsList;
}
