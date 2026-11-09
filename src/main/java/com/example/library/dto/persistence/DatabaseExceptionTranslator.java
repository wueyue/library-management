package com.example.library.dto.persistence;

import com.example.library.exception.BusinessException;
import com.example.library.exception.ErrorCode;

import org.postgresql.util.PSQLException;
import org.springframework.dao.DataAccessException;

public final class DatabaseExceptionTranslator {
    private DatabaseExceptionTranslator() {
    }

    public static RuntimeException translate(DataAccessException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof PSQLException sql
                    && "23505".equals(sql.getSQLState())
                    && sql.getServerErrorMessage() != null) {
                String constraint = sql.getServerErrorMessage().getConstraint();
                if ("uq_active_loan".equals(constraint))
                    return new BusinessException(ErrorCode.BOOK_ALREADY_BORROWED, "你已借阅这本书，请先归还");
                if ("uq_reader_student_number".equals(constraint))
                    return new BusinessException(ErrorCode.STUDENT_NUMBER_ALREADY_EXISTS, "学号已存在");
            }
        }
        return exception;
    }
}
