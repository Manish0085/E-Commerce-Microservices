package com.sparepartshop.customer_service.service.impl;

import com.sparepartshop.customer_service.dto.CustomerResponseDTO;
import com.sparepartshop.customer_service.dto.auth.LoginRequest;
import com.sparepartshop.customer_service.dto.auth.LoginResponse;
import com.sparepartshop.customer_service.dto.auth.SignupRequest;
import com.sparepartshop.customer_service.entity.Customer;
import com.sparepartshop.customer_service.enums.Role;
import com.sparepartshop.customer_service.exception.DuplicateResourceException;
import com.sparepartshop.customer_service.exception.UnauthorizedException;
import com.sparepartshop.customer_service.mapper.CustomerMapper;
import com.sparepartshop.customer_service.repository.CustomerRepository;
import com.sparepartshop.customer_service.security.JwtService;
import com.sparepartshop.customer_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final String INVALID_CREDENTIALS = "Invalid phone or password";

    @Override
    @Transactional
    public CustomerResponseDTO signup(SignupRequest request) {
        log.info("Signup attempt for phone: {}", request.getPhone());

        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException(
                    "A customer with phone " + request.getPhone() + " already exists");
        }
        if (request.getEmail() != null && customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "A customer with email " + request.getEmail() + " already exists");
        }

        Customer customer = Customer.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .city(request.getCity())
                .customerType(request.getCustomerType())
                .businessName(request.getBusinessName())
                .gstNumber(request.getGstNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .creditLimit(BigDecimal.ZERO)
                .currentBalance(BigDecimal.ZERO)
                .active(true)
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Signup successful for customer id: {}", saved.getId());
        return customerMapper.toResponseDTO(saved);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for phone: {}", request.getPhone());

        Customer customer = customerRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> {
                    log.warn("Login failed — unknown phone: {}", request.getPhone());
                    return new UnauthorizedException(INVALID_CREDENTIALS);
                });

        if (Boolean.FALSE.equals(customer.getActive())) {
            log.warn("Login failed — customer inactive: {}", customer.getId());
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        if (customer.getPasswordHash() == null) {
            log.warn("Login failed — customer has no password set: {}", customer.getId());
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            log.warn("Login failed — bad password for customer: {}", customer.getId());
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        String token = jwtService.generateToken(customer);
        log.info("Login successful for customer id: {}", customer.getId());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .customerId(customer.getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .role(customer.getRole())
                .build();
    }
}
