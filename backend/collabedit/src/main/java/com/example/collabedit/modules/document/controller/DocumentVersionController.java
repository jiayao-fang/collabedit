package com.example.collabedit.modules.document.controller;

import com.example.collabedit.modules.document.entity.DocumentVersion;
import com.example.collabedit.modules.document.service.DocumentVersionService;
import com.example.collabedit.modules.system.annotation.LogOperation;
import com.example.collabedit.common.enums.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doc/version")
public class DocumentVersionController {
    
    @Resource
    private DocumentVersionService versionService;
    
    /**
     * 创建文档版本快照
     */
    @PostMapping("/create/{docId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "创建文档版本", resource = "document")
    public Result<DocumentVersion> createVersion(
            @PathVariable Long docId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        String changeDescription = (body != null) ? body.get("changeDescription") : null;
        DocumentVersion version = versionService.createVersion(docId, userId, changeDescription);
        return Result.success(version);
    }
    
    /**
     * 获取文档的版本历史列表
     */
    @GetMapping("/history/{docId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "查看版本历史", resource = "document")
    public Result<List<DocumentVersion>> getVersionHistory(
            @PathVariable Long docId,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        List<DocumentVersion> versions = versionService.getVersionHistory(docId, userId);
        return Result.success(versions);
    }
    
    /**
     * 获取指定版本详情
     */
    @GetMapping("/{versionId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "查看版本详情", resource = "document")
    public Result<DocumentVersion> getVersionById(
            @PathVariable Long versionId,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        DocumentVersion version = versionService.getVersionById(versionId, userId);
        return Result.success(version);
    }
    
    /**
     * 回滚到指定版本
     */
    @PostMapping("/rollback/{docId}/{versionId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "回滚文档版本", resource = "document")
    public Result<String> rollbackToVersion(
            @PathVariable Long docId,
            @PathVariable Long versionId,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        versionService.rollbackToVersion(docId, versionId, userId);
        return Result.success("回滚成功");
    }
    
    /**
     * 删除指定版本
     */
    @DeleteMapping("/{versionId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "删除文档版本", resource = "document")
    public Result<String> deleteVersion(
            @PathVariable Long versionId,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        versionService.deleteVersion(versionId, userId);
        return Result.success("删除成功");
    }
    
    /**
     * 比较两个版本的差异
     */
    @GetMapping("/compare/{versionId1}/{versionId2}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "比较版本差异", resource = "document")
    public Result<String> compareVersions(
            @PathVariable Long versionId1,
            @PathVariable Long versionId2,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        String diff = versionService.compareVersions(versionId1, versionId2, userId);
        return Result.success(diff);
    }
}
