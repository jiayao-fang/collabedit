package com.example.collabedit.modules.task.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskCreateDTO {
    /** 关联文档ID（可选） */
    private Long docId;
    
    /** 任务标题 */
    @NotBlank(message = "任务标题不能为空")
    private String title;
    
    /** 任务描述 */
    private String content;
    
    /** 被分配人ID列表 */
    @NotNull(message = "被分配人不能为空")
    @jakarta.validation.constraints.NotEmpty(message = "至少需要选择一个被分配人")
    private java.util.List<Long> assigneeIds;
    
    /** 截止日期 */
    @NotNull(message = "截止日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;
}

