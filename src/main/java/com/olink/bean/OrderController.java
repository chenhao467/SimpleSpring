package com.olink.bean;

import com.olink.common.annotation.Autowired;
import com.olink.common.annotation.Component;
import com.olink.common.annotation.Controller;
import com.olink.common.annotation.Param;
import com.olink.common.annotation.aop.Aspect;
import com.olink.common.annotation.aop.Before;
import com.olink.common.annotation.requestMapping.GetMapping;
import com.olink.common.annotation.requestMapping.RequestMapping;
import com.olink.common.spring.OrderService;
import lombok.Data;

/*
*功能：
 作者：chenhao
*日期： 2025/7/28 下午4:26
*/
@Component
@Controller
@RequestMapping("/order")
public class OrderController{
    @Autowired
    private OrderServiceImpl orderService;

    @GetMapping("/get")
    public String getUserNameById(@Param("id") String id){
        String name  = orderService.getUserNameById(id);
        return name;
    }
}
