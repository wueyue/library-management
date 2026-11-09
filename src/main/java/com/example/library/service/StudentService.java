package com.example.library.service;

import com.example.library.repository.LibraryRepository;
import com.example.library.exception.BusinessException;
import org.springframework.stereotype.Service;
import java.util.UUID;
import static com.example.library.model.Models.*;

/** 学生录入与演示身份解析。 */
@Service
public class StudentService {
    private final LibraryRepository repo;
    private final WriteTransactionExecutor transactions;
    public StudentService(LibraryRepository repo, WriteTransactionExecutor transactions) {
        this.repo=repo; this.transactions=transactions;
    }
    public Student createStudent(CreateStudentRequest request) { return transactions.execute(() -> repo.createStudent(request)); }
    public UUID authenticate(String value) {
        UUID id;
        try { id=UUID.fromString(value==null?"":value); }
        catch (IllegalArgumentException e) { throw new BusinessException(401,"UNAUTHENTICATED","读者身份无效"); }
        if (!repo.readerExists(id)) throw new BusinessException(401,"UNAUTHENTICATED","读者身份无效");
        return id;
    }
}
