package com.sparepartshop.payment_service.gateway;

import com.sparepartshop.payment_service.dto.WebhookRequestDTO;
import com.sparepartshop.payment_service.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookSimulator {

    private final RestTemplate restTemplate;

    @Value("${server.port:8086}")
    private int serverPort;

    @Async("webhookExecutor")
    public CompletableFuture<Void> scheduleWebhook(String paymentReference,
                                                     String gatewayTransactionId,
                                                     boolean success,
                                                     long delayMs) {
        try {
            log.info("Webhook Simulator: Scheduled webhook for {} in {}ms",
                    paymentReference, delayMs);

            Thread.sleep(delayMs);

            WebhookRequestDTO webhook = WebhookRequestDTO.builder()
                    .paymentReference(paymentReference)
                    .gatewayTransactionId(gatewayTransactionId)
                    .status(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                    .failureReason(success ? null : "Simulated gateway failure")
                    .signature(generateMockSignature(paymentReference, gatewayTransactionId))
                    .rawPayload(buildRawPayload(paymentReference, gatewayTransactionId, success))
                    .build();

            String webhookUrl = "http://localhost:" + serverPort + "/api/v1/payments/webhook";

            log.info("Webhook Simulator: Sending webhook to {} for {}",
                    webhookUrl, paymentReference);

            restTemplate.postForEntity(webhookUrl, webhook, Void.class);

            log.info("Webhook Simulator: Successfully sent webhook for {}",
                    paymentReference);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Webhook simulator interrupted for {}", paymentReference, ex);
        } catch (Exception ex) {
            log.error("Failed to send webhook for {}: {}",
                    paymentReference, ex.getMessage(), ex);
        }

        return CompletableFuture.completedFuture(null);
    }

    private String generateMockSignature(String paymentReference, String gatewayTransactionId) {
        return "MOCK_SIG_" + UUID.nameUUIDFromBytes(
                (paymentReference + gatewayTransactionId).getBytes()
        ).toString();
    }

    private String buildRawPayload(String paymentReference, String gatewayTransactionId, boolean success) {
        return String.format(
                "{\"paymentReference\":\"%s\",\"transactionId\":\"%s\",\"status\":\"%s\"}",
                paymentReference,
                gatewayTransactionId,
                success ? "SUCCESS" : "FAILED"
        );
    }
}
