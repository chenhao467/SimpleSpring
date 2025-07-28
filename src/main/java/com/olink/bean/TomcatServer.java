package com.olink.bean;

import com.olink.common.annotation.Autowired;
import com.olink.common.annotation.Component;
import lombok.Data;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;
import java.io.File;


/*
*功能：
 作者：chenhao
*日期： 2025/4/27 下午3:48
*/
@Component("tomcatServer")
@Data
public class TomcatServer {
    @Autowired
    private DispatcherServlet dispatcherServlet;

    @PostConstruct
    public void start() throws LifecycleException {

            int port = 8080;
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(port);
            tomcat.getConnector();

            String contextPath = "";
            String docBase = new File(".").getAbsolutePath();
            var context = tomcat.addContext(contextPath, docBase);

            tomcat.addServlet(contextPath, "dispatcherServlet", dispatcherServlet);
            context.addServletMappingDecoded("/*", "dispatcherServlet");

            tomcat.start();
            System.out.println("Tomcat started on port: " + port);
    }
}
