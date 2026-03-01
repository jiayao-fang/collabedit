// 创建 d:\collabedit\backend\collabedit\src\main\java\com\example\collabedit\modules\document\dto\YjsStateUpdateDTO.java

package com.example.collabedit.modules.document.dto;

import lombok.Data;

@Data
public class YjsStateUpdateDTO {
    private String contentState;  // Yjs序列化状态
    private String content;       // HTML内容
}