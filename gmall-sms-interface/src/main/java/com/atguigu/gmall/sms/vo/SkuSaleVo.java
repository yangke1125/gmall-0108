package com.atguigu.gmall.sms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuSaleVo {
    private long skuId;
    //积分
    private BigDecimal growBounds;
    private BigDecimal buyBounds;
    private List<Integer> work;
    //打折
    private Integer fullCount;
    private BigDecimal discount;
    private Integer ladderAddOther;
    //满减
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private Integer fullAddOther;
}
