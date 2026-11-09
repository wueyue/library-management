package com.example.library.service;

import java.util.UUID;
import static com.example.library.dto.Models.*;

/** Loan 业务接口，实现位于 service.impl。 */
public interface LoanService {
    Page<CurrentLoan> current(UUID reader,int page,int size);
    LoanResponse borrow(UUID reader,UUID book);
    LoanResponse returnBook(UUID reader,UUID loanId);
}
