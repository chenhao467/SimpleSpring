package com.olink.common.spring;

import com.olink.common.annotation.Component;
import com.olink.entity.User;

public interface UserService{
    public void test();

    String getUserNameById(String id);

    String AddUserName(User user);
}
