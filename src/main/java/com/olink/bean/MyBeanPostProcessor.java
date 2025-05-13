package com.olink.bean;

import com.olink.common.annotation.Component;
import com.olink.common.annotation.Transactional;
import com.olink.common.spring.BeanPostProcessor;
import com.olink.common.spring.TransactionalInvocationHandler;

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
        Class<?> clazz = bean.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Transactional.class)) {
                return Proxy.newProxyInstance(
                        bean.getClass().getClassLoader(),
                        bean.getClass().getInterfaces(),
                        new TransactionalInvocationHandler(bean)
                );
            }
        }

        /*
         * JDK 动态代理：只能代理实现了接口的类。
         * 容器初始化时，会将所有bean用代理对象替换。
         * 当调用代理对象的某个方法时，实际的执行逻辑会被 InvocationHandler.invoke() 方法处理
         *
         */
        return bean;
        }

}
