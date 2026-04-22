package com.sparepartshop.customer_service.service.impl;

import com.sparepartshop.customer_service.dto.CustomerRequestDTO;
import com.sparepartshop.customer_service.dto.CustomerResponseDTO;
import com.sparepartshop.customer_service.entity.Customer;
import com.sparepartshop.customer_service.enums.CustomerType;
import com.sparepartshop.customer_service.exception.BadRequestException;
import com.sparepartshop.customer_service.exception.DuplicateResourceException;
import com.sparepartshop.customer_service.exception.ResourceNotFoundException;
import com.sparepartshop.customer_service.mapper.CustomerMapper;
import com.sparepartshop.customer_service.repository.CustomerRepository;
import com.sparepartshop.customer_service.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerResponseDTO createCustomer(CustomerRequestDTO request) {
        log.info("Creating customer with phone: {}", request.getPhone());

        validateBusinessRules(request);

        if (customerRepository.existsByPhone(request.getPhone())) {
            log.warn("Duplicate phone attempted: {}", request.getPhone());
            throw new DuplicateResourceException(
                    "Customer already exists with phone: " + request.getPhone()
            );
        }

        if (StringUtils.hasText(request.getEmail())
                && customerRepository.existsByEmail(request.getEmail())) {
            log.warn("Duplicate email attempted: {}", request.getEmail());
            throw new DuplicateResourceException(
                    "Customer already exists with email: " + request.getEmail()
            );
        }

        Customer customer = customerMapper.toEntity(request);
        customer.setCurrentBalance(BigDecimal.ZERO);
        customer.setActive(true);

        if (customer.getCreditLimit() == null) {
            customer.setCreditLimit(BigDecimal.ZERO);
        }

        Customer saved = customerRepository.saveAndFlush(customer);

        log.info("Customer created with id: {}", saved.getId());
        return customerMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO getCustomerById(UUID id) {
        log.debug("Fetching customer with id: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + id
                ));

        return customerMapper.toResponseDTO(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO getCustomerByPhone(String phone) {
        log.debug("Fetching customer with phone: {}", phone);

        Customer customer = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with phone: " + phone
                ));

        return customerMapper.toResponseDTO(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponseDTO> getAllCustomers(Pageable pageable) {
        log.debug("Fetching all active customers with pagination: {}", pageable);

        return customerRepository.findByActiveTrue(pageable)
                .map(customerMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponseDTO> getCustomersByType(CustomerType type, Pageable pageable) {
        log.debug("Fetching customers by type: {}", type);

        return customerRepository.findByCustomerType(type, pageable)
                .map(customerMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponseDTO> getCustomersByCity(String city, Pageable pageable) {
        log.debug("Fetching customers by city: {}", city);

        return customerRepository.findByCityIgnoreCase(city, pageable)
                .map(customerMapper::toResponseDTO);
    }

    @Override
    public CustomerResponseDTO updateCustomer(UUID id, CustomerRequestDTO request) {
        log.info("Updating customer with id: {}", id);

        validateBusinessRules(request);

        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + id
                ));

        if (!existing.getPhone().equals(request.getPhone())
                && customerRepository.existsByPhone(request.getPhone())) {
            log.warn("Cannot update — phone already exists: {}", request.getPhone());
            throw new DuplicateResourceException(
                    "Customer already exists with phone: " + request.getPhone()
            );
        }

        if (StringUtils.hasText(request.getEmail())
                && !request.getEmail().equalsIgnoreCase(existing.getEmail())
                && customerRepository.existsByEmail(request.getEmail())) {
            log.warn("Cannot update — email already exists: {}", request.getEmail());
            throw new DuplicateResourceException(
                    "Customer already exists with email: " + request.getEmail()
            );
        }

        customerMapper.updateEntityFromDto(request, existing);

        Customer updated = customerRepository.saveAndFlush(existing);

        log.info("Customer updated with id: {}", updated.getId());
        return customerMapper.toResponseDTO(updated);
    }

    @Override
    public void deleteCustomer(UUID id) {
        log.info("Soft deleting customer with id: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + id
                ));

        customer.setActive(false);
        customerRepository.save(customer);

        log.info("Customer soft-deleted with id: {}", id);
    }

    private void validateBusinessRules(CustomerRequestDTO request) {
        if (request.getCustomerType() == CustomerType.WHOLESALE
                || request.getCustomerType() == CustomerType.WORKSHOP) {

            if (!StringUtils.hasText(request.getBusinessName())) {
                throw new BadRequestException(
                        "Business name is required for " + request.getCustomerType() + " customers"
                );
            }

            if (!StringUtils.hasText(request.getGstNumber())) {
                throw new BadRequestException(
                        "GST number is required for " + request.getCustomerType() + " customers"
                );
            }
        }
    }
}
