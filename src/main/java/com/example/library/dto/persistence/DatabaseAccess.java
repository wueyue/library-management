package com.example.library.dto.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

/** 事务共享的数据库时钟与锁等待设置，不包含业务数据写入。 */
@Repository
public class DatabaseAccess {
    private final JdbcTemplate jdbc;

    public DatabaseAccess(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public OffsetDateTime now() {
        return jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
    }

    public void lockTimeout() {
        jdbc.execute("SET LOCAL lock_timeout = '2s'");
    }
}
