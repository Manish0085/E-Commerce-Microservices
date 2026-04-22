package com.sparepartshop.order_service.dto.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryClientDTO {

    private UUID id;
    private UUID productId;
    private Integer stockQuantity;
    private Boolean lowStock;
}