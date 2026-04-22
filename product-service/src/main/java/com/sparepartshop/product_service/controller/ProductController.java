package com.sparepartshop.product_service.controller;

import com.sparepartshop.product_service.constants.ApiPaths;
import com.sparepartshop.product_service.dto.ApiResponse;
import com.sparepartshop.product_service.dto.ProductRequestDTO;
import com.sparepartshop.product_service.dto.ProductResponseDTO;
import com.sparepartshop.product_service.enums.Category;
import com.sparepartshop.product_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.PRODUCTS)
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDTO>> createProduct(
            @Valid @RequestBody ProductRequestDTO request) {

        log.info("Received request to create product: {}", request.getPartNumber());
        ProductResponseDTO createdProduct = productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", createdProduct));
    }

    @GetMapping(ApiPaths.PRODUCT_BY_ID)
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getProductById(@PathVariable UUID id) {
        log.info("Received request to fetch product with id: {}", id);
        ProductResponseDTO product = productService.getProductById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Product fetched successfully", product)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponseDTO>>> getAllProducts(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        log.info("Received request to fetch all products");
        Page<ProductResponseDTO> products = productService.getAllProducts(pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Products fetched successfully", products)
        );
    }

    @GetMapping(ApiPaths.PRODUCTS_BY_CATEGORY)
    public ResponseEntity<ApiResponse<Page<ProductResponseDTO>>> getProductsByCategory(
            @PathVariable Category category,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Received request to fetch products by category: {}", category);
        Page<ProductResponseDTO> products = productService.getProductsByCategory(category, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Products fetched successfully", products)
        );
    }

    @GetMapping(ApiPaths.PRODUCTS_BY_BRAND)
    public ResponseEntity<ApiResponse<Page<ProductResponseDTO>>> getProductsByBrand(
            @PathVariable String brand,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Received request to fetch products by brand: {}", brand);
        Page<ProductResponseDTO> products = productService.getProductsByBrand(brand, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Products fetched successfully", products)
        );
    }

    @GetMapping(ApiPaths.PRODUCTS_SEARCH)
    public ResponseEntity<ApiResponse<Page<ProductResponseDTO>>> searchProductsByName(
            @RequestParam String name,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Received request to search products by name: {}", name);
        Page<ProductResponseDTO> products = productService.searchProductsByName(name, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Products fetched successfully", products)
        );
    }

    @PutMapping(ApiPaths.PRODUCT_BY_ID)
    public ResponseEntity<ApiResponse<ProductResponseDTO>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequestDTO request) {

        log.info("Received request to update product with id: {}", id);
        ProductResponseDTO updatedProduct = productService.updateProduct(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Product updated successfully", updatedProduct)
        );
    }

    @DeleteMapping(ApiPaths.PRODUCT_BY_ID)
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        log.info("Received request to delete product with id: {}", id);
        productService.deleteProduct(id);

        return ResponseEntity.ok(
                ApiResponse.success("Product deleted successfully")
        );
    }
}
