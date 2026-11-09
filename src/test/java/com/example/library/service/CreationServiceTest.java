package com.example.library.service;

import com.example.library.model.Book;
import com.example.library.model.Reader;
import com.example.library.repository.LibraryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.OffsetDateTime;
import static com.example.library.dto.Models.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 验证入参先转换为领域对象，再送到持久化边界。 */
class CreationServiceTest {
    LibraryRepository repo;
    WriteTransactionExecutor transactions;
    OffsetDateTime now=OffsetDateTime.now();
    @BeforeEach void setup() {
        repo=mock(LibraryRepository.class);
        var manager=mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        transactions=new WriteTransactionExecutor(repo,manager);
        when(repo.now()).thenReturn(now);
    }
    @Test void createBookConvertsRequestAndInitializesDomainStock() {
        when(repo.createBook(any(Book.class))).thenAnswer(i->i.getArgument(0));
        var response=new BookService(repo,transactions).createBook(new CreateBookRequest("  Java  "," 作者 ","简介",4));
        var captor=ArgumentCaptor.forClass(Book.class);verify(repo).createBook(captor.capture());
        var persisted=captor.getValue();
        assertEquals("Java",persisted.getTitle());assertEquals("作者",persisted.getAuthor());
        assertEquals(4,persisted.getTotalCopies());assertEquals(4,persisted.getAvailableCopies());
        assertNotNull(persisted.getId());assertEquals(now,persisted.getCreateTime());
        assertEquals(persisted.getId(),response.id());assertEquals(persisted.getUpdateTime(),response.updateTime());
    }
    @Test void createReaderConvertsRequestAndPreservesNumber() {
        when(repo.createReader(any(Reader.class))).thenAnswer(i->i.getArgument(0));
        var response=new ReaderService(repo,transactions).createReader(new CreateReaderRequest(" 张三 "," 00123 "));
        var captor=ArgumentCaptor.forClass(Reader.class);verify(repo).createReader(captor.capture());
        var persisted=captor.getValue();
        assertEquals("张三",persisted.getName());assertEquals("00123",persisted.getStudentNumber());
        assertEquals(now,persisted.getCreateTime());assertEquals(now,persisted.getUpdateTime());
        assertEquals(persisted.getId(),response.id());assertEquals("00123",response.studentNumber());
    }
}
