package com.sparepartshop.inventory_service.service.impl;

import com.sparepartshop.inventory_service.dto.InventoryRequestDTO;
import com.sparepartshop.inventory_service.dto.InventoryResponseDTO;
import com.sparepartshop.inventory_service.dto.StockUpdateRequestDTO;
import com.sparepartshop.inventory_service.entity.Inventory;
import com.sparepartshop.inventory_service.exception.BadRequestException;
import com.sparepartshop.inventory_service.exception.DuplicateResourceException;
import com.sparepartshop.inventory_service.exception.ResourceNotFoundException;
import com.sparepartshop.inventory_service.repository.InventoryRepository;
import com.sparepartshop.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    public InventoryResponseDTO createInventory(InventoryRequestDTO request) {
        log.info("Creating inventory for productId: {}", request.getProductId());

        if (inventoryRepository.existsByProductId(request.getProductId())) {
            log.warn("Inventory already exists for productId: {}", request.getProductId());
            throw new DuplicateResourceException(
                    "Inventory already exists for product ID: " + request.getProductId()
            );
        }

        Inventory inventory = mapToEntity(request);
        Inventory saved = inventoryRepository.save(inventory);

        log.info("Inventory created with id: {} for productId: {}", saved.getId(), saved.getProductId());
        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponseDTO getInventoryById(UUID id) {
        log.debug("Fetching inventory with id: {}", id);

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found with id: " + id
                ));

        return mapToResponseDTO(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponseDTO getInventoryByProductId(UUID productId) {
        log.debug("Fetching inventory for productId: {}", productId);

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product ID: " + productId
                ));

        return mapToResponseDTO(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponseDTO> getAllInventory(Pageable pageable) {
        log.debug("Fetching all inventory with pagination: {}", pageable);

        return inventoryRepository.findAll(pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    public InventoryResponseDTO updateInventory(UUID id, InventoryRequestDTO request) {
        log.info("Updating inventory with id: {}", id);

        Inventory existing = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found with id: " + id
                ));

        if (!existing.getProductId().equals(request.getProductId())
                && inventoryRepository.existsByProductId(request.getProductId())) {
            log.warn("Cannot update — inventory already exists for productId: {}", request.getProductId());
            throw new DuplicateResourceException(
                    "Inventory already exists for product ID: " + request.getProductId()
            );
        }

        existing.setProductId(request.getProductId());
        existing.setStockQuantity(request.getStockQuantity());
        existing.setReorderLevel(request.getReorderLevel());
        existing.setReorderQuantity(request.getReorderQuantity());
        existing.setWarehouseLocation(request.getWarehouseLocation());
        existing.setUnitOfMeasure(request.getUnitOfMeasure());

        Inventory updated = inventoryRepository.save(existing);

        log.info("Inventory updated with id: {}", updated.getId());
        return mapToResponseDTO(updated);
    }

    @Override
    public void deleteInventory(UUID id) {
        log.info("Deleting inventory with id: {}", id);

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found with id: " + id
                ));

        inventoryRepository.delete(inventory);
        log.info("Inventory deleted with id: {}", id);
    }

    @Override
    public InventoryResponseDTO addStock(UUID productId, StockUpdateRequestDTO request) {
        log.info("Adding {} units of stock for productId: {}", request.getQuantity(), productId);

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product ID: " + productId
                ));

        inventory.setStockQuantity(inventory.getStockQuantity() + request.getQuantity());
        inventory.setLastRestockedAt(LocalDateTime.now());

        Inventory updated = inventoryRepository.save(inventory);

        log.info("Stock added. New stock for productId {}: {}", productId, updated.getStockQuantity());
        return mapToResponseDTO(updated);
    }

    @Override
    public InventoryResponseDTO reduceStock(UUID productId, StockUpdateRequestDTO request) {
        log.info("Reducing {} units of stock for productId: {}", request.getQuantity(), productId);

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product ID: " + productId
                ));

        if (inventory.getStockQuantity() < request.getQuantity()) {
            log.warn("Insufficient stock for productId: {}. Available: {}, Requested: {}",
                    productId, inventory.getStockQuantity(), request.getQuantity());
            throw new BadRequestException(
                    "Insufficient stock. Available: " + inventory.getStockQuantity()
                            + ", Requested: " + request.getQuantity()
            );
        }

        inventory.setStockQuantity(inventory.getStockQuantity() - request.getQuantity());

        Inventory updated = inventoryRepository.save(inventory);

        log.info("Stock reduced. New stock for productId {}: {}", productId, updated.getStockQuantity());
        return mapToResponseDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponseDTO> getLowStockItems(Pageable pageable) {
        log.debug("Fetching low stock items");

        return inventoryRepository.findAll(pageable)
                .map(this::mapToResponseDTO)
                .map(dto -> {
                    if (Boolean.TRUE.equals(dto.getLowStock())) {
                        return dto;
                    }
                    return null;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkStockAvailability(UUID productId, Integer quantity) {
        log.debug("Checking stock availability for productId: {}, quantity: {}", productId, quantity);

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product ID: " + productId
                ));

        return inventory.getStockQuantity() >= quantity;
    }

    private Inventory mapToEntity(InventoryRequestDTO dto) {
        return Inventory.builder()
                .productId(dto.getProductId())
                .stockQuantity(dto.getStockQuantity())
                .reorderLevel(dto.getReorderLevel())
                .reorderQuantity(dto.getReorderQuantity())
                .warehouseLocation(dto.getWarehouseLocation())
                .unitOfMeasure(dto.getUnitOfMeasure())
                .build();
    }

    private InventoryResponseDTO mapToResponseDTO(Inventory inventory) {
        return InventoryResponseDTO.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .stockQuantity(inventory.getStockQuantity())
                .reorderLevel(inventory.getReorderLevel())
                .reorderQuantity(inventory.getReorderQuantity())
                .warehouseLocation(inventory.getWarehouseLocation())
                .unitOfMeasure(inventory.getUnitOfMeasure())
                .lowStock(inventory.getStockQuantity() <= inventory.getReorderLevel())
                .lastRestockedAt(inventory.getLastRestockedAt())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
