package com.example.collabedit.modules.user.service.impl;

import com.example.collabedit.common.dto.PageResultDTO;
import com.example.collabedit.modules.user.dto.AvatarUploadResponseDTO;
import com.example.collabedit.modules.user.dto.LoginResponseDTO;
import com.example.collabedit.modules.user.dto.ResetPasswordDTO;
import com.example.collabedit.modules.user.dto.UserRegisterDTO;
import com.example.collabedit.modules.user.dto.UserUpdateDTO;
import com.example.collabedit.modules.user.entity.User;
import com.example.collabedit.modules.user.mapper.UserMapper;
import com.example.collabedit.modules.user.service.UserService;
import com.example.collabedit.security.JwtUtil;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;
    
    // 注入密码加密器
    @Resource
    private PasswordEncoder passwordEncoder;
    
    // 注入JWT工具类
    @Resource
    private JwtUtil jwtUtil;
    
    // 注入验证码服务
    @Resource
    private com.example.collabedit.modules.user.service.VerifyCodeService verifyCodeService;
    
    // 注入用户状态服务
    @Resource
    private com.example.collabedit.modules.user.service.UserStatusService userStatusService;

     // 头像上传路径
    private final String AVATAR_UPLOAD_PATH = System.getProperty("user.dir") + "/uploads/avatars/";
    
    // 头像访问基础URL
    private final String AVATAR_BASE_URL = "/uploads/avatars/";


    @Override
    public User register(UserRegisterDTO dto) {
        // 1. 验证验证码
        String receiver = "phone".equals(dto.getRegisterType()) ? dto.getPhone() : dto.getEmail();
        if (!verifyCodeService.verifyCode(receiver, dto.getVerifyCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }
        
        // 2. 检查用户名是否已存在
        if (userMapper.findByUsername(dto.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 3. 检查邮箱/手机号是否已存在
        if ("email".equals(dto.getRegisterType())) {
            if (dto.getEmail() == null || dto.getEmail().isEmpty()) {
                throw new RuntimeException("邮箱不能为空");
            }
            if (userMapper.findByEmail(dto.getEmail()) != null) {
                throw new RuntimeException("邮箱已注册");
            }
        } else if ("phone".equals(dto.getRegisterType())) {
            if (dto.getPhone() == null || dto.getPhone().isEmpty()) {
                throw new RuntimeException("手机号不能为空");
            }
            if (userMapper.findByPhone(dto.getPhone()) != null) {
                throw new RuntimeException("手机号已注册");
            }
        }

        // 4. 转换DTO为实体
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setAvatar("/assets/avatars/default-avatar.jpg"); 

        // 5. 保存用户
        int userInsertCount = userMapper.register(user);
        if (userInsertCount > 0 && user.getId() != null) {
            // 向user_role表插入数据，roleId固定为2
            userMapper.insertUserRole(user.getId(), 2);
        }
        return userMapper.findByUsername(dto.getUsername());
    }

    @Override
    public LoginResponseDTO login(String username, String password) {
        // 1. 查询用户
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名不存在");
        }
        
        // 2. 检查状态
        if (user.getStatus() != 1) {
            throw new RuntimeException("账号已禁用");
        }
        
        // 3. 密码校验和查询角色
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        String roleName = userMapper.findRoleNameByUserId(user.getId());
        if (roleName == null) {     
            throw new RuntimeException("该用户未分配角色");
        }

        // 4. 生成JWT令牌
        String token = jwtUtil.generateToken(user.getId().toString(),roleName);
        
        // 5. 构建响应DTO（脱敏处理）
        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        
        LoginResponseDTO.UserInfoDTO userInfo = new LoginResponseDTO.UserInfoDTO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setStatus(user.getStatus());
        userInfo.setRole(roleName);
        response.setUserInfo(userInfo);
    
        return response;
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 密码脱敏
        user.setPassword(null);
        return user;
    }

    @Override
    public Iterable<User> getAllUsers() {
        Iterable<User> users = userMapper.findAll();
        // 密码脱敏
        users.forEach(user -> user.setPassword(null));
        return users;
    }

    @Override
    public User updateStatus(Long userId, Integer status) {
        if (status != 0 && status != 1) {
            throw new RuntimeException("状态值只能是0或1");
        }
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新数据库状态
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateStatus(userId, status);
        
        // 如果是禁用操作，强制用户下线
        if (status == 0) {
            userStatusService.forceUserOffline(userId);
            System.out.println("用户 " + userId + " 已被禁用并强制下线");
        }
        
        return user;
    }
     @Override
    public AvatarUploadResponseDTO uploadAvatar(Long userId, MultipartFile file) {
        // 1. 验证用户是否存在
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 验证文件是否为空
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        
        // 3. 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("请上传图片文件");
        }
        
        // 4. 确保上传目录存在
        File uploadDir = new File(AVATAR_UPLOAD_PATH);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        // 5. 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + fileExtension;
        
        // 6. 保存文件
        try {
            File dest = new File(AVATAR_UPLOAD_PATH + fileName);
            file.transferTo(dest);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
        
        // 7. 更新用户头像URL
        String avatarUrl = AVATAR_BASE_URL + fileName;
        user.setAvatar(avatarUrl);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateAvatar(user);
        
        // 8. 构建响应
        AvatarUploadResponseDTO response = new AvatarUploadResponseDTO();
        response.setAvatarUrl(avatarUrl);
        response.setMessage("头像上传成功");
        
        return response;
    }
    
    @Override
    public User updateUserInfo(Long userId, UserUpdateDTO dto) {
        // 1. 验证用户是否存在
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 检查用户名是否已被占用（如果修改了用户名）
        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {
            User existingUser = userMapper.findByUsername(dto.getUsername());
            if (existingUser != null) {
                throw new RuntimeException("用户名已存在");
            }
        }
        
        // 3. 检查邮箱是否已被占用（如果修改了邮箱）
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            User existingUser = userMapper.findByEmail(dto.getEmail());
            if (existingUser != null) {
                throw new RuntimeException("邮箱已被注册");
            }
        }
        
        // 4. 更新用户信息
        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getSignature() != null) {
            user.setSignature(dto.getSignature());
        }
        
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateUserInfo(user);
        
        // 5. 密码脱敏后返回
        user.setPassword(null);
        return user;
    }
    
    @Override
    public PageResultDTO<User> getUsersByPage(Integer page, Integer size, Long roleId, Integer status) {
       // 1. 分页参数校验（避免负数/0）
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10; // 限制最大每页100条

        // 2. 计算起始位置（offset = (页码-1) * 每页条数）
        int offset = (page - 1) * size;

        // 3. 执行查询：分页数据 + 总条数
        List<User> userList = userMapper.findUsersByPage(roleId, status, offset, size);
        long total = userMapper.countUsers(roleId, status);

        // 密码脱敏
        userList.forEach(user -> user.setPassword(null));
        
        // 构建分页结果
        PageResultDTO<User> result = new PageResultDTO<>();
            result.setList(userList);
            result.setTotal(total);
            result.setPage(page);
            result.setSize(size);
            result.setPages((total + size - 1) / size); // 计算总页数
            return result;
    }

    @Override
    public List<User> getUsersByIds(List<Long> userIds) {
        List<User> users = userMapper.findUsersByIds(userIds);
        // 密码脱敏处理
        users.forEach(user -> user.setPassword(null));
        return users;
    }

    @Override
    public String resetPassword(ResetPasswordDTO dto) {
        // 1. 验证验证码
        if (!verifyCodeService.verifyCode(dto.getReceiver(), dto.getVerifyCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }
        
        // 2. 根据手机号或邮箱查找用户
        User user = null;
        if ("phone".equals(dto.getResetType())) {
            user = userMapper.findByPhone(dto.getReceiver());
            if (user == null) {
                throw new RuntimeException("该手机号未注册");
            }
        } else if ("email".equals(dto.getResetType())) {
            user = userMapper.findByEmail(dto.getReceiver());
            if (user == null) {
                throw new RuntimeException("该邮箱未注册");
            }
        }
        
        // 3. 更新密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updatePassword(user);
        
        return "密码重置成功";
    }

}