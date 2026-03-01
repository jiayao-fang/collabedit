package com.example.collabedit.modules.admin.controller;

import com.example.collabedit.common.dto.PageResultDTO;
import com.example.collabedit.modules.user.dto.UserBehaviorDTO;
import com.example.collabedit.modules.user.dto.UserQueryDTO;
import com.example.collabedit.modules.user.dto.UserStatusDTO;
import com.example.collabedit.modules.user.entity.User;
import com.example.collabedit.modules.user.service.UserBehaviorService;
import com.example.collabedit.modules.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('admin')")
public class AdminUserController {

    @Resource
    private UserService userService;

    @Resource
    private UserBehaviorService userBehaviorService;

    /**
     * 用户列表查询（分页+筛选）
     */
    @GetMapping("/users")
    public PageResultDTO<User> getUsers(@Valid UserQueryDTO queryDTO) {
        return userService.getUsersByPage(
                queryDTO.getPage(),
                queryDTO.getSize(),
                queryDTO.getRoleId(),
                queryDTO.getStatus()
        );
    }

    /**
     * 用户状态调整
     */
    @PutMapping("/user/status")
    public User updateUserStatus(@Valid @RequestBody UserStatusDTO dto) {
        return userService.updateStatus(dto.getUserId(), dto.getStatus());
    }

    /**
     * 用户行为分析
     */
    @GetMapping("/user/behavior")
    public List<UserBehaviorDTO> getUserBehavior() {
        return userBehaviorService.getUserBehaviorStatistics();
    }
}