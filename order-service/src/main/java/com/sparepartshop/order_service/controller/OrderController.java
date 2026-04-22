package com.sparepartshop.order_service.controller;

import com.sparepartshop.order_service.constants.ApiPaths;
import com.sparepartshop.order_service.dto.ApiResponse;
import com.sparepartshop.order_service.dto.OrderRequestDTO;
import com.sparepartshop.order_service.dto.OrderResponseDTO;
import com.sparepartshop.order_service.dto.OrderStatusUpdateDTO;
import com.sparepartshop.order_service.enums.OrderStatus;
import com.sparepartshop.order_service.service.OrderService;
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
@RequestMapping(ApiPaths.ORDERS)
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDTO>> createOrder(
            @Valid @RequestBody OrderRequestDTO request) {

        log.info("Received request to create order for customerId: {}", request.getCustomerId());
        OrderResponseDTO created = orderService.createOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", created));
    }

    @GetMapping(ApiPaths.ORDER_BY_ID)
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(@PathVariable UUID id) {
        log.info("Fetching order with id: {}", id);
        OrderResponseDTO order = orderService.getOrderById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Order fetched successfully", order)
        );
    }

    @GetMapping(ApiPaths.ORDER_BY_NUMBER)
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderByOrderNumber(
            @PathVariable String orderNumber) {

        log.info("Fetching order with orderNumber: {}", orderNumber);
        OrderResponseDTO order = orderService.getOrderByOrderNumber(orderNumber);

        return ResponseEntity.ok(
                ApiResponse.success("Order fetched successfully", order)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponseDTO>>> getAllOrders(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        log.info("Fetching all orders");
        Page<OrderResponseDTO> orders = orderService.getAllOrders(pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Orders fetched successfully", orders)
        );
    }

    @GetMapping(ApiPaths.BY_CUSTOMER)
    public ResponseEntity<ApiResponse<Page<OrderResponseDTO>>> getOrdersByCustomerId(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Fetching orders for customerId: {}", customerId);
        Page<OrderResponseDTO> orders = orderService.getOrdersByCustomerId(customerId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Orders fetched successfully", orders)
        );
    }

    @GetMapping(ApiPaths.BY_STATUS)
    public ResponseEntity<ApiResponse<Page<OrderResponseDTO>>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Fetching orders by status: {}", status);
        Page<OrderResponseDTO> orders = orderService.getOrdersByStatus(status, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Orders fetched successfully", orders)
        );
    }

    @PatchMapping(ApiPaths.UPDATE_STATUS)
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrderStatusUpdateDTO statusUpdate) {

        log.info("Updating status for orderId: {} to {}", id, statusUpdate.getStatus());
        OrderResponseDTO updated = orderService.updateOrderStatus(id, statusUpdate);

        return ResponseEntity.ok(
                ApiResponse.success("Order status updated successfully", updated)
        );
    }

    @PostMapping(ApiPaths.CANCEL_ORDER)
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable UUID id) {
        log.info("Cancelling order with id: {}", id);
        orderService.cancelOrder(id);

        return ResponseEntity.ok(
                ApiResponse.success("Order cancelled successfully")
        );
    }
}
