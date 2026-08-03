package com.catalog.service;

import com.catalog.domain.event.ProductChangedEvent;
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
import org.mockito.ArgumentCaptor;
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

    @Mock private ProductJpaRepository      productRepository;
    @Mock private ProductSearchRepository   searchRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private final ProductMapper productMapper = new ProductMapperImpl();
    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(
                productRepository,
                searchRepository,
                productMapper,
                eventPublisher,
                new LocalStorageService(),
                new SimpleMeterRegistry()
        );
    }

    private ProductRequest req() {
        ProductRequest r = new ProductRequest();
        r.setSku("SKU-001");
        r.setName("Test Product");
        r.setPrice(new BigDecimal("99.99"));
        r.setCategory("Test");
        return r;
    }

    private Product product(UUID id, String sku) {
        return Product.builder()
                .id(id)
                .sku(sku)
                .name("Test Product")
                .price(new BigDecimal("99.99"))
                .category("Test")
                .status(ProductStatus.ACTIVE)
                .deleted(Boolean.FALSE)
                .build();
    }

    private static void assertEventType(ApplicationEventPublisher publisher, ProductChangedEvent.Type type) {
        ArgumentCaptor<ProductChangedEvent> captor = ArgumentCaptor.forClass(ProductChangedEvent.class);
        verify(publisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(type);
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Happy path: saves and publishes CREATED event")
        void happyPath() {
            var r = req();
            var id = UUID.randomUUID();
            var saved = product(id, r.getSku());

            when(productRepository.existsBySku(r.getSku())).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenReturn(saved);

            ProductResponse res = service.create(r);

            assertThat(res.getId()).isEqualTo(id);
            assertThat(res.getSku()).isEqualTo(r.getSku());
            assertThat(res.getStatus()).isEqualTo(ProductStatus.ACTIVE);

            assertEventType(eventPublisher, ProductChangedEvent.Type.CREATED);
        }

        @Test
        @DisplayName("Throws DuplicateSkuException on duplicate SKU")
        void duplicateSku() {
            var r = req();
            when(productRepository.existsBySku(r.getSku())).thenReturn(true);

            assertThatThrownBy(() -> service.create(r))
                    .isInstanceOf(DuplicateSkuException.class);

            verify(productRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Returns product when found")
        void found() {
            UUID id = UUID.randomUUID();
            var p = product(id, "SKU-FOUND");

            when(productRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(p));

            assertThat(service.findById(id).getId()).isEqualTo(id);

            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Throws ProductNotFoundException when not found")
        void notFound() {
            UUID id = UUID.randomUUID();
            when(productRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(id))
                    .isInstanceOf(ProductNotFoundException.class);

            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Happy path: updates and publishes UPDATED event")
        void happyPath() {
            UUID id = UUID.randomUUID();

            // Força o fluxo a entrar na validação de SKU (sem gerar UnnecessaryStubbing)
            var existing = product(id, "OLD-SKU");

            var r = req(); // SKU-001
            when(productRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(existing));
            when(productRepository.existsBySkuAndIdNot(r.getSku(), id)).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenReturn(existing);

            assertThatCode(() -> service.update(id, r)).doesNotThrowAnyException();

            assertEventType(eventPublisher, ProductChangedEvent.Type.UPDATED);
        }

        @Test
        @DisplayName("Throws when SKU conflicts with another product")
        void skuConflict() {
            UUID id = UUID.randomUUID();
            var existing = product(id, "OLD-SKU");

            var r = req();
            r.setSku("OTHER-SKU");

            when(productRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(existing));
            when(productRepository.existsBySkuAndIdNot("OTHER-SKU", id)).thenReturn(true);

            assertThatThrownBy(() -> service.update(id, r))
                    .isInstanceOf(DuplicateSkuException.class);

            verify(productRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Soft-deletes: sets deleted=true and status=INACTIVE and publishes DELETED event")
        void softDelete() {
            UUID id = UUID.randomUUID();
            var p = product(id, "SKU-DEL");

            when(productRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(p));
            when(productRepository.save(any(Product.class))).thenReturn(p);

            service.delete(id);

            assertThat(p.getDeleted()).isTrue();
            assertThat(p.getStatus()).isEqualTo(ProductStatus.INACTIVE);

            assertEventType(eventPublisher, ProductChangedEvent.Type.DELETED);
        }

        @Test
        @DisplayName("Throws ProductNotFoundException when not found")
        void notFound() {
            UUID id = UUID.randomUUID();
            when(productRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(id))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(productRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }
    }
}
