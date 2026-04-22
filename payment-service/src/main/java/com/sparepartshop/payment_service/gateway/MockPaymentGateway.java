package com.sparepartshop.payment_service.gateway;

import com.sparepartshop.payment_service.enums.GatewayProvider;
import com.sparepartshop.payment_service.enums.PaymentStatus;
import com.sparepartshop.payment_service.gateway.dto.GatewayChargeRequest;
import com.sparepartshop.payment_service.gateway.dto.GatewayChargeResponse;
import com.sparepartshop.payment_service.gateway.dto.GatewayRefundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockPaymentGateway implements PaymentGateway {

    private final WebhookSimulator webhookSimulator;
    private final Random random = new Random();

    @Value("${payment.mock.success-rate:90}")
    private int successRate;

    @Value("${payment.mock.webhook-delay-ms:3000}")
    private long webhookDelayMs;

    @Override
    public GatewayChargeResponse charge(GatewayChargeRequest request) {
        log.info("Mock Gateway: Processing charge for payment reference: {}",
                request.getPaymentReference());

        String gatewayTransactionId = "MOCK_TXN_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        boolean willSucceed = random.nextInt(100) < successRate;

        log.info("Mock Gateway: Transaction {} will {} (success rate: {}%)",
                gatewayTransactionId,
                willSucceed ? "SUCCEED" : "FAIL",
                successRate);

        webhookSimulator.scheduleWebhook(
                request.getPaymentReference(),
                gatewayTransactionId,
                willSucceed,
                webhookDelayMs
        );

        return GatewayChargeResponse.builder()
                .gatewayTransactionId(gatewayTransactionId)
                .status(PaymentStatus.PROCESSING)
                .gatewayResponseCode("ACCEPTED")
                .gatewayMessage("Payment is being processed. Webhook will confirm status.")
                .paymentUrl("https://mock-gateway.example.com/pay/" + gatewayTransactionId)
                .build();
    }

    @Override
    public GatewayRefundResponse refund(String gatewayTransactionId, BigDecimal amount) {
        log.info("Mock Gateway: Processing refund for transaction: {}, amount: {}",
                gatewayTransactionId, amount);

        String refundReference = "MOCK_REFUND_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        boolean willSucceed = random.nextInt(100) < successRate;

        if (willSucceed) {
            log.info("Mock Gateway: Refund successful - {}", refundReference);
            return GatewayRefundResponse.builder()
                    .refundReference(refundReference)
                    .refundAmount(amount)
                    .status(PaymentStatus.REFUNDED)
                    .gatewayMessage("Refund processed successfully")
                    .build();
        } else {
            log.warn("Mock Gateway: Refund failed - {}", refundReference);
            return GatewayRefundResponse.builder()
                    .refundReference(refundReference)
                    .refundAmount(amount)
                    .status(PaymentStatus.FAILED)
                    .gatewayMessage("Refund failed: Simulated gateway error")
                    .build();
        }
    }

    @Override
    public GatewayProvider getProvider() {
        return GatewayProvider.MOCK;
    }
}
