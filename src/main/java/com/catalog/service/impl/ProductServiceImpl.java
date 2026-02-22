package com.catalog.service.impl;

import com.catalog.domain.event.ProductChangedEvent;
import com.catalog.domain.model.Product;
import com.catalog.domain.model.ProductDocument;
import com.catalog.domain.model.ProductStatus;
import com.catalog.dto.request.ProductRequest;
import com.catalog.dto.request.ProductSearchFilter;
import com.catalog.dto.response.PageResponse;
import com.catalog.dto.response.ProductResponse;
import com.catalog.exception.DuplicateSkuException;
import com.catalog.exception.ProductNotFoundException;
import com.catalog.exception.StorageException;
import com.catalog.mapper.ProductMapper;
import com.catalog.repository.elasticsearch.ProductSearchRepository;
import com.catalog.repository.jpa.ProductJpaRepository;
import com.catalog.service.ProductService;
import com.catalog.service.storage.StorageService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductJpaRepository     productRepository;
    private final ProductSearchRepository  searchRepository;
    private final ProductMapper            productMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final StorageService           storageService;
    private final Counter createdCounter;
    private final Counter updatedCounter;
    private final Counter deletedCounter;

    public ProductServiceImpl(
            ProductJpaRepository productRepository,
            ProductSearchRepository searchRepository,
            ProductMapper productMapper,
            ApplicationEventPublisher eventPublisher,
            StorageService storageService,
            MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.searchRepository  = searchRepository;
        this.productMapper     = productMapper;
        this.eventPublisher    = eventPublisher;
        this.storageService    = storageService;
        this.createdCounter = Counter.builder("product.created").description("Products created").register(meterRegistry);
        this.updatedCounter = Counter.builder("product.updated").description("Products updated").register(meterRegistry);
        this.deletedCounter = Counter.builder("product.deleted").description("Products deleted").register(meterRegistry);
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        log.info("Creating product: sku={}", request.getSku());
        if (productRepository.existsBySku(request.getSku())) throw new DuplicateSkuException(request.getSku());
        Product product = productMapper.toEntity(request);
        product = productRepository.save(product);
        createdCounter.increment();
        eventPublisher.publishEvent(new ProductChangedEvent(product, ProductChangedEvent.Type.CREATED));
        log.info("Product created: id={}", product.getId());
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductResponse findById(UUID id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(String category, ProductStatus status, int page, int size) {
        Page<Product> result = productRepository.findAllActive(
                category, status,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return PageResponse.from(result, productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductSearchFilter filter) {
        Page<ProductDocument> esPage = searchRepository.search(filter);
        List<UUID> ids = esPage.getContent().stream()
                .map(doc -> UUID.fromString(doc.getId())).toList();
        List<ProductResponse> responses = ids.isEmpty() ? List.of()
                : productRepository.findAllById(ids).stream()
                    .filter(p -> Boolean.FALSE.equals(p.getDeleted()))
                    .map(productMapper::toResponse).toList();
        return PageResponse.<ProductResponse>builder()
                .content(responses).page(esPage.getNumber()).size(esPage.getSize())
                .totalElements(esPage.getTotalElements()).totalPages(esPage.getTotalPages())
                .last(esPage.isLast()).first(esPage.isFirst()).build();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "search",   allEntries = true)
    })
    public ProductResponse update(UUID id, ProductRequest request) {
        log.info("Updating product: id={}", id);
        Product product = findActiveOrThrow(id);
        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySkuAndIdNot(request.getSku(), id))
            throw new DuplicateSkuException(request.getSku());
        productMapper.updateEntity(product, request);
        product = productRepository.save(product);
        updatedCounter.increment();
        eventPublisher.publishEvent(new ProductChangedEvent(product, ProductChangedEvent.Type.UPDATED));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "search",   allEntries = true)
    })
    public void delete(UUID id) {
        log.info("Soft-deleting product: id={}", id);
        Product product = findActiveOrThrow(id);
        product.softDelete();
        productRepository.save(product);
        deletedCounter.increment();
        eventPublisher.publishEvent(new ProductChangedEvent(product, ProductChangedEvent.Type.DELETED));
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductResponse uploadImage(UUID id, MultipartFile file) {
        Product product = findActiveOrThrow(id);
        try {
            String key = "products/%s/%s".formatted(id, file.getOriginalFilename());
            String url = storageService.upload(key, file.getInputStream(), file.getContentType(), file.getSize());
            product.setImageUrl(url);
            product = productRepository.save(product);
            eventPublisher.publishEvent(new ProductChangedEvent(product, ProductChangedEvent.Type.UPDATED));
            return productMapper.toResponse(product);
        } catch (IOException e) {
            throw new StorageException("Failed to read uploaded file", e);
        }
    }

    private Product findActiveOrThrow(UUID id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
