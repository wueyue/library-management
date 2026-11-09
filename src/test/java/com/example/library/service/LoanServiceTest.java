package com.example.library.service;

import com.example.library.repository.LibraryRepository;
import com.example.library.exception.BusinessException;
import com.example.library.model.Book;
import com.example.library.model.Loan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoanServiceTest {
    LibraryRepository repo;
    PlatformTransactionManager manager;
    LoanService service;
    ReaderService readers;
    UUID reader=UUID.randomUUID(),book=UUID.randomUUID(),id=UUID.randomUUID();
    OffsetDateTime now=OffsetDateTime.now();
    Loan loan(boolean returned) { return new Loan(id,book,reader,now,returned?now:null,now); }
    @BeforeEach void setup() {
        repo=mock(LibraryRepository.class);manager=mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        var tx=new WriteTransactionExecutor(repo,manager);
        service=new LoanService(repo,tx);readers=new ReaderService(repo,tx);
        // lenient：身份校验测试不会访问数据库，其他测试按需使用。
        lenient().when(repo.book(book,true)).thenReturn(new Book(book,"图书","作者",null,2,1,now,now));
        lenient().when(repo.now()).thenReturn(now);
        lenient().when(repo.createLoan(any(Loan.class))).thenAnswer(i->i.getArgument(0));
        lenient().when(repo.markReturned(any(Loan.class))).thenAnswer(i->i.getArgument(0));
    }
    @Test void borrowLocksBeforeStockAndCreation() {
        when(repo.decrement(any(Book.class))).thenReturn(1);
        assertEquals("BORROWED",service.borrow(reader,book).status());
        var order=inOrder(repo,manager);
        order.verify(repo).lockTimeout();order.verify(repo).book(book,true);
        order.verify(repo).activeLoan(reader,book);order.verify(repo).now();
        order.verify(repo).decrement(argThat(b->b.getAvailableCopies()==0));
        order.verify(repo).createLoan(argThat(l->l.getBookId().equals(book)&&l.getReaderId().equals(reader)));
        order.verify(manager).commit(any());
    }
    @Test void duplicateHasPriorityAndRollsBack() {
        when(repo.activeLoan(reader,book)).thenReturn(true);
        assertEquals("BOOK_ALREADY_BORROWED",assertThrows(BusinessException.class,()->service.borrow(reader,book)).getCode());
        verify(repo,never()).decrement(any());verify(manager).rollback(any());
    }
    @Test void noStockDoesNotCreateLoan() {
        when(repo.book(book,true)).thenReturn(new Book(book,"图书","作者",null,1,0,now,now));
        assertEquals("BOOK_OUT_OF_STOCK",assertThrows(BusinessException.class,()->service.borrow(reader,book)).getCode());
        verify(repo,never()).createLoan(any());verify(repo,never()).decrement(any());verify(manager).rollback(any());
    }
    @Test void insertFailureRollsBackTransaction() {
        when(repo.decrement(any(Book.class))).thenReturn(1);
        when(repo.createLoan(any(Loan.class))).thenThrow(new IllegalStateException("injected"));
        assertThrows(IllegalStateException.class,()->service.borrow(reader,book));
        verify(manager).rollback(any());verify(manager,never()).commit(any());
    }
    @Test void returnIsIdempotent() {
        var returned=loan(true);
        when(repo.ownedLoan(id,reader,false)).thenReturn(returned);
        when(repo.ownedLoan(id,reader,true)).thenReturn(returned);
        assertEquals(returned.getReturnTime(),service.returnBook(reader,id).returnTime());
        verify(repo,never()).increment(any());verify(repo,never()).markReturned(any());
    }
    @Test void returnUsesConsistentLockOrder() {
        when(repo.ownedLoan(id,reader,false)).thenReturn(loan(false));when(repo.ownedLoan(id,reader,true)).thenReturn(loan(false));
        when(repo.increment(any(Book.class))).thenReturn(1);
        assertEquals("RETURNED",service.returnBook(reader,id).status());
        var order=inOrder(repo);
        order.verify(repo).lockTimeout();order.verify(repo).ownedLoan(id,reader,false);
        order.verify(repo).book(book,true);order.verify(repo).ownedLoan(id,reader,true);order.verify(repo).now();
        order.verify(repo).markReturned(argThat(l->l.getReturnTime().equals(now)));
        order.verify(repo).increment(argThat(b->b.getAvailableCopies()==2));
    }
    @Test void stockFailureRollsBackReturn() {
        when(repo.ownedLoan(id,reader,false)).thenReturn(loan(false));when(repo.ownedLoan(id,reader,true)).thenReturn(loan(false));
        assertThrows(IllegalStateException.class,()->service.returnBook(reader,id));verify(manager).rollback(any());
    }
    @Test void invalidIdentityRejectedBeforeDatabaseAccess() {
        assertEquals(401,assertThrows(BusinessException.class,()->readers.authenticate(null)).getStatus());
        assertEquals(401,assertThrows(BusinessException.class,()->readers.authenticate("invalid")).getStatus());
        verify(repo,never()).readerExists(any());
    }
}
