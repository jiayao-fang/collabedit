package com.example.collabedit.modules.task.service;

import com.example.collabedit.modules.task.dto.TaskCreateDTO;
import com.example.collabedit.modules.task.dto.TaskUpdateStatusDTO;
import com.example.collabedit.modules.task.entity.Task;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskService {
    /**
     * 创建任务（支持多个接收者）
     */
    Task createTask(TaskCreateDTO dto, Long creatorId);

    /**
     * 分配任务（添加接收者）
     */
    Task assignTask(Long taskId, List<Long> assigneeIds, Long operatorId);

    /**
     * 更新任务状态（更新特定接收者的状态）
     */
    Task updateStatus(Long taskId, TaskUpdateStatusDTO dto, Long operatorId);

    /**
     * 查询个人任务（支持按状态、截止日期筛选）
     */
    List<Task> getMyTasks(Long assigneeId, String status, LocalDateTime deadline);

    /**
     * 查询文档关联的任务（支持按状态、截止日期筛选）
     */
    List<Task> getTasksByDocId(Long docId, String status, LocalDateTime deadline);

    /**
     * 根据ID查询任务（包含接收者信息）
     */
    Task getTaskById(Long taskId);
}

