package com.example.collabedit.modules.task.mapper;

import com.example.collabedit.modules.task.entity.Task;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TaskMapper {

    /**
     * 插入任务
     */
    @Insert("INSERT INTO task (doc_id, title, content, creator_id, deadline, create_time) " +
            "VALUES (#{docId}, #{title}, #{content}, #{creatorId}, #{deadline}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Task task);

    /**
     * 根据ID查询任务
     */
    @Select("SELECT * FROM task WHERE id = #{id}")
    Task findById(Long id);

    /**
     * 查询文档关联的任务
     */
    @Select("<script>" +
            "SELECT DISTINCT t.* FROM task t " +
            "LEFT JOIN task_assignee ta ON t.id = ta.task_id " +
            "WHERE t.doc_id = #{docId} " +
            "<if test='status != null and status != \"\"'>" +
            "AND ta.status = #{status} " +
            "</if>" +
            "<if test='deadline != null'>" +
            "AND t.deadline &lt;= #{deadline} " +
            "</if>" +
            "ORDER BY t.deadline ASC, t.create_time DESC" +
            "</script>")
    List<Task> findByDocId(@Param("docId") Long docId,
                           @Param("status") String status,
                           @Param("deadline") LocalDateTime deadline);
}

