package com.example.collabedit.modules.document.controller;

import com.example.collabedit.modules.document.entity.Comment;
import com.example.collabedit.modules.document.service.CommentService;
import com.example.collabedit.modules.system.annotation.LogOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/collab/comment")
public class CommentController {

    @Resource
    private CommentService commentService;

    /**
     * 添加评论/批注
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "添加文档评论", resource = "document")
    public Comment addComment(@RequestBody Comment comment, Principal principal) {
        // 设置评论用户ID
        Long userId = Long.valueOf(principal.getName());
        comment.setUserId(userId);
        return commentService.addComment(comment);
    }

    /**
     * 回复评论
     */
    @PostMapping("/reply")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "回复文档评论", resource = "document")
    public Comment replyToComment(
            @RequestParam Long parentId,
            @RequestBody Comment reply,
            Principal principal) {
        // 设置回复用户ID
        Long userId = Long.valueOf(principal.getName());
        reply.setUserId(userId);
        return commentService.replyToComment(parentId, reply);
    }

    /**
     * 获取文档的所有评论
     */
    @GetMapping("/doc/{docId}")
    @PreAuthorize("isAuthenticated()")
    public List<Comment> getCommentsByDocId(@PathVariable Long docId) {
        return commentService.getCommentsByDocId(docId);
    }

    /**
     * 获取评论的回复
     */
    @GetMapping("/replies/{parentId}")
    @PreAuthorize("isAuthenticated()")
    public List<Comment> getRepliesByParentId(@PathVariable Long parentId) {
        return commentService.getRepliesByParentId(parentId);
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "删除文档评论", resource = "document")
    public String deleteComment(@PathVariable Long commentId, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        commentService.deleteComment(commentId, userId);
        return "success";
    }
}