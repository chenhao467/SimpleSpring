package com.olink.biz;

import com.olink.common.annotation.Component;
import com.olink.common.spring.BeanPostProcessor;

/*
*功能：
 作者：chenhao
*日期： 2025/4/27 下午5:35
*/
@Component("myBeanPostProcessor")
public class MyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object before(Object bean, String beanName) {
        System.out.println("初始化前");
        return bean;
    }

    @Override
    public Object after(Object bean, String beanName) {
        System.out.println("初始化后");
        return bean;
    }
}
