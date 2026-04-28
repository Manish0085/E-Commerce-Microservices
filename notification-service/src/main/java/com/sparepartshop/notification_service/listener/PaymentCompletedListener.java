package com.sparepartshop.notification_service.listener;

import com.sparepartshop.notification_service.client.CustomerServiceClient;
import com.sparepartshop.notification_service.dto.ApiResponseWrapper;
import com.sparepartshop.notification_service.dto.CustomerClientDTO;
import com.sparepartshop.notification_service.event.PaymentCompletedEvent;
import com.sparepartshop.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedListener {

    private final EmailService emailService;
    private final CustomerServiceClient customerServiceClient;

    @KafkaListener(topics = "payment-completed")
    public void handle(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent for invoice {} (payment {})",
                event.getInvoiceNumber(), event.getPaymentReference());

        try {

            ApiResponseWrapper<CustomerClientDTO> response =
                    customerServiceClient.getCustomerById(event.getCustomerId());

            if (response == null || response.getData() == null) {
                log.warn("No customer found for id {} — skipping email", event.getCustomerId());
                return;
            }

            CustomerClientDTO customer = response.getData();

            if (customer.getEmail() == null || customer.getEmail().isBlank()) {
                log.warn("Customer {} has no email — skipping payment confirmation", customer.getId());
                return;
            }

            emailService.sendPaymentConfirmation(
                    customer.getEmail(),
                    customer.getName(),
                    event.getInvoiceNumber(),
                    event.getAmount(),
                    event.getPaymentReference()
            );
        } catch (Exception ex) {
            log.error("Failed to handle PaymentCompletedEvent for {}: {}",
                    event.getPaymentReference(), ex.getMessage(), ex);
        }
    }
}
