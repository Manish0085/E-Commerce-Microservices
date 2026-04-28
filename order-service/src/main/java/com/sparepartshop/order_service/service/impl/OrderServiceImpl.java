package com.sparepartshop.order_service.service.impl;

import com.sparepartshop.order_service.client.CustomerServiceClient;
import com.sparepartshop.order_service.client.InventoryServiceClient;
import com.sparepartshop.order_service.client.ProductServiceClient;
import com.sparepartshop.order_service.dto.*;
import com.sparepartshop.order_service.dto.client.ApiResponseWrapper;
import com.sparepartshop.order_service.dto.client.CustomerClientDTO;
import com.sparepartshop.order_service.dto.client.ProductClientDTO;
import com.sparepartshop.order_service.entity.Order;
import com.sparepartshop.order_service.entity.OrderItem;
import com.sparepartshop.order_service.enums.OrderStatus;
import com.sparepartshop.order_service.enums.PaymentMode;
import com.sparepartshop.order_service.event.OrderCancelledEvent;
import com.sparepartshop.order_service.event.OrderPlacedEvent;
import com.sparepartshop.order_service.exception.BadRequestException;
import com.sparepartshop.order_service.exception.ResourceNotFoundException;
import com.sparepartshop.order_service.exception.ServiceUnavailableException;
import com.sparepartshop.order_service.repository.OrderRepository;
import com.sparepartshop.order_service.service.OrderService;
import org.springframework.kafka.core.KafkaTemplate;
import feign.FeignException;
import org.aspectj.weaver.ast.Or;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerServiceClient customerServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final ProductServiceClient productServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @Override
    public OrderResponseDTO createOrder(OrderRequestDTO request) {
        log.info("Creating order for customerId: {}", request.getCustomerId());
        CustomerClientDTO customer = validateCustomer(
                request.getCustomerId(),
                request.getPaymentMode()
        );
        log.info("Customer validated: {}", customer.getName());

        Map<UUID, ProductClientDTO> productMap = fetchAndValidateProducts(
                request.getOrderItems()
        );
        log.info("All {} products validated", productMap.size());
        checkStockAvailability(request.getOrderItems());
        log.info("Stock availability confirmed for all items");

        BigDecimal totalAmount = calculateTotalAmount(
                request.getOrderItems(),
                productMap
        );

        log.info("Total order amount calculated: {}", totalAmount);

        String orderNumber = generateOrderNumber();
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerId(request.getCustomerId())
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .paymentMode(request.getPaymentMode())
                .orderDate(LocalDateTime.now())
                .notes(request.getNotes())
                .build();

        for(OrderItemRequestDTO itemRequest: request.getOrderItems()) {
            ProductClientDTO product = productMap.get(itemRequest.getProductId());
            BigDecimal subTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(subTotal)
                    .build();

            order.addOrderItem(orderItem);
        }

        Order savedOrder = orderRepository.saveAndFlush(order);
        log.info("Order saved with orderNumber: {}", savedOrder.getOrderNumber());

        reduceStockWithCompensation(request.getOrderItems(), savedOrder);

        savedOrder.setStatus(OrderStatus.CONFIRMED);
        Order confirmedOrder = orderRepository.saveAndFlush(savedOrder);
        log.info("Order confirmed: {}", confirmedOrder.getOrderNumber());

        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(confirmedOrder.getId())
                .orderNumber(confirmedOrder.getOrderNumber())
                .customerId(confirmedOrder.getCustomerId())
                .customerName(customer.getName())
                .customerEmail(customer.getEmail())
                .totalAmount(confirmedOrder.getTotalAmount())
                .placedAt(confirmedOrder.getOrderDate())
                .build();

        kafkaTemplate.send("order-placed", confirmedOrder.getCustomerId().toString(), event);
        log.info("Published OrderPlacedEvent for order {}", confirmedOrder.getOrderNumber());

        return mapToResponseDTO(confirmedOrder);
    }


    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(UUID id) {
        log.debug("Fetching order with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + id
                ));

        return mapToResponseDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderByOrderNumber(String orderNumber) {
        log.debug("Fetching order with orderNumber: {}", orderNumber);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with order number: " + orderNumber
                ));

        return mapToResponseDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(Pageable pageable) {
        log.debug("Fetching all orders with pagination: {}", pageable);
        return orderRepository.findAll(pageable)
                .map(this::mapToResponseDTO);

    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrdersByCustomerId(UUID customerId, Pageable pageable) {
        log.info("Fetching orders for customerId: {}", customerId);

        return orderRepository.findByCustomerId(customerId, pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        log.info("Fetching orders with status: {}", status);

        return orderRepository.findByStatus(status, pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    public OrderResponseDTO updateOrderStatus(UUID id, OrderStatusUpdateDTO statusUpdate) {
        log.info("Updating status for orderId: {} to {}", id, statusUpdate.getStatus());

        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id: " + id)
                );
        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = statusUpdate.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.saveAndFlush(order);

        log.info("Order {} status updated from {} to {}",
                order.getOrderNumber(), currentStatus, newStatus);

        return mapToResponseDTO(updatedOrder);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {

        if (currentStatus == newStatus) {
            throw new BadRequestException("Order is already in " + currentStatus + " status");
        }

        if (currentStatus == OrderStatus.DELIVERED) {
            throw new BadRequestException(
                    "Cannot update status of a DELIVERED order"
            );
        }

        if (currentStatus == OrderStatus.CANCELLED) {
            throw new BadRequestException(
                    "Cannot update status of a CANCELLED order"
            );
        }

        switch (currentStatus) {
            case PENDING ->
            {
                if (newStatus != OrderStatus.CANCELLED && newStatus != OrderStatus.CONFIRMED) {
                    throw new BadRequestException(
                            "Invalid transition from PENDING to " + newStatus
                    );
                }
            }

            case CONFIRMED -> {
                if (newStatus != OrderStatus.DELIVERED && newStatus != OrderStatus.CANCELLED) {
                    throw new BadRequestException(
                            "Invalid transition from CONFIRMED to " + newStatus
                    );
                }

            }

            default -> {
                throw new BadRequestException(
                        "Invalid status transition from " + currentStatus + " to " + newStatus
                );
            }

        }
    }

    @Override
    public void cancelOrder(UUID id) {
        log.info("Cancelling order with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + id
                ));

        validateOrderCancellable(order);

        restoreStockForOrder(order);

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.saveAndFlush(order);
        log.info("Order cancelled: {}", order.getOrderNumber());

        ApiResponseWrapper<CustomerClientDTO> customerResp =
                customerServiceClient.getCustomerById(id);

        CustomerClientDTO customer = customerResp != null ? customerResp.getData(): null;

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .customerName(customer != null ? customer.getName() : null)
                .customerEmail(customer != null ? customer.getEmail() : null)
                .reason("Customer requested cancellation")                              // adapt to your param name
                .cancelledAt(java.time.LocalDateTime.now())
                .build();

        kafkaTemplate.send("order-cancelled", order.getCustomerId().toString(), event);
        log.info("Published OrderCancelledEvent for order {}", order.getOrderNumber());


    }

    private void validateOrderCancellable(Order order) {
        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == OrderStatus.CANCELLED) {
            throw new BadRequestException(
                    "Order is already cancelled: " + order.getOrderNumber()
            );
        }

        if (currentStatus == OrderStatus.DELIVERED) {
            throw new BadRequestException(
                    "Cannot cancel a delivered order: " + order.getOrderNumber()
                            + ". Please initiate a return instead."
            );
        }

        // Only PENDING or CONFIRMED orders can be cancelled
    }

    private void restoreStockForOrder(Order order) {
        List<OrderItem> items = order.getOrderItems();
        List<OrderItem> successfullyRestored = new ArrayList<>();

        try {
            for (OrderItem item : items) {
                Map<String, Integer> request = Map.of("quantity", item.getQuantity());
                inventoryServiceClient.addStock(item.getProductId(), request);
                successfullyRestored.add(item);
                log.info("Stock restored for productId: {}, quantity: {}",
                        item.getProductId(), item.getQuantity());
            }
        } catch (Exception ex) {
            log.error("Failed to restore stock during order cancellation for orderId: {}",
                    order.getId(), ex);
            throw new ServiceUnavailableException(
                    "Failed to restore stock during cancellation. "
                            + "Restored: " + successfullyRestored.size() + "/" + items.size()
                            + ". Reason: " + ex.getMessage()
            );
        }
    }


    private CustomerClientDTO validateCustomer(UUID customerId, PaymentMode paymentMode) {
        CustomerClientDTO customer;
        try {
            ApiResponseWrapper<CustomerClientDTO> response =
                    customerServiceClient.getCustomerById(customerId);

            customer = response.getData();
        } catch (FeignException.NotFound ex) {
            throw new BadRequestException("Customer not found with id: " + customerId);
        } catch (FeignException ex) {
            log.error("Customer Service call failed", ex);
            throw new ServiceUnavailableException("Customer Service is currently unavailable");
        }
        if (customer == null) {
            throw new BadRequestException("Customer data could not be retrieved");
        }

        if (!Boolean.TRUE.equals(customer.getActive())) {
            throw new BadRequestException("Customer is not active");
        }

        if (paymentMode == PaymentMode.CREDIT) {
            String type = customer.getCustomerType();
            if (!"WHOLESALE".equalsIgnoreCase(type) && !"WORKSHOP".equalsIgnoreCase(type)) {
                throw new BadRequestException("Credit card payments are not allowed for " + type + " customers");
            }

            if (customer.getCreditLimit() == null ||
                    customer.getCreditLimit().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Customer has no credit limit");
            }
        }

        return customer;
    }

    private Map<UUID, ProductClientDTO> fetchAndValidateProducts(List<OrderItemRequestDTO> items) {
        Map<UUID, ProductClientDTO> productMap = new HashMap<>();

        for(OrderItemRequestDTO item: items) {
            ProductClientDTO product;

            try {
                 ApiResponseWrapper<ProductClientDTO> response =
                         productServiceClient.getProductById(item.getProductId());

                 product = response.getData();

            } catch (FeignException.NotFound ex) {
                throw new BadRequestException("Product not found with id: " + item.getProductId());
            } catch (FeignException ex) {
                log.error("Product Service call failed for product: {}", item.getProductId(), ex);
                throw new ServiceUnavailableException("Product Service is currently unavailable");
            }

            if (product == null) {
                throw new BadRequestException("Product data not available for: " + item.getProductId());
            }

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new BadRequestException("Product is not available: " + product.getName());
            }

            productMap.put(item.getProductId(), product);


        }

        return productMap;
    }


    private void checkStockAvailability(List<OrderItemRequestDTO> items) {
        for (OrderItemRequestDTO item : items) {
            try {
                ApiResponseWrapper<Boolean> response = inventoryServiceClient.checkStockAvailability(
                        item.getProductId(),
                        item.getQuantity()
                );

                if (!Boolean.TRUE.equals(response.getData())) {
                    throw new BadRequestException(
                            "Insufficient stock for productId: " + item.getProductId()
                                    + ", requested: " + item.getQuantity()
                    );
                }


            } catch (FeignException.NotFound ex) {
                throw new BadRequestException(
                        "Inventory not found for productId: " + item.getProductId()
                );
            } catch (FeignException ex) {
                log.error("Inventory Service call failed", ex);
                throw new ServiceUnavailableException("Inventory Service is currently unavailable");
            }
        }
    }

    private BigDecimal calculateTotalAmount(List<OrderItemRequestDTO> items,
                                            Map<UUID, ProductClientDTO> productMap) {

        return items.stream()
                .map(
                        item -> {
                            ProductClientDTO product = productMap.get(item.getProductId());
                            return product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                        })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    private String generateOrderNumber() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String prefix = "ORD-" + year + "-";
        long count = orderRepository.countByOrderNumberStartingWith(prefix);
        return prefix + String.format("%05d", count+1);

    }


    private void reduceStockWithCompensation(List<OrderItemRequestDTO> items, Order savedOrder) {
        List<OrderItemRequestDTO> successfullyReduced = new ArrayList<>();

        try {
            for (OrderItemRequestDTO item : items) {
                Map<String, Integer> request = Map.of("quantity", item.getQuantity());
                inventoryServiceClient.reduceStock(item.getProductId(), request);
                successfullyReduced.add(item);
                log.info("Stock reduced for productId: {}, quantity: {}",
                        item.getProductId(), item.getQuantity());
            }
        } catch (Exception ex) {
            log.error("Stock reduction failed, initiating compensation", ex);
            rollbackStockReductions(successfullyReduced);

            // Cancel the saved order
            savedOrder.setStatus(OrderStatus.CANCELLED);
            orderRepository.saveAndFlush(savedOrder);

            throw new BadRequestException(
                    "Failed to reduce stock. Order cancelled. Reason: " + ex.getMessage()
            );
        }
    }

    private void rollbackStockReductions(List<OrderItemRequestDTO> successfullyReduced) {
        // Reverse order for rollback
        for (int i = successfullyReduced.size() - 1; i >= 0; i--) {
            OrderItemRequestDTO item = successfullyReduced.get(i);
            try {
                Map<String, Integer> request = Map.of("quantity", item.getQuantity());
                inventoryServiceClient.addStock(item.getProductId(), request);
                log.info("Compensation: restored stock for productId: {}", item.getProductId());
            } catch (Exception ex) {
                log.error("CRITICAL: Compensation failed for productId: {}. Manual intervention needed.",
                        item.getProductId(), ex);
                // In production: send alert, write to dead-letter queue
            }
        }
    }


    private OrderResponseDTO mapToResponseDTO(Order order) {
        List<OrderItemResponseDTO> itemDTOs = order.getOrderItems().stream()
                .map(this::mapItemToResponseDTO)
                .collect(Collectors.toList());

        return OrderResponseDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .orderItems(itemDTOs)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMode(order.getPaymentMode())
                .orderDate(order.getOrderDate())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponseDTO mapItemToResponseDTO(OrderItem item) {
        return OrderItemResponseDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }
}
