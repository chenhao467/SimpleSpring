package com.olink.biz.service;

import com.olink.common.annotation.BeanNameAware;
import com.olink.common.annotation.InitiallizingBean;
import com.olink.common.annotation.Autowired;
import com.olink.common.annotation.Component;

/*
*功能：
 作者：chenhao
*日期： 2025/4/26 下午3:34
*/
@Component("orderService")
public class OrderService implements BeanNameAware, InitiallizingBean {
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
