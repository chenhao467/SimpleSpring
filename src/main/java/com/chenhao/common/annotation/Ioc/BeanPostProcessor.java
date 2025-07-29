package com.chenhao.common.annotation.Ioc;

/*
*功能：
 作者：chenhao
*日期： 2025/4/26 下午5:20
*/
public interface BeanPostProcessor{
    public Object before(Object bean,String beanName);

    public Object after(Object bean, String beanName);
}
