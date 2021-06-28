package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.SpuVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.SpuEntity;

import java.util.Map;

/**
 * spu信息
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-06-22 15:15:40
 */
public interface SpuService extends IService<SpuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    PageResultVo queryCategoryById(PageParamVo pageParamVo, long categoryId);

    void bigSave(SpuVo spu);

    void saveSku(SpuVo spu, Long spuId);

    void saveBaseAttr(SpuVo spu, Long spuId);

    void savaSpuDesc(SpuVo spu, Long spuId);

    Long savaSpu(SpuVo spu);
}

