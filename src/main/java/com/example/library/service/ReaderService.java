package com.example.library.service;

import com.example.library.repository.LibraryRepository;
import com.example.library.exception.BusinessException;
import org.springframework.stereotype.Service;
import com.example.library.model.Reader;
import com.example.library.dto.ResponseMapper;
import java.util.UUID;
import static com.example.library.dto.Models.*;

/** 读者录入与演示身份解析。 */
@Service
public class ReaderService {
    private final LibraryRepository repo;
    private final WriteTransactionExecutor transactions;
    public ReaderService(LibraryRepository repo, WriteTransactionExecutor transactions) {
        this.repo=repo; this.transactions=transactions;
    }
    public ReaderResponse createReader(CreateReaderRequest request) { return transactions.execute(() -> {
        var reader=Reader.create(request.name(),request.studentNumber(),repo.now());
        return ResponseMapper.reader(repo.createReader(reader));
    }); }
    public UUID authenticate(String value) {
        UUID id;
        try { id=UUID.fromString(value==null?"":value); }
        catch (IllegalArgumentException e) { throw new BusinessException(401,"UNAUTHENTICATED","读者身份无效"); }
        if (!repo.readerExists(id)) throw new BusinessException(401,"UNAUTHENTICATED","读者身份无效");
        return id;
    }
}
