// src/main/java/com/example/collabedit/modules/contact/entity/ContactRequest.java
package com.example.collabedit.modules.contact.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class ContactRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long senderId;       // 发送请求的用户ID
    private Long receiverId;     // 接收请求的用户ID
    private LocalDateTime sendTime;  // 发送时间
    private Integer status;      // 0-待处理, 1-已同意, 2-已拒绝
}