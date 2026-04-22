package com.sparepartshop.payment_service.service;

import com.sparepartshop.payment_service.dto.PaymentInitiateRequestDTO;
import com.sparepartshop.payment_service.dto.PaymentResponseDTO;
import com.sparepartshop.payment_service.dto.RefundRequestDTO;
import com.sparepartshop.payment_service.dto.WebhookRequestDTO;
import com.sparepartshop.payment_service.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponseDTO initiatePayment(PaymentInitiateRequestDTO request);

    PaymentResponseDTO getPaymentById(UUID id);

    PaymentResponseDTO getPaymentByReference(String paymentReference);

    List<PaymentResponseDTO> getPaymentsByInvoiceId(UUID invoiceId);

    Page<PaymentResponseDTO> getPaymentsByCustomerId(UUID customerId, Pageable pageable);

    Page<PaymentResponseDTO> getPaymentsByStatus(PaymentStatus status, Pageable pageable);

    PaymentResponseDTO processWebhook(WebhookRequestDTO webhook);

    PaymentResponseDTO refundPayment(UUID id, RefundRequestDTO request);
}
