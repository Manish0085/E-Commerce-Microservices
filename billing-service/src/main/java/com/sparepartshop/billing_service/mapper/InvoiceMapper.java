package com.sparepartshop.billing_service.mapper;

import com.sparepartshop.billing_service.dto.InvoiceRequestDTO;
import com.sparepartshop.billing_service.dto.InvoiceResponseDTO;
import com.sparepartshop.billing_service.entity.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "customerPhone", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "taxPercentage", ignore = true)
    @Mapping(target = "grandTotal", ignore = true)
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "dueAmount", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "invoiceDate", ignore = true)
    @Mapping(target = "dueDate", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Invoice toEntity(InvoiceRequestDTO dto);

    InvoiceResponseDTO toResponseDTO(Invoice invoice);
}
