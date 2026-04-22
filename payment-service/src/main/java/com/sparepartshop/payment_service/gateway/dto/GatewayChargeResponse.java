package com.sparepartshop.payment_service.gateway.dto;

import com.sparepartshop.payment_service.enums.PaymentStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayChargeResponse {

    private String gatewayTransactionId;
    private PaymentStatus status;
    private String gatewayResponseCode;
    private String gatewayMessage;
    private String paymentUrl;
}
