package com.example.collabedit.modules.user.service;

import com.example.collabedit.modules.user.entity.User;

/**
 * 用户状态服务接口
 */
public interface UserStatusService {
    
    /**
     * 检查用户状态是否有效
     * @param userId 用户ID
     * @return true-有效，false-已禁用
     */
    boolean isUserActive(Long userId);
    
    /**
     * 强制用户下线（禁用后清除其所有会话）
     * @param userId 用户ID
     */
    void forceUserOffline(Long userId);
    
    /**
     * 批量禁用用户
     * @param userIds 用户ID列表
     */
    void batchDisableUsers(java.util.List<Long> userIds);
    
    /**
     * 批量启用用户
     * @param userIds 用户ID列表
     */
    void batchEnableUsers(java.util.List<Long> userIds);
}

