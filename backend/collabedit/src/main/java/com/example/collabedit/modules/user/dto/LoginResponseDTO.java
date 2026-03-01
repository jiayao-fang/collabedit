package com.example.collabedit.modules.user.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {
    // JWT令牌
    private String token;
    // 用户基本信息（不含密码）
    private UserInfoDTO userInfo;

    // 内部类：用户信息（脱敏）
    @Data
    public static class UserInfoDTO {
        private Long id;
        private String username;
        private String email;
        private String role;
        private Integer status;
    }
}