package com.olink.bean;

import com.olink.common.annotation.Component;
import com.olink.common.annotation.Transactional;
import com.olink.common.spring.TransactionManager;
import com.olink.common.spring.UserService;
import com.olink.entity.User;
import lombok.Data;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

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

    @Override
    @Transactional
    public String AddUserName(User user) {
        try (Connection conn = TransactionManager.getConnection()) {
            String sql = "INSERT INTO truck_user (id, phone, password,create_time,enabled) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setLong(1, user.getId());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getPassword());
            ps.setTimestamp(4, Timestamp.valueOf("2025-04-01 10:21:30"));
            ps.setLong(5, 1);

            int rows = ps.executeUpdate();
            return rows > 0 ? "添加成功" : "添加失败";

        } catch (Exception e) {
            e.printStackTrace();
            return "添加异常：" + e.getMessage();
        }
    }



}
