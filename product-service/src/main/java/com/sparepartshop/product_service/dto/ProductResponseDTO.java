package com.sparepartshop.product_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sparepartshop.product_service.enums.Category;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO {

    private UUID id;
    private String name;
    private String description;
    private String brand;
    private Category category;
    private String partNumber;
    private BigDecimal price;
    private String vehicleCompatibility;
    private Boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}