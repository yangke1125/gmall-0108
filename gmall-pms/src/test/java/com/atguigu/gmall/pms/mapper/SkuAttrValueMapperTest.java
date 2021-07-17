package com.atguigu.gmall.pms.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SkuAttrValueMapperTest {

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Test
    void queryMappingBySpuId() {
        System.out.println(this.skuAttrValueMapper.queryMappingBySpuId(Arrays.asList(11l, 12l, 13l, 14l)));
    }
}