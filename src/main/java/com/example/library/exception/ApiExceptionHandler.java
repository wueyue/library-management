package com.example.library.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.example.library.dto.Models.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log=LoggerFactory.getLogger(ApiExceptionHandler.class);
    private ResponseEntity<ErrorResponse> response(int status,String code,String message,HttpServletRequest r) {
        return ResponseEntity.status(status).body(new ErrorResponse(new ErrorDetail(code,message,(String)r.getAttribute("requestId"))));
    }
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> business(BusinessException e,HttpServletRequest r) { return response(e.getStatus(),e.getCode(),e.getMessage(),r); }
    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class,HttpMessageNotReadableException.class,HandlerMethodValidationException.class})
    public ResponseEntity<ErrorResponse> invalid(Exception e,HttpServletRequest r) { return response(400,"INVALID_ARGUMENT","请求参数不合法",r); }
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> database(DataAccessException e,HttpServletRequest r) {
        // 精确匹配目标唯一索引，不将其他数据库错误误报为重复借阅。
        for(Throwable c=e;c!=null;c=c.getCause()) {
            if(c instanceof org.postgresql.util.PSQLException sql && "23505".equals(sql.getSQLState())
                && sql.getServerErrorMessage()!=null
                && "uq_active_loan".equals(sql.getServerErrorMessage().getConstraint()))
                return response(409,"BOOK_ALREADY_BORROWED","你已借阅这本书，请先归还",r);
            if(c instanceof org.postgresql.util.PSQLException sql && "23505".equals(sql.getSQLState())
                && sql.getServerErrorMessage()!=null
                && "uq_reader_student_number".equals(sql.getServerErrorMessage().getConstraint()))
                return response(409,"STUDENT_NUMBER_ALREADY_EXISTS","学号已存在",r);
        }
        if(e instanceof org.springframework.jdbc.CannotGetJdbcConnectionException)
            return response(503,"SERVICE_BUSY","数据库暂不可用",r);
        return unexpected(e,r);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception e,HttpServletRequest r) {
        log.error("request failed: {}",r.getAttribute("requestId"),e);
        return response(500,"INTERNAL_ERROR","服务内部错误",r);
    }
}
