// src/main/java/com/example/collabedit/modules/document/entity/Tag.java
package com.example.collabedit.modules.document.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Tag {
    private Long id;
    private String tagName;
    private LocalDateTime createTime;
}