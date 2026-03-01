package com.example.collabedit.modules.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysOperationLog {
    private Long id;
    private Long userId;
    private String operation;
    private LocalDateTime operateTime;
    private String ipAddress;
    private String resource;
    private String userName;
}