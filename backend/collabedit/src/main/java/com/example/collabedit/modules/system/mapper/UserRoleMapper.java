package com.example.collabedit.modules.system.mapper;

import com.example.collabedit.modules.system.entity.UserRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserRoleMapper {

    @Insert("INSERT INTO user_role(user_id, role_id,create_time) VALUES(#{userId}, #{roleId},#{createTime})")
    void insert(UserRole userRole);

    @Delete("DELETE FROM user_role WHERE user_id = #{userId}")
    void deleteByUserId(Long userId);

    @Select("SELECT role_id FROM user_role WHERE user_id = #{userId}")
    List<Long> findRoleIdsByUserId(Long userId);
}