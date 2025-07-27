package com.olink.common.context;

import com.olink.common.annotation.*;
import com.olink.common.annotation.aop.Aspect;
import com.olink.common.annotation.aop.Before;
import com.olink.common.aop.AdviceDefinition;
import com.olink.common.aop.AdviceType;
import com.olink.common.aop.AspectDefinition;
import com.olink.common.spring.*;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;


import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
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
    private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();
    private List<AspectDefinition> aspectDefinitions = new ArrayList<>(); //存储 Aspect 信息
    private Map<String, List<Method>> preDestroyMethodsMap = new ConcurrentHashMap<>(); //存储destory方法

    public MyApplicationContext(Class configClass) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        this.configClass = configClass;
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
        String path = componentScanAnnotation.value();
        scan(path);
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beandefinition = entry.getValue();
            if (beandefinition.getScope().equals("singleton")) {
                Object bean = createBean(beandefinition);
                singletonObjects.put(beanName, bean);
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
                            if (!clazz.isInterface()) {
                                Object instance = clazz.getDeclaredConstructor().newInstance();
                                if (instance instanceof BeanPostProcessor) {
                                    beanPostProcessorList.add((BeanPostProcessor) instance);
                                }
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
                            beanDefinitionMap.put(beanName, beanDefinition);

                            //处理AspectJ
                            if (clazz.isAnnotationPresent(Aspect.class)) {
                                processAspect(clazz, beanName);
                            }

                            List<Method> preDestroyMethods = new ArrayList<>();
                            for (Method method : clazz.getDeclaredMethods()) {
                                if (method.isAnnotationPresent(PreDestroy.class)) {
                                    preDestroyMethods.add(method);
                                }
                            }
                            preDestroyMethodsMap.put(beanName, preDestroyMethods); // 存储 @PreDestroy 方法信息
                        }


                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    public Object getBean(String beanName) {
        if (singletonObjects.containsKey(beanName))
            return singletonObjects.get(beanName);
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) {
            throw new NullPointerException();
        }
        Object bean = createBean(beanDefinition);
        singletonObjects.put(beanName, bean);
        return bean;
    }

    public Object createBean(BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getClazz();
        try {

            Object instance = clazz.getDeclaredConstructor().newInstance();

            // 提前加入到singleton池子里
            // 修改单例池注册逻辑，优先读取注解中的名称
            Component component = (Component) clazz.getAnnotation(Component.class);
            String value = component.value();
            String beanName = clazz.isAnnotationPresent(Component.class)
                    ? value// 读取注解中的名称
                    : lowerFirstChar(clazz.getSimpleName());

            //应用AOP,在放入单例池之前应用
            instance = applyAOP(instance, beanName);

            singletonObjects.put(beanName, instance);


            //依赖注入
            Object bean = null;
            for (Field declaredField : clazz.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(Autowired.class)) {
                    bean = getBean(declaredField.getName());
                    declaredField.setAccessible(true);
                    declaredField.set(instance, bean);
                }
            }
            //Aware回调方法
            if (instance instanceof BeanNameAware) {
                String A = ((BeanNameAware) instance).getBeanName(instance);
                String B = bean.getClass().getSimpleName();
                System.out.println("成功给" + A + "赋值" + B);

            }
            //BeanPostProcessor前置
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.before(instance, beanName);
            }
            // 执行 @PostConstruct 注解的方法
            invokePostConstructMethods(instance);
            //初始化逻辑
            if (instance instanceof InitiallizingBean) {
                ((InitiallizingBean) instance).afterPropertiesSet();
            }


            //BeanPostProcessor后置
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                //对bean进行增强操作 其中(url,handler)就是在这里被加入到handlerMapping中
                // handler:(bean,method)
                instance = beanPostProcessor.after(instance, beanName);
            }

            return instance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
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

    public List<Object> values() {
        List<Object> list = new ArrayList<>();
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
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

    //--------------------------------------------------AOP相关逻辑-----------------------------------------------------------------//

    /**
     * 专门处理AOP的逻辑
     * @param aspectClass
     * @param beanName
     */
    private void processAspect(Class<?> aspectClass, String beanName) {
        AspectDefinition aspectDefinition = new AspectDefinition();
        aspectDefinition.setBeanName(beanName);
        aspectDefinition.setAspectClass(aspectClass);

        // 扫描 Aspect 类中的 Advice 方法
        Method[] methods = aspectClass.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Before.class)) {
                Before beforeAnnotation = method.getAnnotation(Before.class);
                String pointcutExpression = beforeAnnotation.value();
                AdviceDefinition adviceDefinition = new AdviceDefinition();
                adviceDefinition.setMethod(method);
                adviceDefinition.setType(AdviceType.BEFORE);
                adviceDefinition.setPointcutExpression(pointcutExpression);
                aspectDefinition.getAdviceDefinitions().add(adviceDefinition);
            }
            // 其他 Advice 类型的处理 (After, AfterReturning, AfterThrowing, Around) ...
        }
        aspectDefinitions.add(aspectDefinition);
    }

    private boolean shouldApplyAOP(Object bean, AspectDefinition aspectDefinition) {
        //简单的类名匹配  后续可以考虑使用 AspectJ的表达式解析器
        Class<?> targetClass = bean.getClass();
        for (AdviceDefinition adviceDefinition : aspectDefinition.getAdviceDefinitions()) {
            String pointcutExpression = adviceDefinition.getPointcutExpression();
            if (pointcutExpression.contains(targetClass.getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 应用AOP逻辑
     * @param bean
     * @param beanName
     * @return
     */
    private Object applyAOP(Object bean, String beanName) {
        for (AspectDefinition aspectDefinition : aspectDefinitions) {
            //TODO:根据pointCut表达式来判断
            try {
                if (shouldApplyAOP(bean, aspectDefinition)) {
                    //创建代理对象
                    bean = createAopProxy(bean, aspectDefinition);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
        return bean;
    }

    private Object createAopProxy(Object bean, AspectDefinition aspectDefinition) throws Exception {
        Class<?> targetClass = bean.getClass();
        Class<?>[] interfaces = targetClass.getInterfaces();
        if (interfaces.length > 0) {
            return java.lang.reflect.Proxy.newProxyInstance(
                    bean.getClass().getClassLoader(),
                    interfaces,
                    (proxy, method, args) -> {
                        // 1. Before Advice
                        Object result = null;
                        invokeAdvice(aspectDefinition, AdviceType.BEFORE, bean, method, args);
                        try {
                            // 2. 执行目标方法
                            result = method.invoke(bean, args);

                            // 3. AfterReturning Advice
                            invokeAdvice(aspectDefinition, AdviceType.AFTER_RETURNING, bean, method, args, result);
                        } catch (Throwable e) {
                            // 4. AfterThrowing Advice
                            invokeAdvice(aspectDefinition, AdviceType.AFTER_THROWING, bean, method, args, e);
                            throw e; // 重新抛出异常
                        } finally {
                            // 5. After Advice
                            invokeAdvice(aspectDefinition, AdviceType.AFTER, bean, method, args);
                        }
                        return result;
                    }
            );
        }// 2. 使用 CGLIB 代理 (如果目标类没有实现接口)
        else {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(targetClass);
            enhancer.setCallback(new MethodInterceptor() {
                @Override
                public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                    Object result = null;

                    // 1. Before Advice
                    invokeAdvice(aspectDefinition, AdviceType.BEFORE, bean, method, args);

                    try {
                        // 2. 执行目标方法
                        result = proxy.invokeSuper(obj, args);

                        // 3. AfterReturning Advice
                        invokeAdvice(aspectDefinition, AdviceType.AFTER_RETURNING, bean, method, args, result);

                    } catch (Throwable e) {
                        // 4. AfterThrowing Advice
                        invokeAdvice(aspectDefinition, AdviceType.AFTER_THROWING, bean, method, args, e);
                        throw e; // 重新抛出异常
                    } finally {
                        // 5. After Advice
                        invokeAdvice(aspectDefinition, AdviceType.AFTER, bean, method, args);
                    }

                    return result;
                }
            });
            return enhancer.create();
        }
    }

    //简单的判断是否匹配
    private boolean isMatch(Object bean, Method method, String pointcutExpression) {
        String className = bean.getClass().getName();
        String methodName = method.getName();
        String signature = className + "." + methodName + "(..)";
        return pointcutExpression.contains(className) && pointcutExpression.contains(methodName);
    }

    private void invokeAdvice(AspectDefinition aspectDefinition, AdviceType adviceType, Object bean, Method method, Object[] args, Object result) throws Exception {
        for (AdviceDefinition advice : aspectDefinition.getAdviceDefinitions()) {
            if (advice.getType() == adviceType && isMatch(bean, method, advice.getPointcutExpression())) {
                Method adviceMethod = advice.getMethod();
                Object aspectInstance = getBean(aspectDefinition.getBeanName()); // 获取 Aspect 实例
                switch (adviceType) {
                    case BEFORE:
                        adviceMethod.invoke(aspectInstance);
                        break;
                    case AFTER_RETURNING:
                        adviceMethod.invoke(aspectInstance, result); // 传递返回值
                        break;
                    case AFTER_THROWING:
                        adviceMethod.invoke(aspectInstance, result); // 传递异常
                        break;
                    default:
                        adviceMethod.invoke(aspectInstance);
                }
            }
        }
    }

    private void invokeAdvice(AspectDefinition aspectDefinition, AdviceType adviceType, Object bean, Method method, Object[] args) throws Exception {
        invokeAdvice(aspectDefinition, adviceType, bean, method, args, null);
    }

    //----------------------------------------@PreDestory--------------------------------------//
    // 在 destroyBean 方法中，从 preDestroyMethodsMap 中获取 @PreDestroy 方法信息
    public void destroyBean(String beanName) {
        Object bean = singletonObjects.get(beanName);
        if (bean == null) {
            return;
        }

        List<Method> preDestroyMethods = preDestroyMethodsMap.get(beanName);
        if (preDestroyMethods == null) {
            return;
        }

        for (Method method : preDestroyMethods) {
            try {
                method.setAccessible(true); // 允许访问私有方法
                method.invoke(bean); // 调用 @PreDestroy 方法
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 从单例池中移除 Bean
        singletonObjects.remove(beanName);
    }
    public void shutdown() {
        for (String beanName : singletonObjects.keySet()) {
            destroyBean(beanName);
        }
    }
}