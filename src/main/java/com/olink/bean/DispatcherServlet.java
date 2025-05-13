
package com.olink.bean;

import com.alibaba.fastjson2.JSON;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
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
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }


        resp.setContentType("text/html");
        resp.getWriter().write(
                "<h1>Hello from Embedded Tomcat!</h1><br>" + req.getRequestURL().toString()
        );




    }

    private Object[] resolveArgs(HttpServletRequest req, Method method) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String value = null;
            Param param = parameter.getAnnotation(Param.class);
            if (param != null) {
                value = req.getParameter(param.value());
            } else {
                value = req.getParameter(parameter.getName());
            }
            Class<?> parameterType = parameter.getType();

            // 如果参数带有 @RequestBody 注解，说明要从请求体中解析 JSON
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                StringBuilder jsonBuilder = new StringBuilder();
                BufferedReader reader = req.getReader();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                String json = jsonBuilder.toString();

                // 使用 fastjson 或 jackson 反序列化
                Object paramObj = JSON.parseObject(json, parameterType); // fastjson 示例
                args[i] = paramObj;
            }
            // 如果是基本类型
            else if (parameterType == String.class || parameterType == Integer.class || parameterType == Long.class) {
                String baseval = null;
                Param parm = parameter.getAnnotation(Param.class);
                if (parm != null) {
                    value = req.getParameter(parm.value());
                } else {
                    value = req.getParameter(parameter.getName());
                }
                args[i] = convertType(value, parameterType.getSimpleName());
            }

        }
        return args;
    }

    private Object convertType(String value, String basetype) {

        if(basetype.equals("String")){
            return value;
        }else if(basetype.equals("Integer")){
            return Integer.parseInt(value);
        }else if(basetype.equals("Long")){
            return Long.parseLong(value);
        }else{
            return null;
        }
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
