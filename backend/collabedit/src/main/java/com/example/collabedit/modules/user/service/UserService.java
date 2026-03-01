package com.example.collabedit.modules.user.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.collabedit.common.dto.PageResultDTO;
import com.example.collabedit.modules.user.dto.AvatarUploadResponseDTO;
import com.example.collabedit.modules.user.dto.LoginResponseDTO;
import com.example.collabedit.modules.user.dto.ResetPasswordDTO;
import com.example.collabedit.modules.user.dto.UserRegisterDTO;
import com.example.collabedit.modules.user.dto.UserUpdateDTO;
import com.example.collabedit.modules.user.entity.User;

public interface UserService {
    // 注册
    User register(UserRegisterDTO dto);
    
    // 登录：返回令牌DTO
    LoginResponseDTO login(String username, String password);
    
    User getUserById(Long id);
    
    // 新增：获取所有用户（管理员用）
    Iterable<User> getAllUsers();
    
    // 新增：修改用户状态
    User updateStatus(Long userId, Integer status);

     // 新增：上传头像
    AvatarUploadResponseDTO uploadAvatar(Long userId, MultipartFile file);
    
    // 新增：更新个人信息
    User updateUserInfo(Long userId, UserUpdateDTO dto);

    // 在UserService.java中添加
    List<User> getUsersByIds(List<Long> userIds);

    //获得分页结果
    PageResultDTO<User> getUsersByPage(Integer page, Integer size, Long roleId, Integer status);
    
    // 找回密码
    String resetPassword(ResetPasswordDTO dto);
}