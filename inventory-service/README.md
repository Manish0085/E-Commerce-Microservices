# Inventory Service

Tracks how many of each product sit in the warehouse. Exposes operations for adding stock (restocking), reducing stock (order confirmation), and checking availability before committing an order. This is the service that **physically gates** whether an order can proceed.

**Port:** `8082`  ·  **Database:** `inventory_db` on MySQL `:3308`  ·  **Registers as:** `inventory-service`

---

## Responsibility

Stock-quantity is a **hot, contested resource**. Two customers hitting "Place Order" at the same time for the last unit must not both succeed. That invariant lives here, enforced by JPA's **optimistic locking** (`@Version`):

```
   ┌──────────────────────────────────────────────────────────────┐
   │                   inventory-service                          │
   │                                                              │
   │   One Inventory row per product (unique index on productId). │
   │   Reduce-stock = read → check → decrement → save.            │
   │   @Version increments every save; concurrent writers with    │
   │   stale version get OptimisticLockException → retry/fail.    │
   └──────────────────────────────────────────────────────────────┘
           ▲                                         ▲
           │ Feign calls from order-service:         │
           │   POST /product/{pid}/check             │
           │   POST /product/{pid}/reduce            │
           │   POST /product/{pid}/add  ← compensation on saga failure
```

### Concurrent-reduce timeline (why @Version matters)

```
  T0  Thread A reads row (stock=1, version=7)
  T1  Thread B reads row (stock=1, version=7)
  T2  Thread A writes    (stock=0, version=8)  ✅
  T3  Thread B writes    (stock=0, version=8)  ✖ stale version → rollback
```

---

## Entity

[`Inventory.java`](src/main/java/com/sparepartshop/inventory_service/entity/Inventory.java)

| Field                | Type         | Notes                                    |
|----------------------|--------------|------------------------------------------|
| `id`                 | UUID         | PK                                       |
| `productId`          | UUID         | **unique** — one stock row per product   |
| `stockQuantity`      | Integer      | current on-hand count                    |
| `reorderLevel`       | Integer      | threshold for low-stock alert (default 10) |
| `reorderQuantity`    | Integer      | suggested replenish qty (default 50)     |
| `warehouseLocation`  | String(100)  | indexed; e.g. "WH-DELHI-01"              |
| `unitOfMeasure`      | String(50)   | defaults to `"PIECE"`                    |
| `lastRestockedAt`    | LocalDateTime |                                         |
| `createdAt` / `updatedAt` | LocalDateTime | auto                                 |
| `version`            | Long         | optimistic lock                          |

---

## API

Base path: `/api/v1/inventory` — see [`ApiPaths.java`](src/main/java/com/sparepartshop/inventory_service/constants/ApiPaths.java).

| Method | Path                                           | Purpose                                     |
|--------|------------------------------------------------|---------------------------------------------|
| POST   | `/api/v1/inventory`                            | Create inventory row for a product          |
| GET    | `/api/v1/inventory`                            | List all                                    |
| GET    | `/api/v1/inventory/{id}`                       | By PK                                       |
| GET    | `/api/v1/inventory/product/{productId}`        | By productId (the common lookup)            |
| PUT    | `/api/v1/inventory/{id}`                       | Update metadata (reorder levels, location)  |
| DELETE | `/api/v1/inventory/{id}`                       | Remove                                      |
| POST   | `/api/v1/inventory/product/{productId}/add`    | Add stock (restock)                         |
| POST   | `/api/v1/inventory/product/{productId}/reduce` | Reduce stock (order confirmation)           |
| GET    | `/api/v1/inventory/low-stock`                  | Rows where `stockQuantity <= reorderLevel`  |
| GET    | `/api/v1/inventory/product/{productId}/check?quantity=N` | Availability check (no mutation) |

The `check` endpoint is the **pre-flight** that order-service calls before starting a saga; `reduce` and `add` are the saga step + compensation.

---

## Patterns used here

- **Optimistic locking** — primary defense against over-selling (detailed above).
- **Idempotent lookups** — `GET /product/{productId}` and `check` don't mutate, so retries are safe.
- **Compensating action** — `add` is the inverse of `reduce`; called by the order saga when a later step fails.
- **Unique constraint on `productId`** — the database rejects any attempt to create two inventory rows for the same product, independent of app-level checks.
- **Soft schema evolution via `ddl-auto: update`** — acceptable in dev; production should switch to Flyway/Liquibase.

---

## Configuration

[src/main/resources/application.yaml](src/main/resources/application.yaml) — port 8082, MySQL on 3308, Eureka on 8761.

---

## Smoke test

```bash
# Create a stock row for an existing product
curl -X POST http://localhost:8082/api/v1/inventory \
  -H 'Content-Type: application/json' \
  -d '{
        "productId": "<uuid-from-product-service>",
        "stockQuantity": 100,
        "reorderLevel": 10,
        "warehouseLocation": "WH-DELHI-01"
      }'

# Check availability (no mutation)
curl "http://localhost:8082/api/v1/inventory/product/<productId>/check?quantity=5"

# Reduce — should succeed
curl -X POST "http://localhost:8082/api/v1/inventory/product/<productId>/reduce?quantity=5"

# Low-stock alert
curl http://localhost:8082/api/v1/inventory/low-stock
```
