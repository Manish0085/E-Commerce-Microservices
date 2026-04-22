package com.sparepartshop.order_service.dto.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductClientDTO {

    private UUID id;
    private String name;
    private String brand;
    private String partNumber;
    private BigDecimal price;
    private Boolean active;
}