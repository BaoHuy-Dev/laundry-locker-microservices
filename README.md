# Laundry Locker Microservices

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho
> locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng
> Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`,
`RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

This repository is the Java/Spring microservices backend for the Smart Laundry Locker project. It is separate from the
old monolith and is now the backend used by the web frontend and Flutter mobile app.

## Current Snapshot

- Java 21, Spring Boot 3.5.14, Spring Cloud 2025.0.2, Maven multi-module.
- Gateway at `http://localhost:18080`, Eureka at `http://localhost:8761`.
- PostgreSQL runs on host port `15432`; RabbitMQ runs on `5672` with UI at `http://localhost:15672`.
- Locker Phase 1 and Phase 2 are implemented: physical cell layout, SEND, RENTAL, signed QR token, PIN/QR access
  verification, manager APIs, maintenance APIs, RBAC, and schedulers.
- `laundry-service` and `partner-service` are not present as source modules in the current workspace. They remain
  reserved in compose/database naming, but `docker-compose.override.yml` places them behind the `missing-source` profile
  so normal local compose runs skip them.

## Modules

| Module                 | Port | Current status                                                                                              |
|------------------------|-----:|-------------------------------------------------------------------------------------------------------------|
| `common-lib`           |    - | Shared DTOs, responses, event names, common exceptions. No shared entities.                                 |
| `discovery-server`     | 8761 | Eureka registry.                                                                                            |
| `api-gateway`          | 8080 | Gateway routing, JWT validation, RBAC, identity header propagation.                                         |
| `auth-service`         | 8081 | Register/login/refresh/logout, JWT, OTP/password flows, admin auth.                                         |
| `user-service`         | 8082 | User profiles, roles, admin user APIs, internal profile lookup.                                             |
| `order-service`        | 8083 | Laundry orders, SEND/RENTAL, PIN/QR access, promotions, ratings, complaints, scheduler, manager order APIs. |
| `locker-service`       | 8084 | Lockers, physical cells, layout, fault reports, manager and maintenance APIs.                               |
| `payment-service`      | 8086 | Payments, refunds, cash flow, VNPay/MoMo callback parity.                                                   |
| `notification-service` | 8087 | Notifications, FCM token storage, FCM/WebSocket hooks, Rabbit listeners.                                    |
| `iot-service`          | 8088 | Device status, unlock facade, PIN/QR access verification, MQTT command facade.                              |
| `store-service`        | 8089 | Store CRUD and admin APIs.                                                                                  |
| `staff-service`        | 8090 | Staff assignment/order facade.                                                                              |
| `loyalty-service`      | 8092 | Points, stamps, rewards, redemption/admin APIs.                                                             |
| `laundry-service`      | 8085 | Reserved/missing source; skipped by compose override.                                                       |
| `partner-service`      | 8091 | Reserved/missing source; skipped by compose override.                                                       |

## Databases

Local Docker creates one PostgreSQL database per service through `docker/postgres/init-databases.sql`. Source-backed
services use their matching schema:

| Service              | Database          | Schema                |
|----------------------|-------------------|-----------------------|
| auth-service         | `auth_db`         | `auth_schema`         |
| user-service         | `user_db`         | `user_schema`         |
| order-service        | `order_db`        | `order_schema`        |
| locker-service       | `locker_db`       | `locker_schema`       |
| payment-service      | `payment_db`      | `payment_schema`      |
| notification-service | `notification_db` | `notification_schema` |
| iot-service          | `iot_db`          | `iot_schema`          |
| store-service        | `store_db`        | `store_schema`        |
| staff-service        | `staff_db`        | `staff_schema`        |
| loyalty-service      | `loyalty_db`      | `loyalty_schema`      |

`laundry_db` and `partner_db` may also be initialized for future compatibility, but there is no current source module
using them.

## Gateway Routes

Clients should call the gateway on host port `18080`. The gateway still runs on container port `8080`; direct service
ports are for local debugging.

| Gateway path                                                                                                            | Target service                         |
|-------------------------------------------------------------------------------------------------------------------------|----------------------------------------|
| `/api/auth/**`, `/api/admin/auth/**`                                                                                    | `auth-service`                         |
| `/api/user/**`, `/api/users/**`, `/api/admin/users/**`, `/api/admin/audit-logs/**`                                      | `user-service`                         |
| `/api/orders/**`, `/api/manage/orders/**`, `/api/admin/orders/**`, `/api/admin/scheduler/**`, `/api/admin/dashboard/**` | `order-service`                        |
| `/api/promotions/**`, `/api/admin/promotions/**`                                                                        | `order-service`                        |
| `/api/lockers/**`, `/api/boxes/**`, `/api/manage/lockers/**`, `/api/maintenance/**`, `/api/admin/lockers/**`            | `locker-service`                       |
| `/api/payments/**`, `/api/admin/payments/**`                                                                            | `payment-service`                      |
| `/api/notifications/**`, `/api/admin/notifications/**`                                                                  | `notification-service`                 |
| `/ws`, `/ws/**`                                                                                                         | `notification-service` WebSocket/STOMP |
| `/api/iot/**`                                                                                                           | `iot-service`                          |
| `/api/stores/**`, `/api/admin/stores/**`                                                                                | `store-service`                        |
| `/api/staff/**`                                                                                                         | `staff-service`                        |
| `/api/loyalty/**`, `/api/admin/loyalty/**`                                                                              | `loyalty-service`                      |

RBAC is enforced in the gateway:

- `/api/admin/**`: `ADMIN`
- `/api/manage/**`: `MANAGER` or `ADMIN`
- `/api/maintenance/**`: `MAINTENANCE` or `ADMIN`
- `/internal/**`: service-to-service only. The gateway blocks external calls to this prefix with `403`.

## Locker Flow Status

Implemented:

- Physical locker cells with `DRONE`, `STANDARD`, and `XL` types.
- Row/column layout API: `GET /api/lockers/{id}/layout`.
- Demo cabinet `CAB-DEMO-01` with landing pad metadata.
- Cell lifecycle: `AVAILABLE -> RESERVED -> OCCUPIED -> AVAILABLE`.
- Sticky `FAULT` state with report/claim/resolve flows.
- SEND order: customer drops a parcel, system rotates PIN for the receiver, receiver completes pickup.
- RENTAL order: hourly rental, extend rental, end rental, multi-use PIN during the rental.
- Signed QR token returned as `qrToken` in `OrderResponse`; QR becomes invalid when the active PIN changes.
- IoT access verification with PIN or QR token through `POST /api/iot/verify-access`.
- Manager APIs under `/api/manage/**`.
- Maintenance APIs under `/api/maintenance/**`.

Future/Phase 3:

- Tablet-web locker screen.
- Real sensor-driven auto-occupy/release.
- Real drone delivery service integration.
- Real biometric verification on Raspberry Pi.

## Events

RabbitMQ exchange: `laundry.events`.

Current event names from `DomainEventNames`:

- `order.created`
- `order.status.changed`
- `payment.completed`
- `payment.failed`
- `notification.requested`
- `locker.box.opened`
- `locker.box.fault`
- `iot.device.status.changed`

`notification-service` listens to order/payment events and creates baseline notifications.

## Run Locally

PowerShell:

```powershell
Set-Location G:\BigProject\laundry-locker-microservices

# If Maven is not on PATH, use:
# C:\Maven\apache-maven-3.9.16\bin\mvn.cmd clean package -DskipTests
mvn.cmd clean package -DskipTests

docker compose config
docker compose up --build -d
docker compose ps
```

Useful URLs:

- Gateway: `http://localhost:18080`
- Eureka: `http://localhost:8761`
- RabbitMQ UI: `http://localhost:15672` (`guest` / `guest`)
- PostgreSQL: `localhost:15432` (`postgres` / `postgres`)

Health check:

```powershell
curl.exe -s -o NUL -w "%{http_code}" http://localhost:18080/actuator/health
```

Expected result: `200`.

## Smoke Test

All requests below go through the gateway.

```powershell
$BASE = "http://localhost:18080"

# Register a customer if the local DB is fresh.
$email = "demo$((Get-Random))@laundry.test"
$password = "secret123"
$register = Invoke-RestMethod -Method Post -Uri "$BASE/api/auth/register" -ContentType "application/json" -Body (@{
  email = $email
  phoneNumber = "0900000000"
  firstName = "Demo"
  lastName = "User"
  password = $password
  roles = @("CUSTOMER")
} | ConvertTo-Json)

$login = Invoke-RestMethod -Method Post -Uri "$BASE/api/auth/login" -ContentType "application/json" -Body (@{
  identifier = $email
  password = $password
} | ConvertTo-Json)

$TOKEN = $login.data.accessToken
$AUTH = @{ Authorization = "Bearer $TOKEN" }

# Find the demo cabinet seeded by Flyway.
$lockers = Invoke-RestMethod -Method Get -Uri "$BASE/api/lockers" -Headers $AUTH
$locker = @($lockers.data | Where-Object { $_.code -eq "CAB-DEMO-01" } | Select-Object -First 1)[0]
$LOCKER_ID = $locker.id

Invoke-RestMethod -Method Get -Uri "$BASE/api/lockers/$LOCKER_ID/layout" -Headers $AUTH

# SEND flow.
$send = Invoke-RestMethod -Method Post -Uri "$BASE/api/orders/send" -Headers $AUTH -ContentType "application/json" -Body (@{
  lockerId = $LOCKER_ID
  receiverPhone = "0900000001"
  receiverName = "Receiver"
  note = "Smoke test parcel"
} | ConvertTo-Json)

$SEND_ORDER_ID = $send.data.id
Invoke-RestMethod -Method Put -Uri "$BASE/api/orders/$SEND_ORDER_ID/confirm" -Headers $AUTH

# RENTAL flow.
$rental = Invoke-RestMethod -Method Post -Uri "$BASE/api/orders/rental" -Headers $AUTH -ContentType "application/json" -Body (@{
  lockerId = $LOCKER_ID
  cellType = "XL"
  hours = 2
  note = "Smoke test rental"
} | ConvertTo-Json)

$RENTAL_ORDER_ID = $rental.data.id
Invoke-RestMethod -Method Post -Uri "$BASE/api/orders/$RENTAL_ORDER_ID/extend-rental" -Headers $AUTH -ContentType "application/json" -Body (@{ hours = 1 } | ConvertTo-Json)
Invoke-RestMethod -Method Post -Uri "$BASE/api/orders/$RENTAL_ORDER_ID/pickup-storage" -Headers $AUTH
```

Manager and maintenance endpoints need role-specific tokens:

```powershell
# Dev convenience: register role-scoped accounts in local DB.
$manager = Invoke-RestMethod -Method Post -Uri "$BASE/api/auth/register" -ContentType "application/json" -Body (@{
  email = "manager$((Get-Random))@laundry.test"
  phoneNumber = "0910000000"
  firstName = "Manager"
  lastName = "User"
  password = "secret123"
  roles = @("MANAGER")
} | ConvertTo-Json)

$MANAGER_AUTH = @{ Authorization = "Bearer $($manager.data.accessToken)" }
Invoke-RestMethod -Method Get -Uri "$BASE/api/manage/lockers/stats" -Headers $MANAGER_AUTH
Invoke-RestMethod -Method Get -Uri "$BASE/api/manage/orders/statistics" -Headers $MANAGER_AUTH

$maintenance = Invoke-RestMethod -Method Post -Uri "$BASE/api/auth/register" -ContentType "application/json" -Body (@{
  email = "maintenance$((Get-Random))@laundry.test"
  phoneNumber = "0920000000"
  firstName = "Maintenance"
  lastName = "User"
  password = "secret123"
  roles = @("MAINTENANCE")
} | ConvertTo-Json)

$MAINT_AUTH = @{ Authorization = "Bearer $($maintenance.data.accessToken)" }
Invoke-RestMethod -Method Get -Uri "$BASE/api/maintenance/faults" -Headers $MAINT_AUTH
Invoke-RestMethod -Method Get -Uri "$BASE/api/maintenance/reports" -Headers $MAINT_AUTH
```

## Documentation Map

- `docs/CURRENT_PROJECT_STATUS.md`: source of truth for the current project state.
- `docs/PROJECT_PROGRESS_TRACKER.md`: living progress tracker; update after every feature/fix/verification.
- `docs/BUSINESS_FLOWS_CURRENT.md`: living detailed business-flow document; update whenever behavior changes.
- `RUN_AND_TEST_GUIDE.md`: Windows PowerShell run and smoke-test guide for this backend.
- `LOCKER_FLOW_PLAN.md`: locker Phase 1/2/3 plan and completion status.
- `RUN_RESULT.md`: latest verified run results and changed-file log.
- `docs/project-artifacts/`: collected guides, logs, screenshots, and categorized Markdown copies.
