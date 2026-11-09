package com.example.library.service;

import com.example.library.command.*;
import com.example.library.model.*;

import java.util.UUID;

/** Loan 业务接口；不依赖 API 请求或响应 DTO。 */
public interface LoanService {
    PageResult<CurrentBorrowing> current(UUID reader, int page, int size);

    Loan borrow(BorrowLoanCommand command);

    Loan returnBook(ReturnLoanCommand command);
}
