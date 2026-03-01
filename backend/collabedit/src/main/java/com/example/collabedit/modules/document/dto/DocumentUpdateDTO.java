package com.example.collabedit.modules.document.dto;

import lombok.Data;

@Data
public class DocumentUpdateDTO {
    private Long id; // 文档ID
    private String title; // 标题
    private String coverImage; // 封面图
    private Long folderId; // 文件夹ID（可为null）
    private String tagIds; // 标签ID，逗号分隔
    private Integer visibility; // 权限
    private Integer version; // 乐观锁版本号
    private String content; // 原有HTML内容（兼容）
    private String contentState; // Yjs序列化内容
}
