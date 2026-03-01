package com.example.collabedit.modules.notification.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Notification {
    /** 通知ID */
    private Long id;
    
    /** 接收人ID */
    private Long receiverId;
    
    /** 发送人ID */
    private Long senderId;
    
    /** 通知内容 */
    private String content;
    
    /** 关联文档ID */
    private Long docId;
    
    /** 关联评论/回复ID */
    private Long commentId;
    
    /** 通知类型：COMMENT/REPLY/MENTION */
    private String type;
    
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /** 是否已读：0-未读，1-已读 */
    private Integer isRead;
    
    /** 阅读时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

     /** 关联ID（如联系人请求ID、任务ID等） */
    private Long relatedId;
    
    /** 状态（用于联系人请求：0-待处理, 1-已接受, 2-已拒绝） */
    private Integer status;
}