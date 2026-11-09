package com.example.library.service.impl;

import com.example.library.service.LoanService;
import com.example.library.service.WriteTransactionExecutor;

import com.example.library.dto.persistence.BookDao;
import com.example.library.dto.persistence.LoanDao;
import com.example.library.exception.BusinessException;
import org.springframework.stereotype.Service;
import com.example.library.model.Loan;
import com.example.library.dto.ResponseMapper;
import java.util.UUID;
import static com.example.library.dto.Models.*;

/** 借阅、归还及当前借阅查询业务；统一持有库存操作事务。 */
@Service
public class LoanServiceImpl implements LoanService {
    private final BookDao bookDao;
    private final LoanDao loanDao;
    private final WriteTransactionExecutor transactions;
    public LoanServiceImpl(BookDao bookDao, LoanDao loanDao, WriteTransactionExecutor transactions) {
        this.bookDao=bookDao;
        this.loanDao=loanDao;
        this.transactions=transactions;
    }
    @Override
    public Page<CurrentLoan> current(UUID reader,int page,int size) { return new Page<>(loanDao.currentLoans(reader,page,size).stream().map(row -> ResponseMapper.current(row.loan(),row.title(),row.author())).toList(),page,size); }

    /** 图书行锁是多实例间的并发入口；创建记录与库存扣减共同提交。 */
    @Override
    public LoanResponse borrow(UUID reader, UUID book) {
        return transactions.execute(() -> {
            var lockedBook=bookDao.book(book,true);
            if(loanDao.activeLoan(reader,book)) throw new BusinessException(409,"BOOK_ALREADY_BORROWED","你已借阅这本书，请先归还");
            if(lockedBook.getAvailableCopies()<=0) throw new BusinessException(409,"BOOK_OUT_OF_STOCK","图书库存不足");
            var now=transactions.now();
            if(bookDao.decrement(lockedBook.borrowCopy(now))!=1) throw new BusinessException(409,"BOOK_OUT_OF_STOCK","图书库存不足");
            var loan=Loan.borrow(reader,book,now);
            return ResponseMapper.loan(loanDao.createLoan(loan));
        });
    }
    /** 固定先锁图书再锁借阅；重复归还不更新任何字段。 */
    @Override
    public LoanResponse returnBook(UUID reader,UUID loanId) {
        return transactions.execute(() -> {
            var ref=loanDao.ownedLoan(loanId,reader,false);
            var lockedBook=bookDao.book(ref.getBookId(),true);
            var loan=loanDao.ownedLoan(loanId,reader,true);
            if(loan.getReturnTime()!=null) return ResponseMapper.loan(loan);
            var now=transactions.now();
            var result=loanDao.markReturned(loan.returnBook(now));
            if(bookDao.increment(lockedBook.returnCopy(now))!=1) throw new IllegalStateException("stock invariant violated");
            return ResponseMapper.loan(result);
        });
    }
}
