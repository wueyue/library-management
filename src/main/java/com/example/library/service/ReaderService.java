package com.example.library.service;

import java.util.UUID;
import static com.example.library.dto.Models.*;

/** Reader 业务接口，实现位于 service.impl。 */
public interface ReaderService {
    ReaderResponse createReader(CreateReaderRequest request);
    UUID authenticate(String value);
}
