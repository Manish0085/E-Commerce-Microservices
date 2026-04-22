package com.sparepartshop.billing_service.service.impl;

import com.sparepartshop.billing_service.client.CustomerServiceClient;
import com.sparepartshop.billing_service.dto.InvoiceRequestDTO;
import com.sparepartshop.billing_service.dto.InvoiceResponseDTO;
import com.sparepartshop.billing_service.dto.PaymentRequestDTO;
import com.sparepartshop.billing_service.dto.client.ApiResponseWrapper;
import com.sparepartshop.billing_service.dto.client.CustomerClientDTO;
import com.sparepartshop.billing_service.entity.Invoice;
import com.sparepartshop.billing_service.enums.PaymentMode;
import com.sparepartshop.billing_service.enums.PaymentStatus;
import com.sparepartshop.billing_service.exception.BadRequestException;
import com.sparepartshop.billing_service.exception.DuplicateResourceException;
import com.sparepartshop.billing_service.exception.ResourceNotFoundException;
import com.sparepartshop.billing_service.exception.ServiceUnavailableException;
import com.sparepartshop.billing_service.mapper.InvoiceMapper;
import com.sparepartshop.billing_service.repository.InvoiceRepository;
import com.sparepartshop.billing_service.service.InvoiceService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private static final BigDecimal TAX_PERCENTAGE = new BigDecimal("18.00");
    private static final int CREDIT_PAYMENT_DAYS = 30;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final CustomerServiceClient customerServiceClient;

    @Override
    public InvoiceResponseDTO createInvoice(InvoiceRequestDTO request) {
        log.info("Creating invoice for orderId: {}", request.getOrderId());

        if (invoiceRepository.existsByOrderId(request.getOrderId())) {
            log.warn("Invoice already exists for orderId: {}", request.getOrderId());
            throw new DuplicateResourceException(
                    "Invoice already exists for orderId: " + request.getOrderId()
            );
        }

        CustomerClientDTO customer = fetchCustomer(request.getCustomerId());
        validateCustomerForBilling(customer, request.getPaymentMode());

        BigDecimal taxAmount = calculateTax(request.getSubtotal());
        BigDecimal grandTotal = request.getSubtotal().add(taxAmount);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = calculateDueDate(now, request.getPaymentMode());

        Invoice invoice = invoiceMapper.toEntity(request);
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setCustomerName(customer.getName());
        invoice.setCustomerPhone(customer.getPhone());
        invoice.setTaxPercentage(TAX_PERCENTAGE);
        invoice.setTaxAmount(taxAmount);
        invoice.setGrandTotal(grandTotal);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setDueAmount(grandTotal);
        invoice.setPaymentStatus(PaymentStatus.PENDING);
        invoice.setInvoiceDate(now);
        invoice.setDueDate(dueDate);
        invoice.setActive(true);

        Invoice savedInvoice = invoiceRepository.saveAndFlush(invoice);
        log.info("Invoice created: {} for orderId: {}",
                savedInvoice.getInvoiceNumber(), savedInvoice.getOrderId());

        return invoiceMapper.toResponseDTO(savedInvoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDTO getInvoiceById(UUID id) {
        log.debug("Fetching invoice with id: {}", id);

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with id: " + id
                ));

        return invoiceMapper.toResponseDTO(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDTO getInvoiceByNumber(String invoiceNumber) {
        log.debug("Fetching invoice with number: {}", invoiceNumber);

        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with number: " + invoiceNumber
                ));

        return invoiceMapper.toResponseDTO(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDTO getInvoiceByOrderId(UUID orderId) {
        log.debug("Fetching invoice for orderId: {}", orderId);

        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found for orderId: " + orderId
                ));

        return invoiceMapper.toResponseDTO(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponseDTO> getAllInvoices(Pageable pageable) {
        log.debug("Fetching all invoices with pagination: {}", pageable);

        return invoiceRepository.findAll(pageable)
                .map(invoiceMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponseDTO> getInvoicesByCustomerId(UUID customerId, Pageable pageable) {
        log.debug("Fetching invoices for customerId: {}", customerId);

        return invoiceRepository.findByCustomerId(customerId, pageable)
                .map(invoiceMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponseDTO> getInvoicesByStatus(PaymentStatus status, Pageable pageable) {
        log.debug("Fetching invoices by status: {}", status);

        return invoiceRepository.findByPaymentStatus(status, pageable)
                .map(invoiceMapper::toResponseDTO);
    }

    @Override
    public InvoiceResponseDTO recordPayment(UUID id, PaymentRequestDTO paymentRequest) {
        log.info("Recording payment of {} for invoiceId: {}",
                paymentRequest.getAmount(), id);

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with id: " + id
                ));

        validateInvoiceForPayment(invoice, paymentRequest.getAmount());

        BigDecimal newPaidAmount = invoice.getPaidAmount().add(paymentRequest.getAmount());
        BigDecimal newDueAmount = invoice.getGrandTotal().subtract(newPaidAmount);

        invoice.setPaidAmount(newPaidAmount);
        invoice.setDueAmount(newDueAmount);

        if (newDueAmount.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setPaymentStatus(PaymentStatus.PAID);
            invoice.setPaidAt(LocalDateTime.now());
            log.info("Invoice {} fully paid", invoice.getInvoiceNumber());
        } else {
            invoice.setPaymentStatus(PaymentStatus.PARTIAL);
            log.info("Invoice {} partially paid. Remaining due: {}",
                    invoice.getInvoiceNumber(), newDueAmount);
        }

        Invoice updatedInvoice = invoiceRepository.saveAndFlush(invoice);
        return invoiceMapper.toResponseDTO(updatedInvoice);
    }

    @Override
    public void voidInvoice(UUID id) {
        log.info("Voiding invoice with id: {}", id);

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with id: " + id
                ));

        validateInvoiceForVoid(invoice);

        invoice.setPaymentStatus(PaymentStatus.VOIDED);
        invoice.setActive(false);

        invoiceRepository.saveAndFlush(invoice);
        log.info("Invoice {} voided successfully", invoice.getInvoiceNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponseDTO> getOutstandingInvoices(UUID customerId) {
        log.debug("Fetching outstanding invoices for customerId: {}", customerId);

        List<Invoice> outstanding = invoiceRepository.findByCustomerIdAndPaymentStatusIn(
                customerId,
                List.of(PaymentStatus.PENDING, PaymentStatus.PARTIAL)
        );

        return outstanding.stream()
                .map(invoiceMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalOutstandingAmount(UUID customerId) {
        log.debug("Calculating total outstanding for customerId: {}", customerId);

        List<Invoice> outstanding = invoiceRepository.findByCustomerIdAndPaymentStatusIn(
                customerId,
                List.of(PaymentStatus.PENDING, PaymentStatus.PARTIAL)
        );

        return outstanding.stream()
                .map(Invoice::getDueAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CustomerClientDTO fetchCustomer(UUID customerId) {
        try {
            ApiResponseWrapper<CustomerClientDTO> response =
                    customerServiceClient.getCustomerById(customerId);
            return response.getData();
        } catch (FeignException.NotFound ex) {
            throw new BadRequestException("Customer not found with id: " + customerId);
        } catch (FeignException ex) {
            log.error("Customer Service call failed", ex);
            throw new ServiceUnavailableException(
                    "Customer Service is currently unavailable"
            );
        }
    }

    private void validateCustomerForBilling(CustomerClientDTO customer, PaymentMode paymentMode) {
        if (customer == null) {
            throw new BadRequestException("Customer data could not be retrieved");
        }

        if (!Boolean.TRUE.equals(customer.getActive())) {
            throw new BadRequestException("Customer is inactive");
        }

        if (paymentMode == PaymentMode.CREDIT) {
            String type = customer.getCustomerType();
            if (!"WHOLESALE".equalsIgnoreCase(type) && !"WORKSHOP".equalsIgnoreCase(type)) {
                throw new BadRequestException(
                        "CREDIT payment is only allowed for WHOLESALE or WORKSHOP customers"
                );
            }

            if (customer.getCreditLimit() == null
                    || customer.getCreditLimit().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Customer has no credit limit configured");
            }
        }
    }

    private BigDecimal calculateTax(BigDecimal subtotal) {
        return subtotal
                .multiply(TAX_PERCENTAGE)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private LocalDateTime calculateDueDate(LocalDateTime invoiceDate, PaymentMode paymentMode) {
        if (paymentMode == PaymentMode.CREDIT) {
            return invoiceDate.plusDays(CREDIT_PAYMENT_DAYS);
        }
        return invoiceDate;
    }

    private String generateInvoiceNumber() {
        String year = String.valueOf(Year.now().getValue());
        String prefix = "INV-" + year + "-";
        long count = invoiceRepository.countByInvoiceNumberStartingWith(prefix);
        return prefix + String.format("%05d", count + 1);
    }

    private void validateInvoiceForPayment(Invoice invoice, BigDecimal paymentAmount) {
        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException(
                    "Invoice is already fully paid: " + invoice.getInvoiceNumber()
            );
        }

        if (invoice.getPaymentStatus() == PaymentStatus.VOIDED) {
            throw new BadRequestException(
                    "Cannot pay a voided invoice: " + invoice.getInvoiceNumber()
            );
        }

        if (!Boolean.TRUE.equals(invoice.getActive())) {
            throw new BadRequestException(
                    "Invoice is not active: " + invoice.getInvoiceNumber()
            );
        }

        if (paymentAmount.compareTo(invoice.getDueAmount()) > 0) {
            throw new BadRequestException(
                    "Payment amount " + paymentAmount
                            + " exceeds due amount " + invoice.getDueAmount()
            );
        }
    }

    private void validateInvoiceForVoid(Invoice invoice) {
        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException(
                    "Cannot void a paid invoice: " + invoice.getInvoiceNumber()
                            + ". Initiate a refund instead."
            );
        }

        if (invoice.getPaymentStatus() == PaymentStatus.VOIDED) {
            throw new BadRequestException(
                    "Invoice is already voided: " + invoice.getInvoiceNumber()
            );
        }
    }
}
