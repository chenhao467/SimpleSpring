package com.olink.common.spring;

import com.olink.common.config.ConnectionManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class TransactionalInvocationHandler implements InvocationHandler {
    private final Object target;

    public TransactionalInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("事务代理逻辑");
        Object result;
        try {
            ConnectionManager.begin();
            result = method.invoke(target, args);
            ConnectionManager.commit();
        } catch (Exception e) {
            ConnectionManager.rollback();
            throw e;
        } finally {
            ConnectionManager.closeConnection();
        }
        return result;
    }
}
