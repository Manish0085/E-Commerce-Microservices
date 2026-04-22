package com.sparepartshop.billing_service.controller;

import com.sparepartshop.billing_service.constants.ApiPaths;
import com.sparepartshop.billing_service.dto.ApiResponse;
import com.sparepartshop.billing_service.dto.InvoiceRequestDTO;
import com.sparepartshop.billing_service.dto.InvoiceResponseDTO;
import com.sparepartshop.billing_service.dto.PaymentRequestDTO;
import com.sparepartshop.billing_service.enums.PaymentStatus;
import com.sparepartshop.billing_service.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.INVOICES)
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> createInvoice(
            @Valid @RequestBody InvoiceRequestDTO request) {

        log.info("Received request to create invoice for orderId: {}", request.getOrderId());
        InvoiceResponseDTO created = invoiceService.createInvoice(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice created successfully", created));
    }

    @GetMapping(ApiPaths.INVOICE_BY_ID)
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> getInvoiceById(@PathVariable UUID id) {
        log.info("Fetching invoice with id: {}", id);
        InvoiceResponseDTO invoice = invoiceService.getInvoiceById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Invoice fetched successfully", invoice)
        );
    }

    @GetMapping(ApiPaths.INVOICE_BY_NUMBER)
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> getInvoiceByNumber(
            @PathVariable String invoiceNumber) {

        log.info("Fetching invoice with number: {}", invoiceNumber);
        InvoiceResponseDTO invoice = invoiceService.getInvoiceByNumber(invoiceNumber);

        return ResponseEntity.ok(
                ApiResponse.success("Invoice fetched successfully", invoice)
        );
    }

    @GetMapping(ApiPaths.BY_ORDER)
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> getInvoiceByOrderId(
            @PathVariable UUID orderId) {

        log.info("Fetching invoice for orderId: {}", orderId);
        InvoiceResponseDTO invoice = invoiceService.getInvoiceByOrderId(orderId);

        return ResponseEntity.ok(
                ApiResponse.success("Invoice fetched successfully", invoice)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvoiceResponseDTO>>> getAllInvoices(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        log.info("Fetching all invoices");
        Page<InvoiceResponseDTO> invoices = invoiceService.getAllInvoices(pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Invoices fetched successfully", invoices)
        );
    }

    @GetMapping(ApiPaths.BY_CUSTOMER)
    public ResponseEntity<ApiResponse<Page<InvoiceResponseDTO>>> getInvoicesByCustomerId(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Fetching invoices for customerId: {}", customerId);
        Page<InvoiceResponseDTO> invoices = invoiceService.getInvoicesByCustomerId(customerId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Invoices fetched successfully", invoices)
        );
    }

    @GetMapping(ApiPaths.BY_STATUS)
    public ResponseEntity<ApiResponse<Page<InvoiceResponseDTO>>> getInvoicesByStatus(
            @PathVariable PaymentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Fetching invoices by status: {}", status);
        Page<InvoiceResponseDTO> invoices = invoiceService.getInvoicesByStatus(status, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Invoices fetched successfully", invoices)
        );
    }

    @PostMapping(ApiPaths.PAY_INVOICE)
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentRequestDTO paymentRequest) {

        log.info("Recording payment for invoiceId: {}", id);
        InvoiceResponseDTO updated = invoiceService.recordPayment(id, paymentRequest);

        return ResponseEntity.ok(
                ApiResponse.success("Payment recorded successfully", updated)
        );
    }

    @PostMapping(ApiPaths.VOID_INVOICE)
    public ResponseEntity<ApiResponse<Void>> voidInvoice(@PathVariable UUID id) {
        log.info("Voiding invoice with id: {}", id);
        invoiceService.voidInvoice(id);

        return ResponseEntity.ok(
                ApiResponse.success("Invoice voided successfully")
        );
    }

    @GetMapping(ApiPaths.OUTSTANDING)
    public ResponseEntity<ApiResponse<List<InvoiceResponseDTO>>> getOutstandingInvoices(
            @PathVariable UUID customerId) {

        log.info("Fetching outstanding invoices for customerId: {}", customerId);
        List<InvoiceResponseDTO> outstanding = invoiceService.getOutstandingInvoices(customerId);

        return ResponseEntity.ok(
                ApiResponse.success("Outstanding invoices fetched successfully", outstanding)
        );
    }

    @GetMapping("/customer/{customerId}/outstanding-amount")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalOutstandingAmount(
            @PathVariable UUID customerId) {

        log.info("Calculating outstanding amount for customerId: {}", customerId);
        BigDecimal amount = invoiceService.getTotalOutstandingAmount(customerId);

        return ResponseEntity.ok(
                ApiResponse.success("Total outstanding amount calculated", amount)
        );
    }
}
