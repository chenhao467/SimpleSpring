package com.olink;

import com.olink.common.config.AppConfig;
import com.olink.common.context.MyApplicationContext;
import com.olink.biz.OrderServiceImpl;

import java.lang.reflect.InvocationTargetException;

/*
*功能：
 作者：chenhao
*日期： 2025/4/26 下午1:32
*/
public class Test {
    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        new MyApplicationContext(AppConfig.class);
//        OrderService orderService =(OrderService) context.getBean("orderService");
//        orderService.test();
//        UserService userService=new UserService();
//        userService.test();
    }
}
