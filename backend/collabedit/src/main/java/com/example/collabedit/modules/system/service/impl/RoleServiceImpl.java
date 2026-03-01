package com.example.collabedit.modules.system.service.impl;

import com.example.collabedit.modules.system.dto.RoleDTO;
import com.example.collabedit.modules.system.entity.Role;
import com.example.collabedit.modules.system.mapper.RoleMapper;
import com.example.collabedit.modules.system.service.RoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    @Resource
    private RoleMapper roleMapper;

    @Override
    public Role createRole(RoleDTO roleDTO) {
        Role role = new Role();
        role.setRoleName(roleDTO.getRoleName());
        role.setRoleDesc(roleDTO.getRoleDesc());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);
        return role;
    }

    @Override
    public Role updateRole(Long id, RoleDTO roleDTO) {
        Role role = roleMapper.findById(id);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        role.setRoleName(roleDTO.getRoleName());
        role.setRoleDesc(roleDTO.getRoleDesc());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.update(role);
        return role;
    }

    @Override
    public Role getRoleById(Long id) {
        Role role = roleMapper.findById(id);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        return role;
    }

    @Override
    public List<Role> getAllRoles() {
        return roleMapper.findAll();
    }

    @Override
    public List<Role> getRolesByUserId(Long userId) {
        return roleMapper.findByUserId(userId);
    }
}