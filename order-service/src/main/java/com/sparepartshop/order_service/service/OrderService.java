package com.sparepartshop.order_service.service;

import com.sparepartshop.order_service.dto.OrderRequestDTO;
import com.sparepartshop.order_service.dto.OrderResponseDTO;
import com.sparepartshop.order_service.dto.OrderStatusUpdateDTO;
import com.sparepartshop.order_service.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderResponseDTO createOrder(OrderRequestDTO request);

    OrderResponseDTO getOrderById(UUID id);

    OrderResponseDTO getOrderByOrderNumber(String orderNumber);

    Page<OrderResponseDTO> getAllOrders(Pageable pageable);

    Page<OrderResponseDTO> getOrdersByCustomerId(UUID customerId, Pageable pageable);

    Page<OrderResponseDTO> getOrdersByStatus(OrderStatus status, Pageable pageable);

    OrderResponseDTO updateOrderStatus(UUID id, OrderStatusUpdateDTO statusUpdate);

    void cancelOrder(UUID id);
}