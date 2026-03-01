package com.example.collabedit.modules.system.service.impl;

import com.example.collabedit.modules.system.entity.Permission;
import com.example.collabedit.modules.system.entity.RolePermission;
import com.example.collabedit.modules.system.mapper.PermissionMapper;
import com.example.collabedit.modules.system.mapper.RolePermissionMapper;
import com.example.collabedit.modules.system.service.PermissionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private PermissionMapper permissionMapper;

    @Resource
    private RolePermissionMapper rolePermissionMapper;

    @Override
    public List<Permission> getPermissionsByUserId(Long userId) {
        return permissionMapper.findByUserId(userId);
    }

    @Override
    public List<Permission> getAllPermissions() {
        return permissionMapper.findAll();
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permIds) {
        // 先删除该角色已有的权限
        rolePermissionMapper.deleteByRoleId(roleId);
        
        // 再添加新的权限
        if (permIds != null && !permIds.isEmpty()) {
            for (Long permId : permIds) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermId(permId);
                rolePermissionMapper.insert(rolePermission);
            }
        }
    }
}