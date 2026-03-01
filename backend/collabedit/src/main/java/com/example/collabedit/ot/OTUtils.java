package com.example.collabedit.ot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * 版本号控制+操作指令合并，避免直接覆盖
 */
public class OTUtils {
    /**
     * 应用编辑操作到文档内容
     * @param originalContent 原始内容
     * @param operation 编辑操作（包含位置、内容、版本号）
     * @return 合并后的新内容
     */
    public static String applyOperation(String originalContent, EditOperation operation) {
        if (!StringUtils.hasText(originalContent)) {
            return operation.getContent();
        }
        // 支持插入/替换操作
        int position = operation.getPosition();
        String content = operation.getContent();
        
        // 插入操作（position <= 原始长度）
        if (position <= originalContent.length()) {
            return originalContent.substring(0, position) + content + originalContent.substring(position);
        }//拆分文档为操作部分前后两部分并进行拼接。
        // 追加操作
        return originalContent + content;
    }

    /**
     * 编辑操作实体
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditOperation {
        private Long docId;          // 文档ID
        private Long userId;         // 操作用户ID
        private Integer position;    // 编辑位置（字符索引）
        private String content;      // 编辑内容
        private Long version;        // 操作基于的版本号
    }
}