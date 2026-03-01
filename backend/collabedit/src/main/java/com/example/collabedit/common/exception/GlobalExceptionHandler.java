package com.example.collabedit.common.exception;

import com.example.collabedit.common.enums.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 处理自定义业务异常（BusinessException）
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusinessException(BusinessException e) {
        // 封装Result对象，并设置对应的HTTP状态码
        Result<?> result = Result.error(e.getCode(), e.getMessage());
        // 根据业务码匹配HTTP状态码
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR; // 默认500
        if (e.getCode() == 400) {
            httpStatus = HttpStatus.BAD_REQUEST;
        } else if (e.getCode() == 403) {
            httpStatus = HttpStatus.FORBIDDEN;
        } else if (e.getCode() == 401) {
            httpStatus = HttpStatus.UNAUTHORIZED;
        }
        return new ResponseEntity<>(result, httpStatus);
    }
    // 处理ResponseStatusException（用于抛403的异常）
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<?>> handleResponseStatusException(ResponseStatusException e) {
        int statusCode = e.getStatusCode().value();
        String message = e.getReason() != null ? e.getReason() : "请求被拒绝";
        Result<?> result = Result.error(statusCode, message);
        // 直接使用异常自带的HTTP状态码
        return new ResponseEntity<>(result, e.getStatusCode());
    }

    // 处理其他所有未捕获的异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleGlobalException(Exception e) {
        Result<?> result = Result.error(500, "系统异常：" + e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}