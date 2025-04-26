package com.olink.common.annotation;

public interface BeanNameAware {
    public void setBeanName(String beanName);

    public String getBeanName(Object object);
}
