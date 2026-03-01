package com.example.collabedit.modules.system.mapper;

import com.example.collabedit.modules.system.entity.Role;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoleMapper {

    @Insert("INSERT INTO role(role_name, role_desc, create_time, update_time) " +
            "VALUES(#{roleName}, #{roleDesc}, #{createTime}, #{updateTime})")
    void insert(Role role);

    @Update("UPDATE role SET role_name = #{roleName}, role_desc = #{roleDesc}, update_time = #{updateTime} WHERE id = #{id}")
    void update(Role role);

    @Select("SELECT * FROM role WHERE id = #{id}")
    Role findById(Long id);

    @Select("SELECT * FROM role")
    List<Role> findAll();

    @Select("SELECT r.* FROM role r JOIN user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<Role> findByUserId(Long userId);
}