package com.sparepartshop.product_service.repository;

import com.sparepartshop.product_service.enums.Category;
import com.sparepartshop.product_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    // Find by part number (unique field)
    Optional<Product> findByPartNumber(String partNumber);

    // Check if part number exists (faster than findBy + isPresent)
    boolean existsByPartNumber(String partNumber);

    // Find all by category (with pagination)
    Page<Product> findByCategory(Category category, Pageable pageable);

    // Find all by brand (case-insensitive)
    Page<Product> findByBrandIgnoreCase(String brand, Pageable pageable);

    // Find only active products
    Page<Product> findByActiveTrue(Pageable pageable);

    // Search by name (partial match, case-insensitive)
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}