package com.example.collabedit.modules.task.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskAssignee {
    /** 关联ID */
    private Long id;
    
    /** 任务ID */
    private Long taskId;
    
    /** 被分配人ID */
    private Long assigneeId;
    
    /** 该接收者的任务状态：PENDING-待处理, IN_PROGRESS-处理中, COMPLETED-已完成 */
    private String status;
    
    /** 分配时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;
    
    /** 是否已发送截止日期提醒（0-未发送，1-已发送） */
    private Integer deadlineNotified;
}




