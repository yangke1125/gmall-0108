package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.GroupVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-06-22 15:15:40
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<AttrGroupEntity> queryArrtGroupByCatId(long catId);

    List<GroupVo> queryGroupsWithAttrValuesByCidAndSpuIdAndSkuId(Long cid, Long spuId, Long skuId);
}

