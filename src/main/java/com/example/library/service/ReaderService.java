package com.example.library.service;

import com.example.library.command.*;
import com.example.library.model.*;
import java.util.UUID;

/** Reader 业务接口；不依赖 API 请求或响应 DTO。 */
public interface ReaderService {
    Reader createReader(CreateReaderCommand command);
    UUID authenticate(String value);
}
