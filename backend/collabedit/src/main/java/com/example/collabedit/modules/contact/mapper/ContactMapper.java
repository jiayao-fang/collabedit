// src/main/java/com/example/collabedit/modules/contact/mapper/ContactMapper.java
package com.example.collabedit.modules.contact.mapper;

import com.example.collabedit.modules.contact.entity.Contact;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ContactMapper {
    @Insert("INSERT INTO contact (user_id, contact_user_id, create_time, remark) " +
            "VALUES (#{userId}, #{contactUserId}, NOW(), #{remark})")
    int insert(Contact contact);
    
    @Delete("DELETE FROM contact WHERE user_id = #{userId} AND contact_user_id = #{contactUserId}")
    int delete(@Param("userId") Long userId, @Param("contactUserId") Long contactUserId);
    
    @Select("SELECT * FROM contact WHERE user_id = #{userId}")
    List<Contact> findByUserId(Long userId);
    
    @Select("SELECT COUNT(*) FROM contact WHERE user_id = #{userId} AND contact_user_id = #{contactUserId}")
    int countByUserAndContact(@Param("userId") Long userId, @Param("contactUserId") Long contactUserId);
    
    @Update("UPDATE contact SET remark = #{remark} WHERE user_id = #{userId} AND contact_user_id = #{contactUserId}")
    int updateRemark(@Param("userId") Long userId, @Param("contactUserId") Long contactUserId, @Param("remark") String remark);
}