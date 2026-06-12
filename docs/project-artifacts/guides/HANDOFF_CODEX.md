# HANDOFF CHO CODEX — Hoàn thiện luồng tủ end-to-end (backend + FE + mobile 3 role)

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

> Ngày bàn giao: 2026-06-13 • Người bàn giao: Claude (phiên trước đó đã làm Phase 1 + Phase 2 backend, FE dashboard, và ~80% mobile)
> Workspace: `G:\BigProject` (KHÔNG phải D:\). Git: thư mục gốc KHÔNG phải git repo.

## 0A. CẬP NHẬT SAU KHI CODEX TIẾP TỤC (2026-06-13)

Các mục handoff ban đầu bên dưới đã được tiếp tục xử lý trong phiên hiện tại:

- Mobile đã repoint quick actions ở `home_page.dart`: `Thuê tủ`, `Gửi hàng`, `Đơn tủ` đi vào route locker ops thật.
- `flutter pub get`, targeted `flutter analyze`, `flutter build apk --debug` đều PASS.
- APK debug đã cài lên emulator; customer login `demo@laundry.test` / `secret123` PASS; các route Rental, Send Parcel, My Locker Orders render được.
- FE admin `npm.cmd run build` đã PASS sau khi sửa lỗi TypeScript cũ ở examples/loyalty/order timeline.
- Backend health qua gateway trả `200`; login manager và maintenance qua API PASS.
- Tài liệu/artifact đã được gom vào `laundry-locker-microservices/docs/project-artifacts/`.

Phần còn lại chính sau cập nhật này là Phase 3/tương lai: tablet-web cabinet UI, sensor auto-occupy, drone-service thật, payment provider thật cho SEND/RENTAL nếu cần, và xử lý các legacy endpoint mobile như advertisements/blogs/wallet.

## 0. YÊU CẦU GỐC CỦA NGƯỜI DÙNG (chưa hoàn thành 100%)

Người dùng (Nguyễn Quốc Bảo Huy, sinh viên FPT, capstone "Drone Delivery and Smart Locker Management System") yêu cầu:
1. Thực hiện các phase còn lại của `LOCKER_FLOW_PLAN.md` (Phase 2, 3).
2. Backend chuẩn production, chuyên nghiệp.
3. FE (React) đầy đủ giao diện cho các role liên quan.
4. **Mobile (Flutter) phải có 3 role: (1) User, (2) Đội quản lý (MANAGER), (3) Đội bảo trì (MAINTENANCE)** — mỗi role có giao diện riêng.
5. Luồng nghiệp vụ tủ hoàn chỉnh từ đầu đến cuối: user dùng từ lúc tạo đơn đến kết thúc, admin/quản lý vận hành ra sao.

**Ràng buộc bắt buộc (người dùng đặt từ đầu, vẫn hiệu lực):**
- KHÔNG xóa file, KHÔNG reset git, KHÔNG in secrets (mask ****), KHÔNG đổi DB production khi chưa hỏi.
- Sửa lỗi config/build an toàn, không tự ý đổi business logic lớn.
- Luồng tủ và luồng drone là 2 luồng TÁCH BIỆT — drone chỉ là 1 kênh deposit vào ô hàng 1 (`cellType=DRONE`, reserve với `?channel=DRONE`). Mọi nghiệp vụ tủ khác không phụ thuộc drone.

## 1. KIẾN TRÚC & MÔI TRƯỜNG (đang chạy)

- **Backend**: `laundry-locker-microservices/` — Java 21, Spring Boot 3.5.14, Maven (`C:\Maven\apache-maven-3.9.16\bin\mvn`, KHÔNG có mvnw). 14 container Docker đang chạy (tên dạng `ll-ms-*`). Gateway **:8080**, Eureka :8761, Postgres host-port :15432 (user `postgres`/`postgres`, mỗi service 1 DB riêng: `locker_db` schema `locker_schema`, `order_db`, `user_db`, `auth_db`...), RabbitMQ :5672.
- 2 module `laundry-service`, `partner-service` KHÔNG tồn tại trên đĩa → đã loại bằng `docker-compose.override.yml` (profile `missing-source`). ĐỪNG cố build chúng.
- **FE**: `laundry-locker-frontend/fe/` — React 19 + Vite, chạy dev tại **:3000** (`npm run dev`; `.env` → `VITE_API_BASE_URL=http://localhost:8080`). Redux Toolkit Query + tailwind + radix ui + antd + sonner.
- **Mobile**: `smart-laundry-locker-mobile/` — Flutter 3.44.2 tại `C:\flutter` (thêm vào PATH nếu shell mới: `$env:PATH += ";C:\flutter\bin"`). `.env` có `API_BASE_URL=http://10.0.2.2:8080` (envied — nếu đổi .env phải chạy `dart run build_runner build --delete-conflicting-outputs`). Emulator AVD `Pixel_8` (đã sửa skin). App id `com.laundrylocker.mobile`.
- **IoT**: `smart-locker-iot/` Python/uv, SIMULATION=true — không liên quan việc còn lại.

### Lệnh build/deploy backend (QUAN TRỌNG — thứ tự bắt buộc)
```powershell
cd G:\BigProject\laundry-locker-microservices
mvn -q clean package -DskipTests              # PHẢI mvn clean trước; jar build dở sẽ làm container crash-loop ClassNotFoundException
docker compose build <service> ; docker compose up -d <service>
```
Service khởi động ~60-180s (chờ đăng ký Eureka rồi mới test qua gateway).

### Tài khoản test (đều hoạt động, đăng nhập `POST /api/auth/login {"identifier","password"}`)
| Email | Mật khẩu | Role | userId |
|---|---|---|---|
| demo@laundry.test | secret123 | CUSTOMER | 1 |
| nqbhuy2004nt@gmail.com | Test@123456 | CUSTOMER | 2 |
| admin@laundry.test | Admin@123456 | ADMIN | 5 |
| manager@laundry.test | Manager@123456 | MANAGER | 6 |
| maintenance@laundry.test | Maint@123456 | MAINTENANCE | 7 |

Đăng ký kèm role: `POST /api/auth/register` nhận field `roles: ["MANAGER"]` (dev convenience).

### Tủ demo
- Locker **id=2** `CAB-DEMO-01` (landing pad, marker ARUCO-23): 10 ô = 3 DRONE (hàng 1, box id 13-15), 6 STANDARD (hàng 2-3, id 16-21), 1 XL vali (id 22, hàng 2 cột 0). Locker id=1 `LCK-Q1-01` là tủ cũ.
- Xem layout: `GET /api/lockers/2/layout` (public GET).

## 2. ĐÃ HOÀN THÀNH (100% test PASS)

### 2.1 Backend Phase 1 (phiên trước) — locker-service cell model
- Flyway V2 (cell_type/row_index/col_index/fault_reason, landing_pad/landing_marker_id) + V3 (seed CAB-DEMO-01).
- Vòng đời ô AVAILABLE→RESERVED→OCCUPIED→AVAILABLE + FAULT (sticky). Guard `DRONE_CELL_RESTRICTED` khi reserve ô DRONE không có `?channel=DRONE`.
- API: layout, occupy, fault, clear-fault (admin path `/api/admin/lockers/boxes/{id}/clear-fault`), find-available.
- order-service: confirm→occupy ô, return→occupy ô nhận, PATCH /status CANCELED/COMPLETED→release ô, delegate (ủy quyền, PIN mới + notification).

### 2.2 Backend Phase 2 (phiên này) — TẤT CẢ ĐÃ DEPLOY + E2E PASS lúc 2026-06-13
**locker-service** (`locker-service/src/main/java/.../locker/`):
- Migration `V4__maintenance_upgrade.sql`: locker_reports thêm `box_id`, `assigned_to_user_id`, `assigned_at` + index.
- `LockerReport` entity + `LockerReportResponse` thêm 3 field; `markFault` giờ gắn boxId vào report.
- DTO mới: `LockerStatsResponse`, `FaultCellResponse`.
- `LockerService`: `stats(storeId)` (đếm theo trạng thái + utilization% + openReports), `openFaults()`, `openReports()`, `assignedReports(userId)`, `claimReport` (OPEN→IN_PROGRESS + assignee), `resolveReportAndClearFault` (RESOLVED + ô FAULT→AVAILABLE), `getCell(boxId)`.
- Controller endpoints mới:
  - Manager: `GET /api/manage/lockers/stats`, `GET /api/manage/lockers`, `GET /api/manage/lockers/{id}/layout`, `GET /api/manage/lockers/reports`
  - Maintenance: `GET /api/maintenance/faults`, `GET /api/maintenance/reports?mine=`, `PUT /api/maintenance/reports/{id}/claim`, `PUT /api/maintenance/reports/{id}/resolve`, `POST /api/maintenance/boxes/{id}/clear-fault`
  - Internal: `GET /internal/lockers/boxes/{boxId}/cell`

**order-service** (`order-service/src/main/java/.../order/`):
- Migration `V2__phase2_send_rental.sql`: orders thêm `last_reminder_at` + index status/receiver_phone/type.
- `QrTokenService` — QR ký số stateless: format `LLQR.<orderId-base36>.<HMAC-SHA256(orderId:pinCode) base64url>`; secret config `app.security.qr-secret` (default dev `laundry-locker-dev-qr-secret`). Đổi PIN là QR cũ tự vô hiệu. `OrderResponse` thêm field `qrToken` (sau `pinCode`).
- **Luồng SEND**: `POST /api/orders/send {lockerId, receiverPhone, receiverName?, note?, boxId?, size?}` (header X-User-Id do gateway gắn) → tự tìm ô STANDARD trống → PIN bỏ hàng. `PUT /api/orders/{id}/confirm` với type SEND → occupy + **xoay PIN mới cho người nhận** + deadline 48h (`app.order.send-pickup-hours-limit`) + tìm user theo SĐT (user-service `GET /internal/users/by-phone?phone=`) → set receiverId + notification `ORDER_PARCEL_READY`; sender nhận notification `ORDER_PARCEL_STORED`. Người nhận `PUT /api/orders/{id}/complete` được phép (assertOwnerOrReceiver) → release ô.
- **Luồng RENTAL**: `POST /api/orders/rental {lockerId, cellType STANDARD|XL, hours 1-720, boxId?}` — giá `app.order.rental-rate-standard:5000`/`rental-rate-xl:10000` đ/giờ, deadline = now+hours; confirm → STORING (PIN đa lần — verify không xóa PIN); `POST /api/orders/{id}/extend-rental {hours}` (lấy cellType thật của ô qua LockerCellClient.getCell để tính giá); kết thúc: `POST /api/orders/{id}/pickup-storage` (đã mở rộng cho RENTAL, tính phí quá giờ calculatePickupOvertimeFee) → release.
- **QR/access**: `GET /api/orders/access/{code}` + `GET /internal/orders/by-access?code=` — nhận PIN 6 số hoặc QR token; iot-service `POST /api/iot/verify-access {boxId, pinCode:<PIN hoặc QR>}` (IotService.verifyAccess; OrderClient đổi sang `@FeignClient(name="order-service")` không prefix, gọi by-access).
- **Manager**: `GET /api/manage/orders?status=&type=&lockerId=` (limit 500, sort desc), `GET /api/manage/orders/statistics`.
- **Scheduler** (`OrderScheduler` + `@EnableScheduling` trên `OrderServiceApplication`): nhắc quá hạn mỗi 10' (`app.order.reminder-interval-ms`, cooldown 60' qua `last_reminder_at`, notify owner + receiver `ORDER_PICKUP_OVERDUE`); quét release ô của đơn COMPLETED hằng đêm 03:15.
- Feign mới: `LockerCellClient` (contextId="lockerCellClient", path /internal/lockers — findAvailable + getCell), `UserClient.getUserByPhone`.

**user-service**: `UserProfileRepository.findFirstByPhoneNumber`, `UserProfileService.getByPhone` (ném BusinessException USER_NOT_FOUND), endpoint `GET /internal/users/by-phone?phone=`.

**api-gateway** (`JwtGatewayFilter` + `application.yml`):
- **RBAC**: `/api/admin/**`→ADMIN; `/api/manage/**`→MANAGER|ADMIN; `/api/maintenance/**`→MAINTENANCE|ADMIN.
- **`/internal/**` bị CHẶN HOÀN TOÀN qua gateway (403)** — service gọi nhau qua Eureka/Feign. Test nội bộ phải `docker exec ll-ms-<svc> curl http://localhost:<port>/internal/...` hoặc qua Feign.
- Public chỉ còn GET (browse catalogue: stores/lockers/services/promotions); POST/PUT/DELETE lên `/api/lockers|/api/boxes` cần role ADMIN/MANAGER trừ path kết thúc `/fault`, `/report`, `/open` (khách được báo hỏng/report/mở ô).
- Routes thêm: `/api/manage/orders(,/**)` → order-service; `/api/manage/lockers(,/**)`, `/api/maintenance/**` → locker-service.

**Kết quả E2E backend (đều PASS, chạy 2026-06-13):** RBAC matrix (manager 403 vào /api/admin, customer 403 vào /api/manage, internal 403); SEND đầy đủ 2 giai đoạn PIN + notification người nhận + người nhận complete; RENTAL XL 2h=20.000đ → extend 3h → 50.000đ → end COMPLETED; QR cấp/tra cứu/verify OK + QR giả bị từ chối; bảo trì: user báo hỏng → maintenance thấy fault+report → claim (IN_PROGRESS) → resolve → ô AVAILABLE. Layout cuối: 10/10 ô AVAILABLE.

### 2.3 FE React (code xong, type-check các file mới SẠCH, **chưa smoke-test trên browser**)
- `src/stores/apis/admin/lockerOps.ts` — RTK Query: getLockerStats/getLockerLayout/getFaultCells/getMaintenanceReports/claimReport/resolveReport/reportBoxFault/clearBoxFault (tag `Lockers`).
- `src/pages/Admin/lockers/layout-view.tsx` — **trang sơ đồ tủ trực quan** route `/admin/lockers/:lockerId`: lưới ô theo hàng/cột đúng vật lý, màu theo trạng thái, icon DRONE/XL, badge landing pad ARUCO, 4 thẻ thống kê, nút Báo hỏng/Đã sửa từng ô, polling 15s.
- `src/pages/Admin/maintenance/index.tsx` — trang bảo trì route `/admin/maintenance`: danh sách ô hỏng + nút "Đã sửa xong"; phiếu sự cố + Nhận việc/Hoàn tất.
- `src/routes/routes-config.tsx`: route `lockers` (trang list có sẵn `pages/Admin/lockers/index.tsx` — dữ liệu THẬT qua useGetAllLockersQuery), `lockers/:lockerId` (layout-view mới), `maintenance`. Lazy imports thêm ở đầu file.
- `src/constants/sidebar.ts`: thêm 2 mục Lockers (Boxes icon) + Maintenance (Briefcase icon). i18n key `admin.sidebar.maintenance` đã thêm vào `messages/vi.json`, `en.json`, `ja.json`.
- Đã sửa lỗi CÓ SẴN: `useLockers.ts` thiếu `handleMaintenance`/`handleActivate` (đã thêm bằng useSetLockerMaintenanceMutation + toast).
- ✅ **`npm.cmd run build` đã PASS** sau khi sửa lỗi TypeScript cũ: `src/components/ui/examples/*.tsx`, `src/stores/apis/loyaltyApi.ts`, và `src/pages/Admin/orders/components/OrderTimeline.tsx`.

### 2.4 Mobile Flutter (CODE XONG + BUILD/SMOKE PASS)
Module mới `lib/features/locker_ops/`:
- `data/locker_ops_service.dart` — service Dio gọi gateway: lockers/layout/myOrders/createSend/createRental/confirmDrop/completePickup/endRental/extendRental/cancelOrder/delegate/reportFault/managerStats/managerOrders/faults/reports/claimReport/resolveReport/clearFault + `errorMessage()`.
- `presentation/widgets/ops_widgets.dart` — StatusChip, AccessCredentials (PIN copy + **QR render bằng `qr_flutter`**), màu/label trạng thái tiếng Việt.
- `presentation/pages/send_parcel_page.dart` — luồng GỬI HÀNG 2 bước (form → PIN bỏ hàng → "Tôi đã bỏ hàng" → PIN người nhận).
- `presentation/pages/rent_locker_page.dart` — THUÊ TỦ (chọn tủ + loại ô STANDARD/XL + slider giờ + tạm tính → PIN/QR → bắt đầu kỳ thuê).
- `presentation/pages/my_locker_orders_page.dart` — ĐƠN TỦ CỦA TÔI (filter active/done, bottom-sheet chi tiết: PIN/QR + hành động confirm-drop/complete/extend/end-rental/delegate/cancel theo status+type).
- `presentation/pages/manager_home_page.dart` — HOME role MANAGER 3 tab: Thống kê (cards + utilization bar), Sơ đồ tủ (grid theo hàng/cột, long-press ô để báo hỏng/mở lại), Đơn hàng (filter chip status). AppBar có logout.
- `presentation/pages/maintenance_home_page.dart` — HOME role MAINTENANCE 2 tab: Sự cố (ô lỗi + phiếu, nút Nhận việc/Hoàn tất), Việc của tôi (mine=true). AppBar đỏ đậm + logout.

Đã sửa core:
- `lib/features/auth/presentation/pages/login_screen.dart` — **VIẾT LẠI HOÀN TOÀN**: form identifier+password thật → `POST /api/auth/login` → `TokenService.saveTokens` → điều hướng theo role qua `homeForRoles(roles)`. (UI cũ OTP mock đã bỏ.)
- `lib/core/routing/role_routes.dart` (MỚI) — `homeForRoles`: MANAGER/ADMIN→`/manager`, MAINTENANCE→`/maintenance-home`, khác→`/home`.
- `lib/features/auth/presentation/pages/splash_screen.dart` — khi có token sẵn → cũng điều hướng theo role.
- `lib/core/routing/app_router.dart` — thêm constants + 5 GoRoute: `/manager`, `/maintenance-home`, `/locker/send-parcel`, `/locker/rent`, `/locker/my-orders` + 5 import pages.
- `pubspec.yaml` — đã thêm `qr_flutter: ^4.1.0`; `flutter pub get` đã PASS.

## 3. VIỆC CÒN LẠI (sau cập nhật 2026-06-13)

Các việc Mobile/FE ở mục 3.1 và 3.2 bên dưới là checklist handoff cũ và phần chính đã được xử lý. Trạng thái mới:

- Mobile customer quick actions đã nối route thật; targeted analyze/build APK/debug install đã PASS.
- FE admin build prod đã PASS.
- API login manager/maintenance đã PASS.
- UI smoke thủ công cho manager/maintenance trên emulator chưa hoàn tất vì input/launcher ADB không ổn định, nhưng backend role response và routing code đã xác minh.

Phần còn lại nên ưu tiên Phase 3 và legacy cleanup.

### 3.1 Mobile — checklist handoff cũ (đã xử lý phần chính)
1. **`lib/features/home/presentation/pages/home_page.dart`** (file 1303 dòng) — repoint quick actions (đang sửa DỞ — chưa đổi gì):
   - Dòng ~685: card "Thuê tủ" `context.push('/locker-action', extra: LockerAction.rent)` → `context.push(AppRouter.rentLocker)`.
   - Dòng ~708: card "Gửi hàng" `context.push('/locker-action', extra: LockerAction.send)` → `context.push(AppRouter.sendParcel)`.
   - Khối Row thứ 2 (dòng ~715-741) có ô `const Expanded(child: SizedBox())` trống — thay bằng card "Đơn tủ" trỏ `AppRouter.myLockerOrders` (icon gợi ý `LucideIcons.package`, theo mẫu `_buildExpressCard(title, subtitle, icon, bgColor, iconColor, onTap)`).
   - Import thêm nếu cần (file đã import AppRouter; `LockerAction` import có thể thừa sau khi đổi — nếu unused thì cứ để, đừng xóa import đang được dùng chỗ khác).
2. `cd G:\BigProject\smart-laundry-locker-mobile; C:\flutter\bin\flutter pub get` (kéo qr_flutter).
3. `C:\flutter\bin\flutter analyze lib/features/locker_ops lib/core/routing lib/features/auth/presentation/pages/login_screen.dart lib/features/auth/presentation/pages/splash_screen.dart` — sửa lỗi nếu có (cảnh báo deprecation withOpacity/`DropdownButtonFormField value` là Info, bỏ qua được).
4. Chạy emulator + app: `C:\flutter\bin\flutter emulators --launch Pixel_8` rồi `C:\flutter\bin\flutter run -d emulator-5554` (backend Docker phải đang chạy).
5. **Test 3 role trên app**: đăng nhập từng tài khoản ở bảng mục 1 → user thấy home cũ + 3 quick action mới hoạt động (tạo đơn gửi/thuê thật, xem PIN+QR, ủy quyền); manager@ thấy ManagerHomePage; maintenance@ thấy MaintenanceHomePage (tạo fault trước bằng cách user báo hỏng hoặc qua FE).
6. Lỗi dễ gặp: nếu login bị redirect loop — kiểm tra `AppRouter.redirect` (chỉ chặn transactions/topUp nên không sao); nếu API 403 — token role sai; emulator gọi `10.0.2.2:8080`.

### 3.2 FE — smoke test (15')
- FE dev server có thể đã chạy ở :3000 (nếu chưa: `cd G:\BigProject\laundry-locker-frontend\fe; npm run dev`).
- Đăng nhập admin@laundry.test → kiểm tra sidebar có "Lockers"/"Bảo trì tủ" → `/admin/lockers` (bảng), bấm 1 tủ → `/admin/lockers/2` (lưới ô CAB-DEMO-01), thử Báo hỏng + Đã sửa; `/admin/maintenance`.
- Lưu ý: FE login admin có thể qua flow 2FA admin (`/api/admin/auth/login`) — nếu trang login admin không vào được bằng tài khoản trên, thử login user thường rồi truy cập route admin, hoặc kiểm tra `pages/auth/Login`. (Chưa kiểm tra phần này.)

### 3.3 Kiểm tra hồi quy nhỏ
- `G:\BigProject\seed-test-data.ps1` — sau khi gateway siết quyền, script này có thể fail ở các bước mutation /api/lockers|/api/boxes nếu chạy bằng tài khoản CUSTOMER. Chạy lại thử; nếu fail, đổi script sang đăng nhập `admin@laundry.test` (hoặc chỉ đổi các call bị 403). KHÔNG nới lỏng lại gateway.
- Luồng giặt ủi cũ (LAUNDRY) đã pass hồi quy ở phiên trước (sau các thay đổi confirm/occupy) — không cần test lại trừ khi đổi thêm order-service.

### 3.4 Tài liệu (sau khi mobile chạy được)
- Cập nhật `RUN_RESULT.md` — thêm mục 10: Phase 2 (SEND/RENTAL/QR/RBAC/maintenance/scheduler) + 5 tài khoản + FE/mobile mới.
- Cập nhật `LOCKER_FLOW_PLAN.md` — tick các mục Phase 2 đã xong.
- Cập nhật memory Claude (`C:\Users\Admin\.claude\projects\g--BigProject\memory\bigproject-structure.md`) nếu muốn.

### 3.5 Phase 3 (CHƯA làm — tương lai, đừng tự ý bắt đầu nếu user chưa yêu cầu)
- Tablet-web màn hình tủ (`laundry-locker-frontend/iot/tablet-web`?) gọi layout + verify-access.
- iot-service auto-occupy theo cảm biến cửa; kênh drone thật (drone-service riêng theo `GAP_ANALYSIS_AND_PLAN.md`).
- Payment thật cho RENTAL/SEND (hiện paymentRequired chỉ là cờ, flow payment-service có sẵn POST /api/payments).
- Mobile: lockerOtp page cũ + LockerActionPage cũ vẫn mock — có thể nối verify-access sau.

## 4. BẪY ĐÃ BIẾT (tránh dẫm lại)
1. **PowerShell `Get-Content|Set-Content` phá UTF-8 tiếng Việt** — luôn dùng Write/Edit tool hoặc python io với `encoding='utf-8'`.
2. **Maven build bị ngắt → jar hỏng → docker container crash-loop `ClassNotFoundException`** — luôn `mvn clean package` xong hẳn rồi mới `docker compose build`.
3. PowerShell 5.1: không có `&&`; dùng Git Bash cho lệnh phức tạp.
4. Gateway giờ CHẶN `/internal/**` — đừng test internal qua :8080; dùng `docker exec ll-ms-locker-service curl http://localhost:8084/internal/...`.
5. `DropdownButtonFormField(value:)` deprecated trong Flutter 3.44 — chỉ là info, app vẫn build.
6. FE `npm.cmd run build` hiện đã PASS; lỗi TS cũ ở `src/components/ui/examples/*`, `loyaltyApi.ts` và `OrderTimeline.tsx` đã được sửa trong phiên Codex tiếp theo.
7. Mobile login cũ là MOCK — đã thay; `verifyOtp`/`register` datasource cũ vẫn sai base path ('/auth' thiếu '/api') nhưng KHÔNG còn được dùng bởi màn login mới.
8. Khi sửa record Java (OrderResponse/BoxRequest...) nhớ sửa MỌI chỗ `new XxxResponse(...)` — toResponse trong OrderService là chỗ duy nhất với OrderResponse.

## 5. THAM CHIẾU NHANH API MỚI (đầy đủ hơn xem mục 2.2)
```
# User
POST /api/orders/send | /api/orders/rental | /api/orders/{id}/extend-rental {hours} | /api/orders/{id}/pickup-storage | /api/orders/{id}/delegate {phone,name,note}
PUT  /api/orders/{id}/confirm | /complete | /cancel
GET  /api/orders/my-orders | /api/orders/access/{PIN hoặc LLQR...} | /api/lockers/{id}/layout (public GET)
POST /api/boxes/{id}/fault {reason}
POST /api/iot/verify-access {boxId, pinCode}
# Manager (role MANAGER/ADMIN)
GET /api/manage/lockers/stats | /api/manage/lockers/{id}/layout | /api/manage/orders?status=&type= | /api/manage/orders/statistics | /api/manage/lockers/reports
# Maintenance (role MAINTENANCE/ADMIN)
GET /api/maintenance/faults | /api/maintenance/reports?mine=true
PUT /api/maintenance/reports/{id}/claim | /resolve
POST /api/maintenance/boxes/{id}/clear-fault
```
Response chuẩn: `{success, code, message, data}`. OrderResponse có `pinCode` + `qrToken` khi PIN active.
