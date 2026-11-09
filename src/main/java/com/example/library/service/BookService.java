package com.example.library.service;

import com.example.library.command.*;
import com.example.library.model.*;
import java.util.UUID;

/** Book 业务接口；不依赖 API 请求或响应 DTO。 */
public interface BookService {
    Book createBook(CreateBookCommand command);
    PageResult<Book> books(int page,int size);
    Book book(UUID id);
}
