package com.example.collabedit.modules.user.service;

public interface VerifyCodeService {
    
    /**
     * 发送验证码
     * @param receiver 接收方（手机号或邮箱）
     * @param type 类型（phone 或 email）
     * @return 成功消息
     */
    String sendVerifyCode(String receiver, String type);
    
    /**
     * 验证验证码
     * @param receiver 接收方
     * @param code 验证码
     * @return 是否验证成功
     */
    boolean verifyCode(String receiver, String code);
}

