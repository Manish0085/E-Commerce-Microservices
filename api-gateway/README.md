# API Gateway

Single ingress for the platform. Every external request (from Postman, a frontend, a mobile app, etc.) enters through this service on port **8080** and gets routed to the appropriate business service via Eureka-backed client-side load balancing.

**Port:** `8080`  ·  **Role:** Edge / Infrastructure  ·  **Depends on:** Eureka Server

> Built on **Spring Cloud Gateway MVC** (the Servlet-based flavor), not the reactive WebFlux variant. Config keys live under `spring.cloud.gateway.mvc.*` — do not copy reactive-flavor examples from the web into this service.

---

## Why a Gateway?

Without a gateway, every caller needs to know the URL of every service. That couples clients to internal topology and makes cross-cutting concerns (auth, rate limiting, logging, CORS) impossible to apply uniformly.

```
  WITHOUT GATEWAY                            WITH GATEWAY
  ────────────────                           ────────────

  client ─▶ product-service:8081             client ─▶ gateway:8080 ─▶ product-service
  client ─▶ order-service:8084                                    └─▶ order-service
  client ─▶ billing-service:8085                                  └─▶ billing-service
     (caller knows every port)                (caller knows ONE port)
```

Benefits applied in this module:
- **Path-based routing** to logical service names (no IPs in config)
- **Client-side load balancing** via `lb://` URI scheme + Eureka
- **Unified error contract** — every downstream failure is normalized to the same `ErrorResponse` JSON shape
- Foundation for future cross-cutting filters (auth, rate-limit, request ID propagation)

---

## Routing Table

Defined in [src/main/resources/application.yaml](src/main/resources/application.yaml):

| Incoming path                   | Routes to                    | Internal port |
|---------------------------------|------------------------------|---------------|
| `/api/v1/products/**`           | `lb://product-service`       | 8081          |
| `/api/v1/inventory/**`          | `lb://inventory-service`     | 8082          |
| `/api/v1/customers/**`          | `lb://customer-service`      | 8083          |
| `/api/v1/orders/**`             | `lb://order-service`         | 8084          |
| `/api/v1/invoices/**`           | `lb://billing-service`       | 8085          |
| `/api/v1/payments/**`           | `lb://payment-service`       | 8086          |

### Request lifecycle

```
 Client ──▶ GET http://localhost:8080/api/v1/products/{id}
             │
             ▼
  ┌─────────────────────────────────────────┐
  │  API Gateway (:8080)                    │
  │  1. Match predicate  Path=/api/v1/...   │
  │  2. Resolve lb://product-service        │──▶ Eureka
  │  3. Pick a healthy instance              │◀── returns host/port list
  │  4. Forward via RestClient (MVC flavor) │
  └──────────────┬──────────────────────────┘
                 ▼
        product-service:8081/api/v1/products/{id}
                 │
                 ▼
        ◀── 200 OK + JSON body
  ┌──────────────▼──────────────────────────┐
  │  Gateway returns response as-is         │
  │  OR intercepts failure via exception    │
  │  handler and returns normalized JSON    │
  └─────────────────────────────────────────┘
```

---

## Error Handling

All downstream failures funnel through [`GatewayExceptionHandler`](src/main/java/com/sparepartshop/api_gateway/exception/GatewayExceptionHandler.java), producing a consistent envelope via [`ErrorResponse`](src/main/java/com/sparepartshop/api_gateway/dto/ErrorResponse.java):

| Downstream condition                           | HTTP status | `error` field              |
|------------------------------------------------|-------------|----------------------------|
| Target service down / unreachable              | 503         | `Service Unavailable`      |
| Target reachable but returns non-2xx / 5xx     | 502         | `Bad Gateway`              |
| Any other runtime failure                      | 500         | `Internal Server Error`    |

Example 503 envelope:

```json
{
  "timestamp": "2026-04-22T12:34:56",
  "status": 503,
  "error": "Service Unavailable",
  "message": "product-service is currently unavailable. Please try again later.",
  "path": "/api/v1/products",
  "service": "product-service"
}
```

The service name is derived from the `/api/v1/<segment>` prefix so the error tells you exactly which downstream to inspect.

---

## Configuration highlights

```yaml
spring:
  cloud:
    gateway:
      mvc:
        http-client:
          connect-timeout: 5000
          read-timeout: 30000
        discovery:
          locator:
            enabled: true                 # auto-route by service ID
            lower-case-service-id: true   # Eureka IDs are uppercased by default

eureka:
  instance:
    hostname: localhost
    prefer-ip-address: false     # critical — see Troubleshooting in root README
```

Actuator exposes `health`, `gateway`, `info`, and `routes`:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/gateway/routes
```

---

## Running

```bash
./mvnw spring-boot:run
```

The Gateway must start **after** Eureka (so it can resolve `lb://` URIs) and ideally after business services have registered — `start-all.sh` handles this ordering with a 45-second wait.

---

## Smoke test via Gateway

```bash
# hits product-service through the gateway
curl http://localhost:8080/api/v1/products

# hits order-service through the gateway
curl http://localhost:8080/api/v1/orders
```
