package com.example.collabedit.modules.document.service;

import com.example.collabedit.modules.document.entity.Tag;
import java.util.List;
import java.util.Map;

public interface TagService {
    List<Map<String, Object>> getAllTagsWithDocCount();
    Tag createTag(Tag tag);

    List <Tag> tagSearch(String keyword);

    Integer countDocByTag(Long tagId);
}