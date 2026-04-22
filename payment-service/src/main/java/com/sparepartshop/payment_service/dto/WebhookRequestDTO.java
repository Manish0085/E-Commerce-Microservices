package com.sparepartshop.payment_service.dto;

import com.sparepartshop.payment_service.enums.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookRequestDTO {

    @NotBlank(message = "Payment reference is required")
    private String paymentReference;

    @NotBlank(message = "Gateway transaction ID is required")
    private String gatewayTransactionId;

    @NotNull(message = "Status is required")
    private PaymentStatus status;

    private String failureReason;

    private String signature;

    private String rawPayload;
}
