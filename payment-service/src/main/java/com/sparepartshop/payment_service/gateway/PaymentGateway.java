package com.sparepartshop.payment_service.gateway;

import com.sparepartshop.payment_service.enums.GatewayProvider;
import com.sparepartshop.payment_service.gateway.dto.GatewayChargeRequest;
import com.sparepartshop.payment_service.gateway.dto.GatewayChargeResponse;
import com.sparepartshop.payment_service.gateway.dto.GatewayRefundResponse;

import java.math.BigDecimal;

public interface PaymentGateway {

    GatewayChargeResponse charge(GatewayChargeRequest request);

    GatewayRefundResponse refund(String gatewayTransactionId, BigDecimal amount);

    GatewayProvider getProvider();
}
