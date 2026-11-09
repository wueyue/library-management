package com.example.library;

public class BusinessException extends RuntimeException {
    final int status;
    final String code;
    public BusinessException(int status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
}
