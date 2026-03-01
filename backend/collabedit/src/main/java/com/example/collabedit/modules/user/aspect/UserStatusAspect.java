package com.example.collabedit.modules.user.aspect;

import com.example.collabedit.modules.user.service.UserStatusService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 用户状态变更切面
 * 自动处理用户禁用后的下线操作
 */
@Slf4j
@Aspect
@Component
public class UserStatusAspect {

    @Resource
    private UserStatusService userStatusService;

    /**
     * 用户状态更新后的处理
     */
    @AfterReturning(
            pointcut = "execution(* com.example.collabedit.modules.user.service.UserService.updateStatus(..))",
            returning = "result"
    )
    public void afterStatusUpdate(Object result) {
        try {
            if (result != null) {
                // 使用反射获取用户信息
                java.lang.reflect.Field idField = result.getClass().getDeclaredField("id");
                java.lang.reflect.Field statusField = result.getClass().getDeclaredField("status");
                
                idField.setAccessible(true);
                statusField.setAccessible(true);
                
                Long userId = (Long) idField.get(result);
                Integer status = (Integer) statusField.get(result);
                
                if (status == 0) {
                    log.info("检测到用户 {} 被禁用，执行下线操作", userId);
                    userStatusService.forceUserOffline(userId);
                }
            }
        } catch (Exception e) {
            log.error("用户状态变更后处理失败", e);
        }
    }
}

