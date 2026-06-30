# Luồng Nghiệp Vụ Hiện Tại

> Cập nhật lần cuối: 2026-06-21 (PA4 — STAFF+TECHNICIAN roles)
> Workspace: `G:\BigProject`
> Cặp tài liệu nguồn: file này + `docs/PROJECT_PROGRESS_TRACKER.md`

## Quy Tắc Cập Nhật Bắt Buộc

Mỗi khi developer hoặc AI coding agent thêm, xoá, đổi tên, nối lại, sửa lỗi, kiểm thử, hoặc xác nhận một chức năng nghiệp vụ, file này phải được cập nhật trong cùng phiên làm việc.

Cập nhật file này khi có thay đổi về:

- Vai trò người dùng, quyền truy cập, route, màn hình, endpoint, bảng database, event, scheduler, hoặc tích hợp bên ngoài.
- Luồng trạng thái nghiệp vụ, hành vi PIN/QR, thanh toán, thông báo, hoặc vòng đời ô tủ locker.
- Một chức năng chuyển từ mock/legacy/partial sang thật, hoặc từ đang chạy sang lỗi/deprecated.

Sau khi cập nhật file này, phải cập nhật thêm `docs/PROJECT_PROGRESS_TRACKER.md` để ghi tiến độ triển khai và trạng thái verification.

## 1. Bối Cảnh Hệ Thống

Dự án hiện tại là nền tảng Smart Laundry Locker gồm:

- Backend: `laundry-locker-microservices/`, Java 21, Spring Boot 3.5.14, Spring Cloud Gateway, Eureka, PostgreSQL, RabbitMQ.
- Web frontend: `laundry-locker-frontend/fe/`, React 19 + Vite cho admin/partner portal.
- Flutter mobile: `smart-laundry-locker-mobile/`, các flow cho customer, manager và maintenance.
- IoT runtime: `smart-locker-iot/`, Python/uv cho Raspberry Pi hoặc mô phỏng cabinet.
- Thư mục legacy/liên quan: `laundry-locker-frontend/mobile`, `laundry-locker-frontend/iot`, `laundry-locker-frontend/drone`, và landing page.

Entrypoint chính của backend:

```text
API Gateway (deploy, dùng chung cả nhóm): http://146.190.84.136:8080
API Gateway (local khi chạy docker để dev): http://localhost:18080  (host 8080 thường bị chiếm)
```

**Chính sách môi trường (chốt 2026-06-16):** cả nhóm dùng chung backend + PostgreSQL đã deploy trên DigitalOcean droplet `146.190.84.136`; không chạy local nữa, không bắt buộc bật docker (chỉ bật docker local khi đang sửa code backend để test). Web FE (`laundry-locker-frontend/fe`) và mobile (`smart-laundry-locker-mobile`) đều cấu hình base URL mặc định về `http://146.190.84.136:8080`. Vì client luôn đi qua gateway, trỏ FE/mobile về server đồng nghĩa dùng luôn database trên server (client không nối DB trực tiếp). Nối DB trực tiếp (DBeaver/psql) dùng `146.190.84.136:15432` nhưng cần mở port `15432` trên cloud firewall trước (hiện chỉ mở `22` + `8080`). Khi backend có code mới merge vào nhánh chính (`develop`), workflow `deploy-droplet.yml` tự deploy lên droplet và Flyway tự migrate DB lúc service khởi động.

Phạm vi hiện tại cần ghi nhớ:

- Source `laundry-service` đang thiếu trong repo backend hiện tại; vẫn giữ tên trong compose/database naming nhưng bị skip qua `docker-compose.override.yml` khi chạy local.
- Role `PARTNER` và `partner-service` đã được gỡ khỏi backend (seed/role/permission/compose) vì không còn dùng. **PA3 (2026-06-15)**: đã dọn nốt — drop DB rỗng `partner_db` + `laundry_db`, bỏ cột `stores.partner_id`, drop 3 bảng RBAC orphan `roles/permissions/role_permissions`.
- **Dữ liệu Demo (2026-06-19)**: Đã bổ sung script `scripts/generate_100_seed_data.py` sinh 100 record thực tế (tên VN, địa chỉ thật, file SQL ~580KB) cho mỗi bảng của 9 service đang chạy để UI có dữ liệu hiển thị.
- Sản phẩm đang chạy chính là nền tảng locker/laundry/SEND/RENTAL. Drone delivery đầy đủ, engine phân công drone, realtime tracking và AI/RAG vẫn là việc tương lai.
- Quyết định kiến trúc (K8s/Helm/GitOps, Kafka, CQRS/Event Sourcing, GraphQL, service mesh) hiện **được hoãn có chủ đích** ở quy mô 1 droplet — xem `docs/ARCHITECTURE_DECISIONS.md` để biết lý do và điều kiện xem lại. Stack hiện tại: REST qua gateway + Eureka + Resilience4j + RabbitMQ + Docker Compose.

## 2. Mô Hình Vai Trò Và Quyền Truy Cập

### Khách Hàng / User

Dùng cho các flow trên app khách hàng:

- Đăng ký/đăng nhập.
- Xem cửa hàng/locker.
- Tạo đơn gửi hàng qua locker, gọi là SEND.
- Tạo đơn thuê ô tủ tạm thời, gọi là RENTAL.
- Tạo đơn giặt theo flow cũ.
- Xem đơn của mình.
- Xác nhận đã bỏ hàng/đồ vào ô tủ.
- Hoàn tất nhận hàng/lấy đồ.
- Gia hạn hoặc kết thúc RENTAL.
- Uỷ quyền người khác nhận hộ.
- Báo lỗi locker/ô tủ.
- Xem thông báo.
- Sử dụng điểm thưởng/loyalty nếu đã nối flow.

Tên role hiện chưa đồng nhất hoàn toàn:

- Backend mặc định role mới là `CUSTOMER`.
- Một số seed/demo profile đang dùng `USER`.
- Flutter routing xem mọi role không phải `MANAGER`/`MAINTENANCE` là customer home.

**Cấp phát role (2026-06-20):** khách tự đăng ký trên mobile (email/phone/Google qua Firebase) **chỉ ra `CUSTOMER`**. Các role vận hành `ADMIN`/`MANAGER`/`MAINTENANCE` **chỉ do admin web tạo** (`POST /api/admin/users`, tạo cả `auth_account` để login được) — xem mục 3.

### Admin

Dùng cho web admin:

- Dashboard tổng quan.
- Quản lý user và role.
- Quản lý đơn hàng và trạng thái đơn.
- Quản lý cửa hàng.
- Quản lý locker và layout ô tủ.
- Xử lý bảo trì/báo lỗi.
- Thanh toán/hoàn tiền.
- Loyalty.
- Thông báo.
- Khuyến mãi.
- Scheduler jobs.

Gateway yêu cầu role `ADMIN` cho `/api/admin/**`.

### Manager

Dùng cho quản lý vận hành locker:

- Màn hình Manager Home trên Flutter.
- Thống kê locker.
- Layout locker.
- Danh sách đơn và thống kê đơn.
- Tổng quan báo cáo locker.

Gateway yêu cầu `MANAGER` hoặc `ADMIN` cho `/api/manage/**`.

Endpoint backend hiện có:

- `GET /api/manage/lockers/stats`
- `GET /api/manage/lockers`
- `GET /api/manage/lockers/{id}/layout`
- `GET /api/manage/lockers/reports`
- `GET /api/manage/orders`
- `GET /api/manage/orders/statistics`

### Maintenance

Dùng cho nhân sự xử lý lỗi locker:

- Màn hình Maintenance Home trên Flutter.
- Xem các ô tủ bị lỗi.
- Xem báo cáo đang mở hoặc đã được giao.
- Claim báo cáo.
- Resolve báo cáo và clear fault cho ô tủ.
- Clear fault trực tiếp.
- Xem địa chỉ/toạ độ locker lỗi và mở chỉ đường tới tủ qua bản đồ ngoài thiết bị.

Gateway yêu cầu `MAINTENANCE` hoặc `ADMIN` cho `/api/maintenance/**`.

Endpoint backend hiện có:

- `GET /api/maintenance/faults`
- `GET /api/maintenance/reports?mine=true|false`
- `PUT /api/maintenance/reports/{id}/claim`
- `PUT /api/maintenance/reports/{id}/resolve`
- `POST /api/maintenance/boxes/{id}/clear-fault`
- `POST /api/maintenance/boxes/{id}/out-of-service` (L5, body `{reason?}`): ngưng dùng ô có chủ đích.
- `POST /api/maintenance/boxes/{id}/cleaning` (L5): đánh dấu ô đang vệ sinh/khử khuẩn.
- `POST /api/maintenance/boxes/{id}/return-to-service` (L5): khôi phục ô `OUT_OF_SERVICE`/`CLEANING` về `AVAILABLE`.
- `GET /api/maintenance/lockers/{lockerId}/box-health` (mới 2026-06-30): **box-health cho bảo trì** — trạng thái logic (theo đơn, locker-service) đặt cạnh trạng thái **phần cứng** cửa (iot-service, GAP 2). Mỗi ô trả `{boxId, boxNumber, cellType, logicalStatus, hwState, lastReportedAt, doorOpen, needsAttention}`; `doorOpen`=cửa đang OPEN, `needsAttention`=cửa mở nhưng ô KHÔNG `OCCUPIED` (nghi cửa kẹt/quên đóng). locker-service Feign `GET /internal/iot/box-status` (best-effort; iot-service chết → `hwState=null`, vẫn hiện logic).
- `GET /api/maintenance/box-anomalies` (mới 2026-06-30): **tổng quan ca trực** — gom **mọi ô trên tất cả tủ** đang có cửa phần cứng `OPEN` nhưng KHÔNG `OCCUPIED` (từ dữ liệu GAP 2) vào một danh sách, mỗi mục kèm metadata locker (`lockerCode/lockerName/lockerAddress/lat/long` để chỉ đường) + `boxId/boxNumber/cellType/logicalStatus/hwState/lastReportedAt`. Read-only, best-effort (iot-service chết → list rỗng); **không** tự tạo report/notify (không side-effect) — chỉ để KTV thấy bất thường ở mọi tủ mà không phải chọn từng tủ.
- `GET /api/maintenance/reports/{id}/logs` (L5): nhật ký xử lý (work-log) của phiếu.
- `POST /api/maintenance/reports/{id}/logs` (L5, body `{note}`): KTV thêm 1 bước xử lý (actor = `X-User-Id`). Bảng mới `repair_logs` (migration V5).
- `GET /api/maintenance/schedules` (L5): lịch bảo trì phòng ngừa (kèm cờ `due` khi `now >= next_due_at`).
- `POST /api/maintenance/schedules/{id}/complete` (L5): KTV đánh dấu đã kiểm tra → dời `next_due_at = now + interval_days`.
- `POST /api/admin/lockers/schedules` (L5, ADMIN, body `{lockerId,title,intervalDays}`): tạo lịch định kỳ. Bảng mới `maintenance_schedules` (migration V6).
- `DELETE /api/admin/lockers/schedules/{id}` (L5, ADMIN): xóa mềm lịch (active=false).

Ô `OUT_OF_SERVICE`/`CLEANING` tự động bị loại khỏi phân phối (reserve/findAvailable chỉ nhận `AVAILABLE`); không thể ngưng dùng/vệ sinh ô đang `OCCUPIED`/`RESERVED`. Surface: Maintenance mobile (tab Kiểm tra tủ → bottom sheet hành động theo trạng thái ô) và Admin web (sơ đồ tủ `layout-view` → action theo từng ô).

Response của `faults` và `reports` hiện trả thêm metadata định vị locker để web/mobile không cần gọi vòng lại khi điều phối kỹ thuật:

- `lockerCode`, `lockerName`, `lockerAddress`
- `lockerLatitude`, `lockerLongitude`
- Với report có ô cụ thể: `boxNumber`, `cellType`
- SLA (L5): `slaHours` (mặc định 4h, cấu hình `app.maintenance.sla-hours`), `slaDueAt` (= `createdAt + slaHours`), `overdue` (`true` khi quá hạn và phiếu chưa `RESOLVED`). Mobile + Admin web hiển thị badge "Quá hạn SLA" theo cờ này (authoritative, không tự đoán ở client).

### Staff

> **Tái tạo (PA4, 2026-06-20)**: `staff-service` riêng đã bị gỡ (PA3 2026-06-15), nhưng RBAC path `/api/staff/**` đã được thêm lại dưới dạng controllers nhẹ trong locker-service và order-service. Không có DB riêng, không có service riêng. `StaffController` trong locker-service và `StaffOrderController` trong order-service reuse hoàn toàn service methods sẵn có.

Gateway yêu cầu `STAFF` hoặc `ADMIN` cho `/api/staff/**`.

Endpoint backend hiện có (read-only):

- `GET /api/staff/lockers` — locker list (optionally filtered by `?storeId=`) → locker-service
- `GET /api/staff/lockers/{id}/layout` — grid layout ô tủ → locker-service
- `GET /api/staff/lockers/stats` — occupancy stats (optionally filtered by `?storeId=`) → locker-service
- `GET /api/staff/orders` — active orders (optionally filtered by `?status=&type=&lockerId=`) → order-service

Test account: `staff@lockr.test` / `12345678` (user_id 9005).

### Technician

> **Mới (PA4, 2026-06-20)**: Role TECHNICIAN mới, dùng cho nhân viên kỹ thuật IoT. `TechnicianController` trong iot-service reuse `DeviceStatusRepository` và `BoxAccessLogRepository`.

Gateway yêu cầu `TECHNICIAN` hoặc `ADMIN` cho `/api/technician/**`.

Endpoint backend hiện có:

- `GET /api/technician/devices` — danh sách tất cả IoT device và trạng thái hiện tại
- `GET /api/technician/devices/{id}` — chi tiết một device (health, last-seen, status)
- `PUT /api/technician/devices/{id}/status` — override thủ công trạng thái device (`{status: "ONLINE"|"OFFLINE"|"ERROR"}`)
- `GET /api/technician/devices/{id}/logs` — audit log (box_access_logs) của locker được device gắn với, mới nhất trước
- `POST /api/technician/devices/{id}/restart` — publish lệnh restart qua MQTT (best-effort), luôn ghi audit log dù MQTT thất bại

Test account: `tech@lockr.test` / `12345678` (user_id 9006).

### Partner (đã gỡ)

Role `PARTNER` đã được gỡ khỏi backend (2026-06-13) vì không còn dùng: bỏ role `PARTNER`, permission `PARTNER_MANAGE`, account demo `partner.seed`, và service `partner-service` trong `docker-compose.yml`/`docker-compose.override.yml`. `partner-service` vốn đã thiếu source từ trước. **PA3 (2026-06-15) đã dọn**: drop DB `partner_db` (script `scripts/drop-legacy-databases.sql`), gỡ cột `stores.partner_id` (store-service V2 + entity/DTO/service), gỡ khỏi `init-databases.sql`. Route Partner ở React web/legacy mobile xem như deprecated.

## 3. Luồng Xác Thực Và Hồ Sơ Người Dùng

### Database

Auth và profile được tách riêng:

- `auth_db.auth_schema.auth_accounts`: tài khoản đăng nhập, password hash, provider, verification flags, status.
- `auth_db.auth_schema.refresh_tokens`: refresh token.
- `auth_db.auth_schema.email_otps`: OTP hash.
- `user_db.user_schema.user_profiles`: hồ sơ người dùng, phone/email, status, role.
- ~~`user_db.user_schema.roles`, `permissions`, `role_permissions`~~: **đã drop (PA3)** — 3 bảng RBAC này không có entity/repository, không bao giờ được query. Phân quyền thực tế dùng cột `user_profiles.roles` (VARCHAR) + claim `roles` trong JWT.
- ~~`user_db.user_schema.audit_logs`~~: **ĐÃ DROP (PA3 đợt 2)** — không có code runtime nào ghi (tính năng audit chưa hoàn thiện); đã gỡ bảng (user-service V4) + entity/repository + 4 endpoint `/api/admin/audit-logs*` + seed.

Không lưu mật khẩu plain text. Chỉ lưu bcrypt hash.

### Đăng Ký

Client gọi:

```http
POST /api/auth/register
```

Body gồm email/phone/password (field `roles` nếu có **bị bỏ qua**).

Hành vi backend:

1. `auth-service` tạo auth account và hash password.
2. `auth-service` gọi internal provisioning endpoint của `user-service`.
3. `user-service` tạo profile trong `user_profiles`.
4. `auth-service` cấp access token và refresh token.

**Self-register chỉ ra role `CUSTOMER` (2026-06-20).** `AuthService.register()` ép `roles = {CUSTOMER}` bất kể client gửi gì (trước đó honor `roles` client → ai cũng tự đăng ký `ADMIN`). Đường cho phép roles tùy ý đã tách riêng thành `provisionWithRoles()` (nội bộ) + `createAccount()` (dùng cho admin tạo role khác, xem dưới).

### Đăng Ký / Đăng Nhập Nhanh Qua Firebase (mobile)

Mobile dùng **Firebase Auth làm identity broker thống nhất** cho cả số điện thoại (OTP) và Google; mọi provider đều sinh **một Firebase ID token**. Backend chỉ có một endpoint verify:

```http
POST /api/auth/firebase
{ "idToken": "<firebase-id-token>" }
```

Hành vi backend (`AuthService.firebaseLogin`):

1. `FirebaseAuth.verifyIdToken(idToken)` → lấy `uid`, `sign_in_provider` (`phone`/`google.com`/...), `phone_number`, `email`, `name`. idToken sai → `AUTH_FIREBASE_INVALID`.
2. Tra `social_identities(provider, uid)`; nếu có → login account đã link.
3. Nếu chưa link → tìm account theo email/phone để **link** vào account cũ; nếu vẫn chưa có → provision user `CUSTOMER` + tạo `auth_account` (password random) rồi lưu `social_identities`.
4. Cấp `AuthResponse` (accessToken/refreshToken/roles).

Lưu trữ: bảng mới `auth_schema.social_identities(account_id, provider, provider_user_id)` unique `(provider, provider_user_id)` — migration `auth-service V2__auth_social_identities.sql`. `password_hash` giữ `NOT NULL` (social/phone dùng random hash).

Khởi tạo Firebase Admin: `app.firebase.credentials-json` (`FIREBASE_CREDENTIALS_JSON`, nội dung service account JSON). `FirebaseConfig` fail-soft khi thiếu credential (chỉ log warn, không chặn boot). **Facebook (2026-06-21)** đã wire ở mobile (`flutter_facebook_auth` + native config strings.xml/AndroidManifest, `signInWithFacebook()` → Firebase credential → cùng endpoint `/api/auth/firebase`); chạy thật khi đã bật Facebook provider trong Firebase (App ID/Secret) + thêm OAuth redirect URI. Backend không cần đổi (nhận mọi `sign_in_provider`).

Mobile (`smart-laundry-locker-mobile`): `firebase_auth` + `google_sign_in` (v7) trong `FirebaseAuthService`; UI nút Google + dialog phone-OTP trong `auth_bottom_sheet.dart` (cả tab Đăng nhập lẫn Đăng ký) → `LoginProvider.loginWithGoogle()/sendPhoneOtp()/confirmPhoneOtp()` → `POST /api/auth/firebase`.

### Tài Khoản Do Admin Tạo (ADMIN / MANAGER / MAINTENANCE)

Self-register **không** tạo được role vận hành. Admin web tạo qua:

```http
POST /api/admin/users   (role ADMIN)
{ email, phoneNumber, firstName, lastName, password, roles:["MANAGER"|"MAINTENANCE"|"ADMIN"|"CUSTOMER"] }
```

Hành vi (`user-service UserController.adminCreate`, 2026-06-20):

1. Tạo `user_profile` (`UserProfileService.create`).
2. Gọi Feign `AuthClient.createAccount` → `POST /internal/auth/accounts` ở `auth-service` → tạo `auth_account` có password hash thật cho `userId` (validate roles ∈ {CUSTOMER, ADMIN, MANAGER, MAINTENANCE}).
3. Nếu tạo auth account lỗi → **xóa profile vừa tạo** (compensate) + ném `ACCOUNT_CREATION_FAILED` (tránh profile mồ côi không login được).

Trước thay đổi này, `POST /api/admin/users` chỉ tạo `user_profile` (không có `auth_account`) → manager/maintenance admin tạo ra **không đăng nhập được**. Web `CreateUserModal` đã đổi danh sách role sang `CUSTOMER/ADMIN/MANAGER/MAINTENANCE` (bỏ stale `USER/STAFF/MODERATOR/PARTNER`); `RoleNameSchema` (Zod) mở rộng để không chặn role mới.

### Đăng Nhập

Client gọi:

```http
POST /api/auth/login
```

Lưu ý quan trọng: body dùng `identifier`, không dùng `email`.

```json
{
  "identifier": "demo@laundry.test",
  "password": "secret123"
}
```

Hành vi backend:

1. `auth-service` tìm account theo email hoặc phone.
2. BCrypt kiểm tra password.
3. `auth-service` lấy role/profile summary từ `user-service`.
4. JWT chứa identity của user/account và roles.
5. JWT có claim `tokenUse`; token truy cập API nghiệp vụ phải là `tokenUse=access`, refresh token chỉ dùng cho refresh flow.
6. Gateway validate JWT access token và forward các header:
   - `X-User-Id`
   - `X-Account-Id`
   - `X-User-Roles`
   - `X-Correlation-Id`

Lưu ý kỹ thuật:
- Do `auth-service` gọi sang `user-service` qua OpenFeign, lần gọi đầu tiên (Cold Start) thường tốn nhiều thời gian khởi tạo kết nối DB của Hibernate. Resilience4j TimeLimiter mặc định 1s sẽ gây lỗi 500/503. Đã cấu hình tăng `timeout-duration` lên 10s để khắc phục.
- Tài khoản test đầy đủ (`binhtntse182370@fpt.edu.vn` / `12345678`) đã được chèn qua SQL seed script trực tiếp vào Postgres container (qua 4 DB: `auth`, `user`, `order`, `loyalty`).
- **Refresh token (làm mới access token):** `POST /api/auth/refresh-token` body `{"refreshToken":"..."}` → trả `AuthResponse` mới (`accessToken/refreshToken/...`). Backend **xoay vòng** refresh token (revoke token cũ, cấp token mới) mỗi lần refresh. Access token sống ~24h. **Mobile (2026-06-15):** `AuthInterceptor` khi gặp 401 sẽ tự gọi refresh-token (serialize 1 lần/đợt) rồi retry request gốc với token mới; chỉ logout về onboarding khi không có refresh token hoặc refresh thất bại — thay cho hành vi cũ là logout ngay khi access token hết hạn.

### Admin Auth

Endpoint riêng cho admin:

- `POST /api/admin/auth/login`
- `POST /api/admin/auth/verify-2fa`
- `POST /api/admin/auth/refresh`

Flow admin login phụ thuộc credential/OTP. Trong dev/test, `/api/auth/login` cũng đã được dùng để lấy token admin khi account có role `ADMIN`.

Luồng 2FA admin (web) và **shape response** (để client chuẩn hoá đúng):

1. `POST /api/admin/auth/login` `{email,password}` → kiểm tra mật khẩu + role `ADMIN`, gửi OTP qua email và trả `{requiresTwoFactor, tempToken, expiresIn, maskedEmail, message}` (trong `data`). OTP **luôn được log** ở `auth-service` (`Development OTP: <code>`) — dùng để lấy mã khi chưa cấu hình SMTP. **Gửi email thật (2026-06-16):** mail config trong `docker-compose.yml` (auth-service) đã cho overridable qua env — mặc định `localhost:1025` (không gửi được, chỉ log). Để OTP tới email thật, đặt trên droplet (`.env`): `SPRING_MAIL_HOST=smtp.gmail.com`, `SPRING_MAIL_PORT=587`, `SPRING_MAIL_USERNAME=<gmail>`, `SPRING_MAIL_PASSWORD=<app password 16 ký tự>`, `SPRING_MAIL_SMTP_AUTH=true`, `SPRING_MAIL_SMTP_STARTTLS=true` rồi recreate auth-service. `SmtpEmailService` nuốt lỗi gửi mail (warn-log, không chặn flow) nên OTP vẫn dùng được qua log nếu SMTP sai.
2. `POST /api/admin/auth/verify-2fa` `{tempToken, otpCode}` → trả **payload phẳng** (qua `AuthService.authMap`), **không** lồng trong key `user`: `{accountId, userId, accessToken, refreshToken, tokenType, expiresAt, roles, isNewUser, name?}`. **Không có field `id`** (dùng `userId`) và **không echo `email`** (client tự giữ email đã nhập ở bước 1). Web FE chuẩn hoá `User` từ payload phẳng này (`id ← userId`, `role ← roles`, `fullName ← name`). *(Bug 2026-06-15: FE từng đọc `data.user.id` nên crash `Cannot read properties of undefined (reading 'id')`; đã sửa ở `auth-context.tsx`.)*

## 4. Gateway Và Ranh Giới Service

Client chỉ nên gọi qua gateway. Port service trực tiếp chỉ dùng debug.

Request tracing kỹ thuật:

- Gateway tạo hoặc giữ nguyên `X-Correlation-Id` cho mọi request.
- Header này được forward xuống service phía sau và trả lại trong response.
- Servlet services đưa correlation id vào MDC log key `correlationId` để tra lỗi xuyên service.

Route ownership của gateway:

- Auth: `/api/auth/**`, `/api/admin/auth/**`
- User: `/api/user/**`, `/api/users/**`, `/api/admin/users/**`, `/api/admin/audit-logs/**`
- Order: `/api/orders/**`, `/api/manage/orders/**`, `/api/admin/orders/**`, `/api/admin/scheduler/**`, `/api/admin/dashboard/**`, `/api/promotions/**`, `/api/admin/promotions/**`
- Locker: `/api/lockers/**`, `/api/boxes/**`, `/api/manage/lockers/**`, `/api/maintenance/**`, `/api/admin/lockers/**`
- Payment: `/api/payments/**`, `/api/admin/payments/**`, `/payments/vnpay/callback` (public alias cho mobile WebView VNPay topup callback, thêm 2026-06-18)
- Notification: `/api/notifications/**`, `/api/admin/notifications/**`, `/ws`, `/ws/**`
- IoT: `/api/iot/**`, `/api/manage/iot/**` (mới 2026-06-16: device health dashboard, role MANAGER/ADMIN)
- Store: `/api/stores/**`, `/api/admin/stores/**`
- Staff: `/api/staff/**`
- Loyalty: `/api/loyalty/**`, `/api/admin/loyalty/**`

RBAC tại gateway:

- `/api/admin/**`: `ADMIN`
- `/api/manage/**`: `MANAGER` hoặc `ADMIN`
- `/api/maintenance/**`: `MAINTENANCE` hoặc `ADMIN`
- `/internal/**`: bị chặn qua gateway; chỉ dùng service-to-service.

Endpoint kỹ thuật/vận hành:

- `/actuator/health`: health/readiness-liveness probes.
- `/actuator/info`: runtime info; app module có build metadata từ Spring Boot Maven `build-info`.
- `/actuator/metrics`: runtime metrics.
- `/actuator/prometheus`: Prometheus scrape endpoint.
- `/actuator/sbom`: SBOM runtime endpoint cho app khi actuator được expose.
- `/v3/api-docs`: OpenAPI runtime docs cho gateway và từng servlet service, có thể tắt bằng `SPRINGDOC_API_DOCS_ENABLED=false`.
- `/v3/api-docs/<service-name>`: gateway route aggregate OpenAPI docs cho các service đang có source như `auth-service`, `order-service`, `locker-service`, `payment-service`, `iot-service`.
- `/swagger-ui/index.html`: Swagger UI tại gateway, gom các OpenAPI docs ở trên; có thể tắt bằng `SPRINGDOC_SWAGGER_UI_ENABLED=false`.

CORS cho web frontend:

- Web admin/FE chạy ở origin `http://localhost:3000` gọi gateway (local `:18080`, mặc định `:8080`). CORS được cấu hình tại **`spring.cloud.gateway.server.webflux.globalcors`**: cho phép origin `http://localhost:3000`, methods `GET,POST,PUT,PATCH,DELETE,OPTIONS`, `allowedHeaders: "*"`, `allowCredentials: true`.
- **Lưu ý (Spring Cloud Gateway 4.3.x / Spring Cloud 2025.0.x):** prefix cũ `spring.cloud.gateway.globalcors` đã `deprecated` từ gateway 4.3.0 và **không còn bind** vào `GlobalCorsProperties`; đặt CORS ở đó sẽ bị bỏ qua, làm trình duyệt báo `No 'Access-Control-Allow-Origin' header` / "Failed to fetch" khi web đăng nhập (`POST /api/admin/auth/login`). Mọi cấu hình gateway (routes/discovery/globalcors) phải nằm dưới `spring.cloud.gateway.server.webflux.*`. (Fix 2026-06-15.)

Hardening hiện tại:

- Gateway chỉ authorize JWT có `tokenUse=access`; refresh token bị từ chối ở API nghiệp vụ.
- JWT secret của `auth-service` và `api-gateway` dùng cùng policy trong `common-lib`.
- Profile `prod`/`production` sẽ fail-fast nếu JWT secret còn là giá trị demo/dev/change-me/default/localhost/sandbox hoặc ngắn hơn 32 UTF-8 bytes.
- Gateway RBAC/access-token hiện có unit tests cho:
  - Chặn `/internal/**`.
  - Từ chối refresh token ở business API.
  - Role guard cho `/api/admin/**`, `/api/manage/**`, `/api/maintenance/**`.
  - Forward `X-User-Id`, `X-Account-Id`, `X-User-Roles`.
  - Public OpenAPI/Swagger UI và catalogue GET.
- Service-to-service OpenFeign trong các service chính có circuit breaker Resilience4j và default timeout cấu hình bằng env:
  - `SPRING_CLOUD_OPENFEIGN_CIRCUITBREAKER_ENABLED`
  - `SPRING_CLOUD_OPENFEIGN_CIRCUITBREAKER_GROUP_ENABLED`
  - `APP_FEIGN_CONNECT_TIMEOUT_MS`
  - `APP_FEIGN_READ_TIMEOUT_MS`
  - `APP_RESILIENCE4J_CB_*`
- CI/CD backend hiện có:
  - Backend CI chạy `mvn -B test`.
  - Backend security workflow chạy Dependency Review, CodeQL, generate SBOM artifact, và Trivy image scan cho 12 image có Dockerfile.
  - Deploy workflow (`deploy-droplet.yml`) tự chạy khi `push` vào nhánh chính `develop` (+ `workflow_dispatch`): build bằng `mvn -B clean verify` (không skip test), đóng gói tarball, SCP/SSH vào droplet rồi `scripts/deploy-from-artifact.sh` chạy `docker compose up -d --build`. Mỗi service tự chạy Flyway migration lúc khởi động ⇒ **merge code mới vào `develop` sẽ tự deploy code + migrate DB trên server** (dữ liệu giữ qua docker volume Postgres).
  - Deploy artifact có SHA-256 checksum và GitHub artifact attestation/provenance; deploy script verify checksum nếu file `.sha256` được upload.
  - Deploy script mặc định chỉ chờ các Eureka apps có source trong repo; `laundry-service`/`partner-service` có thể được thêm lại bằng env `EUREKA_EXPECTED_APPS` khi source thật được khôi phục.
  - Release workflow theo tag `v*` tạo tarball backend, root CycloneDX SBOM, checksum, GitHub provenance attestation, upload artifact và publish GitHub Release.
  - `scripts/verify-release-artifact.sh` hỗ trợ kiểm SHA-256 và, nếu có GitHub CLI, kiểm GitHub artifact attestation.

## 5. Mô Hình Locker Vật Lý

### Locker

`locker-service` sở hữu locker và box/cell.

Metadata quan trọng của locker:

- `id`
- `storeId`
- `code`
- `name`
- `status`
- `address`
- `latitude`, `longitude`
- `landingPad`
- `landingMarkerId`

### Box / Cell

Metadata quan trọng của box/cell:

- `id`
- `lockerId`
- `boxNumber`
- `size`
- `status`
- `cellType`
- `rowIndex`
- `colIndex`
- `faultReason`
- `reservedUntil` (mới 2026-06-16, không expose qua API — chỉ dùng nội bộ cho backstop sweep, xem dưới)

Loại cell hiện có:

- `DRONE`: ô hàng trên cho drone deposit, chỉ reserve khi `channel=DRONE`.
- `STANDARD`: ô bình thường cho customer/staff/SEND/LAUNDRY.
- `XL`: ô lớn hơn cho storage/rental.

`size` hiện có 2 giá trị thật trong demo seed (`MEDIUM` cho ô STANDARD/DRONE, `XL` cho ô vali) — `SMALL`/`LARGE` chỉ tồn tại trong logic fallback của `findAvailableBox` (2026-06-16, thứ tự SMALL→MEDIUM→LARGE→XL khi hết đúng size yêu cầu), chưa có dữ liệu seed thật để minh họa.

Vòng đời cell hiện tại:

```text
AVAILABLE -> RESERVED -> OCCUPIED -> AVAILABLE
AVAILABLE/RESERVED/OCCUPIED -> FAULT -> AVAILABLE (clear-fault)
AVAILABLE/FAULT -> OUT_OF_SERVICE | CLEANING -> AVAILABLE (return-to-service)   # L5
```

Hành vi quan trọng:

- `RESERVED`: flow đã giữ ô tủ. **(2026-06-16)** Mỗi lần reserve, `reservedUntil` được set = `now + app.locker.reserved-ttl-hours` (mặc định 24h, cùng cửa sổ `app.order.auto-cancel-hours`). `LockerScheduler.sweepExpiredReservations` (cron mỗi giờ) chỉ là **lưới an toàn**: order-service đã tự release ô khi auto-cancel đơn (sweep mỗi 15 phút) nên đường này hiếm khi cần kích hoạt, chỉ phòng khi sweep bên order-service gặp sự cố.
- `OCCUPIED`: vật phẩm được xem là đang nằm trong ô tủ.
- `FAULT`: trạng thái lỗi, chặn reserve bình thường cho đến khi clear.
- `EXPIRED`: hiện được đại diện ở cấp order/deadline, chưa là cell status riêng.

### Demo Cabinet

Migration `V3__seed_demo_cabinet.sql` seed:

- `CAB-DEMO-01`
- Landing pad bật.
- Marker `ARUCO-23`.
- 3 cell `DRONE`.
- 6 cell `STANDARD`.
- 1 cell `XL`.

## 6. Luồng Đơn Giặt

Đây là lifecycle giặt đồ cũ, vẫn tồn tại trong `order-service`.

Endpoint tạo đơn chính:

```http
POST /api/orders
```

Flow thông thường:

1. Customer chọn store, locker, ô gửi, dịch vụ/items.
2. Đơn được tạo với type/category như `LAUNDRY`.
3. Ô locker được reserve.
4. Customer nhận PIN.
5. Customer mở ô và xác nhận đã bỏ đồ vào.
6. Staff thu gom đơn.
7. Staff cân/xử lý đồ giặt.
8. Staff mark ready.
9. Staff trả đồ sạch vào ô nhận.
10. Customer lấy đồ.
11. Đơn hoàn tất và ô được release.

Endpoint quan trọng:

- `PUT /api/orders/{orderId}/confirm`
- `PUT /api/orders/{orderId}/collect`
- `PUT /api/orders/{orderId}/weight`
- `PUT /api/orders/{orderId}/process`
- `PUT /api/orders/{orderId}/ready`
- `PUT /api/orders/{orderId}/return?boxId=...`
- `PUT /api/orders/{orderId}/complete`
- `PUT /api/orders/{orderId}/cancel`
- `GET /api/orders/{orderId}/timeline`

Lưu ý hiện tại:

- Một số màn hình legacy trên frontend/mobile vẫn diễn tả flow laundry/courier cũ, có thể chưa đồng bộ hoàn toàn với các màn hình locker ops Phase 2.

## 7. Luồng SEND Parcel

SEND là flow gửi hàng từ người gửi đến người nhận qua locker.

Endpoint tạo đơn:

```http
POST /api/orders/send
```

Request mẫu:

```json
{
  "lockerId": 2,
  "boxId": null,
  "size": "MEDIUM",
  "receiverPhone": "0900000001",
  "receiverName": "Receiver Name",
  "note": "optional",
  "totalPrice": 0
}
```

Flow nghiệp vụ:

1. Sender đăng nhập.
2. Sender chọn locker và phone người nhận.
3. Nếu `boxId` không có, backend tìm cell `STANDARD` đang `AVAILABLE`.
4. Backend reserve cell.
5. Backend tạo order theo semantics SEND.
6. Backend trả về `pinCode` và `qrToken` ban đầu.
7. Sender mở ô và bỏ hàng vào.
8. Sender xác nhận đã bỏ hàng bằng `PUT /api/orders/{orderId}/confirm`.
9. Backend chuyển cell sang occupied.
10. Backend rotate PIN thành PIN cho người nhận.
11. Backend thử tìm user người nhận theo phone và set `receiverId` nếu có.
12. Backend gửi thông báo cho sender/receiver nếu có thể.
13. Receiver dùng PIN/QR để nhận hàng.
14. `PUT /api/orders/{orderId}/complete` hoàn tất order và release cell.

Endpoint quan trọng:

- `POST /api/orders/send`
- `PUT /api/orders/{orderId}/confirm`
- `PUT /api/orders/{orderId}/complete`
- `GET /api/orders/access/{code}`
- `POST /api/iot/verify-access`

Hành vi quan trọng:

- SEND dùng PIN hai giai đoạn.
- PIN ban đầu dành cho sender deposit.
- Sau confirm drop, PIN được rotate thành PIN pickup cho receiver.
- QR token gắn với PIN đang active, nên QR cũ sẽ invalid sau khi rotate PIN.
- **Mở tủ qua mobile (2026-06-16)**: chi tiết đơn trên mobile có nút "Mở tủ" gọi `POST /api/iot/unlock` (xem mục 19) thay vì chỉ hiện PIN/QR để người dùng tự nhập ở thiết bị cabinet — vẫn 2 bước thủ công riêng (mở tủ rồi mới bấm "Xác nhận đã bỏ hàng"/"Hoàn tất"), không tự động chain.
- **Đặt lại đơn (2026-06-16)**: `POST /api/orders/{orderId}/reorder` cho đơn SEND `COMPLETED`/`CANCELED` giờ gọi lại `createSend()` (tìm ô trống mới + sinh PIN/QR mới) thay vì hành vi cũ tạo đơn không có ô nào được giữ.

## 8. Luồng RENTAL

RENTAL là flow thuê ô tủ tạm thời theo giờ.

Endpoint tạo đơn:

```http
POST /api/orders/rental
```

Request mẫu:

```json
{
  "lockerId": 2,
  "boxId": null,
  "cellType": "STANDARD",
  "hours": 2,
  "note": "optional"
}
```

Flow nghiệp vụ:

1. Customer đăng nhập.
2. Customer chọn locker, loại cell và số giờ thuê.
3. Backend tìm/reserve cell phù hợp đang available.
4. Backend tính giá thuê:
   - `STANDARD`: mặc định `5000` VND/giờ.
   - `XL`: mặc định `10000` VND/giờ.
5. Backend tạo rental order và trả về PIN/QR.
6. Customer có thể mở ô trong thời gian thuê. PIN được dùng nhiều lần khi rental đang active.
7. Customer gia hạn rental:
   - `POST /api/orders/{orderId}/extend-rental`
8. Customer kết thúc rental:
   - `POST /api/orders/{orderId}/pickup-storage`
9. Backend complete order và release cell.

Hành vi quan trọng:

- PIN rental không bị consume sau một lần mở.
- Gia hạn rental tính phí theo loại cell thật.
- Phí quá hạn có thể được `order-service` tính khi pickup/end trễ.
- **Đặt lại đơn (2026-06-16)**: `POST /api/orders/{orderId}/reorder` cho đơn RENTAL `COMPLETED`/`CANCELED` giờ gọi lại `createRental()` với `cellType` suy từ ô của đơn cũ (`cellTypeOfRental()`) và `hours` suy từ khoảng `createdAt`→`pickupDeadline` của đơn cũ (clamp 1-720, fallback 1h) — trước đó tạo đơn không có ô/giá `0`/không hạn thuê.

## 9. Luồng Truy Cập Bằng PIN Và QR

PIN và QR là credential truy cập cho các order đang active.

Order response có:

- `pinCode`
- `qrToken`

QR format stateless và được backend ký. Implementation hiện tại gắn với:

- order id
- active PIN
- HMAC secret

QR secret dùng cùng production secret policy với JWT:

- Secret phải dài tối thiểu 32 UTF-8 bytes.
- Profile `prod`/`production` không được dùng giá trị demo/dev/change-me/default.

Endpoint tra cứu/access:

- `GET /api/orders/access/{code}`: lookup public/gateway cho PIN hoặc QR.
- `GET /internal/orders/by-access?code=...`: lookup service-to-service cho PIN hoặc QR.
- `POST /api/iot/verify-access`: cabinet/device xác minh `boxId` kèm PIN/QR.

Flow nghiệp vụ:

1. User nhập PIN hoặc scan QR.
2. IoT service hỏi order service để resolve access.
3. IoT validate access có đúng box được yêu cầu không.
4. IoT gửi/chấp nhận lệnh mở cửa.
5. Order flow quyết định lần mở này là confirm, complete, hay chỉ mở trong rental.

Lưu ý hiện tại:

- Tablet-web cabinet UI là việc tương lai.
- Một số màn hình mobile QR scanner cũ vẫn phục vụ QR-login. Các màn hình locker ops mới đã render QR access, nhưng UX scan tại cabinet vẫn thuộc Phase 3.

## 10. Luồng Uỷ Quyền Nhận Hộ

Uỷ quyền cho phép chủ đơn cho người khác nhận/mở ô.

Endpoint:

```http
POST /api/orders/{orderId}/delegate
```

Flow nghiệp vụ:

1. Owner chọn order active có thể uỷ quyền.
2. Owner nhập phone/tên/ghi chú của người được uỷ quyền.
3. Backend validate quyền owner/receiver.
4. Backend rotate hoặc cấp credential truy cập.
5. Backend lưu thông tin receiver và gửi thông báo nếu có thể.
6. Người được uỷ quyền dùng PIN/access code theo trạng thái order.

Trạng thái hiện tại:

- Backend endpoint đã có.
- Flutter locker order detail sheet có action delegation trong module locker ops.
- Một số màn hình delegation cũ vẫn nằm ở `lib/features/delegations`.

## 11. Luồng Báo Lỗi Và Bảo Trì

Fault flow được implement trong `locker-service`.

User/customer report:

```http
POST /api/boxes/{id}/fault
POST /api/lockers/{id}/report
GET /api/lockers/my-reports
```

Admin/manager/maintenance xem và xử lý:

- Admin:
  - `GET /api/admin/lockers/reports`
  - `PUT /api/admin/lockers/reports/{id}/resolve`
  - `POST /api/admin/lockers/boxes/{id}/clear-fault`
- Manager:
  - `GET /api/manage/lockers/reports`
- Maintenance:
  - `GET /api/maintenance/faults`
  - `GET /api/maintenance/reports`
  - `PUT /api/maintenance/reports/{id}/claim`
  - `PUT /api/maintenance/reports/{id}/resolve`
  - `POST /api/maintenance/boxes/{id}/clear-fault`
  - `POST /api/maintenance/boxes/{id}/force-open` (mới 2026-06-16: mở ô khẩn cấp không cần PIN khách, luôn ghi audit log — xem mục 19)
  - `GET /api/maintenance/my-rating-average` (mới 2026-06-16: điểm đánh giá trung bình KTV nhận được)

Flow nghiệp vụ:

1. User/customer/staff báo ô tủ bị lỗi.
2. Backend mark cell thành `FAULT`.
3. Backend tạo/cập nhật locker report với box id và lý do.
4. Cell bị lỗi bị loại khỏi luồng reserve bình thường.
5. Backend trả danh sách fault/report kèm locker name/code/address/toạ độ, thông tin ô, và **(mới 2026-06-16)** tên/SĐT khách báo cáo (`reporterName`/`reporterPhone`, tra qua `user-service`, best-effort — không vỡ list nếu lookup lỗi).
6. Maintenance user xem các report đang mở, ưu tiên theo trạng thái/SLA, và mở chỉ đường tới tủ lỗi.
7. Maintenance claim report: `OPEN -> IN_PROGRESS`.
8. Maintenance resolve report.
9. Backend clear fault và đưa cell về `AVAILABLE`.
10. **(Mới 2026-06-16)** Sau khi report `RESOLVED`, khách có thể đánh giá 1-5 sao (`POST /api/lockers/reports/{id}/rate`, upsert — đánh giá lại sẽ ghi đè), xem lại bằng `GET /api/lockers/reports/{id}/rating`. Maintenance xem điểm trung bình của chính mình qua `GET /api/maintenance/my-rating-average` (tính trên các report được `assignedToUserId` = mình).

Lưu ý hiện tại:

- `GET /api/maintenance/faults` và `GET /api/maintenance/reports` là nguồn dữ liệu chính cho mobile/web maintenance; client không gọi `/internal/**`.
- Work log của technician (`repair_logs`) vẫn chưa hiện cho khách; bảo trì theo tần suất sử dụng vẫn là việc tương lai. Lịch bảo trì định kỳ (`maintenance_schedules`) đã có cả mobile và web (xem mục 13/21).
- **Customer↔Maintenance qua lại (2026-06-16)**: khi maintenance `claim` hoặc `resolve` một report, `LockerService` publish event mới `locker.report.claimed`/`locker.report.resolved` (RabbitMQ exchange `laundry.events`, có binding riêng trong `notification-service`) kèm `userId` của khách đã báo cáo + message tiếng Việt có tên tủ; `notification-service` tạo notification thật (push/STOMP) cho khách. Khách xem trạng thái report của mình qua mobile, màn mới "Báo cáo của tôi" (`GET /api/lockers/my-reports`), và giờ có thể đánh giá ngược lại khi report xong — khép kín vòng phản hồi 2 chiều. Cả `POST /api/lockers/{id}/report` và `POST /api/boxes/{id}/fault` đều ghi vào cùng bảng `locker_reports` nên cùng được loop này phủ.
- **Force-open khẩn cấp (2026-06-16)**: maintenance có thể mở ô không cần PIN khách qua `POST /api/maintenance/boxes/{id}/force-open` (locker-service gọi Feign `IotClient` → `POST /internal/iot/force-unlock` ở iot-service). Mọi lần mở (PIN/QR khách lẫn MASTER override) đều ghi vào bảng audit `box_access_logs` (actor, credential type, kết quả) — xem mục 19.

### 11.1 Bảo Trì Đội Drone (Fleet) — mới 2026-06-22

Đây là domain mới **"con drone vật lý"** (thiết bị bay), khác hoàn toàn với ô tủ `cellType=DRONE` (ô nhận hàng drone thả xuống, đã có từ Phase 2). Sống trong `locker-service` (bảng `drone_units`/`drone_maintenance_logs`, migration `V10__drone_units.sql`).

Model `DroneUnit`:

- `code` (duy nhất), `lockerId` (tủ làm trạm gốc).
- `status`: `IDLE` (sẵn sàng) | `CHARGING` (đang sạc) | `IN_FLIGHT` (đang bay) | `MAINTENANCE` (đang bảo trì) | `FAULT` (lỗi, bắt buộc kèm `faultReason`).
- `batteryPercent` (0-100) và `assignedTechnicianId` — **hiện đều do kỹ thuật viên nhập tay**, chưa có telemetry thật từ drone (chưa có hardware/MQTT cho drone, khác với cabinet đã có simulator).
- Mỗi lần đổi trạng thái tự ghi 1 dòng vào `drone_maintenance_logs` (audit nhẹ), kỹ thuật viên cũng ghi tay thêm các bước xử lý khác.

Endpoint backend (`locker-service`, role MAINTENANCE/ADMIN qua gateway — dùng lại Path predicate/RBAC có sẵn của `/api/maintenance/**` và `/api/admin/lockers/**`, không cần đổi gateway):

- `GET /api/maintenance/drones`: danh sách toàn bộ drone, kèm `lockerCode/lockerName` và `assignedTechnicianName` (tra qua `user-service`, best-effort).
- `POST /api/maintenance/drones/{id}/claim`: kỹ thuật viên nhận phụ trách.
- `POST /api/maintenance/drones/{id}/status` (`{status, reason?}`): đổi trạng thái; `reason` bắt buộc khi chuyển sang `FAULT`.
- `POST /api/maintenance/drones/{id}/battery` (`{batteryPercent}`): cập nhật % pin nhập tay.
- `GET/POST /api/maintenance/drones/{id}/logs`: nhật ký bảo trì.
- `POST /api/admin/lockers/drones` (ADMIN, `{lockerId, code}`): thêm drone mới vào đội.

Mobile: tab mới **"Drone"** (tab thứ 5) trong `MaintenanceHomePage` — danh sách drone (badge màu theo trạng thái, icon pin theo mức, tên kỹ thuật viên phụ trách hoặc "Chưa nhận"), tap vào mở bottom sheet hành động (Nhận xử lý/Đổi trạng thái/Cập nhật pin %/Nhật ký bảo trì). Banner đầu tab ghi rõ pin/trạng thái bay là dữ liệu nhập tay, tránh hiểu nhầm là telemetry thật.

Demo seed: migration V10 seed 3 drone vào `CAB-DEMO-01` (`DRONE-01` IDLE 92%, `DRONE-02` CHARGING 41%, `DRONE-03` FAULT 15% kèm lý do) để tab có dữ liệu ngay không cần admin tạo trước.

Còn thiếu: gán drone cho một chuyến giao hàng/đơn cụ thể (battery-aware assignment), điều khiển bay/MQTT thật, realtime tracking vị trí — vẫn là "Drone delivery service đầy đủ" ở mục 24.

### 11.2 Mission Planner — Lập Kế Hoạch Bay (Flight Plan) — mới 2026-06-28

Tính năng kiểu **Mission Planner** (Mảng 1: lập kế hoạch bay) cho drone, **chỉ ở mobile**, **thuần frontend + data model offline** — KHÔNG gọi backend, KHÔNG đổi API/DB/event/role. Sống trong `smart-laundry-locker-mobile`, feature mới `lib/features/drone_mission/**`. Đây là bước chuẩn bị cho điều khiển bay thật (Mảng 2 — telemetry MAVLink real-time — **chưa làm**).

**Vai trò & vị trí:** mở từ tab **"Drone"** trong `MaintenanceHomePage` (nút "Lập kế hoạch bay (Mission Planner)"), route `/drone/mission-planner` (`AppRouter.droneMissionPlanner`). Không thêm role mới, không đụng gateway/RBAC (màn cục bộ, không gọi mạng).

**Mô hình dữ liệu (data model):**

- `MavCommand` (enum): các lệnh MAVLink `MAV_CMD` với `code` numeric chuẩn ArduPilot/PX4 — `TAKEOFF`(22), `WAYPOINT`(16), `LOITER_TIME`(19), `LOITER_TURNS`(18), `LOITER_UNLIM`(17), `RTL`(20), `LAND`(21), `CHANGE_SPEED`(178). Mỗi lệnh khai báo có gắn toạ độ/độ cao không + 1 param phụ có nghĩa (loiter time/turns, tốc độ).
- `MissionItem`: 1 dòng mission (frame MAVLink, command, lat/lng/alt, param1-4, autoContinue). Lệnh không toạ độ (RTL/CHANGE_SPEED) áp dụng tại vị trí điểm liền trước.
- `FlightMission`: home (điểm xuất phát) + danh sách item + `cruiseSpeed` (ước tính giờ bay) + `defaultAltitude`. Tính tổng quãng đường (haversine qua `pathPoints` từ home), ETA, số waypoint.

**Định dạng file mission (export/import):**

- **QGC WPL 110** (`.waypoints`) — định dạng chuẩn Mission Planner/QGroundControl đọc-ghi (tab-separated, item 0 = HOME frame `GLOBAL`, các item sau frame `GLOBAL_RELATIVE_ALT`). Cho phép liên thông với GCS thật (ArduPilot/PX4). Command code lạ khi import → fallback `WAYPOINT` (không vỡ).
- **JSON nội bộ** — lossless (giữ tên/cruiseSpeed/defaultAltitude/id/timestamp) để app reload chính xác.
- Lưu/đọc qua `file_picker` (`saveFile`/`pickFiles`). Ngoài ra có library cục bộ qua `shared_preferences` (lưu nhiều kế hoạch theo tên, mở lại/xóa).

**Thao tác UI:** chạm bản đồ (`flutter_map`+OSM) để thêm waypoint; marker đánh số + marker HOME; polyline nối đường bay; tap marker/list để sửa lệnh-độ cao-param hoặc xóa; chế độ "di chuyển" (chạm lại để đặt vị trí mới); reorder danh sách điểm; đặt HOME tại tâm bản đồ; summary bar (số điểm/quãng đường/ETA); xuất `.waypoints`/JSON, nhập file, lưu/mở library, tạo mới, xóa.

Bổ sung Mảng 2 (telemetry real-time) ở mục **11.3**. Drag marker trực tiếp trên map của Mảng 1 vẫn dùng chế độ "di chuyển".

### 11.3 Flight Data — Telemetry & Điều Khiển Real-time (MAVLink) — mới 2026-06-28

Tính năng **Flight Data** (Mảng 2): kết nối flight controller **ArduPilot/PX4 thật** (hoặc SITL simulator) qua giao thức **MAVLink**, hiển thị drone live + HUD + gửi lệnh. **Chỉ ở mobile**, **không qua backend** (app nói chuyện thẳng với autopilot qua mạng), KHÔNG đổi API/DB/event/role. Feature `lib/features/drone_telemetry/**`.

**Vai trò & vị trí:** mở từ tab **"Drone"** trong `MaintenanceHomePage` (nút "Telemetry & điều khiển (Flight Data)"), route `/drone/flight-data` (`AppRouter.droneFlightData`). Không thêm role/gateway.

**Giao thức MAVLink (tự viết, thuần Dart, không thêm dependency):**

- `MavlinkCrc` — checksum CRC-16/MCRF4XX + bảng `CRC_EXTRA` theo `common.xml` (đã verify khớp check-value chuẩn `0x6F91`, nên gửi/nhận tương thích autopilot thật).
- `MavlinkProtocol` — parse khung **MAVLink v1 (0xFE) + v2 (0xFD)**: buffer streaming, resync khi sai CRC, bỏ qua message lạ, học `system_id`/`component_id` của drone từ HEARTBEAT để gửi lệnh đúng đích. Encode: `COMMAND_LONG`(#76), `SET_MODE`(#11), `REQUEST_DATA_STREAM`(#66), `HEARTBEAT`(#0).
- Message decode: HEARTBEAT(#0, armed + flight mode), SYS_STATUS(#1, pin V/A/%), GPS_RAW_INT(#24, fix + sat), ATTITUDE(#30, roll/pitch/yaw), GLOBAL_POSITION_INT(#33, vị trí + alt + heading), VFR_HUD(#74, tốc độ/alt/climb), COMMAND_ACK(#77), STATUSTEXT(#253).

**Transport (`dart:io`, cross-platform, không cần plugin native):**

- **UDP** (`UdpMavConnection`) — bind cổng local (mặc định 14550), học địa chỉ drone từ datagram đầu tiên (hợp SITL "broadcast tới GCS") hoặc nhập sẵn IP drone. Đây là link phổ biến nhất cho ArduPilot/PX4 qua WiFi.
- **TCP** (`TcpMavConnection`) — vd SITL `tcp:127.0.0.1:5760` hoặc bridge telemetry TCP.
- **USB-serial + Bluetooth: chưa làm** (cần plugin native Android + cấu hình USB-host). Abstraction `MavConnection` đã sẵn để cắm vào sau.

**Màn hình `FlightDataPage`:** bản đồ `flutter_map`+OSM với marker drone xoay theo heading + vệt đường bay (trail); **HUD** gồm chân trời nhân tạo (`AttitudeIndicator`, vẽ roll/pitch + thang pitch) + readouts (độ cao tương đối, tốc độ đất, leo/xuống, hướng, pin V/%, GPS fix + số vệ tinh, chế độ bay, ARMED/DISARMED, trạng thái link); sheet kết nối (chọn UDP/TCP, host, cổng). `FlightDataController` (ChangeNotifier) giữ link sống bằng HEARTBEAT 1Hz và gửi `REQUEST_DATA_STREAM` khi nhận heartbeat đầu.

**Lệnh điều khiển (qua COMMAND_LONG/SET_MODE):** ARM/DISARM (`MAV_CMD_COMPONENT_ARM_DISARM` 400), RTL/LOITER/GUIDED/AUTO (đổi mode ArduCopter: 6/5/4/3), TAKEOFF (`MAV_CMD_NAV_TAKEOFF` 22, cần GUIDED + đã ARM). Lệnh nguy hiểm (ARM/RTL/AUTO/DISARM) có dialog xác nhận. **Mode number theo ArduCopter** — PX4 dùng map khác (hiện hiển thị `MODE <số>`).

**An toàn/ghi chú:** tính năng điều khiển bay thật — chỉ dùng khi có thẩm quyền vận hành drone; mode/lệnh đang nhắm ArduPilot (phổ biến với Mission Planner). Chưa smoke với SITL/drone thật trong phiên tạo (kết nối cần peer); đã verify codec bằng unit test (9/9, gồm CRC chuẩn).

## 12. Luồng Manager Operations

Manager là flow vận hành, không phải full admin.

Flutter Manager Home gồm các tab:

- Stats.
- Locker layout.
- Orders.

Nguồn dữ liệu backend:

- `GET /api/manage/lockers/stats`
- `GET /api/manage/lockers`
- `GET /api/manage/lockers/{id}/layout`
- `GET /api/manage/orders`
- `GET /api/manage/orders/statistics`

Flow nghiệp vụ:

1. Manager đăng nhập.
2. Flutter role routing đưa `MANAGER` đến `/manager`.
3. Manager xem utilization và số report đang mở.
4. Manager mở layout cabinet để xem loại/trạng thái từng cell.
5. Manager xem các order hiện tại theo status/type.
6. Manager có thể report/clear cell qua UI action nếu đã wire.

## 13. Luồng Admin Web

React web frontend có route admin dưới `/admin`.

Admin sidebar hiện gồm:

- Dashboard.
- Users.
- Stores.
- Services.
- Lockers.
- Maintenance.
- Orders.
- Payments.
- Partners.
- Feedback.
- Scheduler.
- Notifications.
- Promotions.

Trang admin locker mới của Phase 2:

- `/admin/lockers`
- `/admin/lockers/:lockerId` (`layout-view.tsx` — grid cell + action lifecycle: báo hỏng/đã sửa/ngưng dùng/vệ sinh/khôi phục/**mở khẩn cấp** cho từng ô, không phân biệt trạng thái)
- `/admin/maintenance` (claim/resolve report, work-log, **2026-06-16**: thêm contact khách trên report, section "Bảo trì định kỳ" tạo/đã-kiểm-tra/xóa lịch, section "Sức khỏe thiết bị" liệt kê cabinet online/offline)

Lưu ý: trang `Admin/lockers/detail.tsx` + `BoxForceOpenModal.tsx` vẫn còn trong codebase nhưng **không được route tới** (`routes-config.tsx` chỉ đăng ký `layout-view.tsx` cho `/admin/lockers/:lockerId`) — đây là code chết, không sửa/xóa trong phiên 2026-06-16 nhưng không nên coi là tính năng đang hoạt động.

Flow nghiệp vụ:

1. Admin đăng nhập.
2. Admin xem dashboard tổng quan.
3. Admin quản lý master data: users, stores, services, lockers.
4. Admin mở trang layout locker để xem grid cell.
5. Admin xử lý bảo trì/fault reports.
6. Admin xem order và payment.
7. Admin trigger scheduler job thủ công khi cần.

Dashboard overview (`GET /api/admin/dashboard/overview`, do `order-service` phục vụ qua `OrderService.statistics()`):

- Trả các metric **order-owned**: `totalOrders`, `ordersToday`, `pendingOrders` (không COMPLETED/CANCELED), `totalRevenue` (sum `totalPrice` của đơn COMPLETED), `revenueToday`, và map `byStatus`.
- **Chưa** trả các KPI cross-service (`totalUsers`, `totalStores`, `totalLockers`, `activeServices`, `availableBoxes`, `occupiedBoxes`) vì thuộc service khác; web FE normalize default `0` cho các field này (thẻ hiển thị 0 thay vì crash). Cần wire aggregation chéo service nếu muốn số thật. *(Fix 2026-06-15: trước đó FE đọc field `undefined` → crash `Cannot read properties of undefined (reading 'toString')`.)*

Lưu ý hiện tại:

- UI `services`/partner catalog có thể phụ thuộc `laundry-service`/`partner-service` đang thiếu source hoặc API cũ.
- FE build hiện pass sau khi fix TypeScript, nhưng một số màn hình vẫn có thể gọi endpoint legacy.

## 14. Luồng Staff Operations

Staff service đóng vai trò facade cho assignment và thao tác order.

Flow nghiệp vụ:

1. Staff user được gán vào một order.
2. Staff xem đơn waiting/processing/ready.
3. Staff thu gom laundry/parcel.
4. Staff cập nhật trạng thái order thông qua order service.
5. Staff có thể mở ô locker thông qua facade endpoint.

Trạng thái hiện tại:

- Backend endpoints đã có.
- Một số docs tablet/partner/staff là legacy hoặc future-facing.
- Phase 2 mới tập trung vào Manager/Maintenance hơn là Staff.

## 15. Luồng Thanh Toán Và Hoàn Tiền

Payment service sở hữu payment records và refund records.

Endpoint customer/public:

- `POST /api/payments/topup/create` **(mới 2026-06-18, auth required)**: tạo VNPay URL nạp ví. Body: `{amount: decimal ≥1000, returnUrl?, bankCode?, locale?}`. Response: `{paymentUrl, txnRef}`. userId lấy từ `X-User-Id` header (inject bởi gateway).
- `GET /payments/vnpay/callback` **(mới 2026-06-18, PUBLIC)**: alias callback path để mobile WebView detect VNPay redirect. Cùng handler với `/api/payments/vnpay/return`. Route qua gateway không cần JWT.
- `POST /api/payments/checkout` **(mới 2026-06-21, auth)**: thanh toán đơn. Body `{orderId, method: WALLET|VNPAY|MOMO|CASH, bankCode?, returnUrl?, language?}`. WALLET/CASH settle ngay (COMPLETED + event); VNPAY/MOMO trả `url`/`deeplink`/`qr` để redirect. Amount lấy từ order-service (không tin client); chặn double-pay.
- `GET /api/wallet`, `GET /api/wallet/transactions` **(mới 2026-06-21, auth)**: số dư ví + lịch sử (userId từ `X-User-Id`).
- `POST /api/payments`
- `POST /api/payments/create`
- `PATCH /api/payments/{id}/status`
- `GET /api/payments/{id}`
- `GET /api/payments`
- `GET /api/payments/order/{orderId}`
- `GET /api/payments/vnpay/return`
- `GET /api/payments/vnpay/ipn`
- `POST /api/payments/momo/callback`
- `GET /api/payments/momo/return`
- `POST /api/payments/{paymentId}/refund`
- `GET /api/payments/order/{orderId}/refunds`
- `GET /api/payments/refund/{refundId}`

Endpoint admin:

- `GET /api/admin/wallet/{userId}`, `GET /api/admin/wallet/{userId}/transactions`, `POST /api/admin/wallet/{userId}/adjust` **(mới 2026-06-21)**: xem số dư/lịch sử ví + điều chỉnh (body `{amount, reason}`; dương = cộng, âm = trừ). Web admin: nút "Ví" trong bảng Users mở modal số dư + cộng/trừ.
- `GET /api/admin/payments`
- `PATCH /api/admin/payments/{id}/status`
- `GET /api/admin/payments/{paymentId}`
- `PUT /api/admin/payments/{paymentId}/status`

Flow nghiệp vụ:

1. Order cần thanh toán hoặc customer khởi tạo thanh toán.
2. Payment record được tạo.
3. Method có thể là cash/VNPay/MoMo tuỳ request/config.
4. Callback từ provider cập nhật payment status.
5. Payment thành công publish event.
6. Notification và loyalty có thể react với payment event.
7. Refund có thể được tạo và theo dõi.

Lưu ý hiện tại:

- Credential provider production và đối soát phụ thuộc environment.
- UX thanh toán cho SEND/RENTAL chưa hoàn tất end-to-end; order service đã expose flags/giá, nhưng product flow thanh toán cuối cùng cần làm tiếp.
- Khi chạy profile `prod`/`production`, payment service fail-fast nếu VNPay/MoMo config còn là demo, sandbox, localhost hoặc default placeholder.
- **(2026-06-18 → 2026-06-21) Wallet topup VNPay → ĐÃ NỐI VÍ**: nạp VNPay thành công giờ **cộng số dư ví** (bảng `payment_schema.wallets` + `wallet_transactions`, migration **V3**; idempotent theo `txnRef`). Thêm **thanh toán đơn** `POST /api/payments/checkout` (Ví/VNPay/MoMo/Tiền mặt). order-service **lắng nghe `PAYMENT_COMPLETED`** (queue `order.payment.events`) → set đơn `payment_status=PAID`+`paid_at` (migration order **V5**). **MoMo** có tích hợp thật (`MomoService`: AIO v2 create + HMAC SHA256 + verify IPN), kích hoạt khi cấu hình `MOMO_*` env (chưa cấu hình → checkout MoMo báo `MOMO_NOT_CONFIGURED`, không chặn boot). Thanh toán hiện **không bắt buộc** (chưa chặn cấp PIN). Hoàn tiền/điều chỉnh admin → cộng/trừ ví. `OrderResponse` thêm field `paymentStatus` (UNPAID/PAID/REFUNDED) để client biết đơn đã trả chưa. Mobile: cờ `walletEnabled/transactionsEnabled` đã bật, số dư đọc `GET /api/wallet`; **nút "Thanh toán" trong chi tiết đơn (locker_ops `my_locker_orders_page`)** mở bộ chọn Ví/VNPay/MoMo/Tiền mặt → `POST /api/payments/checkout` (Ví/Tiền mặt tức thì; VNPay/MoMo mở WebView). Xem mục 26.

## 16. Luồng Thông Báo

Notification service sở hữu notifications và FCM token records.

Endpoint internal:

- `POST /internal/notifications`
- `POST /internal/notifications/order-status`
- `POST /internal/notifications/broadcast`
- `POST /internal/fcm-tokens`
- `DELETE /internal/fcm-tokens`

Endpoint user:

- `GET /api/notifications/user/{userId}`
- `GET /api/notifications`
- `GET /api/notifications/all`
- `GET /api/notifications/unread`
- `GET /api/notifications/unread/count`
- `POST /api/notifications/fcm-tokens`
- `DELETE /api/notifications/fcm-tokens`
- `PATCH /api/notifications/{id}/read`
- `PUT /api/notifications/{id}/read`
- `PATCH /api/notifications/read-all`
- `PUT /api/notifications/read-all`
- `PUT /api/notifications/read-batch`
- `DELETE /api/notifications/{id}`
- `DELETE /api/notifications/all`

Endpoint admin:

- `GET /api/admin/notifications`
- `POST /api/admin/notifications/send`
- `POST /api/admin/notifications/broadcast`

Realtime:

- WebSocket/STOMP qua gateway `/ws`.
- Client nên gửi `Authorization: Bearer <access-token>` ở WebSocket handshake header và bắt buộc gửi trong STOMP `CONNECT`; notification-service verify JWT `tokenUse=access` và bind user principal theo `userId`.
- User client subscribe `/user/queue/notifications` để nhận notification realtime riêng tư.

Flow nghiệp vụ:

1. Order/payment/locker/iot event xảy ra.
2. Service publish RabbitMQ event hoặc gọi internal notification endpoint.
3. Notification service lưu notification.
4. Mobile/web đọc danh sách qua `/api/notifications`; mobile đăng ký device token qua `/api/notifications/fcm-tokens` sau khi có JWT.
5. Có thể push FCM nếu có token/config.
6. Có thể broadcast qua WebSocket.
7. User/admin đọc hoặc mark read notification.

Lưu ý hiện tại:

- Firebase production credentials phụ thuộc environment.
- Flutter notification client đã được cập nhật để dùng `/api/notifications/**`, parse response phẳng hiện tại (`data` là list) và unread count dạng số.
- Flutter có STOMP realtime subscriber cho `/user/queue/notifications`; local runtime smoke qua gateway đã PASS với admin send -> private STOMP `MESSAGE`. Deploy/emulator vẫn cần chạy lại khi `api-dev` health route và Firebase/device sẵn sàng.

### 16.1 Push Trạng Thái Giao Hàng (Drone Delivery) — mới 2026-06-28

Phase **chỉ push notification** báo trạng thái chuyến giao hàng (drone) cho người nhận — **CHƯA** làm live map/websocket tracking/MQTT (phase sau). Tái dùng hạ tầng FCM sẵn có; nhắm theo **userId người nhận** (không đăng ký token theo order).

**6 mốc trạng thái (contract):** `dispatched` (drone xuất phát) · `approaching` (sắp đến) · `arrived` (đã đến điểm giao) · `delivered` (giao thành công) · `delayed` (trễ, kèm `eta`) · `failed` (giao hỏng, drone quay về). Mỗi mốc có tiêu đề/nội dung tiếng Việt mặc định (xem `DeliveryNotificationService.DeliveryStatus` ở BE và `DeliveryStatus` ở mobile).

**Payload FCM (phần data):** `{ orderId, status, eta, message, type:"ORDER_STATUS_CHANGED", referenceId:orderId, referenceType:"DELIVERY", notificationId }`. Gửi dạng **hybrid notification+data**: foreground app tự render local notification; background/terminated để system tray hiển thị.

**Backend (notification-service):**

- `POST /internal/notifications/delivery-status` (internal, service-to-service — gateway chặn `/internal/**`): body `{orderId, receiverUserId, status, eta?}` → `DeliveryNotificationService.notifyDeliveryStatus` build title/body theo status → lưu lịch sử (`NotificationMessage`) + đẩy FCM tới mọi token của `receiverUserId` (`FcmPushNotificationService.sendToUser`) + WebSocket.
- Event `delivery.status.changed` (mới, `DomainEventNames.DELIVERY_STATUS_CHANGED`) — binding trong `RabbitConfig`, `NotificationEventListener` route sang `DeliveryNotificationService.notifyFromEvent` (payload kỳ vọng `orderId/userId/status/eta`). Dành cho **luồng giao drone thật bắn ra (TODO)**; hiện test bằng gọi internal endpoint trực tiếp.
- `NotificationService.create(request, extraData)` (overload mới): merge `extraData` (orderId/status/eta/message) vào phần data FCM — backward-compatible, không ảnh hưởng client/luồng cũ.

**Mobile (`smart-laundry-locker-mobile`):**

- Quyền: đã xin Android 13+ (`POST_NOTIFICATIONS`) + iOS; thêm `NotificationPermissionHelper` (kiểm tra/xin + hộp thoại hướng dẫn mở **Settings** khi bị từ chối vĩnh viễn).
- Token: đăng ký `POST /api/notifications/fcm-tokens` sau đăng nhập (backend lấy userId từ JWT) **+ `onTokenRefresh`** tự đăng ký lại khi token xoay vòng.
- Nhận noti ở **cả 3 trạng thái app**: foreground (local notification + cập nhật `NotificationProvider`/`AppEventBus`), background & terminated (system tray).
- **Tap noti → deep-link chi tiết đơn** theo `orderId` (route `order_detail` nhận `orderId`): qua `onMessageOpenedApp` + `getInitialMessage` (background/terminated) **và** tap banner foreground (local-notif, payload JSON). `DeliveryNotification.fromData` bóc tách data; status lạ → `unknown` (không vỡ).
- **Tự cập nhật trạng thái đơn**: data `type:ORDER_STATUS_CHANGED` + `referenceId:orderId` → `NotificationProvider` emit `OrderChangedEvent(orderId)` → `OrderPage`/`CustomerOrderDetailPage` tự reload (cơ chế `AppEventBus` sẵn có).
- **Lịch sử thông báo** (`NotificationListPage`, `/api/notifications`): noti giao hàng có icon `truck`, tap → chi tiết đơn.

**Chưa làm / phụ thuộc:** push thật cần Firebase credential production + thiết bị; luồng giao drone thật bắn `delivery.status.changed` (gắn `drone-service`/order-service) vẫn TODO; live map real-time là phase sau (ngoài scope).

## 17. Luồng Store

Store service sở hữu dữ liệu cửa hàng.

Endpoint public/customer:

- `GET /api/stores`
- `GET /api/stores/nearby`
- `GET /api/stores/{id}`
- `GET /api/stores/{storeId}/ratings`

Endpoint mutation/admin:

- `POST /api/stores`
- `PUT /api/admin/stores/{id}`
- `POST /api/admin/stores`
- `GET /api/admin/stores`
- `GET /api/admin/stores/{id}`
- `PUT /api/admin/stores/{id}/status`
- `PUT /api/admin/stores/{id}/image`
- `DELETE /api/admin/stores/{id}`

Flow nghiệp vụ:

1. Customer xem danh sách cửa hàng.
2. Dữ liệu store hỗ trợ tạo order và nhóm locker.
3. Admin quản lý status, thông tin và hình ảnh store.
4. Store rating được lấy từ order service qua internal client.

Tiêu thụ phía mobile (2026-06-13):

- Flutter customer có màn Cửa hàng (`/stores`, `/stores/detail`) gọi `GET /api/stores`, `GET /api/stores/{id}`, `GET /api/stores/{storeId}/ratings`, và `GET /api/stores?latitude&longitude` cho chế độ "gần tôi". Entry từ home ("Khám phá cửa hàng").

## 18. Luồng Loyalty

Loyalty service sở hữu điểm, stamp, reward và redemption history.

Endpoint customer:

- `GET /api/loyalty/users/{userId}`
- `GET /api/loyalty/summary`
- `GET /api/loyalty/points`
- `GET /api/loyalty/points/history`
- `POST /api/loyalty/redeem-points`
- `POST /api/loyalty/redeem-stamp`
- `GET /api/loyalty/stamps`
- `GET /api/loyalty/stamps/{stampCardId}`
- `GET /api/loyalty/rewards`
- `POST /api/loyalty/rewards/{rewardId}/redeem`
- `GET /api/loyalty/points/expiring`
- `POST /api/loyalty/points`

Endpoint admin:

- `POST /api/admin/loyalty/users/{userId}/points`
- `GET /api/admin/loyalty/users/{userId}/history`
- `GET /api/admin/loyalty/users/{userId}`
- `GET /api/admin/loyalty`
- `GET /api/admin/loyalty/statistics`

Flow nghiệp vụ:

1. User nhận điểm từ hoạt động/payment event nếu đã wire.
2. User xem điểm/stamp/reward.
3. User redeem điểm/stamp/reward.
4. Admin điều chỉnh điểm và xem history.

Lưu ý hiện tại:

- Kết nối event từ payment/order sang loyalty cần được verify trước khi gọi là production-complete.

## 19. Luồng IoT Device

IoT service sở hữu device status và command facade.

Endpoint:

- `POST /api/iot/device-status`
- `GET /api/manage/iot/device-status` (mới 2026-06-16: liệt kê toàn bộ device status cho dashboard Manager/Admin — trước đó dữ liệu chỉ được ghi, không có cách đọc lại)
- `GET /api/manage/iot/box-status` (mới 2026-06-30, **GAP 2**: liệt kê trạng thái **phần cứng** ô cabinet báo lên — `?lockerId=` lọc theo tủ — Manager/Admin; tách khỏi trạng thái logic theo đơn, chỉ để đối chiếu)
- `GET /internal/iot/box-status` (mới 2026-06-30, service-to-service, chặn qua gateway: locker-service gọi để ghép với trạng thái logic dựng view box-health cho bảo trì — xem mục 11/`GET /api/maintenance/lockers/{lockerId}/box-health`)
- `POST /api/iot/unlock`
- `POST /api/iot/verify-pin`
- `POST /api/iot/verify-access`
- `POST /api/iot/pickup`
- `POST /api/iot/box-status`
- `POST /internal/iot/device-status`
- `POST /internal/iot/force-unlock` (mới 2026-06-16: maintenance override, gọi từ `locker-service`, chặn qua gateway public)
- `POST /internal/iot/box-sync` (mới 2026-06-29: **booking → IoT sync (GAP 1)** — gọi từ `locker-service` khi ô đổi vòng đời `RESERVED/OCCUPIED/AVAILABLE/FAULT`, publish MQTT `cabinet/{lockerId}/command/sync` để tủ mô phỏng đồng bộ; chặn qua gateway public; best-effort, không chặn luồng đặt đơn)

Flow nghiệp vụ:

1. Cabinet/tablet/RPi báo status.
2. User nhập PIN hoặc scan QR.
3. Device gọi verify endpoint.
4. IoT service verify với order service và locker service.
5. IoT service chấp nhận/publish lệnh mở qua MQTT facade.
6. Device báo box status/open result.
7. Backend cập nhật order/locker state nếu flow đã wire.
8. **(Mới 2026-06-16)** Mọi lần mở (bước 5 thành công hoặc thất bại) được ghi vào bảng `iot_schema.box_access_logs` (boxId, lockerId, orderId nếu có, actorUserId, credentialType `PIN_OR_QR`|`MASTER`, result, message, thời gian). Maintenance cũng có thể bỏ qua bước 2-4 và gọi trực tiếp force-open (`POST /api/maintenance/boxes/{id}/force-open` → `/internal/iot/force-unlock`) khi cần mở ô không có PIN khách hợp lệ (vd ô FAULT không còn order active) — vẫn ghi audit log với `credentialType=MASTER`.
9. **(Mới 2026-06-16)** `verifyAccess()` đếm số lần verify sai theo `boxId` (bảng `iot_schema.access_attempts`); quá `app.iot.lockout.max-attempts` (mặc định 5) trong cùng một chuỗi thì khóa box `app.iot.lockout.minutes` (mặc định 15) phút, từ chối mọi verify trong lúc đó kể cả PIN đúng.

Lưu ý hiện tại:

- **Booking → IoT sync (GAP 1, mới 2026-06-29)**: trước đây đặt đơn (SEND/RENTAL) chỉ đổi trạng thái ô trong DB của `locker-service`, **không** báo cabinet — tủ chỉ nghe đúng lệnh "mở ô này ngay" (`/api/iot/unlock`), không bao giờ biết ô vừa được giữ/chiếm/giải phóng. Nay `LockerService.reserveBox/occupyBox/releaseBox/markFault/clearFault` gọi best-effort `IotClient.syncBoxState` → `POST /internal/iot/box-sync` (iot-service) → publish MQTT `cabinet/{lockerId}/command/sync` body `{boxId, state, orderId?}`. Đây là **thông báo một chiều fire-and-forget** (không chờ reply, nuốt mọi lỗi broker) nên iot-service/broker chết cũng không làm hỏng luồng đặt đơn. Simulator `simulate_demo_cabinet.py` subscribe topic này và log như thể cabinet cập nhật sơ đồ ô trên màn hình. Khép kín "đặt đơn → tủ mô phỏng đồng bộ".
- **Trạng thái phần cứng ô (GAP 2, mới 2026-06-30)**: cabinet báo trạng thái cửa/cảm biến thật trên `cabinet/{lockerId}/locker/{boxId}/status` (boxId trong `slotIndex`, kèm `hwState`). Trước đây `IotService.updateBoxStatus` chỉ publish event `iot.device.status.changed` rồi vứt — không lưu gì. Nay **upsert** vào bảng riêng `iot_schema.box_hardware_status` (V3: `box_id` PK, `locker_id`, `hw_state`, `last_reported_at`) **tách hẳn** khỏi `LockerBox.status` (order-driven, locker-service sở hữu) — đây là *hardware truth*, **KHÔNG bao giờ tự ghi đè** trạng thái logic theo đơn (tránh xung đột state machine). Đọc qua `GET /api/manage/iot/box-status` (Manager/Admin) để đối chiếu/phát hiện lệch. Simulator báo cửa OPEN rồi CLOSED (`SIM_DOOR_CLOSE_SECONDS`) sau mỗi lần mở thành công. **Occupy/release tự động từ sensor vẫn là Phase 3** (chưa lái vòng đời ô); chưa có surface cho MAINTENANCE (mobile/`/api/maintenance/...`) — follow-up.
- Python `smart-locker-iot` cần config MQTT broker khớp với backend và chế độ hardware/simulation.
- Drone vẫn là future channel riêng, chỉ dùng cell `DRONE` khi `channel=DRONE`.
- **Device health dashboard (2026-06-16)**: heartbeat/online-offline giờ xem được qua `GET /api/manage/iot/device-status` + section "Sức khỏe thiết bị" trong web admin — trước đó dữ liệu được ghi (`POST /api/iot/device-status`) nhưng không ai đọc lại được. Vẫn là poll REST (15s/khi load trang), **chưa** push realtime — event `iot.device.status.changed` vẫn chưa có consumer nào tiêu thụ. **2026-06-30 (GAP 3)**: trước đây dashboard luôn rỗng vì **không ai publish** heartbeat (`main.py` chờ setup handshake, simulator cũ chỉ trả lời lệnh mở). Nay `simulate_demo_cabinet.py` tự phát heartbeat: học cabinet từ traffic (`cabinet/{id}/command/open|sync`) + seed env `SIM_HEARTBEAT_CABINETS`, định kỳ (`SIM_HEARTBEAT_SECONDS`, mặc định 30s) publish `cabinet/{id}/heartbeat` `{status:"ONLINE"}` → `LockerMqttService` → `IotService.updateStatus` cập nhật `lastSeenAt` ⇒ thiết bị hiện **ONLINE** thật khi simulator chạy. Thuần simulator, backend không đổi.
- **Mô phỏng mở tủ cho mobile (2026-06-16)**: `main.py` (hardware-track) chỉ trả lời lệnh mở sau khi nhận handshake `SETUP_LOCKERS` qua `iot/{macAddress}/command/setup` — chưa có code Java nào gửi handshake này, nên `main.py` (kể cả `SIMULATION=true`, cờ đó chỉ mock tầng serial) sẽ không trả lời `iot-service`. Thêm script độc lập `smart-locker-iot/simulate_demo_cabinet.py` (không sửa `main.py`/serial/setup) subscribe `cabinet/+/command/open` trực tiếp và trả lời đúng payload Java thật gửi (`{commandId, box_id, action, timeout}`, **không có** `lockerId`/`slotIndex` mà `main.py` mong đợi) trên `cabinet/{lockerId}/command/open/result`. Mobile gọi `POST /api/iot/unlock` qua nút "Mở tủ" trong chi tiết đơn. Khi có hardware + setup handshake thật, retire script này và dùng `main.py`; không cần đổi backend vì cùng contract MQTT.

## 20. RabbitMQ Events

Exchange:

```text
laundry.events
```

Event name hiện có:

- `order.created`
- `order.status.changed`
- `payment.completed`
- `payment.failed`
- `notification.requested`
- `locker.box.opened`
- `locker.box.fault`
- `iot.device.status.changed`
- `delivery.status.changed` (mới 2026-06-28: trạng thái giao hàng drone — notification-service consume → push FCM tới người nhận; xem mục 16.1. Luồng giao drone thật bắn event này vẫn TODO.)

Hành vi event:

- Order service emit event vòng đời order.
- Payment service emit event payment.
- Locker service emit event box/fault.
- IoT service emit event device status.
- Notification service listen và tạo notification; RabbitMQ consumer dùng `SimpleMessageConverter` với allow-list package dự án/JDK cần thiết cho `DomainEvent`, không bật trust-all deserialization.

## 21. Luồng Flutter Mobile

Flutter routing có cả flow cũ và mới.

### Flow Phase 2 đã verify hiện tại

Role routing:

- `MANAGER` hoặc `ADMIN` -> `/manager`
- `MAINTENANCE` -> `/maintenance-home`
- Các role còn lại -> `/home`

Quick action customer:

- `Thuê tủ` -> `/locker/rent`
- `Gửi hàng` -> `/locker/send-parcel`
- `Đơn tủ` -> `/locker/my-orders`
- `Khám phá cửa hàng` -> `/stores` (entry mới trên home)

Revamp UI luồng tủ customer (2026-06-14):

- 3 màn `locker_ops` (Gửi hàng / Thuê tủ / Đơn tủ của tôi) được đồng bộ về design system `AISLShadcnTheme` (navy + Manrope, bo góc 16, card trắng viền `#E2E8F0`) thay cho palette xanh `opsPrimary` cũ.
- Design kit dùng chung: `ops_widgets.dart` (status/type color+label, format giá/ngày/`Còn…|Quá hạn…`, `OpsCard/OpsBanner/OpsPrimaryButton/OpsInfoRow/OpsEmptyState`, `AccessCredentials` hiển thị PIN dạng ô bấm-để-copy + QR có khung) và `locker_picker.dart` (chọn tủ qua bottom-sheet).
- Màn Gửi hàng: stepper 2 giai đoạn (bỏ hàng → người nhận lấy) đúng luồng PIN 2 giai đoạn; banner hướng dẫn; hiển thị phí gửi và hạn nhận có định dạng. **2026-06-16**: thêm chọn kích thước hàng (SMALL/MEDIUM/LARGE, mặc định MEDIUM) gửi kèm field `size` cho `POST /api/orders/send` — backend giờ fallback sang size lớn hơn nếu hết đúng size (xem mục 5/19 ghi chú cell).
- Màn Thuê tủ: card chọn loại ô (STANDARD/XL kèm kích thước+đơn giá), chip giờ nhanh + slider, thẻ giá tính live.
- Màn Đơn tủ của tôi: card đơn có countdown/cảnh báo quá hạn; detail sheet format ngày/giá, hiện phí phát sinh; **action gate đúng theo trạng thái+loại** (confirm bỏ đồ; hoàn tất; gia hạn/kết thúc thuê; ủy quyền; báo ô lỗi; hủy chỉ khi `INITIALIZED`); nút **Chỉ đường tới tủ** gọi `GET /api/lockers/{id}` rồi mở Google Maps bằng toạ độ hoặc địa chỉ.
- **2026-06-16**: detail sheet thêm 2 action mới ở đầu danh sách — **"Mở tủ"** (primary, hiện khi có `boxId`+`pinCode` và đơn chưa `COMPLETED`/`CANCELED`; gọi `POST /api/iot/unlock`, hiện snackbar "Đang mở tủ..." vì có thể chờ tới ~20s nếu simulator/hardware không chạy, không tự chain sang confirm/complete) và **"Đặt lại đơn"** (primary, hiện khi `COMPLETED`/`CANCELED`; gọi `POST /api/orders/{id}/reorder`). Dialog báo lỗi ô (`_reportDialog`) sau khi gửi thành công giờ có nút "Xem" trong snackbar dẫn tới màn mới.
- `flutter analyze` 0 error (debt info/warning cũ không đổi). Chưa smoke trên emulator phiên này.

Màn "Báo cáo của tôi" (mới, 2026-06-16):

- Route `/locker/my-reports` (hằng số `AppRouter.myLockerReports`, không trùng `AppRouter.myReports` route legacy `/maintenance/my-reports`), trang `lib/features/locker_ops/presentation/pages/my_reports_page.dart`. Đọc `GET /api/lockers/my-reports`, hiện card trạng thái (`OPEN/IN_PROGRESS/RESOLVED`) dùng lại `ops_widgets.dart` (`StatusChip`, `OpsCard`, `OpsBanner`). Đây là trang mới trong `locker_ops` (style đơn giản Map<String,dynamic>), **không phải** rewire màn `ReportListPage`/`CreateReportPage` cũ trong `lib/features/maintenance/**` — màn cũ đó dùng kiến trúc clean-arch khác (entity `MaintenanceReport` có field `code/staffNote/photoUrls` không khớp `LockerReportResponse` hiện tại, bắt chọn theo cây Location→Cabinet→Locker cũ và bắt chụp đúng 2 ảnh) nên giữ nguyên không sửa/xoá, không liên kết từ UI mới — đúng tiền lệ tab "Đơn hàng" trước đây cũng repoint sang trang mới thay vì sửa `OrderPage` legacy.
- **Cập nhật cùng ngày (chiều)**: card report ở trạng thái `RESOLVED` giờ hiện 5 sao để đánh giá (gọi `POST /api/lockers/reports/{id}/rate`) nếu chưa đánh giá, hoặc hiện lại điểm đã chấm (`GET /api/lockers/reports/{id}/rating`, 404 nếu chưa có — `LockerOpsService.getReportRating` bắt riêng case này, trả `null` thay vì throw).

Maintenance home — bổ sung 2026-06-30 (box-health phần cứng):

- Tab **"Kiểm tra tủ"** thêm card **"Tình trạng phần cứng ô"** (`_boxHealthCard`/`_boxHealthRow`): khi chọn tủ, gọi `GET /api/maintenance/lockers/{lockerId}/box-health` (`LockerOpsService.boxHealth`, best-effort — không vỡ trang nếu BE/IoT chưa có dữ liệu) hiển thị mỗi ô: trạng thái **logic theo đơn** (`StatusChip`) đặt cạnh trạng thái **phần cứng cửa** (Cửa MỞ/đóng/chưa có tín hiệu, kèm "Báo lúc ..."). Ô `needsAttention` (cửa mở nhưng không `OCCUPIED`) được tô đỏ + banner cảnh báo đếm số ô cần kiểm tra. Đây là **surface MAINTENANCE** cho dữ liệu GAP 2 (trước chỉ Manager/Admin web đọc được). `flutter analyze` 0 issue; chưa smoke emulator. Không đổi backend/contract.

Maintenance home — bổ sung 2026-06-16 (chiều):

- Bottom-sheet hành động của 1 ô (`_cellActions`) giờ luôn có thêm action **"Mở tủ khẩn cấp"** bất kể trạng thái ô — có dialog xác nhận ("hành động sẽ được ghi vào nhật ký") trước khi gọi `POST /api/maintenance/boxes/{id}/force-open`.
- Report card hiện thêm contact khách báo cáo (`reporterName`/`reporterPhone`) nếu backend trả về.
- Banner điểm đánh giá trung bình của KTV (`GET /api/maintenance/my-rating-average`) trên tab tổng quan ca trực, chỉ hiện khi đã có ít nhất 1 lượt đánh giá.
- **Làm lại UI (tối 2026-06-16, visual only)**: màn hình này trước đó là màn duy nhất chưa qua lần revamp design system (AppBar `#7F1D1D` cứng, `Card`/`Container` tự vẽ tay). Đã chuyển sang `BrandHeroHeader` + `OpsCard`/`OpsSectionLabel`/`OpsEmptyState`/`OpsBanner` đồng bộ với các màn customer; locker picker đổi sang `LockerPickerField` dùng lại từ Gửi hàng/Thuê tủ; cell-action bottom sheet đổi sang widget dùng chung mới `OpsSheetAction` (promote từ `_SheetAction` private của `my_locker_orders_page.dart` lên `ops_widgets.dart`, có thêm tham số `color` để giữ 5 màu hành động khác nhau). **Giữ nguyên 100% chức năng** — 4 tab, tên method/state, mọi endpoint gọi đều không đổi. `flutter analyze` 0 lỗi; chưa xem trực quan trên emulator phiên này.

Màn Cửa hàng (mới, 2026-06-13):

- `/stores`: danh sách cửa hàng, tìm theo tên/địa chỉ, nút "gần tôi" (geolocator) gọi `GET /api/stores?latitude&longitude`.
- `/stores/detail`: chi tiết cửa hàng (tên, trạng thái, địa chỉ, SĐT, mô tả, khoảng cách), nút Chỉ đường (mở Google Maps qua `url_launcher`), nút Xem tủ (`/lockers`), và danh sách đánh giá từ `GET /api/stores/{id}/ratings`.
- Feature: `lib/features/stores/**` (clean-arch: domain entity `Store`/`StoreRating` + infrastructure `StoreService` + presentation pages/widgets). Đã `flutter analyze` 0 error.

Maintenance home (cập nhật 2026-06-14):

- Role `MAINTENANCE` đăng nhập/auto-login được route tới `/maintenance-home` (`homeForRoles()` ở `splash_screen` và `login_screen`).
- `MaintenanceHomePage` gọi `/api/maintenance/faults`, `/api/maintenance/reports?mine=`, claim/resolve/clear-fault. Backend API đã có sẵn trong `locker-service`.
- UI có tổng quan ca trực (ô lỗi, phiếu mới, đang xử lý, việc của tôi), chip SLA theo thời gian mở phiếu, thông tin locker/ô/cell type, địa chỉ locker và nút **Chỉ đường** cho fault/report.
- Notification mobile client đã chuyển sang `/api/notifications/**`; FCM token sync dùng `/api/notifications/fcm-tokens`; realtime subscriber dùng WebSocket/STOMP `Authorization` và `/user/queue/notifications`. Backend runtime smoke local đã PASS list/count, FCM token save/delete và private STOMP `MESSAGE`; targeted `flutter analyze --no-pub` cho các file thay đổi PASS.

`locker_ops` service gọi:

- Lockers/layout.
- My orders.
- Create SEND.
- Create RENTAL.
- Confirm drop.
- Complete pickup.
- Extend rental.
- End rental.
- Cancel order.
- Delegate.
- Report fault.
- Manager stats/orders.
- Maintenance faults/reports/claim/resolve/clear.
- Notification list/unread/read-all và FCM token registration.

### Sửa lệch endpoint mobile↔backend (2026-06-14)

App Flutter có gốc từ app cũ (Revoland/courier) nên nhiều màn legacy gọi endpoint không khớp backend microservices hiện tại (thiếu prefix `/api` hoặc path đã đổi). ApiClient base = `API_BASE_URL` (không tự thêm `/api`), nên path phải tự viết đủ `/api/...`. Branch `fix/mobile-api-endpoints-alignment` đã sửa các màn người dùng gặp lỗi trực tiếp:

- **Tab Hồ sơ (Profile)**: `getProfile` đổi `/users/me` → `GET /api/user/profile` (+ normalize `id` int→String). Lỗi phụ: `ProfileProvider._getFaceRegistrationStatus` gọi `/auth/ai/registered/{id}` (AI legacy, 404) **không bắt lỗi** → `loadProfile()` ném → trang kẹt spinner và hiện sai "Bạn cần đăng nhập" dù đã login → đã bọc try/catch.
- **Đăng ký**: `register` đổi `/auth/register` → `POST /api/auth/register`, body từ `{fullName, role}` → `{firstName, lastName, roles:[...], phoneNumber, email, password}` đúng `RegisterRequest` backend; sau đăng ký chuyển sang tab Đăng nhập (backend cấp tài khoản ngay, bỏ bước OTP legacy).
- **Tab Tủ (danh sách tủ)**: cả 3 method `getLocations` / `getLocationsForCustomer` / `getLocationsForCourier` đổi `/locations*` → `GET /api/lockers`, map sang `LockerLocation` với null-safety (id/address/lat/long có thể null), lọc tìm kiếm client-side.

Đã verify on-device (emulator) bằng logcat + screenshot: tab Tủ hiện 2 tủ, tab Hồ sơ hiện profile, register trả `AUTH_REGISTERED`.

**Feature mobile legacy CHƯA wire backend hiện tại (gọi endpoint chết, nên ẩn hoặc dựng lại sau):**

- Ví/nạp tiền: `POST /api/payments/topup/create` đã có backend **(2026-06-18)**; `GET /wallet/balance` và `GET /payments/transactions*` vẫn chưa có (không có wallet service) — xem mục 26 để hoàn thiện.
- Khuôn mặt/QR-login: `/auth/ai/*`, `/auth/face-verify`, `/auth/qr/confirm` (không có AI service).
- Courier/giao hàng + đăng ký nhân viên: `/courier/*`, `/orders/courier/*`, `/staff-applications`.
- Ủy quyền (màn legacy): `/delegations/*` (luồng ủy quyền thật dùng `POST /api/orders/{id}/delegate`).
- Home: `/advertisements`, `/blogs` (404, đã biết, không chặn).
- **Tab Đơn hàng** (bottom nav): ĐÃ repoint (2026-06-14, commit `08567d3`) sang `MyLockerOrdersPage` (`locker_ops`, `GET /api/orders/my-orders`) thay cho `OrderPage` legacy (`/orders/me` chết). Tab này giờ hiện đơn tủ thật với đầy đủ action theo state machine. `OrderPage` legacy không còn được route.
- **Navigation bug fix (2026-06-15)**: `LockerUtilitiesRow` (quick action "Đơn tủ" trên tab Tủ) trước dùng `context.push(AppRouter.myLockerOrders)` → route `/locker/my-orders` ngoài `ShellRoute` → mất bottom nav bar và không có nút quay lại. Đã sửa sang `context.go(AppRouter.orders)` → chuyển tab `/orders` trong `ShellRoute` → bottom nav hiển thị đúng, hành vi tab chuẩn.
- **UI revamp (2026-06-15)**: Bottom nav frosted-glass không label; `MyLockerOrdersPage` header dùng `BrandHeroHeader(title='Đơn tủ')`; `ProfilePage` cấu trúc đổi sang `Column → [BrandHeroHeader, Expanded(ScrollView)]` khớp layout LockerPage; Home page có wallet card. Không đổi API/route/flow nghiệp vụ.
- Chi tiết tủ (khi bấm 1 tủ ở tab Tủ): `LockerDetailMapPage` chỉ render bản đồ từ `LockerLocation` (call legacy `getLockerCountBySize` đã bị comment-out), nên KHÔNG gọi endpoint chết — đã chạy. Tủ thiếu toạ độ (vd `LCK-Q1-01`) nay map sang `NaN` → hiện màn "toạ độ không hợp lệ" thân thiện thay vì pin (0,0); tủ có toạ độ (`CAB-DEMO-01`) render bản đồ bình thường.

### Các khu vực mobile legacy/partial

App vẫn còn route/feature cho:

- Old locker action flow.
- Locker map.
- Locker OTP.
- Delegations.
- Maintenance report creation/list.
- Courier dispatch/delivery.
- Face verify/registration.
- Transactions/top-up.
- Vouchers (route `/my-vouchers` còn tồn tại nhưng chip "Ưu đãi" trên home đã redirect về `/promotions` — xem mục 27).
- User laundry order.
- QR login scanner.

Không nên xem các feature trên là hoàn tất với backend hiện tại nếu chưa verify riêng.

Lỗi không chặn demo hiện tại:

- Home có thể gọi endpoint legacy như `/advertisements`, `/blogs`, `/wallet/balance`; backend hiện tại có thể trả `404`. **(2026-06-19)** Đã short-circuit ở client (không gọi nữa) — xem mục "Tạm ẩn tính năng" bên dưới.

### Sửa lỗi đỏ khi chạy trên web (2026-06-19)

Khi chạy `flutter run -d chrome`, app báo nhiều lỗi đỏ + đổ thông tin nhạy cảm ra console. Đã sửa:

- **Firebase trên web**: bọc init bằng `if (!kIsWeb)` trong `main.dart` — web chưa cấu hình `DefaultFirebaseOptions` nên trước đó ném lỗi đỏ "DefaultFirebaseOptions have not been configured for web". FCM chỉ chạy trên Android/iOS.
- **Dio logger**: `PrettyDioLogger` tắt `requestHeader/requestBody/responseBody` (`dio_client.dart`), chỉ còn dòng request + lỗi. Trước đó in cả `accessToken`/`refreshToken` + toàn bộ body ra console (rủi ro lộ token + nhiễu).
- **RenderFlex overflow màn Đăng nhập**: Row "Đăng nhập nhanh (DEV)" tràn ~68px khi viewport hẹp (vd màn chia đôi với DevTools); bọc Text giữa vào `Flexible` + `ellipsis` (`login_screen.dart`). Không xảy ra ở bề rộng mặc định.
- **Logo launcher**: icon trong project ĐÃ là logo navy đúng (`mipmap-*/ic_launcher.png`); nếu máy còn hiện icon Flutter mặc định là do APK build cũ → cần `flutter build apk` + cài lại (gỡ app cũ nếu launcher cache icon).

### Tạm ẩn tính năng backend chưa có (2026-06-19)

Để hết "lỗi báo đỏ" trên Network/log và không hiển thị màn rỗng/sai cho người dùng, các tính năng mobile gọi endpoint **chưa tồn tại ở backend** đã được **tạm ẩn ở mức UI** bằng cờ trong `lib/core/config/feature_flags.dart` (tất cả mặc định `false`; chỉ cần đổi `true` để bật lại khi backend sẵn sàng — **không xóa route/page nào**):

| Cờ | Tính năng ẩn | Endpoint thiếu | Nơi ẩn |
|---|---|---|---|
| `walletEnabled` | Ví/số dư | `GET /wallet/balance` | Card "Số dư ví" ở home header + section ví ở courier home; `WalletProvider.getWalletBalance()` short-circuit (không gọi mạng) |
| `transactionsEnabled` | Nạp tiền + lịch sử giao dịch | `GET /payments/transactions` | Menu "Giao dịch" ở Profile (route `/transactions`, `/top-up` còn nhưng không có entry-point) |
| `subscriptionEnabled` | Gói dịch vụ/subscription | `/plans/customer`, `/pricings`, `/subscriptions/*` | Menu "Gói dịch vụ" ở Profile |
| `vouchersEnabled` | Kho voucher cá nhân | `GET /promotions/vouchers/my` | Menu "Ưu đãi & Quà tặng" ở Profile |
| `homeContentFeedEnabled` | Quảng cáo + blog trang chủ | `/advertisements`, `/blogs` | `HomeRepository` short-circuit (trả rỗng, không gọi mạng) |
| `faceRecognitionEnabled` | Nhận diện khuôn mặt (đăng ký/đăng nhập) | `/api/auth/ai/registered/{id}`, `/api/auth/ai/register`, `/api/auth/ai/verify` (trả **500**) | `ProfileProvider._getFaceRegistrationStatus` short-circuit (không gọi mạng) + ẩn menu "Đăng ký khuôn mặt" ở Profile (2026-06-19) |

Lưu ý quan trọng:
- Trang **"Ưu đãi"** (PromotionsPage, `GET /api/promotions/active`) **vẫn hoạt động** và KHÔNG bị ẩn — chip "Ưu đãi" + section Flash Sale ở home dùng endpoint thật này.
- Flow **SEND/RENTAL thật** (`locker_ops`) không phụ thuộc ví nên không bị ảnh hưởng. Các màn thanh toán legacy phụ thuộc ví (`confirm_rent_page`/`locker_action_page`) là code chết, không reachable từ UI.
- Khi backend bổ sung wallet service (xem mục 26), bật `walletEnabled`/`transactionsEnabled` lại; tương tự cho subscription/voucher/ads-blog khi có service tương ứng.

### Logo app (2026-06-19)

Đổi logo app sang logo tủ khóa nền navy người dùng cung cấp: `assets/images/logo.png` (dùng cho splash/onboarding/appbar) + regen launcher icon Android (`mipmap-*/ic_launcher.png`, mọi mật độ) và iOS (`AppIcon.appiconset/*`) qua `flutter_launcher_icons` (đổi `pubspec.yaml` `flutter_launcher_icons.android` → `true` để khớp manifest `@mipmap/ic_launcher`). Không đổi API/flow nghiệp vụ.

### Realtime auto-refresh qua AppEventBus (2026-06-22)

`lib/core/services/app_event_bus.dart` — singleton `StreamController<AppEvent>.broadcast()`:

- Event types: `OrderChangedEvent(orderId?)`, `PaymentCompletedEvent(orderId?)`, `PaymentFailedEvent(orderId?)`, `WalletUpdatedEvent`, `ReportUpdatedEvent(reportId?)`.

`NotificationProvider` parse field `actionType` từ cả 2 nguồn thông báo, emit lên `AppEventBus`:

| actionType | Event emit |
|---|---|
| `ORDER_STATUS_CHANGED` | `OrderChangedEvent(orderId: referenceId)` |
| `PAYMENT_COMPLETED` | `PaymentCompletedEvent(orderId)` + `WalletUpdatedEvent` |
| `PAYMENT_FAILED` | `PaymentFailedEvent(orderId)` |
| `LOCKER_REPORT_CLAIMED` / `LOCKER_REPORT_RESOLVED` | `ReportUpdatedEvent(reportId)` |

Nguồn parse:
- **STOMP WebSocket**: `notification.dataPayload?.actionType` + `notification.dataPayload?.referenceId`
- **FCM push**: `message.data['type']` + `message.data['referenceId']`

Consumers (subscribe trong `initState`/constructor, hủy trong `dispose` bằng `StreamSubscription.cancel()`):

- `OrderPage`: reload `OrderProvider.refresh()` khi nhận `OrderChangedEvent` hoặc `PaymentCompletedEvent`.
- `CustomerOrderDetailPage`: gọi `fetchOrderDetail(currentId)` khi nhận `OrderChangedEvent` khớp `orderId` (hoặc `orderId == null` — broadcast toàn bộ).
- `WalletProvider` (constructor): gọi `getWalletBalance()` khi nhận `WalletUpdatedEvent` hoặc `PaymentCompletedEvent`.

Kết quả: App tự refresh màn Đơn hàng, Chi tiết đơn, Số dư ví khi backend thay đổi trạng thái qua STOMP hoặc FCM push — không cần pull-to-refresh thủ công.

### Trang chi tiết Voucher và Promotion (2026-06-22)

**VoucherDetailPage** (`lib/features/vouchers/presentation/pages/voucher_detail_page.dart`, MỚI):

- Dùng cho `VoucherModel` (từ `/promotions/vouchers/my` khi có backend, hiện wired từ `MyVouchersPage`).
- Ticket card gradient (màu khi `UNUSED`, xám khi `USED`/`EXPIRED`) với notch divider hai bên (`_NotchPainter`).
- Code chip bấm-để-copy (`Clipboard.setData` + `SnackBar` xác nhận).
- Bảng thông tin: loại giảm, giảm tối đa, đơn tối thiểu, hiệu lực từ, hạn dùng, trạng thái.
- Back navigation: `context.pop()`.
- Wired từ `MyVouchersPage`: mỗi card bọc trong `GestureDetector` → `Navigator.of(context, rootNavigator: true).push(VoucherDetailPage(voucher: voucher))`.

**PromotionDetailPage** (`lib/features/promotions/presentation/pages/promotion_detail_page.dart`, MỚI):

- Dùng cho `PromotionModel` (từ `GET /api/promotions/active`).
- Banner ảnh 180px (`CachedNetworkImage` + gradient overlay tối từ dưới lên; fallback gradient khi không có ảnh).
- Chip "Flash Sale ⚡" + badge giảm giá + tên promotion trên ảnh.
- Ticket banner gradient (6 màu, chọn theo `promo.id % 6`): text phần thưởng + hạn dùng.
- Code chip bấm-để-copy.
- Bảng thông tin: mức giảm, tối đa, đơn tối thiểu, bắt đầu, kết thúc, trạng thái (dùng getter `isExpired` mới thêm vào `PromotionModel`: `endAt?.isBefore(DateTime.now())`).
- 3 steps "Cách sử dụng": copy mã → nhập khi thanh toán → giảm được áp dụng.
- Back navigation: `Navigator.of(context).pop()`.

Wiring điều hướng:

| Nguồn | Cách push | Lý do |
|---|---|---|
| `PromotionsPage` (`/promotions`, ngoài ShellRoute) | `Navigator.of(ctx).push(MaterialPageRoute)` | Đã ở root navigator — push thường là đủ |
| `_FlashSaleCard` trên home (trong ShellRoute `/home`) | `Navigator.of(context, rootNavigator: true).push(MaterialPageRoute)` | Phải dùng rootNavigator để phủ qua bottom nav bar |

Lỗi trước khi fix: `_FlashSaleCard.onTap` gọi `context.push(AppRouter.promotions)` (mở trang danh sách, không phải chi tiết); `_buildPromoCard` trong `PromotionsPage` không có `onTap` — tap vào card không làm gì.

### 10 mock ô tủ per locker (2026-06-22)

`_LockerCardState._enrichLayout(Map<String, dynamic> raw)` — static method trong `store_lockers_page.dart`:

- Chạy sau khi `GET /api/lockers/{id}/layout` thành công (nếu trả <10 ô) hoặc thất bại (hiển thị demo grid thay vì màn trắng).
- Grid 2×5 (2 hàng × 5 cột, tổng 10 ô):
  - **Row 0** (col 0–4): 5 STANDARD cells, size `SMALL/MEDIUM/LARGE/MEDIUM/SMALL`, status `AVAILABLE/OCCUPIED/AVAILABLE/RESERVED/AVAILABLE`.
  - **Row 1** col 0–1: 2 STANDARD (`AVAILABLE/OCCUPIED`); col 2–4: 3 DRONE (`AVAILABLE/LARGE`).
- Giữ ô thật (từ API) ở đúng vị trí theo `rowIndex`/`colIndex`, chỉ lấp các vị trí còn trống bằng mock.
- Kết quả: Lưới ô tủ luôn hiện đủ 10 ô demo, kể cả khi seed backend CAB-DEMO-01 chỉ trả về 1 ô.

## 22. Luồng Web Frontend

Flow giá trị cao hiện tại của React app:

- Admin dashboard.
- Admin users.
- Admin stores.
- Admin services.
- Admin lockers.
- Admin locker layout.
- Admin maintenance: tổng quan ô hỏng/phiếu mới/đang xử lý/đã xong, claim/resolve/clear fault, hiển thị địa chỉ/toạ độ locker và mở chỉ đường bằng Google Maps.
- Admin orders.
- Admin payments.
- Admin loyalty.
- Admin partners.
- Admin feedback.
- Admin scheduler.
- Admin notifications.
- Admin promotions.

Partner portal routes tồn tại:

- `/partner/dashboard`
- `/partner/orders`
- `/partner/staff`
- `/partner/stores`
- `/partner/notifications`
- `/partner/settings`

Lưu ý hiện tại:

- Role `PARTNER` và `partner-service` đã được gỡ khỏi backend (2026-06-13). Partner portal trên React web là deprecated/legacy; không còn account/role partner để đăng nhập.

## 23. Seed Và Tài Khoản Test

Seed account đã quan sát trong deploy DB:

| Email | Role | User ID | Phone |
|---|---|---:|---|
| `customer.seed@laundry.test` | `USER` | `1001` | `0901001001` |
| `staff.seed@laundry.test` | `STAFF` | `1002` | `0901001002` |
| `admin.seed@laundry.test` | `ADMIN` | `1004` | `0901001004` |
| `customer.vip@laundry.test` | `USER` | `1005` | `0901001005` |

### Bộ seed demo đầy đủ (2026-06-15)

`scripts/seed-full-demo-ms.sql` (idempotent, dải id riêng ≥9001/≥90001 + marker `*-DEMO-*`) tạo dữ liệu test A→Z cho toàn bộ MS DB. **4 tài khoản đặt sẵn — tất cả mật khẩu `12345678`:**

| Email | Role | user_id |
|---|---|---:|
| `baohuy2k12k4@gmail.com` | `ADMIN` | 9001 |
| `nqbhuy2004nt@gmail.com` | `CUSTOMER` | 9002 |
| `se180211nguyenquocbaohuy@gmail.com` | `MAINTENANCE` | 9003 |
| `huynqbse180211@fpt.edu.vn` | `MANAGER` | 9004 |

Kèm 100 khách bulk (`*@demo.laundry.test`) và ~100+ bản ghi mỗi bảng nghiệp vụ (stores/lockers/boxes/orders/payments/notifications/loyalty/maintenance...). Đa số dữ liệu order/payment/notification/loyalty gắn với CUSTOMER 9002 để test luồng người dùng; report bảo trì gán cho MAINTENANCE 9003. Áp bằng: `psql -U postgres -f scripts/seed-full-demo-ms.sql` (chạy như superuser, file tự `\connect` từng DB).

Quan trọng:

- `seed-demo-data.sql` chỉ lưu password hash. Plaintext password không được document trong seed file.
- Account `partner.seed` (user `1003`, role `PARTNER`) và toàn bộ seed `partner_db` đã bị gỡ khỏi `seed-demo-data.sql` (2026-06-13). Deploy DB cũ có thể vẫn còn record này cho tới khi re-seed.
- Nếu cần demo account có mật khẩu biết trước, hãy reset password hash có chủ đích và ghi vào `docs/PROJECT_PROGRESS_TRACKER.md` ở phần verification notes.
- Dev accounts từng dùng trong local smoke test gồm `demo@laundry.test`, `admin@laundry.test`, `manager@laundry.test`, và `maintenance@laundry.test`, nhưng có thể không tồn tại trong deploy DB mới.

## 24. Các Luồng Nghiệp Vụ Chưa Hoàn Tất

Những phần sau chưa hoàn tất trong sản phẩm đang chạy:

- Source `laundry-service` thật và ownership catalog.
- Tablet-web cabinet UI cho người dùng đứng trước locker.
- Tự động occupy/release từ door/weight sensors.
- Drone delivery service đầy đủ (điều khiển bay/MQTT thật cho drone).
- Drone fleet management cơ bản (trạng thái/pin/kỹ thuật viên/nhật ký) **đã có từ 2026-06-22** — xem mục 11.1; pin/trạng thái bay vẫn nhập tay, chưa có telemetry thật.
- Phân công drone theo pin/battery-aware assignment cho một chuyến giao hàng cụ thể (khác với chỉ xem trạng thái/pin của fleet).
- Drone simulator và realtime map tracking.
- AI/RAG knowledge support.
- Đối soát/thanh quyết toán provider payment cấp production.
- Cài đặt Firebase/FCM credential cấp production.
- Full parity các feature mobile legacy với backend hiện tại.

## 25. Phân Tích Luồng Tủ & Bổ Sung Chuẩn Thực Tế (tham chiếu spec)

Tài liệu đặc tả đầy đủ (phân tích as-is bám code + bổ sung toàn bộ luồng nghiệp vụ tủ khóa chuẩn thực tế + gap map + backlog): **`docs/project-artifacts/guides/LOCKER_FLOWS_STANDARD_SPEC.md`** (2026-06-14).

Các điểm as-is đã xác minh trực tiếp từ code, cần lưu ý vì là lỗ hổng đúng đắn của luồng tủ đang chạy:

- **Trạng thái ô (cập nhật L5)**: `AVAILABLE / RESERVED / OCCUPIED / FAULT / OUT_OF_SERVICE / CLEANING` (xem `LockerService`). `OUT_OF_SERVICE`/`CLEANING` được thêm ở L5 (bảo trì vận hành) — set qua `/api/maintenance/boxes/{id}/{out-of-service|cleaning}`, khôi phục qua `return-to-service`; ô ở 2 trạng thái này bị loại khỏi reserve. `EXPIRED` vẫn chỉ tồn tại ở cấp order qua `pickupDeadline` (chưa có ở cấp ô).
- **Ô `RESERVED` không có TTL** và `autoCancelUnconfirmedOrders()` đổi đơn `INITIALIZED`>24h sang `CANCELED` nhưng **không release ô** và **không được `@Scheduled`** → ô có thể kẹt `RESERVED` (Gap G1/G2). `cancel()` thủ công thì có release.
- **Quá hạn lấy hàng chỉ nhắc + cộng phí**; ô vẫn `OCCUPIED` tới khi có lệnh complete/checkout — chưa có move-to-storage/giải phóng tự động (Gap G3).
- **Luồng nhận hàng qua shipper/courier (PARCEL_RECEIVE) chưa có code** — SEND hiện chỉ là C2C giữa 2 app-user; chưa có "courier access code" tách với PIN khách (Gap G6).
- **Deadline SEND mặc định 48h**, laundry pickup 24h; rental deadline = số giờ thuê (cấu hình `app.order.*`).
- **PIN/QR**: QR ký số HMAC gắn PIN hiện tại → đổi PIN (delegate/reset/SEND handover) vô hiệu QR cũ; `getByAccess` nhận cả PIN và QR.
- **Trạng thái ô là bản sao best-effort** của order (occupy/release nuốt lỗi) → có rủi ro lệch trạng thái, chưa có job đối soát (Gap G4).

Toàn bộ danh sách 16 gap (G1–G16), đề xuất data model/API, và lộ trình implement 7 giai đoạn (L1–L7, mỗi giai đoạn 1 branch) nằm trong spec ở trên.

**Tiến độ lộ trình (cập nhật 2026-06-14):**

- **L1 (phần order-layer) — ĐÃ LÀM** trên branch `fix/locker-reservation-ttl-and-release`: `autoCancelUnconfirmedOrders` giờ **release ô** khi hủy và chạy transition đầy đủ (history + event + notify); thêm job `@Scheduled` (`OrderScheduler.sweepUnconfirmedReservations`, cron mặc định mỗi 15 phút). Đóng **G1** (auto-cancel chưa @Scheduled) và **G2** (ô kẹt `RESERVED` của đơn bỏ dở) ở mức hành vi. Cửa sổ giữ chỗ cấu hình `app.order.auto-cancel-hours` (mặc định 24h). Verify: `mvn -pl order-service -am test` BUILD SUCCESS. *(Phần cell-level `reserved_until` TTL trong locker-service vẫn là follow-up.)*
- **Mobile UI luồng tủ — ĐÃ LÀM** trên branch `feat/locker-customer-ui-revamp` (xem mục 21).
- L2–L7: chưa làm.

## 26. Luồng VNPay Wallet Topup — NẠP TIỀN VÍ (2026-06-18, cập nhật 2026-06-21)

> **Trạng thái**: Tạo VNPay URL ✅ | Callback xử lý ✅ | **Wallet/balance update ✅ (2026-06-21)** | Test thực tế (sandbox) ⏳
>
> **(2026-06-21) Ví đã hoàn thiện**: bảng `payment_schema.wallets` + `wallet_transactions` (migration V3). `PaymentService.handleVnPayReturn` cộng ví khi topup COMPLETED (idempotent theo `txnRef`). Thanh toán đơn đa hình thức qua `POST /api/payments/checkout` (WALLET trừ ví tức thì / VNPAY / MOMO / CASH). order-service nghe `PAYMENT_COMPLETED` → đơn `payment_status=PAID`. MoMo thật (`MomoService`) gated theo env. Admin điều chỉnh ví `POST /api/admin/wallet/{userId}/adjust`. Mobile bật lại cờ ví + đọc `GET /api/wallet`.

### Endpoint đã implement

```
POST /api/payments/topup/create
  Auth: Bearer JWT required (X-User-Id inject tự động bởi JwtGatewayFilter từ claims.getSubject())
  Body: { amount: decimal (min 1000), returnUrl?: string, bankCode?: string, locale?: string }
  Response: { paymentUrl: string, txnRef: string }

GET /payments/vnpay/callback  (PUBLIC — không cần JWT)
  VNPay redirect browser/WebView về đây sau thanh toán
  Mobile WebView detect URL chứa /payments/vnpay/callback → đóng WebView, báo thành công
  Cùng logic handler với GET /api/payments/vnpay/return
```

### Flow nghiệp vụ hiện tại (đã có code)

```
1. User chọn số tiền (20k–1M VND) trên màn hình NẠP TIỀN (top_up_page.dart)
2. Mobile POST /api/payments/topup/create → nhận { paymentUrl, txnRef } từ VNPay sandbox
3. App mở WebView với paymentUrl (trang thanh toán VNPay)
4. User hoàn tất thanh toán trên VNPay (nhập OTP/chọn ngân hàng)
5. VNPay redirect WebView đến /payments/vnpay/callback?vnp_*=...
6. Spring Boot payment-service xử lý callback:
   - Verify VNPay HMAC signature
   - findByReferenceId(txnRef) → cập nhật PaymentRecord status = COMPLETED
7. Mobile WebView detect url.contains('/payments/vnpay/callback') → đóng WebView
8. App hiện thông báo thành công
9. [THIẾU] App refresh wallet balance (GET /api/wallet/balance — CHƯA IMPLEMENT)
```

### Thiết kế kỹ thuật quan trọng

- `orderId = 0L` là sentinel value cho topup (DB constraint `order_id NOT NULL` được giữ, dùng 0 thay null)
- `method = "VNPAY_TOPUP"` để phân biệt với payment thông thường của order
- `txnRef = "TOPUP_{userId}_{System.currentTimeMillis()}"` — pattern từ reference backend aisl_backend
- `returnUrl` từ mobile = `{apiBaseUrl}/payments/vnpay/callback` → phải route qua gateway về payment-service
- Flyway V2 thêm index `idx_payments_reference_id` trên `payment_schema.payments(reference_id)` để callback lookup nhanh (thay `findAll().stream()` trước đó)

### Database

- Table: `payment_schema.payments`
- `order_id = 0` (sentinel, không có order thật)
- `method = "VNPAY_TOPUP"`
- `reference_id = txnRef = "TOPUP_{userId}_{timestamp}"`
- Flyway: `V2__allow_topup_payments.sql` (chỉ thêm index, không đổi schema)

### Routing (api-gateway)

- `/api/payments/**` → payment-service (route cũ)
- `/payments/vnpay/callback` → payment-service (**thêm 2026-06-18**, PUBLIC)
- `/payments/vnpay/callback` đã thêm vào `isPublic()` của `JwtGatewayFilter` (không yêu cầu JWT)

### Files đã thay đổi

**Backend — laundry-locker-microservices (Spring Boot, backend chính):**

| File | Loại | Thay đổi |
|---|---|---|
| `payment-service/.../dto/CreateTopupRequest.java` | MỚI | record: `amount @NotNull @Min(1000)`, `returnUrl`, `bankCode`, `locale` |
| `payment-service/.../dto/TopupResponse.java` | MỚI | record: `paymentUrl`, `txnRef` |
| `payment-service/.../repository/PaymentRepository.java` | SỬA | thêm `Optional<PaymentRecord> findByReferenceId(String referenceId)` |
| `payment-service/.../service/PaymentService.java` | SỬA | thêm `createTopupUrl(Long userId, CreateTopupRequest)`, fix `handleVnPayReturn` dùng index |
| `payment-service/.../controller/PaymentController.java` | SỬA | thêm `POST /api/payments/topup/create` và `GET /payments/vnpay/callback` |
| `payment-service/.../db/migration/V2__allow_topup_payments.sql` | MỚI | `CREATE INDEX IF NOT EXISTS idx_payments_reference_id` |
| `api-gateway/.../resources/application.yml` | SỬA | thêm `/payments/vnpay/callback` vào route predicates của payment-service |
| `api-gateway/.../JwtGatewayFilter.java` | SỬA | thêm `path.startsWith("/payments/vnpay/callback")` vào `isPublic()` |

**Mobile — smart-laundry-locker-mobile (Flutter):**

| File | Loại | Thay đổi |
|---|---|---|
| `lib/features/transactions/infrastructure/data_sources/top_up_remote_data_source_impl.dart` | SỬA | `_path`: `/payments/topup/create` → `/api/payments/topup/create` |
| `lib/features/transactions/infrastructure/repositories/top_up_repository_impl.dart` | SỬA | Thêm catch cho `NotFoundException`, `ServerException`, `NetworkException`, `ValidationException`, `AuthenticationException` (thay `UnknownFailure(e.toString())` generic) |

### TODO bắt buộc cho developer tiếp theo

**[P0 — CHẶN TÍNH NĂNG]** Sau khi VNPay callback COMPLETED, balance user không thay đổi. Cần implement wallet/balance service:

1. **Chọn kiến trúc wallet**: thêm cột `balance DECIMAL` vào `user_profiles` (user-service) HOẶC tạo `wallet-service` riêng với bảng `wallets(user_id, balance)`. Khuyến nghị: thêm vào user-service để đơn giản.

2. **Publish RabbitMQ event sau topup COMPLETED**: trong `PaymentService.handleVnPayReturn()`, khi `vnp_ResponseCode = "00"` và `method = "VNPAY_TOPUP"`, publish event:
   ```json
   { "eventName": "wallet.topup.completed", "userId": <long>, "amount": <decimal>, "txnRef": "TOPUP_..." }
   ```
   lên exchange `laundry.events`.

3. **Consumer trong user-service (hoặc wallet-service)**: subscribe routing key `wallet.topup.completed`, cộng `amount` vào `balance` của user tương ứng. **Thêm idempotency**: check `payment.reference_id` đã được process chưa trước khi cộng (tránh double-credit nếu callback retry).

4. **Implement `GET /api/wallet/balance`**: endpoint trả `{ balance: decimal, currency: "VND" }` cho mobile đọc số dư sau topup.

5. **Mobile**: sau khi WebView đóng (topup success), gọi `GET /api/wallet/balance` và cập nhật UI wallet card trên home.

**[P1 — TEST]** Test với VNPay sandbox thật:
- Set `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET` trong payment-service config
- Local test với device thật: cần `ngrok http 8080` → dùng ngrok URL làm `API_BASE_URL` trong mobile `.env` (KHÔNG commit)
- VNPay sandbox merchant cần whitelist URL domain

**[P2 — OPTIONAL]** Thêm màn lịch sử giao dịch ví (gọi `GET /api/payments?method=VNPAY_TOPUP` hoặc endpoint riêng)

---

## 27. Home Page Redesign — Flash Sale & Promotions (2026-06-18)

### Thay đổi UI mobile (customer home)

| Khu vực | Trước | Sau |
|---|---|---|
| Dưới wallet card | `_buildWelcomeCard` → `_buildStoreSection` (legacy) | `_buildPopularLockersSection` (horizontal scroll locker cards) |
| Cuối trang | `_buildTodayServicesSection` (3 service plan cards cứng) | `_buildFlashSaleSection` (horizontal scroll promotion cards từ API thật) |
| Chip "Ưu đãi" | Navigate → `/my-vouchers` (gọi `/promotions/vouchers/my` — endpoint CHẾT) | Navigate → `/promotions` (gọi `GET /api/promotions/active` — endpoint THẬT) |

### Promotions API (đã tồn tại trong order-service)

| Endpoint | Auth | Mô tả |
|---|---|---|
| `GET /api/promotions/active` | Public (không cần JWT) | Trả `List<Promotion>` đang hiệu lực (`status=ACTIVE` + trong khoảng `startAt`–`endAt`) |
| `GET /api/promotions/validate/{code}` | Public | Validate và tính discount cho 1 mã |
| `POST /api/admin/promotions` | ADMIN | Tạo promotion mới |
| `GET /api/admin/promotions` | ADMIN | Lấy tất cả promotions (admin) |

**Route gateway**: `/api/promotions/**` → `order-service` (đã có sẵn).

**Promotion entity fields** (sau migration V4 — 2026-06-18):
```
id, code, name, description (NEW), imageUrl (NEW),
discountType (FIXED_AMOUNT|PERCENTAGE), discountValue,
maxDiscountAmount, minOrderAmount, stackable,
status (ACTIVE|INACTIVE), startAt, endAt, usageCount
```

### Mobile: Promotions feature (lib/features/promotions/)

```
data/
  models/promotion_model.dart        — PromotionModel, discountLabel, expiryLabel, shortDiscountLabel
  repositories/promotion_repository.dart — GET /api/promotions/active
presentation/
  providers/promotion_provider.dart  — ChangeNotifierProvider (Riverpod legacy), promotionNotifierProvider
  pages/promotions_page.dart         — Trang full list ưu đãi, có copy-code, countdown, image/gradient
```

**Provider init**: `home_page.dart` initState + `_onRefresh` đều gọi `ref.read(promotionNotifierProvider).load()`.

### Flash Sale cards (home)

- Width 260×175px, horizontal scroll
- Background: `imageUrl` nếu admin set, fallback gradient từ `_gradients[]`
- Badge cam "FLASH SALE ⚡" (top-left) + badge đỏ discount (top-right, e.g. "-30%" hoặc "-50Kđ")
- Bottom: tên promotion + countdown expiry + code chip (tap = copy to clipboard + SnackBar)
- Empty state: `SizedBox.shrink()` — ẩn hoàn toàn section nếu không có promotions

### Lỗi "Ưu đãi" chip cũ (đã fix)

`VoucherRepository.getMyVouchers()` gọi `/promotions/vouchers/my` — endpoint này **không tồn tại** trong backend. Đã fix bằng cách đổi chip → `/promotions` dùng `GET /api/promotions/active` thật.

Route `/my-vouchers` + `MyVouchersPage` vẫn còn trong code nhưng không còn được navigate từ home chip — giữ lại cho tương lai nếu cần implement user-specific voucher assignment.

### Backend migration V4 (order-service)

File: `order-service/src/main/resources/db/migration/V4__promotions_image_description.sql`
```sql
ALTER TABLE order_schema.promotions ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
ALTER TABLE order_schema.promotions ADD COLUMN IF NOT EXISTS description VARCHAR(1000);
```

Chạy tự động khi deploy (Flyway). Idempotent nhờ `IF NOT EXISTS`.

### Cập nhật 2026-06-22: Tap vào Flash Sale card và PromotionsPage dẫn thẳng vào PromotionDetailPage

**Trước**: `_FlashSaleCard.onTap` (home, ShellRoute) gọi `context.push(AppRouter.promotions)` → mở trang danh sách, không phải trang chi tiết. `_buildPromoCard` trong `PromotionsPage` không có `onTap` — tap card không làm gì.

**Sau**:

- `_FlashSaleCard.onTap`: `Navigator.of(context, rootNavigator: true).push(MaterialPageRoute(builder: (_) => PromotionDetailPage(promo: promo)))` — `rootNavigator: true` vì `_FlashSaleCard` nằm trong ShellRoute (tab `/home`), phải phủ qua bottom nav bar.
- `PromotionsPage` item: bọc `_buildPromoCard` trong `GestureDetector(behavior: HitTestBehavior.opaque, onTap: () => Navigator.of(ctx).push(...))` — `PromotionsPage` nằm ở route `/promotions` ngoài ShellRoute nên push thường là đủ.
- `PromotionModel` thêm getter `isExpired`: `bool get isExpired { if (endAt == null) return false; return endAt!.isBefore(DateTime.now()); }` — dùng trong `PromotionDetailPage` để hiển thị trạng thái "Hết hạn".
- `PromotionDetailPage` (MỚI) — xem mục 21 subsection "Trang chi tiết Voucher và Promotion".

**MyVouchersPage → VoucherDetailPage**: Mỗi voucher card trong `MyVouchersPage` bọc trong `GestureDetector` → `Navigator.of(context, rootNavigator: true).push(VoucherDetailPage(voucher: voucher))`. `VoucherDetailPage` (MỚI) — xem mục 21 subsection "Trang chi tiết Voucher và Promotion".

Route `/my-vouchers` + `MyVouchersPage` vẫn gated bởi cờ `vouchersEnabled = false` (backend chưa có endpoint) — khi bật lại thì VoucherDetailPage đã sẵn sàng.
