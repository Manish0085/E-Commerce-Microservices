package com.sparepartshop.order_service.dto;

import com.sparepartshop.order_service.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private OrderStatus status;
}
