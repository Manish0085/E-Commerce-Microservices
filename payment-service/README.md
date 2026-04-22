# Payment Service

Handles the money flow: the customer's payment attempt, the call out to a payment gateway (mocked in dev, Razorpay/Stripe in prod), and the async webhook confirmation that marks the invoice paid. Designed so that **duplicate requests never charge twice** (idempotency keys) and **gateway latency never blocks the API** (async webhook simulation).

**Port:** `8086`  ·  **Database:** `payment_db` on MySQL `:3312`  ·  **Registers as:** `payment-service`

---

## Responsibility

Payments are the riskiest part of the platform — a bug here charges real customers twice. This service exists as a standalone module so those risks (idempotency, retries, gateway pluggability, webhook handling) stay isolated.

```
                           PAYMENT FLOW

  Client ─ POST /api/v1/payments/initiate ──▶ payment-service
                                                    │
                                                    │  ① Persist Payment row
                                                    │     (status=INITIATED,
                                                    │      idempotencyKey unique)
                                                    │
                                                    │  ② PaymentGateway.charge()
                                                    │     (Strategy: Mock / Razorpay /
                                                    │      Stripe — selected by
                                                    │      gatewayProvider field)
                                                    │
                                                    │  ③ Status = PROCESSING
                                                    │
                                                    ▼
                                           Return 202 + paymentReference
                                                    │
                                                    │  (seconds later, asynchronously)
                                                    │
                                  ┌─────────────────▼──────────────────┐
                                  │ WebhookSimulator  (@Async thread) │
                                  │  · wait N ms                       │
                                  │  · flip a coin using successRate%  │
                                  │  · POST /api/v1/payments/webhook   │
                                  │    with gatewayTxnId + outcome     │
                                  └─────────────────┬──────────────────┘
                                                    │
                                                    ▼
                                     Status = SUCCESS (or FAILED)
                                                    │
                                                    ▼
                           Feign call → billing-service.payInvoice(amount)
```

### Idempotency

Every `initiate` call requires an `idempotencyKey`. The unique index on that column (plus optimistic locking) guarantees that a client retrying after a timeout finds the existing row instead of creating a new charge.

```
  client  POST /initiate (key=K1)  ─▶  inserts row     ─▶ 202
  client  POST /initiate (key=K1)  ─▶  unique violation
  service returns the EXISTING payment instead of creating another
```

---

## Entity

[`Payment.java`](src/main/java/com/sparepartshop/payment_service/entity/Payment.java)

**Identity & idempotency**
| Field              | Notes                                            |
|--------------------|--------------------------------------------------|
| `id`               | UUID PK                                          |
| `paymentReference` | unique, human (e.g. `PAY-20260422-0001`)         |
| `idempotencyKey`   | **unique** — client-supplied; retry safety       |

**Link to invoice/customer**
| `invoiceId`, `invoiceNumber`, `customerId`

**Money**
| `amount` (BigDecimal), `currency` (`INR` default), `refundAmount`

**Gateway**
| Field                  | Notes                                        |
|------------------------|----------------------------------------------|
| `paymentMethod`        | `CARD`, `UPI`, `NETBANKING`, `WALLET`, …     |
| `gatewayProvider`      | `MOCK` / `RAZORPAY` / `STRIPE`               |
| `gatewayTransactionId` | id returned by provider                      |
| `webhookPayload`       | raw JSON from webhook for audit              |

**Status & timing**
| `status` enum: `INITIATED` / `PROCESSING` / `SUCCESS` / `FAILED` / `REFUNDED` / `PARTIAL_REFUND`
| `failureReason` (when `FAILED`)
| `initiatedAt`, `completedAt`, `refundedAt`

**Audit**
| `metadata` (free JSON), `createdAt`, `updatedAt`, `version`

---

## Status machine

```
  INITIATED ─▶ PROCESSING ─▶ SUCCESS ─▶ REFUNDED / PARTIAL_REFUND
                   │
                   └───────▶ FAILED
```

Only `SUCCESS` triggers the downstream billing update. `FAILED` leaves the invoice `PENDING` so the customer can retry.

---

## API

Base path: `/api/v1/payments` — see [`ApiPaths.java`](src/main/java/com/sparepartshop/payment_service/constants/ApiPaths.java).

| Method | Path                                             | Purpose                              |
|--------|--------------------------------------------------|--------------------------------------|
| POST   | `/api/v1/payments/initiate`                      | Start payment (requires idempotencyKey) |
| GET    | `/api/v1/payments/{id}`                          | By UUID                              |
| GET    | `/api/v1/payments/reference/{paymentReference}`  | By human reference                   |
| GET    | `/api/v1/payments/invoice/{invoiceId}`           | Payments for an invoice              |
| GET    | `/api/v1/payments/customer/{customerId}`         | Customer's payment history           |
| GET    | `/api/v1/payments/status/{status}`               | Filter by status                     |
| POST   | `/api/v1/payments/webhook`                       | Gateway → us callback (internal)     |
| POST   | `/api/v1/payments/{id}/refund`                   | Refund                               |

---

## Payment Gateway (Strategy pattern)

[`gateway/PaymentGateway.java`](src/main/java/com/sparepartshop/payment_service/gateway/PaymentGateway.java) is the abstraction. [`MockPaymentGateway.java`](src/main/java/com/sparepartshop/payment_service/gateway/MockPaymentGateway.java) is the dev implementation. Real providers (Razorpay/Stripe) plug in as additional `@Component` implementations chosen by `gatewayProvider`.

The mock uses two knobs from [application.yaml](src/main/resources/application.yaml):

```yaml
payment:
  mock:
    success-rate: 90        # 90% of payments succeed
    webhook-delay-ms: 3000  # webhook fires 3s after initiate
```

That lets you test both happy path and failure path without external calls.

---

## Async webhook

[`gateway/WebhookSimulator.java`](src/main/java/com/sparepartshop/payment_service/gateway/WebhookSimulator.java) runs on the `paymentAsyncExecutor` thread pool defined in [`config/AsyncConfig.java`](src/main/java/com/sparepartshop/payment_service/config/AsyncConfig.java). Using a **named, bounded executor** (not the default) means:

- Slow gateways can't starve the Tomcat worker pool.
- Thread-pool metrics are traceable per-use-case (in a future observability pass).
- Rejection policy is explicit (we can pick `CallerRunsPolicy` vs. `AbortPolicy`).

---

## Patterns used here

- **Idempotency keys** — unique DB constraint + caller-supplied key; the gold-standard pattern for safe retries on payment APIs.
- **Strategy pattern** — pluggable gateways via a single `PaymentGateway` interface.
- **Async boundary** — `@Async` + custom executor decouples webhook simulation from the HTTP thread.
- **State machine** — status transitions enforced in the service layer, not assumed from field writes.
- **Optimistic locking** — two concurrent webhook deliveries for the same payment can't both succeed.
- **Feign** — uses `billing-service` client to mark invoices paid on `SUCCESS`.
- **Audit trail** — `webhookPayload` and `metadata` are stored as raw TEXT for forensic replay.

---

## Configuration

[src/main/resources/application.yaml](src/main/resources/application.yaml) — port 8086, MySQL on 3312, Eureka on 8761, plus the `payment.mock.*` section above.

---

## Smoke test

```bash
# 1. Initiate (assumes invoice exists)
curl -X POST http://localhost:8086/api/v1/payments/initiate \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 8f3a-local-test-001' \
  -d '{
        "invoiceId": "<invoice-uuid>",
        "amount": 1180.00,
        "paymentMethod": "UPI",
        "gatewayProvider": "MOCK",
        "idempotencyKey": "8f3a-local-test-001"
      }'

# response: 202 Accepted, status=PROCESSING, paymentReference=PAY-...
# ~3 seconds later the webhook simulator fires and flips it to SUCCESS/FAILED.

# 2. Poll
curl http://localhost:8086/api/v1/payments/reference/<paymentReference>

# 3. Retry with the SAME idempotency key
# → returns the SAME payment row, no new charge
```

Full flow (customer → order → invoice → payment, webhook included) is scripted in `../test-e2e.sh`.
