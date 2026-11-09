package com.example.library.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class DomainModelTest {
    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    void newBookInitializesStockAndNormalizesText() {
        var book = Book.create("  Java  ", "  作者  ", null, 3, now);
        assertNotNull(book.getId());
        assertEquals("Java", book.getTitle());
        assertEquals("作者", book.getAuthor());
        assertEquals(3, book.getAvailableCopies());
        assertEquals(book.getCreateTime(), book.getUpdateTime());
    }

    @Test
    void bookCannotBeOverborrowedOrOverreturned() {
        var book = Book.create("Java", "作者", null, 1, now);
        assertThrows(IllegalStateException.class, () -> book.returnCopy(now));
        var borrowed = book.borrowCopy(now.plusSeconds(1));
        assertEquals(1, book.getAvailableCopies());
        assertEquals(0, borrowed.getAvailableCopies());
        assertThrows(IllegalStateException.class, () -> borrowed.borrowCopy(now));
        assertEquals(1, borrowed.returnCopy(now.plusSeconds(2)).getAvailableCopies());
    }

    @Test
    void readerPreservesLeadingZerosInNumber() {
        var reader = Reader.create("  张三  ", "  00123  ", now);
        assertEquals("张三", reader.getName());
        assertEquals("00123", reader.getStudentNumber());
        assertEquals(reader.getCreateTime(), reader.getUpdateTime());
    }

    @Test
    void invalidDomainInputRejected() {
        assertThrows(
                IllegalArgumentException.class, () -> Book.create("Java", "作者", null, -1, now));
        assertThrows(IllegalArgumentException.class, () -> Reader.create("张三", " ", now));
    }

    @Test
    void loanReturnIsIdempotentAndKeepsBorrowTime() {
        var loan = Loan.borrow(UUID.randomUUID(), UUID.randomUUID(), now);
        assertEquals("BORROWED", loan.getStatus());
        var returned = loan.returnBook(now.plusHours(1));
        assertEquals("RETURNED", returned.getStatus());
        assertEquals(now, returned.getBorrowTime());
        assertEquals(returned.getReturnTime(), returned.getUpdateTime());
        assertSame(returned, returned.returnBook(now.plusHours(2)));
    }
}
