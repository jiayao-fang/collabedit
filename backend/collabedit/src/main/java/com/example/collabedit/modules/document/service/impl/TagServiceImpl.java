package com.example.collabedit.modules.document.service.impl;

import com.example.collabedit.modules.document.entity.Tag;
import com.example.collabedit.modules.document.mapper.TagMapper;
import com.example.collabedit.modules.document.service.TagService;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class TagServiceImpl implements TagService {

    @Resource
    private TagMapper tagMapper;

    @Override
    public List<Map<String, Object>> getAllTagsWithDocCount() {
        // 调用新增的统计方法
        return tagMapper.findAllWithDocCount();
    }
    
    @Override
    public Tag createTag(Tag tag) {
        // 检查标签是否已存在
        Tag existing = tagMapper.findByName(tag.getTagName());
        if (existing != null) {
            return existing;
        }
        tagMapper.insert(tag);
        return tag;
    }

    @Override
    public List<Tag> tagSearch(String keyword){
        return tagMapper.findTagsByKey(keyword);
    }
    
    @Override 
    public Integer countDocByTag(Long tagId){
        return tagMapper.countDocInTag(tagId);

    }
}