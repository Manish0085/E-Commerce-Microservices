package com.sparepartshop.billing_service.repository;

import com.sparepartshop.billing_service.entity.Invoice;
import com.sparepartshop.billing_service.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    boolean existsByInvoiceNumber(String invoiceNumber);

    Page<Invoice> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Invoice> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);

    Page<Invoice> findByCustomerIdAndPaymentStatus(UUID customerId,
                                                    PaymentStatus paymentStatus,
                                                    Pageable pageable);

    List<Invoice> findByCustomerIdAndPaymentStatusIn(UUID customerId,
                                                      List<PaymentStatus> statuses);

    long countByInvoiceNumberStartingWith(String prefix);
}