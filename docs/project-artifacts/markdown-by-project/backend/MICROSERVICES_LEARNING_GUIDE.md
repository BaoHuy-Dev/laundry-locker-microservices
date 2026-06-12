# Microservices Learning Guide

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

Tài liệu này giải thích microservices từ cơ bản đến nâng cao, dùng chính dự án `laundry-locker-microservices` làm ví dụ thực tế.

Workspace ví dụ:

```text
D:\BigProject\laundry-locker-microservices
```

Các module chính:

```text
api-gateway
discovery-server
common-lib
auth-service
user-service
order-service
locker-service
laundry-service
payment-service
notification-service
iot-service
store-service
staff-service
partner-service
loyalty-service
```

## 1. Microservices Là Gì?

Microservices là cách chia một hệ thống lớn thành nhiều service nhỏ, mỗi service sở hữu một miền nghiệp vụ rõ ràng, có code riêng, database/schema riêng, API riêng và có thể deploy độc lập.

Trong dự án này:

| Service | Trách nhiệm |
|---|---|
| `auth-service` | Đăng ký, đăng nhập, JWT, OTP, admin auth |
| `user-service` | Hồ sơ người dùng, role, permission, FCM token facade |
| `order-service` | Đơn hàng, trạng thái, rating, complaint, promotion |
| `payment-service` | Thanh toán, refund, VNPay/MoMo parity |
| `notification-service` | Notification, FCM token, WebSocket |
| `locker-service` | Locker, box, report, open/reserve/release box |
| `laundry-service` | Danh mục dịch vụ giặt |
| `store-service` | Cửa hàng |
| `iot-service` | Sự kiện thiết bị, unlock, MQTT facade |
| `partner-service` | Đối tác, access code |
| `staff-service` | Luồng nhân viên |
| `loyalty-service` | Điểm, stamp, reward |

Thay vì một app duy nhất xử lý tất cả, mỗi service xử lý một phần nhỏ và giao tiếp với service khác qua REST hoặc message queue.

## 2. Vì Sao Không Dùng Một Monolith?

Monolith có ưu điểm:

- Dễ bắt đầu.
- Một codebase, một database, một deployment.
- Gọi hàm nội bộ đơn giản.

Nhưng khi hệ thống lớn lên:

- Code dễ phụ thuộc chéo.
- Một lỗi nhỏ có thể ảnh hưởng toàn hệ thống.
- Khó scale riêng từng phần.
- Database nhiều foreign key chéo domain.
- Team khó làm việc độc lập.

Ví dụ monolith cũ `laundry-locker-backend` có nhiều module:

```text
module/auth
module/user
module/order
module/payment
module/notification
module/locker
...
```

Đó là modular monolith: code đã chia module, nhưng vẫn chạy chung một process và thường dùng chung database/domain model.

Microservices mới tách thành các app riêng:

```text
auth-service
user-service
order-service
payment-service
notification-service
...
```

## 3. Modular Monolith Vs Microservices

| Tiêu chí | Modular Monolith | Microservices |
|---|---|---|
| Deploy | Một app | Nhiều app |
| Database | Thường dùng chung | Mỗi service sở hữu DB/schema riêng |
| Giao tiếp | Gọi method/class nội bộ | REST, OpenFeign, RabbitMQ |
| Entity | Có thể share trực tiếp | Không share entity |
| Transaction | Dễ dùng local DB transaction | Cần Saga/eventual consistency |
| Debug | Dễ hơn | Khó hơn, cần logs/tracing |
| Scale | Scale cả app | Scale từng service |

Trong dự án này, rule quan trọng là:

```text
Không share entity giữa service.
Không dùng cross-service JPA foreign key.
Reference chéo service chuyển thành ID.
```

Ví dụ:

```text
Order không giữ User entity.
Order chỉ giữ userId.

Payment không giữ Order entity.
Payment chỉ giữ orderId.

Notification không giữ User entity.
Notification chỉ giữ userId.
```

## 4. Kiến Trúc Tổng Quan Dự Án

Luồng frontend đi qua gateway:

```text
Frontend / Mobile App
        |
        v
api-gateway :8080
        |
        +--> auth-service :8081
        +--> user-service :8082
        +--> order-service :8083
        +--> locker-service :8084
        +--> laundry-service :8085
        +--> payment-service :8086
        +--> notification-service :8087
        +--> ...
```

Service discovery:

```text
Service A ----register----> discovery-server :8761
Service B ----register----> discovery-server :8761
api-gateway --find service-> discovery-server :8761
```

Async event:

```text
order-service
   |
   | publish order.status.changed
   v
RabbitMQ exchange: laundry.events
   |
   v
notification-service
```

## 5. api-gateway Là Gì?

`api-gateway` là cửa vào duy nhất cho client.

Client không nên gọi thẳng:

```text
http://localhost:8083/api/orders
http://localhost:8086/api/payments
```

Client nên gọi:

```text
http://localhost:8080/api/orders
http://localhost:8080/api/payments
```

Lợi ích:

- Che giấu topology nội bộ.
- Tập trung auth/JWT/RBAC.
- Route request tới service đúng.
- Dễ đổi service phía sau mà frontend ít bị ảnh hưởng.

Ví dụ route:

| Gateway path | Service nhận |
|---|---|
| `/api/auth/**` | `auth-service` |
| `/api/user/**` | `user-service` |
| `/api/orders/**` | `order-service` |
| `/api/payments/**` | `payment-service` |
| `/api/notifications/**` | `notification-service` |
| `/ws` | `notification-service` |

## 6. discovery-server Là Gì?

`discovery-server` dùng Eureka.

Mục đích:

- Mỗi service tự đăng ký tên và địa chỉ.
- Gateway và Feign client gọi service bằng tên logic.
- Không hard-code IP container.

Ví dụ thay vì gọi:

```text
http://localhost:8084/api/lockers/1
```

Service gọi:

```text
lb://locker-service
```

Trong Java, Feign client có thể khai báo:

```java
@FeignClient(name = "locker-service")
public interface LockerClient {
  @GetMapping("/api/lockers/{lockerId}/boxes/available")
  ApiResponse<List<Map<String, Object>>> availableBoxes(@PathVariable Long lockerId);
}
```

Ví dụ thực tế trong dự án:

```text
partner-service/src/main/java/.../partner/client/LockerClient.java
```

`partner-service` không cần biết `locker-service` chạy port nào. Nó chỉ biết tên service.

## 7. common-lib Nên Chứa Gì?

`common-lib` là thư viện dùng chung.

Nên chứa:

- DTO thật sự dùng chung.
- Response wrapper.
- Exception chung.
- Event DTO.
- Utility không phụ thuộc domain.

Ví dụ trong dự án:

```text
common-lib/src/main/java/com/huynqb/laundrylocker/common/dto/ApiResponse.java
common-lib/src/main/java/com/huynqb/laundrylocker/common/dto/UserSummary.java
common-lib/src/main/java/com/huynqb/laundrylocker/common/dto/OrderSummary.java
common-lib/src/main/java/com/huynqb/laundrylocker/common/dto/NotificationRequest.java
common-lib/src/main/java/com/huynqb/laundrylocker/common/event/DomainEvent.java
common-lib/src/main/java/com/huynqb/laundrylocker/common/exception/BusinessException.java
```

Không nên chứa:

- `User` entity.
- `Order` entity.
- `Payment` entity.
- Repository.
- Service nghiệp vụ.

Lý do: nếu share entity, các service sẽ bị dính chặt vào model của nhau, làm mất ý nghĩa microservices.

## 8. Database Per Service

Mỗi service có database riêng.

Ví dụ:

| Service | Database | Schema |
|---|---|---|
| `auth-service` | `auth_db` | `auth_schema` |
| `user-service` | `user_db` | `user_schema` |
| `order-service` | `order_db` | `order_schema` |
| `payment-service` | `payment_db` | `payment_schema` |
| `notification-service` | `notification_db` | `notification_schema` |

Trong local Docker, tất cả database nằm trong một PostgreSQL container, nhưng vẫn là database/schema riêng.

File tạo database:

```text
docker/postgres/init-databases.sql
```

Ví dụ:

```sql
CREATE USER order_user WITH PASSWORD 'order_pass';
CREATE DATABASE order_db OWNER order_user;
```

Mỗi service chạy Flyway migration riêng:

```text
order-service/src/main/resources/db/migration
payment-service/src/main/resources/db/migration
notification-service/src/main/resources/db/migration
```

## 9. Vì Sao Không Dùng Cross-Service FK?

Trong monolith, có thể có:

```text
payment.order_id -> orders.id
orders.user_id -> users.id
```

Trong microservices, `payment-service` không được FK trực tiếp tới bảng `orders` của `order-service`.

Sai:

```text
payment_db.payments.order_id FK -> order_db.orders.id
```

Đúng:

```text
payment_db.payments.order_id BIGINT
```

`orderId` chỉ là scalar ID. Nếu cần lấy thông tin order, `payment-service` gọi `order-service` qua REST/OpenFeign.

Lý do:

- Mỗi service phải tự deploy DB schema riêng.
- Không service nào được kiểm soát bảng của service khác.
- Tránh coupling ở tầng database.

## 10. REST Và OpenFeign

REST dùng cho request cần response ngay.

Ví dụ:

```text
partner-service cần lấy danh sách box trống từ locker-service.
```

Dùng OpenFeign:

```java
@FeignClient(name = "locker-service")
public interface LockerClient {
  @GetMapping("/api/lockers/{lockerId}/boxes/available")
  ApiResponse<List<Map<String, Object>>> availableBoxes(@PathVariable Long lockerId);
}
```

Khi nào dùng REST/OpenFeign?

- Cần validate dữ liệu ngay.
- Cần response để tiếp tục xử lý.
- Cần query dữ liệu service khác.

Ví dụ:

| Use case | Giao tiếp |
|---|---|
| `auth-service` cần tạo user profile | Feign tới `user-service` |
| `order-service` cần lấy laundry service price | Feign tới `laundry-service` |
| `partner-service` cần lấy locker box trống | Feign tới `locker-service` |
| `store-service` cần lấy rating | Feign tới `order-service` |

## 11. RabbitMQ Và Event-Driven

RabbitMQ dùng cho xử lý bất đồng bộ.

Ví dụ:

```text
order-service update trạng thái order
        |
        | publish event order.status.changed
        v
RabbitMQ
        |
        v
notification-service tạo notification
```

Khi nào dùng event?

- Không cần response ngay.
- Muốn giảm coupling.
- Một hành động có nhiều side effect.
- Chấp nhận eventual consistency.

Ví dụ event trong dự án:

```text
order.created
order.status.changed
payment.completed
payment.failed
notification.requested
locker.box.opened
iot.device.status.changed
```

REST vs Event:

| Câu hỏi | REST/OpenFeign | RabbitMQ event |
|---|---|---|
| Có cần response ngay không? | Có | Không |
| Có muốn xử lý nền không? | Không | Có |
| Có nhiều service cùng nghe không? | Không phù hợp | Phù hợp |
| Có cần transaction tức thì không? | Dễ hơn | Cần eventual consistency |

## 12. Transaction Trong Microservices

Trong monolith:

```java
@Transactional
createOrder() {
  save order;
  save payment;
  save notification;
}
```

Một transaction DB có thể bao nhiều bảng.

Trong microservices:

```text
order-service chỉ transaction trong order_db
payment-service chỉ transaction trong payment_db
notification-service chỉ transaction trong notification_db
```

Không thể dùng một transaction JPA đơn giản cho nhiều database/service.

Giải pháp thường dùng:

- Saga.
- Outbox pattern.
- Idempotent consumer.
- Retry.
- Dead-letter queue.

Dự án hiện tại đang ở mức feature parity, chưa production-hardening đầy đủ các pattern này.

## 13. Saga Pattern

Saga là cách chia một transaction lớn thành nhiều bước nhỏ.

Ví dụ luồng đặt đơn:

```text
1. order-service tạo order
2. locker-service reserve box
3. payment-service tạo payment
4. notification-service gửi notification
```

Nếu bước 3 fail, có thể cần compensation:

```text
payment fail -> order-service mark PAYMENT_FAILED -> locker-service release box
```

Có 2 kiểu Saga:

| Kiểu | Mô tả |
|---|---|
| Choreography | Service publish/listen event, không có coordinator trung tâm |
| Orchestration | Có một service điều phối workflow |

Với dự án này, hướng tự nhiên là choreography qua RabbitMQ.

## 14. Idempotency

Idempotency nghĩa là xử lý cùng một request/event nhiều lần vẫn không gây sai dữ liệu.

Vì sao cần?

- Message queue có thể deliver lại.
- Client có thể retry.
- Network timeout làm client không biết request đã thành công chưa.

Ví dụ:

```text
payment.completed event bị gửi 2 lần
```

`notification-service` không nên tạo 2 notification giống nhau nếu logic nghiệp vụ yêu cầu chỉ một.

Cách xử lý:

- Lưu `eventId`.
- Check event đã xử lý chưa.
- Dùng unique key theo business id.
- Thiết kế handler an toàn khi chạy lại.

## 15. API Gateway Security

Trong dự án, gateway kiểm tra JWT và route admin.

Luồng:

```text
Client login -> auth-service cấp JWT
Client gọi API -> api-gateway verify JWT
Gateway thêm header:
  X-User-Id
  X-Account-Id
  X-User-Roles
Downstream service đọc header
```

Ví dụ:

```text
GET /api/user/profile
Authorization: Bearer <token>
```

Gateway parse token và forward:

```text
X-User-Id: 1
X-User-Roles: USER
```

Admin endpoint:

```text
/api/admin/**
```

Cần role:

```text
ADMIN
```

## 16. Service Ownership

Mỗi service phải sở hữu domain của nó.

Ví dụ:

`user-service` sở hữu:

- user profile.
- roles.
- permissions.

`auth-service` sở hữu:

- auth account.
- password hash.
- refresh token.
- OTP.
- JWT issuing.

`order-service` sở hữu:

- order.
- order details.
- order status history.
- rating.
- complaint.
- promotion.

Không nên để `auth-service` sửa trực tiếp bảng user. Nó gọi `user-service`.

## 17. API Public, Internal, Admin

Trong dự án có 3 nhóm API:

Public/user API:

```text
/api/auth/**
/api/user/**
/api/orders/**
/api/payments/**
```

Admin API:

```text
/api/admin/**
```

Internal API:

```text
/internal/**
```

Ý nghĩa:

| Nhóm | Ai gọi |
|---|---|
| Public/user | Frontend/mobile |
| Admin | Admin frontend |
| Internal | Service khác |

Ví dụ:

```text
POST /internal/notifications
```

Dùng để service khác yêu cầu tạo notification. Frontend bình thường không nên gọi trực tiếp nhóm này.

## 18. WebSocket Trong Microservices

`notification-service` sở hữu WebSocket/STOMP.

Gateway route:

```text
ws://localhost:8080/ws
```

Các destination:

```text
/topic/notifications
/user/{userId}/queue/notifications
```

Luồng:

```text
Client connect /ws
Client subscribe /topic/notifications
notification-service broadcast message
Client nhận real-time notification
```

## 19. Docker Compose Trong Dự Án

File:

```text
docker-compose.yml
```

Chạy:

```powershell
docker compose up --build -d
```

Các container:

```text
ll-ms-postgres
ll-ms-rabbitmq
ll-ms-discovery-server
ll-ms-api-gateway
ll-ms-auth-service
...
```

Kiểm tra:

```powershell
docker compose ps
docker compose logs -f api-gateway
```

Stop:

```powershell
docker compose down
```

Reset data:

```powershell
docker compose down -v
```

## 20. Observability Cơ Bản

Hiện tại dự án có Actuator health/info.

Ví dụ:

```text
http://localhost:8080/actuator/health
http://localhost:8081/actuator/health
http://localhost:8083/actuator/health
```

Eureka UI:

```text
http://localhost:8761
```

RabbitMQ UI:

```text
http://localhost:15672
guest / guest
```

Production nên bổ sung:

- Centralized logging.
- Distributed tracing.
- Metrics dashboard.
- Alerting.

Ví dụ tool:

```text
OpenTelemetry
Prometheus
Grafana
Loki
ELK
Jaeger
Tempo
```

## 21. Resilience

Trong microservices, network có thể lỗi.

Các lỗi thường gặp:

- Service chưa register Eureka.
- Timeout.
- RabbitMQ tạm down.
- PostgreSQL slow.
- Feign call fail.

Pattern cần học:

| Pattern | Mục đích |
|---|---|
| Timeout | Không chờ vô hạn |
| Retry | Thử lại lỗi tạm thời |
| Circuit Breaker | Ngắt call tới service đang lỗi |
| Bulkhead | Cô lập resource |
| Rate Limit | Chặn request quá nhiều |
| Fallback | Trả response thay thế |

Ví dụ:

```text
store-service gọi order-service lấy ratings.
Nếu order-service down, store-service có thể trả ratings rỗng thay vì fail toàn API store.
```

## 22. Versioning API

Khi service đã có client dùng thật, không nên đổi API tùy tiện.

Cách version:

```text
/api/v1/orders
/api/v2/orders
```

Hoặc dùng header:

```text
Accept: application/vnd.laundry.v1+json
```

Dự án hiện tại giữ endpoint cũ để backward compatibility với monolith/frontend.

## 23. Testing Strategy

Các tầng test:

| Loại test | Mục đích |
|---|---|
| Unit test | Test logic trong một class |
| Repository test | Test query/Flyway/schema |
| Controller test | Test API contract |
| Contract test | Đảm bảo service gọi nhau đúng contract |
| Integration test | Test với DB/RabbitMQ |
| E2E test | Test luồng qua api-gateway |

Ví dụ E2E trong dự án:

```text
register/login
create store
create laundry service
create locker/box
create order
update order status
create payment
send notification
save FCM token
admin dashboard
```

Guide chạy test:

```text
RUN_AND_TEST_GUIDE.md
```

## 24. Data Migration

Dự án có template:

```text
docker/postgres/migrate-from-monolith-template.sql
```

Ý tưởng:

```text
monolith_db.users -> user_db.user_profiles
monolith_db.orders -> order_db.orders
monolith_db.payments -> payment_db.payments
monolith_db.notifications -> notification_db.notifications
```

Nguyên tắc:

- Không copy FK chéo service.
- Chỉ copy ID scalar.
- Kiểm tra row count sau migration.
- Chạy staging trước production.

Ví dụ:

```text
orders.user_id vẫn giữ là userId trong order-service.
Không tạo FK từ order_db tới user_db.
```

## 25. Anti-Patterns Cần Tránh

Không nên:

- Share entity qua `common-lib`.
- Dùng chung một database cho tất cả service ở production.
- Gọi vòng tròn service A -> B -> C -> A.
- Dùng Feign cho mọi thứ, kể cả side effect async.
- Để frontend gọi trực tiếp từng service.
- Để service này update DB của service khác.
- Tách quá nhỏ khi domain chưa rõ.
- Không có logging/tracing.
- Không có retry/timeout.

Ví dụ xấu:

```text
payment-service update trực tiếp bảng orders.
```

Ví dụ đúng:

```text
payment-service publish payment.completed.
order-service nghe event hoặc nhận internal API để update trạng thái.
```

## 26. Khi Nào Tách Service?

Nên tách khi:

- Domain rõ ràng.
- Có database ownership rõ.
- Team có thể chịu chi phí vận hành.
- Service có nhu cầu scale riêng.
- Có boundary nghiệp vụ tự nhiên.

Không nên tách chỉ vì:

- Muốn "ngầu".
- Project còn nhỏ.
- Chưa hiểu domain.
- Chưa có CI/CD/logging cơ bản.

Trong dự án này, các boundary khá tự nhiên:

```text
auth, user, order, payment, notification, locker, laundry, store
```

## 27. Cấp Độ Thành Thạo Microservices

Level 1: Hiểu khái niệm

- Service độc lập.
- API gateway.
- Database per service.
- REST vs event.

Level 2: Implement được

- Spring Boot service.
- OpenFeign client.
- RabbitMQ publisher/listener.
- Flyway migration.
- Dockerfile.

Level 3: Vận hành được

- Docker Compose.
- Health check.
- Logs.
- Debug service down.
- Port conflict.
- DB migration.

Level 4: Production-ready

- Observability.
- Distributed tracing.
- Circuit breaker.
- Retry.
- Rate limit.
- Secrets management.
- CI/CD.
- Contract testing.

Level 5: Architecture

- Bounded context.
- Saga.
- Outbox.
- Event versioning.
- Backward compatibility.
- Data ownership.
- Migration strategy.

## 28. Lộ Trình Học Theo Dự Án Này

Tuần 1: Chạy hệ thống

- Đọc `README.md`.
- Chạy `docker compose up --build -d`.
- Mở Eureka, RabbitMQ.
- Test theo `RUN_AND_TEST_GUIDE.md`.

Tuần 2: Hiểu gateway và auth

- Đọc `api-gateway`.
- Đọc `auth-service`.
- Test JWT.
- Test admin role.

Tuần 3: Hiểu service-to-service

- Đọc Feign client:
  - `order-service/client`
  - `partner-service/client`
  - `store-service/client`
- Trace một request qua nhiều service.

Tuần 4: Hiểu event

- Đọc RabbitMQ config/event classes.
- Test `order.status.changed`.
- Xem RabbitMQ exchange/queue.
- Quan sát notification tạo ra.

Tuần 5: Hiểu data ownership

- Đọc Flyway migrations.
- Kiểm tra từng DB/schema.
- So sánh entity không có FK chéo.

Tuần 6: Nâng cao

- Thêm tracing.
- Thêm retry/circuit breaker.
- Thêm contract test.
- Thiết kế outbox cho event quan trọng.

## 29. Bài Tập Thực Hành

Bài 1: Trace một request

```text
POST /api/orders
```

Trả lời:

- Request đi qua gateway nào?
- Service nào xử lý?
- DB nào được ghi?
- Event nào được publish?
- Service nào nghe event?

Bài 2: Phân biệt REST và event

Với mỗi use case, chọn REST hay RabbitMQ:

```text
auth-service tạo user profile
order-service báo trạng thái thay đổi
payment-service xác nhận thanh toán thành công
store-service lấy rating
notification-service gửi FCM
```

Bài 3: Tìm cross-service reference

Mở entity trong:

```text
order-service
payment-service
notification-service
locker-service
```

Tìm các field:

```text
userId
orderId
storeId
lockerId
boxId
```

Giải thích tại sao chúng không phải JPA relation.

Bài 4: Debug service fail

Tắt một service:

```powershell
docker compose stop notification-service
```

Sau đó:

```powershell
docker compose ps
docker compose logs --tail=100 notification-service
```

Khởi động lại:

```powershell
docker compose start notification-service
```

## 30. Checklist Kiến Trúc Cho Mỗi Service Mới

Khi thêm service mới, cần có:

- `pom.xml`
- `Application.java`
- `application.yml`
- Controller
- Service
- Repository
- Entity riêng
- DTO riêng
- Flyway migration
- Dockerfile
- Route gateway
- Eureka client
- Health endpoint
- DB/schema riêng
- Không share entity
- Không cross-service FK
- Feign client nếu cần sync call
- RabbitMQ event nếu async
- README/API docs

## 31. Tóm Tắt Tư Duy Microservices

Microservices không chỉ là tách folder.

Microservices là:

- Tách trách nhiệm.
- Tách dữ liệu.
- Tách deploy.
- Giao tiếp qua contract.
- Chấp nhận distributed system complexity.
- Thiết kế để lỗi cục bộ không kéo sập toàn hệ thống.

Trong dự án này, câu cần nhớ là:

```text
Frontend gọi api-gateway.
Service sở hữu domain của nó.
Service khác chỉ giữ ID, không giữ entity.
REST khi cần response ngay.
RabbitMQ khi xử lý bất đồng bộ.
common-lib không chứa entity.
```

