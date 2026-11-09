package com.example.library.command;

import java.util.UUID;

/** 应用层操作入参，不依赖 HTTP、JSON 或请求 DTO。 */
public record CreateReaderCommand(String name, String studentNumber) {
    public CreateReaderCommand {
        CommandValidation.text(name,100); CommandValidation.text(studentNumber,64);
    }
}
