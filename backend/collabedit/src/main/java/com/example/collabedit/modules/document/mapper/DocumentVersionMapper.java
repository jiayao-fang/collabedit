package com.example.collabedit.modules.document.mapper;

import com.example.collabedit.modules.document.entity.DocumentVersion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DocumentVersionMapper {
    
    @Insert("INSERT INTO document_version (doc_id, version_number, title, content, content_state, created_by, create_time, change_description, file_size) " +
            "VALUES (#{docId}, #{versionNumber}, #{title}, #{content}, #{contentState}, #{createdBy}, #{createTime}, #{changeDescription}, #{fileSize})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(DocumentVersion version);
    
    @Select("SELECT * FROM document_version WHERE doc_id = #{docId} ORDER BY version_number DESC")
    List<DocumentVersion> findByDocId(Long docId);
    
    @Select("SELECT * FROM document_version WHERE id = #{id}")
    DocumentVersion findById(Long id);
    
    @Select("SELECT * FROM document_version WHERE doc_id = #{docId} AND version_number = #{versionNumber}")
    DocumentVersion findByDocIdAndVersionNumber(@Param("docId") Long docId, @Param("versionNumber") Integer versionNumber);
    
    @Select("SELECT MAX(version_number) FROM document_version WHERE doc_id = #{docId}")
    Integer findMaxVersionNumber(Long docId);
    
    @Delete("DELETE FROM document_version WHERE doc_id = #{docId}")
    void deleteByDocId(Long docId);
    
    @Delete("DELETE FROM document_version WHERE id = #{id}")
    void deleteById(Long id);
    
    @Select("SELECT COUNT(*) FROM document_version WHERE doc_id = #{docId}")
    int countByDocId(Long docId);
}
