package com.example.library.command;

import com.example.library.exception.BusinessException;

/** 保护非 HTTP 入口，避免业务操作只依赖 Controller 校验。 */
final class CommandValidation {
    private CommandValidation() {
    }

    static void text(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max) throw invalid();
    }

    static void required(Object value) {
        if (value == null) throw invalid();
    }

    static BusinessException invalid() {
        return new BusinessException(400, "INVALID_ARGUMENT", "业务操作参数不合法");
    }
}
