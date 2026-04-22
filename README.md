# SparePartShop — Microservices Architecture

> A production-style microservices system for a car spare parts business, demonstrating enterprise-grade patterns including Saga orchestration, API Gateway, Service Discovery, and distributed transaction handling.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Services](#services)
- [Tech Stack](#tech-stack)
- [Patterns Implemented](#patterns-implemented)
- [Request Flow Diagrams](#request-flow-diagrams)
- [Quick Start](#quick-start)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Future Enhancements](#future-enhancements)

---

## Overview

SparePartShop is a fully functional microservices e-commerce backend built with Spring Boot 3.5 and Spring Cloud 2025.0. It implements the complete business flow of a spare parts retail operation: product catalog management, inventory tracking, customer management, order processing with distributed transactions, invoice generation, and payment processing with mock gateway integration.

The system demonstrates production-grade architectural patterns including Saga orchestration for distributed transactions, API Gateway for unified entry point, Eureka-based service discovery, optimistic locking for concurrency control, and idempotent webhook processing.

---

## Architecture

### High-Level System Architecture

```
                              ┌─────────────────────────┐
                              │      EXTERNAL CLIENT     │
                              │   (Mobile / Web / cURL)  │
                              └──────────┬──────────────┘
                                         │
                                         │ HTTP / HTTPS
                                         ▼
                              ┌─────────────────────────┐
                              │      API GATEWAY         │
                              │    (Spring Cloud GW)     │
                              │         :8080            │
                              │                          │
                              │  • Route Matching        │
                              │  • Load Balancing        │
                              │  • Service Discovery     │
                              │  • Error Handling        │
                              │  • Timeout Management    │
                              └──────────┬──────────────┘
                                         │
                                         │ lb://service-name
                                         ▼
                              ┌─────────────────────────┐
                              │     EUREKA SERVER        │
                              │   (Service Registry)     │
                              │         :8761            │
                              │                          │
                              │  Registers all services  │
                              │  Health-checks them      │
                              │  Returns their locations │
                              └──────────┬──────────────┘
                                         │
              ┌──────────────┬───────────┼───────────┬───────────────┐
              │              │           │           │               │
              ▼              ▼           ▼           ▼               ▼
       ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
       │ Product  │  │Inventory │  │ Customer │  │  Order   │  │ Billing  │
       │ Service  │  │ Service  │  │ Service  │  │ Service  │  │ Service  │
       │  :8081   │  │  :8082   │  │  :8083   │  │  :8084   │  │  :8085   │
       └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘
            │             │             │             │             │
         [MySQL]       [MySQL]       [MySQL]       [MySQL]       [MySQL]
         3307          3308          3309          3310          3311
                                                   │             │
                                      ┌────────────┘             │
                                      │                          │
                                      ▼                          │
                               Cross-service                     │
                               Feign calls                       │
                                      │                          │
                                      └──────┐        ┌──────────┘
                                             ▼        ▼
                                      ┌─────────────────────┐
                                      │  Payment Service     │
                                      │      :8086           │
                                      │                      │
                                      │  • Mock Gateway      │
                                      │  • Async Webhooks    │
                                      └─────────┬───────────┘
                                                │
                                           [MySQL]
                                            3312
```

### Service Communication Pattern

```
   USER REQUEST FLOW (Order Placement):
   ═══════════════════════════════════════════════════

   Client                    Gateway              Order Service         Other Services
     │                          │                      │                      │
     │ POST /api/v1/orders       │                      │                      │
     │──────────────────────────▶│                      │                      │
     │                          │                      │                      │
     │                          │ Forward to            │                      │
     │                          │ order-service         │                      │
     │                          │─────────────────────▶│                      │
     │                          │                      │                      │
     │                          │                      │ 1. Get Customer     │
     │                          │                      │─────────────────────▶│ Customer
     │                          │                      │◀─────────────────────│
     │                          │                      │                      │
     │                          │                      │ 2. Get Products     │
     │                          │                      │─────────────────────▶│ Product
     │                          │                      │◀─────────────────────│
     │                          │                      │                      │
     │                          │                      │ 3. Check Stock      │
     │                          │                      │─────────────────────▶│ Inventory
     │                          │                      │◀─────────────────────│
     │                          │                      │                      │
     │                          │                      │ 4. Save Order (PENDING)
     │                          │                      │                      │
     │                          │                      │ 5. Reduce Stock     │
     │                          │                      │─────────────────────▶│ Inventory
     │                          │                      │◀─────────────────────│
     │                          │                      │                      │
     │                          │                      │ 6. Mark CONFIRMED   │
     │                          │                      │                      │
     │                          │◀─────────────────────│                      │
     │◀─────────────────────────│                      │                      │
     │                          │                      │                      │
```

---

## Services

| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| **Eureka Server** | 8761 | — | Service discovery and registration |
| **API Gateway** | 8080 | — | Single entry point, routing, load balancing |
| **Product Service** | 8081 | MySQL :3307 | Product catalog (filters, brake parts, clutch) |
| **Inventory Service** | 8082 | MySQL :3308 | Stock management with optimistic locking |
| **Customer Service** | 8083 | MySQL :3309 | Customer CRUD with credit management |
| **Order Service** | 8084 | MySQL :3310 | Order processing with Saga orchestration |
| **Billing Service** | 8085 | MySQL :3311 | Invoice generation with tax calculation |
| **Payment Service** | 8086 | MySQL :3312 | Payment processing with mock gateway |

---

## Tech Stack

### Core Frameworks

- **Java** 17
- **Spring Boot** 3.5.13
- **Spring Cloud** 2025.0.2

### Libraries

- **Spring Cloud Gateway MVC** — API Gateway
- **Spring Cloud Netflix Eureka** — Service Discovery
- **Spring Cloud OpenFeign** — Declarative REST clients
- **Spring Data JPA** — Database abstraction
- **Spring Validation** — Input validation
- **Lombok** — Boilerplate reduction
- **MapStruct** 1.6.3 — DTO-Entity mapping

### Infrastructure

- **MySQL** 8.0 — Per-service relational storage
- **Docker** — Container orchestration for databases

### Build & Automation

- **Maven** (mvnw wrapper)
- **Bash scripts** for startup/shutdown/testing

---

## Patterns Implemented

### Architectural Patterns

| Pattern | Where |
|---------|-------|
| **Service Discovery** | Netflix Eureka (all services) |
| **API Gateway** | Single entry point pattern |
| **Database Per Service** | Each service has own MySQL |
| **Polyglot Persistence Ready** | Pattern supports different DBs |

### Integration Patterns

| Pattern | Where |
|---------|-------|
| **Declarative REST Client** | OpenFeign in Order, Payment, Billing |
| **Strategy Pattern** | Payment Gateway (swappable: Mock/Razorpay/Stripe) |
| **Mock Pattern** | Mock Payment Gateway for testing |
| **Async Processing** | Webhook simulator with `@Async` |

### Transaction Patterns

| Pattern | Where |
|---------|-------|
| **Saga (Orchestration)** | Order Service coordinates 3 other services |
| **Compensating Transactions** | Stock rollback on order failure |
| **Optimistic Locking** | `@Version` on Inventory, Payment, Order |
| **Snapshot Pattern** | Product name/price captured in Order Items |
| **Idempotency Keys** | Payment Service (duplicate charge prevention) |

### Data Patterns

| Pattern | Where |
|---------|-------|
| **DTO Separation** | Request/Response DTOs distinct from entities |
| **MapStruct Mapping** | Clean entity-DTO conversion |
| **UUID Primary Keys** | All entities use UUID |
| **Audit Fields** | `@CreationTimestamp`, `@UpdateTimestamp` |
| **Soft Delete** | `active` flag (where applicable) |

### Resilience Patterns

| Pattern | Where |
|---------|-------|
| **Timeout Configuration** | Gateway HTTP client |
| **Global Exception Handling** | All services |
| **Structured Error Responses** | Consistent ErrorResponse format |
| **State Machine** | Order status, Invoice payment status |

---

## Request Flow Diagrams

### Saga Pattern — Order Creation

```
   SUCCESSFUL SAGA FLOW:
   ═══════════════════════════════════════════════

   Step 1: Validate customer        ┌─────────┐
                                    │ Customer│
                                    │ Service │
                                    └─────────┘

   Step 2: Fetch product details    ┌─────────┐
   (get prices, verify active)      │ Product │
                                    │ Service │
                                    └─────────┘

   Step 3: Check stock availability ┌──────────┐
                                    │Inventory │
                                    │ Service  │
                                    └──────────┘

   Step 4: Calculate total amount

   Step 5: Generate order number
           ORD-YYYY-NNNNN

   Step 6: Save order (PENDING)     [DB]

   Step 7: Reduce stock (loop)      ┌──────────┐
                                    │Inventory │
                                    │ Service  │
                                    └──────────┘

   Step 8: Update to CONFIRMED      [DB]

   ═══════════════════════════════════════════════
   FAILURE SAGA (Step 7 fails mid-loop):
   ═══════════════════════════════════════════════

   Item 1: Stock reduced ✓
   Item 2: Stock reduced ✓
   Item 3: FAILED ❌

   Rollback triggered:
   Item 2: Stock restored ↺
   Item 1: Stock restored ↺
   Order: Marked CANCELLED
```

### Payment Flow with Mock Gateway

```
   ASYNCHRONOUS PAYMENT FLOW:
   ═══════════════════════════════════════════════

   Client                Payment Service          Mock Gateway
     │                        │                        │
     │ POST /initiate         │                        │
     │───────────────────────▶│                        │
     │                        │                        │
     │                        │ Save (INITIATED)       │
     │                        │                        │
     │                        │ charge()               │
     │                        │───────────────────────▶│
     │                        │                        │
     │                        │ Returns txn ID         │
     │                        │◀───────────────────────│
     │                        │                        │
     │                        │ Update (PROCESSING)    │
     │                        │                        │
     │◀───────────────────────│                        │
     │ 201 Created            │                        │
     │                        │                        │
     │                        │       (3 sec delay)    │
     │                        │                        │
     │                        │  POST /webhook         │
     │                        │◀───────────────────────│
     │                        │                        │
     │                        │ Update (SUCCESS)       │
     │                        │                        │
     │                        │ Notify Billing         │
     │                        │ (Feign call)           │
     │                        │─────────────┐          │
     │                        │             │          │
     │                        │◀────────────┘          │
```

---

## Quick Start

### Prerequisites

- **Java 17+** (verify: `java -version`)
- **Maven 3.8+** (or use included `mvnw` wrapper)
- **Docker 20+** (for MySQL containers)
- **curl** and **jq** (for testing)

### One-Time Setup

```bash
# Clone the repository
git clone <your-repo-url>
cd Microservices

# Install jq (required for test script)
sudo apt install jq

# Make scripts executable
chmod +x start-all.sh stop-all.sh status.sh test-e2e.sh

# Create MySQL containers (one time)
docker run -d --name product-mysql   -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=product_db   -p 3307:3306 mysql:8.0
docker run -d --name inventory-mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=inventory_db -p 3308:3306 mysql:8.0
docker run -d --name customer-mysql  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=customer_db  -p 3309:3306 mysql:8.0
docker run -d --name order-mysql     -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=order_db     -p 3310:3306 mysql:8.0
docker run -d --name billing-mysql   -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=billing_db   -p 3311:3306 mysql:8.0
docker run -d --name payment-mysql   -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=payment_db   -p 3312:3306 mysql:8.0
```

### Running the Project

```bash
# Start all services
./start-all.sh

# Check status (wait until everything is UP)
./status.sh

# Run automated end-to-end tests
./test-e2e.sh

# When done
./stop-all.sh
```

### Manual Verification

```bash
# Eureka dashboard
open http://localhost:8761

# Test via Gateway
curl http://localhost:8080/api/v1/products

# Gateway health
curl http://localhost:8080/actuator/health
```

---

## API Endpoints

All endpoints accessible via Gateway at `http://localhost:8080`:

### Product Service

```
POST   /api/v1/products                      — Create product
GET    /api/v1/products                      — Get all (paginated)
GET    /api/v1/products/{id}                 — Get by UUID
GET    /api/v1/products/category/{category}  — Filter by category
GET    /api/v1/products/brand/{brand}        — Filter by brand
GET    /api/v1/products/search?name=xxx      — Search by name
PUT    /api/v1/products/{id}                 — Update
DELETE /api/v1/products/{id}                 — Soft delete
```

### Inventory Service

```
POST   /api/v1/inventory                            — Create inventory
GET    /api/v1/inventory                            — Get all
GET    /api/v1/inventory/{id}                       — Get by ID
GET    /api/v1/inventory/product/{productId}        — Get by product
POST   /api/v1/inventory/product/{productId}/add    — Add stock
POST   /api/v1/inventory/product/{productId}/reduce — Reduce stock
GET    /api/v1/inventory/product/{id}/check?quantity=N — Stock check
GET    /api/v1/inventory/low-stock                  — Low stock items
```

### Customer Service

```
POST   /api/v1/customers                     — Create customer
GET    /api/v1/customers                     — Get all (paginated)
GET    /api/v1/customers/{id}                — Get by UUID
GET    /api/v1/customers/phone/{phone}       — Get by phone
GET    /api/v1/customers/type/{type}         — Filter (RETAIL/WHOLESALE/etc.)
GET    /api/v1/customers/city/{city}         — Filter by city
PUT    /api/v1/customers/{id}                — Update
DELETE /api/v1/customers/{id}                — Soft delete
```

### Order Service

```
POST   /api/v1/orders                          — Create order (Saga!)
GET    /api/v1/orders                          — Get all (paginated)
GET    /api/v1/orders/{id}                     — Get by ID
GET    /api/v1/orders/number/{orderNumber}     — Get by number
GET    /api/v1/orders/customer/{customerId}    — Customer's orders
GET    /api/v1/orders/status/{status}          — Filter by status
PATCH  /api/v1/orders/{id}/status              — Update status
POST   /api/v1/orders/{id}/cancel              — Cancel (reverse saga)
```

### Billing Service

```
POST   /api/v1/invoices                              — Create invoice
GET    /api/v1/invoices                              — Get all (paginated)
GET    /api/v1/invoices/{id}                         — Get by ID
GET    /api/v1/invoices/number/{invoiceNumber}       — Get by number
GET    /api/v1/invoices/order/{orderId}              — Invoice for order
GET    /api/v1/invoices/customer/{customerId}        — Customer's invoices
POST   /api/v1/invoices/{id}/pay                     — Record payment
POST   /api/v1/invoices/{id}/void                    — Void invoice
```

### Payment Service

```
POST   /api/v1/payments/initiate                      — Start payment
GET    /api/v1/payments/{id}                          — Get by ID
GET    /api/v1/payments/reference/{paymentRef}        — Get by reference
GET    /api/v1/payments/invoice/{invoiceId}           — Payments for invoice
GET    /api/v1/payments/customer/{customerId}         — Customer's payments
GET    /api/v1/payments/status/{status}               — Filter by status
POST   /api/v1/payments/webhook                       — Gateway callback
POST   /api/v1/payments/{id}/refund                   — Refund payment
```

---

## Testing

### Automated End-to-End Testing

The `test-e2e.sh` script runs 20+ assertions covering:

- Service health checks
- Product CRUD
- Customer management
- Inventory operations
- Order creation (full Saga flow)
- Stock reduction verification
- Invoice generation with tax
- Payment recording
- Order cancellation (reverse saga)

```bash
./test-e2e.sh
```

Expected output: All tests pass with green checkmarks.

### Manual Testing Example

```bash
# 1. Create a product
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Oil Filter Premium",
    "brand": "Bosch",
    "category": "FILTER",
    "partNumber": "BOS-OF-001",
    "price": 250.00
  }'

# 2. Create a customer
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Rajesh Kumar",
    "phone": "9876543210",
    "city": "Delhi",
    "customerType": "RETAIL"
  }'

# 3. Create inventory
curl -X POST http://localhost:8080/api/v1/inventory \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "<product-uuid>",
    "stockQuantity": 100,
    "reorderLevel": 10,
    "reorderQuantity": 50,
    "unitOfMeasure": "PIECE"
  }'

# 4. Place an order (triggers Saga)
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "<customer-uuid>",
    "paymentMode": "CASH",
    "orderItems": [
      {"productId": "<product-uuid>", "quantity": 5}
    ]
  }'
```

---

## Project Structure

```
Microservices/
│
├── eureka-server/              # Service discovery server
├── api-gateway/                # Single entry point
├── product-service/            # Product catalog
├── inventory-service/          # Stock management
├── customer-service/           # Customer CRUD
├── order-service/              # Order processing (Saga)
├── billing-service/            # Invoicing
├── payment-service/            # Payment processing
│
├── logs/                       # Service logs (auto-created)
│
├── start-all.sh                # Start all services
├── stop-all.sh                 # Stop all services
├── status.sh                   # Check service status
├── test-e2e.sh                 # End-to-end tests
│
├── Microservices_Complete_Notes.pdf   # Concept documentation
├── Microservices_Complete_Notes.md
│
└── README.md                   # This file
```

Each service follows a consistent structure:

```
<service>/
├── pom.xml
├── src/main/java/.../
│   ├── <Service>Application.java
│   ├── entity/                 # JPA entities
│   ├── enums/                  # Status/type enums
│   ├── dto/                    # Request/response DTOs
│   │   └── client/             # External service DTOs
│   ├── exception/              # Custom exceptions
│   ├── constants/              # API path constants
│   ├── client/                 # Feign clients
│   ├── repository/             # JPA repositories
│   ├── mapper/                 # MapStruct mappers
│   ├── service/                # Business logic
│   │   └── impl/
│   └── controller/             # REST endpoints
│
└── src/main/resources/
    └── application.yml
```

---

## Key Implementation Highlights

### Saga Pattern Implementation

Located in `order-service/src/main/java/.../service/impl/OrderServiceImpl.java`:

```java
@Override
public OrderResponseDTO createOrder(OrderRequestDTO request) {
    // 1. Validate customer via Feign
    CustomerClientDTO customer = validateCustomer(...);

    // 2. Validate products via Feign
    Map<UUID, ProductClientDTO> products = fetchAndValidateProducts(...);

    // 3. Check stock availability via Feign
    checkStockAvailability(...);

    // 4. Calculate total
    BigDecimal total = calculateTotalAmount(...);

    // 5. Save order (PENDING)
    Order order = buildOrder(...);
    Order saved = orderRepository.saveAndFlush(order);

    // 6. Reduce stock with compensation on failure
    reduceStockWithCompensation(request.getOrderItems(), saved);

    // 7. Update to CONFIRMED
    saved.setStatus(OrderStatus.CONFIRMED);
    return mapper.toResponseDTO(orderRepository.saveAndFlush(saved));
}
```

### Strategy Pattern for Payment Gateway

Pluggable gateway implementations:

```java
public interface PaymentGateway {
    GatewayChargeResponse charge(GatewayChargeRequest request);
    GatewayRefundResponse refund(String transactionId, BigDecimal amount);
    GatewayProvider getProvider();
}

@Service public class MockPaymentGateway implements PaymentGateway { ... }
// Future: RazorpayGateway, StripeGateway
```

---

## Future Enhancements

- [ ] **Circuit Breaker** (Resilience4j) — Prevent cascading failures
- [ ] **Distributed Tracing** (Zipkin) — Request tracking across services
- [ ] **Centralized Logging** (ELK Stack) — Unified log aggregation
- [ ] **Config Server** — Externalized configuration
- [ ] **Kafka Integration** — Event-driven architecture
- [ ] **Outbox Pattern** — Reliable event publishing
- [ ] **JWT Authentication** — Security layer at Gateway
- [ ] **Rate Limiting** — Per-user API throttling
- [ ] **AI Chat Service** — RAG-based customer support
- [ ] **Kubernetes Deployment** — Container orchestration
- [ ] **CI/CD Pipeline** — GitHub Actions

---

## Documentation

- [Complete Microservices Notes (PDF)](./Microservices_Complete_Notes.pdf) — Comprehensive concepts
- Each service has its own README with specific details

---

## Learning Outcomes

This project demonstrates hands-on experience with:

- Production-grade microservices architecture
- Distributed transaction management (Saga)
- Service-to-service communication (synchronous)
- Database-per-service pattern
- API gateway pattern
- Resilience patterns (timeout, error handling)
- Domain-Driven Design (bounded contexts)
- Mock gateway implementation for external integrations
- Comprehensive automated testing

---

## License

Educational project for learning microservices architecture.

---

## Author

Built as a learning project to master microservices patterns, distributed systems concepts, and Spring Cloud ecosystem.
