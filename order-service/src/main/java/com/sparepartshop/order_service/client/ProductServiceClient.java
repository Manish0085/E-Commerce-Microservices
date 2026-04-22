package com.sparepartshop.order_service.client;

import com.sparepartshop.order_service.constants.ClientApiPaths;
import com.sparepartshop.order_service.dto.client.ApiResponseWrapper;
import com.sparepartshop.order_service.dto.client.ProductClientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping(ClientApiPaths.Product.GET_BY_ID)
    ApiResponseWrapper<ProductClientDTO> getProductById(@PathVariable("id") UUID productId);
}
