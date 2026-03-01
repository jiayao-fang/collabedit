package com.example.collabedit.modules.system.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserRole {
    private Long id;
    private Long userId;
    private Long roleId;
    private LocalDateTime createTime;
}