package com.olink;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.olink.common.config.AppConfig;
import com.olink.common.context.MyApplicationContext;
import com.olink.common.spring.TransactionManager;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

/*
*功能：
 作者：chenhao
*日期： 2025/4/26 下午1:32
*/
public class Test {
    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        MyApplicationContext context = null;
        try{
           context = new MyApplicationContext(AppConfig.class);
        }catch (Exception e) {
            throw new RuntimeException(e + "MySpring容器运行出错");
        }finally {
            if(ObjectUtils.isNotEmpty(context)){
                context.shutdown();
            }
        }
    }
}
