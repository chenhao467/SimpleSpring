package com.chenhao.common.proxy;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/*
*功能：
 作者：chenhao
*日期： 2025/7/28 下午8:54
*/
public class TransactionalMethodInterceptor implements MethodInterceptor {
    private final Object target;

    public TransactionalMethodInterceptor(Object target) {
        this.target = target;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        System.out.println("CGLIB事务开始");
        Object result = proxy.invoke(target, args);
        System.out.println("CGLIB事务提交");
        return result;
    }
}

