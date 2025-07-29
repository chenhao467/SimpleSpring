package com.chenhao.bean;

/*
*功能：
 作者：chenhao
*日期： 2025/7/28 下午9:47
*/

import com.chenhao.common.annotation.Component;
import com.chenhao.common.annotation.aop.Aspect;
import com.chenhao.common.annotation.aop.Before;

@Aspect
@Component
public class AspectUser {


    @Before("OrderController/getUserNameById")
    public void before(){
        System.out.println("before");
    }

}
