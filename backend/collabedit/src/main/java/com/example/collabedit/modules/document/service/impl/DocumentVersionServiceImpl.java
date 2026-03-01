package com.example.collabedit.modules.document.service.impl;

import com.example.collabedit.modules.document.entity.Document;
import com.example.collabedit.modules.document.entity.DocumentVersion;
import com.example.collabedit.modules.document.mapper.DocumentVersionMapper;
import com.example.collabedit.modules.document.service.DocumentService;
import com.example.collabedit.modules.document.service.DocumentVersionService;
import com.example.collabedit.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentVersionServiceImpl implements DocumentVersionService {
    
    @Resource
    private DocumentVersionMapper versionMapper;
    
    @Resource
    private DocumentService documentService;
    
    @Override
    @Transactional
    public DocumentVersion createVersion(Long docId, Long userId, String changeDescription) {
        // 1. 获取当前文档
        Document doc = documentService.getById(docId);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在");
        }
        
        // 2. 检查权限（只有作者和编辑者可以创建版本）
        if (!doc.getAuthorId().equals(userId) && !documentService.isEditor(docId, userId)) {
            throw new BusinessException(403, "无权限创建版本");
        }
        
        // 3. 获取下一个版本号
        Integer maxVersion = versionMapper.findMaxVersionNumber(docId);
        int nextVersion = (maxVersion == null) ? 1 : maxVersion + 1;
        
        // 4. 创建版本快照
        DocumentVersion version = new DocumentVersion();
        version.setDocId(docId);
        version.setVersionNumber(nextVersion);
        version.setTitle(doc.getTitle());
        version.setContent(doc.getContent());
        version.setContentState(doc.getContentState());
        version.setCreatedBy(userId);
        version.setCreateTime(LocalDateTime.now());
        version.setChangeDescription(changeDescription);
        
        // 计算文件大小
        long size = 0;
        if (doc.getContent() != null) {
            size += doc.getContent().getBytes().length;
        }
        if (doc.getContentState() != null) {
            size += doc.getContentState().length;
        }
        version.setFileSize(size);
        
        versionMapper.insert(version);
        return version;
    }
    
    @Override
    public List<DocumentVersion> getVersionHistory(Long docId, Long userId) {
        // 检查文档访问权限
        Document doc = documentService.getById(docId);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在");
        }
        
        // 检查是否有查看权限
        if (!doc.getAuthorId().equals(userId) && !documentService.isEditor(docId, userId)) {
            // 检查可见性
            if (doc.getVisibility() == 0) {
                throw new BusinessException(403, "无权限查看此文档的版本历史");
            }
        }
        
        return versionMapper.findByDocId(docId);
    }
    
    @Override
    public DocumentVersion getVersionById(Long versionId, Long userId) {
        DocumentVersion version = versionMapper.findById(versionId);
        if (version == null) {
            throw new BusinessException(404, "版本不存在");
        }
        
        // 检查文档访问权限
        Document doc = documentService.getById(version.getDocId());
        if (!doc.getAuthorId().equals(userId) && !documentService.isEditor(version.getDocId(), userId)) {
            if (doc.getVisibility() == 0) {
                throw new BusinessException(403, "无权限查看此版本");
            }
        }
        
        return version;
    }
    
    @Override
    @Transactional
    public void rollbackToVersion(Long docId, Long versionId, Long userId) {
        // 1. 获取目标版本
        DocumentVersion targetVersion = versionMapper.findById(versionId);
        if (targetVersion == null || !targetVersion.getDocId().equals(docId)) {
            throw new BusinessException(404, "版本不存在");
        }
        
        // 2. 获取当前文档
        Document doc = documentService.getById(docId);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在");
        }
        
        // 3. 检查权限（只有作者和编辑者可以回滚）
        if (!doc.getAuthorId().equals(userId) && !documentService.isEditor(docId, userId)) {
            throw new BusinessException(403, "无权限回滚文档");
        }
        
        // 4. 检查文档是否被锁定
        if (doc.getIsLocked() != null && doc.getIsLocked() == 1) {
            throw new BusinessException(423, "文档已被锁定，无法回滚");
        }
        
        // 5. 直接恢复到目标版本（不创建备份）
        doc.setTitle(targetVersion.getTitle());
        doc.setContent(targetVersion.getContent());
        doc.setContentState(targetVersion.getContentState());
        doc.setUpdateTime(LocalDateTime.now());
        doc.setUpdateBy(userId);
        
        documentService.updateByIdWithoutVersion(doc);
        
        // 6. 创建新的版本记录（标记为当前版本）
        createVersion(docId, userId, "恢复到版本 " + targetVersion.getVersionNumber());
    }
    
    @Override
    @Transactional
    public void deleteVersion(Long versionId, Long userId) {
        DocumentVersion version = versionMapper.findById(versionId);
        if (version == null) {
            throw new BusinessException(404, "版本不存在");
        }
        
        // 检查权限（只有文档作者可以删除版本）
        Document doc = documentService.getById(version.getDocId());
        if (!doc.getAuthorId().equals(userId)) {
            throw new BusinessException(403, "只有文档作者可以删除版本");
        }
        
        // 检查是否是最后一个版本（至少保留一个版本）
        int versionCount = versionMapper.countByDocId(version.getDocId());
        if (versionCount <= 1) {
            throw new BusinessException(400, "至少需要保留一个版本");
        }
        
        versionMapper.deleteById(versionId);
    }
    
    @Override
    public String compareVersions(Long versionId1, Long versionId2, Long userId) {
        DocumentVersion v1 = getVersionById(versionId1, userId);
        DocumentVersion v2 = getVersionById(versionId2, userId);
        
        if (!v1.getDocId().equals(v2.getDocId())) {
            throw new BusinessException(400, "两个版本不属于同一文档");
        }
        
        // 简单的文本差异比较（可以使用更复杂的diff算法）
        StringBuilder diff = new StringBuilder();
        diff.append("版本 ").append(v1.getVersionNumber()).append(" vs 版本 ").append(v2.getVersionNumber()).append("\n\n");
        
        if (!v1.getTitle().equals(v2.getTitle())) {
            diff.append("标题变更:\n");
            diff.append("- ").append(v1.getTitle()).append("\n");
            diff.append("+ ").append(v2.getTitle()).append("\n\n");
        }
        
        if (v1.getContent() != null && v2.getContent() != null && !v1.getContent().equals(v2.getContent())) {
            diff.append("内容已变更\n");
            diff.append("字符数变化: ").append(v1.getContent().length()).append(" -> ").append(v2.getContent().length()).append("\n");
        }
        
        return diff.toString();
    }
}

