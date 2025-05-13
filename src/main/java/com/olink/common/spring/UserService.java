package com.olink.common.spring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.olink.entity.User;

public interface UserService{
    public void test();

    String getUserNameById(String id);

    String AddUserName(User user);
}
