package com.atguigu.gmall.sms.service;

import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品spu积分设置
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-06-22 17:13:51
 */
public interface SkuBoundsService extends IService<SkuBoundsEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    void saleSales(SkuSaleVo saleVo);

    List<ItemSaleVo> queryItemSalesBySkuId(Long skuId);
}

