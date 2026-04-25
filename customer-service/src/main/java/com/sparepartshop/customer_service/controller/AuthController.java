package com.sparepartshop.customer_service.controller;

import com.sparepartshop.customer_service.constants.ApiPaths;
import com.sparepartshop.customer_service.dto.ApiResponse;
import com.sparepartshop.customer_service.dto.CustomerResponseDTO;
import com.sparepartshop.customer_service.dto.auth.LoginRequest;
import com.sparepartshop.customer_service.dto.auth.LoginResponse;
import com.sparepartshop.customer_service.dto.auth.SignupRequest;
import com.sparepartshop.customer_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.AUTH)
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping(ApiPaths.AUTH_SIGNUP)
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> signup(
            @Valid @RequestBody SignupRequest request) {

        log.info("Signup request received for phone: {}", request.getPhone());
        CustomerResponseDTO created = authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Signup successful", created));
    }

    @PostMapping(ApiPaths.AUTH_LOGIN)
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login request received for phone: {}", request.getPhone());
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
