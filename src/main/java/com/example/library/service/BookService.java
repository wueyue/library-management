package com.example.library.service;

import com.example.library.repository.LibraryRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;
import static com.example.library.model.Models.*;

/** 图书录入和查询业务。 */
@Service
public class BookService {
    private final LibraryRepository repo;
    private final WriteTransactionExecutor transactions;
    public BookService(LibraryRepository repo, WriteTransactionExecutor transactions) {
        this.repo=repo; this.transactions=transactions;
    }
    public Book createBook(CreateBookRequest request) { return transactions.execute(() -> repo.createBook(request)); }
    public Page<Book> books(int page,int size) { return new Page<>(repo.books(page,size),page,size); }
    public Book book(UUID id) { return repo.book(id,false); }
}
