package com.example.library.service.impl;

import com.example.library.service.ReaderService;
import com.example.library.service.WriteTransactionExecutor;

import com.example.library.dto.persistence.ReaderDao;
import com.example.library.exception.BusinessException;
import org.springframework.stereotype.Service;
import com.example.library.model.Reader;
import com.example.library.dto.ResponseMapper;
import java.util.UUID;
import static com.example.library.dto.Models.*;

/** 读者录入与演示身份解析。 */
@Service
public class ReaderServiceImpl implements ReaderService {
    private final ReaderDao readerDao;
    private final WriteTransactionExecutor transactions;
    public ReaderServiceImpl(ReaderDao readerDao, WriteTransactionExecutor transactions) {
        this.readerDao=readerDao;
        this.transactions=transactions;
    }
    @Override
    public ReaderResponse createReader(CreateReaderRequest request) { return transactions.execute(() -> {
        var reader=Reader.create(request.name(),request.studentNumber(),transactions.now());
        return ResponseMapper.reader(readerDao.createReader(reader));
    }); }
    @Override
    public UUID authenticate(String value) {
        UUID id;
        try { id=UUID.fromString(value==null?"":value); }
        catch (IllegalArgumentException e) { throw new BusinessException(401,"UNAUTHENTICATED","读者身份无效"); }
        if (!readerDao.readerExists(id)) throw new BusinessException(401,"UNAUTHENTICATED","读者身份无效");
        return id;
    }
}
