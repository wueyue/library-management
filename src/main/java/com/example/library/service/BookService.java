package com.example.library.service;

import com.example.library.repository.LibraryRepository;
import org.springframework.stereotype.Service;
import com.example.library.model.Book;
import com.example.library.dto.ResponseMapper;
import java.util.UUID;
import static com.example.library.dto.Models.*;

/** 图书录入和查询业务。 */
@Service
public class BookService {
    private final LibraryRepository repo;
    private final WriteTransactionExecutor transactions;
    public BookService(LibraryRepository repo, WriteTransactionExecutor transactions) {
        this.repo=repo; this.transactions=transactions;
    }
    public BookResponse createBook(CreateBookRequest request) { return transactions.execute(() -> {
        var book=Book.create(request.title(),request.author(),request.description(),request.totalCopies(),repo.now());
        return ResponseMapper.book(repo.createBook(book));
    }); }
    public Page<BookResponse> books(int page,int size) { return new Page<>(repo.books(page,size).stream().map(ResponseMapper::book).toList(),page,size); }
    public BookResponse book(UUID id) { return ResponseMapper.book(repo.book(id,false)); }
}
