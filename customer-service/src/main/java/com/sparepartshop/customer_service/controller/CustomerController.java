package com.sparepartshop.customer_service.controller;


import com.sparepartshop.customer_service.constants.ApiPaths;
import com.sparepartshop.customer_service.dto.ApiResponse;
import com.sparepartshop.customer_service.dto.CustomerRequestDTO;
import com.sparepartshop.customer_service.dto.CustomerResponseDTO;
import com.sparepartshop.customer_service.enums.CustomerType;
import com.sparepartshop.customer_service.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.CUSTOMERS)
@Slf4j
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;


    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> createCustomer(
            @Valid @RequestBody CustomerRequestDTO request) {

        log.info("Received request to create customer: {}", request.getPhone());
        CustomerResponseDTO created = customerService.createCustomer(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerResponseDTO>>> getAllCustomers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        log.info("Fetching all customers with pagination: {}", pageable);
        Page<CustomerResponseDTO> customers = customerService.getAllCustomers(pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Customers fetched successfully", customers)
        );
    }

    @GetMapping(ApiPaths.CUSTOMER_BY_ID)
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> getCustomerById(
            @PathVariable UUID id) {

        log.info("Received request to fetch customer: {}", id);
        CustomerResponseDTO customer = customerService.getCustomerById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Customer fetched successfully", customer)
        );
    }

    @GetMapping(ApiPaths.BY_PHONE)
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> getCustomerByPhone(
            @PathVariable String phone) {

        log.info("Fetching customer by phone: {}", phone);
        CustomerResponseDTO customer = customerService.getCustomerByPhone(phone);

        return ResponseEntity.ok(
                ApiResponse.success("Customer fetched successfully", customer)
        );
    }

    @GetMapping(ApiPaths.BY_TYPE)
    public ResponseEntity<ApiResponse<Page<CustomerResponseDTO>>> getCustomerByType(
            @PathVariable CustomerType type,
            @PageableDefault(size = 20) Pageable pageable
            ) {
        log.info("Fetching customers by type: {}", type);
        Page<CustomerResponseDTO> customers = customerService.getCustomersByType(type, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Customers fetched successfully", customers)
        );
    }

    @GetMapping(ApiPaths.BY_CITY)
    public ResponseEntity<ApiResponse<Page<CustomerResponseDTO>>> getCustomerByCity(
            @PathVariable String city,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("Fetching customers by city: {}", city);
        Page<CustomerResponseDTO> customers = customerService.getCustomersByCity(city, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Customers fetched successfully", customers)
        );
    }

    @PutMapping(ApiPaths.CUSTOMER_BY_ID)
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequestDTO request
    ) {
        log.info("Updating customer: {}", id);
        CustomerResponseDTO updatedCustomer = customerService.updateCustomer(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Customer updated successfully", updatedCustomer)
        );
    }

    @DeleteMapping(ApiPaths.CUSTOMER_BY_ID)
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable UUID id) {
        log.info("Deleting customer: {}", id);
        customerService.deleteCustomer(id);

        return ResponseEntity.ok(
                ApiResponse.success("Customer deleted successfully")
        );
    }


}
