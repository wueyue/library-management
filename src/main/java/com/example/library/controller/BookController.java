package com.example.library.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import static com.example.library.model.Models.*;

import com.example.library.service.BookService;

/** 图书录入与查询接口。 */
@RestController
@RequestMapping("/api/v1")
@Validated
public class BookController {
    private final BookService service;
    public BookController(BookService service) { this.service=service; }

    /** 录入图书。入参 title、author 必填且最多 255 字，description 可选，total_copies 必填且非负。
     * 返回 201 和完整图书信息；available_copies 初始化为 total_copies，ID 和时间由服务端生成。
     * 演示接口无需身份；生产环境需接入管理员授权。重复录入会创建不同的图书 ID。 */
    @PostMapping("/books/insert")
    @ResponseStatus(HttpStatus.CREATED)
    public Book createBook(@Valid @RequestBody CreateBookRequest request) { return service.createBook(request); }

    /** 分页浏览图书，无需身份。page 从 1 开始，page_size 范围 1–100，默认 20。
     * 返回图书列表及分页参数；库存仅为读取时快照。 */
    @GetMapping("/books")
    public Page<Book> books(@RequestParam(defaultValue="1") @Min(1) int page,
                           @RequestParam(name="page_size",defaultValue="20") @Min(1) @Max(100) int size) {
        return service.books(page,size);
    }
    /** 查询图书详情。bookId 为图书 UUID；返回元数据、库存和时间；不存在返回 404。 */
    @GetMapping("/books/{bookId}")
    public Book book(@PathVariable UUID bookId) { return service.book(bookId); }

}
