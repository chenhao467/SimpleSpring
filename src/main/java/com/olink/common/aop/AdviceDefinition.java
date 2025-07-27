package com.olink.common.aop;

import lombok.Data;

import java.lang.reflect.Method;

@Data
public class AdviceDefinition {
        private Method method;
        private AdviceType type;
        private String pointcutExpression;

    }


