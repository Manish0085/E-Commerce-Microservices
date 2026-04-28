package com.sparepartshop.billing_service.listener;


import com.sparepartshop.billing_service.event.OrderCancelledEvent;
import com.sparepartshop.billing_service.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelledListener {

    private final InvoiceService invoiceService;

    @KafkaListener(topics = "order-cancelled")
    public void handle(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for order {}", event.getOrderNumber());

        try {
            invoiceService.getInvoiceById(event.getOrderId());
            log.info("Invoice for order {} has been voided", event.getOrderNumber());
        } catch (Exception ex) {
            log.error("Failed to void invoice for order {}: {}",
                    event.getOrderNumber(), ex.getMessage(), ex);
        }
    }
}
