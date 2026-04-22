package com.sparepartshop.billing_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sparepartshop.billing_service.enums.PaymentMode;
import com.sparepartshop.billing_service.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponseDTO {

    private UUID id;

    private String invoiceNumber;

    private UUID orderId;

    private String orderNumber;

    private UUID customerId;

    private String customerName;

    private String customerPhone;

    private BigDecimal subtotal;

    private BigDecimal taxAmount;

    private BigDecimal taxPercentage;

    private BigDecimal grandTotal;

    private BigDecimal paidAmount;

    private BigDecimal dueAmount;

    private PaymentStatus paymentStatus;

    private PaymentMode paymentMode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime invoiceDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidAt;

    private String notes;

    private Boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
