package com.example.collabedit.modules.system.mapper;

import com.example.collabedit.modules.system.entity.Permission;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PermissionMapper {

    @Insert("INSERT INTO permission(perm_name, perm_key, create_time, update_time) " +
            "VALUES(#{permName}, #{permKey}, #{createTime}, #{updateTime})")
    void insert(Permission permission);

    @Select("SELECT * FROM permission WHERE id = #{id}")
    Permission findById(Long id);

    @Select("SELECT * FROM permission")
    List<Permission> findAll();

    @Select("SELECT p.* FROM permission p JOIN role_permission rp ON p.id = rp.perm_id WHERE rp.role_id = #{roleId}")
    List<Permission> findByRoleId(Long roleId);

    @Select("SELECT p.* FROM permission p " +
            "JOIN role_permission rp ON p.id = rp.perm_id " +
            "JOIN user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Permission> findByUserId(Long userId);
}