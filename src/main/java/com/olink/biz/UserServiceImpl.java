package com.olink.biz;

import com.olink.common.annotation.Component;
import com.olink.common.annotation.Transactional;
import com.olink.common.spring.TransactionManager;
import com.olink.common.spring.UserService;
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
@Component("userService")
public class UserServiceImpl implements UserService  {

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

}
