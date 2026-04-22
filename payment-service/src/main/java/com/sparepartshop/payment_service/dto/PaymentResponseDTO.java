package com.sparepartshop.payment_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sparepartshop.payment_service.enums.GatewayProvider;
import com.sparepartshop.payment_service.enums.PaymentMethod;
import com.sparepartshop.payment_service.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponseDTO {

    private UUID id;
    private String paymentReference;
    private UUID invoiceId;
    private String invoiceNumber;
    private UUID customerId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private GatewayProvider gatewayProvider;
    private String gatewayTransactionId;
    private PaymentStatus status;
    private String failureReason;
    private BigDecimal refundAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime initiatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refundedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
