# Billing Service

Generates and tracks **invoices** for confirmed orders. Owns the tax calculation (18% GST), outstanding-balance queries, and the payment-status lifecycle of every invoice. Called by `order-service` when an order is placed, and by `payment-service` when a payment clears.

**Port:** `8085`  ·  **Database:** `invoice_db` on MySQL `:3311`  ·  **Registers as:** `billing-service`

---

## Responsibility

A business rule carried by this service: **an invoice, once issued, is immutable as a financial document.** Amounts and line-level descriptions don't retroactively change. Only the *payment status* moves (`PENDING` → `PARTIAL` → `PAID`, or `VOIDED` as an explicit correction).

```
                            BILLING FLOW

  order-service ─ POST /api/v1/invoices ──▶ billing-service
                                                 │
                                                 ├─▶ customer-service.get (snapshot
                                                 │   name/phone onto invoice)
                                                 │
                                                 ├─▶ calculate subtotal (from order lines)
                                                 ├─▶ taxAmount = subtotal × 18%
                                                 ├─▶ grandTotal = subtotal + tax
                                                 ├─▶ dueAmount = grandTotal
                                                 │
                                                 └─▶ save Invoice  (status=PENDING)


  payment-service ─ POST /api/v1/invoices/{id}/pay ──▶ billing-service
                                                 │
                                                 ├─▶ paidAmount += amount
                                                 ├─▶ dueAmount  -= amount
                                                 ├─▶ status = PAID if dueAmount==0
                                                 │            else PARTIAL
                                                 │
                                                 └─▶ save (optimistic lock on @Version)
```

---

## Entity

[`Invoice.java`](src/main/java/com/sparepartshop/billing_service/entity/Invoice.java)

Grouped by purpose:

**Identity & links**
| Field           | Type   | Notes                                            |
|-----------------|--------|--------------------------------------------------|
| `id`            | UUID   | PK                                               |
| `invoiceNumber` | String | unique, human (e.g. `INV-20260422-0001`)         |
| `orderId`       | UUID   | **unique** — one invoice per order               |
| `orderNumber`   | String | snapshot of the order's human number             |

**Customer snapshot** (copied at invoice creation)
| `customerId`, `customerName`, `customerPhone`

**Money (all `BigDecimal`, never `double`)**
| Field           | Meaning                                       |
|-----------------|-----------------------------------------------|
| `subtotal`      | Sum of order line subtotals (pre-tax)         |
| `taxAmount`     | `subtotal × taxPercentage / 100`              |
| `taxPercentage` | 18.00 (GST) by default                        |
| `grandTotal`    | `subtotal + taxAmount`                        |
| `paidAmount`    | running total of cleared payments             |
| `dueAmount`     | `grandTotal − paidAmount`                     |

**Status & lifecycle**
| `paymentStatus` enum: `PENDING` / `PARTIAL` / `PAID` / `VOIDED`
| `paymentMode` enum (copied from order)
| `invoiceDate`, `dueDate`, `paidAt`

**Ops**
| `active` (soft-delete), `createdAt`, `updatedAt`, `version`

---

## Status machine

```
               record payment, dueAmount > 0
                        │
          ┌─────────────▼─────────────┐
  PENDING ───────────▶ PARTIAL ────▶ PAID
     │                        dueAmount == 0
     │
     └───▶ VOIDED   (explicit admin correction; freezes the invoice)
```

Transitions happen only via the service layer — controllers never touch the state directly.

---

## API

Base path: `/api/v1/invoices` — see [`ApiPaths.java`](src/main/java/com/sparepartshop/billing_service/constants/ApiPaths.java).

| Method | Path                                             | Purpose                           |
|--------|--------------------------------------------------|-----------------------------------|
| POST   | `/api/v1/invoices`                               | Generate invoice for an order     |
| GET    | `/api/v1/invoices`                               | List                              |
| GET    | `/api/v1/invoices/{id}`                          | By UUID                           |
| GET    | `/api/v1/invoices/number/{invoiceNumber}`        | By human number                   |
| GET    | `/api/v1/invoices/order/{orderId}`               | Lookup from order id              |
| GET    | `/api/v1/invoices/customer/{customerId}`         | Customer's invoice history        |
| GET    | `/api/v1/invoices/status/{status}`               | Filter by status                  |
| POST   | `/api/v1/invoices/{id}/pay`                      | Record a payment (from payment-svc) |
| POST   | `/api/v1/invoices/{id}/void`                     | Void the invoice                  |
| GET    | `/api/v1/invoices/customer/{customerId}/outstanding`        | Unpaid invoices |
| GET    | `/api/v1/invoices/customer/{customerId}/outstanding-amount` | Sum due   |

---

## Patterns used here

- **Snapshot pattern** — customer name/phone are copied onto the invoice at creation. If the customer later changes their phone, the invoice keeps the number that was on the printed copy.
- **Tax configuration** — tax percentage stored on each row so historical rate changes don't rewrite old invoices.
- **Money = `BigDecimal` with scale 2** — never `double` / `float`. Rounding is explicit at every step.
- **Unique index on `orderId`** — DB guarantees one invoice per order. A duplicate create raises a constraint violation the service translates to 409.
- **State machine enforcement** — the service layer rejects illegal transitions (e.g. can't pay a `VOIDED` invoice).
- **Optimistic locking** — two concurrent payment callbacks to the same invoice can't double-count.
- **Feign client** — only consumes `customer-service` (for the snapshot); billing does NOT call order-service, because the caller (order-service) supplies the order details in the create request. That keeps dependency direction clean.

---

## Configuration

[src/main/resources/application.yaml](src/main/resources/application.yaml) — port 8085, MySQL on 3311, Eureka on 8761.

---

## Smoke test

```bash
# 1. Generate invoice for an existing order
curl -X POST http://localhost:8085/api/v1/invoices \
  -H 'Content-Type: application/json' \
  -d '{
        "orderId": "<order-uuid>",
        "paymentMode": "UPI"
      }'

# 2. Record a payment (normally fired by payment-service)
curl -X POST "http://localhost:8085/api/v1/invoices/<invoice-id>/pay?amount=1180.00"

# 3. Outstanding balance for a customer
curl http://localhost:8085/api/v1/invoices/customer/<customer-uuid>/outstanding-amount

# Via gateway
curl http://localhost:8080/api/v1/invoices/order/<order-uuid>
```
