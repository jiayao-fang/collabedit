package com.example.collabedit.modules.document.entity;

import java.time.LocalDateTime;
import lombok.Data;
@Data
public class DocumentEditor {
    private Long id;
    private Long documentId;
    private Long userId;
    private LocalDateTime createTime;
}