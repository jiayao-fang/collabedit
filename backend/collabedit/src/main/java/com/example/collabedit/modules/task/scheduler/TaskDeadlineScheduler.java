package com.example.collabedit.modules.task.scheduler;

import com.example.collabedit.modules.notification.service.NotificationService;
import com.example.collabedit.modules.task.entity.Task;
import com.example.collabedit.modules.task.entity.TaskAssignee;
import com.example.collabedit.modules.task.mapper.TaskAssigneeMapper;
import com.example.collabedit.modules.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskDeadlineScheduler {

    private final TaskMapper taskMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final NotificationService notificationService;

    /**
     * 定时检查截止日期临近的任务（每1小时执行一次）
     * 提前1天发送通知，且每个任务-接收者组合只发送一次
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // 每小时执行一次
    public void checkTasksNearDeadline() {
        log.info("开始检查截止日期临近的任务");
        
        try {
            // 查询截止日期在1天内且未发送过通知的任务接收者
            List<TaskAssignee> taskAssignees = taskAssigneeMapper.findTasksNearDeadline();
            
            if (taskAssignees.isEmpty()) {
                log.info("没有需要发送截止日期提醒的任务");
                return;
            }
            
            int successCount = 0;
            int failCount = 0;
            
            for (TaskAssignee taskAssignee : taskAssignees) {
                try {
                    // 查询任务信息
                    Task task = taskMapper.findById(taskAssignee.getTaskId());
                    if (task == null) {
                        log.warn("任务不存在，跳过：taskId={}", taskAssignee.getTaskId());
                        continue;
                    }
                    
                    // 发送截止日期临近通知
                    notificationService.sendTaskDeadlineApproachingNotification(
                        taskAssignee.getAssigneeId(),
                        taskAssignee.getTaskId(),
                        task.getTitle(),
                        task.getDocId()
                    );
                    
                    // 标记该任务-接收者已发送通知，避免重复发送
                    taskAssigneeMapper.markDeadlineNotified(
                        taskAssignee.getTaskId(),
                        taskAssignee.getAssigneeId()
                    );
                    
                    successCount++;
                    log.info("已发送任务截止日期临近通知：任务ID={}, 任务标题={}, 被分配人ID={}", 
                        taskAssignee.getTaskId(), task.getTitle(), taskAssignee.getAssigneeId());
                } catch (Exception e) {
                    failCount++;
                    log.error("发送任务截止日期临近通知失败：任务ID={}, 接收者ID={}", 
                        taskAssignee.getTaskId(), taskAssignee.getAssigneeId(), e);
                }
            }
            
            log.info("完成检查截止日期临近的任务，共处理{}个任务接收者，成功{}个，失败{}个", 
                taskAssignees.size(), successCount, failCount);
        } catch (Exception e) {
            log.error("检查截止日期临近的任务时发生错误", e);
        }
    }
}

