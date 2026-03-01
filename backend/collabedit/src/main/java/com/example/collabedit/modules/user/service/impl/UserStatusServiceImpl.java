package com.example.collabedit.modules.user.service.impl;

import com.example.collabedit.modules.user.entity.User;
import com.example.collabedit.modules.user.mapper.UserMapper;
import com.example.collabedit.modules.user.service.UserStatusService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户状态服务实现
 */
@Slf4j
@Service
public class UserStatusServiceImpl implements UserStatusService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // Redis key 前缀
    private static final String USER_STATUS_PREFIX = "user:status:";
    private static final String USER_TOKEN_PREFIX = "user:token:";

    @Override
    public boolean isUserActive(Long userId) {
        // 1. 先从 Redis 缓存中查询（提高性能）
        String cacheKey = USER_STATUS_PREFIX + userId;
        String cachedStatus = stringRedisTemplate.opsForValue().get(cacheKey);
        
        if (cachedStatus != null) {
            return "1".equals(cachedStatus);
        }

        // 2. 缓存未命中，从数据库查询
        User user = userMapper.findById(userId);
        if (user == null) {
            return false;
        }

        // 3. 更新缓存（5分钟过期）
        stringRedisTemplate.opsForValue().set(
            cacheKey, 
            String.valueOf(user.getStatus()), 
            5, 
            TimeUnit.MINUTES
        );

        return user.getStatus() == 1;
    }

    @Override
    public void forceUserOffline(Long userId) {
        try {
            // 1. 清除用户状态缓存
            String statusKey = USER_STATUS_PREFIX + userId;
            stringRedisTemplate.delete(statusKey);

            // 2. 清除用户的所有 Token（如果使用 Redis 存储 Token）
            String tokenPattern = USER_TOKEN_PREFIX + userId + ":*";
            Set<String> keys = stringRedisTemplate.keys(tokenPattern);
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
                log.info("用户 {} 的 {} 个 Token 已清除", userId, keys.size());
            }

            // 3. 可选：发送 WebSocket 消息通知用户下线
            // webSocketService.sendForceLogout(userId);

            log.info("用户 {} 已被强制下线", userId);
        } catch (Exception e) {
            log.error("强制用户 {} 下线失败", userId, e);
        }
    }

    @Override
    public void batchDisableUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (Long userId : userIds) {
            try {
                // 更新数据库状态
                userMapper.updateStatus(userId, 0);
                
                // 强制下线
                forceUserOffline(userId);
                
                log.info("用户 {} 已被禁用", userId);
            } catch (Exception e) {
                log.error("禁用用户 {} 失败", userId, e);
            }
        }
    }

    @Override
    public void batchEnableUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (Long userId : userIds) {
            try {
                // 更新数据库状态
                userMapper.updateStatus(userId, 1);
                
                // 清除缓存
                String statusKey = USER_STATUS_PREFIX + userId;
                stringRedisTemplate.delete(statusKey);
                
                log.info("用户 {} 已被启用", userId);
            } catch (Exception e) {
                log.error("启用用户 {} 失败", userId, e);
            }
        }
    }
}

