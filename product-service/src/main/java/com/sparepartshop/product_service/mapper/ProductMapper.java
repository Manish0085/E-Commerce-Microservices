package com.sparepartshop.product_service.mapper;

import com.sparepartshop.product_service.dto.ProductRequestDTO;
import com.sparepartshop.product_service.dto.ProductResponseDTO;
import com.sparepartshop.product_service.entity.Product;

public class ProductMapper {

    private Product mapToEntity(ProductRequestDTO dto) {
        return Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .brand(dto.getBrand())
                .category(dto.getCategory())
                .partNumber(dto.getPartNumber())
                .price(dto.getPrice())
                .vehicleCompatibility(dto.getVehicleCompatibility())
                .active(true)
                .build();
    }


    private ProductResponseDTO mapToResponseDTO(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .category(product.getCategory())
                .partNumber(product.getPartNumber())
                .price(product.getPrice())
                .vehicleCompatibility(product.getVehicleCompatibility())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
