package com.catalog.service;

import com.catalog.domain.model.Product;
import com.catalog.domain.model.ProductStatus;
import com.catalog.dto.request.ProductRequest;
import com.catalog.dto.response.ProductResponse;
import com.catalog.exception.DuplicateSkuException;
import com.catalog.exception.ProductNotFoundException;
import com.catalog.mapper.ProductMapper;
import com.catalog.mapper.ProductMapperImpl;
import com.catalog.repository.elasticsearch.ProductSearchRepository;
import com.catalog.repository.jpa.ProductJpaRepository;
import com.catalog.service.impl.ProductServiceImpl;
import com.catalog.service.storage.LocalStorageService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock private ProductJpaRepository     productRepository;
    @Mock private ProductSearchRepository  searchRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ProductMapper   productMapper  = new ProductMapperImpl();
    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(productRepository, searchRepository,
                productMapper, eventPublisher, new LocalStorageService(), new SimpleMeterRegistry());
    }

    private ProductRequest req() {
        ProductRequest r = new ProductRequest();
        r.setSku("SKU-001"); r.setName("Test Product");
        r.setPrice(new BigDecimal("99.99")); r.setCategory("Test");
        return r;
    }

    private Product product(ProductRequest r) {
        return Product.builder().id(UUID.randomUUID()).sku(r.getSku())
                .name(r.getName()).price(r.getPrice()).status(ProductStatus.ACTIVE).deleted(Boolean.FALSE).build();
    }

    @Nested @DisplayName("create()")
    class Create {

        @Test @DisplayName("Happy path — saves and publishes event")
        void happyPath() {
            var r = req(); var saved = product(r);
            when(productRepository.existsBySku(r.getSku())).thenReturn(false);
            when(productRepository.save(any())).thenReturn(saved);
            var res = service.create(r);
            assertThat(res.getSku()).isEqualTo(r.getSku());
            assertThat(res.getStatus()).isEqualTo(ProductStatus.ACTIVE);
            verify(eventPublisher).publishEvent(any());
        }

        @Test @DisplayName("Throws DuplicateSkuException on duplicate SKU")
        void duplicateSku() {
            var r = req();
            when(productRepository.existsBySku(r.getSku())).thenReturn(true);
            assertThatThrownBy(() -> service.create(r)).isInstanceOf(DuplicateSkuException.class);
            verify(productRepository, never()).save(any());
        }
    }

    @Nested @DisplayName("findById()")
    class FindById {

        @Test @DisplayName("Returns product when found")
        void found() {
            UUID id = UUID.randomUUID();
            var p = product(req()); p.setId(id);
            when(productRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(p));
            assertThat(service.findById(id).getId()).isEqualTo(id);
        }

        @Test @DisplayName("Throws ProductNotFoundException when not found")
        void notFound() {
            UUID id = UUID.randomUUID();
            when(productRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.findById(id)).isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested @DisplayName("update()")
    class Update {

        @Test @DisplayName("Happy path — updates and publishes event")
        void happyPath() {
            UUID id = UUID.randomUUID();
            var r = req(); var p = product(r); p.setId(id);
            when(productRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(p));
            when(productRepository.existsBySkuAndIdNot(r.getSku(), id)).thenReturn(false);
            when(productRepository.save(any())).thenReturn(p);
            assertThatCode(() -> service.update(id, r)).doesNotThrowAnyException();
            verify(eventPublisher).publishEvent(any());
        }

        @Test @DisplayName("Throws when SKU conflicts with another product")
        void skuConflict() {
            UUID id = UUID.randomUUID();
            var r = req(); r.setSku("OTHER-SKU");
            var p = product(new ProductRequest()); p.setId(id); p.setSku("OLD-SKU");
            when(productRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(p));
            when(productRepository.existsBySkuAndIdNot("OTHER-SKU", id)).thenReturn(true);
            assertThatThrownBy(() -> service.update(id, r)).isInstanceOf(DuplicateSkuException.class);
        }
    }

    @Nested @DisplayName("delete()")
    class Delete {

        @Test @DisplayName("Soft-deletes — sets deleted=true and status=INACTIVE")
        void softDelete() {
            UUID id = UUID.randomUUID();
            var p = product(req()); p.setId(id);
            when(productRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(p));
            when(productRepository.save(any())).thenReturn(p);
            service.delete(id);
            assertThat(p.getDeleted()).isTrue();
            assertThat(p.getStatus()).isEqualTo(ProductStatus.INACTIVE);
            verify(eventPublisher).publishEvent(any());
        }

        @Test @DisplayName("Throws ProductNotFoundException when not found")
        void notFound() {
            UUID id = UUID.randomUUID();
            when(productRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ProductNotFoundException.class);
            verify(productRepository, never()).save(any());
        }
    }
}
