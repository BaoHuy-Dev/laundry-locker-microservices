# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot microservices architecture for the Smart Laundry Locker platform. Java 21, Maven multi-module. 11 business
services + 1 API Gateway + 1 Eureka discovery server, all orchestrated via Docker Compose.

**Tech stack:** Spring Boot 3.5.14 · Spring Cloud 2025.0.2 · Java 21 · Spring Data JPA · PostgreSQL 16 · Flyway ·
RabbitMQ · OpenFeign · Resilience4j · Eureka · Spring Cloud Gateway · JJWT 0.13.0 · Lombok · springdoc-openapi

## Commands

```bash
# Build all modules (skip tests)
mvn clean package -DskipTests

# Build with tests
mvn clean verify

# Run all tests
mvn -B test

# Start full local stack
docker compose up --build -d

# Validate compose config
docker compose config

# View running services
docker compose ps

# Health check (API Gateway)
curl http://localhost:18080/actuator/health

# Service UIs
# Eureka:    http://localhost:8761
# RabbitMQ:  http://localhost:15672  (guest/guest)
# Swagger:   http://localhost:18080/swagger-ui.html
```

## Service Map

| Service                | Port (host) | Package                                 |
|------------------------|-------------|-----------------------------------------|
| `api-gateway`          | 18080       | `com.huynqb.laundrylocker.gateway`      |
| `discovery-server`     | 8761        | `com.huynqb.laundrylocker.discovery`    |
| `auth-service`         | 8081        | `com.huynqb.laundrylocker.auth`         |
| `user-service`         | 8082        | `com.huynqb.laundrylocker.user`         |
| `order-service`        | 8083        | `com.huynqb.laundrylocker.order`        |
| `locker-service`       | 8084        | `com.huynqb.laundrylocker.locker`       |
| `payment-service`      | 8086        | `com.huynqb.laundrylocker.payment`      |
| `notification-service` | 8087        | `com.huynqb.laundrylocker.notification` |
| `iot-service`          | 8088        | `com.huynqb.laundrylocker.iot`          |
| `store-service`        | 8089        | `com.huynqb.laundrylocker.store`        |
| `loyalty-service`      | 8092        | `com.huynqb.laundrylocker.loyalty`      |

`common-lib` is a shared Maven module containing `ApiResponse`, domain event names (`DomainEventNames`), and common
exceptions. It has no runtime Spring context — do not add Spring Boot auto-configurations to it.

## Architecture

Each service follows the same internal layered structure:

```
controller/    ← REST endpoints (@RestController)
service/       ← Business logic (@Service)
model/         ← JPA entities (@Entity) — never shared across services
repository/    ← Spring Data JPA repositories
dto/           ← Request/response DTOs
client/        ← Feign clients for inter-service calls
config/        ← Spring @Configuration (RabbitMQ, security, etc.)
```

Each service owns its own database and schema (e.g., `auth_db` / `auth_schema`). Never write a JPA entity that
references a table in another service's database.

## Inter-Service Communication

**Synchronous (Feign):** use for blocking lookups (e.g., order-service checking a locker's availability).

```java
@FeignClient(name = "locker-service", path = "/internal/boxes")
public interface LockerClient {
    @GetMapping("/{id}")
    ApiResponse<LockerBoxSummary> getBox(@PathVariable Long id);
}
```

- Internal endpoints use path prefix `/internal/**` — the gateway blocks these from external callers (403)
- Wrap Feign calls with `@CircuitBreaker` / `@Retry` from Resilience4j
- Default Feign timeouts: `connectTimeout: 2000ms`, `readTimeout: 5000ms`

**Asynchronous (RabbitMQ):** use for domain events (order created, payment completed, notification requested, etc.).

- Single topic exchange: `laundry.events`
- Event routing keys defined in `DomainEventNames` (in `common-lib`)
- Consumers in `notification-service` listen to order/payment events for baseline notifications
- Publish from service layer after a successful commit; never publish inside a transaction

## API Gateway & RBAC

JWT validation and role enforcement happen in `JwtGatewayFilter` (api-gateway). Services themselves do not re-validate
JWT — they trust the `X-User-Id`, `X-User-Email`, `X-User-Roles`, `X-Correlation-Id` headers forwarded by the gateway.

Role-path mapping:

Canonical role set: `CUSTOMER` · `ADMIN` (web console) · `TECHNICIAN` (locker upkeep + IoT) ·
`MAINTENANCE` (drone team). `MANAGER` and `STAFF` were retired — do not reintroduce them.

- `/api/admin/**` → `ADMIN` only
- `/api/maintenance/drone**` → `MAINTENANCE` or `ADMIN` (drone team surface)
- `/api/maintenance/**` → `MAINTENANCE`, `TECHNICIAN` or `ADMIN`
- `/api/technician/**` → `TECHNICIAN` or `ADMIN`
- `/internal/**` → blocked externally (403)
- `/{service-id}/**` → 404. The discovery locator is disabled; this shape used to bypass
  both the `/internal` block and the RBAC above.

## Database & Migrations

Each service uses Flyway for schema management. JPA `ddl-auto` is `validate` — Flyway owns schema creation, not
Hibernate.

Migration files: `src/main/resources/db/migration/V<N>__<description>.sql`

Naming rules:

- Table names: `snake_case`, singular or plural consistent within the service
- Column names: `snake_case`
- FK columns: `<referenced_table>_id`

Database config is passed via env vars; defaults in `application.yml` point to `localhost:15432` for local dev outside
Docker.

## Naming Conventions

| Artifact        | Pattern                   | Example                               |
|-----------------|---------------------------|---------------------------------------|
| Entity          | `{Domain}` (no suffix)    | `LockerOrder`, `LockerUnit`           |
| Service         | `{Domain}Service`         | `AuthService`                         |
| Controller      | `{Domain}Controller`      | `OrderController`                     |
| Repository      | `{Domain}Repository`      | `PaymentRepository`                   |
| Feign client    | `{Service}Client`         | `LockerClient`, `UserClient`          |
| Request DTO     | `{Action}{Domain}Request` | `LoginRequest`, `CreateOrderRequest`  |
| Response DTO    | `{Domain}Response`        | `AuthResponse`, `OrderDetailResponse` |
| RabbitMQ config | `{Feature}RabbitConfig`   | `NotificationRabbitConfig`            |

Package convention: `com.huynqb.laundrylocker.<service>.<layer>`

## API Response Format

All responses use `ApiResponse<T>` from `common-lib`:

```java
public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data,
    List<ApiError> errors
) {}
```

## Environment Configuration

No Spring Cloud Config server. Configuration is purely via env vars with defaults in each service's `application.yml`.

Key shared vars (set in `docker-compose.yml` or a `.env` file at repo root):

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/<service>_db
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-server:8761/eureka
SPRING_RABBITMQ_HOST: rabbitmq
APP_SECURITY_JWT_SECRET: <32-char secret, same across all services>
```

For local dev (outside Docker), Eureka defaults to `http://localhost:8761/eureka` and DB to `localhost:15432`.

## Observability

All services expose Spring Actuator: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`. Correlation IDs (
`X-Correlation-Id`) are propagated across services via MDC and included in log pattern:
`%5p [${spring.application.name},%X{correlationId:-}]`.

## Architecture Decisions (Deliberately Deferred)

These are explicitly NOT being built at current scale (1 VM, small team). Do not propose or implement them unless
the stated trigger conditions are met. See `docs/ARCHITECTURE_DECISIONS.md`.

| Topic                 | Decision                  | Trigger to revisit                                          |
|-----------------------|---------------------------|-------------------------------------------------------------|
| Kubernetes / Helm     | Stay on Docker Compose    | Need >1 node, zero-downtime SLA, or autoscale               |
| Kafka                 | Keep RabbitMQ             | Need event replay, streaming analytics, or high throughput  |
| CQRS / Event Sourcing | Keep CRUD + domain events | Heavy read load harming OLTP, or complex audit requirements |
| GraphQL               | Keep REST                 | Client over/under-fetch is measurably painful               |
| Service mesh (Istio)  | Defer (gated on K8s)      | After K8s adoption + need mTLS or canary                    |

Current investment priority: healthcheck + deploy runbook, Postgres backups, RabbitMQ DLQ/retry, Prometheus alerting.

## Known Critical Gotchas

- **Login body uses `identifier`**, not `email`: `{"identifier": "email@example.com", "password": "..."}` — the field
  name change is intentional.
- **JWT `tokenUse` claim**: gateway only accepts `tokenUse=access`; refresh tokens are rejected at business API routes.
- **CORS config path**: must be under `spring.cloud.gateway.server.webflux.globalcors` — the old
  `spring.cloud.gateway.globalcors` prefix was deprecated in Gateway 4.3.0 and is silently ignored.
- **Resilience4j default timeout is 1s** — too short for Feign cold-start (Hibernate connection init). Production config
  raises `timeout-duration` to 10s. Don't regress this.
- **`staff-service` has been removed** (PA3, 2026-06-15) — no source, no container, no DB. `staff_db` and
  `staff_assignments` table no longer exist.
- **`partner-service` and `PARTNER` role have been removed** (2026-06-13) — no source, no DB `partner_db`, no role in
  seed. Partner portal routes in any frontend are deprecated.
- **`laundry-service` source is missing** — kept in compose naming but skipped via `docker-compose.override.yml`. Do not
  add `laundry_db` queries.

## Deployed Environment

- **Shared backend (Azure):** `https://api.locker-drone.tech` — all clients default to this.
- **Local (Docker Compose):** gateway at `http://localhost:18080` (host 8080 may be occupied).
- Deploy is automatic: merge to `develop` → GitHub Actions (`deploy-azure.yml`) builds + deploys + Flyway migrates on
  startup.
- **Direct DB access** requires SSH tunnel via `<AZURE_VM_IP>:22`; PostgreSQL exposed at port `15432` inside the
  tunnel (the Azure NSG only opens 22 + 80 + 443; port 8080 stays VM-internal behind Nginx).

## Test Accounts (password `12345678` for all demo accounts)

| Email                                | Role        |
|--------------------------------------|-------------|
| `baohuy2k12k4@gmail.com`             | ADMIN       |
| `nqbhuy2004nt@gmail.com`             | CUSTOMER    |
| `se180211nguyenquocbaohuy@gmail.com` | MAINTENANCE |
| `huynqbse180211@fpt.edu.vn`          | TECHNICIAN  |

## RabbitMQ Events

Exchange: `laundry.events`. All routing keys defined in `DomainEventNames` (common-lib):

`order.created` · `order.status.changed` · `payment.completed` · `payment.failed` · `notification.requested` ·
`locker.box.opened` · `locker.box.fault` · `locker.report.claimed` · `locker.report.resolved` ·
`iot.device.status.changed`

Publish from service layer after a successful commit. `notification-service` is the primary consumer for
order/payment/locker events. Use DLQ + retry + consumer idempotency for important events.

## Living Docs

Read these before making business logic changes — they are kept current and override older docs:

- `docs/BUSINESS_FLOWS_CURRENT.md` — authoritative roles, endpoints, flows, and known gaps
- `docs/PROJECT_PROGRESS_TRACKER.md` — implementation progress and verification log
- `docs/ARCHITECTURE_DECISIONS.md` — ADRs for deferred architecture choices

## Git Convention

Commit message format: `<type>(<scope>): <description>` — e.g. `feat(order): add rental order endpoint`,
`fix(gateway): fix RBAC path matching`. Never stage `.env`, `target/`, or generated files.
