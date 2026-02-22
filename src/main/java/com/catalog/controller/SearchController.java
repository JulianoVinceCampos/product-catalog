package com.catalog.controller;

import com.catalog.dto.request.ProductSearchFilter;
import com.catalog.dto.response.PageResponse;
import com.catalog.dto.response.ProductResponse;
import com.catalog.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductService productService;

    @GetMapping("/products")
    public ResponseEntity<PageResponse<ProductResponse>> search(ProductSearchFilter filter) {
        return ResponseEntity.ok(productService.search(filter));
    }
}
