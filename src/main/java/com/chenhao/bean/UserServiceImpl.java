package com.chenhao.bean;

import com.chenhao.common.annotation.Autowired;
import com.chenhao.common.annotation.Component;
import com.chenhao.common.annotation.Transactional;
import com.chenhao.common.proxy.TransactionManager;
import com.chenhao.common.annotation.service.UserService;
import com.chenhao.entity.User;
import lombok.Data;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/*
*功能：
 作者：chenhao
*日期： 2025/4/26 下午1:31
*/
@Data
@Component
public class UserServiceImpl  implements UserService {
@Autowired
private OrderServiceImpl orderService;
    public void test(){
        System.out.println("hello Spring!");
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
    @Transactional
    public String AddUserName(User user) {
        try (Connection conn = TransactionManager.getConnection()) {

        } catch (Exception e) {
            e.printStackTrace();
            return "添加异常：" + e.getMessage();
        }
        return "添加成功";
    }



}
