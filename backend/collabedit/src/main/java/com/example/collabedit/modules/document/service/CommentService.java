package com.example.collabedit.modules.document.service;

import com.example.collabedit.modules.document.entity.Comment;
import java.util.List;

public interface CommentService {
    Comment addComment(Comment comment);
    
    Comment replyToComment(Long parentId, Comment reply);
    
    List<Comment> getCommentsByDocId(Long docId);
    
    List<Comment> getRepliesByParentId(Long parentId);
    
    void deleteComment(Long commentId, Long operatorId);
    
    // 解析评论内容中的@用户
    List<Long> parseMentionedUsers(String content);
}