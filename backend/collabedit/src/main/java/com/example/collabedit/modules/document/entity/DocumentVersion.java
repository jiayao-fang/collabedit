package com.example.collabedit.modules.document.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Data;

@Data
@Entity
public class DocumentVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long docId; // 关联的文档ID
    
    private Integer versionNumber; // 版本号
    
    private String title; // 文档标题快照
    
    private String content; // HTML内容快照
    
    @Lob
    private byte[] contentState; // Yjs内容状态快照
    
    private Long createdBy; // 创建此版本的用户ID
    
    private LocalDateTime createTime; // 版本创建时间
    
    private String changeDescription; // 变更描述（可选）
    
    private Long fileSize; // 文件大小（字节）
}
