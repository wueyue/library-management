package com.example.library.model;

import java.util.List;

/** 与 API 响应格式独立的应用分页结果。 */
public record PageResult<T>(List<T> items,int page,int pageSize) {
    public PageResult { items=List.copyOf(items); }
}
