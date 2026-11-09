package com.example.library.command;

import java.util.UUID;

/** 应用层操作入参，不依赖 HTTP、JSON 或请求 DTO。 */
public record BorrowLoanCommand(UUID readerId, UUID bookId) {
    public BorrowLoanCommand {
        CommandValidation.required(readerId);
        CommandValidation.required(bookId);
    }
}
