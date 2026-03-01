package com.example.collabedit.modules.document.controller;

import com.example.collabedit.modules.document.entity.Folder;
import com.example.collabedit.modules.document.service.FolderService;
import com.example.collabedit.modules.system.annotation.LogOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doc")
public class FolderController {

    @Resource
    private FolderService folderService;

    // 创建文件夹
    @PostMapping("/createFolder")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "创建文件夹", resource = "folder")
    public Folder createFolder(@RequestBody Folder folder, Authentication authentication) {
        Long creatorId = Long.valueOf(authentication.getName());
        folder.setCreatorId(creatorId);
        return folderService.createFolder(folder);
    }

    // 查询文件夹列表（支持父文件夹筛选）
    @GetMapping("/folders")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> getAllMyFolders(Authentication authentication) {
        Long creatorId = Long.valueOf(authentication.getName());
        return folderService.getAllFoldersWithDocCount(creatorId);
    }

    // 获取所有文件夹
    @GetMapping("/allFolders")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> getAllFolders() {
        return folderService.AllFolder();
    }

    // 查询单个文件夹的文档数
    @GetMapping("/{folderId}/doc-count")
    @PreAuthorize("isAuthenticated()")
    public Integer getFolderDocCount(
            @PathVariable Long folderId) {
        return folderService.countDocByFolder(folderId);
    }

    // 删除文件夹
    @DeleteMapping("/delFolder/{id}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "删除文件夹", resource = "folder")
    public String deleteFolder(@PathVariable Long id, Authentication authentication) {
        Long creatorId = Long.valueOf(authentication.getName());
        folderService.deleteFolder(id, creatorId);
        return "success";
    }

    // 搜索文件夹
    @GetMapping("/folderSearch")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "搜索文档夹", resource = "folder")
    public List<Folder> folderSearch(
            @RequestParam String keyword){
        return folderService.folderSearch(keyword);
    }

}