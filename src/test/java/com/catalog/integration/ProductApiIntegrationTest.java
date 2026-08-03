package com.catalog.integration;

import com.catalog.dto.request.ProductRequest;
import com.catalog.dto.response.ApiErrorResponse;
import com.catalog.dto.response.PageResponse;
import com.catalog.dto.response.ProductResponse;
import com.catalog.repository.jpa.ProductJpaRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Product API Integration Tests")
class ProductApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ProductJpaRepository repo;

    @BeforeEach void clean() { repo.deleteAll(); }

    private ProductRequest req(String sku) {
        var r = new ProductRequest();
        r.setSku(sku); r.setName("Integration Product"); r.setDescription("Test desc");
        r.setPrice(new BigDecimal("299.99")); r.setCategory("Integration");
        return r;
    }

    private ResponseEntity<ProductResponse> create(String sku) {
        return restTemplate.postForEntity("/api/products", req(sku), ProductResponse.class);
    }

    @Test @DisplayName("201: creates and persists product")
    void shouldCreate() {
        var res = create("IT-001");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().getId()).isNotNull();
        assertThat(repo.count()).isEqualTo(1);
    }

    @Test @DisplayName("409: duplicate SKU")
    void shouldRejectDuplicate() {
        create("DUP-SKU");
        var res = restTemplate.postForEntity("/api/products", req("DUP-SKU"), ApiErrorResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test @DisplayName("422 - validation: blank name")
    void shouldRejectBlankName() {
        var r = req("VAL-SKU"); r.setName("");
        var res = restTemplate.postForEntity("/api/products", r, ApiErrorResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody().getFieldErrors()).containsKey("name");
    }

    @Test @DisplayName("200: finds product by id")
    void shouldFindById() {
        UUID id = create("GET-001").getBody().getId();
        var res = restTemplate.getForEntity("/api/products/{id}", ProductResponse.class, id);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getId()).isEqualTo(id);
    }

    @Test @DisplayName("404: unknown id")
    void shouldReturn404() {
        var res = restTemplate.getForEntity("/api/products/{id}", ApiErrorResponse.class, UUID.randomUUID());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test @DisplayName("200: updates product")
    void shouldUpdate() {
        UUID id = create("UPD-001").getBody().getId();
        var r = req("UPD-001"); r.setName("Updated Name");
        var res = restTemplate.exchange("/api/products/{id}", HttpMethod.PUT, new HttpEntity<>(r), ProductResponse.class, id);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getName()).isEqualTo("Updated Name");
    }

    @Test @DisplayName("204: soft deletes, then 404")
    void shouldSoftDelete() {
        UUID id = create("DEL-001").getBody().getId();
        restTemplate.delete("/api/products/{id}", id);
        var res = restTemplate.getForEntity("/api/products/{id}", ApiErrorResponse.class, id);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(repo.findById(id)).isPresent(); // still in DB
    }

    @Test @DisplayName("200: lists products paginated")
    void shouldList() {
        create("LIST-A"); create("LIST-B"); create("LIST-C");
        var res = restTemplate.getForEntity("/api/products?page=0&size=2", PageResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getContent()).hasSize(2);
        assertThat(res.getBody().getTotalElements()).isEqualTo(3L);
    }

    @Test @DisplayName("200: filters by category")
    void shouldFilterByCategory() {
        create("CAT-A");
        var other = req("CAT-B"); other.setCategory("Other");
        restTemplate.postForEntity("/api/products", other, ProductResponse.class);
        var res = restTemplate.getForEntity("/api/products?category=Integration", PageResponse.class);
        assertThat(res.getBody().getTotalElements()).isEqualTo(1L);
    }
}
