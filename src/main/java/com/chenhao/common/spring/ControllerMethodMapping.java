package com.chenhao.common.spring;

import com.chenhao.common.annotation.ResponseBody;
import lombok.Data;

import java.lang.reflect.Method;
@Data
public class ControllerMethodMapping {
        private final Object controller;
        private final Method method;
        private final ResultType resultType;
        private final String httpMethod;
        public ControllerMethodMapping(Object controller, Method method,String httpMethod) {
            this.controller = controller;
            this.method = method;
            this.resultType = resolveResultType(method);
            this.httpMethod = httpMethod;
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