package com.olink.bean;

/*
*功能：
 作者：chenhao
*日期： 2025/7/28 下午9:47
*/

import com.olink.common.annotation.Component;
import com.olink.common.annotation.aop.Aspect;
import com.olink.common.annotation.aop.Before;

@Aspect
@Component
public class AspectUser {


    @Before("OrderServiceImpl/getUserNameById")
    public void before(){
        System.out.println("before");
    }

}
