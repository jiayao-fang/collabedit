package com.example.collabedit.modules.system.controller;

import com.example.collabedit.modules.system.dto.RoleDTO;
import com.example.collabedit.modules.system.entity.Role;
import com.example.collabedit.modules.system.service.RoleService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/admin/role")
@PreAuthorize("hasRole('admin')")
public class RoleController {

    @Resource
    private RoleService roleService;

    @PostMapping
    public Role createRole(@RequestBody RoleDTO roleDTO) {
        return roleService.createRole(roleDTO);
    }

    @PutMapping("/{id}")
    public Role updateRole(@PathVariable Long id, @RequestBody RoleDTO roleDTO) {
        return roleService.updateRole(id, roleDTO);
    }

    @GetMapping("/{id}")
    public Role getRole(@PathVariable Long id) {
        return roleService.getRoleById(id);
    }

    @GetMapping("/roles")
    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }
}