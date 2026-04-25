package com.sparepartshop.customer_service.service;

import com.sparepartshop.customer_service.dto.CustomerResponseDTO;
import com.sparepartshop.customer_service.dto.auth.LoginRequest;
import com.sparepartshop.customer_service.dto.auth.LoginResponse;
import com.sparepartshop.customer_service.dto.auth.SignupRequest;

public interface AuthService {

    CustomerResponseDTO signup(SignupRequest request);

    LoginResponse login(LoginRequest request);
}
