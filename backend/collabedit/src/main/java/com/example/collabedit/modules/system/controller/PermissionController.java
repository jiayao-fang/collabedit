package com.example.collabedit.modules.system.controller;

import com.example.collabedit.modules.system.dto.PermissionAssignDTO;
import com.example.collabedit.modules.system.entity.Permission;
import com.example.collabedit.modules.system.service.PermissionService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PermissionController {

    @Resource
    private PermissionService permissionService;

    @PostMapping("/api/admin/role/assign-perm")
    @PreAuthorize("hasRole('admin')")
    public String assignPermissions(@RequestBody PermissionAssignDTO dto) {
        permissionService.assignPermissions(dto.getRoleId(), dto.getPermIds());
        return "权限分配成功";
    }

    @GetMapping("/api/user/permissions")
    public List<Permission> getUserPermissions(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        return permissionService.getPermissionsByUserId(userId);
    }
}