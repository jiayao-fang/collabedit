package com.example.collabedit.modules.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Permission {
    private Long id;
    private String permName;
    private String permKey;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}