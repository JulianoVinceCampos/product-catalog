package com.catalog.mapper;

import com.catalog.domain.model.Product;
import com.catalog.dto.request.ProductRequest;
import com.catalog.dto.response.ProductResponse;
import org.mapstruct.*;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductMapper {

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted",   ignore = true)
    @Mapping(target = "imageUrl",  ignore = true)
    @Mapping(target = "status",    defaultValue = "ACTIVE")
    Product toEntity(ProductRequest request);

    ProductResponse toResponse(Product product);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted",   ignore = true)
    @Mapping(target = "imageUrl",  ignore = true)
    void updateEntity(@MappingTarget Product product, ProductRequest request);
}
