package com.example.collabedit.modules.document.service.impl;

import com.example.collabedit.common.exception.BusinessException;
import com.example.collabedit.modules.document.dto.DocCategoryDTO;
import com.example.collabedit.modules.document.entity.Document;
import com.example.collabedit.modules.document.entity.DocumentEditor;
import com.example.collabedit.modules.document.entity.Folder;
import com.example.collabedit.modules.document.entity.Tag;
import com.example.collabedit.modules.document.mapper.DocumentEditorMapper;
import com.example.collabedit.modules.document.mapper.DocumentMapper;
import com.example.collabedit.modules.document.mapper.TagMapper;
import com.example.collabedit.modules.document.mapper.FolderMapper;
import com.example.collabedit.modules.document.service.DocumentService;

import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    @Resource
    private DocumentMapper documentmapper;
    @Resource 
    private TagMapper tagMapper;
    @Resource
    private FolderMapper folderMapper;
    @Resource
    private DocumentEditorMapper documentEditorMapper;


    @Override
    public Document create(Document doc) {
        documentmapper.insert(doc);
        return doc;
    }

    @Override
    public Document getById(Long id) {
        Document doc = documentmapper.findById(id);
        if (doc == null) {
            throw new RuntimeException("文档不存在或已删除");
        }
        return doc;
    }

    @Override
    public List<Document> getAll() {
        return documentmapper.findAll();
    }

    @Override
    public List<Document> getByAuthor(Long authorId) {
        return documentmapper.findByAuthor(authorId);
    }

   // 带版本校验的编辑
    @Override
    public Document edit(Document doc) {
        //将Markdown内容转换为HTML
         if (doc.getContent() != null && doc.getContent().startsWith("``")) {
        String markdown = doc.getContent().replace("``", "");
        doc.setContent(markdownToHtml(markdown)); // 调用转换方法
        }
        int rows = documentmapper.updateWithVersion(doc);
        if (rows!= 0) {
            System.out.println("修改已保存！");
        }
        return documentmapper.findById(doc.getId());
    }

       // 逻辑删除
    @Override
    public void delete(Long id) {
        int rows = documentmapper.logicDelete(id);
        if (rows == 0) {
            throw new RuntimeException("删除失败，文档不存在");
        }
    }

    @Override
    public void delPer(Long id) {
        int rows = documentmapper.perDelete(id);
        if (rows == 0) {
            throw new RuntimeException("删除失败，文档不存在");
        }
    }

    // 查询回收站
    @Override
    public List<Document> getRecycleByAuthor(Long authorId) {
        return documentmapper.findRecycleByAuthor(authorId);
    }

    // 恢复文档
    @Override
    public void recover(Long id) {
        int rows = documentmapper.recover(id);
        if (rows == 0) {
            throw new RuntimeException("恢复失败，文档不存在");
        }
    }

    // Markdown转HTML
    @Override
    public String markdownToHtml(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document);
    }

    @Override
    public Document getByIdWithPermission(Long id, Long userId, boolean isAdmin) {
        Document doc = documentmapper.findByIdWithPermission(id, userId, isAdmin);
        if (doc == null) {
            throw new RuntimeException("文档不存在或没有访问权限");
        }
        return doc;
    }

    @Override
    @Transactional
    public void updateVisibility(Long docId, Integer visibility, Long operatorId, boolean isAdmin) {
        // 验证可见性参数合法性
        if (visibility < 0 || visibility > 2) {
            throw new RuntimeException("无效的可见性设置");
        }
        
        Document doc = documentmapper.findById(docId);
        if (doc == null) {
            throw new RuntimeException("文档不存在");
        }
        
        // 检查权限：作者或管理员可以修改
        if (!doc.getAuthorId().equals(operatorId) && !isAdmin) {
            throw new RuntimeException("没有权限修改文档可见性");
        }
        
        // 管理员特殊权限：只能将所有人可见改为仅自己可见
        if (isAdmin && visibility != 0) {
            throw new RuntimeException("管理员只能将文档设置为仅自己可见");
        }
        
        int rows = documentmapper.updateVisibility(docId, visibility);
        if (rows == 0) {
            throw new RuntimeException("更新可见性失败");
        }
    }

      @Override
    @Transactional
    public void addEditor(Long docId, Long userId, Long operatorId) {
        Document doc = documentmapper.findById(docId);
        if (doc == null) {
            throw new RuntimeException("文档不存在");
        }
        
        // 只有作者可以邀请编辑者
        if (!doc.getAuthorId().equals(operatorId)) {
            throw new RuntimeException("没有权限邀请编辑者");
        }
        
        // 不能添加自己为编辑者
        if (doc.getAuthorId().equals(userId)) {
            throw new RuntimeException("不能添加自己为编辑者");
        }
        
        // 检查是否已经是编辑者
        int count = documentEditorMapper.countByDocumentAndUser(docId, userId);
        if (count > 0) {
            throw new RuntimeException("该用户已经是编辑者");
        }
        
        DocumentEditor editor = new DocumentEditor();
        editor.setDocumentId(docId);
        editor.setUserId(userId);
        documentEditorMapper.insert(editor);
    }
    
    @Override
    @Transactional
    public void removeEditor(Long docId, Long editorId, Long operatorId) {
        Document doc = documentmapper.findById(docId);
        if (doc == null) {
            throw new RuntimeException("文档不存在");
        }
        
        // 只有作者可以移除编辑者
        if (!doc.getAuthorId().equals(operatorId)) {
            throw new RuntimeException("没有权限移除编辑者");
        }
        
        int rows = documentEditorMapper.delete(docId, editorId);
        if (rows == 0) {
            throw new RuntimeException("移除编辑者失败");
        }
    }
    
    @Override
    public List<Long> getEditors(Long docId) {
        return documentEditorMapper.findByDocumentId(docId);
    }
    
    @Override
    public boolean isEditor(Long docId, Long userId) {
        return documentEditorMapper.countByDocumentAndUser(docId, userId) > 0 ;
    }
    
    @Override
    public List<Document> getAllWithPermission(Long userId, boolean isAdmin) {
        return documentmapper.findAllWithPermission(userId, isAdmin);
    }

     @Override
    public List <DocCategoryDTO> listByTagId(Long tagId) {
        // 1. 校验参数
        if (tagId == null) {
            throw new RuntimeException("标签ID不能为空");
        }
        // 2. 校验标签是否存在
        Tag tag = tagMapper.findById(tagId); // 需要在TagMapper中新增findById方法
        if (tag == null) {
            throw new RuntimeException("标签不存在");
        }
        return documentmapper.listByTagId(tagId);
    }

     @Override
     public List<DocCategoryDTO> listByFolderId(Long folderId) {
        // 1. 校验参数
        if (folderId == null) {
            throw new RuntimeException("文件ID不能为空");
        }
        // 2. 校验文件是否存在
        Folder folder = folderMapper.findById(folderId); 
        if (folder == null) {
            throw new RuntimeException("文件不存在");
        }
       return documentmapper.listByFolderId(folderId);
    }

    @Override
    public List<Document> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new RuntimeException("搜索关键词不能为空");
        }
        return documentmapper.search(keyword);
    }

    @Override
    public List<Document> advancedSearch(Long authorId, LocalDateTime createTime, LocalDateTime updateTime) {
        // 至少需要一个搜索条件
        if (authorId == null && createTime == null && updateTime == null) {
            throw new RuntimeException("至少需要一个搜索条件");
        }
        return documentmapper.advancedSearch(authorId, createTime, updateTime);
    }


    @Override
    public void updateDocumentFolder(Long docId, Long folderId) {
        Document doc = documentmapper.findById(docId);
        if (doc == null) {
            throw new RuntimeException("文档不存在");
        }
        documentmapper.updateFolderId(docId, folderId);
    }

@Override
@Transactional
public void addTagsToDocument(Long docId, List<Long> tagIds) {
    Document doc = documentmapper.findById(docId);
    if (doc == null) {
        throw new RuntimeException("文档不存在");
    }
    
    List<Long> existTagIds = tagMapper.findExistIdsByIds(tagIds);
    // 格式化为逗号分隔的字符串（如"1,2,3"）
    String tagIdsStr = String.join(",", tagIds.stream()
            .map(String::valueOf)
            .collect(Collectors.toList()));
    documentmapper.updateTagIds(docId, tagIdsStr);
}

    @Override
    public Folder getDocumentFolder(Long docId, Long userId, boolean isAdmin) {
        Document doc = getByIdWithPermission(docId, userId, isAdmin);
        if (doc.getFolderId() == null) {
            return null;
        }
        return folderMapper.findById(doc.getFolderId());
    }

    @Override
    public List<Tag> getDocumentTags(Long docId, Long userId, boolean isAdmin) {
        Document doc = getByIdWithPermission(docId, userId, isAdmin);
        if (doc.getTagIds() == null || doc.getTagIds().isEmpty()) {
            return List.of();
        }
        return tagMapper.findByIds(doc.getTagIds());
    }

    @Override
    public void updateById(Document document) {
        documentmapper.updateWithVersion(document);
    }
    
    @Override
    public void updateByIdWithoutVersion(Document document) {
        documentmapper.updateWithoutVersion(document);
    }

    @Override
    @Transactional
    public void lockDocument(Long docId, Long userId) {
        Document doc = documentmapper.findById(docId);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在");
        }
        
        // 检查权限：只有作者和编辑者可以锁定
        if (!doc.getAuthorId().equals(userId) && !isEditor(docId, userId)) {
            throw new BusinessException(403, "无权限锁定此文档");
        }
        
        // 检查是否已被锁定
        if (doc.getIsLocked() != null && doc.getIsLocked() == 1) {
            throw new BusinessException(423, "文档已被锁定");
        }
        
        documentmapper.lockDocument(docId, userId, LocalDateTime.now());
    }
    
    @Override
    @Transactional
    public void unlockDocument(Long docId, Long userId) {
        Document doc = documentmapper.findById(docId);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在");
        }
        
        // 检查权限：只有锁定者或作者可以解锁
        if (!doc.getAuthorId().equals(userId) && !userId.equals(doc.getLockedBy())) {
            throw new BusinessException(403, "无权限解锁此文档");
        }
        
        documentmapper.unlockDocument(docId);
    }
    
    @Override
    public boolean isDocumentLocked(Long docId) {
        Document doc = documentmapper.findById(docId);
        return doc != null && doc.getIsLocked() != null && doc.getIsLocked() == 1;
    }

}

