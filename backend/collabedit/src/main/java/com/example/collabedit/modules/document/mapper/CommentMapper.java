package com.example.collabedit.modules.document.mapper;

import com.example.collabedit.modules.document.entity.Comment;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CommentMapper {
    
    //添加文档评论
    @Insert("INSERT INTO comment(doc_id, user_id, content, comment_time, parent_id, position) " +
            "VALUES(#{docId}, #{userId}, #{content}, NOW(), #{parentId}, #{position})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);
    
    //获取文档评论列表
    @Select("SELECT * FROM comment WHERE doc_id = #{docId} ORDER BY comment_time ASC")
    List<Comment> findByDocId(Long docId);
    
    //获取评论的回复列表
    @Select("SELECT * FROM comment WHERE parent_id = #{parentId} ORDER BY comment_time ASC")
    List<Comment> findByParentId(Long parentId);
    
    //获取该评论详情
    @Select("SELECT * FROM comment WHERE id = #{id}")
    Comment findById(Long id);
    
    //删除评论
    @Delete("DELETE FROM comment WHERE id = #{id}")
    int delete(Long id);
}