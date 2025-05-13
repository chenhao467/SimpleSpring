package com.olink.bean;

import com.olink.common.annotation.*;
import com.olink.common.spring.ModelAndView;

import com.olink.common.spring.UserService;
import com.olink.entity.User;

/*
*功能：
 作者：chenhao
*日期： 2025/4/26 下午3:22
*/
@Component("userController")
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping("/hello")
    public String Hello(@Param("name")String name, @Param("age")Integer age){
       return String.format("<h1>name:%s age:%s</h1>", name,age);
    }
    @RequestMapping("/json")
    @ResponseBody
    public User Json(@Param("name")String name, @Param("age")Integer age){
        User user = new User();
        user.setPhone("123");
        user.setPassword("123");
        return user;
    }
    @RequestMapping("/html")
    public ModelAndView html(){
        ModelAndView modelAndView = new ModelAndView("index.html");
        return modelAndView;
    }

    @RequestMapping("/get")
    public String getUserNameById(@Param("id") String id){
       String name  = userService.getUserNameById(id);
       return name;
    }
    @RequestMapping("/insert")
    @ResponseBody
    public String AddUser(@RequestBody User user){
        return userService.AddUserName(user);

    }
}
