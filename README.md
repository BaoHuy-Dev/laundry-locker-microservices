# Laundry Locker Microservices

This workspace is a big-bang microservices migration created beside the old monolith. It does not modify `laundry-locker-backend`.

The current pass migrates the monolith modules into independently buildable services with service-owned entities, Flyway schemas, OpenFeign clients, RabbitMQ events, gateway routing, and Docker packaging. The goal is feature parity with the existing monolith, not production hardening beyond what the monolith already had.

Parity scan result: 209 monolith controller endpoints scanned, 265 microservice controller endpoints found, 0 monolith endpoints missing. See `PARITY_REPORT.md` for the full API and service/use-case table.

## Architecture

- Java 21
- Spring Boot 3.5.14
- Spring Cloud 2025.0.2
- Maven multi-module
- Eureka discovery server
- Spring Cloud Gateway
- OpenFeign for synchronous internal REST calls
- RabbitMQ topic exchange for asynchronous domain events
- PostgreSQL with one database and one Flyway schema per service
- No cross-service JPA foreign keys
- No shared entity classes
- Shared library only contains DTO/common response/event DTO/common exception support

## Modules

| Module | Responsibility | Port |
|---|---|---:|
| discovery-server | Eureka registry | 8761 |
| api-gateway | Gateway routes | 8080 |
| auth-service | Auth accounts, JWT, phone/email OTP, admin auth, password reset | 8081 |
| user-service | User profiles | 8082 |
| order-service | Orders and order status events | 8083 |
| locker-service | Lockers, boxes, box-open events | 8084 |
| laundry-service | Laundry service catalog | 8085 |
| payment-service | Payments and payment events | 8086 |
| notification-service | Notifications, FCM tokens, FCM push, WebSocket push | 8087 |
| iot-service | IoT device status, PIN/access-code unlock, MQTT command facade | 8088 |
| store-service | Stores | 8089 |
| staff-service | Staff assignments | 8090 |
| partner-service | Partners and staff access codes | 8091 |
| loyalty-service | Loyalty accounts and point transactions | 8092 |

## Database Ownership

All databases are created by `docker/postgres/init-databases.sql` in one PostgreSQL container for local development.

| Service | Database | Schema |
|---|---|---|
| auth-service | `auth_db` | `auth_schema` |
| user-service | `user_db` | `user_schema` |
| order-service | `order_db` | `order_schema` |
| locker-service | `locker_db` | `locker_schema` |
| laundry-service | `laundry_db` | `laundry_schema` |
| payment-service | `payment_db` | `payment_schema` |
| notification-service | `notification_db` | `notification_schema` |
| iot-service | `iot_db` | `iot_schema` |
| store-service | `store_db` | `store_schema` |
| staff-service | `staff_db` | `staff_schema` |
| partner-service | `partner_db` | `partner_schema` |
| loyalty-service | `loyalty_db` | `loyalty_schema` |

## Domain Mapping

- `module/auth` -> `auth-service`
- `module/user` -> `user-service`
- `module/order` -> `order-service`
- `module/locker` -> `locker-service`
- `module/laundry` -> `laundry-service`
- `module/payment` -> `payment-service`
- `module/notification` -> `notification-service`
- `module/iot` -> `iot-service`
- `module/store` -> `store-service`
- `module/staff` -> `staff-service`
- `module/partner` -> `partner-service`
- `module/loyalty` -> `loyalty-service`
- `module/admin` -> admin API ownership is distributed into the relevant service; gateway handles routing.

Cross-service entity references were converted to ID fields: `userId`, `orderId`, `storeId`, `partnerId`, `staffId`, `lockerId`, `boxId`.

## Events

RabbitMQ exchange: `laundry.events`

Current event names:

- `order.created`
- `order.status.changed`
- `payment.completed`
- `payment.failed`
- `notification.requested`
- `locker.box.opened`
- `iot.device.status.changed`

`notification-service` listens to order/payment events and creates baseline notifications.

## Gateway Routes

Frontend clients should call `api-gateway` on port `8080`. Direct service ports are exposed for local debugging only.

| Gateway path | Target service |
|---|---|
| `/api/auth/**`, `/api/admin/auth/**` | `auth-service` |
| `/api/users/**`, `/api/user/**`, `/api/admin/users/**` | `user-service` |
| `/api/orders/**`, `/api/admin/orders/**`, `/api/admin/dashboard/**` | `order-service` |
| `/api/promotions/**`, `/api/admin/promotions/**` | `order-service` |
| `/api/lockers/**`, `/api/boxes/**`, `/api/admin/lockers/**` | `locker-service` |
| `/api/laundry-services/**`, `/api/services/**`, `/api/admin/services/**` | `laundry-service` |
| `/api/payments/**`, `/api/admin/payments/**` | `payment-service` |
| `/api/notifications/**`, `/api/admin/notifications/**` | `notification-service` |
| `/ws`, `/ws/**` | `notification-service` WebSocket/STOMP |
| `/api/iot/**`, `/api/devices/**` | `iot-service` |
| `/api/stores/**`, `/api/admin/stores/**` | `store-service` |
| `/api/staff/**` | `staff-service` |
| `/api/partners/**`, `/api/partner/**`, `/api/admin/partners/**` | `partner-service` |
| `/api/loyalty/**`, `/api/admin/loyalty/**` | `loyalty-service` |
| `/internal/**` | service-to-service calls only; routed for local smoke testing |

JWT/RBAC is enforced at the gateway for protected and admin paths. The gateway forwards identity headers (`X-User-Id`, `X-Account-Id`, `X-User-Roles`) to downstream services.

## Run

```bash
mvn clean package
mvn test
docker compose config
docker compose up --build -d
```

The service Dockerfiles copy the already packaged local JARs from each module's `target` directory. Run `mvn clean package` before `docker compose up --build -d`.

Useful URLs:

- Gateway: http://localhost:8080
- Eureka: http://localhost:8761
- RabbitMQ UI: http://localhost:15672 (`guest` / `guest`)
- PostgreSQL: localhost:15432 (`postgres` / `postgres`)

## Main API Smoke Test

Register and login:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"demo@laundry.test\",\"phoneNumber\":\"0900000000\",\"firstName\":\"Demo\",\"lastName\":\"User\",\"password\":\"secret123\",\"roles\":[\"CUSTOMER\"]}"

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"demo@laundry.test\",\"password\":\"secret123\"}"
```

Save the returned `accessToken`:

```bash
set TOKEN=replace-with-access-token
```

Create store, laundry service, locker, and box:

```bash
curl -X POST http://localhost:8080/api/stores \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"District 1 Store\",\"address\":\"HCMC\"}"

curl -X POST http://localhost:8080/api/lockers \
  -H "Content-Type: application/json" \
  -d "{\"storeId\":1,\"code\":\"LCK-001\",\"name\":\"Locker 001\"}"

curl -X POST http://localhost:8080/api/boxes \
  -H "Content-Type: application/json" \
  -d "{\"lockerId\":1,\"boxNumber\":1,\"size\":\"MEDIUM\"}"

curl -X POST http://localhost:8080/api/services \
  -H "Content-Type: application/json" \
  -d "{\"storeId\":1,\"name\":\"Wash and Fold\",\"category\":\"LAUNDRY\",\"basePrice\":50000,\"unit\":\"KG\",\"estimatedHours\":24}"
```

Create order and update status:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer %TOKEN%" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":1,\"storeId\":1,\"lockerId\":1,\"sendBoxId\":1,\"serviceCategory\":\"LAUNDRY\",\"totalPrice\":50000,\"items\":[{\"serviceId\":1,\"quantity\":1,\"description\":\"Wash and Fold\"}]}"

curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Authorization: Bearer %TOKEN%" \
  -H "Content-Type: application/json" \
  -d "{\"status\":\"READY\",\"staffId\":1,\"receiveBoxId\":1}"
```

Create payment and complete it:

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer %TOKEN%" \
  -H "Content-Type: application/json" \
  -d "{\"orderId\":1,\"userId\":1,\"amount\":50000,\"method\":\"CASH\",\"referenceId\":\"PAY-001\"}"

curl -X PATCH http://localhost:8080/api/payments/1/status \
  -H "Authorization: Bearer %TOKEN%" \
  -H "Content-Type: application/json" \
  -d "{\"status\":\"COMPLETED\"}"
```

Check notifications:

```bash
curl http://localhost:8080/api/notifications/user/1 \
  -H "Authorization: Bearer %TOKEN%"
```

IoT locker event:

```bash
curl -X POST http://localhost:8080/api/iot/verify-pin \
  -H "Authorization: Bearer %TOKEN%" \
  -H "Content-Type: application/json" \
  -d "{\"pinCode\":\"123456\",\"boxId\":1}"

curl -X POST http://localhost:8080/api/iot/box-status \
  -H "Authorization: Bearer %TOKEN%" \
  -H "Content-Type: application/json" \
  -d "{\"deviceId\":\"locker-001\",\"lockerId\":1,\"boxId\":1,\"status\":\"OPENED\"}"
```

Admin report baseline:

```bash
curl http://localhost:8080/api/admin/dashboard/overview \
  -H "Authorization: Bearer %TOKEN%"
```

## Data Migration

`docker/postgres/migrate-from-monolith-template.sql` contains a dblink-based migration template from the old monolith database into per-service databases.

Assumptions:

- Old table/column names must be checked against the active monolith schema before running.
- Cross-domain foreign keys are copied as scalar IDs only.
- Production migration should run through staging first, with row counts and reconciliation queries per service.

## Migration Status

Detailed file mapping is tracked in `MIGRATION_MAPPING.md`.

| Service | Current status |
|---|---|
| `common-lib` | Shared DTO, response, exception, event contracts migrated. No shared entities. |
| `auth-service` | Register/login/refresh/logout, JWT issuing, BCrypt, phone/email OTP, SMTP email sender, password reset/change, admin auth. |
| `api-gateway` | Routes all public/admin APIs, JWT validation, admin RBAC guard, identity header propagation. |
| `user-service` | User profiles, roles/permissions tables, admin user APIs, auth provisioning endpoint. |
| `notification-service` | Notifications, FCM tokens, internal/admin/public APIs, Rabbit event listener, Firebase push hook, WebSocket/STOMP push. |
| `store-service` | Store CRUD/admin/status/nearby APIs. |
| `laundry-service` | Laundry catalog, pricing fields, estimate endpoint, admin APIs. |
| `locker-service` | Lockers, boxes, reserve/release/open, reports, admin APIs. |
| `order-service` | Orders, details, status history, ratings, complaints, promotions, lifecycle events, scheduler/admin dashboard parity. |
| `payment-service` | Payments, refunds, cash flow, VNPay return/IPN signing parity, MoMo callback parity. |
| `iot-service` | Device registry, PIN verify, unlock/pickup flow, MQTT publishing, device status events. |
| `staff-service` | Staff order views and assignment facade via order-service. |
| `partner-service` | Partner profile/status/admin APIs, staff access code lifecycle, order/store/locker facades. |
| `loyalty-service` | Points, stamps, rewards, redemption/admin APIs. |

## Parity Notes

There are no controller endpoints missing from the microservices parity scan. Remaining partials are either source-level TODO/mock behavior already present in the monolith or environment-dependent integrations:

- `AdminSystemController.checkExternalServices` returned empty/mock external checks in the monolith; gateway health keeps a lightweight equivalent.
- `OrderService.calculatePromotionDiscount` had a monolith TODO for service-specific `FREE_SERVICE` discounts; order-service preserves that baseline.
- `RefundService.processRefundWithGateway` simulated provider refund success in the monolith; payment-service keeps provider-bound behavior at the same maturity level.
- OAuth2, Firebase phone verification, VNPay, MoMo, and MQTT remain credential/protocol dependent.
- Redis token blacklist/session invalidation is represented by DB refresh tokens and in-memory temp tokens for parity; Redis is not required to match the current monolith behavior.
- Firebase Admin initialization and production FCM credentials for `notification-service`.
- Full production settlement/reconciliation and real device fleet hardening are outside the monolith parity target.
