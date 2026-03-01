package com.example.collabedit.config;

import com.example.collabedit.modules.document.handler.DocumentWebSocketHandler;
import com.example.collabedit.modules.document.interceptor.DocumentHandshakeInterceptor;
import io.micrometer.common.lang.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Slf4j
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DocumentWebSocketHandler documentWebSocketHandler;
    private final DocumentHandshakeInterceptor documentHandshakeInterceptor;

    public WebSocketConfig(DocumentWebSocketHandler documentWebSocketHandler, 
                          DocumentHandshakeInterceptor documentHandshakeInterceptor) {
        this.documentWebSocketHandler = documentWebSocketHandler;
        this.documentHandshakeInterceptor = documentHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        // 注册文档编辑的WebSocket端点
        registry.addHandler(documentWebSocketHandler, "/ws/doc/{docId}")
                .addInterceptors(documentHandshakeInterceptor)
                .setAllowedOriginPatterns("*"); 
    }
}