package com.sparepartshop.notification_service.listener;


import com.sparepartshop.notification_service.event.OrderCancelledEvent;
import com.sparepartshop.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCancelledListener {

    private final EmailService emailService;

    @KafkaListener(topics = "order-cancelled")
    public void handle(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for order {}", event.getOrderNumber());

        try {
            if (event.getCustomerEmail() == null || event.getCustomerEmail().isBlank()) {
                log.warn("No email on event — skipping cancellation email for order {}",
                        event.getOrderNumber());
                return;
            }
            emailService.sendOrderCancellation(
                    event.getCustomerEmail(),
                    event.getCustomerName(),
                    event.getOrderNumber(),
                    event.getReason()
            );

        } catch (Exception ex) {
            log.error("Failed to handle OrderCancelledEvent for {}: {}",
                    event.getOrderNumber(), ex.getMessage(), ex);
        }
    }
}
