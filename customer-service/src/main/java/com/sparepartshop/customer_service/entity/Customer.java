package com.sparepartshop.customer_service.entity;

import com.sparepartshop.customer_service.enums.CustomerType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "customers",
        indexes = {
                @Index(name = "idx_customer_phone", columnList = "phone", unique = true),
                @Index(name = "idx_customer_email", columnList = "email", unique = true),
                @Index(name = "idx_customer_type", columnList = "customer_type"),
                @Index(name = "idx_customer_city", columnList = "city")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(unique = true, length = 150)
    private String email;

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(length = 250)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CustomerType customerType;


    @Column(length = 150)
    private String businessName;

    @Column(length = 20)
    private String gstNumber;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Builder.Default
    private Boolean active = true;

    @Column(updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

}
