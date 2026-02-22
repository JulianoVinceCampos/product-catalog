package com.catalog.controller;

import com.catalog.domain.model.ProductStatus;
import com.catalog.dto.request.ProductRequest;
import com.catalog.dto.response.PageResponse;
import com.catalog.dto.response.ProductResponse;
import com.catalog.exception.DuplicateSkuException;
import com.catalog.exception.GlobalExceptionHandler;
import com.catalog.exception.ProductNotFoundException;
import com.catalog.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ProductController.class, GlobalExceptionHandler.class})
@DisplayName("ProductController Tests (MockMvc)")
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  ProductService productService;

    private final ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());

    private ProductRequest req() {
        var r = new ProductRequest();
        r.setSku("SKU-CTRL-001"); r.setName("Controller Product");
        r.setPrice(new BigDecimal("199.99")); r.setCategory("Test");
        return r;
    }

    private ProductResponse resp(UUID id) {
        return ProductResponse.builder().id(id).sku("SKU-CTRL-001").name("Controller Product")
                .price(new BigDecimal("199.99")).category("Test").status(ProductStatus.ACTIVE)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    @Test @DisplayName("POST 201 — creates product")
    void createOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.create(any())).thenReturn(resp(id));
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test @DisplayName("POST 422 — name too short")
    void createValidationError() throws Exception {
        var r = req(); r.setName("AB");
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(r)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test @DisplayName("POST 422 — price is zero")
    void createPriceZero() throws Exception {
        var r = req(); r.setPrice(BigDecimal.ZERO);
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(r)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test @DisplayName("POST 409 — duplicate SKU")
    void createDuplicate() throws Exception {
        when(productService.create(any())).thenThrow(new DuplicateSkuException("SKU-CTRL-001"));
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test @DisplayName("GET 200 — returns product")
    void getByIdOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.findById(id)).thenReturn(resp(id));
        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test @DisplayName("GET 404 — product not found")
    void getByIdNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.findById(id)).thenThrow(new ProductNotFoundException(id));
        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test @DisplayName("PUT 200 — updates product")
    void updateOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.update(eq(id), any())).thenReturn(resp(id));
        mockMvc.perform(put("/api/products/{id}", id).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req())))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("DELETE 204 — soft deletes product")
    void deleteOk() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(productService).delete(id);
        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test @DisplayName("GET list 200 — paginated results")
    void listOk() throws Exception {
        UUID id = UUID.randomUUID();
        var page = PageResponse.<ProductResponse>builder()
                .content(List.of(resp(id))).page(0).size(20).totalElements(1).totalPages(1).last(true).first(true).build();
        when(productService.findAll(any(), any(), anyInt(), anyInt())).thenReturn(page);
        mockMvc.perform(get("/api/products").param("page","0").param("size","20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content").isArray());
    }
}
