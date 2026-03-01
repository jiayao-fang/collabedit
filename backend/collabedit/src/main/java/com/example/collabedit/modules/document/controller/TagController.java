package com.example.collabedit.modules.document.controller;

import com.example.collabedit.modules.document.entity.Tag;
import com.example.collabedit.modules.document.service.TagService;
import com.example.collabedit.modules.system.annotation.LogOperation;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Resource
    private TagService tagService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> getAllTags() {
        return tagService.getAllTagsWithDocCount();
    }
    
    // 搜索标签
    @GetMapping("/tagSearch")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "搜索tag", resource = "tag")
    public List<Tag> tagSearch(
            @RequestParam(required = false) String keyword){
        return tagService.tagSearch(keyword);
    }

    //获取标签中的文档数量
    @GetMapping("/{tagId}/doc-count")
    @PreAuthorize("isAuthenticated()")
    public Integer getTagDocCount(
            @PathVariable Long tagId) {
        return tagService.countDocByTag(tagId);
    }

     // 创建标签
    @PostMapping("/createTag")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "创建标签", resource = "tag")
    public Tag createTag(@RequestBody Tag tag) {
        return tagService.createTag(tag);
    }
}