package com.example.library.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.library.command.*;
import com.example.library.dto.persistence.*;
import com.example.library.exception.BusinessException;
import com.example.library.model.Book;
import com.example.library.model.Loan;
import com.example.library.service.impl.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

class LoanServiceTest {
    BookDao bookDao;
    ReaderDao readerDao;
    LoanDao loanDao;
    DatabaseAccess database;
    PlatformTransactionManager manager;
    LoanService service;
    ReaderService readers;
    UUID reader = UUID.randomUUID(), book = UUID.randomUUID(), id = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();

    Loan loan(boolean returned) {
        return new Loan(id, book, reader, now, returned ? now : null, now);
    }

    @BeforeEach
    void setup() {
        bookDao = mock(BookDao.class);
        readerDao = mock(ReaderDao.class);
        loanDao = mock(LoanDao.class);
        database = mock(DatabaseAccess.class);
        manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        var tx = new WriteTransactionExecutor(database, manager);
        service = new LoanServiceImpl(bookDao, loanDao, tx);
        readers = new ReaderServiceImpl(readerDao, tx);
        // lenient：身份校验测试不会访问数据库，其他测试按需使用。
        lenient()
                .when(bookDao.book(book, true))
                .thenReturn(new Book(book, "图书", "作者", null, 2, 1, now, now));
        lenient().when(database.now()).thenReturn(now);
        lenient().when(loanDao.createLoan(any(Loan.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(loanDao.markReturned(any(Loan.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void borrowLocksBeforeStockAndCreation() {
        when(bookDao.decrement(any(Book.class))).thenReturn(1);
        assertEquals("BORROWED", service.borrow(new BorrowLoanCommand(reader, book)).getStatus());
        var order = inOrder(database, bookDao, readerDao, loanDao, manager);
        order.verify(database).lockTimeout();
        order.verify(bookDao).book(book, true);
        order.verify(loanDao).activeLoan(reader, book);
        order.verify(database).now();
        order.verify(bookDao).decrement(argThat(b -> b.getAvailableCopies() == 0));
        order.verify(loanDao)
                .createLoan(
                        argThat(l -> l.getBookId().equals(book) && l.getReaderId().equals(reader)));
        order.verify(manager).commit(any());
    }

    @Test
    void duplicateHasPriorityAndRollsBack() {
        when(loanDao.activeLoan(reader, book)).thenReturn(true);
        assertEquals(
                "BOOK_ALREADY_BORROWED",
                assertThrows(
                                BusinessException.class,
                                () -> service.borrow(new BorrowLoanCommand(reader, book)))
                        .getCode());
        verify(bookDao, never()).decrement(any());
        verify(manager).rollback(any());
    }

    @Test
    void noStockDoesNotCreateLoan() {
        when(bookDao.book(book, true)).thenReturn(new Book(book, "图书", "作者", null, 1, 0, now, now));
        assertEquals(
                "BOOK_OUT_OF_STOCK",
                assertThrows(
                                BusinessException.class,
                                () -> service.borrow(new BorrowLoanCommand(reader, book)))
                        .getCode());
        verify(loanDao, never()).createLoan(any());
        verify(bookDao, never()).decrement(any());
        verify(manager).rollback(any());
    }

    @Test
    void insertFailureRollsBackTransaction() {
        when(bookDao.decrement(any(Book.class))).thenReturn(1);
        when(loanDao.createLoan(any(Loan.class))).thenThrow(new IllegalStateException("injected"));
        assertThrows(
                IllegalStateException.class,
                () -> service.borrow(new BorrowLoanCommand(reader, book)));
        verify(manager).rollback(any());
        verify(manager, never()).commit(any());
    }

    @Test
    void returnIsIdempotent() {
        var returned = loan(true);
        when(loanDao.ownedLoan(id, reader, false)).thenReturn(returned);
        when(loanDao.ownedLoan(id, reader, true)).thenReturn(returned);
        assertEquals(
                returned.getReturnTime(),
                service.returnBook(new ReturnLoanCommand(reader, id)).getReturnTime());
        verify(bookDao, never()).increment(any());
        verify(loanDao, never()).markReturned(any());
    }

    @Test
    void returnUsesConsistentLockOrder() {
        when(loanDao.ownedLoan(id, reader, false)).thenReturn(loan(false));
        when(loanDao.ownedLoan(id, reader, true)).thenReturn(loan(false));
        when(bookDao.increment(any(Book.class))).thenReturn(1);
        assertEquals("RETURNED", service.returnBook(new ReturnLoanCommand(reader, id)).getStatus());
        var order = inOrder(database, bookDao, readerDao, loanDao);
        order.verify(database).lockTimeout();
        order.verify(loanDao).ownedLoan(id, reader, false);
        order.verify(bookDao).book(book, true);
        order.verify(loanDao).ownedLoan(id, reader, true);
        order.verify(database).now();
        order.verify(loanDao).markReturned(argThat(l -> l.getReturnTime().equals(now)));
        order.verify(bookDao).increment(argThat(b -> b.getAvailableCopies() == 2));
    }

    @Test
    void stockFailureRollsBackReturn() {
        when(loanDao.ownedLoan(id, reader, false)).thenReturn(loan(false));
        when(loanDao.ownedLoan(id, reader, true)).thenReturn(loan(false));
        assertThrows(
                IllegalStateException.class,
                () -> service.returnBook(new ReturnLoanCommand(reader, id)));
        verify(manager).rollback(any());
    }

    @Test
    void invalidIdentityRejectedBeforeDatabaseAccess() {
        assertEquals(
                401,
                assertThrows(BusinessException.class, () -> readers.authenticate(null))
                        .getStatus());
        assertEquals(
                401,
                assertThrows(BusinessException.class, () -> readers.authenticate("invalid"))
                        .getStatus());
        verify(readerDao, never()).readerExists(any());
    }
}
