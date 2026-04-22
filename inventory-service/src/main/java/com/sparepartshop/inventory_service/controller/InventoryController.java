package com.sparepartshop.inventory_service.controller;

import com.sparepartshop.inventory_service.constants.ApiPaths;
import com.sparepartshop.inventory_service.dto.ApiResponse;
import com.sparepartshop.inventory_service.dto.InventoryRequestDTO;
import com.sparepartshop.inventory_service.dto.InventoryResponseDTO;
import com.sparepartshop.inventory_service.dto.StockUpdateRequestDTO;
import com.sparepartshop.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.INVENTORY)
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponseDTO>> createInventory(
            @Valid @RequestBody InventoryRequestDTO request) {

        log.info("Received request to create inventory for productId: {}", request.getProductId());
        InventoryResponseDTO created = inventoryService.createInventory(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inventory created successfully", created));
    }

    @GetMapping(ApiPaths.INVENTORY_BY_ID)
    public ResponseEntity<ApiResponse<InventoryResponseDTO>> getInventoryById(@PathVariable UUID id) {
        log.info("Received request to fetch inventory with id: {}", id);
        InventoryResponseDTO inventory = inventoryService.getInventoryById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Inventory fetched successfully", inventory)
        );
    }

    @GetMapping(ApiPaths.BY_PRODUCT_ID)
    public ResponseEntity<ApiResponse<InventoryResponseDTO>> getInventoryByProductId(
            @PathVariable UUID productId) {

        log.info("Received request to fetch inventory for productId: {}", productId);
        InventoryResponseDTO inventory = inventoryService.getInventoryByProductId(productId);

        return ResponseEntity.ok(
                ApiResponse.success("Inventory fetched successfully", inventory)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InventoryResponseDTO>>> getAllInventory(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        log.info("Received request to fetch all inventory");
        Page<InventoryResponseDTO> inventory = inventoryService.getAllInventory(pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Inventory fetched successfully", inventory)
        );
    }

    @PutMapping(ApiPaths.INVENTORY_BY_ID)
    public ResponseEntity<ApiResponse<InventoryResponseDTO>> updateInventory(
            @PathVariable UUID id,
            @Valid @RequestBody InventoryRequestDTO request) {

        log.info("Received request to update inventory with id: {}", id);
        InventoryResponseDTO updated = inventoryService.updateInventory(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Inventory updated successfully", updated)
        );
    }

    @DeleteMapping(ApiPaths.INVENTORY_BY_ID)
    public ResponseEntity<ApiResponse<Void>> deleteInventory(@PathVariable UUID id) {
        log.info("Received request to delete inventory with id: {}", id);
        inventoryService.deleteInventory(id);

        return ResponseEntity.ok(
                ApiResponse.success("Inventory deleted successfully")
        );
    }

    @PostMapping(ApiPaths.ADD_STOCK)
    public ResponseEntity<ApiResponse<InventoryResponseDTO>> addStock(
            @PathVariable UUID productId,
            @Valid @RequestBody StockUpdateRequestDTO request) {

        log.info("Received request to add {} units for productId: {}", request.getQuantity(), productId);
        InventoryResponseDTO updated = inventoryService.addStock(productId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Stock added successfully", updated)
        );
    }

    @PostMapping(ApiPaths.REDUCE_STOCK)
    public ResponseEntity<ApiResponse<InventoryResponseDTO>> reduceStock(
            @PathVariable UUID productId,
            @Valid @RequestBody StockUpdateRequestDTO request) {

        log.info("Received request to reduce {} units for productId: {}", request.getQuantity(), productId);
        InventoryResponseDTO updated = inventoryService.reduceStock(productId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Stock reduced successfully", updated)
        );
    }

    @GetMapping(ApiPaths.LOW_STOCK)
    public ResponseEntity<ApiResponse<Page<InventoryResponseDTO>>> getLowStockItems(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Received request to fetch low stock items");
        Page<InventoryResponseDTO> lowStock = inventoryService.getLowStockItems(pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Low stock items fetched successfully", lowStock)
        );
    }

    @GetMapping(ApiPaths.CHECK_STOCK)
    public ResponseEntity<ApiResponse<Boolean>> checkStockAvailability(
            @PathVariable UUID productId,
            @RequestParam Integer quantity) {

        log.info("Checking stock availability for productId: {}, quantity: {}", productId, quantity);
        boolean available = inventoryService.checkStockAvailability(productId, quantity);

        String message = available ? "Stock is available" : "Insufficient stock";
        return ResponseEntity.ok(
                ApiResponse.success(message, available)
        );
    }
}
