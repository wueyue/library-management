package com.example.library.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import static com.example.library.model.Models.*;

import com.example.library.service.LoanService;
import com.example.library.service.StudentService;

/** 借阅、归还及当前借阅查询接口。 */
@RestController
@RequestMapping("/api/v1")
@Validated
public class LoanController {
    private final LoanService service;
    private final StudentService studentService;
    public LoanController(LoanService service, StudentService studentService) { this.service=service; this.studentService=studentService; }

    /** 借阅图书。X-Reader-Id 为演示读者身份，JSON 入参为 book_id。
     * 成功返回 201 和 Loan；重复借阅或库存不足返回 409，图书不存在返回 404。
     * 同一事务内完成并发检查、库存扣减和记录创建。 */
    @PostMapping("/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public Loan borrow(@RequestHeader(name="X-Reader-Id",required=false) String reader,
                       @Valid @RequestBody BorrowRequest request) {
        return service.borrow(studentService.authenticate(reader),request.bookId());
    }
    /** 归还自己的借阅。loanId 为借阅 UUID，无请求体。
     * 返回 200 和 Loan；重复归还返回原结果；不存在或属于他人返回 404。 */
    @PostMapping("/loans/{loanId}/return")
    public Loan returnBook(@RequestHeader(name="X-Reader-Id",required=false) String reader,
                           @PathVariable UUID loanId) {
        return service.returnBook(studentService.authenticate(reader),loanId);
    }
    /** 查询自己的未归还借阅，入参为身份头和分页参数，返回借阅及图书摘要。 */
    @GetMapping("/me/loans")
    public Page<CurrentLoan> current(@RequestHeader(name="X-Reader-Id",required=false) String reader,
                                    @RequestParam(defaultValue="1") @Min(1) int page,
                                    @RequestParam(name="page_size",defaultValue="20") @Min(1) @Max(100) int size) {
        return service.current(studentService.authenticate(reader),page,size);
    }
}
