package com.example.library.service;

import com.example.library.repository.LibraryRepository;
import com.example.library.exception.BusinessException;
import org.springframework.stereotype.Service;
import java.util.UUID;
import static com.example.library.model.Models.*;

/** 借阅、归还及当前借阅查询业务；统一持有库存操作事务。 */
@Service
public class LoanService {
    private final LibraryRepository repo;
    private final WriteTransactionExecutor transactions;
    public LoanService(LibraryRepository repo, WriteTransactionExecutor transactions) {
        this.repo=repo; this.transactions=transactions;
    }
    public Page<CurrentLoan> current(UUID reader,int page,int size) { return new Page<>(repo.currentLoans(reader,page,size),page,size); }

    /** 图书行锁是多实例间的并发入口；创建记录与库存扣减共同提交。 */
    public Loan borrow(UUID reader, UUID book) {
        return transactions.execute(() -> {
            repo.book(book,true);
            if(repo.activeLoan(reader,book)) throw new BusinessException(409,"BOOK_ALREADY_BORROWED","你已借阅这本书，请先归还");
            if(repo.decrement(book)!=1) throw new BusinessException(409,"BOOK_OUT_OF_STOCK","图书库存不足");
            return repo.createLoan(reader,book);
        });
    }
    /** 固定先锁图书再锁借阅；重复归还不更新任何字段。 */
    public Loan returnBook(UUID reader,UUID loanId) {
        return transactions.execute(() -> {
            var ref=repo.ownedLoan(loanId,reader,false);
            repo.book(ref.bookId(),true);
            var loan=repo.ownedLoan(loanId,reader,true);
            if(loan.returnTime()!=null) return loan;
            var result=repo.markReturned(loanId);
            if(repo.increment(loan.bookId())!=1) throw new IllegalStateException("stock invariant violated");
            return result;
        });
    }
}
