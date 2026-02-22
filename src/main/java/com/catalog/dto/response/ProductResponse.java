package com.catalog.dto.response;

import com.catalog.domain.model.ProductStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class ProductResponse {
    private UUID          id;
    private String        sku;
    private String        name;
    private String        description;
    private BigDecimal    price;
    private String        category;
    private ProductStatus status;
    private String        imageUrl;
    private Instant       createdAt;
    private Instant       updatedAt;
}
