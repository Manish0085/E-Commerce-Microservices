package com.sparepartshop.notification_service.listener;


import com.sparepartshop.notification_service.event.CustomerRegisteredEvent;
import com.sparepartshop.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomerRegisteredListener {

    private final EmailService emailService;

    @KafkaListener(topics = "customer-registered")
    public void handle(CustomerRegisteredEvent event) {
        log.info("Received CustomerRegisteredEvent for customer {} ({})",
                event.getName(), event.getCustomerId());
        try {
            if (event.getEmail() == null || event.getEmail().isBlank()) {
                log.warn("Customer {} has no email — skipping welcome", event.getCustomerId());
                return;
            }

            emailService.sendWelcomeEmail(event.getEmail(), event.getName());
            ;
        } catch (Exception e) {
            log.info("Failed to handle Customer registered event, {}: {}: {}", event.getName(),
                    event.getEmail(), event.getCustomerType());
        }

    }
}
