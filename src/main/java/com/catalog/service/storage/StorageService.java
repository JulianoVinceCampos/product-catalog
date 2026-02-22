package com.catalog.service.storage;

import java.io.InputStream;

public interface StorageService {
    String upload(String key, InputStream inputStream, String contentType, long contentLength);
    void delete(String key);
}
