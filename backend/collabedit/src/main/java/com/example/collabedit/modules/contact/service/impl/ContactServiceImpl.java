// src/main/java/com/example/collabedit/modules/contact/service/impl/ContactServiceImpl.java
package com.example.collabedit.modules.contact.service.impl;

import com.example.collabedit.modules.contact.entity.Contact;
import com.example.collabedit.modules.contact.entity.ContactRequest;
import com.example.collabedit.modules.contact.mapper.ContactMapper;
import com.example.collabedit.modules.contact.mapper.ContactRequestMapper;
import com.example.collabedit.modules.contact.service.ContactService;
import com.example.collabedit.modules.document.service.DocumentService;
import com.example.collabedit.modules.notification.service.NotificationService;
import com.example.collabedit.modules.user.entity.User;
import com.example.collabedit.modules.user.mapper.UserMapper;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContactServiceImpl implements ContactService {

    @Resource
    private ContactMapper contactMapper;
    
    @Resource
    private ContactRequestMapper contactRequestMapper;
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private DocumentService documentService;
    
    @Resource
    private NotificationService notificationService;

    @Override
    @Transactional
    public void sendContactRequest(Long senderId, Long receiverId) {
         // 检查是否为自己
    if (senderId.equals(receiverId)) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "不能添加自己为联系人");
    }

    // 检查是否已经是联系人
    if (contactMapper.countByUserAndContact(senderId, receiverId) > 0) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "该用户已经是您的联系人");
    }

    // 检查是否有未处理的请求
    if (contactRequestMapper.countPendingRequest(senderId, receiverId) > 0) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "已发送添加请求，请等待对方回应");
    }
        
        // 创建请求
        ContactRequest request = new ContactRequest();
        request.setSenderId(senderId);
        request.setReceiverId(receiverId);
        contactRequestMapper.insert(request);
        
        // 发送通知
        String senderName = userMapper.findUsernameById(senderId);
        notificationService.sendContactRequestNotification(
            receiverId, senderId, request.getId(), senderName
        );
    }

    @Override
    @Transactional
    public void handleContactRequest(Long requestId, Long userId, Integer status) {
        ContactRequest request = contactRequestMapper.findById(requestId);
        if (request == null) {
            throw new RuntimeException("请求不存在");
        }
        
        // 验证是否是接收者
        if (!request.getReceiverId().equals(userId)) {
            throw new RuntimeException("无权处理此请求");
        }
        
        // 更新请求状态
        contactRequestMapper.updateStatus(requestId, status);
        
        // 更新通知状态
        notificationService.updateNotificationStatus(
            requestId, "CONTACT_REQUEST", userId, status
        );
        
        // 如果同意，则添加双向联系人
        if (status == 1) {
            // 添加发送者到接收者的联系人列表
            Contact receiverContact = new Contact();
            receiverContact.setUserId(request.getReceiverId());
            receiverContact.setContactUserId(request.getSenderId());
            contactMapper.insert(receiverContact);
            
            // 添加接收者到发送者的联系人列表
            Contact senderContact = new Contact();
            senderContact.setUserId(request.getSenderId());
            senderContact.setContactUserId(request.getReceiverId());
            contactMapper.insert(senderContact);
            
            // 发送通知告知请求已同意
            String receiverName = userMapper.findUsernameById(request.getReceiverId());
            notificationService.sendContactApprovedNotification(
                request.getSenderId(), request.getReceiverId(), receiverName
            );
        }
    }

    @Override
    public List<ContactRequest> getPendingRequests(Long userId) {
        return contactRequestMapper.findPendingByUserId(userId);
    }

    @Override
    public void addContact(Long userId, Long contactUserId, String remark) {
        // 实际项目中通常通过请求流程添加联系人，此方法可用于特殊情况
        Contact contact = new Contact();
        contact.setUserId(userId);
        contact.setContactUserId(contactUserId);
        contact.setRemark(remark);
        contactMapper.insert(contact);
    }

    @Override
    @Transactional
    public void deleteContact(Long userId, Long contactUserId) {
        // 删除双向联系
        contactMapper.delete(userId, contactUserId);
        contactMapper.delete(contactUserId, userId);
        
        // 发送通知告知对方已被删除
        String userName = userMapper.findUsernameById(userId);
        notificationService.sendContactDeletedNotification(contactUserId, userId, userName);
    }

    @Override
    public List<User> getContactList(Long userId) {
        List<Contact> contacts = contactMapper.findByUserId(userId);
        return contacts.stream()
                .map(contact -> userMapper.findById(contact.getContactUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public void updateContactRemark(Long userId, Long contactUserId, String remark) {
        contactMapper.updateRemark(userId, contactUserId, remark);
    }

    @Override
    public List<User> searchUsersByPhoneOrEmail(Long userId, String keyword) {
        return userMapper.findByPhoneOrEmail(keyword, userId);
    }

    @Override
    public List<User> getInvitableContacts(Long userId, Long docId) {
        // 获取当前用户的所有联系人
        List<User> contacts = getContactList(userId);
        
        // 获取文档的现有编辑者
        List<Long> editors = documentService.getEditors(docId);
        Long authorId = documentService.getById(docId).getAuthorId();
        
        // 排除作者和已有编辑者
        return contacts.stream()
                .filter(contact -> !editors.contains(contact.getId()) && !contact.getId().equals(authorId))
                .collect(Collectors.toList());
    }
}