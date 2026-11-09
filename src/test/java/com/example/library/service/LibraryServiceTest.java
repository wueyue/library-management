package com.example.library.service;

import com.example.library.repository.LibraryRepository;
import com.example.library.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.example.library.model.Models.*;

class LibraryServiceTest {
    LibraryRepository repo;
    PlatformTransactionManager manager;
    LibraryService service;
    UUID reader=UUID.randomUUID(), book=UUID.randomUUID(), id=UUID.randomUUID();
    Loan loan(boolean returned) {
        var now=OffsetDateTime.now();
        return new Loan(id,book,reader,returned?"RETURNED":"BORROWED",now,returned?now:null,now);
    }
    @BeforeEach void setup() {
        repo=mock(LibraryRepository.class); manager=mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        service=new LibraryService(repo,manager);
    }
    @Test void borrowLocksBeforeStockAndCreation() {
        when(repo.decrement(book)).thenReturn(1);
        when(repo.createLoan(reader,book)).thenReturn(loan(false));
        assertEquals("BORROWED",service.borrow(reader,book).status());
        var order=inOrder(repo,manager);
        order.verify(repo).lockTimeout(); order.verify(repo).book(book,true);
        order.verify(repo).activeLoan(reader,book); order.verify(repo).decrement(book);
        order.verify(repo).createLoan(reader,book); order.verify(manager).commit(any());
    }
    @Test void duplicateHasPriorityAndRollsBack() {
        when(repo.activeLoan(reader,book)).thenReturn(true);
        var error=assertThrows(BusinessException.class,()->service.borrow(reader,book));
        assertEquals("BOOK_ALREADY_BORROWED",error.getCode());
        verify(repo,never()).decrement(any()); verify(manager).rollback(any());
    }
    @Test void noStockDoesNotCreateLoan() {
        var error=assertThrows(BusinessException.class,()->service.borrow(reader,book));
        assertEquals("BOOK_OUT_OF_STOCK",error.getCode());
        verify(repo,never()).createLoan(any(),any()); verify(manager).rollback(any());
    }
    @Test void insertFailureRollsBackTransaction() {
        when(repo.decrement(book)).thenReturn(1);
        when(repo.createLoan(reader,book)).thenThrow(new IllegalStateException("injected"));
        assertThrows(IllegalStateException.class,()->service.borrow(reader,book));
        verify(manager).rollback(any()); verify(manager,never()).commit(any());
    }
    @Test void returnIsIdempotent() {
        var returned=loan(true);
        when(repo.ownedLoan(id,reader,false)).thenReturn(returned);
        when(repo.ownedLoan(id,reader,true)).thenReturn(returned);
        assertSame(returned,service.returnBook(reader,id));
        verify(repo,never()).increment(any()); verify(repo,never()).markReturned(any());
    }
    @Test void returnUsesConsistentLockOrder() {
        when(repo.ownedLoan(id,reader,false)).thenReturn(loan(false));
        when(repo.ownedLoan(id,reader,true)).thenReturn(loan(false));
        when(repo.markReturned(id)).thenReturn(loan(true)); when(repo.increment(book)).thenReturn(1);
        assertEquals("RETURNED",service.returnBook(reader,id).status());
        var order=inOrder(repo);
        order.verify(repo).lockTimeout(); order.verify(repo).ownedLoan(id,reader,false);
        order.verify(repo).book(book,true); order.verify(repo).ownedLoan(id,reader,true);
        order.verify(repo).markReturned(id); order.verify(repo).increment(book);
    }
    @Test void stockFailureRollsBackReturn() {
        when(repo.ownedLoan(id,reader,false)).thenReturn(loan(false));
        when(repo.ownedLoan(id,reader,true)).thenReturn(loan(false));
        assertThrows(IllegalStateException.class,()->service.returnBook(reader,id));
        verify(manager).rollback(any());
    }
    @Test void invalidIdentityRejectedBeforeDatabaseAccess() {
        assertEquals(401,assertThrows(BusinessException.class,()->service.authenticate(null)).getStatus());
        assertEquals(401,assertThrows(BusinessException.class,()->service.authenticate("invalid")).getStatus());
        verifyNoInteractions(repo);
    }
}
