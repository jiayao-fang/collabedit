package com.example.collabedit.modules.user.service.impl;

import com.example.collabedit.modules.document.mapper.DocumentMapper;
import com.example.collabedit.modules.user.dto.UserBehaviorDTO;
import com.example.collabedit.modules.user.entity.User;
import com.example.collabedit.modules.user.mapper.UserMapper;
import com.example.collabedit.modules.user.service.UserBehaviorService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserBehaviorServiceImpl implements UserBehaviorService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private DocumentMapper documentMapper;

    @Override
    public List<UserBehaviorDTO> getUserBehaviorStatistics() {
        // 获取所有用户
        List<User> users = userMapper.findAll();
        
        // 转换为行为统计DTO
        return users.stream().map(user -> {
            UserBehaviorDTO dto = new UserBehaviorDTO();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            
            // 统计文档创建数
            int docCount = documentMapper.countDocumentsByAuthor(user.getId());
            dto.setDocumentCount(docCount);
            
            // 统计编辑次数（默认0如果为null）
            Integer editCount = documentMapper.sumEditCountsByAuthor(user.getId());
            dto.setEditCount(editCount != null ? editCount : 0);
            
            return dto;
        }).collect(Collectors.toList());
    }
}