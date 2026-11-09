package com.example.library.service.impl;

import com.example.library.command.*;
import com.example.library.dto.persistence.BookDao;
import com.example.library.model.Book;
import com.example.library.model.PageResult;
import com.example.library.service.BookService;
import com.example.library.service.WriteTransactionExecutor;

import org.springframework.stereotype.Service;

import java.util.UUID;

/** 图书录入和查询业务。 */
@Service
public class BookServiceImpl implements BookService {
    private final BookDao bookDao;
    private final WriteTransactionExecutor transactions;

    public BookServiceImpl(BookDao bookDao, WriteTransactionExecutor transactions) {
        this.bookDao = bookDao;
        this.transactions = transactions;
    }

    @Override
    public Book createBook(CreateBookCommand command) {
        return transactions.execute(
                () -> {
                    var book =
                            Book.create(
                                    command.title(),
                                    command.author(),
                                    command.description(),
                                    command.totalCopies(),
                                    transactions.now());
                    return bookDao.createBook(book);
                });
    }

    @Override
    public PageResult<Book> books(int page, int size) {
        return new PageResult<>(bookDao.books(page, size), page, size);
    }

    @Override
    public Book book(UUID id) {
        return bookDao.book(id, false);
    }
}
