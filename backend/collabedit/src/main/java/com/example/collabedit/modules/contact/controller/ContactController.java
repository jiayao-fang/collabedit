// src/main/java/com/example/collabedit/modules/contact/controller/ContactController.java
package com.example.collabedit.modules.contact.controller;

import com.example.collabedit.common.enums.Result;
import com.example.collabedit.modules.contact.entity.ContactRequest;
import com.example.collabedit.modules.contact.service.ContactService;
import com.example.collabedit.modules.system.annotation.LogOperation;
import com.example.collabedit.modules.user.entity.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    @Resource
    private ContactService contactService;

    /**
     * 发送添加联系人请求
     */
    @PostMapping("/request/{receiverId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "发送添加联系人请求", resource = "contact")
    public Result<?> sendContactRequest(@PathVariable Long receiverId, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        contactService.sendContactRequest(userId, receiverId);
        return Result.success("添加请求已发送");
    }

    /**
     * 处理联系人请求
     * @param requestId 请求ID
     * @param status 1-同意，2-拒绝
     */
    @PostMapping("/handle-request/{requestId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "处理联系人请求", resource = "contact")
    public String handleContactRequest(
            @PathVariable Long requestId,
            @RequestParam Integer status,
            Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        contactService.handleContactRequest(requestId, userId, status);
        return status == 1 ? "已同意请求" : "已拒绝请求";
    }

    /**
     * 获取待处理的联系人请求
     */
    @GetMapping("/pending-requests")
    @PreAuthorize("isAuthenticated()")
    public List<ContactRequest> getPendingRequests(Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        return contactService.getPendingRequests(userId);
    }

    /**
     * 获取联系人列表
     */
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public List<User> getContactList(Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        return contactService.getContactList(userId);
    }

    /**
     * 删除联系人
     */
    @DeleteMapping("/{contactUserId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "删除联系人", resource = "contact")
    public Result<?> deleteContact(@PathVariable Long contactUserId, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        contactService.deleteContact(userId, contactUserId);
        return Result.success("删除成功");
    }

    /**
     * 更新联系人备注
     */
    @PutMapping("/remark/{contactUserId}")
    @PreAuthorize("isAuthenticated()")
    public String updateContactRemark(
            @PathVariable Long contactUserId,
            @RequestParam String remark,
            Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        contactService.updateContactRemark(userId, contactUserId, remark);
        return "更新成功";
    }

    /**
     * 搜索用户
     */
    @GetMapping("/searchUser")
    @PreAuthorize("isAuthenticated()")
    public List<User> searchUsers(@RequestParam String keyword, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        return contactService.searchUsersByPhoneOrEmail(userId, keyword);
    }

    /**
     * 获取可邀请编辑的联系人
     */
    @GetMapping("/invitable/{docId}")
    @PreAuthorize("isAuthenticated()")
    public List<User> getInvitableContacts(@PathVariable Long docId, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        return contactService.getInvitableContacts(userId, docId);
    }
}