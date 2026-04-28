package com.sparepartshop.billing_service.listener;

import com.sparepartshop.billing_service.dto.PaymentRequestDTO;
import com.sparepartshop.billing_service.enums.PaymentMode;
import com.sparepartshop.billing_service.event.PaymentCompletedEvent;
import com.sparepartshop.billing_service.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentCompletedListener {

    private final InvoiceService invoiceService;

    @KafkaListener(topics = "payment-completed")
    public void handle(PaymentCompletedEvent event) {

        try {
            PaymentRequestDTO paymentRequest = PaymentRequestDTO.builder()
                    .amount(event.getAmount())
                    .paymentMode(mapPaymentMode(event.getPaymentMethod()))
                    .notes("Auto-recorded from payment " + event.getPaymentReference())
                    .build();
            log.info("Received PaymentCompletedEvent for invoice {} (payment {})",
                    event.getInvoiceNumber(), paymentRequest);
        } catch (Exception ex) {
            log.error("Failed to mark invoice {} as paid: {}",
                    event.getInvoiceNumber(), ex.getMessage(), ex);
        }
    }

    private PaymentMode mapPaymentMode(String paymentMethod) {
        if (paymentMethod == null) return PaymentMode.UPI;
        return switch (paymentMethod.toUpperCase()) {
            case "UPI" -> PaymentMode.UPI;
            case "CARD", "NETBANKING", "WALLET" -> PaymentMode.UPI;
            default -> PaymentMode.UPI;
        };
    }
}
