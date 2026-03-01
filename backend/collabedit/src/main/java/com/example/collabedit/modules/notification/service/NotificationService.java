package com.example.collabedit.modules.notification.service;

import com.example.collabedit.modules.notification.entity.Notification;
import java.util.List;

public interface NotificationService {
    /**
     * 发送评论通知（文档作者+@提及用户）
     */
    void sendCommentNotification(Long docId, Long commentId, Long senderId, Long docAuthorId, List<Long> mentionedUserIds);

    /**
     * 发送评论回复通知（原评论作者+@提及用户）
     */
    void sendCommentReplyNotification(Long docId, Long parentCommentId, Long replyId, Long senderId, List<Long> mentionedUserIds);

    /**
     * 查询用户通知（支持分页、按是否已读、类型筛选）
     */
    List<Notification> getNotifications(Long receiverId, Integer isRead, String type, Integer pageNum, Integer pageSize);

    /**
     * 标记单条通知为已读
     */
    boolean markNotificationAsRead(Long notificationId, Long receiverId);

    /**
     * 批量标记通知为已读
     */
    int batchMarkAsRead(List<Long> notificationIds, Long receiverId);

    /**
     * 查询用户未读通知数量
     */
    Integer getUnreadCount(Long receiverId);

    /**
     * 删除通知
     */
    boolean deleteNotification(Long notificationId, Long receiverId);

    Integer getNotificationCount(Long receiverId, Integer isRead, String type);
    
    /**
     * 更新通知状态（用于联系人请求）
     */
    void updateNotificationStatus(Long relatedId, String type, Long receiverId, Integer status);

    /**
 * 发送添加联系人请求通知
 */
void sendContactRequestNotification(Long receiverId, Long senderId, Long requestId, String senderName);

/**
 * 发送联系人请求同意通知
 */
void sendContactApprovedNotification(Long receiverId, Long contactUserId, String contactName);

/**
 * 发送联系人被删除通知
 */
void sendContactDeletedNotification(Long receiverId, Long operatorId, String operatorName);

/**
 * 发送文档编辑邀请通知
 */
void sendEditInvitationNotification(Long receiverId, Long docId, String docTitle, Long inviterId, String inviterName);

/**
 * 发送任务分配通知
 */
void sendTaskAssignmentNotification(Long receiverId, Long taskId, String taskTitle, Long creatorId, String creatorName, Long docId);

/**
 * 发送任务状态变更通知
 */
void sendTaskStatusChangeNotification(Long receiverId, Long taskId, String taskTitle, String newStatus, Long operatorId, String operatorName, Long docId);

/**
 * 发送任务截止日期临近通知（提前1天）
 */
void sendTaskDeadlineApproachingNotification(Long receiverId, Long taskId, String taskTitle, Long docId);

/**
 * 发送任务全部完成通知（给任务创建人）
 */
void sendTaskAllCompletedNotification(Long receiverId, Long taskId, String taskTitle, Long docId);
}