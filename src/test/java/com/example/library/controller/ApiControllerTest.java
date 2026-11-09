package com.example.library.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.library.command.*;
import com.example.library.exception.BusinessException;
import com.example.library.model.*;
import com.example.library.service.BookService;
import com.example.library.service.LoanService;
import com.example.library.service.ReaderService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

@WebMvcTest({
    BookController.class, ReaderController.class, LoanController.class
})
class ApiControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean BookService bookService;
    @MockitoBean ReaderService readerService;
    @MockitoBean LoanService loanService;

    @Test
    void borrowUsesSnakeCaseAndCreatedResponse() throws Exception {
        var reader = UUID.randomUUID();
        var book = UUID.randomUUID();
        var now = OffsetDateTime.now();
        when(readerService.authenticate(reader.toString())).thenReturn(reader);
        when(loanService.borrow(new BorrowLoanCommand(reader, book)))
                .thenReturn(new Loan(UUID.randomUUID(), book, reader, now, null, now));
        mvc.perform(
                        post("/api/v1/loans")
                                .header("X-Reader-Id", reader)
                                .contentType("application/json")
                                .content("{\"book_id\":\"" + book + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.book_id").value(book.toString()))
                .andExpect(jsonPath("$.borrow_time").exists())
                .andExpect(jsonPath("$.update_time").exists())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void invalidBodyAndPaginationReturn400() throws Exception {
        mvc.perform(post("/api/v1/loans").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
        mvc.perform(get("/api/v1/books?page=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/books?page_size=101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/books/not-uuid")).andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/v1/loans")
                                .contentType("application/json")
                                .content("{\"unexpected\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void businessErrorKeepsRequestId() throws Exception {
        when(readerService.authenticate(null))
                .thenThrow(new BusinessException(401, "UNAUTHENTICATED", "读者身份无效"));
        mvc.perform(get("/api/v1/me/loans"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.request_id").exists())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void createBookAndReaderReturnGeneratedFields() throws Exception {
        var now = OffsetDateTime.now();
        var id = UUID.randomUUID();
        when(bookService.createBook(any()))
                .thenReturn(new Book(id, "Java", "作者", null, 3, 3, now, now));
        mvc.perform(
                        post("/api/v1/books/insert")
                                .contentType("application/json")
                                .content(
                                        "{\"title\":\"Java\",\"author\":\"作者\",\"total_copies\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.available_copies").value(3))
                .andExpect(jsonPath("$.create_time").exists())
                .andExpect(jsonPath("$.update_time").exists());
        when(readerService.createReader(any()))
                .thenReturn(new Reader(id, "张三", "20260001", now, now));
        mvc.perform(
                        post("/api/v1/readers/insert")
                                .contentType("application/json")
                                .content("{\"name\":\"张三\",\"student_number\":\"20260001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("张三"))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.student_number").value("20260001"))
                .andExpect(jsonPath("$.update_time").exists());
    }

    @Test
    void controllerConvertsEntryRequestsToCommands() throws Exception {
        var now = OffsetDateTime.now();
        when(bookService.createBook(any())).thenReturn(Book.create("Java", "作者", null, 3, now));
        when(readerService.createReader(any())).thenReturn(Reader.create("张三", "00123", now));
        mvc.perform(
                        post("/api/v1/books/insert")
                                .contentType("application/json")
                                .content(
                                        "{\"title\":\"Java\",\"author\":\"作者\",\"total_copies\":3}"))
                .andExpect(status().isCreated());
        mvc.perform(
                        post("/api/v1/readers/insert")
                                .contentType("application/json")
                                .content("{\"name\":\"张三\",\"student_number\":\"00123\"}"))
                .andExpect(status().isCreated());
        verify(bookService).createBook(new CreateBookCommand("Java", "作者", null, 3));
        verify(readerService).createReader(new CreateReaderCommand("张三", "00123"));
    }

    @Test
    void bookQueriesMapDomainResultsToResponses() throws Exception {
        var id = UUID.randomUUID();
        var now = OffsetDateTime.now();
        var book = new Book(id, "Java", "作者", null, 3, 2, now, now);
        when(bookService.books(1, 20)).thenReturn(new PageResult<>(java.util.List.of(book), 1, 20));
        when(bookService.book(id)).thenReturn(book);
        mvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].available_copies").value(2))
                .andExpect(jsonPath("$.page_size").value(20));
        mvc.perform(get("/api/v1/books/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void loanQueriesAndReturnMapDomainResultsToResponses() throws Exception {
        var reader = UUID.randomUUID();
        var book = UUID.randomUUID();
        var id = UUID.randomUUID();
        var now = OffsetDateTime.now();
        var loan = new Loan(id, book, reader, now, null, now);
        when(readerService.authenticate(reader.toString())).thenReturn(reader);
        when(loanService.current(reader, 1, 20))
                .thenReturn(
                        new PageResult<>(
                                java.util.List.of(new CurrentBorrowing(loan, "Java", "作者")),
                                1,
                                20));
        when(loanService.returnBook(new ReturnLoanCommand(reader, id)))
                .thenReturn(loan.returnBook(now.plusHours(1)));
        mvc.perform(get("/api/v1/me/loans").header("X-Reader-Id", reader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].book.title").value("Java"));
        mvc.perform(post("/api/v1/loans/" + id + "/return").header("X-Reader-Id", reader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
        verify(loanService).returnBook(new ReturnLoanCommand(reader, id));
    }

    @Test
    void entryValidationRejectsInvalidAndServerOwnedFields() throws Exception {
        for (String body :
                new String[] {
                    "{}",
                    "{\"title\":\" \",\"author\":\"a\",\"total_copies\":1}",
                    "{\"title\":\"t\",\"author\":\"a\",\"total_copies\":-1}",
                    "{\"title\":\"t\",\"author\":\"a\",\"total_copies\":1,\"available_copies\":9}"
                }) {
            mvc.perform(post("/api/v1/books/insert").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
        }
        for (String body :
                new String[] {
                    "{}",
                    "{\"name\":\" \"}",
                    "{\"name\":\"" + "a".repeat(101) + "\"}",
                    "{\"name\":\"张三\",\"id\":\"x\"}"
                }) {
            mvc.perform(
                            post("/api/v1/readers/insert")
                                    .contentType("application/json")
                                    .content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(bookService, never()).createBook(any());
        verify(readerService, never()).createReader(any());
    }

    @Test
    void studentNumberValidation() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        for (String number : new String[] {
            "", "   ", "x".repeat(65)
        }) {
            var body =
                    mapper.writeValueAsString(
                            java.util.Map.of("name", "张三", "student_number", number));
            mvc.perform(
                            post("/api/v1/readers/insert")
                                    .contentType("application/json")
                                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
        }
        mvc.perform(
                        post("/api/v1/readers/insert")
                                .contentType("application/json")
                                .content("{\"name\":\"张三\"}"))
                .andExpect(status().isBadRequest());
        verify(readerService, never()).createReader(any());
    }

    @Test
    void duplicateReaderNumberReturns409() throws Exception {
        var serverError =
                new org.postgresql.util.ServerErrorMessage(
                        "SERROR\0C23505\0Mduplicate\0nuq_reader_student_number\0");
        var sql = new org.postgresql.util.PSQLException(serverError);
        when(readerService.createReader(any()))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate", sql));
        mvc.perform(
                        post("/api/v1/readers/insert")
                                .contentType("application/json")
                                .content("{\"name\":\"张三\",\"student_number\":\"20260001\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("STUDENT_NUMBER_ALREADY_EXISTS"));
    }
}
