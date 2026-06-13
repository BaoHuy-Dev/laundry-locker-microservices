# Theo Dõi Tiến Độ Dự Án

> Cập nhật lần cuối: 2026-06-14
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
6. `docs/project-artifacts/guides/LOCKER_FLOWS_STANDARD_SPEC.md` (phân tích luồng tủ as-is + đặc tả chuẩn thực tế + gap map + backlog).
7. `docs/ARCHITECTURE_DECISIONS.md` (ADR: K8s/Kafka/CQRS/GraphQL/mesh — quyết định hoãn + điều kiện xem lại).
8. File code liên quan đến task.

Không mặc định tài liệu cũ là đúng nếu chúng mâu thuẫn với các file trên.

## Trạng Thái Tổng Quan Hiện Tại

Dự án hiện có nền tảng locker/laundry đang chạy với backend Locker Phase 2, màn hình web admin cho locker/maintenance, và Flutter routing theo role cho các flow locker.

Tóm tắt trạng thái:

- Backend Locker Phase 1: đã xong và đã test.
- Backend Locker Phase 2: đã xong và đã test.
- Backend production hardening Phase 1: đã implement correlation ID, Prometheus metrics endpoint, OpenAPI docs runtime, security workflow, và deploy verify.
- Backend production hardening Phase 2: đã implement Resilience4j/timeout cho Feign, SBOM runtime/CI, Trivy container image scan gate, Testcontainers smoke cho locker, và đã verify Maven.
- Backend production hardening Phase 3/4: đã implement gateway RBAC/access-token tests, Swagger UI/OpenAPI aggregation qua gateway, build metadata, full 12-image Trivy matrix, deploy artifact checksum, và đã verify Maven.
- Backend production hardening Phase 4 continuation: đã thêm GitHub artifact attestations/provenance cho deploy artifact, tag-based backend release workflow, release SBOM/checksum, và script verify release artifact.
- Web admin locker/maintenance: đã implement và đã verify build.
- Flutter customer/manager/maintenance locker flows: đã implement, đã verify targeted build/analyze.
- IoT backend verify-access bằng PIN/QR: đã implement.
- Drone delivery/assignment/tracking thật: chưa implement.
- AI/RAG: chưa implement.
- `laundry-service`: thiếu source module.
- Role `PARTNER`/`partner-service`: đã gỡ khỏi backend (seed/role/permission/compose) vì không còn dùng.

## Đang Làm

| Ngày | Branch | Task | Phạm vi dự kiến sửa | Không sửa | Ảnh hưởng |
|---|---|---|---|---|---|
| 2026-06-14 | `docs/locker-flows-standard-spec` | Ghi chính thức 5 quyết định kiến trúc (K8s/Helm/GitOps, Kafka, CQRS/ES, GraphQL, service mesh) dưới dạng ADR — đều là "hoãn/chưa áp dụng now" kèm điều kiện xem lại + phác thảo cho CodeX. Chỉ tài liệu. | `docs/ARCHITECTURE_DECISIONS.md` (mới); pointer trong 2 file sống; mirror. | Không sửa code/compose/CI/migration; không deploy K8s/Kafka/mesh. Chỉ tài liệu governance. |
| 2026-06-14 | `docs/locker-flows-standard-spec` | Phân tích luồng tủ (as-is, bám code locker/order/iot) + đặc tả toàn bộ luồng nghiệp vụ tủ khóa chuẩn thực tế (to-be) + gap map + backlog. Tài liệu blueprint, chưa code. | `docs/project-artifacts/guides/LOCKER_FLOWS_STANDARD_SPEC.md` (mới), `docs/BUSINESS_FLOWS_CURRENT.md`, `docs/PROJECT_PROGRESS_TRACKER.md`, mirror artifact backend/docs. | Không sửa code Java/migration/business logic, không đổi API/DB/event/UI runtime, không stage `docker/postgres/init-databases.sql`, không commit file private/secret. | Chỉ tài liệu; không đổi hành vi runtime. Định hướng cho các giai đoạn implement L1–L7 sau. |
| 2026-06-13 | `chore/backend-production-phase1` | Production hardening Phase 1 đã implement, verify, commit và push trên branch riêng; chờ merge nếu cần. | Backend Maven modules, CI workflow, cấu hình runtime service, living docs. | Không sửa nghiệp vụ locker/order/payment hiện có, không sửa migration cũ đã chạy, không stage `docker/postgres/init-databases.sql` đang bị xoá local từ trước, không commit file private/secret. | Ảnh hưởng kỹ thuật vận hành backend; không đổi API nghiệp vụ/database/event/UI/mobile. |
| 2026-06-13 | `chore/backend-production-phase2` | Production hardening Phase 2 đã implement, verify, commit và push trên branch riêng; chờ merge nếu cần. | Backend Maven modules, service `application.yml`, GitHub Actions, docs. | Không sửa nghiệp vụ locker/order/payment hiện có, không sửa migration cũ đã chạy, không stage `docker/postgres/init-databases.sql` đang bị xoá local từ trước, không commit file private/secret. | Ảnh hưởng kỹ thuật vận hành/backend quality; không đổi API nghiệp vụ/database/event/UI/mobile. |
| 2026-06-13 | `chore/backend-production-phase3-4` | Production hardening Phase 3/4 đã implement, verify, commit và push trên branch riêng; chờ merge nếu cần. | `api-gateway` tests/config, root Maven config, GitHub Actions, docs/artifact mirrors. | Không sửa nghiệp vụ locker/order/payment hiện có, không sửa migration cũ đã chạy, không stage `docker/postgres/init-databases.sql` đang bị xoá local từ trước, không commit file private/secret. | Ảnh hưởng kỹ thuật vận hành/API docs/CI; không đổi API nghiệp vụ/database/event/UI/mobile. |
| 2026-06-13 | `chore/backend-production-phase4-provenance` | Production hardening Phase 4 continuation đã implement, verify, commit và push trên branch riêng; chờ merge nếu cần. | GitHub Actions release/deploy workflow, `scripts/`, living docs/artifact mirrors. | Không sửa nghiệp vụ locker/order/payment hiện có, không sửa migration cũ đã chạy, không stage `docker/postgres/init-databases.sql` đang bị xoá local từ trước, không commit file private/secret. | Ảnh hưởng CI/CD/release supply-chain; không đổi API nghiệp vụ/database/event/UI/mobile. |

## Bản Đồ Repository

| Khu vực | Đường dẫn | Branch quan sát gần nhất | Trạng thái |
|---|---|---|---|
| Backend | `laundry-locker-microservices/` | `chore/backend-production-phase4-provenance` | Source backend chính; Phase 4 provenance/release hardening đang ở branch riêng, có một xoá local ngoài scope tại `docker/postgres/init-databases.sql` không được stage. |
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
| Gateway routing/RBAC | DONE | Guard cho `/api/admin/**`, `/api/manage/**`, `/api/maintenance/**`, `/internal/**`; gateway chỉ authorize JWT `tokenUse=access`; có unit tests cho internal block, refresh-token reject, admin/manage/maintenance RBAC, public OpenAPI/catalog read. | Cập nhật route table khi thêm service; bổ sung end-to-end gateway tests khi có harness chạy đủ service. |
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
| Partner portal | Đã gỡ | Role `PARTNER`/permission `PARTNER_MANAGE`/account `partner.seed`/`partner-service` đã gỡ khỏi seed + docker-compose (2026-06-13). | FE partner routes là deprecated; dọn UI partner khi có thời gian. |
| Services/laundry catalog | PARTIAL | FE/admin docs tồn tại; backend `laundry-service` thiếu. | Build lại `laundry-service` hoặc route catalog qua service khác. |
| Payment/refund | PARTIAL | `payment-service` endpoints tồn tại; provider phụ thuộc environment; production profile đã fail-fast nếu còn config demo/sandbox/localhost. | Verify VNPay/MoMo với credential sandbox/real. |
| Notifications/FCM/WebSocket | PARTIAL | Service endpoints/events tồn tại; Firebase production chưa verify. | Verify FCM và WebSocket trên deploy. |
| Loyalty | PARTIAL | `loyalty-service` endpoints tồn tại. | Verify event integration và FE/mobile usage. |
| Store management | DONE | `store-service` endpoints và route admin/public. | Browser smoke admin stores nếu cần. |
| Flutter customer locker ops | DONE | Login/home quick actions/rental/send/my-orders smoke pass. UI 3 màn revamp về design system shadcn navy + action gate theo state machine (2026-06-14, `flutter analyze` 0 error). | Smoke trên emulator với deploy cho UI mới (create/confirm/complete/extend/delegate/report). |
| Flutter stores (customer) | DONE | Feature `lib/features/stores/**`: list (search + nearby), detail (info + ratings + directions), entry "Khám phá cửa hàng" từ home; gọi `/api/stores`, `/api/stores/{id}`, `/api/stores/{id}/ratings`. `flutter analyze` 0 error. | Manual smoke trên emulator với deploy có store data. |
| Flutter manager home | PARTIAL | Code và backend role login đã verify; manual UI smoke bị giới hạn. | Test manager UI trên emulator/device ổn định. |
| Flutter maintenance home | PARTIAL | Backend API `/api/maintenance/**` đã có (`locker-service`); role routing `MAINTENANCE -> /maintenance-home` đã verify (`homeForRoles` ở splash/login); `flutter analyze` 0 error. Manual UI smoke còn giới hạn. | Test maintenance UI với seeded fault trên emulator. |
| Flutter legacy courier/logistics | PARTIAL | Nhiều route tồn tại; chưa align backend hiện tại trong pass gần nhất. | Audit hoặc remove/mark legacy. |
| Python IoT runtime | PARTIAL | README/runtime tồn tại; hardware/simulation cần config. | Đồng bộ broker và test với verify-access. |
| Tablet-web cabinet UI | TODO | Mới được nhắc trong docs. | Build dựa trên layout + verify-access. |
| Real drone service | TODO | Thư mục drone có prototype docs/code, chưa có backend service. | Thiết kế `drone-service` và assignment model. |
| Battery-aware assignment | TODO | Gap capstone yêu cầu, chưa có code. | Implement sau khi có drone model. |
| Realtime drone tracking map | TODO | Chưa có map/telemetry service. | Implement bằng simulator và WebSocket. |
| AI/RAG support | TODO | Chưa có `ai-service`. | Chọn provider/storage rồi scaffold. |
| CI/CD | PARTIAL | Backend CI `mvn -B test`, backend security workflow Dependency Review/CodeQL, SBOM artifact, Trivy image scan gate cho 12 image có Dockerfile, build-info metadata, deploy workflow `mvn -B clean verify`, deploy artifact checksum + GitHub provenance attestation, tag-based release workflow, release SBOM/checksum, rollback script và verify-release script đã có. | Theo dõi CI trên remote; thêm SLSA policy enforcement, immutable release setting, blue-green/zero-downtime nếu cần production nghiêm ngặt hơn. |

## Chi Tiết Tiến Độ Backend

### Đã Xong

- Maven multi-module backend đã có.
- Database và schema theo service đã có.
- Gateway route table đã có.
- JWT identity propagation đã có.
- Gateway chặn `/internal/**` từ bên ngoài.
- Gateway chỉ cho token `tokenUse=access` truy cập API nghiệp vụ; refresh token chỉ dùng ở auth refresh flow.
- Secret policy dùng chung trong `common-lib` cho JWT/QR/payment production guard:
  - Không tự pad HMAC secret ngắn.
  - JWT/QR secret phải có tối thiểu 32 UTF-8 bytes.
  - Profile `prod`/`production` fail-fast nếu còn giá trị demo/dev/change-me/default/localhost/sandbox.
- GitHub Actions backend CI chạy `mvn -B test` cho PR/push vào các nhánh làm việc chính.
- Production hardening Phase 1 đã implement:
  - Gateway và servlet services gắn/forward `X-Correlation-Id`, trả lại header response, và đưa vào MDC log ở servlet services.
  - Tất cả app có actuator expose thêm `/actuator/metrics` và `/actuator/prometheus`; metrics có tag `application`.
  - Các servlet service và gateway có OpenAPI runtime endpoint `/v3/api-docs`, bật/tắt bằng `SPRINGDOC_API_DOCS_ENABLED`.
  - GitHub Actions có workflow security cho Dependency Review và CodeQL.
  - Deploy workflow build bằng `mvn -B clean verify`, không còn package với `-DskipTests`.
- Production hardening Phase 2 đã implement:
  - Các service dùng OpenFeign có `spring-cloud-starter-circuitbreaker-resilience4j`.
  - Bật OpenFeign circuit breaker theo env `SPRING_CLOUD_OPENFEIGN_CIRCUITBREAKER_ENABLED`, có group mode, default connect timeout `APP_FEIGN_CONNECT_TIMEOUT_MS`, read timeout `APP_FEIGN_READ_TIMEOUT_MS`.
  - Thêm default Resilience4j circuit breaker config bằng env `APP_RESILIENCE4J_CB_*`.
  - Actuator expose thêm `/actuator/sbom`; Maven build generate CycloneDX SBOM dưới `target/classes/META-INF/sbom/application.cdx.json`.
  - GitHub Actions security workflow generate SBOM artifact và scan container image bằng Trivy cho `api-gateway`, `auth-service`, `order-service`, `locker-service`, `payment-service`, `iot-service`.
  - Thêm Testcontainers smoke test cho `locker-service` để verify Flyway seed demo cabinet và mapping maintenance report trên PostgreSQL thật khi Docker khả dụng.
- Production hardening Phase 3/4 đã implement:
  - Gateway có Swagger UI aggregation, gồm `/swagger-ui/index.html` và các route `/v3/api-docs/<service>` cho 10 service servlet đang có source.
  - Gateway public filter cho phép OpenAPI/Swagger UI public, nhưng vẫn chặn API nghiệp vụ mutating nếu không có JWT.
  - Thêm `JwtGatewayFilterTest` cho các contract quan trọng: chặn `/internal/**`, reject refresh token ở business API, RBAC admin/manage/maintenance, forward identity headers, public OpenAPI/catalog GET.
  - Spring Boot Maven `build-info` chạy cho các app module để `/actuator/info` có metadata build khi actuator đọc `META-INF/build-info.properties`.
  - Backend security workflow mở rộng Trivy scan matrix từ 6 image lên 12 image có Dockerfile; không thêm `laundry-service`/`partner-service` vì thiếu source.
  - Deploy workflow tạo/upload SHA-256 checksum cho deploy artifact; deploy script verify checksum nếu file `.sha256` tồn tại.
  - Deploy script bỏ mặc định `LAUNDRY-SERVICE`/`PARTNER-SERVICE` khỏi expected Eureka apps, có thể override bằng env `EUREKA_EXPECTED_APPS`.
- Production hardening Phase 4 continuation đã implement:
  - Deploy workflow dùng GitHub artifact attestation cho deploy tarball và checksum.
  - Thêm `backend-release.yml`: khi push tag `v*`, workflow build/test bằng `mvn -B clean verify`, package release tarball, copy root CycloneDX SBOM, tạo checksum, attest provenance và publish GitHub Release.
  - Release workflow upload release artifacts với retention 30 ngày.
  - Thêm `scripts/verify-release-artifact.sh` để verify SHA-256 và, nếu có GitHub CLI, verify attestation bằng `gh attestation verify`.
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

- `docker-compose.yml` vẫn khai báo `laundry-service` đang thiếu; `docker-compose.override.yml` skip nó khi chạy local. `partner-service` đã được gỡ hẳn khỏi compose (2026-06-13).
- Một số docs cũ và FE pages vẫn giả định có service catalog/partner APIs.
- Seed data role hiện còn `USER`, `STAFF`, `ADMIN` (đã gỡ `PARTNER`), trong khi role routing mới dùng thêm `CUSTOMER`, `MANAGER`, `MAINTENANCE`.
- Password của deployed seed account không biết từ seed hash; có thể cần reset cho demo.

### Chưa Bắt Đầu

- Real drone backend service.
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
- Module `stores` mới (2026-06-13): màn danh sách cửa hàng (tìm kiếm + gần tôi), màn chi tiết cửa hàng (thông tin + đánh giá + chỉ đường), entry "Khám phá cửa hàng" trên home. `flutter analyze` 0 error.
- Xác minh `develop` đã là superset (đã merge `ThaiBinh_NewUI_v1` qua PR #1) nên không cần merge thủ công; UI Đội bảo trì (MAINTENANCE) đã có sẵn và wire route đầy đủ.

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
  - `admin.seed@laundry.test`
  - `customer.vip@laundry.test`
  - (`partner.seed@laundry.test` đã gỡ khỏi seed file 2026-06-13; deploy DB cũ có thể còn record cho tới khi re-seed.)

Quan trọng:

- `POSTGRES_PASSWORD` trong `docker inspect` không chắc là password DB hiện tại nếu volume PostgreSQL đã init trước đó.
- Nếu demo account cần password biết trước, reset `auth_schema.auth_accounts.password_hash` có chủ đích và document password demo đã chọn tại đây.
- Không commit private server notes.

## Nhật Ký Verification

| Ngày | Khu vực | Lệnh / Kiểm tra | Kết quả | Ghi chú |
|---|---|---|---|---|
| 2026-06-14 | Tài liệu | `git diff --check` cho `ARCHITECTURE_DECISIONS.md` + 2 file sống + mirror | PASS | ADR governance; docs-only, không có file private. |
| 2026-06-14 | Backend | `mvn -pl order-service -am test` trên branch `fix/locker-reservation-ttl-and-release` | PASS | BUILD SUCCESS; 8 test common-lib pass; order-service (gồm sửa auto-cancel/scheduler L1) compile sạch. Không cần Docker. |
| 2026-06-14 | Flutter | `flutter analyze lib/features/locker_ops` và `flutter analyze` toàn project | PASS | "No issues found" trên `locker_ops`; toàn project 0 error (413 info/warning debt cũ không đổi) sau revamp UI luồng tủ. |
| 2026-06-14 | Tài liệu | `git diff --check` trên branch `docs/locker-flows-standard-spec` + quét private-file (`env.txt`/`pro.txt`/`Application.txt`/`Host *.txt`) trong `docs/project-artifacts` | PASS | Chỉ thêm/sửa Markdown (spec luồng tủ + 2 file sống + mirror); không có file private bị copy vào artifacts. |
| 2026-06-13 | Flutter | `flutter analyze lib/features/stores lib/core/routing/app_router.dart lib/features/home/...` | PASS | "No issues found"; 0 lỗi trên feature stores mới + file wiring. |
| 2026-06-13 | Flutter | `flutter analyze` toàn project | PASS/PARTIAL | 0 lỗi mức error; 413 issue còn lại là info/warning debt cũ của codebase migrate (gồm `test/widget_test.dart`). |
| 2026-06-13 | Backend/Seed | `git diff --check` sau khi bỏ role `PARTNER` khỏi seed/compose | PASS | Không lỗi whitespace; seed SQL giữ cấu trúc/comma hợp lệ (`USER/STAFF/ADMIN`, permission `1001-1007`); chỉ còn cột `partner_id` ở store là schema cố ý giữ. Không đụng Java/migration. |
| 2026-06-13 | Backend | `mvn -pl api-gateway -am test` | PASS | Phase 4 continuation không đổi Java logic; targeted gateway/common tests vẫn pass: 8 common-lib tests, 2 correlation gateway tests, 7 JWT/RBAC gateway tests. |
| 2026-06-13 | Release scripts | `C:\Program Files\Git\bin\bash.exe -n scripts/deploy-from-artifact.sh` và `... verify-release-artifact.sh` | PASS | Shell syntax check pass cho deploy script và script verify release artifact. |
| 2026-06-13 | Git/Docs | `git diff --check` và quét private-file trong `docs/project-artifacts` | PASS | Không phát hiện whitespace/error; không có `env.txt`, `pro.txt`, `Application.txt`, `Host *.txt` trong artifacts. |
| 2026-06-13 | Backend | `mvn -B clean verify` | PASS/PARTIAL | 14 module backend clean/test/package/verify pass sau Phase 3/4; build-info/repackage pass; Testcontainers locker smoke skip 2 test vì Docker local không khả dụng. |
| 2026-06-13 | Backend | `mvn -B test` | PASS/PARTIAL | 14 module backend build/test pass sau Phase 3/4; `JwtGatewayFilterTest` 7 tests pass; Testcontainers locker smoke skip 2 test do Docker local không khả dụng. |
| 2026-06-13 | Backend | `mvn -pl api-gateway -am test` | PASS | Targeted gateway/common tests pass: 8 common-lib tests, 2 correlation gateway tests, 7 JWT/RBAC gateway tests. |
| 2026-06-13 | Deploy script | `C:\Program Files\Git\bin\bash.exe -n scripts/deploy-from-artifact.sh` | PASS | Shell syntax check pass bằng Git Bash; WSL `bash -n` không chạy được vì môi trường này không có `/bin/bash`. |
| 2026-06-13 | Backend | `mvn -B clean verify` | PASS/PARTIAL | 14 module backend clean/test/package/verify pass sau Phase 2; Testcontainers locker smoke được phát hiện nhưng skip 2 test vì máy local không có Docker environment, khi CI/server có Docker sẽ chạy PostgreSQL container thật. |
| 2026-06-13 | Backend | `mvn -B test` | PASS/PARTIAL | 14 module backend build/test pass sau Phase 2; CycloneDX SBOM generate cho các module; Testcontainers locker smoke skip 2 test do Docker local không khả dụng. |
| 2026-06-13 | Backend | `mvn -pl locker-service -am test` | PASS/PARTIAL | Targeted test cho locker-service pass; Testcontainers PostgreSQL smoke được cấu hình `disabledWithoutDocker=true` nên skip trên máy không có Docker daemon. |
| 2026-06-13 | Git/Docs | `git diff --check` | PASS | Kiểm tra sau khi cập nhật Phase 2, không phát hiện whitespace/error. |
| 2026-06-13 | Backend | `mvn -B clean verify` | PASS | 14 module backend clean/test/package/verify pass với dependency OpenAPI/Prometheus và filter correlation ID mới. |
| 2026-06-13 | Backend | `mvn test` | PASS | 14 module backend build/test pass sau production hardening Phase 1. |
| 2026-06-13 | Backend | `mvn -pl common-lib,api-gateway -am test` | PASS | Targeted test cho `CorrelationIdFilter`, `CorrelationIdGatewayFilter`, và `SecuritySecrets`. |
| 2026-06-13 | Backend | `mvn test` tại `laundry-locker-microservices` | PASS | 14 module backend build/test pass; thêm 6 unit tests cho `SecuritySecrets`. |
| 2026-06-13 | Backend | `mvn -pl common-lib,api-gateway,auth-service,order-service,payment-service -am test` | PASS | Targeted test cho các module hardening bảo mật. |
| 2026-06-13 | Git/Docs | `git diff --check` | PASS | Không phát hiện whitespace/error trong diff hiện tại. |
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

- **Luồng tủ — backlog từ `LOCKER_FLOWS_STANDARD_SPEC.md`** (xem gap map G1–G16, lộ trình L1–L7):
  - L1 (ưu tiên cao nhất): vá auto-cancel `@Scheduled` + release ô khi hủy (G1/G2), thêm TTL cho ô `RESERVED`, đối soát drift trạng thái ô↔order (G4).
  - L2: cell `EXPIRED`/move-to-storage khi quá hạn (G3), enforce `size` khi reserve (G9).
  - L3: luồng nhận hàng courier `PARCEL_RECEIVE` + courier access code (G6).
  - L4+: gate thanh toán (G5), access-log từng lần mở ô (G11), SMS/email OTP thật (G13), PIN brute-force lockout (G14).
- Thêm/verify payment UX cho SEND và RENTAL.
- Re-test old laundry lifecycle sau các thay đổi Phase 2 trong order service.
- Đồng nhất role name trên backend seed data, mobile, FE permissions và docs.
- Tạo demo data script ổn định cho deploy và local.
- Hoàn thiện phần còn lại sau production hardening Phase 3/4:
  - Mở rộng Testcontainers integration tests sang RabbitMQ và các flow order/locker/payment quan trọng.
  - Thêm end-to-end gateway tests khi có harness chạy đủ service thật.
  - Theo dõi runtime Swagger UI aggregation trên deploy sau khi service đăng ký Eureka.
- Thêm automated tests cho:
  - SEND PIN hai giai đoạn.
  - RENTAL extend/end.
  - QR hợp lệ/không hợp lệ.
  - Maintenance claim/resolve.
  - Gateway RBAC.

### P2 - Service Đang Thiếu Source

- Build lại hoặc remove scope `laundry-service`.
- `partner-service`: đã remove scope (gỡ role/seed/compose 2026-06-13). Còn lại: dọn FE partner pages deprecated khi có thời gian.
- Cập nhật FE services pages theo quyết định cuối.

### P3 - Capstone / Advanced Scope

- Tablet-web locker UI.
- Tích hợp sensor thật.
- Drone service.
- Drone simulator.
- Battery-aware assignment.
- Realtime map tracking.
- AI/RAG support.
- Dựng dashboard/alert thực tế cho Prometheus/Grafana/Loki hoặc stack observability được chọn.
- CI/CD release pipeline nâng cao: SLSA policy enforcement, immutable release setting trên GitHub, rollback có kiểm chứng sâu hơn, blue-green/zero-downtime nếu cần production nghiêm ngặt hơn.

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
| 2026-06-14 | Claude | Thêm `docs/ARCHITECTURE_DECISIONS.md` (ADR-001..005): hoãn/không-áp-dụng-now K8s/Helm/GitOps, Kafka (giữ RabbitMQ), CQRS/Event Sourcing, GraphQL (giữ REST), service mesh — mỗi ADR có bối cảnh, lý do, phương án nhẹ đang dùng, **điều kiện xem lại**, và phác thảo khi áp dụng (handoff CodeX). Pointer trong 2 file sống. | `docs/ARCHITECTURE_DECISIONS.md`; `docs/BUSINESS_FLOWS_CURRENT.md`; `docs/PROJECT_PROGRESS_TRACKER.md`; mirror. | `git diff --check` PASS; docs-only, không đụng code/CI/compose. |
| 2026-06-14 | Claude | Backend L1 (luồng tủ): vá lỗ hổng ô kẹt `RESERVED` (G1/G2). `OrderService.autoCancelUnconfirmedOrders` giờ release ô + transition đầy đủ (history/event/notify); thêm `OrderScheduler.sweepUnconfirmedReservations` `@Scheduled` (cron mặc định mỗi 15 phút); cửa sổ giữ chỗ cấu hình `app.order.auto-cancel-hours` (24h). | `laundry-locker-microservices` branch `fix/locker-reservation-ttl-and-release`: `order-service/.../service/OrderService.java`, `OrderScheduler.java`; living docs. | `mvn -pl order-service -am test` BUILD SUCCESS (8 test common-lib pass; order-service compile sạch). |
| 2026-06-14 | Claude | Mobile: revamp UI 3 màn locker customer (Gửi hàng/Thuê tủ/Đơn tủ của tôi) về design system shadcn navy; thêm design kit dùng chung (`ops_widgets`, `locker_picker`); format giá/ngày/countdown; gate action theo trạng thái+loại (confirm/complete/extend/end/delegate/report/cancel). Bắt đầu lộ trình luồng tủ phần mobile. | `smart-laundry-locker-mobile` branch `feat/locker-customer-ui-revamp`: `lib/features/locker_ops/presentation/{widgets/ops_widgets.dart,widgets/locker_picker.dart,pages/send_parcel_page.dart,pages/rent_locker_page.dart,pages/my_locker_orders_page.dart}`; living docs (backend repo). | `flutter analyze` PASS (0 error; 413 info/warning debt cũ không đổi). |
| 2026-06-14 | Claude | Phân tích luồng tủ as-is (đọc trực tiếp `LockerService`, `OrderService`, `IotService`, entities, migrations, scheduler, `QrTokenService`) và đặc tả toàn bộ luồng nghiệp vụ tủ khóa chuẩn thực tế (to-be) + 16 gap (G1–G16) + backlog L1–L7. Phát hiện lỗ hổng: auto-cancel không @Scheduled & không release ô (G1/G2), quá hạn không giải phóng ô (G3), thiếu luồng nhận hàng courier PARCEL_RECEIVE (G6). Chỉ tài liệu, không đụng code. | `docs/project-artifacts/guides/LOCKER_FLOWS_STANDARD_SPEC.md` (mới); `docs/BUSINESS_FLOWS_CURRENT.md`; `docs/PROJECT_PROGRESS_TRACKER.md`; mirror `docs/project-artifacts/markdown-by-project/backend/docs/*`. | `git diff --check` PASS; quét private-file trong artifacts PASS. |
| 2026-06-13 | Claude | Mobile: thêm feature `stores` (list + detail + ratings) cho customer + entry "Khám phá cửa hàng" trên home; xác minh `develop` đã merge `ThaiBinh_NewUI_v1` (PR #1) nên không cần merge thủ công; xác minh UI Đội bảo trì (MAINTENANCE) đã wire (`homeForRoles`). | `smart-laundry-locker-mobile`: `lib/features/stores/**`, `lib/core/routing/app_router.dart`, `lib/features/home/presentation/pages/home_page.dart`; living docs (backend repo). | `flutter analyze` targeted PASS (0 issue) + full PASS (0 error). |
| 2026-06-13 | Claude | Bỏ role `PARTNER` khỏi backend mức shallow: gỡ role/permission `PARTNER_MANAGE`/account `partner.seed`/seed `partner_db` khỏi seed-demo-data, gỡ check partner trong verify-demo-data, gỡ `partner-service` khỏi docker-compose + override. Giữ schema `partner_db`/`partner_schema`/`partner_id` cũ (không đụng migration). | `docker/postgres/seed-demo-data.sql`, `docker/postgres/verify-demo-data.sql`, `docker-compose.yml`, `docker-compose.override.yml`, `docs/BUSINESS_FLOWS_CURRENT.md`, `docs/PROJECT_PROGRESS_TRACKER.md`. | `git diff --check` PASS; review seed SQL comma/structure PASS; grep Java `PARTNER` = no match. |
| 2026-06-13 | Codex | Production hardening Phase 4 continuation: thêm GitHub artifact attestations cho deploy artifact, tag-based backend release workflow tạo tarball/SBOM/checksum/provenance và publish GitHub Release, thêm script verify release artifact. | `.github/workflows/deploy-droplet.yml`, `.github/workflows/backend-release.yml`, `scripts/verify-release-artifact.sh`, living docs. | `mvn -pl api-gateway -am test` PASS; Git Bash `bash -n` cho deploy/verify scripts PASS; `git diff --check` PASS. |
| 2026-06-13 | Codex | Production hardening Phase 3/4: thêm gateway RBAC/access-token unit tests, Swagger UI/OpenAPI aggregation qua gateway, build-info metadata, full 12-image Trivy matrix, deploy artifact checksum và checksum verify trong deploy script. | `api-gateway`, root `pom.xml`, `.github/workflows/backend-security.yml`, `.github/workflows/deploy-droplet.yml`, `scripts/deploy-from-artifact.sh`, living docs. | `mvn -pl api-gateway -am test` PASS; `mvn -B test` PASS/PARTIAL; `mvn -B clean verify` PASS/PARTIAL; Git Bash `bash -n scripts/deploy-from-artifact.sh` PASS. |
| 2026-06-13 | Codex | Production hardening Phase 2: bật Resilience4j circuit breaker/timeout cho OpenFeign, expose `/actuator/sbom`, generate CycloneDX SBOM, thêm Testcontainers smoke cho locker-service, và thêm SBOM/Trivy container scan gate trong backend security workflow. | Root `pom.xml`, service `pom.xml`/`application.yml`, `locker-service/src/test/**`, `.github/workflows/backend-security.yml`, living docs. | `mvn -pl locker-service -am test` PASS/PARTIAL; `mvn -B test` PASS/PARTIAL; `mvn -B clean verify` PASS/PARTIAL; Docker local không khả dụng nên Testcontainers skip 2 smoke tests. |
| 2026-06-13 | Codex | Production hardening Phase 1: correlation ID xuyên gateway/service, Prometheus metrics endpoint, OpenAPI runtime docs, backend security workflow, deploy bắt buộc `clean verify`. | `common-lib`, `api-gateway`, service `pom.xml`/`application.yml`, `.github/workflows/*`, living docs. | `mvn -pl common-lib,api-gateway -am test` PASS; `mvn test` PASS; `mvn -B clean verify` PASS. |
| 2026-06-13 | Codex | Hardening backend nền tảng: secret policy dùng chung, chặn refresh token ở gateway, guard config payment production, thêm backend CI. | `common-lib`, `api-gateway`, `auth-service`, `order-service`, `payment-service`, `.github/workflows/backend-ci.yml`. | `mvn test` PASS toàn bộ backend; `git diff --check` PASS. |
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
