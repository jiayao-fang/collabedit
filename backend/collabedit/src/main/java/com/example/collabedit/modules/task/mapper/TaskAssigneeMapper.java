package com.example.collabedit.modules.task.mapper;

import com.example.collabedit.modules.task.entity.TaskAssignee;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TaskAssigneeMapper {

    /**
     * 插入任务接收者关联
     */
    @Insert("INSERT INTO task_assignee (task_id, assignee_id, status, create_time) " +
            "VALUES (#{taskId}, #{assigneeId}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(TaskAssignee taskAssignee);

    /**
     * 批量插入任务接收者关联
     */
    @Insert("<script>" +
            "INSERT INTO task_assignee (task_id, assignee_id, status, create_time) VALUES " +
            "<foreach collection='assignees' item='assignee' separator=','>" +
            "(#{taskId}, #{assignee.assigneeId}, #{assignee.status}, NOW())" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("taskId") Long taskId, @Param("assignees") List<TaskAssignee> assignees);

    /**
     * 根据任务ID查询所有接收者
     */
    @Select("SELECT * FROM task_assignee WHERE task_id = #{taskId}")
    List<TaskAssignee> findByTaskId(Long taskId);

    /**
     * 根据接收者ID查询任务
     */
    @Select("<script>" +
            "SELECT ta.* FROM task_assignee ta " +
            "JOIN task t ON ta.task_id = t.id " +
            "WHERE ta.assignee_id = #{assigneeId} " +
            "<if test='status != null and status != \"\"'>" +
            "AND ta.status = #{status} " +
            "</if>" +
            "<if test='deadline != null'>" +
            "AND t.deadline &lt;= #{deadline} " +
            "</if>" +
            "ORDER BY t.deadline ASC, ta.create_time DESC" +
            "</script>")
    List<TaskAssignee> findByAssigneeId(@Param("assigneeId") Long assigneeId,
                                        @Param("status") String status,
                                        @Param("deadline") LocalDateTime deadline);

    /**
     * 更新任务接收者状态
     */
    @Update("<script>" +
            "UPDATE task_assignee SET status = #{status} " +
            "<if test='status == \"COMPLETED\"'>" +
            ", complete_time = NOW() " +
            "</if>" +
            "WHERE task_id = #{taskId} AND assignee_id = #{assigneeId}" +
            "</script>")
    int updateStatus(@Param("taskId") Long taskId,
                    @Param("assigneeId") Long assigneeId,
                    @Param("status") String status);

    /**
     * 删除任务的所有接收者
     */
    @Delete("DELETE FROM task_assignee WHERE task_id = #{taskId}")
    int deleteByTaskId(Long taskId);

    /**
     * 删除任务的指定接收者
     */
    @Delete("DELETE FROM task_assignee WHERE task_id = #{taskId} AND assignee_id = #{assigneeId}")
    int deleteByTaskIdAndAssigneeId(@Param("taskId") Long taskId, @Param("assigneeId") Long assigneeId);

    /**
     * 查询截止日期临近的任务接收者（提前1天，且未发送过通知）
     */
    @Select("SELECT ta.* FROM task_assignee ta " +
            "JOIN task t ON ta.task_id = t.id " +
            "WHERE ta.status IN ('PENDING', 'IN_PROGRESS') " +
            "AND (ta.deadline_notified IS NULL OR ta.deadline_notified = 0) " +
            "AND t.deadline BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 1 DAY)")
    List<TaskAssignee> findTasksNearDeadline();

    /**
     * 根据任务ID和接收者ID查询
     */
    @Select("SELECT * FROM task_assignee WHERE task_id = #{taskId} AND assignee_id = #{assigneeId}")
    TaskAssignee findByTaskIdAndAssigneeId(@Param("taskId") Long taskId, @Param("assigneeId") Long assigneeId);
    
    /**
     * 标记截止日期通知已发送
     */
    @Update("UPDATE task_assignee SET deadline_notified = 1 WHERE task_id = #{taskId} AND assignee_id = #{assigneeId}")
    int markDeadlineNotified(@Param("taskId") Long taskId, @Param("assigneeId") Long assigneeId);
    
    /**
     * 统计任务的完成情况
     */
    @Select("SELECT COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed " +
            "FROM task_assignee WHERE task_id = #{taskId}")
    java.util.Map<String, Object> countTaskProgress(@Param("taskId") Long taskId);
}




