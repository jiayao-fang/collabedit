package com.example.collabedit.modules.document.handler;

import com.example.collabedit.modules.document.service.RedisDocumentService;
import com.example.collabedit.ot.OTUtils;
import com.example.collabedit.ot.OTUtils.EditOperation;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentWebSocketHandler extends TextWebSocketHandler {

    private final RedisDocumentService redisDocumentService;
    private final ObjectMapper objectMapper; // Spring默认提供的Bean

    // 会话管理
    private final Map<Long, Set<String>> documentSessionIds = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long docId = (Long) session.getAttributes().get("docId");
        Long userId = (Long) session.getAttributes().get("userId");

        // 存储会话ID和会话对象
        documentSessionIds.computeIfAbsent(docId, k -> new CopyOnWriteArraySet<>()).add(session.getId());
        sessionMap.put(session.getId(), session);

        // 初始化文档内容（从Redis/DB加载）
        String content = redisDocumentService.getDocumentContent(docId);
        Long version = redisDocumentService.getDocumentVersion(docId);

        // 发送初始化消息
        Map<String, Object> initMsg = Map.of(
                "type", "INIT",
                "content", content,
                "version", version
        );
        sendMessage(session, initMsg);

        // 广播用户加入
        broadcastJoinLeave(docId, userId, "JOIN");
        log.info("User {} joined document {} (session: {})", userId, docId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long docId = (Long) session.getAttributes().get("docId");
        Long userId = (Long) session.getAttributes().get("userId");

        try {
            // 解析编辑操作（包含位置、内容、版本号）
            EditOperation operation = objectMapper.readValue(message.getPayload(), EditOperation.class);
            operation.setDocId(docId);
            operation.setUserId(userId);

            // 使用OT算法处理并发，避免直接覆盖
            String originalContent = redisDocumentService.getDocumentContent(docId);
            Long currentVersion = redisDocumentService.getDocumentVersion(docId);

            // 版本号校验（防止过期操作覆盖）
            if (operation.getVersion() != currentVersion) {
                // 发送版本冲突提示
                sendMessage(session, Map.of("type", "CONFLICT", "currentVersion", currentVersion));
                return;
            }

            // 应用OT操作生成新内容
            String newContent = OTUtils.applyOperation(originalContent, operation);
            Long newVersion = redisDocumentService.incrementVersionAndSaveContent(docId, newContent);

            // 广播更新消息（包含版本号）
            Map<String, Object> updateMsg = Map.of(
                    "type", "UPDATE",
                    "content", newContent,
                    "version", newVersion,
                    "editor", userId
            );
            broadcastToOthers(docId, session.getId(), updateMsg);

        } catch (Exception e) {
            log.error("Handle text message error", e);
            sendMessage(session, Map.of("type", "ERROR", "message", "Invalid operation"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long docId = (Long) session.getAttributes().get("docId");
        Long userId = (Long) session.getAttributes().get("userId");
        String sessionId = session.getId();

        // 移除会话
        documentSessionIds.getOrDefault(docId, new CopyOnWriteArraySet<>()).remove(sessionId);
        sessionMap.remove(sessionId);

        // 广播用户离开
        broadcastJoinLeave(docId, userId, "LEAVE");

        // 最后一个会话关闭时同步到DB
        if (documentSessionIds.get(docId).isEmpty()) {
            redisDocumentService.syncToDatabase(docId);
            documentSessionIds.remove(docId);
        }

        log.info("User {} left document {} (session: {})", userId, docId, sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Transport error for session {}", session.getId(), exception);
        // 异常时主动清理无效会话
        sessionMap.remove(session.getId());
        documentSessionIds.forEach((docId, sessionIds) -> sessionIds.remove(session.getId()));
    }

    /**
     * 发送消息给指定会话
     */
    private void sendMessage(WebSocketSession session, Object msg) throws IOException {
        if (!session.isOpen()) {
            sessionMap.remove(session.getId());
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
        } catch (IOException e) {
            log.error("Send message failed to session {}", session.getId(), e);
            sessionMap.remove(session.getId());
            documentSessionIds.forEach((docId, sessionIds) -> sessionIds.remove(session.getId()));
        }
    }

    /**
     * 广播消息给文档的其他用户
     */
    private void broadcastToOthers(Long docId, String excludeSessionId, Object msg) {
        Set<String> sessionIds = documentSessionIds.getOrDefault(docId, new CopyOnWriteArraySet<>());
        for (String sessionId : sessionIds) {
            if (!sessionId.equals(excludeSessionId)) {
                WebSocketSession session = sessionMap.get(sessionId);
                if (session != null) {
                    try {
                        sendMessage(session, msg);
                    } catch (IOException e) {
                        log.error("Broadcast failed to session {}", sessionId, e);
                    }
                }
            }
        }
    }

    /**
     * 广播用户加入/离开
     */
    private void broadcastJoinLeave(Long docId, Long userId, String type) {
        Map<String, Object> msg = Map.of(
                "type", type,
                "userId", userId
        );
        broadcastToOthers(docId, null, msg);
    }
}