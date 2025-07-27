package com.olink.common.aop;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AspectDefinition {
        private String beanName;
        private Class<?> aspectClass;
        private List<AdviceDefinition> adviceDefinitions = new ArrayList<>();
    }