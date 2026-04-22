package com.sparepartshop.inventory_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryRequestDTO {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "Reorder level is required")
    @Min(value = 1, message = "Reorder level must be at least 1")
    private Integer reorderLevel;

    @NotNull(message = "Reorder quantity is required")
    @Min(value = 1, message = "Reorder quantity must be at least 1")
    private Integer reorderQuantity;

    @Size(max = 100, message = "Warehouse location cannot exceed 100 characters")
    private String warehouseLocation;

    @NotBlank(message = "Unit of measure is required")
    @Size(max = 50, message = "Unit of measure cannot exceed 50 characters")
    private String unitOfMeasure;
}
