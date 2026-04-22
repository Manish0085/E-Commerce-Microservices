package com.sparepartshop.payment_service.mapper;

import com.sparepartshop.payment_service.dto.PaymentInitiateRequestDTO;
import com.sparepartshop.payment_service.dto.PaymentResponseDTO;
import com.sparepartshop.payment_service.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentReference", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "gatewayProvider", ignore = true)
    @Mapping(target = "gatewayTransactionId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "webhookPayload", ignore = true)
    @Mapping(target = "initiatedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "refundedAt", ignore = true)
    @Mapping(target = "refundAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Payment toEntity(PaymentInitiateRequestDTO dto);

    PaymentResponseDTO toResponseDTO(Payment payment);
}
