package com.example.collabedit.modules.document.service.impl;

import com.example.collabedit.modules.document.entity.Folder;
import com.example.collabedit.modules.document.mapper.FolderMapper;
import com.example.collabedit.modules.document.service.FolderService;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class FolderServiceImpl implements FolderService {

    @Resource
    private FolderMapper folderMapper;

    @Override
    public Folder getByIdAndCreator(Long folderId, Long creatorId) {
        return folderMapper.findByIdAndCreator(folderId, creatorId);
    }

    @Override
    public List<Map<String, Object>> getAllFoldersWithDocCount(Long creatorId) {
        return folderMapper.findAllFoldersWithDocCount(creatorId);
    }

    @Override
    public List<Map<String, Object>> AllFolder() {
        return folderMapper.AllFolder();
    }

    @Override
    public Integer countDocByFolder(Long folderId) {
        return folderMapper.countDocByFolder(folderId);
    }

    @Override
    public Folder createFolder(Folder folder) {
        folderMapper.insert(folder);
        return folder;
    }

    @Override
    public void deleteFolder(Long folderId, Long creatorId) {
        // 可添加删除前校验（如是否有子文件夹/文档）
        folderMapper.delete(folderId, creatorId);
    }

    @Override
   public List <Folder> folderSearch(String keyword){
        return folderMapper.findFoldersByKey(keyword);
    }
}