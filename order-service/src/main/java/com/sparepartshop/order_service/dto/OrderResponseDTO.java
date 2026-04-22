package com.sparepartshop.order_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sparepartshop.order_service.enums.OrderStatus;
import com.sparepartshop.order_service.enums.PaymentMode;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {

    private UUID id;

    private String orderNumber;

    private UUID customerId;

    private List<OrderItemResponseDTO> orderItems;

    private BigDecimal totalAmount;

    private OrderStatus status;

    private PaymentMode paymentMode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderDate;

    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
