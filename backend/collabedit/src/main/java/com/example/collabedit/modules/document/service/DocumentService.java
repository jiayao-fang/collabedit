package com.example.collabedit.modules.document.service;
import com.example.collabedit.modules.document.dto.DocCategoryDTO;
import com.example.collabedit.modules.document.entity.Document;
import com.example.collabedit.modules.document.entity.Folder;
//import com.example.collabedit.modules.document.entity.Folder;
import com.example.collabedit.modules.document.entity.Tag;
import java.time.LocalDateTime;
import java.util.List;

public interface DocumentService {
    Document create(Document doc);
    Document getById(Long id);
    List<Document> getAll();
    List<Document> getByAuthor(Long authorId);
    Document edit(Document doc);
    //逻辑删除
    void delete(Long id);
    //永久删除
    void delPer(Long id);
     // 查询回收站文档
    List<Document> getRecycleByAuthor(Long authorId);
    
    // 恢复文档
    void recover(Long id);

    String markdownToHtml(String markdown);

    List <DocCategoryDTO> listByTagId(Long tagId);

    List <DocCategoryDTO> listByFolderId(Long folderId);

    Document getByIdWithPermission(Long id, Long userId, boolean isAdmin);
    
    List<Document> getAllWithPermission(Long userId, boolean isAdmin);
    
    void updateVisibility(Long docId, Integer visibility, Long operatorId, boolean isAdmin);
    
    void addEditor(Long docId, Long userId, Long operatorId);
    
    void removeEditor(Long docId, Long editorId, Long operatorId);
    
    List<Long> getEditors(Long docId);
    
     boolean isEditor(Long docId, Long userId);

      // 全文搜索
    List<Document> search(String keyword);
    
    // List<Tag> getTags();
    // 高级搜索
    List<Document> advancedSearch(Long authorId, LocalDateTime createTime, LocalDateTime updateTime);

    void updateDocumentFolder(Long docId, Long folderId);

    void addTagsToDocument(Long docId, List<Long> tagIds);

    Folder getDocumentFolder(Long docId, Long userId, boolean isAdmin);

    List<Tag> getDocumentTags(Long docId, Long userId, boolean isAdmin);

    void updateById(Document document);
    
    void updateByIdWithoutVersion(Document document);

    // 版本锁定相关
    void lockDocument(Long docId, Long userId);
    
    void unlockDocument(Long docId, Long userId);
    
    boolean isDocumentLocked(Long docId);

}
