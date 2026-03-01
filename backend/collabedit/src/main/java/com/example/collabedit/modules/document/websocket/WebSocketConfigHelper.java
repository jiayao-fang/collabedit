package com.example.collabedit.modules.document.websocket;

import com.example.collabedit.modules.document.service.DocumentService;
import com.example.collabedit.security.JwtUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.socket.server.standard.SpringConfigurator;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

import java.util.List;

public class WebSocketConfigHelper extends SpringConfigurator implements ApplicationContextAware {
    
    private static volatile ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        WebSocketConfigHelper.context = applicationContext;
    }

    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }
    
    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, 
                               HandshakeResponse response) {
        super.modifyHandshake(config, request, response);
        
        try {
            // 1. 从请求URL中提取文档ID
            String requestURI = request.getRequestURI().toString();
            String docIdStr = requestURI.substring(requestURI.lastIndexOf("/") + 1);
            Long docId = Long.parseLong(docIdStr);

            // 2. 从请求头中获取JWT token
            List<String> authHeaders = request.getHeaders().get("Authorization");
            String authToken = null;
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String header = authHeaders.get(0);
                if (header.startsWith("Bearer ")) {
                    authToken = header.substring(7);
                }
            }

            // 如果没有token，无法验证用户身份
            if (authToken == null || authToken.isEmpty()) {
                config.getUserProperties().put("authToken", null);
                config.getUserProperties().put("docId", docId);
                return;
            }

            // 3. 获取JWTUtil和DocumentService Bean
            JwtUtil jwtUtil = getBean(JwtUtil.class);
            DocumentService documentService = getBean(DocumentService.class);

            // 4. 解析token获取用户信息
            String userId = jwtUtil.extractUserId(authToken);

            // 5. 验证token是否过期
            if (userId == null || jwtUtil.extractExpiration(authToken).before(new java.util.Date())) {
                // token无效或过期
                config.getUserProperties().put("authToken", null);
                config.getUserProperties().put("docId", docId);
                return;
            }

            // 6. 验证用户是否有访问该文档的权限
            boolean hasPermission = documentService.isEditor(docId, Long.valueOf(userId));

            // 7. 如果有权限，将用户信息存储到会话属性中
            if (hasPermission) {
                // 通过UserService获取完整的用户信息
                com.example.collabedit.modules.user.entity.User loginUser = 
                    new com.example.collabedit.modules.user.entity.User();
                loginUser.setId(Long.valueOf(userId));
                
                // 为简化当前实现，这里先设置ID，用户名将在WebSocket端点中获取
                config.getUserProperties().put("loginUser", loginUser);
                config.getUserProperties().put("docId", docId);
                config.getUserProperties().put("authToken", authToken);
            } else {
                // 权限不足
                config.getUserProperties().put("authToken", null);
                config.getUserProperties().put("docId", docId);
            }
        } catch (NumberFormatException e) {
            // 文档ID解析失败
            throw new RuntimeException("Invalid document ID", e);
        } catch (Exception e) {
            // 其他错误，如无法解析用户信息
            throw new RuntimeException("Authentication error: " + e.getMessage(), e);
        }
    }
}