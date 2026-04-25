package com.sparepartshop.customer_service.dto.auth;

import com.sparepartshop.customer_service.enums.CustomerType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must be 10-15 digits")
    private String phone;

    @Email(message = "Email format is invalid")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    private String password;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @Size(max = 250)
    private String address;

    @NotNull(message = "Customer type is required")
    private CustomerType customerType;

    @Size(max = 150)
    private String businessName;

    @Size(max = 20)
    private String gstNumber;
}
