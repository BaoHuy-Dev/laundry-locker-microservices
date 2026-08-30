# PROJECT_FLOW.md — Smart Laundry Locker System

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho
> locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng
> Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`,
`RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

> Tài liệu mô tả **luồng nghiệp vụ** và **luồng kỹ thuật** của toàn bộ hệ thống trong workspace `BigProject`.
> Cập nhật gần nhất: 2026-06-13

---

## 0. Snapshot hiện tại sau Phase 2

- Backend đang chạy qua gateway `:8080` với 10 service nghiệp vụ có source: auth, user, order, locker, payment,
  notification, iot, store, staff, loyalty. `laundry-service` và `partner-service` vẫn là scope dự kiến/reserved, chưa
  có source, đã được skip bằng `docker-compose.override.yml`.
- Locker flow đã hoàn tất Phase 2: cell model `DRONE/STANDARD/XL`, SEND, RENTAL, PIN/QR signed token, RBAC gateway,
  manager endpoints `/api/manage/**`, maintenance endpoints `/api/maintenance/**`, scheduler reminder/cleanup.
- FE admin đã build pass và có trang locker layout, locker list, maintenance.
- Flutter mobile đã có login thật, routing theo role Customer/Manager/Maintenance, quick actions `Thuê tủ`, `Gửi hàng`,
  `Đơn tủ`.
- Drone delivery service, tablet-web cabinet UI, AI/RAG, sensor-driven auto-occupy và tích hợp phần cứng thật vẫn là
  phần tương lai.

Nếu nội dung các phần cũ bên dưới mâu thuẫn với snapshot này, ưu tiên
`laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md` và `LOCKER_FLOW_PLAN.md`.

## 1. Tổng quan các thành phần

| Thành phần                    | Thư mục                                               | Công nghệ                                                                                                                                | Vai trò                                                                                                                                                |
|-------------------------------|-------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| Backend microservices         | `laundry-locker-microservices/`                       | Java 21, Spring Boot 3.5.14, Spring Cloud 2025.0.2, Maven multi-module, PostgreSQL 16, RabbitMQ, Eureka, Spring Cloud Gateway, OpenFeign | Toàn bộ API hệ thống hiện tại: 10 service nghiệp vụ có source + gateway/discovery; `laundry-service` và `partner-service` đang reserved/missing-source |
| Frontend Web (Admin/Customer) | `laundry-locker-frontend/fe/`                         | React 19 + Vite 7 + TypeScript, Redux Toolkit, Ant Design 6 + Radix UI + Tailwind 4, STOMP/SockJS                                        | Web app gọi API Gateway (port 3000)                                                                                                                    |
| Landing Page                  | `laundry-locker-frontend/landingPage/`                | Vite SPA riêng                                                                                                                           | Trang giới thiệu                                                                                                                                       |
| Mobile (React Native cũ)      | `laundry-locker-frontend/mobile/`                     | React Native/Expo + Firebase                                                                                                             | App mobile thế hệ trước (tham khảo)                                                                                                                    |
| Firmware tủ                   | `laundry-locker-frontend/iot/`                        | PlatformIO/Arduino + tablet-web                                                                                                          | Firmware ESP/Arduino cho tủ                                                                                                                            |
| Mobile App (chính)            | `smart-laundry-locker-mobile/`                        | Flutter (Dart), envied, dio, Riverpod/Bloc, Firebase                                                                                     | App khách hàng (Android/iOS)                                                                                                                           |
| IoT Locker                    | `smart-locker-iot/`                                   | Python (uv), paho-MQTT, pyserial, Arduino RS485, PostgreSQL/SQLite                                                                       | Phần mềm chạy trên Raspberry Pi điều khiển tủ thật                                                                                                     |
| File cấu hình tham khảo       | `Application.txt`, `env.txt`, `pro.txt`, `Host *.txt` | —                                                                                                                                        | Config của **monolith cũ** (`laundry-locker-backend`) + thông tin DB server `<AZURE_VM_IP>` (deploy)                                                  |

> ⚠️ Các file `.txt` ở gốc workspace chứa secret (SMTP, OAuth2, VNPay, MoMo, Supabase, Azure...). **Không commit**
> chúng. Trong tài liệu này mọi secret được che `****`.

---

## 2. Kiến trúc backend microservices

### 2.1 Danh sách service + port

| Service              |     Port | Database (schema)                     | Vai trò                                                                                                                     |
|----------------------|---------:|---------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| discovery-server     |     8761 | —                                     | Eureka service registry                                                                                                     |
| **api-gateway**      | **8080** | —                                     | Cổng vào duy nhất, JWT/RBAC, route theo path                                                                                |
| auth-service         |     8081 | auth_db (auth_schema)                 | Đăng ký/đăng nhập, JWT, OTP phone/email, reset mật khẩu, admin auth                                                         |
| user-service         |     8082 | user_db (user_schema)                 | Hồ sơ người dùng, roles/permissions                                                                                         |
| order-service        |     8083 | order_db (order_schema)               | Đơn giặt, trạng thái, đánh giá, khiếu nại, khuyến mãi, dashboard admin                                                      |
| locker-service       |     8084 | locker_db (locker_schema)             | Tủ locker, ô tủ (box), reserve/release/open                                                                                 |
| ~~laundry-service~~  |     8085 | laundry_db                            | ⚠️ **Chưa có source code** — chỉ có trong README/docker-compose; đã bị loại khỏi compose bằng `docker-compose.override.yml` |
| payment-service      |     8086 | payment_db (payment_schema)           | Thanh toán, hoàn tiền, VNPay/MoMo                                                                                           |
| notification-service |     8087 | notification_db (notification_schema) | Thông báo, FCM push, WebSocket/STOMP                                                                                        |
| iot-service          |     8088 | iot_db (iot_schema)                   | Thiết bị IoT, verify PIN, lệnh mở tủ qua MQTT                                                                               |
| store-service        |     8089 | store_db (store_schema)               | Cửa hàng                                                                                                                    |
| staff-service        |     8090 | staff_db (staff_schema)               | Nhân viên, phân công đơn                                                                                                    |
| ~~partner-service~~  |     8091 | partner_db                            | ⚠️ **Chưa có source code** — như laundry-service                                                                            |
| loyalty-service      |     8092 | loyalty_db (loyalty_schema)           | Điểm thưởng, tem, đổi quà                                                                                                   |

Hạ tầng (docker-compose):

- **PostgreSQL 16** — container `ll-ms-postgres`, host port **15432** (user `postgres`/`****`). 12 database được tạo bởi
  `docker/postgres/init-databases.sql`, mỗi service một DB + một Flyway schema riêng. Không có FK chéo service.
- **RabbitMQ 3 (management)** — container `ll-ms-rabbitmq`, port **5672** (AMQP) + **15672** (UI, `guest`/`****`).

### 2.2 API base URL

- Client (web/mobile) **chỉ gọi qua Gateway**: `http://localhost:8080`
- Port 8081–8092 chỉ expose để debug local.
- Gateway xác thực JWT, kiểm tra RBAC cho path `/api/admin/**`, rồi forward kèm header định danh: `X-User-Id`,
  `X-Account-Id`, `X-User-Roles`.
- Đường `/internal/**` chỉ dành cho gọi service-to-service (OpenFeign).

### 2.3 Bảng route Gateway → service

| Gateway path                                                                                                                                                              | Service đích         |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------|
| `/api/auth/**`, `/api/admin/auth/**`                                                                                                                                      | auth-service         |
| `/api/users/**`, `/api/user/**`, `/api/admin/users/**`, `/api/admin/audit-logs/**`                                                                                        | user-service         |
| `/api/orders/**`, `/api/manage/orders/**`, `/api/admin/orders/**`, `/api/admin/scheduler/**`, `/api/admin/dashboard/**`, `/api/promotions/**`, `/api/admin/promotions/**` | order-service        |
| `/api/lockers/**`, `/api/boxes/**`, `/api/manage/lockers/**`, `/api/maintenance/**`, `/api/admin/lockers/**`                                                              | locker-service       |
| `/api/payments/**`, `/api/admin/payments/**`                                                                                                                              | payment-service      |
| `/api/notifications/**`, `/api/admin/notifications/**`, `/ws`, `/ws/**`                                                                                                   | notification-service |
| `/api/iot/**`                                                                                                                                                             | iot-service          |
| `/api/stores/**`, `/api/admin/stores/**`                                                                                                                                  | store-service        |
| `/api/staff/**`                                                                                                                                                           | staff-service        |
| `/api/loyalty/**`, `/api/admin/loyalty/**`                                                                                                                                | loyalty-service      |

(Route cho laundry-service/partner-service chưa khai trong `api-gateway/application.yml` — nhất quán với việc 2 module
chưa tồn tại.)

Gateway RBAC hiện tại:

- `/api/admin/**` yêu cầu `ADMIN`.
- `/api/manage/**` yêu cầu `MANAGER` hoặc `ADMIN`.
- `/api/maintenance/**` yêu cầu `MAINTENANCE` hoặc `ADMIN`.
- `/internal/**` bị chặn khi đi qua gateway; chỉ dùng cho service-to-service/Feign hoặc debug trực tiếp service port.

### 2.4 Giao tiếp bất đồng bộ (RabbitMQ)

Exchange topic: `laundry.events`. Các event:

```
order.created             order-service phát khi tạo đơn
order.status.changed      order-service phát khi đổi trạng thái
payment.completed         payment-service phát khi thanh toán xong
payment.failed            payment-service phát khi thanh toán lỗi
notification.requested    các service yêu cầu gửi thông báo
locker.box.opened         locker-service phát khi ô tủ được mở
locker.box.fault          locker-service phát khi ô tủ bị báo hỏng
iot.device.status.changed iot-service phát khi thiết bị đổi trạng thái
```

`notification-service` lắng nghe các event order/payment và tự tạo thông báo (DB + FCM + WebSocket).

---

## 3. Luồng nghiệp vụ

### 3.1 Đăng ký / Đăng nhập

```
Mobile/Web → API Gateway (8080) → auth-service (8081) → auth_db
                                        │
                                        ├─ BCrypt hash mật khẩu, tạo tài khoản
                                        ├─ Feign → user-service: provision hồ sơ user
                                        ├─ OTP phone/email (SMTP) nếu cần xác thực
                                        └─ Trả về accessToken (JWT) + refreshToken
```

1. Client POST `/api/auth/register` (email, phone, password, roles).
2. auth-service tạo account trong `auth_db`, gọi `user-service /internal/users` để tạo profile.
3. Client POST `/api/auth/login` với body `{"identifier": "<email hoặc phone>", "password": "..."}` → nhận
   `accessToken` + `refreshToken`.
4. Mọi request sau gắn `Authorization: Bearer <token>`; Gateway verify chữ ký JWT (HS256, secret chung) rồi forward kèm
   `X-User-Id`, `X-User-Roles`.
5. Refresh: POST `/api/auth/refresh`. Logout: thu hồi refresh token trong DB.
6. Admin đăng nhập qua `/api/admin/auth/**`; Gateway chặn `/api/admin/**` nếu role không đủ.

### 3.2 Tạo đơn giặt

```
Mobile/Web → Gateway → order-service (8083) → order_db
                          │
                          ├─ Feign → user-service: validate user
                          ├─ Feign → locker-service: reserve box (ô gửi đồ)
                          ├─ Lưu order + order_details + status_history, sinh PIN
                          └─ RabbitMQ: order.created → notification-service
```

1. Khách chọn cửa hàng, dịch vụ, tủ locker.
2. POST `/api/orders` với `storeId`, `lockerId`, `sendBoxId`, danh sách item.
3. order-service giữ chỗ box qua locker-service, tính giá (kèm promotion nếu có), lưu đơn, phát event `order.created`.
4. Khách nhận PIN để mở ô tủ bỏ đồ vào.
5. Vòng đời trạng thái:
   `INITIALIZED → STORING (khách bỏ đồ) → COLLECTED (staff lấy) → PROCESSING → READY → RETURNED (bỏ lại tủ, PIN mới) → COMPLETED` (
   hoặc `CANCELED`). Các endpoint staff: `/collect`, `/weight`, `/process`, `/ready`, `/return`, `/checkout`.
6. Scheduler trong order-service: tự hủy đơn không xác nhận, nhả box sau hoàn tất, nhắc lấy đồ, tính phí quá giờ (mặc
   định 24h, 500đ/h, trần 50.000đ).

### 3.3 Thanh toán

```
Mobile/Web → Gateway → payment-service (8086) → payment_db
                          │
                          ├─ CASH: tạo payment, chờ staff xác nhận
                          ├─ VNPay: tạo URL sandbox → user thanh toán → VNPay gọi
                          │    /api/payments/vnpay/return + /ipn (verify HMAC chữ ký)
                          ├─ MoMo: tạo request → MoMo gọi /api/payments/momo/callback
                          └─ Khi COMPLETED: RabbitMQ payment.completed
                                  ├→ order-service: cập nhật trạng thái đơn
                                  ├→ notification-service: báo cho user
                                  └→ loyalty-service: cộng điểm
```

1. POST `/api/payments` với `orderId`, `amount`, `method` (CASH/VNPAY/MOMO).
2. Với cổng online: payment-service ký request (HMAC secret sandbox), trả `payUrl` cho client redirect.
3. Cổng thanh toán gọi ngược về return/IPN URL; service verify chữ ký rồi cập nhật trạng thái.
4. `payment.completed`/`payment.failed` được phát lên RabbitMQ cho các service liên quan.
5. Hoàn tiền (refund) và cash-flow report nằm trong payment-service (`/api/admin/payments/**`).

### 3.4 Locker / Mở tủ / Nhận đồ

```
Khách tại tủ → nhập PIN trên màn hình tủ (hoặc app)
   │
   ├─ App:  Mobile → Gateway → iot-service /api/iot/verify-pin
   └─ Tủ:   RPi (smart-locker-iot) → MQTT broker → iot-service
                                          │
                                          ├─ Verify PIN/access-code với order/locker
                                          ├─ MQTT publish lệnh mở: cabinet/{lockerId}/command/open
                                          ▼
                          RPi nhận lệnh → Serial RS485 → Arduino → servo mở ô tủ
                                          │
                          RPi báo lại:  cabinet/{name}/heartbeat
                                        cabinet/{name}/locker/{id}/status
                                        cabinet/{name}/command/{id}/result
                                          ├─ event locker.box.opened / iot.device.status.changed
                                          ├─ locker-service: cập nhật trạng thái box
                                          └─ order-service: chuyển trạng thái đơn
```

- **Gửi đồ**: sau khi tạo đơn, khách nhập PIN → ô tủ mở → bỏ đồ → đóng → khách xác nhận (`/confirm`) → đơn `STORING`.
- **Staff lấy đồ**: staff dùng access code hoặc master PIN → mở ô → `/collect` → đơn `COLLECTED`, box gửi được nhả.
- **Trả đồ**: staff bỏ đồ sạch vào ô (`/return?boxId=`), đơn `RETURNED`, PIN mới sinh ra, khách nhận thông báo.
- **Nhận đồ**: khách nhập PIN → mở ô → lấy đồ → `/complete` → đơn `COMPLETED` (tự cộng phí quá giờ nếu trễ) → box được
  nhả.

### 3.5 Notification

```
order/payment/iot events → RabbitMQ (laundry.events) → notification-service (8087)
                                                            │
                                                            ├─ Lưu notification vào notification_db
                                                            ├─ FCM push → Mobile (token đăng ký qua /api/notifications/fcm-token)
                                                            └─ WebSocket/STOMP (/ws) → Web frontend realtime
```

- Mobile đăng ký FCM token sau khi login.
- Web frontend kết nối SockJS/STOMP tới `http://localhost:8080/ws` (route qua gateway) để nhận realtime.
- Email (SMTP) dùng cho OTP/verify trong auth-service.

### 3.6 Admin / Staff

```
Web Admin → Gateway (check role ADMIN qua JWT) → service tương ứng
   ├─ /api/admin/dashboard/**  → order-service (thống kê tổng quan)
   ├─ /api/admin/users/**      → user-service
   ├─ /api/admin/orders/**     → order-service
   ├─ /api/admin/payments/**   → payment-service (doanh thu, refund)
   ├─ /api/admin/lockers/**    → locker-service
   ├─ /api/admin/stores/**     → store-service
   └─ /api/admin/loyalty/**    → loyalty-service

Staff (mobile/web) → /api/staff/** → staff-service → Feign → order-service
   ├─ Xem đơn được phân công
   ├─ Cập nhật trạng thái đơn (collect/process/ready/return/checkout)
   └─ Mở tủ bằng access code / master PIN
```

### 3.7 Mobile gọi backend

- Flutter app dùng `dio` với base URL từ `.env` (envied): Android emulator dùng `http://10.0.2.2:8080` (alias localhost
  của host).
- Luồng: login → lưu JWT (flutter_secure_storage) → interceptor gắn Bearer token → gọi `/api/orders`,
  `/api/lockers`... → FCM nhận push.
- ⚠️ Lưu ý code-gen: `env_config.dart` đọc biến `API_BASE_URL` (file `.env` gốc chỉ khai `API_URL` — đã bổ sung
  `API_BASE_URL` cho khớp). Sau khi đổi `.env` phải chạy `dart run build_runner build --delete-conflicting-outputs` để
  regenerate `env_config.g.dart`.

### 3.8 Web frontend gọi backend

- React app (Vite, port **3000** theo `vite.config.ts`) đọc `VITE_API_BASE_URL=http://localhost:8080` từ `fe/.env` → mọi
  request đi qua Gateway.
- Có mock-data mode (`VITE_ENABLE_MOCK_DATA=true`) chạy không cần backend.
- Realtime qua `@stomp/stompjs` + `sockjs-client` → `/ws`.

### 3.9 Sơ đồ tổng

```
┌─────────┐   ┌─────────┐                ┌──────────────┐
│ Mobile  │   │ Web FE  │                │ RPi + Arduino│
│(Flutter)│   │(React)  │                │ (IoT locker) │
└────┬────┘   └────┬────┘                └──────┬───────┘
     │ REST/FCM    │ REST/WS :3000              │ MQTT/Serial
     ▼             ▼                            ▼
   ┌─────────────────────┐              ┌──────────────┐
   │  API GATEWAY :8080  │              │ MQTT Broker  │
   │  (JWT + RBAC)       │              │ (HiveMQ pub) │
   └──────────┬──────────┘              └──────┬───────┘
              │ lb:// (Eureka :8761)           │
   ┌──────────┴───────────────────────────────┴──────────┐
   │ auth:8081  user:8082  order:8083  locker:8084       │
   │ payment:8086  notification:8087  iot:8088           │
   │ store:8089  staff:8090  loyalty:8092                │
   └───────┬──────────────────────────┬───────────────────┘
           │ JDBC (mỗi service 1 DB)  │ AMQP (laundry.events)
           ▼                          ▼
   ┌──────────────┐           ┌──────────────┐
   │ PostgreSQL16 │           │  RabbitMQ    │
   │ :15432       │           │ :5672/:15672 │
   └──────────────┘           └──────────────┘
```

---

## 4. Luồng kỹ thuật — chạy hệ thống

### 4.1 Thứ tự khởi động đúng

1. **PostgreSQL + RabbitMQ** (docker compose) — phải healthy trước.
2. **discovery-server** (Eureka 8761).
3. **api-gateway** (8080).
4. **10 service nghiệp vụ** — thứ tự bất kỳ, tự đăng ký Eureka, Flyway tự migrate schema.
5. **Frontend web** (Vite dev 3000) — sau khi gateway sẵn sàng.
6. **Mobile/IoT** — tùy chọn, cần emulator/phần cứng.

### 4.2 Biến môi trường chính (mỗi service)

| Biến                                   | Ý nghĩa                           | Giá trị local                                             |
|----------------------------------------|-----------------------------------|-----------------------------------------------------------|
| `SERVER_PORT`                          | Port service                      | 8081–8092                                                 |
| `SPRING_DATASOURCE_URL`                | JDBC tới DB riêng                 | `jdbc:postgresql://postgres:5432/<svc>_db` (trong Docker) |
| `SPRING_DATASOURCE_USERNAME/PASSWORD`  | User DB riêng từng service        | `<svc>_user` / `****`                                     |
| `SPRING_RABBITMQ_HOST`                 | RabbitMQ                          | `rabbitmq` (Docker) / `localhost`                         |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka                            | `http://discovery-server:8761/eureka`                     |
| `APP_SECURITY_JWT_SECRET`              | Secret JWT chung gateway + auth   | `****` (đổi khi production)                               |
| `SPRING_MAIL_*`                        | SMTP cho OTP email (auth-service) | `****`                                                    |
| `mqtt.broker-url` (iot-service)        | MQTT broker                       | `tcp://broker.hivemq.com:1883`                            |

### 4.3 Build & chạy

```powershell
cd G:\BigProject\laundry-locker-microservices
mvn clean package -DskipTests     # build các module backend có source
docker compose up --build -d      # chạy toàn bộ trong Docker
```

Hoặc dùng script tổng ở gốc workspace: `.\run-all.ps1` / `.\stop-all.ps1` (xem `RUN_ALL_GUIDE.md`).

### 4.4 URL hữu ích

| URL                    | Mô tả                                |
|------------------------|--------------------------------------|
| http://localhost:8080  | API Gateway (mọi client gọi vào đây) |
| http://localhost:8761  | Eureka dashboard                     |
| http://localhost:15672 | RabbitMQ UI (`guest`/`****`)         |
| localhost:15432        | PostgreSQL (`postgres`/`****`)       |
| http://localhost:3000  | Frontend web (Vite dev)              |

### 4.5 Môi trường deploy hiện có (tham khảo)

- Server `<AZURE_VM_IP>` (theo file Host): PostgreSQL của microservices expose port 15432 — cấu trúc DB giống local.
- Monolith cũ từng deploy với Supabase (Postgres) + Upstash (Redis) + Azure — xem `pro.txt` (secret nằm trong file,
  không in lại ở đây).
- Monolith cũ (`Application.txt`) dùng thêm Redis, OAuth2 (Google/GitHub/Facebook/Zalo), HiveMQ public broker — các phần
  OAuth2/Redis **chưa** chuyển hết sang microservices (xem mục Parity Notes trong README microservices).
