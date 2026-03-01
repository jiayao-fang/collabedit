package com.example.collabedit.modules.notification.controller;

import com.example.collabedit.modules.notification.entity.Notification;
import com.example.collabedit.modules.notification.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    /**
     * 查询当前用户的通知列表
     * @param isRead 0-未读，1-已读，null-全部
     * @param type 通知类型
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Map<String,Object> getMyNotifications(
            @RequestParam(required = false) Integer isRead,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        List<Notification> records = notificationService.getNotifications(userId, isRead, type, pageNum, pageSize);
        Integer total = notificationService.getNotificationCount(userId, isRead, type);    
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    /**
     * 标记单条通知为已读
     */
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public String markAsRead(@PathVariable Long notificationId, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        boolean success = notificationService.markNotificationAsRead(notificationId, userId);
        return success ? "success" : "fail";
    }

    /**
     * 批量标记通知为已读
     */
    @PutMapping("/batch/read")
    @PreAuthorize("isAuthenticated()")
    public String batchMarkAsRead(@RequestBody List<Long> notificationIds, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        int count = notificationService.batchMarkAsRead(notificationIds, userId);
        return String.format("成功标记%d条通知为已读", count);
    }

    /**
     * 查询当前用户未读通知数量
     */
    @GetMapping("/unread/count")
    @PreAuthorize("isAuthenticated()")
    public Integer getUnreadCount(Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        return notificationService.getUnreadCount(userId);
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    public String deleteNotification(@PathVariable Long notificationId, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        boolean success = notificationService.deleteNotification(notificationId, userId);
        return success ? "success" : "fail";
    }
}