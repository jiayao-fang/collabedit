# CollabEdit 协同文档编辑系统 

##  项目概述

CollabEdit 是一个基于 Web 的实时协同文档编辑系统，支持多人同时在线编辑文档，提供完整的文档管理、版本控制、任务分配、用户权限管理等功能。

**核心特性：** 实时协同编辑、冲突自动解决、版本控制、权限管理  
**技术架构：** 前后端分离 + WebSocket 实时通信 + Redis 缓存

---

##  技术栈

### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Boot** | 3.5.8 | 核心框架 |
| **Java** | 21 | 开发语言 |
| **Spring Security** | 3.5.8 | 安全认证框架 |
| **MyBatis** | 3.0.5 | ORM 持久层框架 |
| **MySQL** | 8.x | 关系型数据库 |
| **Redis** | 最新版 | 缓存 + 会话管理 |
| **WebSocket** | Spring 内置 | 实时通信 |
| **JWT** | 0.11.5 | 无状态身份认证 |
| **Lombok** | 1.18.34 | 代码简化工具 |
| **Jackson** | 2.17.2 | JSON 序列化 |
| **Hibernate Validator** | 8.0.1 | 参数校验 |
| **CommonMark** | 0.21.0 | Markdown 解析 |

### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Vue 3** | 3.5.26 | 前端框架 |
| **Vite** | 6.0.0 | 构建工具 |
| **Vue Router** | 4.6.3 | 路由管理 |
| **Pinia** | 3.0.4 | 状态管理 |
| **Element Plus** | 2.12.0 | UI 组件库 |
| **Axios** | 1.13.2 | HTTP 客户端 |
| **Quill** | 1.3.7 | 富文本编辑器 |
| **Yjs** | 13.6.27 | CRDT 协同算法 |
| **y-websocket** | 3.0.0 | Yjs WebSocket 适配器 |
| **quill-cursors** | 4.0.4 | 多人光标显示 |
| **Tailwind CSS** | 3.4.18 | CSS 框架 |

### 协同编辑服务

| 技术 | 版本 | 用途 |
|------|------|------|
| **Node.js** | 最新 LTS | 运行环境 |
| **Yjs** | 13.6.27 | CRDT 协同算法 |
| **y-redis** | 1.0.3 | Redis 持久化 |
| **WebSocket (ws)** | 8.16.0 | WebSocket 服务器 |
| **jsonwebtoken** | 9.0.2 | JWT 验证 |

---

## 🎯 核心功能

### 1. 用户管理模块

#### 1.1 用户注册与登录
-  支持用户名/密码注册
-  支持手机号注册（验证码验证）
-  支持邮箱注册（验证码验证）
-  密码找回功能
-  JWT Token 认证
-  密码 BCrypt 加密存储

#### 1.2 用户信息管理
-  个人资料编辑（用户名、签名）
-  头像上传（本地存储）
-  密码修改
-  用户状态管理（启用/禁用）

#### 1.3 权限管理（RBAC）
-  角色管理（admin、user）
-  权限管理（细粒度权限控制）
-  角色-权限关联
-  用户-角色关联
-  基于注解的权限校验（@PreAuthorize）

#### 1.4 账号禁用功能
-  管理员禁用/启用用户账号
-  禁用后立即强制用户下线
-  禁用用户无法登录
-  禁用用户的请求被拦截（JWT 过滤器）
-  批量禁用/启用功能
-  Redis 缓存优化性能

### 2. 文档管理模块

#### 2.1 文档基础操作
-  文档创建（支持富文本编辑）
-  文档编辑（实时保存）
-  文档删除（逻辑删除）
-  文档恢复（从回收站）
-  文档彻底删除
-  文档搜索（标题、内容全文搜索）

#### 2.2 文档分类管理
-  文件夹管理（创建、编辑、删除）
-  标签管理（创建、编辑、删除）
-  按文件夹分类浏览
-  按标签分类浏览
-  多标签支持

#### 2.3 文档权限控制
-  可见性设置（公开/私有）
-  文档所有者权限
-  协作者权限管理
-  文档锁定功能

#### 2.4 文档版本控制
-  手动创建版本快照
-  版本历史查看
-  版本对比（差异显示）
-  版本回滚
-  回滚前自动备份
-  版本变更描述
-  Yjs 状态持久化

### 3. 实时协同编辑

#### 3.1 Yjs CRDT 算法
-  多人实时协同编辑
-  自动冲突解决（无需中心化协调）
-  离线编辑支持
-  操作自动合并
-  最终一致性保证

#### 3.2 OT 算法（备选方案）
-  操作转换（Operational Transformation）
-  冲突检测与解决
-  操作历史记录
-  冲突统计与监控
-  可自定义冲突策略

#### 3.3 协同编辑特性
-  多人光标显示
-  用户在线状态显示
-  实时内容同步
-  WebSocket 长连接
-  断线自动重连
-  Redis 持久化存储

### 4. 任务管理模块

#### 4.1 任务基础功能
-  任务创建（关联文档）
-  任务分配
-  任务状态管理（待处理/进行中/已完成）
-  任务截止日期设置
-  任务描述

#### 4.2 任务状态跟踪
-  任务整体状态计算（全部完成/部分完成/进行中/待处理）
-  完成进度显示（已完成人数/总人数）
-  实时状态更新

#### 4.3 任务通知
-  任务分配通知
-  任务状态变更通知
-  截止日期临近提醒（提前1天）
-  任务全部完成通知
-  防止重复通知

### 5. 通知系统

#### 5.1 通知类型
-  系统通知
-  任务通知
-  文档通知
-  评论通知

#### 5.2 通知功能
-  实时通知推送
-  未读消息提示
-  通知列表查看
-  标记已读/未读
-  批量标记已读
-  通知删除

### 6. 联系人管理

#### 6.1 好友系统
-  添加好友（发送请求）
-  好友请求处理（接受/拒绝）
-  好友列表查看
-  删除好友

#### 6.2 联系人功能
-  用户搜索
-  在线状态显示
-  联系人分组

### 7. 评论系统

-  文档评论
-  评论回复
-  评论删除
-  评论列表查看
-  评论通知

### 8. 操作日志

-  用户操作记录
-  AOP 切面自动记录
-  日志查询
-  操作审计

---

##  技术亮点

### 1. 实时协同编辑架构

#### 1.1 Yjs CRDT 算法
**核心优势：**
- **无需中心化服务器协调**：每个客户端独立计算，自动合并操作
- **离线编辑支持**：断网后继续编辑，重连后自动同步
- **最终一致性**：保证所有客户端最终状态一致
- **高性能**：操作复杂度低，适合大规模并发

**实现细节：**
```javascript
// 客户端初始化 Yjs 文档
const ydoc = new Y.Doc();
const ytext = ydoc.getText('quill');

// WebSocket 连接
const provider = new WebsocketProvider(
  'ws://localhost:1234',
  docId,
  ydoc
);

// 绑定 Quill 编辑器
const binding = new QuillBinding(ytext, quillEditor, provider.awareness);
```

**服务端持久化：**
```javascript
// Redis 持久化
const persistence = new RedisPersistence({
  host: 'localhost',
  port: 6379,
  prefix: 'yjs_doc_'
});

// 绑定文档状态到 Redis
await persistence.bindState(docId, ydoc);
```

#### 1.2 OT 算法（备选方案）
**核心优势：**
- **冲突解决逻辑可控**：可自定义冲突策略
- **易于调试**：操作历史清晰可追溯
- **审计友好**：完整的操作记录

**操作转换示例：**
```javascript
// 两个并发插入操作
const op1 = { type: 'insert', position: 5, content: 'hello' };
const op2 = { type: 'insert', position: 5, content: 'world' };

// OT 转换后
const transformed = transform(op1, op2);
// op2 的位置调整为 10（5 + 'hello'.length）
```

### 2. 账号禁用与强制下线

#### 2.1 三层防护机制
**第一层：登录拦截**
```java
// 登录时检查用户状态
if (user.getStatus() != 1) {
    throw new RuntimeException("账号已禁用");
}
```

**第二层：JWT 过滤器拦截**
```java
// 每次请求都检查用户状态
if (!userStatusService.isUserActive(userId)) {
    response.setStatus(403);
    response.getWriter().write("{\"error\":\"账号已被禁用\"}");
    return;
}
```

**第三层：强制下线**
```java
// 禁用时清除所有 Token
public void forceUserOffline(Long userId) {
    // 1. 清除 Redis 缓存
    stringRedisTemplate.delete("user:status:" + userId);
    
    // 2. 清除所有 Token
    stringRedisTemplate.delete("user:token:" + userId + ":*");
}
```

#### 2.2 Redis 缓存优化
```java
// 缓存用户状态（5分钟）
stringRedisTemplate.opsForValue().set(
    "user:status:" + userId,
    String.valueOf(status),
    5,
    TimeUnit.MINUTES
);
```

### 3. 文档版本控制

#### 3.1 乐观锁并发控制
```java
// 使用版本号防止并发冲突
@Update("UPDATE document SET content=#{content}, version=version+1 " +
        "WHERE id=#{id} AND version=#{version}")
int updateWithVersion(Document doc);
```

#### 3.2 版本快照机制
```java
// 手动创建版本快照
public void createVersion(Long docId, Long userId, String description) {
    Document doc = documentMapper.selectById(docId);
    
    DocumentVersion version = new DocumentVersion();
    version.setDocId(docId);
    version.setTitle(doc.getTitle());
    version.setContent(doc.getContent());
    version.setContentState(doc.getContentState()); // Yjs 状态
    version.setChangeDescription(description);
    
    documentVersionMapper.insert(version);
}
```

#### 3.3 版本回滚
```java
// 回滚前自动备份
public void rollbackToVersion(Long docId, Long versionId, Long userId) {
    // 1. 备份当前状态
    createVersion(docId, userId, "回滚前自动备份");
    
    // 2. 恢复到目标版本
    DocumentVersion targetVersion = versionMapper.selectById(versionId);
    doc.setContent(targetVersion.getContent());
    doc.setContentState(targetVersion.getContentState());
    documentService.updateByIdWithoutVersion(doc);
    
    // 3. 创建回滚记录
    createVersion(docId, userId, "回滚到版本 " + targetVersion.getVersionNumber());
}
```

### 4. 任务状态智能跟踪

#### 4.1 自动计算任务状态
```java
private void calculateTaskProgress(Task task, List<TaskAssignee> assignees) {
    long total = assignees.size();
    long completed = assignees.stream()
        .filter(a -> "COMPLETED".equals(a.getStatus()))
        .count();
    long inProgress = assignees.stream()
        .filter(a -> "IN_PROGRESS".equals(a.getStatus()))
        .count();
    
    // 设置进度
    task.setProgress(completed + "/" + total);
    
    // 设置整体状态
    if (completed == total) {
        task.setOverallStatus("ALL_COMPLETED");
    } else if (completed > 0) {
        task.setOverallStatus("PARTIAL_COMPLETED");
    } else if (inProgress > 0) {
        task.setOverallStatus("IN_PROGRESS");
    } else {
        task.setOverallStatus("PENDING");
    }
}
```

#### 4.2 截止日期智能提醒
```java
@Scheduled(cron = "0 0 * * * ?") // 每小时执行
public void checkDeadlineApproaching() {
    // 查询1天内到期且未通知的任务
    List<TaskAssignee> approaching = taskAssigneeMapper.findDeadlineApproaching();
    
    for (TaskAssignee ta : approaching) {
        // 发送通知
        notificationService.sendTaskDeadlineNotification(ta);
        
        // 标记已通知（避免重复）
        taskAssigneeMapper.markDeadlineNotified(ta.getTaskId(), ta.getAssigneeId());
    }
}
```

### 5. RBAC 权限管理

#### 5.1 权限模型
```
用户（User） ←→ 用户角色（UserRole） ←→ 角色（Role）
                                          ↓
                                    角色权限（RolePermission）
                                          ↓
                                      权限（Permission）
```

#### 5.2 权限校验
```java
// 方法级别权限控制
@PreAuthorize("hasRole('admin')")
public void deleteUser(Long userId) {
    // 只有管理员可以删除用户
}

@PreAuthorize("hasAuthority('doc:delete')")
public void deleteDocument(Long docId) {
    // 需要 doc:delete 权限
}
```

#### 5.3 JWT 权限集成
```java
// JWT 中包含角色和权限
List<SimpleGrantedAuthority> authorities = new ArrayList<>();
authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

for (Permission permission : permissions) {
    authorities.add(new SimpleGrantedAuthority(permission.getPermKey()));
}
```

### 6. AOP 切面编程

#### 6.1 操作日志记录
```java
@Aspect
@Component
public class LogOperationAspect {
    
    @Around("@annotation(logOperation)")
    public Object logOperation(ProceedingJoinPoint joinPoint, LogOperation logOperation) {
        // 记录操作前状态
        String operation = logOperation.value();
        String username = getCurrentUsername();
        
        // 执行方法
        Object result = joinPoint.proceed();
        
        // 记录操作日志
        sysOperationLogService.save(new SysOperationLog(
            username, operation, new Date()
        ));
        
        return result;
    }
}
```

#### 6.2 用户状态变更监听
```java
@Aspect
@Component
public class UserStatusAspect {
    
    @AfterReturning("execution(* updateStatus(..))")
    public void afterStatusUpdate(JoinPoint joinPoint) {
        Long userId = (Long) joinPoint.getArgs()[0];
        Integer status = (Integer) joinPoint.getArgs()[1];
        
        // 禁用时强制下线
        if (status == 0) {
            userStatusService.forceUserOffline(userId);
        }
    }
}
```

### 7. Redis 缓存策略

#### 7.1 缓存设计
```
user:status:{userId}        → 用户状态（5分钟）
verify_code:{receiver}      → 验证码（5分钟）
yjs_doc_{docId}            → Yjs 文档状态（永久）
```

#### 7.2 缓存更新策略
- **Cache Aside**：先更新数据库，再删除缓存
- **自动过期**：设置合理的 TTL
- **主动清除**：状态变更时立即删除缓存

### 8. WebSocket 实时通信

#### 8.1 连接管理
```javascript
// 客户端 ID 分配
const clientID = nextClientID++;
doc.clients.set(clientID, ws);

// 用户状态同步
doc.awareness.setLocalStateField(clientID, {
    user: {
        id: userId,
        name: username,
        color: userColor
    }
});
```

#### 8.2 消息广播
```javascript
function broadcast(doc, senderClientID, message) {
    doc.clients.forEach((ws, clientID) => {
        if (clientID !== senderClientID && ws.readyState === WebSocket.OPEN) {
            ws.send(message);
        }
    });
}
```

#### 8.3 断线重连
```javascript
// 延迟销毁机制（30分钟无连接后销毁内存实例）
function scheduleDocDestroy(docId, doc) {
    const timer = setTimeout(() => {
        if (doc.clients.size === 0) {
            doc.ydoc.destroy();
            docs.delete(docId);
        }
    }, 30 * 60 * 1000);
    destroyTimers.set(docId, timer);
}
```

---

## 项目结构

### 后端结构
```
backend/collabedit/
├── src/main/java/com/example/collabedit/
│   ├── modules/                    # 业务模块
│   │   ├── user/                   # 用户模块
│   │   │   ├── controller/         # 控制器
│   │   │   ├── service/            # 服务层
│   │   │   ├── mapper/             # 数据访问层
│   │   │   ├── entity/             # 实体类
│   │   │   ├── dto/                # 数据传输对象
│   │   │   └── aspect/             # AOP 切面
│   │   ├── document/               # 文档模块
│   │   ├── task/                   # 任务模块
│   │   ├── notification/           # 通知模块
│   │   ├── contact/                # 联系人模块
│   │   ├── system/                 # 系统模块（权限、角色）
│   │   └── admin/                  # 管理员模块
│   ├── security/                   # 安全配置
│   │   ├── JwtUtil.java            # JWT 工具类
│   │   ├── JwtAuthenticationFilter.java  # JWT 过滤器
│   │   └── SecurityConfig.java     # Spring Security 配置
│   ├── config/                     # 配置类
│   │   ├── RedisConfig.java        # Redis 配置
│   │   ├── WebSocketConfig.java    # WebSocket 配置
│   │   └── FileUploadConfig.java   # 文件上传配置
│   ├── common/                     # 公共类
│   │   ├── exception/              # 异常处理
│   │   ├── enums/                  # 枚举类
│   │   └── dto/                    # 公共 DTO
│   └── CollabeditApplication.java  # 启动类
├── src/main/resources/
│   ├── application.properties      # 配置文件
│   └── mapper/                     # MyBatis XML 映射文件
└── pom.xml                         # Maven 配置
```

### 前端结构
```
frontend/
├── src/
│   ├── pages/                      # 页面组件
│   │   ├── Login.vue               # 登录页
│   │   ├── Register.vue            # 注册页
│   │   ├── DocumentEdit.vue        # 文档编辑页
│   │   ├── DocumentRead.vue        # 文档阅读页
│   │   ├── DocumentVersionHistory.vue  # 版本历史页
│   │   ├── AdminUserManagement.vue # 用户管理页
│   │   └── ...
│   ├── components/                 # 公共组件
│   ├── api/                        # API 接口
│   │   ├── user.js                 # 用户 API
│   │   ├── document.js             # 文档 API
│   │   ├── task.js                 # 任务 API
│   │   └── http.js                 # Axios 封装
│   ├── router/                     # 路由配置
│   │   └── index.js
│   ├── store/                      # 状态管理
│   │   └── user.js
│   ├── utils/                      # 工具类
│   │   ├── websocket.js            # Yjs WebSocket 客户端
│   │   ├── ot-client.js            # OT 客户端
│   │   └── ot-client-engine.js     # OT 引擎
│   ├── App.vue                     # 根组件
│   └── main.js                     # 入口文件
├── public/                         # 静态资源
├── package.json                    # 依赖配置
├── vite.config.js                  # Vite 配置
└── tailwind.config.js              # Tailwind 配置
```

### 协同编辑服务
```
ysj-collab-server/
├── server.js                       # Yjs WebSocket 服务器
└── package.json                    # 依赖配置
```

---

## 🚀 部署说明

### 环境要求
- Java 21+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### 后端部署
```bash
cd backend/collabedit
mvn clean package
java -jar target/collabedit-0.0.1-SNAPSHOT.jar
```

### 前端部署
```bash
cd frontend
npm install
npm run build
# 将 dist 目录部署到 Nginx
```

### 协同编辑服务部署
```bash
cd ysj-collab-server
npm install
node server.js
# 或使用 PM2
pm2 start server.js --name collab-server
```

---

##  性能优化

1. **Redis 缓存**：用户状态、验证码等高频数据缓存
2. **数据库索引**：关键字段建立索引
3. **乐观锁**：文档并发编辑使用版本号控制
4. **WebSocket 连接池**：复用连接，减少开销
5. **Yjs 增量同步**：只传输变更部分
6. **延迟销毁**：文档无连接后延迟销毁，提高重连速度

---

##  安全措施

1. **密码加密**：BCrypt 加密存储
2. **JWT 认证**：无状态身份验证
3. **权限控制**：RBAC 细粒度权限管理
4. **SQL 注入防护**：MyBatis 参数化查询
5. **XSS 防护**：前端输入过滤
6. **CSRF 防护**：Spring Security 内置
7. **验证码防刷**：60秒限制 + Redis 存储

---

##  总结

CollabEdit 是一个功能完善、技术先进的协同文档编辑系统，采用前后端分离架构，集成了 Yjs CRDT 算法实现实时协同编辑，支持版本控制、任务管理、权限管理等企业级功能。系统具有高性能、高可用、易扩展的特点，适合中小型团队协作使用。

**核心技术优势：**
-  实时协同编辑，自动冲突解决
-  完善的权限管理和安全机制
-  Redis 缓存优化性能
-  版本控制和回滚功能
-  响应式设计，支持多端访问
-  模块化设计，易于扩展维护

---

**文档版本：** v1.0  
**最后更新：** 2026-02-27

