package com.example.library.dto;

import com.example.library.model.Book;
import com.example.library.model.Reader;
import com.example.library.model.Loan;
import static com.example.library.dto.Models.*;

/** 领域模型到 API 响应 DTO 的显式转换。 */
public final class ResponseMapper {
    private ResponseMapper() {}
    public static BookResponse book(Book b) { return new BookResponse(b.getId(),b.getTitle(),b.getAuthor(),b.getDescription(),b.getTotalCopies(),b.getAvailableCopies(),b.getCreateTime(),b.getUpdateTime()); }
    public static ReaderResponse reader(Reader r) { return new ReaderResponse(r.getId(),r.getName(),r.getStudentNumber(),r.getCreateTime(),r.getUpdateTime()); }
    public static LoanResponse loan(Loan l) { return new LoanResponse(l.getId(),l.getBookId(),l.getReaderId(),l.getStatus(),l.getBorrowTime(),l.getReturnTime(),l.getUpdateTime()); }
    public static CurrentLoan current(Loan l,String title,String author) { return new CurrentLoan(l.getId(),l.getBookId(),l.getReaderId(),l.getStatus(),l.getBorrowTime(),l.getReturnTime(),l.getUpdateTime(),new BookRef(l.getBookId(),title,author)); }
}
