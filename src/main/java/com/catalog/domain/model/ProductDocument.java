package com.catalog.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.Instant;

@Document(indexName = "products", createIndex = true)
@Setting(shards = 1, replicas = 0)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String sku;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSX")
    private Instant createdAt;

    @Field(type = FieldType.Boolean)
    private Boolean deleted;

    public static ProductDocument from(Product p) {
        return ProductDocument.builder()
                .id(p.getId().toString())
                .sku(p.getSku())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice() != null ? p.getPrice().doubleValue() : null)
                .category(p.getCategory())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .createdAt(p.getCreatedAt())
                .deleted(p.getDeleted())
                .build();
    }
}
