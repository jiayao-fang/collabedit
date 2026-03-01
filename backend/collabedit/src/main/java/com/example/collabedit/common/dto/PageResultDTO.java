package com.example.collabedit.common.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResultDTO<T> {
    private long total; // 总条数
    private List<T> list; // 分页数据
    private int page; // 当前页
    private int size; // 每页条数
    private long pages; // 总页数

}