package com.example.collabedit.modules.task;

import com.example.collabedit.modules.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // 查询个人任务（被分配人），支持按状态、截止日期筛选
    @Query("SELECT t FROM Task t WHERE t.assigneeId = :userId " +
            "(:status IS NULL OR t.status = :status) " +
            "(:startDeadline IS NULL OR t.deadline >= :startDeadline) " +
            "(:endDeadline IS NULL OR t.deadline <= :endDeadline) " +
            "ORDER BY t.deadline ASC")
    List<Task> findMyTasks(@Param("userId") Long userId,
                          @Param("status") String status,
                          @Param("startDeadline") LocalDateTime startDeadline,
                          @Param("endDeadline") LocalDateTime endDeadline);

    // 查询文档关联任务，支持按状态、截止日期筛选
    @Query("SELECT t FROM Task t WHERE t.docId = :docId " +
            "(:status IS NULL OR t.status = :status) " +
            "(:startDeadline IS NULL OR t.deadline >= :startDeadline) " +
            "(:endDeadline IS NULL OR t.deadline <= :endDeadline) " +
            "ORDER BY t.deadline ASC")
    List<Task> findTasksByDocId(@Param("docId") Long docId,
                                @Param("status") String status,
                                @Param("startDeadline") LocalDateTime startDeadline,
                                @Param("endDeadline") LocalDateTime endDeadline);

    // 查询截止日期临近（提前1天）的未完成任务
    @Query("SELECT t FROM Task t WHERE t.deadline BETWEEN :now AND :oneDayLater " +
            "AND t.status IN ('pending', 'processing')")
    List<Task> findPendingDeadlineTasks(@Param("now") LocalDateTime now,
                                        @Param("oneDayLater") LocalDateTime oneDayLater);
}