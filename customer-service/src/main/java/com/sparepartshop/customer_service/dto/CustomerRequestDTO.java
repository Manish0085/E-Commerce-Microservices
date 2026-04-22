package com.sparepartshop.customer_service.dto;

import com.sparepartshop.customer_service.enums.CustomerType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name cannot exceed 150 characters")
    private String name;

    @Email(message = "Invalid email format")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String phone;

    @Size(max = 250, message = "Address cannot exceed 250 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @NotNull(message = "Customer type is required")
    private CustomerType customerType;

    @Size(max = 150, message = "Business name cannot exceed 150 characters")
    private String businessName;

    @Size(max = 20, message = "GST number cannot exceed 20 characters")
    private String gstNumber;

    @DecimalMin(value = "0.00", message = "Credit limit cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Credit limit format is invalid")
    private BigDecimal creditLimit;
}
