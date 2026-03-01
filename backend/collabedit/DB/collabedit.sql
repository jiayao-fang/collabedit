## 一、用户管理模块

### 1. 用户表 (user)
```sql
CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `email` VARCHAR(100) COMMENT '邮箱',
    `phone` VARCHAR(11) COMMENT '手机号',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `signature` VARCHAR(500) COMMENT '个性签名',
    `status` INT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_username` (`username`),
    INDEX `idx_email` (`email`),
    INDEX `idx_phone` (`phone`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 2. 角色表 (role)
```sql
CREATE TABLE `role` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    `role_name` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称（admin/user）',
    `role_desc` VARCHAR(255) COMMENT '角色描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';
```

### 3. 权限表 (permission)
```sql
CREATE TABLE `permission` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    `perm_name` VARCHAR(50) NOT NULL COMMENT '权限名称',
    `perm_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '权限标识（doc:view）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_perm_key` (`perm_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';
```

### 4. 用户角色关联表 (user_role)
```sql
CREATE TABLE `user_role` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_role_id` (`role_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`role_id`) REFERENCES `role`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
```

### 5. 角色权限关联表 (role_permission)
```sql
CREATE TABLE `role_permission` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `perm_id` BIGINT NOT NULL COMMENT '权限ID',
    UNIQUE KEY `uk_role_perm` (`role_id`, `perm_id`),
    INDEX `idx_role_id` (`role_id`),
    INDEX `idx_perm_id` (`perm_id`),
    FOREIGN KEY (`role_id`) REFERENCES `role`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`perm_id`) REFERENCES `permission`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';
```

## 二、文档管理模块

### 6. 文档表 (document)
```sql
CREATE TABLE `document` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
    `title` VARCHAR(255) NOT NULL COMMENT '文档标题',
    `content` LONGTEXT COMMENT 'HTML内容',
    `content_state` LONGBLOB COMMENT 'Yjs序列化状态',
    `author_id` BIGINT NOT NULL COMMENT '作者ID',
    `folder_id` BIGINT COMMENT '文件夹ID',
    `tag_ids` VARCHAR(500) COMMENT '标签ID列表（逗号分隔）',
    `version` INT DEFAULT 1 COMMENT '版本号（乐观锁）',
    `edit_count` INT DEFAULT 0 COMMENT '编辑次数',
    `visibility` INT DEFAULT 0 COMMENT '可见性：0-私有，1-编辑者，2-公开',
    `is_locked` INT DEFAULT 0 COMMENT '是否锁定：0-未锁定，1-已锁定',
    `locked_by` BIGINT COMMENT '锁定者ID',
    `locked_at` DATETIME COMMENT '锁定时间',
    `is_delete` INT DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `update_by` BIGINT COMMENT '最后更新者ID',
    INDEX `idx_author_id` (`author_id`),
    INDEX `idx_folder_id` (`folder_id`),
    INDEX `idx_is_delete` (`is_delete`),
    INDEX `idx_update_time` (`update_time`),
    FULLTEXT INDEX `ft_title_content` (`title`, `content`),
    FOREIGN KEY (`author_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';
```

### 7. 文档版本表 (document_version)
```sql
CREATE TABLE `document_version` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '版本ID',
    `doc_id` BIGINT NOT NULL COMMENT '文档ID',
    `version_number` INT NOT NULL COMMENT '版本号',
    `title` VARCHAR(255) NOT NULL COMMENT '标题快照',
    `content` LONGTEXT COMMENT '内容快照',
    `content_state` LONGBLOB COMMENT 'Yjs状态快照',
    `created_by` BIGINT NOT NULL COMMENT '创建者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `change_description` VARCHAR(500) COMMENT '变更描述',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    INDEX `idx_doc_id` (`doc_id`),
    INDEX `idx_version_number` (`doc_id`, `version_number`),
    FOREIGN KEY (`doc_id`) REFERENCES `document`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档版本表';
```

### 8. 文件夹表 (folder)
```sql
CREATE TABLE `folder` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件夹ID',
    `folder_name` VARCHAR(100) NOT NULL COMMENT '文件夹名称',
    `parent_id` BIGINT COMMENT '父文件夹ID',
    `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件夹表';
```

### 9. 标签表 (tag)
```sql
CREATE TABLE `tag` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    `tag_name` VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';
```

### 10. 文档编辑者表 (document_editor)
```sql
CREATE TABLE `document_editor` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `document_id` BIGINT NOT NULL COMMENT '文档ID',
    `user_id` BIGINT NOT NULL COMMENT '编辑者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_doc_user` (`document_id`, `user_id`),
    INDEX `idx_document_id` (`document_id`),
    INDEX `idx_user_id` (`user_id`),
    FOREIGN KEY (`document_id`) REFERENCES `document`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档编辑者表';
```

### 11. 评论表 (comment)
```sql
CREATE TABLE `comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    `doc_id` BIGINT NOT NULL COMMENT '文档ID',
    `user_id` BIGINT NOT NULL COMMENT '评论者ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `parent_id` BIGINT COMMENT '父评论ID（回复）',
    `position` VARCHAR(500) COMMENT '批注位置（JSON）',
    `comment_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    INDEX `idx_doc_id` (`doc_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_parent_id` (`parent_id`),
    FOREIGN KEY (`doc_id`) REFERENCES `document`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';
```

## 三、任务管理模块

### 12. 任务表 (task)
```sql
CREATE TABLE `task` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    `doc_id` BIGINT COMMENT '关联文档ID',
    `title` VARCHAR(255) NOT NULL COMMENT '任务标题',
    `content` TEXT COMMENT '任务描述',
    `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
    `deadline` DATETIME COMMENT '截止日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_doc_id` (`doc_id`),
    INDEX `idx_creator_id` (`creator_id`),
    INDEX `idx_deadline` (`deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';
```

### 13. 任务分配表 (task_assignee)
```sql
CREATE TABLE `task_assignee` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `assignee_id` BIGINT NOT NULL COMMENT '被分配人ID',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING/IN_PROGRESS/COMPLETED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
    `complete_time` DATETIME COMMENT '完成时间',
    `deadline_notified` INT DEFAULT 0 COMMENT '是否已发送截止提醒：0-否，1-是',
    UNIQUE KEY `uk_task_assignee` (`task_id`, `assignee_id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_assignee_id` (`assignee_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deadline_notified` (`deadline_notified`),
    FOREIGN KEY (`task_id`) REFERENCES `task`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`assignee_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务分配表';
```

## 四、通知模块

### 14. 通知表 (notification)
```sql
CREATE TABLE `notification` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
    `sender_id` BIGINT COMMENT '发送者ID',
    `content` VARCHAR(500) NOT NULL COMMENT '通知内容',
    `type` VARCHAR(50) NOT NULL COMMENT '通知类型：COMMENT/REPLY/MENTION/TASK',
    `doc_id` BIGINT COMMENT '关联文档ID',
    `comment_id` BIGINT COMMENT '关联评论ID',
    `related_id` BIGINT COMMENT '关联ID（任务ID等）',
    `status` INT COMMENT '状态（联系人请求：0-待处理，1-已接受，2-已拒绝）',
    `is_read` INT DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `read_time` DATETIME COMMENT '阅读时间',
    INDEX `idx_receiver_read` (`receiver_id`, `is_read`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_type` (`type`),
    FOREIGN KEY (`receiver_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
```

## 五、联系人模块

### 15. 联系人表 (contact)
```sql
CREATE TABLE `contact` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '联系人ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `contact_user_id` BIGINT NOT NULL COMMENT '联系人用户ID',
    `remark` VARCHAR(100) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    UNIQUE KEY `uk_user_contact` (`user_id`, `contact_user_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_contact_user_id` (`contact_user_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`contact_user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联系人表';
```

### 16. 联系人请求表 (contact_request)
```sql
CREATE TABLE `contact_request` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '请求ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
    `send_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `status` INT DEFAULT 0 COMMENT '状态：0-待处理，1-已同意，2-已拒绝',
    INDEX `idx_sender_id` (`sender_id`),
    INDEX `idx_receiver_id` (`receiver_id`),
    INDEX `idx_status` (`status`),
    FOREIGN KEY (`sender_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`receiver_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联系人请求表';
```

## 六、系统模块

### 17. 操作日志表 (sys_operation_log)
```sql
CREATE TABLE `sys_operation_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT NOT NULL COMMENT '操作用户ID',
    `user_name` VARCHAR(50) COMMENT '操作用户名',
    `operation` VARCHAR(255) NOT NULL COMMENT '操作描述',
    `resource` VARCHAR(255) COMMENT '操作资源',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `operate_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

## 七、初始化数据

### 插入默认角色
```sql
INSERT INTO `role` (`role_name`, `role_desc`) VALUES 
('admin', '管理员'),
('user', '普通用户');
```

### 插入默认权限
```sql
INSERT INTO `permission` (`perm_name`, `perm_key`) VALUES 
('查看文档', 'doc:view'),
('编辑文档', 'doc:edit'),
('删除文档', 'doc:delete'),
('管理用户', 'user:manage'),
('管理角色', 'role:manage'),
('管理权限', 'perm:manage');
```

### 分配权限给角色
```sql
-- admin 拥有所有权限
INSERT INTO `role_permission` (`role_id`, `perm_id`) 
SELECT 1, id FROM `permission`;

-- user 只有查看和编辑权限
INSERT INTO `role_permission` (`role_id`, `perm_id`) VALUES 
(2, 1), (2, 2);
```

---

**说明：**
- 所有表使用 InnoDB 引擎，支持事务和外键
- 字符集使用 utf8mb4，支持 emoji 和特殊字符
- 关键字段建立索引，提高查询性能
- 使用外键约束保证数据一致性
- LONGBLOB 用于存储 Yjs 二进制状态
- LONGTEXT 用于存储富文本内容
- 使用 FULLTEXT 索引支持全文搜索

