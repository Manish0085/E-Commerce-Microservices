# Eureka Server

Service registry for the SparePartShop platform. Every other service registers here on startup and queries it to discover peers by logical name (e.g., `lb://product-service`).

**Port:** `8761`  ·  **Role:** Infrastructure  ·  **Depends on:** _nothing — starts first_

---

## Why this service exists

In a microservices deployment, instances come and go (scale events, redeploys, crashes). Hard-coding hostnames is a dead-end. Eureka solves this with a **push/pull registry**:

```
    ┌────────────────────────┐
    │      Eureka Server     │     ◀── single source of truth
    │   (this module, :8761) │        for service locations
    └───────────▲────────────┘
                │
    ┌───────────┼─────────────────────────────────────────┐
    │  REGISTER & HEARTBEAT every 30s (clients push)      │
    │  FETCH REGISTRY every 30s     (clients pull & cache)│
    └───────────┼─────────────────────────────────────────┘
                │
   ┌────────┬───┴────┬─────────┬────────┬─────────┬────────┐
   ▼        ▼        ▼         ▼        ▼         ▼        ▼
 gateway product  inventory customer  order   billing  payment
```

When the API Gateway resolves `lb://product-service`, it calls Eureka for the list of healthy `product-service` instances and load-balances across them.

---

## Configuration

[src/main/resources/application.yaml](src/main/resources/application.yaml):

```yaml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  client:
    register-with-eureka: false   # this IS the server — don't self-register
    fetch-registry: false         # nothing to fetch; we ARE the registry
```

The two `false` flags are the critical bit — without them Eureka tries to register with itself and log-spam with connection errors.

---

## Running

```bash
# from this directory
./mvnw spring-boot:run

# or from repo root (part of start-all.sh)
../start-all.sh
```

Dashboard: [http://localhost:8761](http://localhost:8761)

Registered-apps JSON API:
```bash
curl -H "Accept: application/json" http://localhost:8761/eureka/apps
```

---

## Health

```bash
curl http://localhost:8761/actuator/health
```

The `status.sh` script in the repo root treats 200/302/404 from `http://localhost:8761` as healthy.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Clients log `Connect refused` on 8761 | Eureka hasn't started yet — give it ~20s |
| Dashboard shows instance with wrong IP | Client missing `eureka.instance.hostname: localhost` and `prefer-ip-address: false` |
| `UNKNOWN` apps in dashboard | Client `spring.application.name` not set |
| Gateway 503 on routed calls | Target service registered but unhealthy, or registered with an unreachable IP |
