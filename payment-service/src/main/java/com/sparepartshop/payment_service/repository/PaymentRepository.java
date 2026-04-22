package com.sparepartshop.payment_service.repository;

import com.sparepartshop.payment_service.entity.Payment;
import com.sparepartshop.payment_service.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    boolean existsByPaymentReference(String paymentReference);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<Payment> findByInvoiceId(UUID invoiceId);

    Page<Payment> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    Page<Payment> findByCustomerIdAndStatus(UUID customerId,
                                             PaymentStatus status,
                                             Pageable pageable);

    List<Payment> findByInvoiceIdAndStatus(UUID invoiceId, PaymentStatus status);

    long countByPaymentReferenceStartingWith(String prefix);
}
