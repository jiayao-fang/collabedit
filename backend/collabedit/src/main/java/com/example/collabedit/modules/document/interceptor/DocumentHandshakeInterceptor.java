package com.example.collabedit.modules.document.interceptor;

import com.example.collabedit.modules.document.service.DocumentService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;


import java.util.Map;

@Component
@RequiredArgsConstructor

public class DocumentHandshakeInterceptor implements HandshakeInterceptor {
    private final DocumentService documentService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
          // 1. 解析文档ID
        String path = request.getURI().getPath();
        String docIdStr = path.substring(path.lastIndexOf("/") + 1);
        Long docId;
        try {
            docId = Long.parseLong(docIdStr);
        } catch (NumberFormatException e) {
            response.setStatusCode(org.springframework.http.HttpStatus.BAD_REQUEST);
            return false;
        }

        // 2. 解析当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 用户ID转换增加异常处理
        Long userId;
        try {
            userId = Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            response.setStatusCode(org.springframework.http.HttpStatus.BAD_REQUEST);
            return false;
        }

        //3. 权限验证（校验管理员权限）
        boolean hasPermission;
        try {
            // 文档作者/被授权编辑者/管理员可访问
            hasPermission = documentService.isEditor(docId, userId) 
                    || isAdmin(authentication);
        } catch (Exception e) {
            // 文档不存在
            if (e.getMessage().contains("Document not found")) {
                response.setStatusCode(org.springframework.http.HttpStatus.NOT_FOUND);
            } else {
                response.setStatusCode(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return false;
        }

        if (!hasPermission) {
            response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
            return false;
        }

        // 4. 存储文档ID和用户ID到属性中
        attributes.put("docId", docId);
        attributes.put("userId", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                               WebSocketHandler wsHandler, Exception exception) {}

    /**
     * 判断管理员权限（从Authorities中校验）
     */
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority));
    }
}