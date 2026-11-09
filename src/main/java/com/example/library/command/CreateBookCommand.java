package com.example.library.command;

import java.util.UUID;

/** 应用层操作入参，不依赖 HTTP、JSON 或请求 DTO。 */
public record CreateBookCommand(String title, String author, String description, int totalCopies) {
    public CreateBookCommand {
        CommandValidation.text(title,255); CommandValidation.text(author,255);
        if(totalCopies<0) throw CommandValidation.invalid();
    }
}
