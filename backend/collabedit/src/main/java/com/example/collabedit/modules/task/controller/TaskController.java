package com.example.collabedit.modules.task.controller;

import com.example.collabedit.modules.task.dto.TaskCreateDTO;
import com.example.collabedit.modules.task.dto.TaskUpdateStatusDTO;
import com.example.collabedit.modules.task.entity.Task;
import com.example.collabedit.modules.task.service.TaskService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/task")
@PreAuthorize("isAuthenticated()")
public class TaskController {

    @Resource
    private TaskService taskService;

    /**
     * 创建任务
     */
    @PostMapping
    public Task createTask(@Valid @RequestBody TaskCreateDTO dto, Principal principal) {
        Long creatorId = Long.valueOf(principal.getName());
        return taskService.createTask(dto, creatorId);
    }

    /**
     * 分配任务（添加接收者）
     */
    @PutMapping("/assign/{taskId}")
    public Task assignTask(
            @PathVariable Long taskId,
            @RequestBody List<Long> assigneeIds,
            Principal principal) {
        Long operatorId = Long.valueOf(principal.getName());
        return taskService.assignTask(taskId, assigneeIds, operatorId);
    }

    /**
     * 更新任务状态
     */
    @PutMapping("/status/{taskId}")
    public Task updateStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateStatusDTO dto,
            Principal principal) {
        Long operatorId = Long.valueOf(principal.getName());
        return taskService.updateStatus(taskId, dto, operatorId);
    }

    /**
     * 查询个人任务
     * 支持按状态筛选：PENDING, IN_PROGRESS, COMPLETED
     * 支持按截止日期筛选（查询截止日期早于指定日期的任务）
     */
    @GetMapping("/my")
    public List<Task> getMyTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime deadline,
            Principal principal) {
        Long assigneeId = Long.valueOf(principal.getName());
        return taskService.getMyTasks(assigneeId, status, deadline);
    }

    /**
     * 查询文档关联的任务
     * 支持按状态筛选和截止日期筛选
     */
    @GetMapping("/doc/{docId}")
    public List<Task> getTasksByDocId(
            @PathVariable Long docId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime deadline) {
        return taskService.getTasksByDocId(docId, status, deadline);
    }

    /**
     * 根据ID查询任务
     */
    @GetMapping("/{taskId}")
    public Task getTaskById(@PathVariable Long taskId) {
        return taskService.getTaskById(taskId);
    }
}

