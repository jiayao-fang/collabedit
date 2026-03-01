package com.example.collabedit.modules.system.dto;

import lombok.Data;
import java.util.List;

@Data
public class PermissionAssignDTO {
    private Long roleId;
    private List<Long> permIds;
}