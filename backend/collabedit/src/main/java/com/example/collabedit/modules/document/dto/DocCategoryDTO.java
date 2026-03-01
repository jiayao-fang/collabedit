// src/main/java/com/example/collabedit/modules/document/dto/DocCategoryDTO.java
package com.example.collabedit.modules.document.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档分类展示DTO（前端需要的字段：标题、创建时间、编辑者名称）
 */
@Data
public class DocCategoryDTO {
    private Long id; // 文档ID
    private String title; // 文档标题
    private LocalDateTime createTime; // 创建时间
    private String editorName; // 编辑者（作者）名称
}