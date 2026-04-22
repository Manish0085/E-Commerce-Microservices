package com.sparepartshop.billing_service.dto;

import com.sparepartshop.billing_service.enums.PaymentMode;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequestDTO {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotBlank(message = "Order number is required")
    @Size(max = 30, message = "Order number cannot exceed 30 characters")
    private String orderNumber;

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Subtotal is required")
    @DecimalMin(value = "0.01", message = "Subtotal must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Subtotal format is invalid")
    private BigDecimal subtotal;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
