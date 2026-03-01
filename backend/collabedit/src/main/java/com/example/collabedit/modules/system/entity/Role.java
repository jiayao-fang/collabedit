package com.example.collabedit.modules.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Role {
    private Long id;
    private String roleName;
    private String roleDesc;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}