package com.sparepartshop.payment_service.dto.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceClientDTO {

    private UUID id;
    private String invoiceNumber;
    private UUID orderId;
    private UUID customerId;
    private String customerName;
    private BigDecimal grandTotal;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;
    private String paymentStatus;
    private Boolean active;
}
