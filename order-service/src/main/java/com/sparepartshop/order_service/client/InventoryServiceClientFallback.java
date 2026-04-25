package com.sparepartshop.order_service.client;

import com.sparepartshop.order_service.dto.client.ApiResponseWrapper;
import com.sparepartshop.order_service.dto.client.InventoryClientDTO;
import com.sparepartshop.order_service.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class InventoryServiceClientFallback implements InventoryServiceClient {

    private static final String SERVICE_DOWN_MSG =
            "inventory-service is currently unavailable. Please try again shortly.";

    @Override
    public ApiResponseWrapper<InventoryClientDTO> getInventoryByProductId(UUID productId) {
        log.warn("Fallback: getInventoryByProductId({}) — inventory-service unavailable", productId);
        throw new ServiceUnavailableException(SERVICE_DOWN_MSG);
    }

    @Override
    public ApiResponseWrapper<Boolean> checkStockAvailability(UUID productId, Integer quantity) {
        log.warn("Fallback: checkStockAvailability({}, {}) — inventory-service unavailable",
                productId, quantity);
        throw new ServiceUnavailableException(SERVICE_DOWN_MSG);
    }

    @Override
    public ApiResponseWrapper<InventoryClientDTO> reduceStock(UUID productId,
                                                              Map<String, Integer> request) {
        log.warn("Fallback: reduceStock({}, {}) — inventory-service unavailable", productId, request);
        throw new ServiceUnavailableException(SERVICE_DOWN_MSG);
    }

    @Override
    public ApiResponseWrapper<InventoryClientDTO> addStock(UUID productId,
                                                           Map<String, Integer> request) {
        // addStock is the COMPENSATION for reduceStock in the saga. If this fallback fires,
        // stock was reduced but we can't give it back — that's a real data-consistency event,
        // not a normal "service down" case. Log at ERROR so ops can manually reconcile.
        log.error("COMPENSATION FAILED: addStock({}, {}) — inventory-service unavailable. " +
                "Manual reconciliation required.", productId, request);
        throw new ServiceUnavailableException(
                "Cannot compensate reduceStock — inventory-service is unavailable. " +
                        "Manual reconciliation required for productId=" + productId);
    }
}
