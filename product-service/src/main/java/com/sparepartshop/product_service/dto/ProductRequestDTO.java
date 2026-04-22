package com.sparepartshop.product_service.dto;

import com.sparepartshop.product_service.enums.Category;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDTO {

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name cannot exceed 150 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "Brand is required")
    @Size(max = 100, message = "Brand cannot exceed 100 characters")
    private String brand;

    @NotNull(message = "Category is required")
    private Category category;

    @NotBlank(message = "Part number is required")
    @Size(min = 3, max = 100, message = "Part number must be between 3 and 100 characters")
    private String partNumber;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid")
    private BigDecimal price;

    @Size(max = 500, message = "Vehicle compatibility cannot exceed 500 characters")
    private String vehicleCompatibility;
}