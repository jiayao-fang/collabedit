package com.example.collabedit.modules.task.service.impl;

import com.example.collabedit.common.exception.BusinessException;
import com.example.collabedit.modules.notification.service.NotificationService;
import com.example.collabedit.modules.task.dto.TaskCreateDTO;
import com.example.collabedit.modules.task.dto.TaskUpdateStatusDTO;
import com.example.collabedit.modules.task.entity.Task;
import com.example.collabedit.modules.task.entity.TaskAssignee;
import com.example.collabedit.modules.task.mapper.TaskAssigneeMapper;
import com.example.collabedit.modules.task.mapper.TaskMapper;
import com.example.collabedit.modules.task.service.TaskService;
import com.example.collabedit.modules.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private TaskAssigneeMapper taskAssigneeMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private NotificationService notificationService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task createTask(TaskCreateDTO dto, Long creatorId) {
        // 1. 校验截止日期不能早于当前时间
        if (dto.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "截止日期不能早于当前时间");
        }

        // 2. 校验被分配人列表
        if (dto.getAssigneeIds() == null || dto.getAssigneeIds().isEmpty()) {
            throw new BusinessException(400, "至少需要选择一个被分配人");
        }

        // 3. 校验所有被分配人是否存在
        for (Long assigneeId : dto.getAssigneeIds()) {
            if (userMapper.findById(assigneeId) == null) {
                throw new BusinessException(400, "被分配人ID " + assigneeId + " 不存在");
            }
        }

        // 4. 创建任务
        Task task = new Task();
        task.setDocId(dto.getDocId());
        task.setTitle(dto.getTitle());
        task.setContent(dto.getContent());
        task.setCreatorId(creatorId);
        task.setDeadline(dto.getDeadline());

        taskMapper.insert(task);

        // 5. 批量插入任务接收者关联
        List<TaskAssignee> assignees = new ArrayList<>();
        for (Long assigneeId : dto.getAssigneeIds()) {
            TaskAssignee taskAssignee = new TaskAssignee();
            taskAssignee.setTaskId(task.getId());
            taskAssignee.setAssigneeId(assigneeId);
            taskAssignee.setStatus("PENDING");
            assignees.add(taskAssignee);
        }
        taskAssigneeMapper.batchInsert(task.getId(), assignees);

        // 6. 发送任务分配通知给所有接收者
        String creatorName = userMapper.findUsernameById(creatorId);
        if (creatorName == null) {
            creatorName = "未知用户";
        }
        for (Long assigneeId : dto.getAssigneeIds()) {
            notificationService.sendTaskAssignmentNotification(
                assigneeId,
                task.getId(),
                task.getTitle(),
                creatorId,
                creatorName,
                dto.getDocId()
            );
        }

        // 7. 加载接收者信息
        loadTaskAssignees(task);

        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task assignTask(Long taskId, List<Long> assigneeIds, Long operatorId) {
        // 1. 查询任务
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BusinessException(500, "任务不存在");
        }

        // 2. 校验权限（只有创建人可以添加接收者）
        if (!task.getCreatorId().equals(operatorId)) {
            throw new BusinessException(403, "只有任务创建人可以分配任务");
        }

        // 3. 校验被分配人列表
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            throw new BusinessException(400, "至少需要选择一个被分配人");
        }

        // 4. 校验所有被分配人是否存在，并过滤已存在的接收者
        List<Long> newAssigneeIds = new ArrayList<>();
        for (Long assigneeId : assigneeIds) {
            if (userMapper.findById(assigneeId) == null) {
                throw new BusinessException(400, "被分配人ID " + assigneeId + " 不存在");
            }
            // 检查是否已经是接收者
            TaskAssignee existing = taskAssigneeMapper.findByTaskIdAndAssigneeId(taskId, assigneeId);
            if (existing == null) {
                newAssigneeIds.add(assigneeId);
            }
        }

        // 5. 批量插入新的任务接收者关联
        if (!newAssigneeIds.isEmpty()) {
            List<TaskAssignee> assignees = new ArrayList<>();
            for (Long assigneeId : newAssigneeIds) {
                TaskAssignee taskAssignee = new TaskAssignee();
                taskAssignee.setTaskId(taskId);
                taskAssignee.setAssigneeId(assigneeId);
                taskAssignee.setStatus("PENDING");
                assignees.add(taskAssignee);
            }
            taskAssigneeMapper.batchInsert(taskId, assignees);

            // 6. 发送任务分配通知给新添加的接收者
            String operatorName = userMapper.findUsernameById(operatorId);
            if (operatorName == null) {
                operatorName = "未知用户";
            }
            for (Long assigneeId : newAssigneeIds) {
                notificationService.sendTaskAssignmentNotification(
                    assigneeId,
                    taskId,
                    task.getTitle(),
                    operatorId,
                    operatorName,
                    task.getDocId()
                );
            }
        }

        // 7. 加载接收者信息
        loadTaskAssignees(task);

        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task updateStatus(Long taskId, TaskUpdateStatusDTO dto, Long operatorId) {
        // 1. 查询任务
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BusinessException(500, "任务不存在");
        }

        // 2. 判断操作者身份
        boolean isCreator = task.getCreatorId().equals(operatorId);
        TaskAssignee taskAssignee = taskAssigneeMapper.findByTaskIdAndAssigneeId(taskId, operatorId);
        boolean isAssignee = taskAssignee != null;

        // 3. 权限校验：必须是创建者或接收者
        if (!isCreator && !isAssignee) {
            throw new BusinessException(403, "您没有权限更新该任务状态");
        }

        // 4. 确定要更新的接收者ID
        Long targetAssigneeId;
        if (dto.getAssigneeId() != null) {
            // 如果指定了接收者ID，则更新指定接收者的状态
            targetAssigneeId = dto.getAssigneeId();
            
            // 只有创建者可以更新其他人的状态
            if (!isCreator && !targetAssigneeId.equals(operatorId)) {
                throw new BusinessException(403, "您只能更新自己的任务状态");
            }
            
            // 查询目标接收者
            taskAssignee = taskAssigneeMapper.findByTaskIdAndAssigneeId(taskId, targetAssigneeId);
            if (taskAssignee == null) {
                throw new BusinessException(400, "指定的接收者不存在");
            }
        } else {
            // 如果没有指定接收者ID，则更新操作者自己的状态
            if (!isAssignee) {
                throw new BusinessException(403, "您不是该任务的接收者");
            }
            targetAssigneeId = operatorId;
        }

        // 5. 校验状态流转规则
        String currentStatus = taskAssignee.getStatus();
        String newStatus = dto.getStatus();

        // 已完成的任务不能再改为其他状态（创建者除外）
        if ("COMPLETED".equals(currentStatus) && !"COMPLETED".equals(newStatus) && !isCreator) {
            throw new BusinessException(400, "已完成的任务不能再修改状态");
        }

        // 6. 更新该接收者的状态
        taskAssigneeMapper.updateStatus(taskId, targetAssigneeId, newStatus);
        taskAssignee.setStatus(newStatus);
        if ("COMPLETED".equals(newStatus)) {
            taskAssignee.setCompleteTime(LocalDateTime.now());
        }

        // 7. 发送状态变更通知
        String operatorName = userMapper.findUsernameById(operatorId);
        if (operatorName == null) {
            operatorName = "未知用户";
        }
        
        // 如果操作者不是创建者，通知创建者
        if (!isCreator) {
            notificationService.sendTaskStatusChangeNotification(
                task.getCreatorId(),
                taskId,
                task.getTitle(),
                newStatus,
                operatorId,
                operatorName,
                task.getDocId()
            );
        }
        
        // 如果操作者是创建者且更新的是其他人的状态，通知被更新的接收者
        if (isCreator && !targetAssigneeId.equals(operatorId)) {
            notificationService.sendTaskStatusChangeNotification(
                targetAssigneeId,
                taskId,
                task.getTitle(),
                newStatus,
                operatorId,
                operatorName,
                task.getDocId()
            );
        }

        // 8. 检查是否所有被分配人都已完成任务
        if ("COMPLETED".equals(newStatus)) {
            java.util.Map<String, Object> progress = taskAssigneeMapper.countTaskProgress(taskId);
            Long total = ((Number) progress.get("total")).longValue();
            Long completed = ((Number) progress.get("completed")).longValue();
            
            // 如果所有人都完成了，发送任务全部完成通知
            if (total.equals(completed)) {
                notificationService.sendTaskAllCompletedNotification(
                    task.getCreatorId(),
                    taskId,
                    task.getTitle(),
                    task.getDocId()
                );
            }
        }

        // 9. 加载接收者信息
        loadTaskAssignees(task);

        return task;
    }

    @Override
    public List<Task> getMyTasks(Long assigneeId, String status, LocalDateTime deadline) {
        // 通过TaskAssigneeMapper查询
        List<TaskAssignee> taskAssignees = taskAssigneeMapper.findByAssigneeId(assigneeId, status, deadline);
        
        // 转换为Task列表
        List<Task> tasks = new ArrayList<>();
        for (TaskAssignee ta : taskAssignees) {
            Task task = taskMapper.findById(ta.getTaskId());
            if (task != null) {
                // 设置该接收者的状态
                task.setAssigneeIds(List.of(ta.getAssigneeId()));
                tasks.add(task);
            }
        }
        
        return tasks;
    }

    @Override
    public List<Task> getTasksByDocId(Long docId, String status, LocalDateTime deadline) {
        List<Task> tasks = taskMapper.findByDocId(docId, status, deadline);
        
        // 为每个任务加载接收者信息
        for (Task task : tasks) {
            loadTaskAssignees(task);
        }
        
        return tasks;
    }

    @Override
    public Task getTaskById(Long taskId) {
        Task task = taskMapper.findById(taskId);
        if (task != null) {
            loadTaskAssignees(task);
        }
        return task;
    }

    /**
     * 加载任务的接收者信息
     */
    private void loadTaskAssignees(Task task) {
        if (task == null || task.getId() == null) {
            return;
        }
        
        List<TaskAssignee> assignees = taskAssigneeMapper.findByTaskId(task.getId());
        task.setAssignees(assignees);
        
        if (assignees != null && !assignees.isEmpty()) {
            List<Long> assigneeIds = assignees.stream()
                .map(TaskAssignee::getAssigneeId)
                .collect(Collectors.toList());
            task.setAssigneeIds(assigneeIds);
            
            // 计算任务整体状态和进度
            calculateTaskProgress(task, assignees);
        }
    }
    
    /**
     * 计算任务的整体状态和完成进度
     */
    private void calculateTaskProgress(Task task, List<TaskAssignee> assignees) {
        if (assignees == null || assignees.isEmpty()) {
            task.setOverallStatus("PENDING");
            task.setProgress("0/0");
            return;
        }
        
        long total = assignees.size();
        long completed = assignees.stream()
            .filter(a -> "COMPLETED".equals(a.getStatus()))
            .count();
        long inProgress = assignees.stream()
            .filter(a -> "IN_PROGRESS".equals(a.getStatus()))
            .count();
        
        // 设置进度
        task.setProgress(completed + "/" + total);
        
        // 设置整体状态
        if (completed == total) {
            task.setOverallStatus("ALL_COMPLETED");
        } else if (completed > 0) {
            task.setOverallStatus("PARTIAL_COMPLETED");
        } else if (inProgress > 0) {
            task.setOverallStatus("IN_PROGRESS");
        } else {
            task.setOverallStatus("PENDING");
        }
    }
}
