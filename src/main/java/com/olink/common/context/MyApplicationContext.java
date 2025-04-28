package com.olink.common.context;

import com.olink.biz.DispatcherServlet;
import com.olink.common.annotation.*;
import com.olink.common.spring.*;


import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
*功能：
 作者：chenhao
*日期： 2025/4/26 下午1:28
*/

public class MyApplicationContext {
    private Class configClass;
    private ConcurrentHashMap<String,Object> singletonObjects = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();



    public MyApplicationContext(Class configClass) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        this.configClass = configClass;
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
        String path = componentScanAnnotation.value();
        scan(path);
        for(Map.Entry<String,BeanDefinition> entry: beanDefinitionMap.entrySet()){
            String beanName = entry.getKey();
            BeanDefinition beandefinition = entry.getValue();
            if(beandefinition.getScope().equals("singleton")){
                Object bean = createBean(beandefinition);
                singletonObjects.put(beanName,bean);
            }
        }

    }
    public void scan(String path) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        path = path.replace(".", "/");
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL resource = classLoader.getResource(path);
        File file = new File(resource.getFile());
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                String fileName = f.getAbsolutePath();
                if (fileName.endsWith(".class")) {
                    String className = fileName.substring(fileName.indexOf("com"), fileName.indexOf(".class"));
                    className = className.replace("\\", ".");
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        if (clazz.isAnnotationPresent(Component.class)) {
                            //自定义BeanPostProcessor加入容器
                            Object instance = clazz.getDeclaredConstructor().newInstance();
                            if(instance instanceof BeanPostProcessor){
                                beanPostProcessorList.add((BeanPostProcessor) instance);
                            }

                            Component annotation = clazz.getDeclaredAnnotation(Component.class);
                            String beanName = annotation.value();
                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setClazz(clazz);
                            if (clazz.isAnnotationPresent(Scope.class)) {
                                Scope scopeAnnotation = clazz.getDeclaredAnnotation(Scope.class);
                                beanDefinition.setScope(scopeAnnotation.value());
                            } else {
                                beanDefinition.setScope("singleton");

                            }
                            beanDefinitionMap.put(beanName,beanDefinition);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    public Object getBean (String beanName){
       if(singletonObjects.containsKey(beanName))
           return singletonObjects.get(beanName);
       BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
       if(beanDefinition==null){
           throw new NullPointerException();
       }
       Object bean = createBean(beanDefinition);
       singletonObjects.put(beanName,bean);
       return bean;
    }
    public Object createBean(BeanDefinition beanDefinition){
        Class clazz = beanDefinition.getClazz();
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();

            //提前加入到singleton池子里
            // 修改单例池注册逻辑，优先读取注解中的名称
             Component component = (Component) clazz.getAnnotation(Component.class);
             String value = component.value();
             String beanName = clazz.isAnnotationPresent(Component.class)
                    ? value// 读取注解中的名称
                    : lowerFirstChar(clazz.getSimpleName());
            singletonObjects.put(beanName, instance);


            //依赖注入
            Object bean = null;
            for(Field declaredField : clazz.getDeclaredFields()){

                if(declaredField.isAnnotationPresent(Autowired.class)){
                    bean = getBean(declaredField.getName());
                    declaredField.setAccessible(true);
                    declaredField.set(instance,bean);
                }
            }
            //Aware回调方法
            if(instance instanceof BeanNameAware){
                String A = ((BeanNameAware) instance).getBeanName(instance);
                String B = bean.getClass().getSimpleName();
                System.out.println("成功给"+A+"赋值"+B);

            }
            //BeanPostProcessor前置
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.before(instance,beanName);
            }
            // 执行 @PostConstruct 注解的方法
            invokePostConstructMethods(instance);
            //初始化逻辑
            if(instance instanceof InitiallizingBean){
                ((InitiallizingBean) instance).afterPropertiesSet();
            }


            //BeanPostProcessor后置
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                //对bean进行增强操作 其中(url,handler)就是在这里被加入到handlerMapping中
                // handler:(bean,method)
                instance = beanPostProcessor.after(instance,beanName);
            }

            return instance;
        }catch (InstantiationException e){
            e.printStackTrace();
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }catch (InvocationTargetException e){
            e.printStackTrace();
        }catch (NoSuchMethodException e){
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
    public String lowerFirstChar(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        char[] chars = str.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
    public List<Object> values(){
        List<Object> list = new ArrayList<>();
        for(Map.Entry<String,BeanDefinition> entry: beanDefinitionMap.entrySet()){
            String beanName = entry.getKey();
            Object bean = getBean(beanName);
            list.add(bean);
        }
        return list;
    }
    private void invokePostConstructMethods(Object instance) throws Exception {
        // 获取所有的方法
        Method[] methods = instance.getClass().getDeclaredMethods();

        for (Method method : methods) {
            // 如果方法上有 @PostConstruct 注解
            if (method.isAnnotationPresent(PostConstruct.class)) {
                method.setAccessible(true); // 确保可以访问私有方法
                method.invoke(instance); // 执行该方法
            }
        }
}
}