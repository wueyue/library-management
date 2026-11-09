package com.example.library.controller;

import com.example.library.service.LibraryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import static com.example.library.model.Models.*;

/** 对外 REST API。字段说明、示例、时序图和错误码见 README。 */
@RestController
@RequestMapping("/api/v1")
@Validated
public class LibraryController {
    private final LibraryService service;
    public LibraryController(LibraryService service) { this.service=service; }

    /** 录入图书。入参 title、author 必填且最多 255 字，description 可选，total_copies 必填且非负。
     * 返回 201 和完整图书信息；available_copies 初始化为 total_copies，ID 和时间由服务端生成。
     * 演示接口无需身份；生产环境需接入管理员授权。重复录入会创建不同的图书 ID。 */
    @PostMapping("/books/insert")
    @ResponseStatus(HttpStatus.CREATED)
    public Book createBook(@Valid @RequestBody CreateBookRequest request) { return service.createBook(request); }

    /** 录入学生。入参 name 必填且最多 100 字，允许同名；返回 201 以及 id、name、create_time、update_time。
     * 学生沿用 Reader 模型，返回 id 可作为借阅接口的 X-Reader-Id。演示接口无需身份。 */
    @PostMapping("/students/insert")
    @ResponseStatus(HttpStatus.CREATED)
    public Student createStudent(@Valid @RequestBody CreateStudentRequest request) { return service.createStudent(request); }

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

    /** 借阅图书。X-Reader-Id 为演示读者身份，JSON 入参为 book_id。
     * 成功返回 201 和 Loan；重复借阅或库存不足返回 409，图书不存在返回 404。
     * 同一事务内完成并发检查、库存扣减和记录创建。 */
    @PostMapping("/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public Loan borrow(@RequestHeader(name="X-Reader-Id",required=false) String reader,
                       @Valid @RequestBody BorrowRequest request) {
        return service.borrow(service.authenticate(reader),request.bookId());
    }
    /** 归还自己的借阅。loanId 为借阅 UUID，无请求体。
     * 返回 200 和 Loan；重复归还返回原结果；不存在或属于他人返回 404。 */
    @PostMapping("/loans/{loanId}/return")
    public Loan returnBook(@RequestHeader(name="X-Reader-Id",required=false) String reader,
                           @PathVariable UUID loanId) {
        return service.returnBook(service.authenticate(reader),loanId);
    }
    /** 查询自己的未归还借阅，入参为身份头和分页参数，返回借阅及图书摘要。 */
    @GetMapping("/me/loans")
    public Page<CurrentLoan> current(@RequestHeader(name="X-Reader-Id",required=false) String reader,
                                    @RequestParam(defaultValue="1") @Min(1) int page,
                                    @RequestParam(name="page_size",defaultValue="20") @Min(1) @Max(100) int size) {
        return service.current(service.authenticate(reader),page,size);
    }
}
