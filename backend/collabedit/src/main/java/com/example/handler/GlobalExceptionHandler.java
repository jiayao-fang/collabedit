package com.example.handler;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.collabedit.common.exception.BusinessException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // 全局异常处理注解
public class GlobalExceptionHandler {
    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Map<String, Object> handleBusinessException(BusinessException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", e.getCode());
        result.put("message", e.getMessage());
        return result;
    }

    // 处理业务异常（如用户名重复）
    @ExceptionHandler(RuntimeException.class)
    public Map<String, Object> handleRuntimeException(RuntimeException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500); // 错误码
        result.put("message", e.getMessage()); // 错误信息（如"用户名已存在"）
        return result;
    }

    // 处理数据库唯一约束异常（防止漏判）
    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    public Map<String, Object> handleDuplicateKeyException(org.springframework.dao.DuplicateKeyException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        // 解析异常信息，判断是用户名还是邮箱重复
        if (e.getMessage().contains("uk_username")) {
            result.put("message", "用户名已存在");
        } else if (e.getMessage().contains("uk_email")) {
            result.put("message", "邮箱已存在");
        } else {
            result.put("message", "数据重复，请检查");
        }
        return result;
    }
}