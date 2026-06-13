# RUN_RESULT.md — Kết quả chạy toàn bộ hệ thống

> Thời điểm: 2026-06-12 • Máy: Windows 11, workspace `G:\BigProject`

## 1. Tổng kết nhanh

| Hạng mục | Kết quả |
|---|---|
| Build backend (Maven, 12 module) | ✅ PASS (sau khi sửa 3 lỗi compile/config) |
| Backend chạy Docker (14 container) | ✅ Tất cả Up, không restart loop |
| Đăng ký Eureka | ✅ 11/11 app (gateway + 10 service + iot) |
| Smoke test API qua gateway | ✅ register / login / my-orders (JWT) / stores đều OK |
| Frontend web | ✅ Chạy tại http://localhost:3000, trỏ đúng gateway |
| Mobile Flutter | ✅ Môi trường sẵn sàng (Flutter 3.44.2 + AVD Pixel_8, pub get + build_runner đã chạy) — `flutter run` để chạy app |
| IoT Python (Raspberry Pi) | ✅ Môi trường sẵn sàng (uv + Python 3.13.5, deps OK) — chạy thật cần phần cứng Arduino/serial |

## 2. Trạng thái từng service

| Service | Port | Container | Trạng thái |
|---|---:|---|---|
| PostgreSQL 16 | 15432 | ll-ms-postgres | ✅ healthy |
| RabbitMQ 3 | 5672 / 15672 | ll-ms-rabbitmq | ✅ healthy |
| discovery-server (Eureka) | 8761 | ll-ms-discovery-server | ✅ Up |
| api-gateway | **8080** | ll-ms-api-gateway | ✅ Up, health UP |
| auth-service | 8081 | ll-ms-auth-service | ✅ Up + Eureka |
| user-service | 8082 | ll-ms-user-service | ✅ Up + Eureka |
| order-service | 8083 | ll-ms-order-service | ✅ Up + Eureka |
| locker-service | 8084 | ll-ms-locker-service | ✅ Up + Eureka |
| laundry-service | 8085 | — | ❌ **Không có source code** (chỉ có trong README/compose) |
| payment-service | 8086 | ll-ms-payment-service | ✅ Up + Eureka |
| notification-service | 8087 | ll-ms-notification-service | ✅ Up + Eureka |
| iot-service | 8088 | ll-ms-iot-service | ✅ Up + Eureka + kết nối MQTT HiveMQ |
| store-service | 8089 | ll-ms-store-service | ✅ Up + Eureka |
| staff-service | 8090 | ll-ms-staff-service | ✅ Up + Eureka |
| partner-service | 8091 | — | ❌ **Không có source code** |
| loyalty-service | 8092 | ll-ms-loyalty-service | ✅ Up + Eureka |

## 3. URL hệ thống

- **API Gateway**: http://localhost:8080 (mọi client gọi vào đây)
- **Frontend web**: http://localhost:3000 (`fe/.env` → `VITE_API_BASE_URL=http://localhost:8080` ✓ đúng)
- Eureka dashboard: http://localhost:8761
- RabbitMQ UI: http://localhost:15672 (guest/guest)
- **Database**: PostgreSQL 16 trong Docker, host port 15432 (postgres/postgres), 12 database riêng (`auth_db`, `user_db`, `order_db`, `locker_db`, `laundry_db`, `payment_db`, `notification_db`, `iot_db`, `store_db`, `staff_db`, `partner_db`, `loyalty_db`), Flyway migrate tự động.

## 4. Smoke test đã chạy (qua gateway)

| Test | Kết quả |
|---|---|
| `POST /api/auth/register` | ✅ `AUTH_REGISTERED`, trả accessToken + refreshToken |
| `POST /api/auth/login` (body dùng field `identifier`, không phải `email`) | ✅ `AUTH_LOGIN_OK` |
| `GET /api/orders/my-orders` + Bearer token | ✅ trả `[]` (JWT verify + forward header hoạt động) |
| `GET /api/stores` | ✅ trả `[]` |

## 5. Lỗi đã phát hiện và sửa

| # | File | Lỗi | Cách sửa |
|---|---|---|---|
| 1 | `laundry-locker-microservices/order-service/.../service/OrderService.java` | Controller gọi 6 method không tồn tại → build FAIL | Bổ sung `collect`, `updateWeight`, `process`, `ready`, `returnOrder`, `checkout` theo đúng pattern `transition()`/`validateStatus()` sẵn có |
| 2 | `laundry-locker-microservices/iot-service/.../service/LockerMqttService.java` | (a) Sai chữ ký `MqttClient.subscribe` → build FAIL; (b) sau khi sửa kiểu, Paho v5 1.2.5 có bug đệ quy vô hạn ở overload subscribe-with-listeners → StackOverflowError lúc runtime | Chuyển sang `client.setCallback(MqttCallback)` + `subscribe(String[], int[])` (không dùng listener array) |
| 3 | `laundry-locker-microservices/api-gateway/src/main/resources/application.yml` | Route `loyalty-service` thiếu `uri: lb://loyalty-service` | Thêm dòng `uri` |
| 4 | `laundry-locker-microservices/docker-compose.yml` tham chiếu `laundry-service/` và `partner-service/` không tồn tại trên đĩa → `docker compose up` FAIL | 2 module chỉ có trong README, không có trong `pom.xml` | Tạo mới `docker-compose.override.yml` gán profile `missing-source` để compose bỏ qua (không sửa/xóa file gốc) |
| 5 | `smart-laundry-locker-mobile/.env` | Khai `API_URL` nhưng code envied đọc `API_BASE_URL` → app sẽ dùng URL mặc định `https://api-dev.aisl.io.vn` thay vì backend local | Thêm dòng `API_BASE_URL=http://10.0.2.2:8080` (giữ nguyên dòng cũ); cần chạy `dart run build_runner build` khi build app |

File tạo mới: `run-all.ps1`, `stop-all.ps1`, `PROJECT_FLOW.md`, `RUN_ALL_GUIDE.md`, `RUN_RESULT.md`, `laundry-locker-microservices/docker-compose.override.yml`, `fe-dev.log` (log Vite).

Cài đặt mới trên máy: **Node.js LTS v24.16.0** (qua winget) — npm 11.13.0.

## 6. Môi trường đã cài bổ sung (2026-06-12, đợt 2)

| Công cụ | Phiên bản | Vị trí | Ghi chú |
|---|---|---|---|
| Node.js LTS | v24.16.0 | `C:\Program Files\nodejs` | winget |
| Python | 3.12.10 | `%LOCALAPPDATA%\Programs\Python\Python312` | winget |
| uv | 0.11.21 | winget package dir (PATH ở terminal mới) | IoT dùng Python 3.13.5 do uv tự quản lý |
| Flutter SDK | 3.44.2 (Dart 3.12.2) | `C:\flutter` | Tải zip chính thức, đã thêm `C:\flutter\bin` vào user PATH |
| Android cmdline-tools | 11076708 | `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest` | Android Studio + SDK 36.1.0 + AVD `Pixel_8` đã có sẵn từ trước |
| Biến môi trường | — | `ANDROID_HOME`, PATH += flutter/platform-tools/emulator | Set ở mức User — **mở terminal mới để nhận** |

Đã chạy sẵn cho project: `flutter pub get` + `dart run build_runner build` (mobile — env_config.g.dart đã nhận `API_BASE_URL=http://10.0.2.2:8080`), `uv sync` (IoT — import paho-mqtt/pyserial/fastapi/psycopg2 OK).

`flutter doctor`: còn 2 cảnh báo không chặn việc chạy Android app: (a) 1/7 SDK license phụ chưa accept được tự động (license chính `android-sdk-license` đã OK; nếu Gradle có yêu cầu, chạy tay `flutter doctor --android-licenses` và gõ y); (b) thiếu Visual Studio C++ — chỉ cần nếu build app **Windows desktop**, không liên quan Android.

## 7. Việc còn lại (không chặn việc chạy hệ thống)

1. **laundry-service + partner-service chưa có source** — các path `/api/services/**`, `/api/partners/**` sẽ trả lỗi route. Cần viết 2 module này (README đã mô tả scope) rồi xóa `docker-compose.override.yml`.
2. **Mobile Android**: đã build/smoke-test debug APK thành công trên emulator. Khi cần chạy lại: `cd smart-laundry-locker-mobile` → `flutter run -d emulator-5554` hoặc cài APK `build/app/outputs/flutter-apk/app-debug.apk`.
3. **IoT Python**: chạy thật cần Raspberry Pi + Arduino RS485. Backend iot-service và RPi phải trỏ **cùng MQTT broker** (hiện backend dùng `broker.hivemq.com:1883` public — chỉ phù hợp dev).
4. Email OTP của auth-service trong Docker đang trỏ SMTP `localhost:1025` (không có mailserver) — đăng ký/login vẫn chạy, chỉ tính năng gửi mail OTP là không gửi thật.
5. Login API dùng field `identifier` — frontend/mobile cần gửi đúng tên field này.

## 8. Lệnh chạy lại toàn bộ

```powershell
cd G:\BigProject
.\run-all.ps1            # build + chạy backend Docker + frontend
# hoặc nhanh hơn nếu không đổi code backend:
.\run-all.ps1 -SkipBuild
```

Dừng:

```powershell
cd G:\BigProject
.\stop-all.ps1
```

> Lưu ý: workspace nằm ở **G:\BigProject** (không phải D:\BigProject). Sau khi cài Node lần đầu, mở terminal PowerShell mới để `npm` có trong PATH.

## 9. Nâng cấp luồng tủ (Phase 1 — 2026-06-12, theo `LOCKER_FLOW_PLAN.md`)

**locker-service** (Flyway V2 + V3 đã áp):
- Model ô mới: `cell_type` (DRONE/STANDARD/XL), `row_index`, `col_index`, `fault_reason`; tủ có `landing_pad`, `landing_marker_id`.
- Tủ demo **CAB-DEMO-01** (locker id=2, ARUCO-23): 3 ô DRONE hàng 1 + 6 ô STANDARD hàng 2–3 + 1 ô XL (vali).
- Vòng đời ô: `AVAILABLE → RESERVED → OCCUPIED → AVAILABLE`, thêm `FAULT` (sticky, chỉ admin clear).
- API mới: `GET /api/lockers/{id}/layout`, `POST /internal/boxes/{id}/reserve?channel=` (ô DRONE chỉ nhận `channel=DRONE`), `POST /internal/boxes/{id}/occupy`, `POST /api/boxes/{id}/fault`, `POST /api/admin/lockers/boxes/{id}/clear-fault`, `GET /internal/lockers/{id}/boxes/find?size=&cellType=`.

**order-service**:
- `POST /api/orders/{id}/delegate {phone,name,note}` — ủy quyền lấy hộ (PIN mới + notification).
- `confirm` (khách xác nhận bỏ đồ) và `return` (staff trả đồ) giờ chuyển ô sang **OCCUPIED**; `PATCH /status` sang CANCELED/COMPLETED giờ **trả ô** (trước đây ô kẹt RESERVED vĩnh viễn khi hủy qua endpoint generic).

**Tài khoản admin dev**: `admin@laundry.test` / `Admin@123456` (roles ADMIN — cần cho các API `/api/admin/**`).

> Sự cố đã xử lý trong phiên: jar locker-service build dở (bị ngắt) → container crash-loop `ClassNotFoundException: LockerRequest` → `mvn clean package` lại + rebuild image là hết.

## 10. Phase 2 luồng tủ end-to-end (2026-06-13)

**Backend**:
- Phase 2 đã triển khai cho SEND / RENTAL / QR token / RBAC `/api/manage/**` và `/api/maintenance/**` / maintenance reports / scheduler reminder.
- Docker đang chạy đủ service cốt lõi; `GET http://localhost:8080/actuator/health` trả `200`.
- API login thật đã xác nhận role:
  - `manager@laundry.test` / `Manager@123456` → userId `6`, role `MANAGER`.
  - `maintenance@laundry.test` / `Maint@123456` → userId `7`, role `MAINTENANCE`.

**Frontend web**:
- Admin locker layout + maintenance dashboard đã nối route/sidebar/API.
- `npm.cmd run build` tại `laundry-locker-frontend/fe` PASS: `tsc -b && vite build` hoàn tất.
- Đã sửa các lỗi TS tồn đọng: `PageableRequest` dùng `page/size`, `loyaltyApi` map đúng `page/size`, `OrderTimeline` hỗ trợ `CANCELED`.
- Dev server `http://localhost:3000` trả `200`.

**Mobile Flutter**:
- Customer home đã nối quick actions:
  - `Thuê tủ` → `AppRouter.rentLocker`.
  - `Gửi hàng` → `AppRouter.sendParcel`.
  - `Đơn tủ` → `AppRouter.myLockerOrders`.
- `flutter pub get` PASS.
- `flutter analyze lib/features/locker_ops lib/core/routing lib/features/auth/presentation/pages/login_screen.dart lib/features/auth/presentation/pages/splash_screen.dart lib/features/home/presentation/pages/home_page.dart` PASS, no issues.
- `flutter build apk --debug` PASS, tạo `build/app/outputs/flutter-apk/app-debug.apk`.
- Đã cài APK lên emulator `emulator-5554`, app mở qua splash tới login không crash.
- Customer smoke thật trên emulator:
  - Login `demo@laundry.test` / `secret123` → `AUTH_LOGIN_OK`, role `CUSTOMER`.
  - Home hiển thị đủ 3 quick action mới: `Thuê tủ`, `Gửi hàng`, `Đơn tủ`.
  - Route `Thuê tủ` render form chọn tủ/loại ô/thời gian/giá.
  - Route `Gửi hàng` render form chọn tủ/SĐT người nhận/tên/ghi chú.
  - Route `Đơn tủ` gọi `GET /api/orders/my-orders` qua gateway và nhận `200 OK`, `data: []`.

Ghi chú không chặn:
- Flutter build cảnh báo Kotlin Gradle Plugin sẽ cần migrate trong các phiên bản Flutter tương lai; hiện tại build Android vẫn PASS.
- Home mobile cũ còn gọi vài API ngoài scope locker (`/advertisements`, `/blogs`, `/wallet/balance`) và nhận 404 trong môi trường local hiện tại; luồng locker ops mới không phụ thuộc các API này.
- Smoke UI cho MANAGER/MAINTENANCE bằng bàn phím ADB thủ công bị nhiễu bởi emulator/Pixel Launcher, nhưng API login role đã PASS và code routing đang map `MANAGER/ADMIN → /manager`, `MAINTENANCE → /maintenance-home`.

## 11. Nhật ký thay đổi Codex (2026-06-13)

### 11.1 Mobile Flutter

**File đã thay đổi chính**:
- `smart-laundry-locker-mobile/lib/features/home/presentation/pages/home_page.dart`
  - Đổi quick action `Thuê tủ` từ màn mock `/locker-action` sang `AppRouter.rentLocker`.
  - Đổi quick action `Gửi hàng` sang `AppRouter.sendParcel`.
  - Thay ô trống ở hàng tiện ích bằng card `Đơn tủ`, trỏ tới `AppRouter.myLockerOrders`.
  - Dọn deprecation `withOpacity` → `withValues(alpha:)`.
  - Sửa `Geolocator.getCurrentPosition` sang `locationSettings` và thêm guard `context.mounted`.
- `smart-laundry-locker-mobile/lib/features/auth/presentation/pages/login_screen.dart`
  - Dọn deprecation màu `withOpacity` → `withValues(alpha:)`.
- `smart-laundry-locker-mobile/lib/features/auth/presentation/pages/splash_screen.dart`
  - Thêm guard `mounted` sau các async gap.
  - Dọn deprecation màu.
- `smart-laundry-locker-mobile/lib/features/locker_ops/**`
  - Dọn deprecation trong các page/widget mới: `DropdownButtonFormField.value` → `initialValue`, `withOpacity` → `withValues(alpha:)`.

**Luồng mobile đã xác nhận**:
- Login customer thật qua gateway bằng `demo@laundry.test` / `secret123`.
- Home hiển thị đủ `Thuê tủ`, `Gửi hàng`, `Đơn tủ`.
- `Thuê tủ` render form chọn tủ, loại ô, số giờ, giá.
- `Gửi hàng` render form chọn tủ, SĐT người nhận, tên, ghi chú.
- `Đơn tủ` gọi `GET /api/orders/my-orders` và nhận `200 OK`.

### 11.2 Frontend Web

**File đã thay đổi chính**:
- `laundry-locker-frontend/fe/src/components/ui/examples/loading-error-examples.tsx`
  - Đổi query sample từ `{ pageNumber, pageSize }` sang `{ page, size }`.
- `laundry-locker-frontend/fe/src/components/ui/examples/server-error-examples.tsx`
  - Đổi query sample từ `{ pageNumber, pageSize }` sang `{ page, size }`.
- `laundry-locker-frontend/fe/src/stores/apis/loyaltyApi.ts`
  - Sửa mapper pagination dùng đúng `page` và `size`.
- `laundry-locker-frontend/fe/src/pages/Admin/orders/components/OrderTimeline.tsx`
  - Type `STATUS_FLOW` là `OrderStatus[]` để timeline chấp nhận cả status `CANCELED` mà không lỗi TypeScript.

**Kết quả**:
- `npm.cmd run build` PASS: TypeScript build và Vite production build hoàn tất.
- Dev server hiện có ở `http://localhost:3000` và trả `200`.

### 11.3 Tài liệu

**File đã cập nhật**:
- `RUN_RESULT.md`
  - Thêm mục Phase 2 end-to-end, mobile smoke, FE build, backend health, tài khoản role.
  - Thêm mục nhật ký thay đổi theo file trong phiên Codex.
- `LOCKER_FLOW_PLAN.md`
  - Tick Phase 2 đã hoàn tất: QR, SEND, RENTAL, mobile 3 role, FE dashboard, RBAC, scheduler.

### 11.4 Verification cuối phiên

| Lệnh / kiểm tra | Kết quả |
|---|---|
| `flutter pub get` | PASS |
| `flutter analyze lib/features/locker_ops lib/core/routing lib/features/auth/presentation/pages/login_screen.dart lib/features/auth/presentation/pages/splash_screen.dart lib/features/home/presentation/pages/home_page.dart` | PASS, no issues |
| `flutter build apk --debug` | PASS |
| `adb install -r build/app/outputs/flutter-apk/app-debug.apk` | PASS |
| Customer login + 3 quick actions trên emulator | PASS |
| `npm.cmd run build` trong `laundry-locker-frontend/fe` | PASS |
| `GET http://localhost:8080/actuator/health` | `200` |
| `POST /api/auth/login` manager | PASS, userId `6`, role `MANAGER` |
| `POST /api/auth/login` maintenance | PASS, userId `7`, role `MAINTENANCE` |

## 12. Gom tài liệu và artifact vào backend (2026-06-13)

Đã tạo thư mục riêng trong backend:

`laundry-locker-microservices/docs/project-artifacts/`

Cấu trúc:
- `guides/`: copy các file tài liệu/kết quả/handoff/plan từ root workspace (`*.md`, `*.txt`).
- `screenshots/`: copy các ảnh emulator/smoke-test (`*.png`, `*.jpg`, `*.jpeg`).
- `logs/`: copy log (`*.log`).
- `scripts/`: copy script tiện ích (`*.ps1`).

Đã thêm `laundry-locker-microservices/docs/project-artifacts/README.md` làm mục lục.

Lưu ý: thao tác này **copy** file vào backend, không xóa hoặc di chuyển file gốc ở `G:\BigProject`, để tránh làm mất đường dẫn các tab/tài liệu đang mở.

### 12.1 Gom Markdown trong các project con

Đã tạo thêm:

`laundry-locker-microservices/docs/project-artifacts/markdown-by-project/`

Phân loại:
- `workspace-root/`: Markdown ở root workspace.
- `backend/`: Markdown trong `laundry-locker-microservices/`.
- `frontend/`: Markdown trong `laundry-locker-frontend/`.
- `mobile-flutter/`: Markdown trong `smart-laundry-locker-mobile/`.
- `iot-python/`: Markdown trong `smart-locker-iot/`.

Đã copy 67 file Markdown dự án. Các thư mục dependency/generated/build đã được loại trừ: `node_modules`, `.venv`, `.pio`, `dist`, `build`, `.dart_tool`, `.git`, `.gradle`, `.idea`, `.vscode`, và chính `docs/project-artifacts`.

Đã thêm `laundry-locker-microservices/docs/project-artifacts/markdown-by-project/README.md` làm mục lục và thống kê.

## 13. Cập nhật tài liệu cũ theo trạng thái hiện tại (2026-06-13)

Đã rà soát và cập nhật các tài liệu lõi để bám theo dự án hiện tại:

- `laundry-locker-microservices/README.md`: viết lại theo backend hiện tại, nêu rõ 10 service có source, `laundry-service`/`partner-service` đang missing-source, route gateway/RBAC, locker Phase 2, smoke test SEND/RENTAL/PIN/QR.
- `laundry-locker-microservices/RUN_AND_TEST_GUIDE.md`: viết lại guide chạy/test bằng PowerShell, không còn hướng dẫn gọi `/internal/**` qua gateway hoặc dùng endpoint cũ `/api/services`.
- `smart-laundry-locker-mobile/README.md`: thay README Flutter mặc định bằng mô tả app hiện tại, role routing, file chính, lệnh analyze/build và kết quả smoke test.
- `smart-locker-iot/README.md`: cập nhật vai trò IoT với verify access bằng PIN/QR và hướng chạy bằng `uv`.
- `RUN_ALL_GUIDE.md`: sửa mô tả backend thành 10 service nghiệp vụ có source, thêm link tới `CURRENT_PROJECT_STATUS.md`.
- `PROJECT_FLOW.md`: thêm snapshot sau Phase 2, sửa route `/api/manage/**`, `/api/maintenance/**`, RBAC và event `locker.box.fault`.
- `GAP_ANALYSIS_AND_PLAN.md`: thêm phần cập nhật sau Locker Phase 2, sửa các gap đã đóng như mobile login mock, maintenance backend, QR/locker ops; vẫn giữ gap chính của DDSLMS là drone/AI/realtime tracking.
- `HANDOFF_CODEX.md`: thêm phần cập nhật sau Codex tiếp tục, sửa trạng thái FE build/mobile build từ fail/chưa test sang PASS.

Lưu ý commit/artifact:

- Các file `env.txt`, `pro.txt`, `Application.txt` được giữ ngoài luồng commit theo yêu cầu.
- Các bản copy phân loại trong `laundry-locker-microservices/docs/project-artifacts/markdown-by-project/` cần được đồng bộ lại sau batch cập nhật này.

## 14. Tạo tài liệu sống cho luồng nghiệp vụ và tiến độ dự án (2026-06-13)

Đã tạo 2 file nguồn để bàn giao cho developer/AI coding agent tiếp theo:

- `laundry-locker-microservices/docs/BUSINESS_FLOWS_CURRENT.md`
  - Mô tả chi tiết các luồng nghiệp vụ hiện tại: auth/profile, laundry order, SEND parcel, RENTAL, PIN/QR unlock, delegation, fault/maintenance, manager/admin/staff, payment/refund, notification, store/loyalty, IoT, RabbitMQ, mobile và web frontend.
  - File này phải được cập nhật mỗi khi hành vi nghiệp vụ, role, endpoint, event, màn hình hoặc trạng thái luồng thay đổi.
- `laundry-locker-microservices/docs/PROJECT_PROGRESS_TRACKER.md`
  - Theo dõi tiến độ triển khai theo component, trạng thái đã làm/chưa làm, verification log, deployment/database notes, known risks và change log.
  - File này phải được cập nhật sau mỗi lần làm chức năng, fix bug, thay đổi docs quan trọng, chạy test/smoke hoặc thay đổi deploy/database.

Đã link 2 file trên vào:

- `laundry-locker-microservices/README.md`
- `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`
- `laundry-locker-microservices/docs/project-artifacts/README.md`

Quy tắc bàn giao mới:

1. Người/AI tiếp theo đọc `docs/PROJECT_PROGRESS_TRACKER.md` trước để biết đang làm tới đâu.
2. Sau đó đọc `docs/BUSINESS_FLOWS_CURRENT.md` để hiểu nghiệp vụ hiện tại.
3. Sau mỗi thay đổi mới, cập nhật ít nhất `PROJECT_PROGRESS_TRACKER.md`; nếu đổi behavior thì cập nhật cả `BUSINESS_FLOWS_CURRENT.md`.
