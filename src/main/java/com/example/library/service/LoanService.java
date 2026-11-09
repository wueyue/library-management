package com.example.library.service;

import com.example.library.repository.LibraryRepository;
import com.example.library.exception.BusinessException;
import org.springframework.stereotype.Service;
import com.example.library.model.Loan;
import com.example.library.dto.ResponseMapper;
import java.util.UUID;
import static com.example.library.dto.Models.*;

/** 借阅、归还及当前借阅查询业务；统一持有库存操作事务。 */
@Service
public class LoanService {
    private final LibraryRepository repo;
    private final WriteTransactionExecutor transactions;
    public LoanService(LibraryRepository repo, WriteTransactionExecutor transactions) {
        this.repo=repo; this.transactions=transactions;
    }
    public Page<CurrentLoan> current(UUID reader,int page,int size) { return new Page<>(repo.currentLoans(reader,page,size).stream().map(row -> ResponseMapper.current(row.loan(),row.title(),row.author())).toList(),page,size); }

    /** 图书行锁是多实例间的并发入口；创建记录与库存扣减共同提交。 */
    public LoanResponse borrow(UUID reader, UUID book) {
        return transactions.execute(() -> {
            var lockedBook=repo.book(book,true);
            if(repo.activeLoan(reader,book)) throw new BusinessException(409,"BOOK_ALREADY_BORROWED","你已借阅这本书，请先归还");
            if(lockedBook.getAvailableCopies()<=0) throw new BusinessException(409,"BOOK_OUT_OF_STOCK","图书库存不足");
            var now=repo.now();
            if(repo.decrement(lockedBook.borrowCopy(now))!=1) throw new BusinessException(409,"BOOK_OUT_OF_STOCK","图书库存不足");
            var loan=Loan.borrow(reader,book,now);
            return ResponseMapper.loan(repo.createLoan(loan));
        });
    }
    /** 固定先锁图书再锁借阅；重复归还不更新任何字段。 */
    public LoanResponse returnBook(UUID reader,UUID loanId) {
        return transactions.execute(() -> {
            var ref=repo.ownedLoan(loanId,reader,false);
            var lockedBook=repo.book(ref.getBookId(),true);
            var loan=repo.ownedLoan(loanId,reader,true);
            if(loan.getReturnTime()!=null) return ResponseMapper.loan(loan);
            var now=repo.now();
            var result=repo.markReturned(loan.returnBook(now));
            if(repo.increment(lockedBook.returnCopy(now))!=1) throw new IllegalStateException("stock invariant violated");
            return ResponseMapper.loan(result);
        });
    }
}
