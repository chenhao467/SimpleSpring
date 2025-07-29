package com.chenhao.common.annotation.service;

import com.chenhao.entity.User;

public interface UserService{
    public void test();

    String getUserNameById(String id);

    String AddUserName(User user);
}
