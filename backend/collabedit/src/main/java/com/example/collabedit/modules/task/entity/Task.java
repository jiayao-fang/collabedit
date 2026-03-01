package com.example.collabedit.modules.task.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Task {
    /** 任务ID */
    private Long id;
    
    /** 关联文档ID（可选，任务可独立） */
    private Long docId;
    
    /** 任务标题 */
    private String title;
    
    /** 任务描述 */
    private String content;
    
    /** 创建人ID */
    private Long creatorId;
    
    /** 截止日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;
    
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /** 被分配人ID列表（用于查询结果） */
    private java.util.List<Long> assigneeIds;
    
    /** 被分配人信息列表（用于查询结果） */
    private java.util.List<TaskAssignee> assignees;
    
    /** 任务整体状态（计算得出）：ALL_COMPLETED-全部完成, PARTIAL_COMPLETED-部分完成, IN_PROGRESS-进行中, PENDING-待处理 */
    private String overallStatus;
    
    /** 完成进度（计算得出）：已完成人数/总人数 */
    private String progress;
}

