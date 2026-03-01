// src/main/java/com/example/collabedit/modules/user/dto/UserStatusDTO.java
package com.example.collabedit.modules.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "状态值不能为空")
    private Integer status; // 0-禁用 1-启用
}