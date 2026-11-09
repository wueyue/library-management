package com.example.library.service;

import java.util.UUID;
import static com.example.library.dto.Models.*;

/** Book 业务接口，实现位于 service.impl。 */
public interface BookService {
    BookResponse createBook(CreateBookRequest request);
    Page<BookResponse> books(int page,int size);
    BookResponse book(UUID id);
}
