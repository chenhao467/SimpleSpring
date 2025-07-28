# SimpleSpring
A Simple Spring For Me
你好！ 这是我为了增强对Spring的理解 写的一个简易版Spring 
实现的功能
-- 1.Ioc基本功能
    Bean定义来源：
      支持注解扫描：@Component, @Service,@Controller等
    Bean 的作用域 (Scope)：
      支持Singleton
      支持Prototype
    Bean 的生命周期：
      支持 @PostConstruct 和 @PreDestroy 注解
      支持InitializingBean 接口
      支持通用的BeanPostProcessor
      支持通用的Aware回调方法
    依赖注入：
      仅支持@Autowired注入
      暂不支持注入集合类型依赖
      通过提前暴露到单例池解决循环依赖（后续会通过三级缓存解决构造器注入时的循环依赖问题）
      暂未解决多个bean满足依赖类型的情况
-- 2.Aop基本功能 
    动态代理方式：
      支持JDK
      支持CGLIB
    Advice类型：
      支持@Before, @After, @AfterReturning, @AfterThrowing
-- 3.支持事务      
      
