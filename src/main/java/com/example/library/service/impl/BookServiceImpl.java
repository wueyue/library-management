package com.example.library.service.impl;

import com.example.library.service.BookService;
import com.example.library.service.WriteTransactionExecutor;

import com.example.library.dto.persistence.BookDao;
import org.springframework.stereotype.Service;
import com.example.library.model.Book;
import com.example.library.dto.ResponseMapper;
import java.util.UUID;
import static com.example.library.dto.Models.*;

/** 图书录入和查询业务。 */
@Service
public class BookServiceImpl implements BookService {
    private final BookDao bookDao;
    private final WriteTransactionExecutor transactions;
    public BookServiceImpl(BookDao bookDao, WriteTransactionExecutor transactions) {
        this.bookDao=bookDao;
        this.transactions=transactions;
    }
    @Override
    public BookResponse createBook(CreateBookRequest request) { return transactions.execute(() -> {
        var book=Book.create(request.title(),request.author(),request.description(),request.totalCopies(),transactions.now());
        return ResponseMapper.book(bookDao.createBook(book));
    }); }
    @Override
    public Page<BookResponse> books(int page,int size) { return new Page<>(bookDao.books(page,size).stream().map(ResponseMapper::book).toList(),page,size); }
    @Override
    public BookResponse book(UUID id) { return ResponseMapper.book(bookDao.book(id,false)); }
}
