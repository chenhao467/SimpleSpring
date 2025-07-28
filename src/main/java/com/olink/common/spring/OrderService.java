package com.olink.common.spring;

import com.olink.common.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public interface OrderService {
    public void test();

    public String getUserNameById(String id);
}
