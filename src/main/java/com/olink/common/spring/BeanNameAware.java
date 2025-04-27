package com.olink.common.spring;

public interface BeanNameAware {
    public void setBeanName(String beanName);

    public String getBeanName(Object object);
}
