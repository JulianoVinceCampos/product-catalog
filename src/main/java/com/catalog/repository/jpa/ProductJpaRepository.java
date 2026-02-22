package com.catalog.repository.jpa;

import com.catalog.domain.model.Product;
import com.catalog.domain.model.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductJpaRepository extends JpaRepository<Product, UUID> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    Optional<Product> findByIdAndDeletedFalse(UUID id);

    @Query("""
            SELECT p FROM Product p
            WHERE p.deleted = false
              AND (:category IS NULL OR p.category = :category)
              AND (:status   IS NULL OR p.status   = :status)
            """)
    Page<Product> findAllActive(
            @Param("category") String        category,
            @Param("status")   ProductStatus status,
            Pageable pageable
    );
}
