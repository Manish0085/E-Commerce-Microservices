package com.sparepartshop.customer_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRegisteredEvent {
    private UUID customerId;
    private String name;
    private String email;
    private String phone;
    private String customerType;
    private LocalDateTime registeredAt;
}