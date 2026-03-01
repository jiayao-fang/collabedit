package com.example.collabedit.modules.user.service.impl;

import com.example.collabedit.modules.user.service.VerifyCodeService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class VerifyCodeServiceImpl implements VerifyCodeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    // 验证码有效期（分钟）
    private static final long CODE_EXPIRE_MINUTES = 5;
    
    // 验证码长度
    private static final int CODE_LENGTH = 6;
    
    // Redis key 前缀
    private static final String REDIS_KEY_PREFIX = "verify_code:";
    
    // 手机号正则
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    // 邮箱正则
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    @Override
    public String sendVerifyCode(String receiver, String type) {
        // 1. 格式校验
        if ("phone".equals(type)) {
            if (!PHONE_PATTERN.matcher(receiver).matches()) {
                throw new RuntimeException("手机号格式不正确");
            }
        } else if ("email".equals(type)) {
            if (!EMAIL_PATTERN.matcher(receiver).matches()) {
                throw new RuntimeException("邮箱格式不正确");
            }
        } else {
            throw new RuntimeException("类型参数错误");
        }
        
        // 2. 检查是否频繁发送（60秒内只能发送一次）
        String redisKey = REDIS_KEY_PREFIX + receiver;
        Long ttl = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        if (ttl != null && ttl > (CODE_EXPIRE_MINUTES * 60 - 60)) {
            throw new RuntimeException("验证码发送过于频繁，请稍后再试");
        }
        
        // 3. 生成6位随机验证码
        String code = generateCode();
        
        // 4. 存储到 Redis（5分钟过期）
        stringRedisTemplate.opsForValue().set(redisKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        // 5. 发送验证码（这里模拟发送，实际项目需要对接短信/邮件服务）
        if ("phone".equals(type)) {
            sendSms(receiver, code);
        } else {
            sendEmail(receiver, code);
        }
        
        return "验证码已发送，请注意查收";
    }

    @Override
    public boolean verifyCode(String receiver, String code) {
        if (receiver == null || code == null) {
            return false;
        }
        
        String redisKey = REDIS_KEY_PREFIX + receiver;
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);
        
        if (storedCode == null) {
            return false;
        }
        
        // 验证成功后删除验证码（防止重复使用）
        if (storedCode.equals(code)) {
            stringRedisTemplate.delete(redisKey);
            return true;
        }
        
        return false;
    }
    
    /**
     * 生成随机验证码
     */
    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
    
    /**
     * 发送短信验证码（模拟）
     */
    private void sendSms(String phone, String code) {
        System.out.println("【模拟发送短信】手机号：" + phone + "，验证码：" + code);

    }
    
    /**
     * 发送邮件验证码（模拟）
     */
    private void sendEmail(String email, String code) {
        System.out.println("【模拟发送邮件】邮箱：" + email + "，验证码：" + code);

    }
}

