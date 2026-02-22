package com.catalog.dto.request;

import com.catalog.domain.model.ProductStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must have at most 100 characters")
    private String sku;

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 255, message = "Name must have between 3 and 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must have at most 5000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal price;

    @Size(max = 100, message = "Category must have at most 100 characters")
    private String category;

    private ProductStatus status;
}
