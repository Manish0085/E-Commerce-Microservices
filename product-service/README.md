# Product Service

Catalog service for the SparePartShop platform. Owns the master list of car filters, brake parts, clutch parts, and miscellaneous spares — their name, brand, price, vehicle compatibility, and part number.

**Port:** `8081`  ·  **Database:** `product_db` on MySQL `:3307`  ·  **Registers as:** `product-service`

---

## Responsibility

Product Service is the **source of truth for catalog data**. It does NOT track stock levels (that's `inventory-service`) and does NOT track prices applied to historical orders (the order stores a price snapshot at the moment of placement).

```
  ┌─────────────────────────────────────────────────────────┐
  │                  product-service                        │
  │                                                         │
  │  · CRUD on the Product master table                     │
  │  · Lookup by id / category / brand / partial name       │
  │  · Soft-delete via `active = false`                     │
  │  · Optimistic locking on updates (@Version)             │
  └─────────────────────────────────────────────────────────┘
                        ▲
                        │ Feign call: GET /{id}
                        │
                 ┌──────┴──────┐
                 │ order-service│   (during order placement, to snapshot
                 └──────────────┘    product name/price onto OrderItem)
```

---

## Entity

[`Product.java`](src/main/java/com/sparepartshop/product_service/entity/Product.java)

| Field                   | Type          | Notes                                            |
|-------------------------|---------------|--------------------------------------------------|
| `id`                    | UUID          | PK, generated                                    |
| `name`                  | String(150)   | indexed                                          |
| `description`           | String(500)   |                                                  |
| `brand`                 | String(100)   | indexed (Bosch, MANN, Mahle, …)                  |
| `category`              | enum          | `FILTER`, `BRAKE`, `CLUTCH`, `OTHER`             |
| `partNumber`            | String(100)   | **unique** — the OEM/aftermarket identifier      |
| `price`                 | BigDecimal    | 10,2 — INR                                       |
| `vehicleCompatibility`  | String(500)   | free text (e.g. "Swift 2015+, Baleno 2016+")     |
| `active`                | Boolean       | soft-delete flag                                 |
| `createdAt` / `updatedAt` | LocalDateTime | auto via `@CreationTimestamp`/`@UpdateTimestamp` |
| `version`               | Long          | optimistic lock                                  |

---

## API

Base path: `/api/v1/products` — see [`ApiPaths.java`](src/main/java/com/sparepartshop/product_service/constants/ApiPaths.java).

| Method | Path                            | Purpose                        |
|--------|---------------------------------|--------------------------------|
| POST   | `/api/v1/products`              | Create a product               |
| GET    | `/api/v1/products`              | List all active products       |
| GET    | `/api/v1/products/{id}`         | Get one                        |
| GET    | `/api/v1/products/category/{category}` | Filter by category     |
| GET    | `/api/v1/products/brand/{brand}`       | Filter by brand        |
| GET    | `/api/v1/products/search?name=...`     | Partial-name search    |
| PUT    | `/api/v1/products/{id}`         | Update                         |
| DELETE | `/api/v1/products/{id}`         | **Soft** delete (sets `active=false`) |

All routes are also reachable through the gateway on `:8080` using the same paths.

---

## Patterns used here

- **UUID primary keys** — globally unique, no leak of row-count in URLs.
- **Optimistic locking** (`@Version`) — two concurrent edits can't silently overwrite each other; the loser gets an `OptimisticLockException`.
- **Soft delete** — catalog rows are almost always referenced by historical orders, so deleting them would violate referential intent. `active=false` hides them from listings without breaking snapshots.
- **Snapshot handoff** — the service returns full product details; the caller (order-service) copies `name` and `price` onto the order row so later price edits don't rewrite history.
- **Constants-based API paths** — `ApiPaths` avoids magic strings sprinkled across controllers.
- **Global exception handler** — returns structured `ErrorResponse` JSON for validation and lookup failures.

---

## Configuration

[src/main/resources/application.yaml](src/main/resources/application.yaml):

```yaml
server:
  port: 8081
spring:
  application: { name: product-service }
  datasource:
    url: jdbc:mysql://localhost:3307/product_db
    username: root
    password: root
eureka:
  client: { service-url: { defaultZone: http://localhost:8761/eureka/ } }
  instance:
    hostname: localhost
    prefer-ip-address: false
```

The `prefer-ip-address: false` + explicit `hostname` is deliberate — on machines with VPN/corporate adapters, Spring otherwise picks a non-routable IP and the Gateway can't reach the instance.

---

## Running

```bash
# MySQL container (one-time)
docker start product-mysql    # created during initial setup

# service
./mvnw spring-boot:run
```

## Smoke test

```bash
curl -X POST http://localhost:8081/api/v1/products \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "Bosch Oil Filter F026407123",
        "brand": "Bosch",
        "category": "FILTER",
        "partNumber": "F026407123",
        "price": 499.00,
        "vehicleCompatibility": "Maruti Swift 2015+"
      }'

curl http://localhost:8081/api/v1/products
curl http://localhost:8080/api/v1/products   # same, via Gateway
```
