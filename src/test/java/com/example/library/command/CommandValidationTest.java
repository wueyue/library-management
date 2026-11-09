package com.example.library.command;

import com.example.library.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CommandValidationTest {
    @Test void nonHttpCallerCannotSubmitInvalidBook() {
        assertThrows(BusinessException.class,()->new CreateBookCommand(" ","作者",null,1));
        assertThrows(BusinessException.class,()->new CreateBookCommand("Java","作者",null,-1));
    }
    @Test void nonHttpCallerCannotSubmitMissingReaderNumber() {
        assertThrows(BusinessException.class,()->new CreateReaderCommand("张三",null));
        assertThrows(BusinessException.class,()->new CreateReaderCommand("张三","x".repeat(65)));
    }
    @Test void loanCommandsRequireIdentityAndTarget() {
        assertThrows(BusinessException.class,()->new BorrowLoanCommand(null,UUID.randomUUID()));
        assertThrows(BusinessException.class,()->new ReturnLoanCommand(UUID.randomUUID(),null));
    }
}
