package com.example.collabedit.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDTO {
    
    // 接收方（手机号或邮箱）
    @NotBlank(message = "手机号或邮箱不能为空")
    private String receiver;
    
    // 验证码
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码必须为6位")
    private String verifyCode;
    
    // 新密码
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String newPassword;
    
    // 重置类型：phone 或 email
    @NotBlank(message = "重置类型不能为空")
    @Pattern(regexp = "^(phone|email)$", message = "重置类型只能是 phone 或 email")
    private String resetType;
}

