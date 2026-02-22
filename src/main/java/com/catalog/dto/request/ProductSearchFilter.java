package com.catalog.dto.request;

import com.catalog.domain.model.ProductStatus;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

@Data
public class ProductSearchFilter {

    private String q;
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private ProductStatus status;
    private String sort  = "createdAt";
    private String order = "desc";
    private int page = 0;
    private int size = 20;

    public Pageable toPageable() {
        Sort.Direction dir   = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField     = "price".equalsIgnoreCase(sort) ? "price" : "createdAt";
        int safeSize         = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(dir, sortField));
    }

    public boolean isHighPage() { return page > 50; }
}
