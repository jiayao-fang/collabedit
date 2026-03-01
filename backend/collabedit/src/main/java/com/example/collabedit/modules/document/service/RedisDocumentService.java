package com.example.collabedit.modules.document.service;

import com.example.collabedit.modules.document.entity.Document;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class RedisDocumentService {
    @Resource 
    private DocumentService documentService;
    @Resource 
    private RedisTemplate <String, Object> redisTemplate;
     private static final Logger log = LoggerFactory.getLogger(RedisDocumentService.class);

    // Redis键前缀
    private static final String DOCUMENT_CONTENT_PREFIX = "doc:content:";
    private static final String DOCUMENT_VERSION_PREFIX = "doc:version:";
    private static final long REDIS_EXPIRE_HOURS = 24;

     /**
     * 获取文档内容（优先Redis，未命中则从DB加载）
     */
    public String getDocumentContent(Long docId) {
        String redisKey = DOCUMENT_CONTENT_PREFIX + docId;
        String content = (String)redisTemplate.opsForValue().get(redisKey);
        if (content == null) {
            // 从DB加载并缓存
            Optional<Document> docOpt = Optional.ofNullable(documentService.getById(docId));
            content = docOpt.map(Document::getContent).orElse("");
            saveDocumentContent(docId, content);
            // 初始化版本号（与DB同步）
            Long dbVersion = docOpt.map(Document::getVersion).map(Long::valueOf).orElse(0L);
            redisTemplate.opsForValue().set(DOCUMENT_VERSION_PREFIX + docId, String.valueOf(dbVersion));
        }
        return content;
    }

    /**
     * 获取文档版本号
     */
    public Long getDocumentVersion(Long docId) {
        String versionStr = (String)redisTemplate.opsForValue().get(DOCUMENT_VERSION_PREFIX + docId);
        return versionStr != null ? Long.parseLong(versionStr) : 0L;
    }

    /**
     * 递增版本号并保存内容（原子操作，避免并发问题）
     */
    public Long incrementVersionAndSaveContent(Long docId, String content) {
        String versionKey = DOCUMENT_VERSION_PREFIX + docId;
        // 原子递增版本号
        Long newVersion = redisTemplate.opsForValue().increment(versionKey, 1);
        // 保存内容并设置过期时间
        String contentKey = DOCUMENT_CONTENT_PREFIX + docId;
        redisTemplate.opsForValue().set(contentKey, content, REDIS_EXPIRE_HOURS, TimeUnit.HOURS);
        return newVersion != null ? newVersion : 1L;
    }

    /**
     * 保存文档内容（版本号同步）
     */
    public void saveDocumentContent(Long docId, String content) {
        String contentKey = DOCUMENT_CONTENT_PREFIX + docId;
        redisTemplate.opsForValue().set(contentKey, content, REDIS_EXPIRE_HOURS, TimeUnit.HOURS);
        // 确保版本号初始化
        if (getDocumentVersion(docId) == 0) {
            redisTemplate.opsForValue().set(DOCUMENT_VERSION_PREFIX + docId, "0");
        }
    }

    /**
     * 同步Redis内容到数据库
     */
    public void syncToDatabase(Long docId) {
        String content = getDocumentContent(docId);
        Long version = getDocumentVersion(docId);
        if (content == null) {
            log.warn("Document {} content not found in Redis", docId);
            return;
        }

        try {
           Document doc = documentService.getById(docId);
            if (doc == null) {
                throw new RuntimeException("Document not found: " + docId);
            }
            // 版本号同步（乐观锁防止DB覆盖）
            doc.setContent(content);
            doc.setVersion(version.intValue());
            documentService.updateById(doc);
            log.info("Synced document {} to database (version: {})", docId, version);
        } catch (Exception e) {
            log.error("Sync document {} to database failed", docId, e);
        }
    }
}