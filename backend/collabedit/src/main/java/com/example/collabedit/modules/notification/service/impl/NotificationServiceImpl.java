package com.example.collabedit.modules.notification.service.impl;

import com.example.collabedit.modules.notification.entity.Notification;
import com.example.collabedit.modules.notification.mapper.NotificationMapper;
import com.example.collabedit.modules.notification.service.NotificationService;
import com.example.collabedit.modules.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private UserMapper userMapper; // 用于查询用户名，拼接通知内容

    // 分页偏移量计算
    private int getOffset(Integer pageNum, Integer pageSize) {
        return (pageNum - 1) * pageSize;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendCommentNotification(Long docId, Long commentId, Long senderId, Long docAuthorId, List<Long> mentionedUserIds) {
        // 1. 获取发送人用户名
        String senderName = userMapper.findUsernameById(senderId);
        if (senderName == null) {
            senderName = "未知用户";
        }

        // 2. 向文档作者发送「新评论」通知
        Notification authorNotification = new Notification();
        authorNotification.setReceiverId(docAuthorId);
        authorNotification.setSenderId(senderId);
        authorNotification.setContent(String.format("用户「%s」在您的文档中发表了新评论", senderName));
        authorNotification.setDocId(docId);
        authorNotification.setCommentId(commentId);
        authorNotification.setType("COMMENT");
        authorNotification.setStatus(null); // 非联系人请求通知，status为null
        notificationMapper.insert(authorNotification);

        // 3. 向被@提及的用户发送「被@」通知
        if (mentionedUserIds != null && !mentionedUserIds.isEmpty()) {
            for (Long userId : mentionedUserIds) {
                // 排除自己和文档作者（避免重复通知）
                if (userId.equals(senderId) || userId.equals(docAuthorId)) {
                    continue;
                }
                Notification mentionNotification = new Notification();
                mentionNotification.setReceiverId(userId);
                mentionNotification.setSenderId(senderId);
                mentionNotification.setContent(String.format("用户「%s」在评论中@了您", senderName));
                mentionNotification.setDocId(docId);
                mentionNotification.setCommentId(commentId);
                mentionNotification.setType("MENTION");
                mentionNotification.setStatus(null); // 非联系人请求通知，status为null
                notificationMapper.insert(mentionNotification);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendCommentReplyNotification(Long docId, Long parentCommentId, Long replyId, Long senderId, List<Long> mentionedUserIds) {
        // 1. 获取发送人用户名
        String senderName = userMapper.findUsernameById(senderId);
        if (senderName == null) {
            senderName = "未知用户";
        }

        // 2. 整理接收人列表：原评论作者 + 被@用户（去重）
        List<Long> receiverIds = new ArrayList<>();
        if (mentionedUserIds != null) {
            receiverIds.addAll(mentionedUserIds);
        }
        // 去重 + 排除自己
        receiverIds = receiverIds.stream().distinct().filter(id -> !id.equals(senderId)).toList();

        // 3. 批量发送回复通知
        for (Long receiverId : receiverIds) {
            Notification replyNotification = new Notification();
            replyNotification.setReceiverId(receiverId);
            replyNotification.setSenderId(senderId);
            replyNotification.setContent(String.format("用户「%s」回复了您的评论", senderName));
            replyNotification.setDocId(docId);
            replyNotification.setCommentId(replyId); // 关联回复ID
            replyNotification.setType("REPLY");
            replyNotification.setStatus(null); // 非联系人请求通知，status为null
            notificationMapper.insert(replyNotification);
        }
    }

    @Override
    public List<Notification> getNotifications(Long receiverId, Integer isRead, String type, Integer pageNum, Integer pageSize) {
        int offset = getOffset(pageNum, pageSize);
        return notificationMapper.selectByReceiverId(receiverId, isRead, type, offset, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markNotificationAsRead(Long notificationId, Long receiverId) {
        int affected = notificationMapper.markAsRead(notificationId, receiverId, LocalDateTime.now());
        return affected > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchMarkAsRead(List<Long> notificationIds, Long receiverId) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return 0;
        }
        return notificationMapper.batchMarkAsRead(notificationIds, receiverId, LocalDateTime.now());
    }

    @Override
    public Integer getUnreadCount(Long receiverId) {
        return notificationMapper.countUnread(receiverId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNotification(Long notificationId, Long receiverId) {
        int affected = notificationMapper.delete(notificationId, receiverId);
        return affected > 0;
    }

    @Override
public Integer getNotificationCount(Long receiverId, Integer isRead, String type) {
    return notificationMapper.countByReceiverId(receiverId, isRead, type);
}

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotificationStatus(Long relatedId, String type, Long receiverId, Integer status) {
        notificationMapper.updateStatusByRelatedId(relatedId, type, receiverId, status);
    }

    @Override
@Transactional(rollbackFor = Exception.class)
public void sendContactRequestNotification(Long receiverId, Long senderId, Long requestId, String senderName) {
    Notification notification = new Notification();
    notification.setReceiverId(receiverId);
    notification.setSenderId(senderId);
    notification.setContent(String.format("用户「%s」请求添加您为联系人", senderName));
    notification.setType("CONTACT_REQUEST");
    notification.setRelatedId(requestId); // 关联请求ID
    notification.setStatus(0); // 0-待处理
    notificationMapper.insert(notification);
}

@Override
@Transactional(rollbackFor = Exception.class)
public void sendContactApprovedNotification(Long receiverId, Long contactUserId, String contactName) {
    Notification notification = new Notification();
    notification.setReceiverId(receiverId);
    notification.setSenderId(contactUserId);
    notification.setContent(String.format("用户「%s」已同意您的联系人请求", contactName));
    notification.setType("CONTACT_APPROVED");
    notification.setStatus(null); // 非联系人请求通知，status为null
    notificationMapper.insert(notification);
}

@Override
@Transactional(rollbackFor = Exception.class)
public void sendContactDeletedNotification(Long receiverId, Long operatorId, String operatorName) {
    Notification notification = new Notification();
    notification.setReceiverId(receiverId);
    notification.setSenderId(operatorId);
    notification.setContent(String.format("用户「%s」已将您从联系人中删除", operatorName));
    notification.setType("CONTACT_DELETED");
    notification.setStatus(null); // 非联系人请求通知，status为null
    notificationMapper.insert(notification);
}

@Override
@Transactional(rollbackFor = Exception.class)
public void sendEditInvitationNotification(Long receiverId, Long docId, String docTitle, Long inviterId, String inviterName) {
    Notification notification = new Notification();
    notification.setReceiverId(receiverId);
    notification.setSenderId(inviterId);
    notification.setContent(String.format("用户「%s」邀请您编辑文档《%s》", inviterName, docTitle));
    notification.setType("EDIT_INVITATION");
    notification.setDocId(docId);
    notification.setStatus(null); // 非联系人请求通知，status为null
    notificationMapper.insert(notification);
}

@Override
@Transactional(rollbackFor = Exception.class)
public void sendTaskAssignmentNotification(Long receiverId, Long taskId, String taskTitle, Long creatorId, String creatorName, Long docId) {
    Notification notification = new Notification();
    notification.setReceiverId(receiverId);
    notification.setSenderId(creatorId);
    notification.setContent(String.format("用户「%s」给您分配了任务《%s》", creatorName, taskTitle));
    notification.setType("TASK_ASSIGNMENT");
    notification.setDocId(docId);
    notification.setRelatedId(taskId);
    notification.setStatus(null); // 非联系人请求通知，status为null
    notificationMapper.insert(notification);
}

@Override
@Transactional(rollbackFor = Exception.class)
public void sendTaskStatusChangeNotification(Long receiverId, Long taskId, String taskTitle, String newStatus, Long operatorId, String operatorName, Long docId) {
    String statusText = "待处理";
    if ("IN_PROGRESS".equals(newStatus)) {
        statusText = "处理中";
    } else if ("COMPLETED".equals(newStatus)) {
        statusText = "已完成";
    }
    
    Notification notification = new Notification();
    notification.setReceiverId(receiverId);
    notification.setSenderId(operatorId);
    notification.setContent(String.format("用户「%s」将任务《%s》的状态更新为「%s」", operatorName, taskTitle, statusText));
    notification.setType("TASK_STATUS_CHANGE");
    notification.setDocId(docId);
    notification.setRelatedId(taskId);
    notification.setStatus(null); // 非联系人请求通知，status为null
    notificationMapper.insert(notification);
}

@Override
@Transactional(rollbackFor = Exception.class)
public void sendTaskDeadlineApproachingNotification(Long receiverId, Long taskId, String taskTitle, Long docId) {
    Notification notification = new Notification();
    notification.setReceiverId(receiverId);
    notification.setSenderId(null); // 系统通知，无发送人
    notification.setContent(String.format("任务《%s》的截止日期即将到来（1天内）", taskTitle));
    notification.setType("TASK_DEADLINE_APPROACHING");
    notification.setDocId(docId);
    notification.setRelatedId(taskId);
    notification.setStatus(null); // 非联系人请求通知，status为null
    notificationMapper.insert(notification);
}

@Override
@Transactional(rollbackFor = Exception.class)
public void sendTaskAllCompletedNotification(Long receiverId, Long taskId, String taskTitle, Long docId) {
    Notification notification = new Notification();
    notification.setReceiverId(receiverId);
    notification.setSenderId(null); // 系统通知，无发送人
    notification.setContent(String.format("任务《%s》已被所有被分配人完成", taskTitle));
    notification.setType("TASK_ALL_COMPLETED");
    notification.setDocId(docId);
    notification.setRelatedId(taskId);
    notification.setStatus(null);
    notificationMapper.insert(notification);
}
}