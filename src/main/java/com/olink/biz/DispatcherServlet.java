
package com.olink.biz;

import com.alibaba.fastjson2.JSONObject;
import com.olink.common.annotation.*;
import com.olink.common.spring.BeanPostProcessor;
import com.olink.common.spring.ControllerMethodMapping;
import com.olink.common.spring.ModelAndView;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

@Component("dispatcherServlet")
public class DispatcherServlet extends HttpServlet implements BeanPostProcessor,Servlet {

    public static Map<String, ControllerMethodMapping> handlerMapping = new HashMap<>();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ControllerMethodMapping handler = find(req);
        if(handler==null){
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().write("<h1>404,你的请求没有对应的Controller</h1><br>");
            return;
        }
        try {
            Object controller = handler.getController();
            Object[] args = resolveArgs(req,handler.getMethod());
            Object result = handler.getMethod().invoke(controller,args);
            switch (handler.getResultType()){
                case HTML->{
                    resp.setContentType("text/html");
                    resp.getWriter().write(result.toString());
                }
                case JSON -> {
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.getWriter().write(JSONObject.toJSONString(result));
                }
                case LOCAL -> {
                    ModelAndView mv = (ModelAndView) result;
                    String view = mv.getView();
                    InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(view);
                    try(resourceAsStream){
                        String html = new String(resourceAsStream.readAllBytes());
                        resp.setContentType("text/html;charset=UTF-8");
                        resp.getWriter().write(html);
                    }
                }
            }
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }


        resp.setContentType("text/html");
        resp.getWriter().write(
                "<h1>Hello from Embedded Tomcat!</h1><br>" + req.getRequestURL().toString()
        );




    }

    private Object[] resolveArgs(HttpServletRequest req, Method method) {
        Parameter[] parameters = method.getParameters();
        Object[]args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String value = null;
            Param param = parameter.getAnnotation(Param.class);
            if(param!=null){
                value = req.getParameter(param.value());
            }else{
                value = req.getParameter(parameter.getName());
            }
            Class<?> parameterType = parameter.getType();
            if (String.class.isAssignableFrom(parameterType)){
                args[i] = value;
            }else if (Integer.class.isAssignableFrom(parameterType)){
                args[i] = Integer.parseInt(value);
            } else{
                args[i] = null;
            }
        }
        return args;
    }

    public ControllerMethodMapping find(HttpServletRequest req){
        return handlerMapping.get(req.getRequestURI());
    }
    @Override
    public Object before(Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object after(Object bean, String beanName) {
        if (!bean.getClass().isAnnotationPresent(Controller.class)){
            return bean;
        }
        RequestMapping classrm = bean.getClass().getAnnotation(RequestMapping.class);
        String url = classrm!=null?classrm.value():"";  //根据类的RequestMapping注解值获取类的url
            Method[] declaredMethods = bean.getClass().getDeclaredMethods();
            for (Method declaredMethod : declaredMethods) {
                if(declaredMethod.isAnnotationPresent(RequestMapping.class)){
                    RequestMapping methodrm = declaredMethod.getAnnotation(RequestMapping.class);
                    String key = url.concat(methodrm.value());
                    ControllerMethodMapping handler = new ControllerMethodMapping(bean, declaredMethod);
                    //根据传入的method解析出resultType
                   if(handlerMapping.containsKey(key)){
                       throw new RuntimeException("url重复"+ key);
                   }
                    handlerMapping.put(key,handler);
                }
            }

        return bean;
    }
}