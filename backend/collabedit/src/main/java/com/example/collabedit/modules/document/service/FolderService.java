// src/main/java/com/example/collabedit/modules/document/service/FolderService.java
package com.example.collabedit.modules.document.service;

import com.example.collabedit.modules.document.entity.Folder;
import java.util.List;
import java.util.Map;

public interface FolderService {
     Folder getByIdAndCreator(Long folderId, Long creatorId);
    // 查询所有文件夹及其文档数
    List<Map<String, Object>> getAllFoldersWithDocCount(Long creatorId);
    List<Map<String, Object>> AllFolder();
    // 新增：统计单个文件夹（含子文件夹）的文档数
    Integer countDocByFolder(Long folderId);
    // 原有创建/删除文件夹方法...
    Folder createFolder(Folder folder);
    void deleteFolder(Long folderId, Long creatorId);
    List<Folder> folderSearch(String keyword);

}