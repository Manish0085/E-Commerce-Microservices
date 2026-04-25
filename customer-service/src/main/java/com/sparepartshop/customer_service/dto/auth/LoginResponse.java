package com.sparepartshop.customer_service.dto.auth;

import com.sparepartshop.customer_service.enums.Role;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String tokenType;
    private long expiresInMs;
    private UUID customerId;
    private String name;
    private String phone;
    private Role role;
}
