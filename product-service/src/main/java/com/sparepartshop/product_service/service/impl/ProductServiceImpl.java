package com.sparepartshop.product_service.service.impl;

import com.sparepartshop.product_service.dto.ProductRequestDTO;
import com.sparepartshop.product_service.dto.ProductResponseDTO;
import com.sparepartshop.product_service.enums.Category;
import com.sparepartshop.product_service.entity.Product;
import com.sparepartshop.product_service.exception.DuplicateResourceException;
import com.sparepartshop.product_service.exception.ResourceNotFoundException;
import com.sparepartshop.product_service.repository.ProductRepository;
import com.sparepartshop.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        log.info("Creating product with part number: {}", request.getPartNumber());

        if (productRepository.existsByPartNumber(request.getPartNumber())) {
            log.warn("Duplicate part number attempted: {}", request.getPartNumber());
            throw new DuplicateResourceException(
                    "Product with part number '" + request.getPartNumber() + "' already exists"
            );
        }

        Product product = mapToEntity(request);
        Product savedProduct = productRepository.save(product);

        log.info("Product created successfully with id: {}", savedProduct.getId());
        return mapToResponseDTO(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(UUID id) {
        log.debug("Fetching product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id
                ));

        return mapToResponseDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getAllProducts(Pageable pageable) {
        log.debug("Fetching all active products with pagination: {}", pageable);

        return productRepository.findByActiveTrue(pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getProductsByCategory(Category category, Pageable pageable) {
        log.debug("Fetching products by category: {}", category);

        return productRepository.findByCategory(category, pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getProductsByBrand(String brand, Pageable pageable) {
        log.debug("Fetching products by brand: {}", brand);

        return productRepository.findByBrandIgnoreCase(brand, pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> searchProductsByName(String name, Pageable pageable) {
        log.debug("Searching products by name: {}", name);

        return productRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    public ProductResponseDTO updateProduct(UUID id, ProductRequestDTO request) {
        log.info("Updating product with id: {}", id);

        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id
                ));

        if (!existingProduct.getPartNumber().equalsIgnoreCase(request.getPartNumber())
                && productRepository.existsByPartNumber(request.getPartNumber())) {
            log.warn("Cannot update — part number already exists: {}", request.getPartNumber());
            throw new DuplicateResourceException(
                    "Product with part number '" + request.getPartNumber() + "' already exists"
            );
        }

        existingProduct.setName(request.getName());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setBrand(request.getBrand());
        existingProduct.setCategory(request.getCategory());
        existingProduct.setPartNumber(request.getPartNumber());
        existingProduct.setPrice(request.getPrice());
        existingProduct.setVehicleCompatibility(request.getVehicleCompatibility());

        Product updatedProduct = productRepository.save(existingProduct);

        log.info("Product updated successfully with id: {}", updatedProduct.getId());
        return mapToResponseDTO(updatedProduct);
    }

    @Override
    public void deleteProduct(UUID id) {
        log.info("Soft deleting product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id
                ));

        product.setActive(false);
        productRepository.save(product);

        log.info("Product soft-deleted successfully with id: {}", id);
    }

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
