package com.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
public class AwsConfig {

    @Value("${aws.region:us-east-1}")    private String region;
    @Value("${aws.access-key:test}")     private String accessKey;
    @Value("${aws.secret-key:test}")     private String secretKey;
    @Value("${aws.s3.endpoint:}")        private String endpoint;

    @Bean
    public S3Client s3Client() {
        var b = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
        if (!endpoint.isBlank())
            b.endpointOverride(URI.create(endpoint))
             .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        return b.build();
    }
}
