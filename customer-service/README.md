# Customer Service

Owns the customer directory — retail walk-ins, independent mechanics, workshops, and wholesale buyers. Exposes CRUD plus lookups (by phone, city, type) and is consulted by `order-service` (customer validation) and `billing-service` (billing snapshot).

**Port:** `8083`  ·  **Database:** `customer_db` on MySQL `:3309`  ·  **Registers as:** `customer-service`

---

## Responsibility

Customer types drive pricing and payment-term policies in the larger business, so they're first-class:

- `RETAIL` — walk-in end users, always prepaid
- `MECHANIC` — independent technicians, may get credit later
- `WORKSHOP` — garages buying in bulk for their jobs
- `WHOLESALE` — redistributors; often on net-30 credit

The entity carries `creditLimit` and `currentBalance` so downstream services can enforce credit policies, even though no service currently automates that check — it's a growth hook for the business.

```
           ┌────────────────────────────────────────────┐
           │             customer-service               │
           │                                            │
           │  Profile data · GST number · credit fields │
           │  Unique on phone AND email                 │
           └────────────┬───────────────────────────────┘
                        │ Feign (read)
           ┌────────────┼────────────────┐
           ▼                             ▼
   order-service                 billing-service
   validates customerId          snapshots name/address/GST
   before saga                   onto the Invoice
```

---

## Entity

[`Customer.java`](src/main/java/com/sparepartshop/customer_service/entity/Customer.java)

| Field              | Type         | Notes                                           |
|--------------------|--------------|-------------------------------------------------|
| `id`               | UUID         | PK                                              |
| `name`             | String(150)  |                                                 |
| `email`            | String(150)  | **unique** (nullable)                           |
| `phone`            | String(15)   | **unique**, required — the main lookup key      |
| `address`          | String(250)  |                                                 |
| `city`             | String(100)  | indexed                                         |
| `customerType`     | enum         | `RETAIL` / `WORKSHOP` / `MECHANIC` / `WHOLESALE` |
| `businessName`     | String(150)  | for workshop/wholesale                          |
| `gstNumber`        | String(20)   | stamped onto invoices for B2B customers         |
| `creditLimit`      | BigDecimal   | default 0                                       |
| `currentBalance`   | BigDecimal   | default 0                                       |
| `active`           | Boolean      | soft-delete flag                                |
| `createdAt` / `updatedAt` | LocalDateTime | auto                                    |
| `version`          | Long         | optimistic lock                                 |

---

## API

Base path: `/api/v1/customers` — see [`ApiPaths.java`](src/main/java/com/sparepartshop/customer_service/constants/ApiPaths.java).

| Method | Path                                 | Purpose                    |
|--------|--------------------------------------|----------------------------|
| POST   | `/api/v1/customers`                  | Create                     |
| GET    | `/api/v1/customers`                  | List all active            |
| GET    | `/api/v1/customers/{id}`             | By UUID                    |
| GET    | `/api/v1/customers/phone/{phone}`    | By phone (primary lookup)  |
| GET    | `/api/v1/customers/type/{type}`      | Filter by customer type    |
| GET    | `/api/v1/customers/city/{city}`      | Filter by city             |
| PUT    | `/api/v1/customers/{id}`             | Update                     |
| DELETE | `/api/v1/customers/{id}`             | Soft delete                |

---

## Patterns used here

- **Unique business keys** — DB-level `UNIQUE` on `phone` and `email`; app code treats duplicate-insert as a user error, not a 500.
- **MapStruct DTO ↔ Entity mapping** — keeps the controller contract decoupled from JPA internals.
- **Snapshot-source, not join-source** — when billing-service generates an Invoice, it *copies* the customer's name/address/GST onto the invoice row. If the customer later updates their address, historical invoices keep the address as-issued. That's a deliberate choice: invoices must be immutable once issued.
- **Soft delete** via `active` — same reason as Product: customer rows are referenced by historical orders/invoices.
- **Optimistic locking** — admin UIs editing the same customer can't silently overwrite each other.

---

## Configuration

[src/main/resources/application.yaml](src/main/resources/application.yaml) — port 8083, MySQL on 3309, Eureka on 8761.

---

## Smoke test

```bash
curl -X POST http://localhost:8083/api/v1/customers \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "Ramesh Auto Works",
        "phone": "9876543210",
        "email": "ramesh@autoworks.in",
        "address": "Shop 14, Karol Bagh",
        "city": "Delhi",
        "customerType": "WORKSHOP",
        "businessName": "Ramesh Auto Works",
        "gstNumber": "07ABCDE1234F1Z5",
        "creditLimit": 50000.00
      }'

curl http://localhost:8083/api/v1/customers/phone/9876543210
curl http://localhost:8083/api/v1/customers/type/WORKSHOP
curl http://localhost:8080/api/v1/customers   # via gateway
```
