package com.chenhao;

import com.chenhao.common.config.AppConfig;
import com.chenhao.common.context.MyApplicationContext;

import java.lang.reflect.InvocationTargetException;

/*
*功能：
 作者：chenhao
*日期： 2025/4/26 下午1:32
*/
public class SpringApplication {
    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        MyApplicationContext context = null;
        //try{
           context = new MyApplicationContext(AppConfig.class);
//        }catch (Exception e) {
//            context.shutdown();
//            throw new RuntimeException(e + "MySpring容器运行出错"+e.getMessage());
//        }finally {
//
//        }
    }
}
