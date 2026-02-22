package com.catalog.service.storage;

import com.catalog.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class S3StorageService implements StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    @Override
    public String upload(String key, InputStream inputStream, String contentType, long contentLength) {
        log.info("S3 upload: bucket={}, key={}", bucket, key);
        try {
            s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key)
                    .contentType(contentType).contentLength(contentLength).build(),
                RequestBody.fromInputStream(inputStream, contentLength)
            );
            String url = endpoint.isBlank()
                    ? "https://%s.s3.amazonaws.com/%s".formatted(bucket, key)
                    : "%s/%s/%s".formatted(endpoint.replaceAll("/+$", ""), bucket, key);
            log.info("S3 upload OK: url={}", url);
            return url;
        } catch (S3Exception e) {
            throw new StorageException("S3 upload failed for key: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            log.warn("S3 delete failed key={}: {}", key, e.getMessage());
        }
    }
}
