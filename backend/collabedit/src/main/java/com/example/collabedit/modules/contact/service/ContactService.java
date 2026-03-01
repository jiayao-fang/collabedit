// src/main/java/com/example/collabedit/modules/contact/service/ContactService.java
package com.example.collabedit.modules.contact.service;

import com.example.collabedit.modules.contact.entity.ContactRequest;
import com.example.collabedit.modules.user.entity.User;
import java.util.List;

public interface ContactService {
    // 联系人请求相关
    void sendContactRequest(Long senderId, Long receiverId);
    void handleContactRequest(Long requestId, Long userId, Integer status);
    List<ContactRequest> getPendingRequests(Long userId);
    
    // 联系人管理相关
    void addContact(Long userId, Long contactUserId, String remark);
    void deleteContact(Long userId, Long contactUserId);
    List<User> getContactList(Long userId);
    void updateContactRemark(Long userId, Long contactUserId, String remark);
    
    // 查找功能
    List<User> searchUsersByPhoneOrEmail(Long userId, String keyword);
    
    // 获取可邀请编辑的联系人（排除已有编辑者）
    List<User> getInvitableContacts(Long userId, Long docId);
}