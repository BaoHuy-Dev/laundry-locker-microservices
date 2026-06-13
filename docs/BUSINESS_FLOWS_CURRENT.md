# Luồng Nghiệp Vụ Hiện Tại

> Cập nhật lần cuối: 2026-06-14
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
API Gateway: http://localhost:8080
```

Phạm vi hiện tại cần ghi nhớ:

- Source `laundry-service` đang thiếu trong repo backend hiện tại; vẫn giữ tên trong compose/database naming nhưng bị skip qua `docker-compose.override.yml` khi chạy local.
- Role `PARTNER` và `partner-service` đã được gỡ khỏi backend (seed/role/permission/compose) vì không còn dùng; schema `partner_db`/`partner_schema` cũ được giữ lại chưa dọn.
- Sản phẩm đang chạy chính là nền tảng locker/laundry/SEND/RENTAL. Drone delivery đầy đủ, engine phân công drone, realtime tracking và AI/RAG vẫn là việc tương lai.

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

Gateway yêu cầu `MAINTENANCE` hoặc `ADMIN` cho `/api/maintenance/**`.

Endpoint backend hiện có:

- `GET /api/maintenance/faults`
- `GET /api/maintenance/reports?mine=true|false`
- `PUT /api/maintenance/reports/{id}/claim`
- `PUT /api/maintenance/reports/{id}/resolve`
- `POST /api/maintenance/boxes/{id}/clear-fault`

### Staff

Dùng cho các flow vận hành cũ/legacy:

- Gán staff cho đơn.
- Liệt kê đơn của staff theo trạng thái.
- Mở ô tủ thông qua staff facade.

Endpoint backend hiện có:

- `POST /api/staff/assignments`
- `POST /api/staff/orders/{orderId}/assign`
- `GET /api/staff/orders/my-assigned`
- `GET /api/staff/orders`
- `GET /api/staff/orders/waiting`
- `GET /api/staff/orders/processing`
- `GET /api/staff/orders/ready`
- `GET /api/staff/lockers`
- `POST /api/staff/unlock-box`

### Partner (đã gỡ)

Role `PARTNER` đã được gỡ khỏi backend (2026-06-13) vì không còn dùng: bỏ role `PARTNER`, permission `PARTNER_MANAGE`, account demo `partner.seed`, và service `partner-service` trong `docker-compose.yml`/`docker-compose.override.yml`. `partner-service` vốn đã thiếu source từ trước. Vẫn giữ schema cũ chưa dùng (`partner_db`/`partner_schema`, cột `partner_id` ở store-service) để không đụng migration đã chạy; có thể dọn ở bước sau nếu cần. Route Partner ở React web/legacy mobile xem như deprecated.

## 3. Luồng Xác Thực Và Hồ Sơ Người Dùng

### Database

Auth và profile được tách riêng:

- `auth_db.auth_schema.auth_accounts`: tài khoản đăng nhập, password hash, provider, verification flags, status.
- `auth_db.auth_schema.refresh_tokens`: refresh token.
- `auth_db.auth_schema.email_otps`: OTP hash.
- `user_db.user_schema.user_profiles`: hồ sơ người dùng, phone/email, status, role.
- `user_db.user_schema.roles`, `permissions`, `role_permissions`: dữ liệu role/permission.

Không lưu mật khẩu plain text. Chỉ lưu bcrypt hash.

### Đăng Ký

Client gọi:

```http
POST /api/auth/register
```

Body gồm email/phone/password và role tuỳ chọn.

Hành vi backend:

1. `auth-service` tạo auth account và hash password.
2. `auth-service` gọi internal provisioning endpoint của `user-service`.
3. `user-service` tạo profile trong `user_profiles`.
4. `auth-service` cấp access token và refresh token.

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

### Admin Auth

Endpoint riêng cho admin:

- `POST /api/admin/auth/login`
- `POST /api/admin/auth/verify-2fa`
- `POST /api/admin/auth/refresh`

Flow admin login phụ thuộc credential/OTP. Trong dev/test, `/api/auth/login` cũng đã được dùng để lấy token admin khi account có role `ADMIN`.

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
- Payment: `/api/payments/**`, `/api/admin/payments/**`
- Notification: `/api/notifications/**`, `/api/admin/notifications/**`, `/ws`, `/ws/**`
- IoT: `/api/iot/**`
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
  - Deploy workflow build bằng `mvn -B clean verify`, không skip test khi đóng gói deploy.
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

Loại cell hiện có:

- `DRONE`: ô hàng trên cho drone deposit, chỉ reserve khi `channel=DRONE`.
- `STANDARD`: ô bình thường cho customer/staff/SEND/LAUNDRY.
- `XL`: ô lớn hơn cho storage/rental.

Vòng đời cell hiện tại:

```text
AVAILABLE -> RESERVED -> OCCUPIED -> AVAILABLE
AVAILABLE/RESERVED/OCCUPIED -> FAULT -> AVAILABLE
```

Hành vi quan trọng:

- `RESERVED`: flow đã giữ ô tủ.
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

Flow nghiệp vụ:

1. User/customer/staff báo ô tủ bị lỗi.
2. Backend mark cell thành `FAULT`.
3. Backend tạo/cập nhật locker report với box id và lý do.
4. Cell bị lỗi bị loại khỏi luồng reserve bình thường.
5. Maintenance user xem các report đang mở.
6. Maintenance claim report: `OPEN -> IN_PROGRESS`.
7. Maintenance resolve report.
8. Backend clear fault và đưa cell về `AVAILABLE`.

Lưu ý hiện tại:

- Lịch bảo trì định kỳ, work log của technician, và bảo trì theo tần suất sử dụng vẫn là việc tương lai.

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
- `/admin/lockers/:lockerId`
- `/admin/maintenance`

Flow nghiệp vụ:

1. Admin đăng nhập.
2. Admin xem dashboard tổng quan.
3. Admin quản lý master data: users, stores, services, lockers.
4. Admin mở trang layout locker để xem grid cell.
5. Admin xử lý bảo trì/fault reports.
6. Admin xem order và payment.
7. Admin trigger scheduler job thủ công khi cần.

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

Flow nghiệp vụ:

1. Order/payment/locker/iot event xảy ra.
2. Service publish RabbitMQ event hoặc gọi internal notification endpoint.
3. Notification service lưu notification.
4. Có thể push FCM nếu có token/config.
5. Có thể broadcast qua WebSocket.
6. User/admin đọc hoặc mark read notification.

Lưu ý hiện tại:

- Firebase production credentials phụ thuộc environment.

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
- `POST /api/iot/unlock`
- `POST /api/iot/verify-pin`
- `POST /api/iot/verify-access`
- `POST /api/iot/pickup`
- `POST /api/iot/box-status`
- `POST /internal/iot/device-status`

Flow nghiệp vụ:

1. Cabinet/tablet/RPi báo status.
2. User nhập PIN hoặc scan QR.
3. Device gọi verify endpoint.
4. IoT service verify với order service và locker service.
5. IoT service chấp nhận/publish lệnh mở qua MQTT facade.
6. Device báo box status/open result.
7. Backend cập nhật order/locker state nếu flow đã wire.

Lưu ý hiện tại:

- Occupy từ sensor thật là Phase 3.
- Python `smart-locker-iot` cần config MQTT broker khớp với backend và chế độ hardware/simulation.
- Drone vẫn là future channel riêng, chỉ dùng cell `DRONE` khi `channel=DRONE`.

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

Hành vi event:

- Order service emit event vòng đời order.
- Payment service emit event payment.
- Locker service emit event box/fault.
- IoT service emit event device status.
- Notification service listen và tạo notification.

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
- Màn Gửi hàng: stepper 2 giai đoạn (bỏ hàng → người nhận lấy) đúng luồng PIN 2 giai đoạn; banner hướng dẫn; hiển thị phí gửi và hạn nhận có định dạng.
- Màn Thuê tủ: card chọn loại ô (STANDARD/XL kèm kích thước+đơn giá), chip giờ nhanh + slider, thẻ giá tính live.
- Màn Đơn tủ của tôi: card đơn có countdown/cảnh báo quá hạn; detail sheet format ngày/giá, hiện phí phát sinh; **action gate đúng theo trạng thái+loại** (confirm bỏ đồ; hoàn tất; gia hạn/kết thúc thuê; ủy quyền; báo ô lỗi; hủy chỉ khi `INITIALIZED`).
- `flutter analyze` 0 error (debt info/warning cũ không đổi). Chưa smoke trên emulator phiên này.

Màn Cửa hàng (mới, 2026-06-13):

- `/stores`: danh sách cửa hàng, tìm theo tên/địa chỉ, nút "gần tôi" (geolocator) gọi `GET /api/stores?latitude&longitude`.
- `/stores/detail`: chi tiết cửa hàng (tên, trạng thái, địa chỉ, SĐT, mô tả, khoảng cách), nút Chỉ đường (mở Google Maps qua `url_launcher`), nút Xem tủ (`/lockers`), và danh sách đánh giá từ `GET /api/stores/{id}/ratings`.
- Feature: `lib/features/stores/**` (clean-arch: domain entity `Store`/`StoreRating` + infrastructure `StoreService` + presentation pages/widgets). Đã `flutter analyze` 0 error.

Maintenance home (đã xác minh 2026-06-13):

- Role `MAINTENANCE` đăng nhập/auto-login được route tới `/maintenance-home` (`homeForRoles()` ở `splash_screen` và `login_screen`).
- `MaintenanceHomePage` gọi `/api/maintenance/faults`, `/api/maintenance/reports?mine=`, claim/resolve/clear-fault. Backend API đã có sẵn trong `locker-service`.

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
- Vouchers.
- User laundry order.
- QR login scanner.

Không nên xem các feature trên là hoàn tất với backend hiện tại nếu chưa verify riêng.

Lỗi không chặn demo hiện tại:

- Home có thể gọi endpoint legacy như `/advertisements`, `/blogs`, `/wallet/balance`; backend hiện tại có thể trả `404`.

## 22. Luồng Web Frontend

Flow giá trị cao hiện tại của React app:

- Admin dashboard.
- Admin users.
- Admin stores.
- Admin services.
- Admin lockers.
- Admin locker layout.
- Admin maintenance.
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
- Drone delivery service đầy đủ.
- Drone fleet management.
- Phân công drone theo pin/battery-aware assignment.
- Drone simulator và realtime map tracking.
- AI/RAG knowledge support.
- Đối soát/thanh quyết toán provider payment cấp production.
- Cài đặt Firebase/FCM credential cấp production.
- Full parity các feature mobile legacy với backend hiện tại.

## 25. Phân Tích Luồng Tủ & Bổ Sung Chuẩn Thực Tế (tham chiếu spec)

Tài liệu đặc tả đầy đủ (phân tích as-is bám code + bổ sung toàn bộ luồng nghiệp vụ tủ khóa chuẩn thực tế + gap map + backlog): **`docs/project-artifacts/guides/LOCKER_FLOWS_STANDARD_SPEC.md`** (2026-06-14).

Các điểm as-is đã xác minh trực tiếp từ code, cần lưu ý vì là lỗ hổng đúng đắn của luồng tủ đang chạy:

- **Trạng thái ô chỉ có 4 giá trị**: `AVAILABLE / RESERVED / OCCUPIED / FAULT` (xem `LockerService`). `EXPIRED` chỉ tồn tại ở cấp order qua `pickupDeadline`, ô không có trạng thái `EXPIRED/OUT_OF_SERVICE/CLEANING`.
- **Ô `RESERVED` không có TTL** và `autoCancelUnconfirmedOrders()` đổi đơn `INITIALIZED`>24h sang `CANCELED` nhưng **không release ô** và **không được `@Scheduled`** → ô có thể kẹt `RESERVED` (Gap G1/G2). `cancel()` thủ công thì có release.
- **Quá hạn lấy hàng chỉ nhắc + cộng phí**; ô vẫn `OCCUPIED` tới khi có lệnh complete/checkout — chưa có move-to-storage/giải phóng tự động (Gap G3).
- **Luồng nhận hàng qua shipper/courier (PARCEL_RECEIVE) chưa có code** — SEND hiện chỉ là C2C giữa 2 app-user; chưa có "courier access code" tách với PIN khách (Gap G6).
- **Deadline SEND mặc định 48h**, laundry pickup 24h; rental deadline = số giờ thuê (cấu hình `app.order.*`).
- **PIN/QR**: QR ký số HMAC gắn PIN hiện tại → đổi PIN (delegate/reset/SEND handover) vô hiệu QR cũ; `getByAccess` nhận cả PIN và QR.
- **Trạng thái ô là bản sao best-effort** của order (occupy/release nuốt lỗi) → có rủi ro lệch trạng thái, chưa có job đối soát (Gap G4).

Toàn bộ danh sách 16 gap (G1–G16), đề xuất data model/API, và lộ trình implement 7 giai đoạn (L1–L7, mỗi giai đoạn 1 branch) nằm trong spec ở trên.
