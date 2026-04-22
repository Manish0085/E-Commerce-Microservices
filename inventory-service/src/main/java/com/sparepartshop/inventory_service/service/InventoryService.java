package com.sparepartshop.inventory_service.service;

import com.sparepartshop.inventory_service.dto.InventoryRequestDTO;
import com.sparepartshop.inventory_service.dto.InventoryResponseDTO;
import com.sparepartshop.inventory_service.dto.StockUpdateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InventoryService {

    InventoryResponseDTO createInventory(InventoryRequestDTO request);

    InventoryResponseDTO getInventoryById(UUID id);

    InventoryResponseDTO getInventoryByProductId(UUID productId);

    Page<InventoryResponseDTO> getAllInventory(Pageable pageable);

    InventoryResponseDTO updateInventory(UUID id, InventoryRequestDTO request);

    void deleteInventory(UUID id);

    InventoryResponseDTO addStock(UUID productId, StockUpdateRequestDTO request);

    InventoryResponseDTO reduceStock(UUID productId, StockUpdateRequestDTO request);

    Page<InventoryResponseDTO> getLowStockItems(Pageable pageable);

    boolean checkStockAvailability(UUID productId, Integer quantity);
}
