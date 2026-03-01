package com.example.collabedit.modules.document.service.impl;

import com.example.collabedit.modules.document.entity.Comment;
import com.example.collabedit.modules.document.entity.Document;
import com.example.collabedit.modules.document.mapper.CommentMapper;
import com.example.collabedit.modules.document.mapper.DocumentMapper;
import com.example.collabedit.modules.document.service.CommentService;
import com.example.collabedit.modules.notification.service.NotificationService;
import com.example.collabedit.modules.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CommentServiceImpl implements CommentService {

    @Resource
    private CommentMapper commentMapper;
    
    @Resource
    private DocumentMapper documentMapper;
    
    @Resource
    private UserMapper userMapper; 
    
    @Resource
    private NotificationService notificationService; 

    @Override
    @Transactional
    public Comment addComment(Comment comment) {
        // 验证文档是否存在
        Document doc = documentMapper.findById(comment.getDocId());
        if (doc == null) {
            throw new RuntimeException("文档不存在");
        }
        
        // 保存评论
        commentMapper.insert(comment);
        
        // 解析@用户
        List<Long> mentionedUserIds = parseMentionedUsers(comment.getContent());
        
        // 获取评论者ID和文档作者ID
        Long commenterId = comment.getUserId();
        Long docAuthorId = doc.getAuthorId();
        
        // 只有当评论者不是文档作者时，才发送通知给文档作者
        if (!commenterId.equals(docAuthorId)) {
            notificationService.sendCommentNotification(
                comment.getDocId(), 
                comment.getId(), 
                commenterId, 
                docAuthorId, 
                mentionedUserIds
            );
        } else {
            // 如果评论者就是文档作者，仍然需要发送通知给被@的用户
            if (mentionedUserIds != null && !mentionedUserIds.isEmpty()) {
                // 过滤掉评论者自己，避免通知自己
                List<Long> filteredMentionedUserIds = mentionedUserIds.stream()
                    .filter(userId -> !userId.equals(commenterId))
                    .collect(java.util.stream.Collectors.toList());
                
                if (!filteredMentionedUserIds.isEmpty()) {
                    // 发送通知给被@的其他用户
                    notificationService.sendCommentNotification(
                        comment.getDocId(), 
                        comment.getId(), 
                        commenterId, 
                        docAuthorId, 
                        filteredMentionedUserIds
                    );
                }
            }
        }    
        return comment;
    }

    @Override
    @Transactional
    public Comment replyToComment(Long parentId, Comment reply) {
        // 验证父评论是否存在
        Comment parentComment = commentMapper.findById(parentId);
        if (parentComment == null) {
            throw new RuntimeException("父评论不存在");
        }
        
        // 设置回复的父评论ID和文档ID
        reply.setParentId(parentId);
        reply.setDocId(parentComment.getDocId());
        
        // 保存回复
        commentMapper.insert(reply);
        
        // 解析@用户
        List<Long> mentionedUserIds = parseMentionedUsers(reply.getContent());
        
        // 获取回复者ID
        Long replierId = reply.getUserId();
        // 获取原评论作者ID
        Long parentCommentUserId = parentComment.getUserId();
        
        // 如果回复者不是原评论作者，则添加原评论作者到通知列表
        if (!replierId.equals(parentCommentUserId)) {
            if (mentionedUserIds == null) {
                mentionedUserIds = new java.util.ArrayList<>();
            }
            if (!mentionedUserIds.contains(parentCommentUserId)) {
                mentionedUserIds.add(parentCommentUserId);
            }
        }
        
        // 过滤掉回复者自己，避免通知自己
        List<Long> filteredMentionedUserIds = mentionedUserIds.stream()
            .filter(userId -> !userId.equals(replierId))
            .collect(java.util.stream.Collectors.toList());
        
        // 发送通知
        notificationService.sendCommentReplyNotification(
            reply.getDocId(), 
            parentId, 
            reply.getId(), 
            reply.getUserId(), 
            filteredMentionedUserIds
        );  
        return reply;
    }

    @Override
    public List<Comment> getCommentsByDocId(Long docId) {
        return commentMapper.findByDocId(docId);
    }

    @Override
    public List<Comment> getRepliesByParentId(Long parentId) {
        return commentMapper.findByParentId(parentId);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long operatorId) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        
        // 验证权限：只有评论作者或文档作者可以删除
        Document doc = documentMapper.findById(comment.getDocId());
        if (!comment.getUserId().equals(operatorId) && !doc.getAuthorId().equals(operatorId)) {
            throw new RuntimeException("没有权限删除此评论");
        }
        
        commentMapper.delete(commentId);
    }

    @Override
    public List<Long> parseMentionedUsers(String content) {
        List<Long> userIds = new ArrayList<>();
        
        // 正则表达式匹配@用户名
        Pattern pattern = Pattern.compile("@([\\w]+)");
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String username = matcher.group(1);
            // 查询用户ID
            Long userId = userMapper.findIdByUsername(username);
            if (userId != null) {
                userIds.add(userId);
            }
        }
        
        return userIds;
    }
}