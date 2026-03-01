package com.example.collabedit.modules.document.scheduler;

import com.example.collabedit.modules.document.service.RedisDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.Cursor;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentSyncScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisDocumentService redisDocumentService;

    private static final String DOCUMENT_CONTENT_PREFIX = "doc:content:";

    /**
     * 定时同步Redis文档到数据库（每30分钟）
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void scheduledSyncToDatabase() {
        log.info("Start scheduled sync Redis documents to database");
        Set<Long> docIds = scanDocumentIds();

        for (Long docId : docIds) {
            try {
                redisDocumentService.syncToDatabase(docId);
            } catch (Exception e) {
                log.error("Sync document {} failed in scheduled task", docId, e);
            }
        }
        log.info("Completed scheduled sync (total docs: {})", docIds.size());
    }

    /**
     * 使用scan命令遍历文档ID
     */
    private Set<Long> scanDocumentIds() {
        Set<Long> docIds = new HashSet<>();
        Set<String> keys = new HashSet<>();
         try {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions()
                            .match(redisTemplate.getStringSerializer().serialize(DOCUMENT_CONTENT_PREFIX + "*"))
                            .count(100)
                            .build())) {

                while (cursor.hasNext()) {
                    byte[] key = cursor.next();
                   keys.add(redisTemplate.getStringSerializer().deserialize(key));
                }
            } catch (Exception e) {
                log.error("Error scanning Redis keys", e);
            }
            return null;
        });
    } catch (Exception e) {
        log.error("Failed to execute Redis scan operation", e);
    }

    for (String key : keys) {
        try {
            String docIdStr = key.substring(DOCUMENT_CONTENT_PREFIX.length());
            docIds.add(Long.parseLong(docIdStr));
        } catch (NumberFormatException e) {
            log.warn("Invalid document key: {}", key);
        }
    }

    return docIds;
    }
}
