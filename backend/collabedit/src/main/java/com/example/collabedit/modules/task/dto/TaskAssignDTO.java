package com.example.collabedit.modules.task.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class TaskAssignDTO {
    @NotNull(message = "被分配人ID不能为空")
    private Long assigneeId; // 新的被分配人ID
}