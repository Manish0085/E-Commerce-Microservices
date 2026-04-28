package com.sparepartshop.payment_service.event;

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
public class PaymentRefundedEvent {
    private UUID paymentId;
    private String paymentReference;
    private UUID invoiceId;
    private UUID customerId;
    private BigDecimal refundAmount;
    private BigDecimal originalAmount;
    private LocalDateTime refundedAt;
}