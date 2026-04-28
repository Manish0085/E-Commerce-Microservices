package com.sparepartshop.notification_service.event;

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
public class OrderCancelledEvent {
    private UUID orderId;
    private String orderNumber;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String reason;
    private LocalDateTime cancelledAt;
}