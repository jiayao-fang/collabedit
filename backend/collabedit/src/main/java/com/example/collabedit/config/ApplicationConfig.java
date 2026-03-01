package com.example.collabedit.config;

import com.example.collabedit.modules.document.interceptor.DocumentHandshakeInterceptor;
import com.example.collabedit.modules.document.service.DocumentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling // 启用定时任务
public class ApplicationConfig {
    // 注册WebSocket握手拦截器
    @Bean("documentHandshakeInterceptorConfig")
    public HandshakeInterceptor documentHandshakeInterceptor(DocumentService documentService) {
        return new DocumentHandshakeInterceptor(documentService);
    }
}