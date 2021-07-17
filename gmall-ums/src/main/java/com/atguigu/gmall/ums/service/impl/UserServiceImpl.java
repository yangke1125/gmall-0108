package com.atguigu.gmall.ums.service.impl;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;
import org.springframework.util.CollectionUtils;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean chekData(String data, Integer type) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        switch (type){
            case 1: wrapper.eq("username",data);break;
            case 2: wrapper.eq("phonr",data);break;
            case 3: wrapper.eq("email",data);break;
            default:
                return null;
        }
       return this.count(wrapper) == 0;
    }

    @Override
    public void register(UserEntity userEntity, String code) {

        String slat = StringUtils.substring(UUID.randomUUID().toString(), 0, 6);
        userEntity.setSalt(slat);
        //密码加密
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword()+slat));
        //新增用户
        userEntity.setLevelId(1l);
        userEntity.setNickname(userEntity.getUsername());
        userEntity.setSourceType(1);
        userEntity.setIntegration(1000);
        userEntity.setGrowth(1000);
        userEntity.setStatus(1);
        userEntity.setCreateTime(new Date());
        this.save(userEntity);
    }

    @Override
    public UserEntity queryUser(String loginName, String password) {
        List<UserEntity> userEntities = this.list(new QueryWrapper<UserEntity>().eq("username", loginName)
                .or().eq("phone", loginName)
                .or().eq("email", loginName));
        // 2.如果用户信息为空，直接返回null
        if (CollectionUtils.isEmpty(userEntities)){
            return null;
        }

        // 3.获取用户信息中的盐，对用户输入的明文密码加盐加密
        for (UserEntity userEntity : userEntities) {
            // 4.用户输入的明文密码加盐加密后 和 数据库中的密码 比较
            if (StringUtils.equals(DigestUtils.md5Hex(password + userEntity.getSalt()), userEntity.getPassword())) {
                return userEntity;
            }
        }

        return null;
    }

}