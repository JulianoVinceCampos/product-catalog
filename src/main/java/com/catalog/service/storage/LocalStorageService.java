package com.catalog.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalStorageService implements StorageService {

    @Override
    public String upload(String key, InputStream inputStream, String contentType, long contentLength) {
        log.info("[LOCAL-STORAGE] Simulated upload: key={}, type={}, bytes={}", key, contentType, contentLength);
        return "http://localhost:8080/static/" + key;
    }

    @Override
    public void delete(String key) {
        log.info("[LOCAL-STORAGE] Simulated delete: key={}", key);
    }
}
