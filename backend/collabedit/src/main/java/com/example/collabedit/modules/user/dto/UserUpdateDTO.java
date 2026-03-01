package com.example.collabedit.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDTO {
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20之间")
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    private String signature;
    
}