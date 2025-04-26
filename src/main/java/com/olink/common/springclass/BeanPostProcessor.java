package com.olink.common.springclass;

import com.olink.common.annotation.Component;

/*
*功能：
 作者：chenhao
*日期： 2025/4/26 下午5:20
*/
@Component("beanPostProcessor")
public class BeanPostProcessor{
    public Object before(Object bean,String beanName){
        System.out.println("初始化前");
        return bean;
    }
    public Object after(Object bean,String beanName){
        System.out.println("初始化后");
        return bean;
    }
}
