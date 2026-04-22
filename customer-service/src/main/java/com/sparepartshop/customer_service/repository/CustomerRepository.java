package com.sparepartshop.customer_service.repository;

import com.sparepartshop.customer_service.entity.Customer;
import com.sparepartshop.customer_service.enums.CustomerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    Page<Customer> findByCustomerType(CustomerType customerType, Pageable pageable);

    Page<Customer> findByCityIgnoreCase(String city, Pageable pageable);

    Page<Customer> findByActiveTrue(Pageable pageable);

    Page<Customer> findByNameContainingIgnoreCase(String name, Pageable pageable);
}