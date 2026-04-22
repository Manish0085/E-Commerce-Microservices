# Order Service

The **orchestration brain** of the platform. Placing an order isn't a single DB write — it's a coordinated sequence across four other services that must either all succeed or all roll back. That coordination lives here, implemented as a **Saga (orchestration flavor)** with explicit compensating actions.

**Port:** `8084`  ·  **Database:** `order_db` on MySQL `:3310`  ·  **Registers as:** `order-service`

---

## Responsibility

1. Accept a new order request (customerId + line items + payment mode).
2. Validate customer (Feign → `customer-service`).
3. For each line item: fetch product (Feign → `product-service`) and snapshot name/price.
4. For each line item: reserve stock (Feign → `inventory-service`).
5. Persist `Order` + `OrderItems` with status `PENDING`.
6. Trigger billing (Feign → `billing-service`) to generate an invoice.
7. On any failure, run compensations (release reserved stock, mark order `CANCELLED`).

---

## The Saga

```
                        ORDER PLACEMENT SAGA (happy path)

   Client  POST /api/v1/orders  {customerId, items[], paymentMode}
     │
     ▼
  ┌─────────────────────────────────────────────────────────────────┐
  │ 1. customer-service.getCustomer(customerId)      [Feign/validate]│
  │ 2. for each item:                                               │
  │       product-service.getProduct(productId)   → snapshot price  │
  │       inventory-service.reduce(productId, qty)                  │
  │ 3. Save Order (status=PENDING) + OrderItems                     │
  │ 4. billing-service.createInvoice(order)                         │
  │ 5. return OrderResponse                                         │
  └─────────────────────────────────────────────────────────────────┘

                       ORDER PLACEMENT SAGA (failure at step 4)

  ✗ billing-service is down
     │
     ▼
  COMPENSATION:
     · for each item already reduced → inventory.add(productId, qty)
     · Order row marked CANCELLED (or delete, depending on policy)
     · Return 5xx to client with the failing step
```

This is **orchestration** (not choreography): `order-service` explicitly calls each participant and drives the control flow. That's easier to reason about than event-based choreography for a small platform, at the cost of tighter coupling to participant APIs.

---

## Entities

[`Order.java`](src/main/java/com/sparepartshop/order_service/entity/Order.java) and [`OrderItem.java`](src/main/java/com/sparepartshop/order_service/entity/OrderItem.java).

### Order

| Field           | Type          | Notes                                       |
|-----------------|---------------|---------------------------------------------|
| `id`            | UUID          | PK                                          |
| `orderNumber`   | String(30)    | human-readable, unique (e.g. `ORD-20260422-0001`) |
| `customerId`    | UUID          | foreign reference (no FK — remote service)  |
| `orderItems`    | List<OrderItem> | one-to-many, cascade + orphan-removal     |
| `totalAmount`   | BigDecimal    | sum of line subtotals                       |
| `status`        | enum          | `PENDING` → `CONFIRMED` → `DELIVERED` / `CANCELLED` |
| `paymentMode`   | enum          | `CASH`, `UPI`, `CARD`, `CREDIT`, …          |
| `orderDate`     | LocalDateTime | when placed                                 |
| `notes`         | String(500)   | free text                                   |
| `version`       | Long          | optimistic lock                             |

### OrderItem  (the snapshot row)

| Field         | Type       | Notes                                                 |
|---------------|------------|-------------------------------------------------------|
| `id`          | UUID       | PK                                                    |
| `order`       | Order      | parent (lazy, ManyToOne)                              |
| `productId`   | UUID       | reference                                             |
| `productName` | String(150)| **snapshotted** at placement — never re-fetched later |
| `quantity`    | Integer    |                                                       |
| `unitPrice`   | BigDecimal | **snapshotted** — catalog price edits don't rewrite history |
| `subtotal`    | BigDecimal | `unitPrice × quantity`                                |

**Why snapshot?** Product.name and Product.price in `product-service` will change (rebranding, price revisions). An order from last month must still show the price the customer actually paid. That requires the order to own its copy, not live-fetch.

---

## State machine

```
  PENDING ──(payment succeeds)──▶ CONFIRMED ──(shipped/picked up)──▶ DELIVERED
     │                               │
     └──(customer cancels /          └──(force-cancel, rare)──▶ CANCELLED
        saga fails / timeout)
                                       
                                 ▶ CANCELLED
```

Illegal transitions (e.g. `DELIVERED → PENDING`) are rejected at the service layer, not assumed-impossible.

---

## API

Base path: `/api/v1/orders` — see [`ApiPaths.java`](src/main/java/com/sparepartshop/order_service/constants/ApiPaths.java).

| Method | Path                                   | Purpose                         |
|--------|----------------------------------------|---------------------------------|
| POST   | `/api/v1/orders`                       | Place a new order (runs saga)   |
| GET    | `/api/v1/orders`                       | List                            |
| GET    | `/api/v1/orders/{id}`                  | By UUID                         |
| GET    | `/api/v1/orders/number/{orderNumber}`  | By human-readable number        |
| GET    | `/api/v1/orders/customer/{customerId}` | Order history for a customer    |
| GET    | `/api/v1/orders/status/{status}`       | By status                       |
| PATCH  | `/api/v1/orders/{id}/status`           | Transition status               |
| POST   | `/api/v1/orders/{id}/cancel`           | Cancel (triggers stock release) |

---

## Feign clients

[`client/`](src/main/java/com/sparepartshop/order_service/client/):

| Client                     | Targets             | Purpose                                        |
|----------------------------|---------------------|------------------------------------------------|
| `CustomerServiceClient`    | `customer-service`  | validate `customerId`, read shipping address   |
| `ProductServiceClient`     | `product-service`   | fetch name + price for snapshot                |
| `InventoryServiceClient`   | `inventory-service` | `reduce` (step) and `add` (compensation)       |

Each client uses `lb://<service-name>` via Eureka — no hard-coded hosts.

---

## Patterns used here

- **Saga (orchestration)** — this service is the coordinator.
- **Compensating transactions** — `inventory.add` undoes `inventory.reduce` on failure.
- **Snapshot pattern** — `OrderItem` stores product name + price at placement time.
- **OpenFeign declarative clients** — interfaces + `@FeignClient` annotations; Spring generates the HTTP call code.
- **Synthetic human-readable ID** (`orderNumber`) alongside the UUID PK — UUIDs are for machines, `ORD-20260422-0001` is for customers on the phone.
- **One-to-many with cascade + orphanRemoval** — OrderItems live and die with their parent Order.
- **Optimistic locking** — status transitions can't clash.

---

## Configuration

[src/main/resources/application.yaml](src/main/resources/application.yaml) — port 8084, MySQL on 3310, Eureka on 8761.

---

## Smoke test

```bash
# End-to-end (assumes a customer and product with stock exist)
curl -X POST http://localhost:8084/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{
        "customerId": "<customer-uuid>",
        "paymentMode": "UPI",
        "items": [
          { "productId": "<product-uuid>", "quantity": 2 }
        ],
        "notes": "Deliver before 5pm"
      }'

curl http://localhost:8084/api/v1/orders
curl http://localhost:8080/api/v1/orders/customer/<customer-uuid>   # via gateway
```

Run `../test-e2e.sh` from the repo root for a scripted 20+ assertion walk through customer → product → inventory → order → invoice → payment.
