package com.chenhao.bean;

import com.chenhao.common.annotation.Autowired;
import com.chenhao.common.annotation.Component;
import com.chenhao.common.annotation.Controller;
import com.chenhao.common.annotation.Param;
import com.chenhao.common.annotation.requestMapping.GetMapping;
import com.chenhao.common.annotation.requestMapping.RequestMapping;

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
