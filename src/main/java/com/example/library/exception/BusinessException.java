package com.example.library.exception;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = java.util.Objects.requireNonNull(errorCode);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getCode() {
        return errorCode.name();
    }
}
