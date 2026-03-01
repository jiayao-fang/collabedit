package com.example.collabedit.modules.system.mapper;

import com.example.collabedit.modules.system.entity.RolePermission;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RolePermissionMapper {

    @Insert("INSERT INTO role_permission(role_id, perm_id) VALUES(#{roleId}, #{permId})")
    void insert(RolePermission rolePermission);

    @Delete("DELETE FROM role_permission WHERE role_id = #{roleId}")
    void deleteByRoleId(Long roleId);

    @Select("SELECT perm_id FROM role_permission WHERE role_id = #{roleId}")
    List<Long> findPermIdsByRoleId(Long roleId);
}