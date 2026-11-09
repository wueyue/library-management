package com.example.library.service.impl;

import com.example.library.command.*;
import com.example.library.dto.persistence.ReaderDao;
import com.example.library.exception.BusinessException;
import com.example.library.exception.ErrorCode;
import com.example.library.model.Reader;
import com.example.library.service.ReaderService;
import com.example.library.service.WriteTransactionExecutor;

import org.springframework.stereotype.Service;

import java.util.UUID;

/** 读者录入与演示身份解析。 */
@Service
public class ReaderServiceImpl implements ReaderService {
    private final ReaderDao readerDao;
    private final WriteTransactionExecutor transactions;

    public ReaderServiceImpl(ReaderDao readerDao, WriteTransactionExecutor transactions) {
        this.readerDao = readerDao;
        this.transactions = transactions;
    }

    @Override
    public Reader createReader(CreateReaderCommand command) {
        return transactions.execute(
                () -> {
                    var reader =
                            Reader.create(
                                    command.name(), command.studentNumber(), transactions.now());
                    return readerDao.createReader(reader);
                });
    }

    @Override
    public UUID authenticate(String value) {
        UUID id;
        try {
            id = UUID.fromString(value == null ? "" : value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "读者身份无效");
        }
        if (!readerDao.readerExists(id))
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "读者身份无效");
        return id;
    }
}
