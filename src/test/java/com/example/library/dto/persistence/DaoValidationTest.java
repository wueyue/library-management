package com.example.library.dto.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.library.model.Book;
import com.example.library.model.Loan;
import com.example.library.model.Reader;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

/** setter 修改后的非法状态必须在 SQL 执行之前被拒绝。 */
class DaoValidationTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    void invalidTitleSetBySetterNeverReachesDatabase() {
        var book = Book.create("Java", "作者", null, 2, now);
        book.setTitle(" ");
        assertThrows(IllegalArgumentException.class, () -> new BookDao(jdbc).createBook(book));
        verifyNoInteractions(jdbc);
    }

    @Test
    void invalidNumberSetBySetterNeverReachesDatabase() {
        var reader = Reader.create("张三", "00123", now);
        reader.setStudentNumber("  ");
        assertThrows(
                IllegalArgumentException.class, () -> new ReaderDao(jdbc).createReader(reader));
        verifyNoInteractions(jdbc);
    }

    @Test
    void protectedFieldsDoNotExposePublicSetters() {
        assertThrows(
                NoSuchMethodException.class,
                () -> Book.class.getMethod("setAvailableCopies", int.class));
        assertThrows(NoSuchMethodException.class, () -> Book.class.getMethod("setId", UUID.class));
        assertThrows(
                NoSuchMethodException.class,
                () -> Loan.class.getMethod("setReturnTime", OffsetDateTime.class));
        assertThrows(
                NoSuchMethodException.class, () -> Reader.class.getMethod("setId", UUID.class));
    }
}
