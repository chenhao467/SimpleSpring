package com.chenhao.bean;

import com.chenhao.common.annotation.Ioc.BeanNameAware;
import com.chenhao.common.annotation.Ioc.InitiallizingBean;
import com.chenhao.common.annotation.Transactional;
import com.chenhao.common.annotation.service.OrderService;
import com.chenhao.common.proxy.TransactionManager;
import com.chenhao.common.annotation.Autowired;
import com.chenhao.common.annotation.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/*
*功能：
 作者：chenhao
*日期： 2025/4/26 下午3:34
*/
@Component
public class OrderServiceImpl implements BeanNameAware, InitiallizingBean, OrderService {
    @Autowired
    private UserServiceImpl userService;

   public void test(){
       userService.test();
   }

    @Override
    @Transactional
    public String getUserNameById(String id) {
        String name = null;
        try (Connection conn = TransactionManager.getConnection()) {
            String sql = "SELECT phone FROM truck_user WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                name = rs.getString("phone");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
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
