package com.sparepartshop.payment_service.gateway.dto;

import com.sparepartshop.payment_service.enums.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayChargeRequest {

    private String paymentReference;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private String customerReference;
    private String description;
}
