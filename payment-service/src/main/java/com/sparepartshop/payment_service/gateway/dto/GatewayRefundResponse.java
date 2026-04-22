package com.sparepartshop.payment_service.gateway.dto;

import com.sparepartshop.payment_service.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayRefundResponse {

    private String refundReference;
    private BigDecimal refundAmount;
    private PaymentStatus status;
    private String gatewayMessage;
}
