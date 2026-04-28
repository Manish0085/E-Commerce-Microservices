package com.sparepartshop.payment_service.service.impl;

import com.sparepartshop.payment_service.client.BillingServiceClient;
import com.sparepartshop.payment_service.dto.PaymentInitiateRequestDTO;
import com.sparepartshop.payment_service.dto.PaymentResponseDTO;
import com.sparepartshop.payment_service.dto.RefundRequestDTO;
import com.sparepartshop.payment_service.dto.WebhookRequestDTO;
import com.sparepartshop.payment_service.dto.client.ApiResponseWrapper;
import com.sparepartshop.payment_service.dto.client.InvoiceClientDTO;
import com.sparepartshop.payment_service.entity.Payment;
import com.sparepartshop.payment_service.enums.PaymentStatus;
import com.sparepartshop.payment_service.event.PaymentCompletedEvent;
import com.sparepartshop.payment_service.event.PaymentFailedEvent;
import com.sparepartshop.payment_service.event.PaymentRefundedEvent;
import com.sparepartshop.payment_service.exception.BadRequestException;
import com.sparepartshop.payment_service.exception.PaymentGatewayException;
import com.sparepartshop.payment_service.exception.ResourceNotFoundException;
import com.sparepartshop.payment_service.exception.ServiceUnavailableException;
import com.sparepartshop.payment_service.gateway.PaymentGateway;
import com.sparepartshop.payment_service.gateway.dto.GatewayChargeRequest;
import com.sparepartshop.payment_service.gateway.dto.GatewayChargeResponse;
import com.sparepartshop.payment_service.gateway.dto.GatewayRefundResponse;
import com.sparepartshop.payment_service.mapper.PaymentMapper;
import com.sparepartshop.payment_service.repository.PaymentRepository;
import com.sparepartshop.payment_service.service.PaymentService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentGateway paymentGateway;
    private final BillingServiceClient billingServiceClient;

    @Override
    public PaymentResponseDTO initiatePayment(PaymentInitiateRequestDTO request) {
        log.info("Initiating payment for invoiceId: {}, amount: {}",
                request.getInvoiceId(), request.getAmount());

        if (StringUtils.hasText(request.getIdempotencyKey())) {
            var existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Returning existing payment for idempotency key: {}",
                        request.getIdempotencyKey());
                return paymentMapper.toResponseDTO(existing.get());
            }
        }

        InvoiceClientDTO invoice = fetchInvoice(request.getInvoiceId());
        validateInvoiceForPayment(invoice, request.getAmount());

        Payment payment = paymentMapper.toEntity(request);
        payment.setPaymentReference(generatePaymentReference());
        payment.setCurrency("INR");
        payment.setGatewayProvider(paymentGateway.getProvider());
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setInitiatedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.saveAndFlush(payment);
        log.info("Payment created: {} with status INITIATED", savedPayment.getPaymentReference());

        try {
            GatewayChargeRequest gatewayRequest = GatewayChargeRequest.builder()
                    .paymentReference(savedPayment.getPaymentReference())
                    .amount(savedPayment.getAmount())
                    .currency(savedPayment.getCurrency())
                    .paymentMethod(savedPayment.getPaymentMethod())
                    .customerReference(savedPayment.getCustomerId().toString())
                    .description("Payment for invoice " + savedPayment.getInvoiceNumber())
                    .build();

            GatewayChargeResponse gatewayResponse = paymentGateway.charge(gatewayRequest);

            savedPayment.setGatewayTransactionId(gatewayResponse.getGatewayTransactionId());
            savedPayment.setStatus(gatewayResponse.getStatus());
            savedPayment.setMetadata(gatewayResponse.getGatewayMessage());

            Payment updated = paymentRepository.saveAndFlush(savedPayment);
            log.info("Payment {} sent to gateway. Transaction: {}, Status: {}",
                    updated.getPaymentReference(),
                    updated.getGatewayTransactionId(),
                    updated.getStatus());

            return paymentMapper.toResponseDTO(updated);

        } catch (Exception ex) {
            log.error("Gateway call failed for payment: {}", savedPayment.getPaymentReference(), ex);
            savedPayment.setStatus(PaymentStatus.FAILED);
            savedPayment.setFailureReason("Gateway error: " + ex.getMessage());
            savedPayment.setCompletedAt(LocalDateTime.now());
            paymentRepository.saveAndFlush(savedPayment);

            throw new PaymentGatewayException(
                    "Payment gateway is currently unavailable. Please try again."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentById(UUID id) {
        log.debug("Fetching payment with id: {}", id);

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with id: " + id
                ));

        return paymentMapper.toResponseDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByReference(String paymentReference) {
        log.debug("Fetching payment with reference: {}", paymentReference);

        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with reference: " + paymentReference
                ));

        return paymentMapper.toResponseDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentsByInvoiceId(UUID invoiceId) {
        log.debug("Fetching payments for invoiceId: {}", invoiceId);

        return paymentRepository.findByInvoiceId(invoiceId).stream()
                .map(paymentMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDTO> getPaymentsByCustomerId(UUID customerId, Pageable pageable) {
        log.debug("Fetching payments for customerId: {}", customerId);

        return paymentRepository.findByCustomerId(customerId, pageable)
                .map(paymentMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDTO> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        log.debug("Fetching payments by status: {}", status);

        return paymentRepository.findByStatus(status, pageable)
                .map(paymentMapper::toResponseDTO);
    }

    @Override
    public PaymentResponseDTO processWebhook(WebhookRequestDTO webhook) {
        log.info("Processing webhook for payment: {}, transactionId: {}, status: {}",
                webhook.getPaymentReference(),
                webhook.getGatewayTransactionId(),
                webhook.getStatus());

        Payment payment = paymentRepository.findByPaymentReference(webhook.getPaymentReference())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with reference: " + webhook.getPaymentReference()
                ));

        if (payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            log.warn("Webhook received for already completed payment: {}. Ignoring.",
                    payment.getPaymentReference());
            return paymentMapper.toResponseDTO(payment);
        }

        validateWebhookTransactionId(payment, webhook.getGatewayTransactionId());

        payment.setStatus(webhook.getStatus());
        payment.setWebhookPayload(webhook.getRawPayload());
        payment.setCompletedAt(LocalDateTime.now());

        if (webhook.getStatus() == PaymentStatus.FAILED) {
            payment.setFailureReason(webhook.getFailureReason());
        }

        Payment updated = paymentRepository.saveAndFlush(payment);
        log.info("Payment {} updated to status: {}",
                updated.getPaymentReference(), updated.getStatus());

        if (updated.getStatus() == PaymentStatus.SUCCESS) {
            notifyBillingService(updated);

            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .paymentId(updated.getId())
                    .paymentReference(updated.getPaymentReference())
                    .invoiceId(updated.getInvoiceId())
                    .invoiceNumber(updated.getInvoiceNumber())
                    .customerId(updated.getCustomerId())
                    .amount(updated.getAmount())
                    .paymentMethod(updated.getPaymentMethod().name())
                    .paidAt(updated.getCompletedAt())
                    .build();

            kafkaTemplate.send("payment-completed", updated.getCustomerId().toString(), event);
            log.info("Published PaymentCompletedEvent for {}", updated.getPaymentReference());
        } else if (updated.getStatus() == PaymentStatus.FAILED) {

            PaymentFailedEvent event = PaymentFailedEvent.builder()
                    .paymentId(updated.getId())
                    .paymentReference(updated.getPaymentReference())
                    .invoiceId(updated.getInvoiceId())
                    .invoiceNumber(updated.getInvoiceNumber())
                    .customerId(updated.getCustomerId())
                    .amount(updated.getAmount())
                    .paymentMethod(updated.getPaymentMethod().name())
                    .failureReason(updated.getFailureReason())
                    .failedAt(updated.getCompletedAt())
                    .build();

            kafkaTemplate.send("payment-failed", updated.getCustomerId().toString(), event);
            log.info("Published PaymentFailedEvent for {}", updated.getPaymentReference());
        }

        return paymentMapper.toResponseDTO(updated);
    }

    @Override
    public PaymentResponseDTO refundPayment(UUID id, RefundRequestDTO request) {
        log.info("Processing refund for paymentId: {}, amount: {}",
                id, request.getAmount());

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with id: " + id
                ));

        validatePaymentForRefund(payment, request.getAmount());

        try {
            GatewayRefundResponse refundResponse = paymentGateway.refund(
                    payment.getGatewayTransactionId(),
                    request.getAmount()
            );

            if (refundResponse.getStatus() != PaymentStatus.REFUNDED) {
                log.error("Gateway refund failed: {}", refundResponse.getGatewayMessage());
                throw new PaymentGatewayException(
                        "Refund failed at gateway: " + refundResponse.getGatewayMessage()
                );
            }

            BigDecimal totalRefunded = payment.getRefundAmount() != null
                    ? payment.getRefundAmount().add(request.getAmount())
                    : request.getAmount();

            payment.setRefundAmount(totalRefunded);
            payment.setRefundedAt(LocalDateTime.now());

            if (totalRefunded.compareTo(payment.getAmount()) == 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIAL_REFUND);
            }

            Payment updated = paymentRepository.saveAndFlush(payment);
            PaymentRefundedEvent event = PaymentRefundedEvent.builder()
                    .paymentId(updated.getId())
                    .paymentReference(updated.getPaymentReference())
                    .invoiceId(updated.getInvoiceId())
                    .customerId(updated.getCustomerId())
                    .refundAmount(request.getAmount())
                    .originalAmount(updated.getAmount())
                    .refundedAt(updated.getRefundedAt())
                    .build();

            kafkaTemplate.send("payment-refunded", updated.getCustomerId().toString(), event);
            log.info("Published PaymentRefundedEvent for {}", updated.getPaymentReference());

            log.info("Refund processed. Payment {}: {} refunded",
                    updated.getPaymentReference(), totalRefunded);

            return paymentMapper.toResponseDTO(updated);

        } catch (PaymentGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during refund", ex);
            throw new PaymentGatewayException("Refund processing failed: " + ex.getMessage());
        }
    }

    private InvoiceClientDTO fetchInvoice(UUID invoiceId) {
        try {
            ApiResponseWrapper<InvoiceClientDTO> response =
                    billingServiceClient.getInvoiceById(invoiceId);
            return response.getData();
        } catch (FeignException.NotFound ex) {
            throw new BadRequestException("Invoice not found with id: " + invoiceId);
        } catch (FeignException ex) {
            log.error("Billing Service call failed", ex);
            throw new ServiceUnavailableException(
                    "Billing Service is currently unavailable"
            );
        }
    }

    private void validateInvoiceForPayment(InvoiceClientDTO invoice, BigDecimal paymentAmount) {
        if (invoice == null) {
            throw new BadRequestException("Invoice data could not be retrieved");
        }

        if (!Boolean.TRUE.equals(invoice.getActive())) {
            throw new BadRequestException(
                    "Invoice is not active: " + invoice.getInvoiceNumber()
            );
        }

        if ("PAID".equals(invoice.getPaymentStatus())) {
            throw new BadRequestException(
                    "Invoice is already paid: " + invoice.getInvoiceNumber()
            );
        }

        if ("VOIDED".equals(invoice.getPaymentStatus())) {
            throw new BadRequestException(
                    "Cannot pay a voided invoice: " + invoice.getInvoiceNumber()
            );
        }

        if (paymentAmount.compareTo(invoice.getDueAmount()) > 0) {
            throw new BadRequestException(
                    "Payment amount " + paymentAmount
                            + " exceeds due amount " + invoice.getDueAmount()
            );
        }
    }

    private void validateWebhookTransactionId(Payment payment, String webhookTxnId) {
        if (payment.getGatewayTransactionId() != null
                && !payment.getGatewayTransactionId().equals(webhookTxnId)) {
            log.error("Webhook transaction ID mismatch. Expected: {}, Got: {}",
                    payment.getGatewayTransactionId(), webhookTxnId);
            throw new BadRequestException(
                    "Webhook transaction ID does not match payment"
            );
        }
    }

    private void validatePaymentForRefund(Payment payment, BigDecimal refundAmount) {
        if (payment.getStatus() != PaymentStatus.SUCCESS
                && payment.getStatus() != PaymentStatus.PARTIAL_REFUND) {
            throw new BadRequestException(
                    "Cannot refund payment in status: " + payment.getStatus()
            );
        }

        if (payment.getGatewayTransactionId() == null) {
            throw new BadRequestException(
                    "Payment has no gateway transaction ID. Cannot refund."
            );
        }

        BigDecimal alreadyRefunded = payment.getRefundAmount() != null
                ? payment.getRefundAmount()
                : BigDecimal.ZERO;

        BigDecimal totalRefundable = payment.getAmount().subtract(alreadyRefunded);

        if (refundAmount.compareTo(totalRefundable) > 0) {
            throw new BadRequestException(
                    "Refund amount " + refundAmount
                            + " exceeds refundable amount " + totalRefundable
            );
        }
    }

    private void notifyBillingService(Payment payment) {
        try {
            Map<String, Object> paymentNotification = new HashMap<>();
            paymentNotification.put("amount", payment.getAmount());
            paymentNotification.put("paymentMode", mapPaymentMethodToBillingMode(payment));
            paymentNotification.put("notes", "Paid via " + payment.getGatewayProvider()
                    + " - Reference: " + payment.getPaymentReference());

            billingServiceClient.recordPayment(payment.getInvoiceId(), paymentNotification);
            log.info("Billing Service notified for payment: {}", payment.getPaymentReference());
        } catch (Exception ex) {
            log.error("Failed to notify Billing Service for payment: {}. Manual reconciliation needed.",
                    payment.getPaymentReference(), ex);
        }
    }

    private String mapPaymentMethodToBillingMode(Payment payment) {
        return switch (payment.getPaymentMethod()) {
            case CARD, NETBANKING -> "CREDIT";
            case UPI -> "UPI";
            case WALLET -> "CASH";
        };
    }

    private String generatePaymentReference() {
        String year = String.valueOf(Year.now().getValue());
        String prefix = "PAY-" + year + "-";
        long count = paymentRepository.countByPaymentReferenceStartingWith(prefix);
        return prefix + String.format("%05d", count + 1);
    }
}
