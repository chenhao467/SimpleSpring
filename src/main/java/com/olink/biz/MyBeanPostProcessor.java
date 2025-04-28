package com.olink.biz;

import com.olink.common.annotation.Component;
import com.olink.common.spring.BeanPostProcessor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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
        Object proxyInstace = Proxy.newProxyInstance(MyBeanPostProcessor.class.getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                System.out.println("执行代理逻辑");
                return method.invoke(bean,args);
            }
        });
        return proxyInstace;
    }
}
