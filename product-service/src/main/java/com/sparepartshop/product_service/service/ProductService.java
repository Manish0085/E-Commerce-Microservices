package com.sparepartshop.product_service.service;

import com.sparepartshop.product_service.dto.ProductRequestDTO;
import com.sparepartshop.product_service.dto.ProductResponseDTO;
import com.sparepartshop.product_service.enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    ProductResponseDTO createProduct(ProductRequestDTO request);

    ProductResponseDTO getProductById(UUID id);

    Page<ProductResponseDTO> getAllProducts(Pageable pageable);

    Page<ProductResponseDTO> getProductsByCategory(Category category, Pageable pageable);

    Page<ProductResponseDTO> getProductsByBrand(String brand, Pageable pageable);

    Page<ProductResponseDTO> searchProductsByName(String name, Pageable pageable);

    ProductResponseDTO updateProduct(UUID id, ProductRequestDTO request);

    void deleteProduct(UUID id);
}
