# SimpleSpring
A Simple Spring For Me
你好！ 这是我为了增强对Spring的理解 写的一个简易版Spring 
实现的功能
-- 1.Ioc基本功能
    Bean定义来源：
      支持ComponentScan注解指定包下类扫描bean
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
      支持@Autowired注入 -> 更新：还支持构造器注入
      暂不支持注入集合类型依赖
      支持三级缓存解决循环依赖
      暂未解决多个bean满足依赖类型的情况
-- 2.Aop基本功能 
    支持@Aspect注解，注解value可以指定具体被增强方法
    动态代理方式：目前还有一定缺陷，如由于设置只要不是接口就走cglib代理--导致几乎所有bean在Aspect代理时都会被cglib代理。@Transactional同理
      支持JDK
      支持CGLIB代理  -- 意味着我可以注入往字段里注入代理类（因为代理类本质是它的子类）
    Advice类型：
      支持@Before, @After, @AfterReturning, @AfterThrowing
-- 3.支持事务      
      
