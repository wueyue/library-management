package com.example.library.dto.persistence;

import com.example.library.model.Book;
import com.example.library.model.Reader;
import com.example.library.model.Loan;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** setter 修改后的非法状态必须在 SQL 执行之前被拒绝。 */
class DaoValidationTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final OffsetDateTime now=OffsetDateTime.now();
    @Test void invalidStockSetBySetterNeverReachesDatabase() {
        var book=Book.create("Java","作者",null,2,now);
        book.setAvailableCopies(3);
        assertEquals(3,book.getAvailableCopies());
        assertThrows(IllegalArgumentException.class,()->new BookDao(jdbc).createBook(book));
        verifyNoInteractions(jdbc);
    }
    @Test void invalidNumberSetBySetterNeverReachesDatabase() {
        var reader=Reader.create("张三","00123",now);
        reader.setStudentNumber("  ");
        assertThrows(IllegalArgumentException.class,()->new ReaderDao(jdbc).createReader(reader));
        verifyNoInteractions(jdbc);
    }
    @Test void invalidReturnTimeSetBySetterNeverReachesDatabase() {
        var loan=Loan.borrow(UUID.randomUUID(),UUID.randomUUID(),now);
        loan.setReturnTime(now.minusHours(1));
        assertThrows(IllegalArgumentException.class,()->new LoanDao(jdbc).markReturned(loan));
        verifyNoInteractions(jdbc);
    }
}
