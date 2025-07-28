package com.olink.bean;

import com.olink.common.annotation.Component;
import com.olink.common.annotation.Transactional;
import com.olink.common.spring.BeanPostProcessor;
import com.olink.common.proxy.TransactionalInvocationHandler;
import com.olink.common.proxy.TransactionalMethodInterceptor;
import net.sf.cglib.proxy.Enhancer;

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

                // 优先使用 JDK 动态代理（如果实现了接口）
                if (clazz.isInterface()) {
                    return Proxy.newProxyInstance(
                            clazz.getClassLoader(),
                            clazz.getInterfaces(),
                            new TransactionalInvocationHandler(bean)
                    );
                } else {
                    // 否则使用 CGLIB 动态代理
                    Enhancer enhancer = new Enhancer();
                    enhancer.setSuperclass(clazz);
                    enhancer.setCallback(new TransactionalMethodInterceptor(bean));
                    return enhancer.create();
                }
            }
        }

        return bean;
    }


}
