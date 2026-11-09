package com.example.library.model;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/** 借阅领域模型，归还后保留原始借阅记录。 */
@Getter
@Setter
public final class Loan {
    private UUID id;
    private UUID bookId;
    private UUID readerId;
    private OffsetDateTime borrowTime;
    private OffsetDateTime returnTime;
    private OffsetDateTime updateTime;

    public Loan(
            UUID id,
            UUID bookId,
            UUID readerId,
            OffsetDateTime borrowTime,
            OffsetDateTime returnTime,
            OffsetDateTime updateTime) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bookId, "bookId");
        Objects.requireNonNull(readerId, "readerId");
        Objects.requireNonNull(borrowTime, "borrowTime");
        Objects.requireNonNull(updateTime, "updateTime");
        if (returnTime != null && returnTime.isBefore(borrowTime))
            throw new IllegalArgumentException("return precedes borrow");
        this.id = id;
        this.bookId = bookId;
        this.readerId = readerId;
        this.borrowTime = borrowTime;
        this.returnTime = returnTime;
        this.updateTime = updateTime;
    }

    public static Loan borrow(UUID readerId, UUID bookId, OffsetDateTime now) {
        return new Loan(UUID.randomUUID(), bookId, readerId, now, null, now);
    }

    public Loan returnBook(OffsetDateTime now) {
        if (returnTime != null) return this;
        return new Loan(id, bookId, readerId, borrowTime, now, now);
    }

    public String getStatus() {
        return returnTime == null ? "BORROWED" : "RETURNED";
    }

    /** 可变模型持久化前必须验证。 */
    public void validate() {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bookId, "bookId");
        Objects.requireNonNull(readerId, "readerId");
        Objects.requireNonNull(borrowTime, "borrowTime");
        Objects.requireNonNull(updateTime, "updateTime");
        if (returnTime != null && returnTime.isBefore(borrowTime))
            throw new IllegalArgumentException("return precedes borrow");
    }
}
