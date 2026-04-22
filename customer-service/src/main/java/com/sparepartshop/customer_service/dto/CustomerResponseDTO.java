package com.sparepartshop.customer_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sparepartshop.customer_service.enums.CustomerType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponseDTO {

    private UUID id;

    private String name;

    private String email;

    private String phone;

    private String address;

    private String city;

    private CustomerType customerType;

    private String businessName;

    private String gstNumber;

    private BigDecimal creditLimit;

    private BigDecimal currentBalance;

    private Boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
