package com.sparepartshop.order_service.client;

import com.sparepartshop.order_service.constants.ClientApiPaths;
import com.sparepartshop.order_service.dto.client.ApiResponseWrapper;
import com.sparepartshop.order_service.dto.client.InventoryClientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "inventory-service", fallback = InventoryServiceClientFallback.class)
public interface InventoryServiceClient {

    @GetMapping(ClientApiPaths.Inventory.GET_BY_PRODUCT_ID)
    ApiResponseWrapper<InventoryClientDTO> getInventoryByProductId(
            @PathVariable("productId") UUID productId);

    @GetMapping(ClientApiPaths.Inventory.CHECK_STOCK)
    ApiResponseWrapper<Boolean> checkStockAvailability(
            @PathVariable("productId") UUID productId,
            @RequestParam("quantity") Integer quantity);

    @PostMapping(ClientApiPaths.Inventory.REDUCE_STOCK)
    ApiResponseWrapper<InventoryClientDTO> reduceStock(
            @PathVariable("productId") UUID productId,
            @RequestBody Map<String, Integer> request);

    @PostMapping(ClientApiPaths.Inventory.ADD_STOCK)
    ApiResponseWrapper<InventoryClientDTO> addStock(
            @PathVariable("productId") UUID productId,
            @RequestBody Map<String, Integer> request);
}
