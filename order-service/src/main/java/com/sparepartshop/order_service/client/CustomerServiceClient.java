package com.sparepartshop.order_service.client;

import com.sparepartshop.order_service.constants.ClientApiPaths;
import com.sparepartshop.order_service.dto.client.ApiResponseWrapper;
import com.sparepartshop.order_service.dto.client.CustomerClientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "customer-service")
public interface CustomerServiceClient {

    @GetMapping(ClientApiPaths.Customer.GET_BY_ID)
    ApiResponseWrapper<CustomerClientDTO> getCustomerById(@PathVariable("id") UUID customerId);
}
