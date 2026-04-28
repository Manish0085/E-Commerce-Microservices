package com.sparepartshop.notification_service.listener;


import com.sparepartshop.notification_service.client.CustomerServiceClient;
import com.sparepartshop.notification_service.event.OrderPlacedEvent;
import com.sparepartshop.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderPlacedListener {

    private final EmailService emailService;

    @KafkaListener(topics = "order-placed")
    public void handle(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent for order {}", event.getOrderNumber());

        try {
            if (event.getCustomerEmail() == null || event.getCustomerName().isBlank()) {
                log.warn("Skipping email — no customer email on event for order {}", event.getOrderNumber());
                return;
            }

            emailService.sendOrderConfirmation(
                    event.getCustomerEmail(),
                    event.getCustomerName(),
                    event.getOrderNumber(),
                    event.getTotalAmount()
            );
        } catch (Exception ex) {
            log.error("Failed to handle OrderCompleteEvent for {}: {}",
                    event.getOrderNumber(), ex.getMessage(), ex);
        }

    }
}
