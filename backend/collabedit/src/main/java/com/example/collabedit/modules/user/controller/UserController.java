package com.example.collabedit.modules.user.controller;

import com.example.collabedit.modules.user.dto.AvatarUploadResponseDTO;
import com.example.collabedit.modules.user.dto.LoginResponseDTO;
import com.example.collabedit.modules.user.dto.ResetPasswordDTO;
import com.example.collabedit.modules.user.dto.SendVerifyCodeDTO;
import com.example.collabedit.modules.user.dto.UserLoginDTO;
import com.example.collabedit.modules.user.dto.UserRegisterDTO;
import com.example.collabedit.modules.user.dto.UserUpdateDTO;
import com.example.collabedit.modules.user.entity.User;
import com.example.collabedit.modules.user.service.UserService;
import com.example.collabedit.modules.user.service.VerifyCodeService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;
    
    @Resource
    private VerifyCodeService verifyCodeService;

    // 发送验证码接口
    @PostMapping("/send-verify-code")
    public Map<String, String> sendVerifyCode(@Valid @RequestBody SendVerifyCodeDTO dto) {
        String message = verifyCodeService.sendVerifyCode(dto.getReceiver(), dto.getType());
        Map<String, String> result = new HashMap<>();
        result.put("message", message);
        return result;
    }
    
    // 注册接口：增加参数校验
    @PostMapping("/register")
    public User register(@Valid @RequestBody UserRegisterDTO dto) {
        System.out.println("收到注册请求，参数：" + dto); 
        return userService.register(dto);
    }

    // 登录接口：返回令牌
    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody UserLoginDTO dto) {
        return userService.login(dto.getUsername(), dto.getPassword());
    }
    
    // 找回密码接口
    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        String message = userService.resetPassword(dto);
        Map<String, String> result = new HashMap<>();
        result.put("message", message);
        return result;
    }

    // 获取用户信息：增加权限控制（仅登录用户可访问）
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("authentication.principal.id == #id or hasRole('admin')")
    public User getUserInfo(@PathVariable Long id) {
        return userService.getUserById(id);
    }


    @GetMapping("/info/{id}")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getUserInfoById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        Map<String, Object> result = new HashMap<>();
        if (user != null) {
            result.put("username", user.getUsername());
            result.put("avatar", user.getAvatar());
        } else {
            result.put("username", "未知用户");
            result.put("avatar", null);
        }
        return result;
    }

    // 新增：管理员查询所有用户（仅管理员可访问）
    @GetMapping("/all")
    @PreAuthorize("hasRole('admin')")
    public Iterable<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // 新增：修改用户状态（仅管理员可访问）
    @PutMapping("/status/{id}")
    @PreAuthorize("hasRole('admin')")
    public User updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return userService.updateStatus(id, status);
    }

    // 新增：头像上传接口（登录用户可访问）
    @PostMapping("/upload-avatar")
    @PreAuthorize("isAuthenticated()")
    public AvatarUploadResponseDTO uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        // 从认证信息中获取当前登录用户ID
        Long userId = Long.valueOf(authentication.getName());
        return userService.uploadAvatar(userId, file);
    }

    // 新增：个人信息更新接口（登录用户可访问）
    @PutMapping("/update-info")
    @PreAuthorize("isAuthenticated()")
    public User updateUserInfo(
            @Valid @RequestBody UserUpdateDTO dto,
            Authentication authentication) {
        // 从认证信息中获取当前登录用户ID
        Long userId = Long.valueOf(authentication.getName());
        return userService.updateUserInfo(userId, dto);
    }

    // 新增：获取当前登录用户信息（用于前端展示个人中心初始数据）
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public User getCurrentUser(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        User user=userService.getUserById(userId);
        user.setPassword(null);
        return user;
    }

   @PostMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public List<User> getUsersByIds(@RequestBody List<Long> ids) {
        return userService.getUsersByIds(ids);
    }
}