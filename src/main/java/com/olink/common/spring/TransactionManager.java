package com.olink.common.spring;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TransactionManager {
    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    public static Connection getConnection() throws SQLException {
        Connection conn = connectionHolder.get();
        if (conn == null) {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/truck", "root", "123456");
            conn.setAutoCommit(false);
            connectionHolder.set(conn);
        }
        return conn;
    }

    public static void commit() throws SQLException {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            conn.commit();
            conn.close();
            connectionHolder.remove();
        }
    }

    public static void rollback() throws SQLException {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            conn.rollback();
            conn.close();
            connectionHolder.remove();
        }
    }
}
