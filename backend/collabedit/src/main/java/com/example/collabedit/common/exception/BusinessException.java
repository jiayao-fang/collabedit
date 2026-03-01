package com.example.collabedit.common.exception; 

import lombok.Data;
import lombok.EqualsAndHashCode;
 //自定义业务异常类
@Data
@EqualsAndHashCode(callSuper=false)
public class BusinessException extends RuntimeException {
    private int code; // 错误码
    private String message; // 错误信息
    // 构造方法：接收错误码和错误信息
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    public int getCode() {
        return code;
    }
    @Override
    public String getMessage() {
        return message;
    }
}