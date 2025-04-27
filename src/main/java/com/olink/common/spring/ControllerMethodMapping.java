package com.olink.common.spring;

import com.olink.common.annotation.ResponseBody;
import lombok.Data;
import lombok.Getter;

import java.lang.reflect.Method;
@Getter
public class ControllerMethodMapping {
        private final Object controller;
        private final Method method;
        private final ResultType resultType;
        public ControllerMethodMapping(Object controller, Method method) {
            this.controller = controller;
            this.method = method;
            this.resultType = resolveResultType(method);
        }

        private ResultType resolveResultType(Method method) {
            if(method.isAnnotationPresent(ResponseBody.class)){
                return ResultType.JSON;
            }
            if(method.getReturnType()==ModelAndView.class){
                return ResultType.LOCAL;
            }
           return ResultType.HTML;
        }

  public  enum ResultType{
            JSON,HTML,LOCAL
        }
    }