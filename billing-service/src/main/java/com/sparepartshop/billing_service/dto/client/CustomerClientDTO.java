package com.sparepartshop.billing_service.dto.client;

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
public class CustomerClientDTO {

    private UUID id;
    private String name;
    private String phone;
    private String customerType;
    private Boolean active;
    private BigDecimal creditLimit;
    private BigDecimal currentBalance;
}
