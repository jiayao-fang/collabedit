package com.example.collabedit.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterDTO {
    // 用户名非空且长度2-20
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20之间")
    private String username;

    // 密码非空且长度6-20
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String password;

    // 邮箱格式校验（可选）
    @Email(message = "邮箱格式不正确")
    private String email;
    
    // 手机号格式校验（可选）
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    // 验证码（必填）
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码必须为6位")
    private String verifyCode;
    
    // 注册类型：phone 或 email
    @NotBlank(message = "注册类型不能为空")
    @Pattern(regexp = "^(phone|email)$", message = "注册类型只能是 phone 或 email")
    private String registerType;
}