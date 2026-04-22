package com.sparepartshop.customer_service.service;

import com.sparepartshop.customer_service.dto.CustomerRequestDTO;
import com.sparepartshop.customer_service.dto.CustomerResponseDTO;
import com.sparepartshop.customer_service.enums.CustomerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {

    CustomerResponseDTO createCustomer(CustomerRequestDTO request);

    CustomerResponseDTO getCustomerById(UUID id);

    CustomerResponseDTO getCustomerByPhone(String phone);

    Page<CustomerResponseDTO> getAllCustomers(Pageable pageable);

    Page<CustomerResponseDTO> getCustomersByType(CustomerType type, Pageable pageable);

    Page<CustomerResponseDTO> getCustomersByCity(String city, Pageable pageable);

    CustomerResponseDTO updateCustomer(UUID id, CustomerRequestDTO request);

    void deleteCustomer(UUID id);
}