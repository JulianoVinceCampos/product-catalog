package com.catalog.domain.event;

import com.catalog.domain.model.Product;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ProductChangedEvent {

    public enum Type { CREATED, UPDATED, DELETED }

    private final UUID    productId;
    private final Type    type;
    private final Product product;
    private final Instant occurredAt;

    public ProductChangedEvent(Product product, Type type) {
        this.productId  = product.getId();
        this.product    = product;
        this.type       = type;
        this.occurredAt = Instant.now();
    }
}
