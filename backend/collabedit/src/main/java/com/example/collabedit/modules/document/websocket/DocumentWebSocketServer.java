package com.example.collabedit.modules.document.websocket;


import org.springframework.stereotype.Component;

import com.example.collabedit.modules.user.entity.User;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint(value = "/ws/doc/{docId}", configurator = WebSocketConfigHelper.class)
@Component
public class DocumentWebSocketServer {
    // 1. 文档房间管理：key=docId，value=该文档的所有连接会话（线程安全）
    private static final Map<String, Set<Session>> DOC_ROOM_MAP = new ConcurrentHashMap<>();
    // 2. 在线编辑者管理：key=docId，value=该文档的在线用户集合（线程安全）
    private static final Map<String, Set<User>> ONLINE_EDITORS_MAP = new ConcurrentHashMap<>();
    
    // 当前会话对应的文档ID
    private String currentDocId;
    // 当前会话对应的用户信息
    private User currentUser;

    /**
     * 连接建立时触发（解决：在线编辑者记录）
     * 功能：1. 加入文档房间 2. 记录在线用户 3. 广播在线编辑者列表更新
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("docId") String docId) {
        this.currentDocId = docId;
        // 从握手拦截器中获取用户信息
        this.currentUser = (User) session.getUserProperties().get("loginUser");
        
        // 初始化文档房间（不存在则创建）
        DOC_ROOM_MAP.putIfAbsent(docId, new CopyOnWriteArraySet<>());
        DOC_ROOM_MAP.get(docId).add(session);
        
        // 初始化在线编辑者列表
        ONLINE_EDITORS_MAP.putIfAbsent(docId, new CopyOnWriteArraySet<>());
        ONLINE_EDITORS_MAP.get(docId).add(currentUser);
        
        // 广播在线编辑者列表更新
        broadcastOnlineEditors(docId);
    }

    /**
     * 接收前端消息时触发（解决：Yjs变更消息广播）
     * 功能：1. 接收前端Yjs变更数据 2. 广播给该文档其他所有编辑者（排除发送者）
     */
    @OnMessage
    public void onMessage(String message, Session session, @PathParam("docId") String docId) {
        // 校验文档房间是否存在
        if (!DOC_ROOM_MAP.containsKey(docId)) {
            return;
        }
        // 遍历该文档所有会话，广播消息（排除发送者自己）
        for (Session targetSession : DOC_ROOM_MAP.get(docId)) {
            if (!targetSession.equals(session) && targetSession.isOpen()) {
                try {
                    targetSession.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 连接关闭时触发（解决：在线编辑者移除）
     * 功能：1. 移出文档房间 2. 删除在线用户记录 3. 广播在线编辑者列表更新
     */
    @OnClose
    public void onClose(Session session) {
        // 移除当前会话 From 文档房间
        if (DOC_ROOM_MAP.containsKey(currentDocId)) {
            DOC_ROOM_MAP.get(currentDocId).remove(session);
            // 房间无连接时，清理内存
            if (DOC_ROOM_MAP.get(currentDocId).isEmpty()) {
                DOC_ROOM_MAP.remove(currentDocId);
            }
        }
        
        // 移除当前用户 From 在线编辑者列表
        if (ONLINE_EDITORS_MAP.containsKey(currentDocId)) {
            ONLINE_EDITORS_MAP.get(currentDocId).remove(currentUser);
            // 无在线用户时，清理内存
            if (ONLINE_EDITORS_MAP.get(currentDocId).isEmpty()) {
                ONLINE_EDITORS_MAP.remove(currentDocId);
            }
        }
        
        // 广播在线编辑者列表更新
        broadcastOnlineEditors(currentDocId);
    }

    /**
     * 连接错误时触发
     */
    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
        // 错误时触发关闭逻辑，确保资源清理
        onClose(session);
    }

    /**
     * 广播在线编辑者列表
     */
    private void broadcastOnlineEditors(String docId) {
        if (!ONLINE_EDITORS_MAP.containsKey(docId) || !DOC_ROOM_MAP.containsKey(docId)) {
            return;
        }
        // 构建广播消息
        StringBuilder broadcastMsg = new StringBuilder();
        broadcastMsg.append("{")
                .append("\"type\":\"online_editors\",")
                .append("\"docId\":\"").append(docId).append("\",")
                .append("\"onlineEditors\":[");
        for (User user : ONLINE_EDITORS_MAP.get(docId)) {
            broadcastMsg.append("{")
                    .append("\"userId\":\"").append(user.getId()).append("\",")
                    .append("\"userName\":\"").append(user.getUsername()).append("\"")
                    .append("},");
        }
        // 移除最后一个逗号
        if (broadcastMsg.charAt(broadcastMsg.length() - 1) == ',') {
            broadcastMsg.deleteCharAt(broadcastMsg.length() - 1);
        }
        broadcastMsg.append("]}");
        
        // 发送给该文档所有在线用户
        for (Session session : DOC_ROOM_MAP.get(docId)) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(broadcastMsg.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
     /**
     * 获取指定文档的在线编辑者列表
     */
    public static Set<User> getOnlineEditors(String docId) {
        Set<User> editors = ONLINE_EDITORS_MAP.get(docId);
        return editors != null ? editors : new HashSet<>();
    }


/**
 * 广播文档更新到指定文档的所有连接
 */
public static void broadcastUpdateToDoc(String docId, String content, byte[] contentState) {
    if (!DOC_ROOM_MAP.containsKey(docId)) {
        return;
    }
    
    // 构建更新消息
    String updateMessage = String.format(
        "{\"type\":\"doc_update\",\"content\":\"%s\",\"contentState\":\"%s\"}",
        content != null ? content.replace("\"", "\\\"") : "",
        contentState != null ? Base64.getEncoder().encodeToString(contentState) : ""
    );
    
    // 向该文档房间的所有连接广播更新
    for (Session session : DOC_ROOM_MAP.get(docId)) {
        if (session.isOpen()) {
            try {
                session.getBasicRemote().sendText(updateMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

}