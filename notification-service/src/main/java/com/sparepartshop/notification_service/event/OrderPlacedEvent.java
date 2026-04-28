package com.sparepartshop.notification_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {
    private UUID orderId;
    private String orderNumber;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;
    private LocalDateTime placedAt;
}