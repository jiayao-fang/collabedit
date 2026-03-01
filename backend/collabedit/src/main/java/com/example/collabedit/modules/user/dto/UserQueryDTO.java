package com.example.collabedit.modules.user.dto;

import lombok.Data;

@Data
public class UserQueryDTO {
    private Integer page = 1; // 默认第一页
    private Integer size = 10; // 默认每页10条
    private Long roleId; // 角色筛选
    private Integer status; // 状态筛选
}