package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;

@Data
public class GroupVo {

    private String name;
    private List<AttrValueVo> attrs;
}