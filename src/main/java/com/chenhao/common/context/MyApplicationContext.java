package com.chenhao.common.context;

import com.chenhao.common.annotation.*;
import com.chenhao.common.annotation.Ioc.BeanNameAware;
import com.chenhao.common.annotation.Ioc.BeanPostProcessor;
import com.chenhao.common.annotation.Ioc.InitiallizingBean;
import com.chenhao.common.annotation.Ioc.ObjectFactory;
import com.chenhao.common.annotation.aop.Aspect;
import com.chenhao.common.annotation.aop.Before;
import com.chenhao.common.aop.AdviceDefinition;
import com.chenhao.common.aop.AdviceType;
import com.chenhao.common.aop.AspectDefinition;
import com.chenhao.common.spring.*;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.reflect.*;
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
    private static final Logger log = LoggerFactory.getLogger(MyApplicationContext.class);
    private Class configClass;
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(); // 一级缓存：单例池，存放完全初始化好的 bean
    private final Map<String, Object> earlySingletonObjects = new HashMap<>(); // 二级缓存：存放早期 bean 引用
    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(); // 三级缓存：存放 ObjectFactory，用于延迟创建 bean
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();
    private List<AspectDefinition> aspectDefinitions = new ArrayList<>(); //存储 Aspect 信息
    private Map<String, List<Method>> preDestroyMethodsMap = new ConcurrentHashMap<>(); //存储destory方法
    private Map<Class<?>, List<Class<?>>> constructorArgTypesMap = new HashMap<>(); // 临时存储构造器参数类型

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

                            String typeName = clazz.getTypeName();
                            Component annotation = clazz.getDeclaredAnnotation(Component.class);
                            String beanName = annotation.value().isEmpty() ? typeName.substring(typeName.lastIndexOf(".") + 1):annotation.value();
                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setClazz(clazz);
                            if (clazz.isAnnotationPresent(Scope.class)) {
                                Scope scopeAnnotation = clazz.getDeclaredAnnotation(Scope.class);
                                beanDefinition.setScope(scopeAnnotation.value());
                            } else {
                                beanDefinition.setScope("singleton");
                            }
                            beanDefinitionMap.put(beanName, beanDefinition);

                            // 解析构造器参数类型
                            List<Class<?>> constructorArgTypes = new ArrayList<>();
                            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                                if (constructor.isAnnotationPresent(Autowired.class)) { // 找到带有 @Autowired 注解的构造器
                                    for (Parameter parameter : constructor.getParameters()) {
                                        constructorArgTypes.add(parameter.getType());
                                    }
                                    break; // 只处理一个带有 @Autowired 的构造器
                                }
                            }
                            constructorArgTypesMap.put(clazz, constructorArgTypes);


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
        // 先从三层缓存里拿

        //判断一级缓存里有没有完备的Bean
        if (singletonObjects.containsKey(beanName))
            return singletonObjects.get(beanName);
        //如果没有，那么判断二级缓存里有没有早期的半成品Bean
        if (earlySingletonObjects.containsKey(beanName)) {
            return earlySingletonObjects.get(beanName);
        }
        //如果没有，那么判断三级缓存里有没有工厂Bean
        if (singletonFactories.containsKey(beanName)) {
            try {
                Object bean = singletonFactories.get(beanName).getObject();
                earlySingletonObjects.put(beanName, bean);
                singletonFactories.remove(beanName);
                return bean;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
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
        String beanName = null;
        Component annotation = (Component) clazz.getAnnotation(Component.class);
        if (annotation.value().isEmpty()) {
            beanName = clazz.getSimpleName();
        } else {
            beanName = annotation.value();
        }

        //默认是Bean的名字，如果Component指定了才是指定值

        try {
            Object instance = null;
            List<Class<?>> constructorArgTypes = constructorArgTypesMap.get(clazz); // 获取构造器参数类型

            // 1. 实例化 Bean
            if (constructorArgTypes != null && !constructorArgTypes.isEmpty()) {
                // 1. 获取所有构造函数
                java.lang.reflect.Constructor<?>[] constructors = clazz.getDeclaredConstructors();
                java.lang.reflect.Constructor<?> autowiredConstructor = null;

                // 2. 找到带有 @Autowired 注解的构造函数
                for (java.lang.reflect.Constructor<?> constructor : constructors) {
                    if (constructor.isAnnotationPresent(Autowired.class)) {
                        autowiredConstructor = constructor;
                        break;
                    }
                }
                if (autowiredConstructor != null) {

                    instance = autowiredConstructor.newInstance();//暂时不进行依赖注入

                } else {
                    // 如果没有使用Autowired注解，那么默认使用无参构造函数
                    instance = clazz.getDeclaredConstructor().newInstance();
                }

            } else {
                // 如果没有指定构造函数参数，则使用无参构造函数
                instance = clazz.getDeclaredConstructor().newInstance();

            }

            // 2. 放入三级缓存 (ObjectFactory)
            Object finalInstance = instance;
            ObjectFactory<?> singletonFactory = new ObjectFactory<Object>() {
                @Override
                public Object getObject() throws Exception {
                    try {
                        List<Class<?>> constructorArgTypes2 = constructorArgTypesMap.get(clazz);
                        java.lang.reflect.Constructor<?>[] constructors = clazz.getDeclaredConstructors();
                        java.lang.reflect.Constructor<?> autowiredConstructor = null;
                        Object[] args = null;

                        // 2. 找到带有 @Autowired 注解的构造函数
                        for (java.lang.reflect.Constructor<?> constructor : constructors) {
                            if (constructor.isAnnotationPresent(Autowired.class)) {
                                autowiredConstructor = constructor;
                                break;
                            }
                        }
                        if (autowiredConstructor != null) {
                            args = new Object[constructorArgTypes2.size()];
                            for (int i = 0; i < constructorArgTypes2.size(); i++) {
                                String dependencyBeanName = lowerFirstChar(constructorArgTypes2.get(i).getSimpleName());
                                args[i] = getBean(dependencyBeanName);
                            }
                            return autowiredConstructor.newInstance(args);
                        }
                        return finalInstance;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            singletonFactories.put(beanName, singletonFactory);

            // 3. 从三级缓存中获取 early bean 并放入二级缓存

            instance = getBean(beanName);
            //应用AOP,在放入单例池之前应用
            instance = applyAOP(instance, beanName);


            //依赖注入
            Object bean = null;
            for (Field declaredField : clazz.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(Autowired.class)) {
                    Autowired autowired = declaredField.getAnnotation(Autowired.class);
                    String dependencyBeanName = declaredField.getType().getSimpleName();// 获取依赖的 beanName (根据类型)
                    //如果Autowired有指定名称，那么按照名称装配
                    if (null != autowired.value() && !autowired.value().isEmpty()) {
                        dependencyBeanName = autowired.value();
                    }
                    bean = getBean(dependencyBeanName);
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
            singletonObjects.put(beanName, instance);
            return instance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
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

    private void invokePostConstructMethods(Object instance) throws InvocationTargetException, IllegalAccessException {
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
            if (pointcutExpression.contains(targetClass.getSimpleName())) {
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
        if (targetClass.isInterface()) {
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
        }// 2. 使用 CGLIB 代理 (如果目标类不是接口)
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
        String className = bean.getClass().getSimpleName();
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