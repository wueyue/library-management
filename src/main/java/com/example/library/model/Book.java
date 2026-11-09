package com.example.library.model;

import lombok.Getter;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.Objects;

/** 图书领域模型，维护库存范围与借还状态变化。 */
@Getter
public final class Book {
    private final UUID id;
    private final String title;
    private final String author;
    private final String description;
    private final int totalCopies;
    private final int availableCopies;
    private final OffsetDateTime createTime;
    private final OffsetDateTime updateTime;
    public Book(UUID id, String title, String author, String description, int totalCopies, int availableCopies, OffsetDateTime createTime, OffsetDateTime updateTime) {
        Objects.requireNonNull(id,"id");
        Objects.requireNonNull(title,"title");
        Objects.requireNonNull(author,"author");
        Objects.requireNonNull(createTime,"createTime");
        Objects.requireNonNull(updateTime,"updateTime");
        if(totalCopies < 0 || availableCopies < 0 || availableCopies > totalCopies) throw new IllegalArgumentException("invalid stock");
        this.id=id;
        this.title=title;
        this.author=author;
        this.description=description;
        this.totalCopies=totalCopies;
        this.availableCopies=availableCopies;
        this.createTime=createTime;
        this.updateTime=updateTime;
    }
    /** 创建图书时由领域对象初始化 ID、库存与时间。 */
    public static Book create(String title, String author, String description, int totalCopies, OffsetDateTime now) {
        if (totalCopies < 0) throw new IllegalArgumentException("totalCopies must be nonnegative");
        return new Book(UUID.randomUUID(), required(title,255), required(author,255), description, totalCopies, totalCopies, now, now);
    }
    public Book borrowCopy(OffsetDateTime now) {
        if (availableCopies <= 0) throw new IllegalStateException("no available copy");
        return new Book(id,title,author,description,totalCopies,availableCopies-1,createTime,now);
    }
    public Book returnCopy(OffsetDateTime now) {
        if (availableCopies >= totalCopies) throw new IllegalStateException("stock invariant violated");
        return new Book(id,title,author,description,totalCopies,availableCopies+1,createTime,now);
    }
    private static String required(String value,int max) {
        if(value == null || value.isBlank() || value.length() > max) throw new IllegalArgumentException("invalid text");
        return value.strip();
    }
}
