package com.olink.entity;

import lombok.Data;

import java.time.LocalDateTime;

/*
*功能：
 作者：chenhao
*日期： 2025/5/12 下午9:09
*/
@Data
public class User {
    private  Long id;
    private  String phone;
    private  String password;
    private LocalDateTime createTime;
    private Long enabled;
}
