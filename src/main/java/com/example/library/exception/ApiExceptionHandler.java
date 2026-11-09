package com.example.library.exception;

import static com.example.library.dto.Models.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private ResponseEntity<ErrorResponse> response(
            int status, String code, String message, HttpServletRequest r) {
        return ResponseEntity.status(status)
                .body(
                        new ErrorResponse(
                                new ErrorDetail(
                                        code, message, (String) r.getAttribute("requestId"))));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> business(BusinessException e, HttpServletRequest r) {
        return response(status(e.getErrorCode()), e.getCode(), e.getMessage(), r);
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        HandlerMethodValidationException.class
    })
    public ResponseEntity<ErrorResponse> invalid(Exception e, HttpServletRequest r) {
        return response(400, ErrorCode.INVALID_ARGUMENT.name(), "请求参数不合法", r);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> database(DataAccessException e, HttpServletRequest r) {
        if (e instanceof org.springframework.jdbc.CannotGetJdbcConnectionException)
            return response(503, ErrorCode.SERVICE_BUSY.name(), "数据库暂不可用", r);
        return unexpected(e, r);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest r) {
        if (e instanceof org.springframework.web.ErrorResponse http) {
            int status = http.getStatusCode().value();
            ErrorCode code =
                    switch (status) {
                        case 400 -> ErrorCode.INVALID_ARGUMENT;
                        case 404 -> ErrorCode.NOT_FOUND;
                        case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
                        case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
                        default -> ErrorCode.HTTP_ERROR;
                    };
            var result = response(status, code.name(), "HTTP 请求不受支持", r);
            return ResponseEntity.status(status).headers(http.getHeaders()).body(result.getBody());
        }
        log.error("request failed: {}", r.getAttribute("requestId"), e);
        return response(500, ErrorCode.INTERNAL_ERROR.name(), "服务内部错误", r);
    }

    private static int status(ErrorCode code) {
        return switch (code) {
            case INVALID_ARGUMENT -> 400;
            case UNAUTHENTICATED -> 401;
            case BOOK_NOT_FOUND, LOAN_NOT_FOUND, NOT_FOUND -> 404;
            case BOOK_ALREADY_BORROWED, BOOK_OUT_OF_STOCK, STUDENT_NUMBER_ALREADY_EXISTS -> 409;
            case SERVICE_BUSY -> 503;
            case METHOD_NOT_ALLOWED -> 405;
            case UNSUPPORTED_MEDIA_TYPE -> 415;
            default -> 500;
        };
    }
}
