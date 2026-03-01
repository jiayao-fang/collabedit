package com.example.collabedit.modules.document.mapper;

import com.example.collabedit.modules.document.entity.DocumentEditor;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DocumentEditorMapper{
    //添加文档编辑者
    @Insert("INSERT INTO document_editor (document_id, user_id, create_time) VALUES (#{documentId}, #{userId}, NOW())")
    int insert(DocumentEditor editor);
    
    //移除文档编辑者
    @Delete("DELETE FROM document_editor WHERE document_id = #{documentId} AND user_id = #{userId}")
    int delete(@Param("documentId") Long documentId, @Param("userId") Long userId);
    
    //根据文档ID获取文档编辑者列表
    @Select("SELECT user_id FROM document_editor WHERE document_id = #{documentId}")
    List<Long> findByDocumentId(Long documentId);
    
    //用于判断用户是否已为编辑者避免重复邀请
    @Select("SELECT COUNT(*) FROM document_editor WHERE document_id = #{documentId} AND user_id = #{userId}")
    int countByDocumentAndUser(@Param("documentId") Long documentId, @Param("userId") Long userId);

}