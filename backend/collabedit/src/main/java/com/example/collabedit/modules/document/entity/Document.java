package com.example.collabedit.modules.document.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Lob; 
import lombok.Data;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Data
@Entity
@DynamicUpdate

public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String content; // HTML 内容（富文本）
    private Long authorId;
    private Integer editCount;
    private LocalDateTime createTime; 
    private LocalDateTime updateTime; 
    private Integer isDelete; 
    private Long folderId; 
    private String tagIds; 
    private Integer version; // 版本号（用于并发控制）
    private Integer visibility; // 0-仅自己可见，1-仅编辑者可见，2-所有人可见
     // 新增：存储Yjs序列化后的文档状态（二进制）
    @Lob
    private byte[] contentState; 
    private Long updateBy;
     private Integer isLocked; // 0-未锁定，1-已锁定
    private Long lockedBy; // 锁定者用户ID
    private LocalDateTime lockedAt; // 锁定时间
}
