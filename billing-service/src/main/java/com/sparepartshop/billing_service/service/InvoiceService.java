package com.sparepartshop.billing_service.service;

import com.sparepartshop.billing_service.dto.InvoiceRequestDTO;
import com.sparepartshop.billing_service.dto.InvoiceResponseDTO;
import com.sparepartshop.billing_service.dto.PaymentRequestDTO;
import com.sparepartshop.billing_service.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InvoiceService {

    InvoiceResponseDTO createInvoice(InvoiceRequestDTO request);

    InvoiceResponseDTO getInvoiceById(UUID id);

    InvoiceResponseDTO getInvoiceByNumber(String invoiceNumber);

    InvoiceResponseDTO getInvoiceByOrderId(UUID orderId);

    Page<InvoiceResponseDTO> getAllInvoices(Pageable pageable);

    Page<InvoiceResponseDTO> getInvoicesByCustomerId(UUID customerId, Pageable pageable);

    Page<InvoiceResponseDTO> getInvoicesByStatus(PaymentStatus status, Pageable pageable);

    InvoiceResponseDTO recordPayment(UUID id, PaymentRequestDTO paymentRequest);

    void voidInvoice(UUID id);

    List<InvoiceResponseDTO> getOutstandingInvoices(UUID customerId);

    BigDecimal getTotalOutstandingAmount(UUID customerId);
}
