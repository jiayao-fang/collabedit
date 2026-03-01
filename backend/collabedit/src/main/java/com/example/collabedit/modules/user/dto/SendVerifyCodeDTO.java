package com.example.collabedit.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendVerifyCodeDTO {
    
    // 接收方（手机号或邮箱）
    @NotBlank(message = "接收方不能为空")
    private String receiver;
    
    // 验证码类型：phone 或 email
    @NotBlank(message = "类型不能为空")
    @Pattern(regexp = "^(phone|email)$", message = "类型只能是 phone 或 email")
    private String type;
}

