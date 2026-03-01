// src/main/java/com/example/collabedit/modules/contact/mapper/ContactRequestMapper.java
package com.example.collabedit.modules.contact.mapper;

import com.example.collabedit.modules.contact.entity.ContactRequest;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ContactRequestMapper {
    @Insert("INSERT INTO contact_request (sender_id, receiver_id, send_time, status) " +
            "VALUES (#{senderId}, #{receiverId}, NOW(), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ContactRequest request);
    
    @Update("UPDATE contact_request SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    @Select("SELECT * FROM contact_request WHERE receiver_id = #{userId} AND status = 0")
    List<ContactRequest> findPendingByUserId(Long userId);
    
    @Select("SELECT COUNT(*) FROM contact_request WHERE sender_id = #{senderId} AND receiver_id = #{receiverId} AND status = 0")
    int countPendingRequest(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
    
    @Select("SELECT * FROM contact_request WHERE id = #{id}")
    ContactRequest findById(Long id);
}