package com.olink.common.springclass;

import lombok.Data;

/*
*功能：
 作者：chenhao
*日期： 2025/4/26 下午2:22
*/
@Data
public class BeanDefinition {
    private Class clazz;
    private String scope;
    public BeanDefinition(Class clazz,String scope){
        this.clazz = clazz;
        this.scope = scope;
    }
    public BeanDefinition(){

    }
}
