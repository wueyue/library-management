package com.example.library.model;

/** 当前借阅查询结果，包含领域借阅与图书摘要。 */
public record CurrentBorrowing(Loan loan, String title, String author) {
}
