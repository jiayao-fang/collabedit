package com.example.collabedit.modules.document.service;

import com.example.collabedit.modules.document.entity.DocumentVersion;
import java.util.List;

public interface DocumentVersionService {
    
    /**
     * 创建文档版本快照
     */
    DocumentVersion createVersion(Long docId, Long userId, String changeDescription);
    
    /**
     * 获取文档的所有历史版本
     */
    List<DocumentVersion> getVersionHistory(Long docId, Long userId);
    
    /**
     * 获取指定版本详情
     */
    DocumentVersion getVersionById(Long versionId, Long userId);
    
    /**
     * 回滚到指定版本
     */
    void rollbackToVersion(Long docId, Long versionId, Long userId);
    
    /**
     * 删除指定版本
     */
    void deleteVersion(Long versionId, Long userId);
    
    /**
     * 比较两个版本的差异
     */
    String compareVersions(Long versionId1, Long versionId2, Long userId);
}
