package com.chenhao.common.annotation.Ioc;

public interface BeanNameAware {
    public void setBeanName(String beanName);

    public String getBeanName(Object object);
}
