package com.olink.common.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/*
*功能：
 作者：chenhao
*日期： 2025/4/30 上午10:41
*/
public class ConnectionManager {

    private static ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    private static Properties properties = new Properties();

    static {
        try {
           // InputStream input = ClassLoader.getSystemClassLoader().getResourceAsStream("db.properties");
            InputStream input = ConnectionManager.class.getClassLoader().getResourceAsStream("application.properties");

            properties.load(input);
            Class.forName(properties.getProperty("db.driver"));
        } catch (Exception e) {
            throw new RuntimeException("数据库配置读取失败", e);
        }
    }

    public static Connection getConnection() {
        Connection conn = connectionHolder.get();
        if (conn == null) {
            try {
                conn = DriverManager.getConnection(
                        properties.getProperty("db.url"),
                        properties.getProperty("db.username"),
                        properties.getProperty("db.password")
                );
                connectionHolder.set(conn);
            } catch (SQLException e) {
                throw new RuntimeException("获取数据库连接失败", e);
            }
        }
        return conn;
    }

    public static void closeConnection() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                connectionHolder.remove();
            }
        }
    }

    public static void begin() throws SQLException {
        Connection conn = getConnection();
        conn.setAutoCommit(false);
    }

    public static void commit() throws SQLException {
        Connection conn = getConnection();
        conn.commit();
    }

    public static void rollback() {
        try {
            Connection conn = getConnection();
            conn.rollback();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
