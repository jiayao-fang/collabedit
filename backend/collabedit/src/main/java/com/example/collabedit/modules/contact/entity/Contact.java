// src/main/java/com/example/collabedit/modules/contact/entity/Contact.java
package com.example.collabedit.modules.contact.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;         // 当前用户ID
    private Long contactUserId;  // 联系人用户ID
    private LocalDateTime createTime;  // 添加时间
    private String remark;       // 联系人备注
}