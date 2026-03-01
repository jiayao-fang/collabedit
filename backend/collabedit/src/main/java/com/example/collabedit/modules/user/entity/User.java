package com.example.collabedit.modules.user.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private Integer status = 1;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private String avatar;//头像URL
    private String signature;//个性签名
}
