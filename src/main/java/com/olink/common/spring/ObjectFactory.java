package com.olink.common.spring;

public interface ObjectFactory<T> {
    T getObject() throws Exception;
}