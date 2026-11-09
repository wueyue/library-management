package com.example.library.model;

import lombok.Getter;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.Objects;

/** 读者领域模型，以学号唯一识别读者。 */
@Getter
public final class Reader {
    private final UUID id;
    private final String name;
    private final String studentNumber;
    private final OffsetDateTime createTime;
    private final OffsetDateTime updateTime;
    public Reader(UUID id, String name, String studentNumber, OffsetDateTime createTime, OffsetDateTime updateTime) {
        Objects.requireNonNull(id,"id");
        Objects.requireNonNull(name,"name");
        Objects.requireNonNull(studentNumber,"studentNumber");
        Objects.requireNonNull(createTime,"createTime");
        Objects.requireNonNull(updateTime,"updateTime");
        this.id=id;
        this.name=name;
        this.studentNumber=studentNumber;
        this.createTime=createTime;
        this.updateTime=updateTime;
    }
    /** 同名允许，学号去除首尾空白；全局唯一性由数据库约束负责。 */
    public static Reader create(String name, String studentNumber, OffsetDateTime now) {
        return new Reader(UUID.randomUUID(),required(name,100),required(studentNumber,64),now,now);
    }
    private static String required(String value,int max) {
        if(value == null || value.isBlank() || value.length() > max) throw new IllegalArgumentException("invalid text");
        return value.strip();
    }
}
