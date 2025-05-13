package com.olink.bean;

import com.olink.common.spring.BeanNameAware;
import com.olink.common.spring.InitiallizingBean;
import com.olink.common.annotation.Autowired;
import com.olink.common.annotation.Component;
import com.olink.common.spring.OrderService;
import com.olink.common.spring.UserService;

/*
*功能：
 作者：chenhao
*日期： 2025/4/26 下午3:34
*/
@Component("orderService")
public class OrderServiceImpl implements BeanNameAware, InitiallizingBean, OrderService {
    @Autowired
    private UserService userService;

   public void test(){
       userService.test();
   }

    @Override
    public void setBeanName(String beanName) {
    }

    @Override
    public String getBeanName(Object o) {
        return o.getClass().getSimpleName();
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("初始化方法");
    }
}
