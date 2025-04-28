package com.olink.biz;

import com.olink.common.annotation.Autowired;
import com.olink.common.annotation.Component;
import com.olink.common.spring.OrderService;
import com.olink.common.spring.UserService;
import lombok.Data;

/*
*功能：
 作者：chenhao
*日期： 2025/4/26 下午1:31
*/
@Data
@Component("userService")
public class UserServiceImpl implements UserService {
    @Autowired
    private OrderService orderService;
    public void test(){
        System.out.println("hello Spring!");
    }


}
