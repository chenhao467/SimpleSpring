package com.chenhao.common.annotation.Ioc;

public interface ObjectFactory<T> {
    T getObject() throws Exception;
}