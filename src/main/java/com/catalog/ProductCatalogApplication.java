package com.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ProductCatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductCatalogApplication.class, args);
    }
}
