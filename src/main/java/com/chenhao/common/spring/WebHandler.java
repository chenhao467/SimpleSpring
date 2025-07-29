package com.chenhao.common.spring;

import lombok.Data;

import java.lang.reflect.Method;

/*
*功能：
 作者：chenhao
*日期： 2025/4/27 下午5:25
*/
@Data
public class WebHandler {
    private final Object controllerBean;
    private final Method controllerMethod;


}
