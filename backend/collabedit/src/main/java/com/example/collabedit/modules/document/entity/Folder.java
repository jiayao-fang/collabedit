package com.example.collabedit.modules.document.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Folder {
    private Long id;
    private String folderName;
    private Long parentId; // 父文件夹ID，null表示根目录
    private Long creatorId; // 创建者ID
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}