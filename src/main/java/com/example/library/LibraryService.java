package com.example.library;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.dao.DataAccessException;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import static com.example.library.Models.*;

@Service
public class LibraryService {
    private final LibraryRepository repo;
    private final TransactionTemplate tx;
    public LibraryService(LibraryRepository repo, PlatformTransactionManager manager) {
        this.repo=repo; this.tx=new TransactionTemplate(manager);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    public Book createBook(CreateBookRequest request) { return write(() -> repo.createBook(request)); }
    public Student createStudent(CreateStudentRequest request) { return write(() -> repo.createStudent(request)); }
    public UUID authenticate(String value) {
        UUID id;
        try { id=UUID.fromString(value==null?"":value); }
        catch (IllegalArgumentException e) { throw new BusinessException(401,"UNAUTHENTICATED","读者身份无效"); }
        if (!repo.readerExists(id)) throw new BusinessException(401,"UNAUTHENTICATED","读者身份无效");
        return id;
    }
    public Page<Book> books(int page,int size) { return new Page<>(repo.books(page,size),page,size); }
    public Book book(UUID id) { return repo.book(id,false); }
    public Page<CurrentLoan> current(UUID reader,int page,int size) { return new Page<>(repo.currentLoans(reader,page,size),page,size); }

    /** 图书行锁是多实例间的并发入口；创建记录与库存扣减共同提交。 */
    public Loan borrow(UUID reader, UUID book) {
        return write(() -> {
            repo.book(book,true);
            if(repo.activeLoan(reader,book)) throw new BusinessException(409,"BOOK_ALREADY_BORROWED","你已借阅这本书，请先归还");
            if(repo.decrement(book)!=1) throw new BusinessException(409,"BOOK_OUT_OF_STOCK","图书库存不足");
            return repo.createLoan(reader,book);
        });
    }
    /** 固定先锁图书再锁借阅；重复归还不更新任何字段。 */
    public Loan returnBook(UUID reader,UUID loanId) {
        return write(() -> {
            var ref=repo.ownedLoan(loanId,reader,false);
            repo.book(ref.bookId(),true);
            var loan=repo.ownedLoan(loanId,reader,true);
            if(loan.returnTime()!=null) return loan;
            var result=repo.markReturned(loanId);
            if(repo.increment(loan.bookId())!=1) throw new IllegalStateException("stock invariant violated");
            return result;
        });
    }
    private <T> T write(Supplier<T> action) {
        for(int attempt=0;attempt<3;attempt++) {
            try { return tx.execute(status -> { repo.lockTimeout(); return action.get(); }); }
            catch(DataAccessException e) {
                String state=sqlState(e);
                // 只对已确定回滚的死锁/序列化失败重试整个事务。
                if("40P01".equals(state)||"40001".equals(state)) {
                    if(attempt==2) throw busy();
                    try { Thread.sleep(ThreadLocalRandom.current().nextLong(10,51)); }
                    catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); throw busy(); }
                } else if("55P03".equals(state)) throw busy();
                else throw e;
            }
        }
        throw busy();
    }
    static String sqlState(Throwable error) {
        for(Throwable c=error;c!=null;c=c.getCause()) if(c instanceof SQLException sql) return sql.getSQLState();
        return null;
    }
    private static BusinessException busy() { return new BusinessException(503,"SERVICE_BUSY","服务繁忙，请稍后重试"); }
}
