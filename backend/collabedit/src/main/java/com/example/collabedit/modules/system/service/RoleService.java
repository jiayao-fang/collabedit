package com.example.collabedit.modules.system.service;

import com.example.collabedit.modules.system.dto.RoleDTO;
import com.example.collabedit.modules.system.entity.Role;

import java.util.List;

public interface RoleService {
    Role createRole(RoleDTO roleDTO);
    Role updateRole(Long id, RoleDTO roleDTO);
    Role getRoleById(Long id);
    List<Role> getAllRoles();
    List<Role> getRolesByUserId(Long userId);
}