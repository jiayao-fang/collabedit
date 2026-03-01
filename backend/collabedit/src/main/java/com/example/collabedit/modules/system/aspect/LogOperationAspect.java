package com.example.collabedit.modules.system.aspect;

import com.example.collabedit.modules.system.annotation.LogOperation;
import com.example.collabedit.modules.system.entity.SysOperationLog;
import com.example.collabedit.modules.system.service.SysOperationLogService;
import com.example.collabedit.modules.user.entity.User;
import com.example.collabedit.modules.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
public class LogOperationAspect {

    @Resource
    private SysOperationLogService sysOperationLogService;

    @Resource
    private UserService userService;

    // 定义切点，匹配所有带有@LogOperation注解的方法
    @Pointcut("@annotation(com.example.collabedit.modules.system.annotation.LogOperation)")
    public void logPointCut() {
    }

    // 后置通知，在方法执行后记录日志
    @AfterReturning(pointcut = "logPointCut()")
    public void afterReturning(JoinPoint joinPoint) {
        // 获取当前请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        
        // 获取认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }
        
        try {
            // 创建日志对象
            SysOperationLog log = new SysOperationLog();
            
            // 设置用户信息
            String userIdStr = authentication.getName();
            Long userId = Long.valueOf(userIdStr);
            log.setUserId(userId);
            
            // 获取用户名
            User user = userService.getUserById(userId);
            if (user != null) {
                log.setUserName(user.getUsername());
            }
            
            // 获取操作描述和资源
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            LogOperation logOperation = method.getAnnotation(LogOperation.class);
            log.setOperation(logOperation.value());
            
            // 获取资源ID（修复空指针异常）
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0 && !logOperation.resource().isEmpty()) {
                // 处理args[0]为null的情况
                String argStr = args[0] != null ? args[0].toString() : "null";
                log.setResource(logOperation.resource() + ":" + argStr);
            }
            
            // 设置IP地址和时间
            log.setIpAddress(getIpAddress(request));
            log.setOperateTime(LocalDateTime.now());
            
            // 保存日志
            sysOperationLogService.saveLog(log);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            e.printStackTrace();
        }
    }

    // 获取客户端IP地址
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于通过多个代理的情况，第一个IP为客户端真实IP
        if (ip != null && ip.length() > 15 && ip.indexOf(",") > 0) {
            ip = ip.substring(0, ip.indexOf(","));
        }
        return ip;
    }
}