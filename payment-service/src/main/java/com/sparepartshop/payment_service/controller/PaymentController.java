package com.sparepartshop.payment_service.controller;

import com.sparepartshop.payment_service.constants.ApiPaths;
import com.sparepartshop.payment_service.dto.ApiResponse;
import com.sparepartshop.payment_service.dto.PaymentInitiateRequestDTO;
import com.sparepartshop.payment_service.dto.PaymentResponseDTO;
import com.sparepartshop.payment_service.dto.RefundRequestDTO;
import com.sparepartshop.payment_service.dto.WebhookRequestDTO;
import com.sparepartshop.payment_service.enums.PaymentStatus;
import com.sparepartshop.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.PAYMENTS)
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping(ApiPaths.INITIATE)
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequestDTO request) {

        log.info("Received payment initiation request for invoiceId: {}", request.getInvoiceId());
        PaymentResponseDTO payment = paymentService.initiatePayment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment initiated successfully", payment));
    }

    @GetMapping(ApiPaths.PAYMENT_BY_ID)
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> getPaymentById(@PathVariable UUID id) {
        log.info("Fetching payment with id: {}", id);
        PaymentResponseDTO payment = paymentService.getPaymentById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Payment fetched successfully", payment)
        );
    }

    @GetMapping(ApiPaths.PAYMENT_BY_REFERENCE)
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> getPaymentByReference(
            @PathVariable String paymentReference) {

        log.info("Fetching payment with reference: {}", paymentReference);
        PaymentResponseDTO payment = paymentService.getPaymentByReference(paymentReference);

        return ResponseEntity.ok(
                ApiResponse.success("Payment fetched successfully", payment)
        );
    }

    @GetMapping(ApiPaths.BY_INVOICE)
    public ResponseEntity<ApiResponse<List<PaymentResponseDTO>>> getPaymentsByInvoiceId(
            @PathVariable UUID invoiceId) {

        log.info("Fetching payments for invoiceId: {}", invoiceId);
        List<PaymentResponseDTO> payments = paymentService.getPaymentsByInvoiceId(invoiceId);

        return ResponseEntity.ok(
                ApiResponse.success("Payments fetched successfully", payments)
        );
    }

    @GetMapping(ApiPaths.BY_CUSTOMER)
    public ResponseEntity<ApiResponse<Page<PaymentResponseDTO>>> getPaymentsByCustomerId(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        log.info("Fetching payments for customerId: {}", customerId);
        Page<PaymentResponseDTO> payments = paymentService.getPaymentsByCustomerId(customerId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Payments fetched successfully", payments)
        );
    }

    @GetMapping(ApiPaths.BY_STATUS)
    public ResponseEntity<ApiResponse<Page<PaymentResponseDTO>>> getPaymentsByStatus(
            @PathVariable PaymentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Fetching payments by status: {}", status);
        Page<PaymentResponseDTO> payments = paymentService.getPaymentsByStatus(status, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Payments fetched successfully", payments)
        );
    }

    @PostMapping(ApiPaths.WEBHOOK)
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> handleWebhook(
            @Valid @RequestBody WebhookRequestDTO webhook) {

        log.info("Received webhook for payment: {}, status: {}",
                webhook.getPaymentReference(), webhook.getStatus());
        PaymentResponseDTO payment = paymentService.processWebhook(webhook);

        return ResponseEntity.ok(
                ApiResponse.success("Webhook processed successfully", payment)
        );
    }

    @PostMapping(ApiPaths.REFUND)
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> refundPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RefundRequestDTO request) {

        log.info("Processing refund for paymentId: {}, amount: {}",
                id, request.getAmount());
        PaymentResponseDTO payment = paymentService.refundPayment(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Refund processed successfully", payment)
        );
    }
}
