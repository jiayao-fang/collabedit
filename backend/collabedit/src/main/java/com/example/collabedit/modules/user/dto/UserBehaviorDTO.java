package com.example.collabedit.modules.user.dto;

import lombok.Data;

@Data
public class UserBehaviorDTO {
    private Long userId;
    private String username;
    private Integer documentCount; // 文档创建数
    private Integer editCount; // 编辑次数
}