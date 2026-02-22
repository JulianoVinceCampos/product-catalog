package com.catalog.service;

import com.catalog.domain.model.ProductStatus;
import com.catalog.dto.request.ProductRequest;
import com.catalog.dto.request.ProductSearchFilter;
import com.catalog.dto.response.PageResponse;
import com.catalog.dto.response.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ProductService {
    ProductResponse create(ProductRequest request);
    ProductResponse findById(UUID id);
    ProductResponse update(UUID id, ProductRequest request);
    void delete(UUID id);
    PageResponse<ProductResponse> findAll(String category, ProductStatus status, int page, int size);
    PageResponse<ProductResponse> search(ProductSearchFilter filter);
    ProductResponse uploadImage(UUID id, MultipartFile file);
}
