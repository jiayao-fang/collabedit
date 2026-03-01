package com.example.collabedit.modules.document.controller;

import com.example.collabedit.modules.document.DocumentRepository;
import com.example.collabedit.modules.document.dto.DocCategoryDTO;
import com.example.collabedit.modules.document.dto.DocumentUpdateDTO;
import com.example.collabedit.modules.document.entity.Document;
import com.example.collabedit.modules.document.entity.Tag;
import com.example.collabedit.modules.document.mapper.DocumentMapper;
import com.example.collabedit.modules.document.entity.Folder;
import com.example.collabedit.modules.document.service.DocumentService;
import com.example.collabedit.modules.document.service.FolderService;
import com.example.collabedit.modules.document.websocket.DocumentWebSocketServer;
import com.example.collabedit.modules.notification.service.NotificationService;
import com.example.collabedit.modules.system.annotation.LogOperation;
import com.example.collabedit.modules.system.service.PermissionService;
import com.example.collabedit.modules.user.mapper.UserMapper;
import com.example.collabedit.security.JwtUtil;
import com.example.collabedit.common.exception.BusinessException;
import com.example.collabedit.common.enums.Result;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Base64;


@RestController
@RequestMapping("/api/doc")
@Slf4j
public class DocumentController {

    @Resource
    private DocumentService service;

    @Resource
    private PermissionService permissionService;

    @Resource
    private FolderService folderService;

    @Resource
    private DocumentMapper documentMapper;

    @Resource
    private JwtUtil jwtUtil;

     @Autowired
    private DocumentRepository documentRepository;
     // 辅助方法：判断用户是否为管理员
     private boolean isAdmin(Authentication authentication) {
        // 从Authentication中获取权限列表，判断是否包含ROLE_admin
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_admin"));
    }

     // 根据作者ID查询作者名
    @GetMapping("/author/{userId}")
    public String getAuthorName(@PathVariable Long userId) {
        String username = documentMapper.getUsernameById(userId);
        return username != null ? username : "未知";
    }

    // 根据文件夹ID查询文件夹名
    @GetMapping("/folder/{folderId}")
    public String getFolderName(@PathVariable Long folderId) {
        String folderName = documentMapper.getFolderNameById(folderId);
        return folderName != null ? folderName : "未分类";
    }

     // 批量查询标签（减少请求次数）
    @GetMapping("/tags/{tagIds}")
    public List<Tag> getTagNames(@PathVariable String tagIds) {
        List<Tag> tagList = new ArrayList<>();
        if (tagIds != null && !tagIds.isEmpty()) {
            String[] tagIdArr = tagIds.split(",");
            for (String tagIdStr : tagIdArr) {
                try {
                    Long tagId = Long.parseLong(tagIdStr);
                    Tag tag = documentMapper.getTagById(tagId);
                    if (tag != null) {
                        tagList.add(tag);
                    }
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
        return tagList;
    }

    //创建文档
   @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "创建文档", resource = "document")
    public Document create(@RequestBody Document doc, Authentication authentication) {
        Long authorId = Long.valueOf(authentication.getName());
        doc.setAuthorId(authorId);
        // 默认可见性设为仅自己可见
        if (doc.getVisibility() == null) {
            doc.setVisibility(0);
        }
        return service.create(doc);
    }

    // 获取文档接口
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "查看文档", resource = "document")
    public Document get(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        boolean admin = isAdmin(authentication);
        return service.getByIdWithPermission(id, userId, admin);
    }

     @GetMapping("/read/{id}")
    public Result<?> getDoc(@PathVariable Long id) {
        Optional<Document> docOptional = documentRepository.findById(id);
        if (!docOptional.isPresent()) {
            return Result.error("文档不存在");
        }
        Document doc = docOptional.get();

        // 构造返回数据（适配前端所需字段）
        return Result.success(new Document() {
            {
                setId(doc.getId());
                setTitle(doc.getTitle());
                setContent(doc.getContent()); // 原有HTML内容
                setContentState(doc.getContentState()); // Yjs内容
                setFolderId(doc.getFolderId());
                setTagIds(doc.getTagIds());
                setVisibility(doc.getVisibility());
                setVersion(doc.getVersion()); // 乐观锁版本号
                setEditCount(doc.getEditCount());
            }
        });
    }

   // 获取所有文档
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "查看所有文档", resource = "document")
    public List<Document> getAll(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        boolean admin = isAdmin(authentication);
        return service.getAllWithPermission(userId, admin);
    }


    //手动保存逻辑
    @PostMapping("/edit/{id}")
    public Result<?> editDoc(@PathVariable Long id, @RequestBody DocumentUpdateDTO dto, HttpServletRequest request) {
        // 1. 解析token获取用户ID
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error(401, "登录已过期，请重新登录");
        }

        token = token.substring(7); // 去掉 "Bearer " 前缀

        String userIdStr = jwtUtil.extractUserId(token);
        Long userId = Long.parseLong(userIdStr);

        if (!jwtUtil.validateToken(token, userIdStr)) {
        return Result.error(401, "登录已过期，请重新登录");
}

        // 2. 查询文档
        Optional<Document> docOptional = documentRepository.findById(id);
        if (!docOptional.isPresent()) {
            return Result.error("文档不存在");
        }
        Document doc = docOptional.get();
        // 3. 乐观锁校验
        if (!doc.getVersion().equals(dto.getVersion())) {
            return Result.error("文档已被修改，请刷新重试");
        }

        // 4. 复制前端提交的元数据
        BeanUtils.copyProperties(dto, doc, "id", "createBy", "createTime", "version");

        // 更新元数据
        doc.setUpdateBy(userId); // 更新人ID
        doc.setVersion(doc.getVersion() + 1); // 版本号+1（乐观锁）
        
        // 5. 接收前端传递的Yjs内容状态并保存
         if (dto.getContentState() != null && !dto.getContentState().isEmpty()) {
        try {
            // 将Base64字符串转为byte[]
            byte[] contentStateBytes = Base64.getDecoder().decode(dto.getContentState());
            doc.setContentState(contentStateBytes);
        } catch (IllegalArgumentException e) {
            return Result.error("内容状态格式错误，请重试");
        }
    }
        // 兼容原有HTML内容
        if (dto.getContent() != null) {
            doc.setContent(dto.getContent());
        }
        
        // 7. 保存到数据库
        documentRepository.save(doc);
        return Result.success("文档保存成功！");
    }

    // 自动保存逻辑，实时更新文档Yjs状态接口
    @PutMapping("/update-yjs-state/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<?> updateYjsState(@PathVariable Long id, @RequestBody DocumentUpdateDTO dto, HttpServletRequest request) {
        // 1. 解析token获取用户ID
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error(401, "登录已过期，请重新登录");
        }

        token = token.substring(7); // 去掉 "Bearer " 前缀

        String userIdStr = jwtUtil.extractUserId(token);
        Long userId = Long.parseLong(userIdStr);

        if (!jwtUtil.validateToken(token, userIdStr)) {
            return Result.error(401, "登录已过期，请重新登录");
        }

        // 2. 查询文档
        Optional<Document> docOptional = documentRepository.findById(id);
        if (!docOptional.isPresent()) {
            return Result.error("文档不存在");
        }
        Document doc = docOptional.get();
        
        // 3. 检查用户是否有编辑权限
        boolean hasPermission = service.isEditor(id, userId) || doc.getAuthorId().equals(userId);
        if (!hasPermission) {
            return Result.error(403, "无权限编辑此文档");
        }

        // 4. 更新Yjs内容状态
        if (dto.getContentState() != null && !dto.getContentState().isEmpty()) {
            try {
                // 将Base64字符串转为byte[]
                byte[] contentStateBytes = Base64.getDecoder().decode(dto.getContentState());
                doc.setContentState(contentStateBytes);
            } catch (IllegalArgumentException e) {
                return Result.error("内容状态格式错误，请重试");
            }
        }
        
        // 5. 兼容原有HTML内容
        if (dto.getContent() != null) {
            doc.setContent(dto.getContent());
        }
        
        // 6. 更新更新时间，但不增加版本号（避免与编辑冲突）
        doc.setUpdateTime(LocalDateTime.now());
        doc.setUpdateBy(userId);
        
        // 7. 保存到数据库
        documentRepository.save(doc);

        DocumentWebSocketServer.broadcastUpdateToDoc(id.toString(), doc.getContent(), doc.getContentState());
        
        return Result.success("Yjs状态更新成功！");
    }


    // 删除文档，严格控制权限
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public String delete(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        boolean isAdmin = isAdmin(authentication);
        
        // 验证权限：只有作者或管理员可以删除
        Document doc = service.getById(id);
        if (!doc.getAuthorId().equals(userId) && !isAdmin) {
            throw new RuntimeException("无权限删除此文档");
        }
        
        service.delete(id);
        return "success";
    }

     // 永久删除文档，严格控制权限
    @DeleteMapping("/delPer/{id}")
    @PreAuthorize("isAuthenticated()")
    public String delPer(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        boolean isAdmin = isAdmin(authentication);   
        // 验证权限：只有作者或管理员可以删除
        Document doc = service.getById(id);
        if (!doc.getAuthorId().equals(userId) && !isAdmin) {
            throw new RuntimeException("无权限删除此文档");
        }
        
        service.delPer(id);
        return "success";
    }

    // 更新文档可见性接口
    @PutMapping("/visibility/{docId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "更新文档可见性", resource = "document")
    public String updateVisibility(
            @PathVariable Long docId,
            @RequestParam Integer visibility,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        boolean isAdmin = isAdmin(authentication);
        service.updateVisibility(docId, visibility, userId, isAdmin);
        return "success";
    }

    // 添加文档编辑者接口
    @PostMapping("/addEditor/{docId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "添加文档编辑者", resource = "document")
    public String addEditor(
            @PathVariable Long docId,
            @RequestParam Long userId,
            Authentication authentication) {
        Long operatorId = Long.valueOf(authentication.getName());
        service.addEditor(docId, userId, operatorId);
        return "success";
    }

      // 移除文档编辑者接口
    @DeleteMapping("/delEditor/{docId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "移除文档编辑者", resource = "document")
    public String removeEditor(
            @PathVariable Long docId,
            @RequestParam Long editorId,
            Authentication authentication) {
        Long operatorId = Long.valueOf(authentication.getName());
        service.removeEditor(docId, editorId, operatorId);
        return "success";
    }

    // 获取文档编辑者列表接口
    @GetMapping("/editors/{docId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "获取文档编辑者", resource = "document")
    public List<Long> getEditors(
            @PathVariable Long docId,
            Authentication authentication) {
        Long myId = Long.valueOf(authentication.getName());
        boolean isAdmin = isAdmin(authentication);
        
        // 验证是否有权限查看
        Document doc = service.getByIdWithPermission(docId, myId, isAdmin);
        if (doc == null) {
            throw new RuntimeException("无权限查看此文档的编辑者");
        }
        
        return service.getEditors(docId);
    }

     // 查询回收站
    @GetMapping("/recycle")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "查看回收站", resource = "document")
    public List<Document> getRecycle(Authentication authentication) {
        Long authorId = Long.valueOf(authentication.getName());
        return service.getRecycleByAuthor(authorId);
    }

    // 恢复文档
    @PutMapping("/recover/{docId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "恢复文档", resource = "document")
    public String recover(@PathVariable Long docId, Authentication authentication) {
        Long authorId = Long.valueOf(authentication.getName());
        // 校验所有权
        Document doc = service.getById(docId); // 注意：这里需要修改getById支持查询已删除文档，或新增方法
        if (!doc.getAuthorId().equals(authorId)) {
            throw new RuntimeException("无权限恢复此文档");
        }
        service.recover(docId);
        return "success";
    }

     // 按标签ID分类文档     
    @GetMapping("/categoryByTag")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocCategoryDTO>> categoryByTag(@RequestParam Long tagId) {
        try {
            List<DocCategoryDTO> docList = service.listByTagId(tagId);
            return ResponseEntity.ok(docList);
        } catch (IllegalArgumentException e) {
            log.error("按标签分类查询失败：{}", e.getMessage());
            Result<?> result = Result.error(e.getMessage());
            throw new BusinessException(result.getCode(), e.getMessage()); // 替换为你的业务异常类
        }
    }

    //按文件夹分类查询
    @GetMapping("/categoryByFolder")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocCategoryDTO>> categoryByFolder(@RequestParam Long folderId) {
        try {
            List<DocCategoryDTO> docList = service.listByFolderId(folderId);
            return ResponseEntity.ok(docList);
        } catch (IllegalArgumentException e) {
            log.error("按文件夹分类查询失败：{}", e.getMessage());
            Result<?> result = Result.error(e.getMessage());
            throw new BusinessException(result.getCode(), result.getMessage()); 
        }
    }

     /**
     * 全文搜索
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "全文搜索文档", resource = "document")
    public List<Document> search(@RequestParam String keyword) {
        return service.search(keyword);
    }
    
    /**
     * 高级搜索
     */
    @GetMapping("/advancedSearch")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "高级搜索文档", resource = "document")
    public List<Document> advancedSearch(
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updateTime) {
        return service.advancedSearch(authorId, createTime, updateTime);
    }
    
    //添加文档到文件夹
    @PutMapping("/{docId}/folder")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "添加文章到文件夹", resource = "document")
    public String addDocToFolder(
            @PathVariable Long docId,
            @RequestParam Long folderId,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        boolean isAdmin = isAdmin(authentication);
        
        // 验证权限
        Document doc = service.getByIdWithPermission(docId, userId, isAdmin);
        Folder folder = folderService.getByIdAndCreator(folderId, userId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在或无权限");
        }
        
        service.updateDocumentFolder(docId, folderId);
        return "success";
    }
    
    //添加文档到标签
    @PostMapping("/{docId}/tags")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "为文章添加标签", resource = "document")
    public String addTagsToDocument(
            @PathVariable Long docId,
            @RequestParam List<Long> tagIds,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        boolean isAdmin = isAdmin(authentication);
        
        // 验证权限
        Document doc = service.getByIdWithPermission(docId, userId, isAdmin);
        service.addTagsToDocument(docId, tagIds);
        return "success";
    }

    //获取文档所在的文件夹
    @GetMapping("/{docId}/inFolder")
    @PreAuthorize("isAuthenticated()")
    public Folder getDocumentFolder(
            @PathVariable Long docId,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        boolean isAdmin = isAdmin(authentication);
        return service.getDocumentFolder(docId, userId, isAdmin);
    }

    //获取文档所在的标签
    @GetMapping("/{docId}/inTags")
    @PreAuthorize("isAuthenticated()")
    public List<Tag> getDocumentTags(
            @PathVariable Long docId,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        boolean isAdmin = isAdmin(authentication);
        return service.getDocumentTags(docId, userId, isAdmin);
    }

    //邀请用户编辑文档
    @PostMapping("/invite-from-contacts/{docId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "从联系人邀请编辑者", resource = "document")
    public String inviteFromContacts(
            @PathVariable Long docId,
            @RequestParam Long userId,
            Authentication authentication,
            @Autowired NotificationService notificationService,
            @Autowired UserMapper userMapper) {
        Long operatorId = Long.valueOf(authentication.getName());
        service.addEditor(docId, userId, operatorId);
        
        // 发送邀请通知
        Document doc = service.getById(docId);
        String inviterName = userMapper.findUsernameById(operatorId);
        notificationService.sendEditInvitationNotification(userId, docId, doc.getTitle(), operatorId, inviterName);
        
        return "邀请已发送";
    }

    // 锁定文档
    @PostMapping("/lock/{docId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "锁定文档", resource = "document")
    public Result<String> lockDocument(
            @PathVariable Long docId,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        service.lockDocument(docId, userId);
        return Result.success("文档已锁定");
    }
    
     // 解锁文档
    @PostMapping("/unlock/{docId}")
    @PreAuthorize("isAuthenticated()")
    @LogOperation(value = "解锁文档", resource = "document")
    public Result<String> unlockDocument(
            @PathVariable Long docId,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        service.unlockDocument(docId, userId);
        return Result.success("文档已解锁");
    }

     // 检查文档是否被锁定
    @GetMapping("/lock-status/{docId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> isDocumentLocked(@PathVariable Long docId) {
        boolean locked = service.isDocumentLocked(docId);
        return Result.success(locked);
    }
}