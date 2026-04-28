package com.sparepartshop.notification_service.client;

import com.sparepartshop.notification_service.dto.ApiResponseWrapper;
import com.sparepartshop.notification_service.dto.CustomerClientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "customer-service")
public interface CustomerServiceClient {

    @GetMapping("/api/v1/customers/{id}")
    ApiResponseWrapper<CustomerClientDTO> getCustomerById(@PathVariable("id") UUID customerId);
}
