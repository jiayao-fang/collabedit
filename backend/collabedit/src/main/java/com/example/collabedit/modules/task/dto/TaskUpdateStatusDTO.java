package com.example.collabedit.modules.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskUpdateStatusDTO {
    @NotBlank(message = "任务状态不能为空")
    private String status;
    private Long assigneeId;
}

