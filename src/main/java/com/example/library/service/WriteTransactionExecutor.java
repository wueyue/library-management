package com.example.library.service;

import com.example.library.repository.LibraryRepository;
import com.example.library.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.dao.DataAccessException;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** 统一写事务、锁等待上限与明确回滚后的有限重试，供各业务 Service 复用。 */
@Component
public class WriteTransactionExecutor {
    private final LibraryRepository repo;
    private final TransactionTemplate tx;
    public WriteTransactionExecutor(LibraryRepository repo, PlatformTransactionManager manager) {
        this.repo=repo;
        this.tx=new TransactionTemplate(manager);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    public <T> T execute(Supplier<T> action) {
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
