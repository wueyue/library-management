package com.example.library.dto;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** API 请求和响应结构；JSON 字段统一为 snake_case。 */
public final class Models {
    private Models() {}
    public record CreateBookRequest(@NotBlank @Size(max=255) String title,
                                    @NotBlank @Size(max=255) String author,
                                    String description, @NotNull @Min(0) Integer totalCopies) {}
    public record CreateReaderRequest(@NotBlank @Size(max=100) String name,
                                       @NotBlank @Size(max=64) String studentNumber) {}
    public record ReaderResponse(UUID id, String name, String studentNumber, OffsetDateTime createTime, OffsetDateTime updateTime) {}
    public record BorrowRequest(@NotNull UUID bookId) {}
    public record BookResponse(UUID id, String title, String author, String description,
                       int totalCopies, int availableCopies, OffsetDateTime createTime,
                       OffsetDateTime updateTime) {}
    public record BookRef(UUID id, String title, String author) {}
    public record LoanResponse(UUID id, UUID bookId, UUID readerId, String status,
                       OffsetDateTime borrowTime, OffsetDateTime returnTime, OffsetDateTime updateTime) {}
    public record CurrentLoan(UUID id, UUID bookId, UUID readerId, String status,
                              OffsetDateTime borrowTime, OffsetDateTime returnTime,
                              OffsetDateTime updateTime, BookRef book) {}
    public record Page<T>(List<T> items, int page, int pageSize) {}
    public record ErrorDetail(String code, String message, String requestId) {}
    public record ErrorResponse(ErrorDetail error) {}
}
