package com.example.collabedit.modules.system.service;

import com.example.collabedit.modules.system.entity.Permission;

import java.util.List;

public interface PermissionService {
    List<Permission> getPermissionsByUserId(Long userId);
    List<Permission> getAllPermissions();
    void assignPermissions(Long roleId, List<Long> permIds);
}