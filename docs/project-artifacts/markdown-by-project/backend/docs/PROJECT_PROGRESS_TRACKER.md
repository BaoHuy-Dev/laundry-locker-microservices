# Theo Dõi Tiến Độ Dự Án

> Cập nhật lần cuối: 2026-06-13
> Workspace: `G:\BigProject`
> Cặp tài liệu nguồn: file này + `docs/BUSINESS_FLOWS_CURRENT.md`

## Quy Tắc Cập Nhật Bắt Buộc

Mỗi khi developer hoặc AI coding agent có tiến độ mới trong dự án, file này phải được cập nhật trong cùng phiên làm việc.

Cập nhật file này sau khi:

- Implement một chức năng.
- Fix bug.
- Đổi API, route, DTO, migration database, event, scheduler, màn hình, hoặc service contract.
- Chạy thành công hoặc chạy lỗi một lệnh verification.
- Phát hiện blocker, technical debt, hoặc giả định cũ bị sai.
- Tạo/đổi demo data hoặc credential deploy dùng cho test account.

Nếu thay đổi làm đổi hành vi nghiệp vụ, phải cập nhật thêm `docs/BUSINESS_FLOWS_CURRENT.md`.

Mức cập nhật tối thiểu cho mỗi phiên làm việc:

1. Thêm một dòng vào **Nhật Ký Thay Đổi Gần Đây**.
2. Cập nhật **Bảng Tiến Độ Theo Component** nếu status thay đổi.
3. Cập nhật **Việc Còn Lại** nếu có việc đã xong, mới thêm, hoặc đổi ưu tiên.
4. Cập nhật **Nhật Ký Verification** với lệnh/kết quả.
5. Nếu docs/artifacts được di chuyển hoặc generate, cập nhật `docs/project-artifacts/README.md`.

## Cách AI Khác Nên Bắt Đầu

Đọc các file theo thứ tự:

1. `docs/PROJECT_PROGRESS_TRACKER.md`
2. `docs/BUSINESS_FLOWS_CURRENT.md`
3. `docs/CURRENT_PROJECT_STATUS.md`
4. `RUN_RESULT.md`
5. `LOCKER_FLOW_PLAN.md`
6. File code liên quan đến task.

Không mặc định tài liệu cũ là đúng nếu chúng mâu thuẫn với các file trên.

## Trạng Thái Tổng Quan Hiện Tại

Dự án hiện có nền tảng locker/laundry đang chạy với backend Locker Phase 2, màn hình web admin cho locker/maintenance, và Flutter routing theo role cho các flow locker.

Tóm tắt trạng thái:

- Backend Locker Phase 1: đã xong và đã test.
- Backend Locker Phase 2: đã xong và đã test.
- Web admin locker/maintenance: đã implement và đã verify build.
- Flutter customer/manager/maintenance locker flows: đã implement, đã verify targeted build/analyze.
- IoT backend verify-access bằng PIN/QR: đã implement.
- Drone delivery/assignment/tracking thật: chưa implement.
- AI/RAG: chưa implement.
- `laundry-service` và `partner-service`: thiếu source module.

## Bản Đồ Repository

| Khu vực | Đường dẫn | Branch quan sát gần nhất | Trạng thái |
|---|---|---|---|
| Backend | `laundry-locker-microservices/` | `develop` | Source backend chính; worktree đang dirty với thay đổi Locker Phase 1/2 và docs. |
| Web frontend | `laundry-locker-frontend/` | `main` | Có admin locker/maintenance và các fix TypeScript. |
| Flutter mobile | `smart-laundry-locker-mobile/` | `develop` | Có module locker ops và role routing. |
| IoT runtime | `smart-locker-iot/` | chưa check trong pass gần nhất | Python runtime/simulation cho cabinet. |
| Workspace root | `G:\BigProject` | không phải git repo | Chứa docs/scripts/local secret notes liên project. |

Không commit:

- `env.txt`
- `pro.txt`
- `Application.txt`
- local host/secret notes
- mobile `.env` nếu chưa sanitize có chủ đích

## Bảng Tiến Độ Theo Component

Ý nghĩa status:

- `DONE`: đã implement và verify đủ cho demo hiện tại.
- `PARTIAL`: có code nhưng chưa verify đầy đủ, còn mismatch legacy, hoặc phụ thuộc service/integration đang thiếu.
- `TODO`: chưa implement.
- `BLOCKED`: không thể tiếp tục nếu thiếu credential, server access, hoặc quyết định product.

| Component / Flow | Status | Bằng chứng | Bước tiếp theo |
|---|---|---|---|
| Auth register/login/JWT | DONE | `auth-service`, login smoke với customer/manager/maintenance pass. | Đồng nhất role naming trong docs/code nếu cần. |
| User profile/roles | DONE | `user-service`, `user_profiles`, admin user APIs. | Đảm bảo deployed seed roles có `MANAGER`/`MAINTENANCE` nếu cần demo. |
| Gateway routing/RBAC | DONE | Guard cho `/api/admin/**`, `/api/manage/**`, `/api/maintenance/**`, `/internal/**`. | Cập nhật route table khi thêm service. |
| Locker physical cell model | DONE | V2/V3 migrations, `DRONE/STANDARD/XL`, layout endpoint. | Chỉ thêm cell status `EXPIRED` nếu product cần. |
| Locker fault/maintenance flow | DONE | V4 migration, API claim/resolve report, FE/mobile pages. | Thêm lịch/log bảo trì định kỳ nếu cần. |
| SEND parcel flow | DONE | `POST /api/orders/send`, PIN hai giai đoạn, QR, receiver flow. | Thêm payment UX nếu SEND là tính năng có phí. |
| RENTAL flow | DONE | `POST /api/orders/rental`, extend/end, tính giá theo giờ. | Nối provider payment nếu yêu cầu. |
| PIN/QR verify-access | DONE | `QrTokenService`, `POST /api/iot/verify-access`. | Build tablet-web/cabinet UX. |
| Laundry order legacy flow | PARTIAL | Core endpoints tồn tại và smoke cũ pass; chưa regression đầy đủ gần nhất. | Re-test full lifecycle sau mỗi thay đổi `order-service`. |
| Staff operations | PARTIAL | Staff endpoints tồn tại; không phải path mobile/FE chính hiện tại. | Quyết định STAFF có tách với MANAGER không. |
| Admin web dashboard | PARTIAL | Main admin routes tồn tại; build pass. | Browser smoke tất cả admin pages. |
| Admin locker list/layout | DONE | `/admin/lockers`, `/admin/lockers/:lockerId`, RTK lockerOps. | Giữ UI khớp `LockerLayoutResponse`. |
| Admin maintenance page | DONE | `/admin/maintenance`, action report/fault. | Browser smoke với admin token thật. |
| Partner portal | PARTIAL | FE routes tồn tại; backend `partner-service` thiếu. | Build lại source service hoặc mark out of scope. |
| Services/laundry catalog | PARTIAL | FE/admin docs tồn tại; backend `laundry-service` thiếu. | Build lại `laundry-service` hoặc route catalog qua service khác. |
| Payment/refund | PARTIAL | `payment-service` endpoints tồn tại; provider phụ thuộc environment. | Verify VNPay/MoMo với credential sandbox/real. |
| Notifications/FCM/WebSocket | PARTIAL | Service endpoints/events tồn tại; Firebase production chưa verify. | Verify FCM và WebSocket trên deploy. |
| Loyalty | PARTIAL | `loyalty-service` endpoints tồn tại. | Verify event integration và FE/mobile usage. |
| Store management | DONE | `store-service` endpoints và route admin/public. | Browser smoke admin stores nếu cần. |
| Flutter customer locker ops | DONE | Login/home quick actions/rental/send/my-orders smoke pass. | Full end-to-end create/complete trên emulator với deploy. |
| Flutter manager home | PARTIAL | Code và backend role login đã verify; manual UI smoke bị giới hạn. | Test manager UI trên emulator/device ổn định. |
| Flutter maintenance home | PARTIAL | Code và backend role login đã verify; manual UI smoke bị giới hạn. | Test maintenance UI với seeded fault. |
| Flutter legacy courier/logistics | PARTIAL | Nhiều route tồn tại; chưa align backend hiện tại trong pass gần nhất. | Audit hoặc remove/mark legacy. |
| Python IoT runtime | PARTIAL | README/runtime tồn tại; hardware/simulation cần config. | Đồng bộ broker và test với verify-access. |
| Tablet-web cabinet UI | TODO | Mới được nhắc trong docs. | Build dựa trên layout + verify-access. |
| Real drone service | TODO | Thư mục drone có prototype docs/code, chưa có backend service. | Thiết kế `drone-service` và assignment model. |
| Battery-aware assignment | TODO | Gap capstone yêu cầu, chưa có code. | Implement sau khi có drone model. |
| Realtime drone tracking map | TODO | Chưa có map/telemetry service. | Implement bằng simulator và WebSocket. |
| AI/RAG support | TODO | Chưa có `ai-service`. | Chọn provider/storage rồi scaffold. |
| CI/CD | TODO/PARTIAL | Docker compose tồn tại; CI chưa verify trong pass gần nhất. | Thêm GitHub Actions build/test. |

## Chi Tiết Tiến Độ Backend

### Đã Xong

- Maven multi-module backend đã có.
- Database và schema theo service đã có.
- Gateway route table đã có.
- JWT identity propagation đã có.
- Gateway chặn `/internal/**` từ bên ngoài.
- Locker cell model đã implement:
  - `cell_type`
  - `row_index`
  - `col_index`
  - `fault_reason`
  - `landing_pad`
  - `landing_marker_id`
- Demo cabinet đã seed:
  - `CAB-DEMO-01`
  - 3 `DRONE`
  - 6 `STANDARD`
  - 1 `XL`
- Locker Phase 2 đã implement:
  - SEND.
  - RENTAL.
  - QR token.
  - Manager endpoints.
  - Maintenance endpoints.
  - Scheduler reminder/cleanup.

### Partial / Cần Chú Ý

- `docker-compose.yml` vẫn khai báo `laundry-service` và `partner-service` đang thiếu; `docker-compose.override.yml` skip chúng khi chạy local.
- Một số docs cũ và FE pages vẫn giả định có service catalog/partner APIs.
- Seed data role có `USER`, `STAFF`, `PARTNER`, `ADMIN`, trong khi role routing mới dùng thêm `CUSTOMER`, `MANAGER`, `MAINTENANCE`.
- Password của deployed seed account không biết từ seed hash; có thể cần reset cho demo.

### Chưa Bắt Đầu

- Real drone backend service.
- Real partner source service.
- Real laundry catalog source service.
- AI/RAG service.

## Chi Tiết Tiến Độ Frontend

### Đã Xong

- React admin route config có:
  - `/admin/lockers`
  - `/admin/lockers/:lockerId`
  - `/admin/maintenance`
- Admin sidebar có Lockers và Maintenance.
- RTK Query locker ops API slice đã có.
- Lỗi TypeScript trong examples/loyalty/order timeline đã fix.
- `npm.cmd run build` pass trong verification gần nhất.

### Partial / Cần Chú Ý

- Partner portal routes tồn tại nhưng phụ thuộc service backend đang thiếu/partial.
- Một số admin screens có thể vẫn gọi API theo assumption cũ và cần browser smoke.
- Admin auth/2FA cần verify theo environment.

### Chưa Bắt Đầu

- Drone fleet/tracking dashboard.
- Tablet/cabinet web UI.
- AI support UI.

## Chi Tiết Tiến Độ Flutter Mobile

### Đã Xong

- Login screen thật gọi `/api/auth/login`.
- Lưu token và routing theo role.
- `role_routes.dart`:
  - Manager/Admin -> `/manager`
  - Maintenance -> `/maintenance-home`
  - Role còn lại -> `/home`
- Home quick actions đã trỏ lại:
  - Thuê tủ.
  - Gửi hàng.
  - Đơn tủ của tôi.
- Module `locker_ops` mới:
  - API service.
  - Shared widgets.
  - Send parcel page.
  - Rent locker page.
  - My locker orders page.
  - Manager home.
  - Maintenance home.
- Render QR qua `qr_flutter`.
- Targeted analyze/build đã pass.
- Customer UI smoke pass trên emulator cho route rendering.

### Partial / Cần Chú Ý

- Manager và maintenance UI cần manual smoke trên emulator/device ổn định.
- Một số file generated `.g.dart` và `.env` đang modified; cần cẩn thận khi commit.
- Các màn hình cũ vẫn tồn tại:
  - Old locker action.
  - QR login scanner.
  - Courier/logistics.
  - Wallet/transactions.
  - Calls `/advertisements`, `/blogs`, `/wallet/balance` trên home.

### Chưa Bắt Đầu

- Tablet-web/device-facing UI tương đương mobile.
- Realtime drone tracking mobile UI.
- Full payment UX cho SEND/RENTAL.

## Chi Tiết Tiến Độ IoT

### Đã Xong

- Backend IoT service hỗ trợ verify-access bằng PIN và QR.
- Endpoint device status và box-status tồn tại.
- MQTT facade code có trong backend.
- Python runtime tồn tại ở `smart-locker-iot`.

### Partial / Cần Chú Ý

- Broker config phải khớp giữa backend và Python runtime.
- Máy PC không có hardware chỉ nên chạy simulation mode.
- Sensor cửa/weight thật chưa được nối vào backend state flow.

### Chưa Bắt Đầu

- Tự động occupy/release từ sensor thật.
- Flow kết quả biometric verification thật.
- Drone deposit hardware integration.

## Ghi Chú Deploy / Database

DigitalOcean Droplet đã quan sát:

- Public IP: `146.190.84.136`
- PostgreSQL expose port `15432`
- Password superuser `postgres` đã được reset thủ công thành `postgres` khi troubleshoot.
- Deploy DB seed accounts đã quan sát:
  - `customer.seed@laundry.test`
  - `staff.seed@laundry.test`
  - `partner.seed@laundry.test`
  - `admin.seed@laundry.test`
  - `customer.vip@laundry.test`

Quan trọng:

- `POSTGRES_PASSWORD` trong `docker inspect` không chắc là password DB hiện tại nếu volume PostgreSQL đã init trước đó.
- Nếu demo account cần password biết trước, reset `auth_schema.auth_accounts.password_hash` có chủ đích và document password demo đã chọn tại đây.
- Không commit private server notes.

## Nhật Ký Verification

| Ngày | Khu vực | Lệnh / Kiểm tra | Kết quả | Ghi chú |
|---|---|---|---|---|
| 2026-06-13 | Tài liệu | Đổi `BUSINESS_FLOWS_CURRENT.md` và `PROJECT_PROGRESS_TRACKER.md` sang tiếng Việt có dấu, sync mirror artifact, và quét heading tiếng Anh cũ | PASS | Hai file nguồn và bản copy artifact đã được Việt hoá; không đụng secret txt. |
| 2026-06-13 | Tài liệu | `rg -n "BUSINESS_FLOWS_CURRENT|PROJECT_PROGRESS_TRACKER" ...` và quét private-file trong `docs/project-artifacts` | PASS | Living docs đã được link/copy; `env.txt`, `pro.txt`, `Application.txt`, và `Host *.txt` không bị copy vào artifacts. |
| 2026-06-13 | Backend | Gateway health `GET /actuator/health` | PASS | Trả `200`. |
| 2026-06-13 | Backend | SEND/Rental/QR/RBAC/Maintenance E2E | PASS | Đã ghi trong `RUN_RESULT.md` và `HANDOFF_CODEX.md`. |
| 2026-06-13 | Web FE | `npm.cmd run build` trong `laundry-locker-frontend/fe` | PASS | TypeScript build pass. |
| 2026-06-13 | Flutter | `flutter pub get` | PASS | Dependency `qr_flutter` đã resolve. |
| 2026-06-13 | Flutter | Targeted `flutter analyze` | PASS | Không có issue trong các file locker/auth/home đã chạm. |
| 2026-06-13 | Flutter | `flutter build apk --debug` | PASS | Debug APK build thành công. |
| 2026-06-13 | Flutter | Customer emulator smoke | PASS | Login và 3 route quick action locker render được. |
| 2026-06-13 | Flutter | Manager/Maintenance backend role login | PASS | API role login đã verify; manual UI smoke bị giới hạn bởi emulator input. |
| 2026-06-13 | Deploy DB | TCP `146.190.84.136:15432` | PASS | Port reachable. |
| 2026-06-13 | Deploy DB | Service users connect vào DB | PASS | `auth_user`, `user_user`, `order_user`, `locker_user`, ... kết nối được. |
| 2026-06-13 | Deploy DB | Reset password `postgres` trong container | PASS | `docker exec ll-ms-postgres psql ...` verify access nội bộ container. |

## Việc Còn Lại

### P0 - Cần Cho Demo Sạch Hiện Tại

- Tạo hoặc reset demo accounts cho các role hiện tại:
  - Customer.
  - Admin.
  - Manager.
  - Maintenance.
- Browser smoke các trang admin locker/maintenance với deployed backend.
- Full smoke trên emulator/device:
  - Customer tạo SEND.
  - Customer tạo RENTAL.
  - Manager home.
  - Maintenance claim/resolve.
- Quyết định deployed seed roles nên dùng `CUSTOMER` thay vì `USER` không.
- Xác nhận mobile `.env` trỏ đến deployed API khi test trên thiết bị thật.

### P1 - Làm Cứng Sản Phẩm Hiện Tại

- Thêm/verify payment UX cho SEND và RENTAL.
- Re-test old laundry lifecycle sau các thay đổi Phase 2 trong order service.
- Đồng nhất role name trên backend seed data, mobile, FE permissions và docs.
- Tạo demo data script ổn định cho deploy và local.
- Thêm automated tests cho:
  - SEND PIN hai giai đoạn.
  - RENTAL extend/end.
  - QR hợp lệ/không hợp lệ.
  - Maintenance claim/resolve.
  - Gateway RBAC.

### P2 - Service Đang Thiếu Source

- Build lại hoặc remove scope `laundry-service`.
- Build lại hoặc remove scope `partner-service`.
- Cập nhật FE partner/services pages theo quyết định cuối.

### P3 - Capstone / Advanced Scope

- Tablet-web locker UI.
- Tích hợp sensor thật.
- Drone service.
- Drone simulator.
- Battery-aware assignment.
- Realtime map tracking.
- AI/RAG support.
- CI/CD pipeline.
- Production monitoring/logging.

## Rủi Ro Và Lưu Ý Đã Biết

- Tài liệu cũ có thể vẫn mô tả assumption trước Phase 2. Ưu tiên file này và `BUSINESS_FLOWS_CURRENT.md`.
- Root workspace không phải git repo, nên root docs không được track cùng backend changes.
- Nhiều repo đang dirty. Không bulk reset hoặc checkout.
- Mobile `.env` đang modified local và có thể chứa base URL riêng theo environment.
- Dự án có cả flow courier/logistics legacy và flow locker ops mới; không được mặc định chúng là một quy trình.
- Việc thiếu `laundry-service` và `partner-service` có thể làm vỡ FE/API expectations cũ.
- Password/env PostgreSQL trên DigitalOcean dễ gây nhầm lẫn vì Docker env không rewrite password sau khi volume đã init.

## Nhật Ký Thay Đổi Gần Đây

| Ngày | Người thực hiện | Thay đổi | File / Khu vực | Verification |
|---|---|---|---|---|
| 2026-06-13 | Codex | Đổi 2 file sống chính sang tiếng Việt có dấu và sync artifact mirror. | `docs/BUSINESS_FLOWS_CURRENT.md`, `docs/PROJECT_PROGRESS_TRACKER.md`, `docs/project-artifacts/markdown-by-project/backend/docs/*`. | Kiểm tra heading tiếng Anh cũ, link/copy artifact, và private txt exclusion. |
| 2026-06-13 | Codex | Tạo living business flow và progress tracker docs. | `docs/BUSINESS_FLOWS_CURRENT.md`, `docs/PROJECT_PROGRESS_TRACKER.md`, artifact copies. | Link đã verify; markdown artifacts đã sync; private local txt không bị copy. |
| 2026-06-13 | Codex | Cập nhật tài liệu cũ theo trạng thái hiện tại. | Backend README/run guide, mobile README, IoT README, root docs, artifact copies. | Markdown synced; private env/pro/Application excluded from artifacts. |
| 2026-06-13 | Codex | Verify lỗi kết nối deployed DB và hướng dẫn reset password postgres. | DigitalOcean/PostgreSQL. | Service users OK; postgres reset verified trong container. |
| 2026-06-13 | Codex | Hoàn tất wiring Flutter quick action route và verification. | `home_page.dart`, locker ops, auth routing. | `flutter analyze`, debug APK build, customer smoke passed. |
| 2026-06-13 | Codex | Fix blocker FE production build. | Examples, loyalty API, order timeline. | `npm.cmd run build` passed. |
| 2026-06-13 | Previous agent/Codex continuation | Implement backend Locker Phase 2. | order/locker/iot/gateway/user services. | Backend E2E recorded trong `RUN_RESULT.md`. |

## Mẫu Cập Nhật Cho Lần Sau

Khi hoàn tất một feature, thêm một dòng như sau:

```markdown
| YYYY-MM-DD | Agent/User | Tóm tắt thay đổi ngắn | File/khu vực chạm tới | PASS/FAIL/PARTIAL kèm lệnh hoặc lý do |
```

Sau đó cập nhật:

- `Bảng Tiến Độ Theo Component`
- `Việc Còn Lại`
- `Nhật Ký Verification`
- `docs/BUSINESS_FLOWS_CURRENT.md` nếu hành vi nghiệp vụ thay đổi
- `RUN_RESULT.md` nếu kết quả chạy/lịch sử verification thay đổi
