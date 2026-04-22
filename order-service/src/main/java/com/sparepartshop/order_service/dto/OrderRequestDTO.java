package com.sparepartshop.order_service.dto;

import com.sparepartshop.order_service.enums.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDTO {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemRequestDTO> orderItems;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
