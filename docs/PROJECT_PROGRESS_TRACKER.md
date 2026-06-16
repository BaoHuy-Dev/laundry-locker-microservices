# Theo Dõi Tiến Độ Dự Án

> Cập nhật lần cuối: 2026-06-16
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
- Mobile mở tủ qua IoT (`/api/iot/unlock`): đã implement, mô phỏng bằng script Python độc lập `smart-locker-iot/simulate_demo_cabinet.py` (chưa có hardware thật).
- Reorder ("đặt lại đơn") cho SEND/RENTAL: đã sửa đúng (trước đó tạo đơn không có ô/giá sai), đã wire mobile.
- Notification khi maintenance claim/resolve báo cáo lỗi tủ: đã implement (event mới `locker.report.claimed/resolved`), mobile có màn "Báo cáo của tôi".
- Drone delivery/assignment/tracking thật: chưa implement.
- AI/RAG: chưa implement.
- `laundry-service`: thiếu source module.
- Role `PARTNER`/`partner-service`: đã gỡ khỏi backend (seed/role/permission/compose) vì không còn dùng.

## Đang Làm

| Ngày | Branch | Task | Phạm vi dự kiến sửa | Không sửa | Ảnh hưởng |
|---|---|---|---|---|---|
| 2026-06-16 | `feat/maintenance-iot-sync-hardening` (3 repo: backend, mobile, web FE) | Audit + đóng backlog P0/P1/P2 từ phân tích "IoT↔User↔Maintenance còn thiếu/chưa đồng bộ": force-open thật + audit log mở ô, PIN/QR brute-force lockout, TTL backstop cho ô RESERVED, contact info khách trên report, rating report sau khi RESOLVED, web admin parity (lifecycle actions đã có ở `layout-view.tsx`, bổ sung thêm lịch bảo trì định kỳ), enforce size khi reserve, device health dashboard. | BE: `iot-service` (migration V2, `BoxAccessLog`/`AccessAttempt`, force-unlock, device-status list), `locker-service` (migration V8/V9, `IotClient`/`UserClient`, force-open, TTL sweep, size fallback, report rating), `common-lib` không đổi, `api-gateway` (+1 path predicate). Mobile: `locker_ops_service.dart`, `maintenance_home_page.dart`, `my_reports_page.dart`, `send_parcel_page.dart`. Web FE: `lockerOps.ts`, `layout-view.tsx`, `BoxSettingModal.tsx`, `Admin/maintenance/index.tsx`. | Bỏ qua P2 #9 (sync trạng thái ô→MQTT, chờ hardware thật) và P2 #10 (payment gating, giữ miễn phí cho demo) theo quyết định user; không đụng `smart-locker-iot` (force-open dùng lại đúng cơ chế unlock/simulator đã có); không sửa `docker/postgres/init-databases.sql`. | Endpoint mới `/api/maintenance/boxes/{id}/force-open`, `/api/lockers/reports/{id}/rate`, `/api/manage/iot/device-status`, `/internal/iot/force-unlock`; migration mới (không sửa cũ); UI mobile + web admin + gateway route. |
| 2026-06-16 | `feat/booking-iot-reorder-maintenance-loop` (3 repo: backend, mobile, `smart-locker-iot`) | Đồng bộ luồng booking tủ từ mobile: (1) mô phỏng kết nối IoT↔mobile để mở tủ (vì hardware thật chưa có); (2) bổ sung "đặt lại đơn" (reorder) hoạt động đúng cho SEND/RENTAL; (3) customer↔maintenance "qua lại" qua notification khi claim/resolve report + màn "Báo cáo của tôi". | BE: `order-service/.../OrderService.java` (`reorder()`), `common-lib/.../DomainEventNames.java`, `locker-service/.../LockerService.java`, `notification-service/.../NotificationService.java`. Mobile: `locker_ops_service.dart`, `my_locker_orders_page.dart`, `my_reports_page.dart` (mới), `app_router.dart`. IoT: `simulate_demo_cabinet.py` (mới, file độc lập), `README.md`. | Không sửa `main.py`/`serial_manager.py`/`setup_handler.py`/hardware-track của `smart-locker-iot` (track thật của người khác); không sửa migration cũ; không đụng `report_list_page.dart`/`create_report_page.dart` legacy; không expose `/internal/**`. | Ảnh hưởng API `/api/orders/{id}/reorder` (sửa logic), event mới `locker.report.claimed/resolved`, notification consumer, mobile UI luồng tủ (mở tủ/đặt lại đơn/báo cáo của tôi); không đổi migration/DB schema. |
| 2026-06-15 | `chore/full-demo-seed` | Tạo seed demo đầy đủ (idempotent) cho toàn bộ MS DB: 4 tài khoản đặt sẵn (admin/customer/maintenance/manager, pw `12345678`) + 100 bản ghi/bảng nghiệp vụ, để test luồng A→Z. | Thêm `scripts/seed-full-demo-ms.sql`; cập nhật living docs + mirror. | Không sửa migration/schema, không sửa code service, không đụng `docker/postgres/init-databases.sql`. | Chỉ thêm DỮ LIỆU demo (dải id ≥9001/≥90001, marker `*-DEMO-*`); không đổi schema/API/UI. Áp lên DB đã chạy (local PASS; droplet do user tự chạy vì agent bị chặn ghi prod DB). |
| 2026-06-15 | `fix/mobile-token-refresh-and-startup` (repo `smart-laundry-locker-mobile`) | Khắc phục lỗi/cảnh báo khi `flutter run`: (1) 401 token hết hạn → app logout ngay; (2) Firebase duplicate-app nuốt lỗi làm bỏ qua messaging init; (3) build chậm/giật do Gradle heap 8G trên máy 16GB chạy kèm Docker+emulator. | `lib/core/network/auth_interceptor.dart` (refresh-on-401), `lib/main.dart` (guard Firebase init), `android/gradle.properties` (heap 8G→3G + parallel/caching). | Không sửa backend/contract API, không đổi UI flow nghiệp vụ, không migrate KGP (chỉ là cảnh báo forward-compat), không đụng emulator/hardware. | Ảnh hưởng vòng đời phiên đăng nhập mobile (tự refresh thay vì đá ra) + khởi tạo Firebase messaging + tốc độ build local; backend/DB/web không đổi. |
| 2026-06-15 | FE `fix/admin-login-2fa-response-normalization` + BE `fix/gateway-cors-globalcors-prefix` | Sửa crash trang `/admin/dashboard` (`Cannot read properties of undefined (reading 'toString')`): dashboard đọc 11 field nhưng backend overview chỉ trả `totalOrders`+`byStatus`. | FE `fe/src/pages/Admin/dashboard/hooks/useDashboard.ts` (normalize overview đủ field, default 0); BE `order-service/.../OrderService.statistics()` (thêm `ordersToday/pendingOrders/totalRevenue/revenueToday`). | Không gọi chéo service trong order-service (cross-service KPI vẫn để FE default 0), không sửa migration/DB, không đổi route/RBAC. | Ảnh hưởng response `/api/admin/dashboard/overview` (thêm key, backward-compatible) + render dashboard web; DB/event/mobile không đổi. |
| 2026-06-15 | `fix/admin-login-2fa-response-normalization` (repo `laundry-locker-frontend`) | Sửa crash sau khi nhập OTP admin trên web: `Cannot read properties of undefined (reading 'id')` do FE đọc `data.user.id` trong khi backend trả payload phẳng (không có key `user`). | `laundry-locker-frontend/fe/src/context/auth-context.tsx` (chuẩn hoá response phẳng của `verify-2fa`, giữ email bước 1). | Không sửa backend/DTO `verify-2fa`, không đổi route/RBAC, không đụng `.env`/`vite.config.ts` (config local của user), không đổi luồng partner OTP. | Chỉ ảnh hưởng chuẩn hoá response phía web admin login; API contract backend không đổi. |
| 2026-06-15 | `fix/gateway-cors-globalcors-prefix` | Sửa lỗi web admin không đăng nhập được: preflight CORS bị chặn (`No 'Access-Control-Allow-Origin'`) do `globalcors` đặt sai prefix YAML của Spring Cloud Gateway 4.3.x. | `api-gateway/src/main/resources/application.yml` (chuyển `globalcors` về `spring.cloud.gateway.server.webflux.globalcors`, thêm method `PATCH`). | Không sửa route/RBAC/JWT filter, không đổi service contract/DB/migration/event, không đụng code FE/mobile. | Chỉ ảnh hưởng cấu hình CORS của gateway cho web FE `http://localhost:3000`; API contract/DB/UI không đổi. |
| 2026-06-14 | `fix/notification-runtime-smoke` | Hoàn tất phần còn dang dở của realtime notification: tách/kiểm thử STOMP JWT principal, bổ sung smoke verification có thể chạy lại khi môi trường ổn định, và cập nhật trạng thái rủi ro. | Dự kiến chạm `notification-service/src/main/java/**`, `notification-service/src/test/**`, có thể thêm `scripts/**`; cập nhật 2 file sống + mirror. | Không sửa migration cũ, không commit secret/mobile `.env`, không expose `/internal/**`, không đổi FE/mobile UI flow. | Ảnh hưởng verification/WebSocket notification; REST API/database/mobile UI không đổi. |
| 2026-06-14 | `fix/realtime-notification-smoke` | Xử lý rủi ro sau merge: harden realtime notification end-to-end, thêm/sửa client WebSocket nếu khả thi, và bổ sung smoke verification cho notification/maintenance directions. | Dự kiến chạm `notification-service`, `smart-laundry-locker-mobile/lib/features/notifications/**`, có thể thêm test/smoke script/docs; cập nhật 2 file sống + mirror. | Không sửa migration cũ, không commit secret/mobile `.env`, không expose `/internal/**`, không đụng partner/laundry-service legacy. | Ảnh hưởng API realtime notification/WebSocket/mobile notification; FCM push thật vẫn phụ thuộc Firebase credential/thiết bị deploy. |
| 2026-06-14 | `feat/maintenance-user-realtime-flows` | Triển khai vertical slice nâng cấp trải nghiệm role theo hướng production: ưu tiên Maintenance chi tiết trên mobile/web, bổ sung đường đi tới tủ/lỗi cho user/maintenance, và khảo sát/nối realtime chat/thông báo nếu backend hiện có cho phép. | Dự kiến chạm `smart-laundry-locker-mobile/lib/features/**`, `laundry-locker-frontend/fe/src/**`, có thể chạm `notification-service`/docs nếu cần API realtime; cập nhật living docs và mirror. | Không sửa migration cũ đã chạy, không đụng `laundry-service`/`partner-service` legacy, không đổi deploy infra/secret/local env, không expose `/internal/**` qua gateway. | Có thể ảnh hưởng mobile UI, web admin/ops UI, API/notification/WebSocket; chỉ thêm migration mới nếu thật sự cần đổi DB. |
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
| Gateway routing/RBAC | DONE | Guard cho `/api/admin/**`, `/api/manage/**`, `/api/maintenance/**`, `/internal/**`; gateway chỉ authorize JWT `tokenUse=access`; có unit tests cho internal block, refresh-token reject, admin/manage/maintenance RBAC, public OpenAPI/catalog read. CORS cho web FE (`http://localhost:3000`) cấu hình tại `spring.cloud.gateway.server.webflux.globalcors` (fix 2026-06-15, trước đó đặt sai prefix `deprecated` nên web login fail). | Cập nhật route table khi thêm service; bổ sung end-to-end gateway tests khi có harness chạy đủ service. |
| Locker physical cell model | DONE | V2/V3 migrations, `DRONE/STANDARD/XL`, layout endpoint. | Chỉ thêm cell status `EXPIRED` nếu product cần. |
| Locker fault/maintenance flow | DONE | V4 migration, API claim/resolve report, FE/mobile pages. **2026-06-16 (sáng)**: customer giờ được notify (event `locker.report.claimed/resolved`) khi maintenance claim/resolve báo cáo của mình; mobile có màn "Báo cáo của tôi" (`GET /api/lockers/my-reports`). **2026-06-16 (chiều)**: report giờ có `reporterName`/`reporterPhone` (tra qua `user-service`, best-effort) cho maintenance xem; customer đánh giá report sau khi RESOLVED (`POST/GET /api/lockers/reports/{id}/rate|rating`), maintenance xem điểm trung bình (`GET /api/maintenance/my-rating-average`). | Chưa expose `repair_logs` (work-log nội bộ KTV) cho customer; chưa có ảnh đính kèm work-log; chưa có role TECHNICIAN riêng. |
| Audit log mở ô (access log) | DONE | **2026-06-16**: bảng mới `iot_schema.box_access_logs` (`iot-service` V2) ghi mọi lần mở ô (actor, credential PIN_OR_QR/MASTER, kết quả) từ cả `unlock()` (khách) và `forceUnlock()` (maintenance). | Chưa có UI xem lại log (chỉ ghi DB); cân nhắc thêm trang admin xem audit trail nếu cần. |
| PIN/QR brute-force lockout | DONE | **2026-06-16**: bảng `iot_schema.access_attempts` (`iot-service` V2); `verifyAccess()` khóa box tạm (mặc định 15 phút sau 5 lần sai, cấu hình `app.iot.lockout.*`). | Chưa có cảnh báo/notify khi 1 box bị khóa nhiều lần (dấu hiệu tấn công). |
| Force-open khẩn cấp (maintenance) | DONE | **2026-06-16**: `POST /api/maintenance/boxes/{id}/force-open` (locker-service) → Feign `IotClient` → `POST /internal/iot/force-unlock` (iot-service, không cần PIN khách, luôn ghi audit log MASTER). Web admin: nút "Mở khẩn cấp" ở `layout-view.tsx` (mọi trạng thái ô) + đã nối `BoxSettingModal.tsx` (trước đó gọi route chết `/api/admin/lockers/boxes/{id}/force-open`). Mobile: action "Mở tủ khẩn cấp" trong bottom-sheet hành động ô (mọi trạng thái). | Trang `Admin/lockers/detail.tsx` + `BoxForceOpenModal.tsx` vẫn là code chết (không route tới, chỉ giả lập toast) — không sửa, không xóa, chỉ ghi nhận. |
| Device health dashboard | DONE | **2026-06-16**: `GET /api/manage/iot/device-status` (role MANAGER/ADMIN, route gateway `/api/manage/iot/**`) liệt kê toàn bộ `DeviceStatus` (trước đó có ghi nhưng không endpoint đọc, không UI). Web admin: section "Sức khỏe thiết bị" trong `Admin/maintenance/index.tsx` (badge online/offline + lastSeenAt). | Chưa tiêu thụ event `iot.device.status.changed` cho push realtime (đang là poll qua REST); chưa có UI mobile. |
| Cell-level RESERVED TTL backstop | DONE | **2026-06-16**: cột `reserved_until` (`locker-service` V8, mặc định 24h = cùng cửa sổ `app.order.auto-cancel-hours`), job `LockerScheduler.sweepExpiredReservations` (cron mỗi giờ) chỉ là backstop — order-service's sweep (mỗi 15 phút) đã release ô khi auto-cancel đơn nên đường này hiếm khi cần kích hoạt. | Không lưu `reserved_order_id` (box được reserve trước khi order có id — xem ghi chú trong migration V8); nếu cần đối soát chặt hơn, phải đổi thứ tự tạo order trong `OrderService.create()`. |
| Enforce size khi reserve | DONE | **2026-06-16**: `findAvailableBox` giờ fallback sang size lớn hơn theo thứ tự SMALL→MEDIUM→LARGE→XL khi không có ô đúng size (trước đó throw `NO_AVAILABLE_BOX` ngay). Mobile SEND có chọn kích thước (SMALL/MEDIUM/LARGE). | Demo seed (`V3__seed_demo_cabinet.sql`) chỉ có size MEDIUM/XL nên đường fallback chưa có dữ liệu thật để minh họa SMALL/LARGE; không sửa seed cho việc này. |
| SEND parcel flow | DONE | `POST /api/orders/send`, PIN hai giai đoạn, QR, receiver flow. **2026-06-16**: `reorder()` sửa để tái tạo đúng đơn SEND (gọi lại `createSend`) thay vì tạo đơn không có ô; mobile có nút "Đặt lại đơn" + "Mở tủ" (gọi `/api/iot/unlock`) trong chi tiết đơn. | Thêm payment UX nếu SEND là tính năng có phí. |
| RENTAL flow | DONE | `POST /api/orders/rental`, extend/end, tính giá theo giờ. **2026-06-16**: `reorder()` sửa để tái tạo đúng đơn RENTAL (gọi lại `createRental` với `cellType`/`hours` suy ra từ đơn cũ) thay vì sai giá/không có hạn thuê. | Nối provider payment nếu yêu cầu. |
| PIN/QR verify-access | DONE | `QrTokenService`, `POST /api/iot/verify-access`. **2026-06-16**: mobile gọi trực tiếp `POST /api/iot/unlock` (nút "Mở tủ" trong chi tiết đơn) — mô phỏng bằng `smart-locker-iot/simulate_demo_cabinet.py` vì chưa có hardware thật; đã verify round-trip MQTT thật qua `broker.hivemq.com`. | Build tablet-web/cabinet UX; thay simulator bằng `main.py` + setup handshake thật khi có Raspberry Pi/Arduino. |
| Laundry order legacy flow | PARTIAL | Core endpoints tồn tại và smoke cũ pass; chưa regression đầy đủ gần nhất. | Re-test full lifecycle sau mỗi thay đổi `order-service`. |
| Staff operations | PARTIAL | Staff endpoints tồn tại; không phải path mobile/FE chính hiện tại. | Quyết định STAFF có tách với MANAGER không. |
| Admin web dashboard | PARTIAL | Main admin routes tồn tại; build pass. Đăng nhập admin (CORS + 2FA + dashboard render) đã thông sau fix 2026-06-15: `/admin/dashboard` không còn crash; KPI đơn/doanh thu lấy thật từ `order-service`. | KPI cross-service (users/stores/lockers/boxes/services) đang hiển thị 0 — cần wire aggregation chéo service cho `/api/admin/dashboard/overview`. Browser smoke các trang admin còn lại. |
| Admin locker list/layout | DONE | `/admin/lockers`, `/admin/lockers/:lockerId`, RTK lockerOps. | Giữ UI khớp `LockerLayoutResponse`. |
| Admin maintenance page | DONE | `/admin/maintenance`, action report/fault; hiển thị tổng quan backlog, địa chỉ/toạ độ locker và nút chỉ đường cho fault/report. **2026-06-16**: thêm contact khách (SĐT) trên report, section "Bảo trì định kỳ" (tạo/đã-kiểm-tra/xóa lịch — trước đó chỉ mobile có) và section "Sức khỏe thiết bị". Action lifecycle ô (out-of-service/cleaning/return-to-service/force-open) đã có sẵn ở `/admin/lockers/:lockerId` (`layout-view.tsx`) từ trước, không phải thiếu như audit ban đầu tưởng — chỉ thiếu force-open thật (đã nối). | Browser smoke với admin token thật trên deploy. |
| Partner portal | Đã gỡ | Role `PARTNER`/permission `PARTNER_MANAGE`/account `partner.seed`/`partner-service` đã gỡ khỏi seed + docker-compose (2026-06-13). | FE partner routes là deprecated; dọn UI partner khi có thời gian. |
| Services/laundry catalog | PARTIAL | FE/admin docs tồn tại; backend `laundry-service` thiếu. | Build lại `laundry-service` hoặc route catalog qua service khác. |
| Payment/refund | PARTIAL | `payment-service` endpoints tồn tại; provider phụ thuộc environment; production profile đã fail-fast nếu còn config demo/sandbox/localhost. | Verify VNPay/MoMo với credential sandbox/real. |
| Notifications/FCM/WebSocket | PARTIAL | Service endpoints/events tồn tại; public JWT endpoint `/api/notifications/fcm-tokens` hoạt động; notification-service xác thực cả WebSocket handshake header và STOMP `CONNECT` bằng JWT access token, bind principal `userId` cho `/user/queue/notifications`; runtime smoke local qua gateway PASS: list/count notification, FCM token save/delete, admin send -> private STOMP `MESSAGE`; RabbitMQ `DomainEvent` listener có allow-list converter và log conversion sạch. | Verify FCM push thật trên thiết bị/Firebase credential production và lặp lại smoke trên deploy/emulator. |
| Loyalty | PARTIAL | `loyalty-service` endpoints tồn tại. | Verify event integration và FE/mobile usage. |
| Store management | DONE | `store-service` endpoints và route admin/public. | Browser smoke admin stores nếu cần. |
| Flutter customer locker ops | DONE | Login/home quick actions/rental/send/my-orders smoke pass. UI 3 màn revamp về design system shadcn navy + action gate theo state machine; chi tiết đơn tủ có nút chỉ đường tới locker qua Google Maps/address fallback. **2026-06-15**: Lưới ô tủ 2D (`StoreLockerGridPage`) — lazy-load layout theo store, DRONE cell icon + màu indigo + non-tappable; booking bỏ picker khi đến từ lưới ô (skip API load, hiện card read-only). `flutter analyze` 0 error. **2026-06-16**: chi tiết đơn tủ thêm action "Mở tủ" (gọi `/api/iot/unlock`, primary action khi đơn còn active) và "Đặt lại đơn" (gọi `/api/orders/{id}/reorder`, khi COMPLETED/CANCELED); thêm màn mới "Báo cáo của tôi" (`/locker/my-reports`, đọc `GET /api/lockers/my-reports`) + link từ snackbar sau khi báo lỗi thành công. `flutter analyze` 0 error trên các file đã sửa/thêm. Màn "Báo cáo của tôi" giờ có thêm chấm sao đánh giá khi report `RESOLVED` (`POST/GET /api/lockers/reports/{id}/rate|rating`). SEND thêm chọn kích thước hàng (SMALL/MEDIUM/LARGE) gửi kèm `size` cho `/api/orders/send`. | Smoke trên emulator với deploy cho UI mới (create/confirm/complete/extend/delegate/report/directions) + lưới ô tủ + "Mở tủ"/"Đặt lại đơn"/"Báo cáo của tôi"/đánh giá/chọn size (chưa chạy trên emulator phiên này, chỉ analyze tĩnh). |
| Flutter stores (customer) | DONE | Feature `lib/features/stores/**`: list (search + nearby), detail (info + ratings + directions), entry "Khám phá cửa hàng" từ home; gọi `/api/stores`, `/api/stores/{id}`, `/api/stores/{id}/ratings`. `flutter analyze` 0 error. | Manual smoke trên emulator với deploy có store data. |
| Flutter manager home | PARTIAL | Code và backend role login đã verify; manual UI smoke bị giới hạn. | Test manager UI trên emulator/device ổn định. |
| Flutter maintenance home | DONE | Màn hình `MaintenanceHomePage` đã hoàn tất luồng: chọn tủ, xem layout ô, báo hỏng, clear fault. Code đã analyze sạch; UI hoạt động với endpoint thật. **2026-06-16**: thêm action "Mở tủ khẩn cấp" (mọi trạng thái ô, có dialog xác nhận), hiện contact khách (SĐT) trên report card, banner điểm đánh giá trung bình của KTV. | Test trên thiết bị thật nếu cần. |
| Flutter legacy courier/logistics | PARTIAL | Nhiều route tồn tại; chưa align backend hiện tại trong pass gần nhất. | Audit hoặc remove/mark legacy. |
| Python IoT runtime | PARTIAL | `main.py` (hardware-track, chờ Raspberry Pi/Arduino + setup handshake — chưa có code BE gửi handshake này) vẫn PARTIAL. **2026-06-16**: thêm `simulate_demo_cabinet.py` (script độc lập, không đụng `main.py`) trả lời đúng payload Java đang gửi thật (`box_id`, không phải `lockerId`/`slotIndex` như `main.py` mong đợi) — đã verify round-trip MQTT thật. | Đồng bộ broker và setup handshake thật khi có hardware; cho tới đó dùng simulator cho demo. |
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
| 2026-06-16 | Backend | `mvn -B test` (toàn bộ reactor 14+ module) | PASS | Exit code 0. Docker không khả dụng phiên này nên 2 test Testcontainers Postgres của `locker-service` skip (đúng thiết kế `disabledWithoutDocker=true`, không phải lỗi). Bao gồm thay đổi `api-gateway/application.yml` (+1 path predicate), `iot-service` (audit log + lockout + force-unlock + device-status list), `locker-service` (force-open + TTL sweep + size fallback + reporter contact + rating). |
| 2026-06-16 | Mobile | `flutter analyze --no-pub` trên `locker_ops_service.dart`, `maintenance_home_page.dart`, `my_reports_page.dart`, `send_parcel_page.dart` | PASS | "No issues found!" trên cả 4 file đã sửa (force-open, rating, size selector, contact info). |
| 2026-06-16 | Web FE | `npm.cmd install` (lần đầu, `node_modules` chưa có trong môi trường này) rồi `npm.cmd run build` (`tsc -b && vite build`) | PASS | 0 lỗi TypeScript; build xong ~12s. Cảnh báo chunk-size/browserslist cũ không liên quan, không chặn build. Gồm `lockerOps.ts` (6 endpoint mới), `layout-view.tsx` (force-open), `BoxSettingModal.tsx` (sửa route chết), `Admin/maintenance/index.tsx` (schedules + device health + contact info). |
| 2026-06-16 | Backend | `mvn -pl common-lib,order-service,locker-service,notification-service -am test` | PASS | Exit code 0. `locker-service` Testcontainers PostgreSQL smoke chạy thật (Docker khả dụng phiên này) thay vì skip như các lần trước; boot full Spring context PASS sau khi thêm `publishReportNotification`. |
| 2026-06-16 | Mobile | `flutter analyze --no-pub` trên `locker_ops_service.dart`, `my_locker_orders_page.dart`, `my_reports_page.dart` (mới), `app_router.dart` | PASS | "No issues found!" — 0 error/warning trên 4 file đã sửa/thêm. |
| 2026-06-16 | IoT simulator | Round-trip MQTT thật: chạy `simulate_demo_cabinet.py` (background), publish payload đúng format Java thật gửi (`{commandId, box_id, action, timeout}`) lên `cabinet/9999-test/command/open` qua `broker.hivemq.com:1883`, nhận lại `cabinet/9999-test/command/open/result` | PASS | `status:"SUCCESS"`, `commandId` khớp, `boxId` khớp, trong ~1.5s. Xác nhận đúng contract mà `IotService.unlock()` cần (`node.has("status") && !"FAILED".equals(...)`). Dùng `pip install paho-mqtt` tạm trên Python 3.11 system (môi trường không có `uv`/Python 3.13) chỉ để test kết nối — không đổi `pyproject.toml`/`uv.lock`. |
| 2026-06-16 | Chưa chạy phiên này | Docker-compose full stack (gateway + order-service + iot-service + locker-service + notification-service) + tạo đơn SEND/RENTAL thật + gọi `/api/iot/unlock` qua gateway + emulator click-through "Mở tủ"/"Đặt lại đơn"/"Báo cáo của tôi" + xác nhận push notification thật khi claim/resolve | CHƯA CHẠY | Docker Desktop trên máy này giới hạn ~6.8GB RAM (thấy qua Testcontainers log) — không đủ an toàn để bật đồng thời 12+ JVM service cho E2E click-through trong phiên này. Mức tin cậy hiện tại dựa trên: Maven test pass (gồm Postgres thật), MQTT round-trip thật, và đọc code xác nhận khớp contract end-to-end. Khuyến nghị chạy lại theo `RUN_RESULT.md`/`HANDOFF_CODEX.md` khi cần demo thật. |
| 2026-06-15 | DB / Full demo seed | Chạy `scripts/seed-full-demo-ms.sql` trên Postgres local (×2) + login 4 account qua gateway `:18080` | PASS | Run #1 áp đủ: user_profiles 104, auth_accounts 104, stores 100, lockers 100, boxes 900, reports 100, schedules 100, repair_logs 100, promotions 100, orders 100, order_details 100, status_history 200, ratings 100, complaints 100, payments 100, refunds 100, notifications 100, fcm_tokens 100, loyalty_accounts 104, point_transactions 100, device_statuses 100. Run #2 **idempotent** (0 error, không trùng key). `POST /api/auth/login` với pw `12345678` cho cả 4 email → `AUTH_LOGIN_OK` role ADMIN/CUSTOMER/MAINTENANCE/MANAGER đúng. **Droplet `146.190.84.136:15432` CHƯA áp**: harness chặn agent ghi vào prod DB → user tự chạy script (lệnh trong báo cáo). |
| 2026-06-15 | Mobile / Auth + Startup | `flutter analyze --no-pub lib/core/network/auth_interceptor.dart lib/main.dart` sau khi sửa | PASS (0 error) | `auth_interceptor.dart` 0 issue; `main.dart` chỉ còn 8 info-level cũ (withOpacity/BuildContext across async gaps) không liên quan thay đổi. Chẩn đoán từ log `flutter run` maintenance: token `exp` Jun 15 15:01 < now 15:51 → 401 (token hết hạn thật, không phải lỗi backend); `AuthInterceptor` cũ logout thẳng. Đã thêm refresh-on-401 (gọi `POST /api/auth/refresh-token`, serialize 1 lần/đợt vì backend xoay vòng refresh token, retry request gốc). Firebase duplicate-app: guard `Firebase.apps.isEmpty`. Build chậm: Gradle heap 8G→3G + parallel/caching (máy 16GB chạy kèm Docker 12 JVM + emulator). |
| 2026-06-15 | Mobile UI | `flutter analyze lib/.../profile_page.dart lib/.../my_locker_orders_page.dart lib/.../location_services.dart` | PASS | 0 error mới. 3 issue info pre-existing (lucide unnecessary_import, deprecated `activeColor`/`withOpacity` từ SDK Flutter) không liên quan thay đổi. UI revamp: ProfilePage BrandHeroHeader layout, MyLockerOrdersPage header, LockerUtilitiesRow navigation fix. |
| 2026-06-15 | Web FE + Backend / Admin Dashboard | FE `npm.cmd run build` + BE `mvn -pl order-service -am package` + rebuild container + verify JSON endpoint | PASS | Crash `/admin/dashboard` (`reading 'toString'` tại `dash-board.tsx:60`) do dashboard đọc 11 field (`ordersToday/totalRevenue/...`) nhưng `OrderService.statistics()` chỉ trả `totalOrders`+`byStatus`. Sửa: FE `useDashboard` normalize overview đủ field default 0 (không crash dù backend thiếu) + BE thêm `ordersToday/pendingOrders/totalRevenue/revenueToday`. FE build `✓` 0 lỗi; BE jar build SUCCESS; rebuild `ll-ms-order-service` UP; gọi trực tiếp `GET :8083/api/admin/dashboard/overview` trả `{totalOrders:11, ordersToday:0, pendingOrders:2, totalRevenue:230000.00, revenueToday:0, byStatus:{...}}`. Cross-service KPI (users/stores/lockers/boxes/services) vẫn 0 (chưa wire aggregation chéo service — follow-up). |
| 2026-06-15 | Web FE / Admin 2FA | `npm.cmd run build` (`tsc -b && vite build`) trong `laundry-locker-frontend/fe` sau khi sửa `auth-context.tsx` | PASS | `✓ built in ~17s`, 0 lỗi type. Nguyên nhân crash sau OTP: FE đọc `data.user.id` nhưng `/api/admin/auth/verify-2fa` (qua `authMap`) trả payload **phẳng** (`accountId/userId/accessToken/refreshToken/roles/name`) — `data.user` `undefined` → `Cannot read properties of undefined (reading 'id')`. Đã chuẩn hoá đọc field phẳng (`userId`/`roles`/`name`) + giữ email bước 1; trace luồng: `adminLoginStep2` → `persistLogin(role:["ADMIN"])` → `getRedirectPath` → `/admin/dashboard`. OTP dev lấy từ log `auth-service` (`docker logs ll-ms-auth-service \| grep "Development OTP"`). Chưa chạy E2E browser thật (cần password admin của user). |
| 2026-06-15 | Backend / Gateway CORS | Rebuild gateway với config đã sửa + curl preflight/POST qua `:18080` | PASS | **Trước:** `OPTIONS /api/admin/auth/login` (Origin `http://localhost:3000`) trả `403`, **không có** `Access-Control-Allow-Origin` → trình duyệt báo "Failed to fetch". **Sau** khi chuyển `globalcors` về `spring.cloud.gateway.server.webflux.globalcors` + rebuild jar/image: preflight `200` với `Access-Control-Allow-Origin: http://localhost:3000`, `Allow-Methods GET,POST,PUT,PATCH,DELETE,OPTIONS`, `Allow-Credentials true`; POST thật cũng có ACAO header (body `AUTH_INVALID` khi cố tình sai mật khẩu → endpoint hoạt động bình thường). Bằng chứng prefix: `GlobalCorsProperties` trong `spring-cloud-gateway-server-4.3.4` chỉ bind `spring.cloud.gateway.server.webflux.globalcors`; prefix cũ `deprecated since 4.3.0`. |
| 2026-06-15 | Mobile | `flutter analyze --no-pub` trên các file đã chỉnh: `store_lockers_page.dart`, `locker_page.dart`, `rent_locker_page.dart`, `send_parcel_page.dart`, `locker_ops_service.dart` | PASS | 0 error trên các file đã sửa. Pre-existing 440+ info-level findings không liên quan không đổi. |
| 2026-06-15 | Backend / Mobile | Fix Cold Start 500 error trên app, tạo seed user, fix lỗi syntax | PASS | Mobile `flutter run` pass sau khi sửa syntax lỗi ngoặc ở `home_page.dart`. Login success sau khi tăng TimeLimiter của `auth-service` lên 10s và chờ API Gateway nhận diện service mới. SQL seed script đã insert test user thành công vào Postgres trực tiếp. |
| 2026-06-14 | Mobile fix/verify | Batch 2 (branch `fix/mobile-api-endpoints-alignment`, commit `08567d3`): repoint bottom-nav **Đơn hàng** từ `OrderPage` legacy (`/orders/me` chết) sang `MyLockerOrdersPage` (locker_ops, `GET /api/orders/my-orders`); tủ thiếu toạ độ → `NaN` để chi tiết tủ hiện màn "toạ độ không hợp lệ" thay vì pin (0,0). | PASS | On-device: tab Đơn hàng `GET /api/orders/my-orders` 200 hiện đơn thật (ORD-2026…); chi tiết CAB-DEMO-01 render bản đồ FPT HCMC. `flutter analyze` 0 error. |
| 2026-06-14 | Mobile fix/verify | Sửa lệch endpoint mobile↔backend (branch `fix/mobile-api-endpoints-alignment`): Profile `getProfile` → `/api/user/profile` + normalize `id` int→String + bọc try/catch `_getFaceRegistrationStatus` (AI 404 từng làm `loadProfile` ném lỗi → kẹt spinner Hồ sơ); Register → `/api/auth/register` với body `firstName/lastName/roles` + sau đăng ký chuyển tab Login (bỏ OTP legacy); tab Tủ (cả 3 method `getLocations*`) → `/api/lockers` map an toàn (id/address/lat/long nullable). | PASS | On-device (emulator) verify qua logcat + screenshot: tab Tủ `GET /api/lockers` 200 hiện 2 tủ; tab Hồ sơ `GET /api/user/profile` 200 hiện "Huy Nguyen"; register `AUTH_REGISTERED` qua API. `flutter analyze` 4 file = 0 error. Phát hiện method thật của tab Tủ là `/locations/customer` (không phải `/locations`) nhờ verify thật. |
| 2026-06-14 | Runtime/Mobile | Run mobile + backend cho manual test luồng Customer/Maintenance: gateway local `:18080` health `UP`; login API customer `nqbhuy2004nt@gmail.com` và maintenance `maintenance@laundry.test` qua `:18080` PASS; sửa mobile `.env`/`env_config.g.dart` trỏ `10.0.2.2:18080`; `flutter pub get` + `flutter build apk --debug` PASS; cài + login app trên emulator Pixel_4 vào đúng Customer Home (badge notification 17). Layout CAB-DEMO-01 (locker id=2): 6 STANDARD + 1 XL + 3 DRONE đều AVAILABLE. | PASS | Emulator Pixel_4 thiếu RAM nên từng ANR "System UI isn't responding" lúc boot; ổn định lại với `-gpu host -memory 2560`. Chỉ APK debug + sửa config local, không đổi code commit. |
| 2026-06-14 | Backend | `mvn -pl locker-service,notification-service -am test` | PASS | common-lib 8 tests pass; locker-service Testcontainers PostgreSQL smoke 2 tests pass; notification-service compile/test phase pass. |
| 2026-06-14 | Flutter | `flutter analyze --no-pub lib/features/locker_ops/presentation/pages/maintenance_home_page.dart lib/features/locker_ops/presentation/pages/my_locker_orders_page.dart lib/features/locker_ops/data/locker_ops_service.dart lib/features/locker_ops/presentation/utils/locker_maps.dart lib/features/notifications/domain/entities/notification_model.dart lib/features/notifications/infrastructure/data_sources/notification_remote_data_source.dart lib/core/services/firebase_messaging_service.dart` | PASS | Targeted analyze 7 items: "No issues found". Lệnh rộng hơn `flutter analyze lib/features/locker_ops lib/features/notifications lib/core/services/firebase_messaging_service.dart` timeout 5 phút không trả output. |
| 2026-06-14 | Web FE | `npm.cmd run build` trong `laundry-locker-frontend/fe` | PASS | `tsc -b && vite build` pass; còn warning Vite/Browserslist/chunk-size cũ, không chặn build. |
| 2026-06-14 | Git/Docs | `git diff --check` ở backend/mobile/frontend và quét private-file trong `docs/project-artifacts` | PASS | Không có whitespace error; không thấy `env.txt`, `pro.txt`, `Application.txt`, `Host *.txt` trong artifacts. |
| 2026-06-14 | Backend | `mvn -pl notification-service -am test` | PASS | common-lib 8 tests pass; notification-service compile/test phase pass sau khi thêm STOMP JWT auth và JJWT deps. |
| 2026-06-14 | Flutter | `flutter analyze --no-pub lib/features/notifications lib/core/services/firebase_messaging_service.dart` | PASS | Notification realtime STOMP client + provider integration analyze sạch; `flutter pub add stomp_dart_client` resolve `stomp_dart_client 3.0.1`. |
| 2026-06-14 | Runtime smoke | `docker ps`, gateway health local, deploy health probe | PARTIAL/FAIL | Kết quả cũ trước khi harden: containers local có vẻ chạy nhưng Docker Desktop/logs báo không start được; `http://localhost:8080/actuator/health` đóng connection; `https://api-dev.aisl.io.vn/actuator/health` trả 404. Đã có dòng PASS mới bên dưới cho local gateway port thay thế `18080`. |
| 2026-06-14 | Backend | `mvn -pl notification-service -am test` | PASS | common-lib 8 tests pass; notification-service 8 tests pass, gồm STOMP JWT principal, Spring bean wiring cho interceptor/handshake handler, và RabbitMQ `DomainEvent` converter allow-list. |
| 2026-06-14 | Backend package | `mvn -pl notification-service -am package -DskipTests` | PASS | Repackage jar notification-service thành công để Docker image copy đúng code mới. |
| 2026-06-14 | Runtime smoke | `docker compose up -d --no-deps --build notification-service`; `scripts/smoke-notification-stomp.ps1`; gateway/API probes | PASS | Do host port `8080` bị `AgentService.exe` chiếm, gateway local chạy bằng override tạm port `18080`. PASS: gateway/notification health UP, maintenance/admin login, `GET /api/notifications`, unread count, FCM token save/delete, subscribe `/user/queue/notifications`, admin send -> STOMP `MESSAGE`, và Rabbit conversion log không còn `unauthorized class DomainEvent`. |
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
  - Customer mở chi tiết đơn tủ và bấm chỉ đường tới locker.
  - Customer bấm "Mở tủ" (cần chạy `smart-locker-iot/simulate_demo_cabinet.py` song song) và "Đặt lại đơn" trên đơn COMPLETED/CANCELED.
  - Customer mở "Báo cáo của tôi" sau khi báo lỗi 1 ô.
  - Manager home.
  - Maintenance claim/resolve/clear fault và bấm chỉ đường tới locker lỗi; xác nhận customer tương ứng nhận được notification claim/resolve.
  - Maintenance bấm "Mở tủ khẩn cấp" (mobile + web `layout-view.tsx` + `BoxSettingModal.tsx`) và xác nhận ghi vào `box_access_logs`.
  - Customer đánh giá report sau khi RESOLVED; maintenance xem điểm trung bình.
  - Web admin: tạo/đã-kiểm-tra/xóa lịch bảo trì định kỳ; xem section "Sức khỏe thiết bị" có dữ liệu thật khi cabinet gửi heartbeat.
  - Thử PIN sai 5 lần liên tiếp trên 1 box, xác nhận box bị khóa tạm 15 phút.
- Verify notification runtime trên deploy/emulator:
  - Local gateway smoke đã PASS cho `/api/notifications`, unread count, FCM token save/delete và STOMP private `MESSAGE` qua `/ws` -> `/user/queue/notifications`.
  - Re-run cùng smoke trên deploy/emulator sau khi `api-dev` health route sẵn sàng.
  - Push FCM thật tới thiết bị khi Firebase credential/config production sẵn sàng.
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
| 2026-06-16 | Claude | **Đóng backlog P0/P1/P2 từ audit "IoT↔User↔Maintenance còn thiếu/chưa đồng bộ"** (xem mục audit trước đó trong phiên). Phát hiện đáng chú ý: web admin có 2 UI "Mở tủ khẩn cấp" làm sẵn nhưng cả 2 không hoạt động thật (1 giả lập toast, 1 gọi route chết `/api/admin/lockers/boxes/{id}/force-open`); và action lifecycle ô (out-of-service/cleaning/return-to-service) **đã có sẵn** ở `Admin/lockers/layout-view.tsx` từ trước — audit ban đầu nhầm là thiếu vì chỉ xem `Admin/maintenance/index.tsx`. **(1) Force-open + audit log**: `iot-service` thêm bảng `box_access_logs`/`access_attempts` (V2), endpoint internal `/internal/iot/force-unlock`; `locker-service` thêm Feign `IotClient`, endpoint `/api/maintenance/boxes/{id}/force-open`; nối cả 2 UI web (sửa `BoxSettingModal.tsx`, thêm nút ở `layout-view.tsx`) + mobile (bottom-sheet hành động ô). Mọi lần mở (PIN/QR khách lẫn MASTER override) đều ghi audit log. **(2) PIN/QR lockout**: khóa box 15 phút sau 5 lần verify-access sai (`app.iot.lockout.*`). **(3) TTL backstop cho ô RESERVED**: cột `reserved_until` (V8, 24h, cùng cửa sổ auto-cancel order-service), job giờ `LockerScheduler` chỉ là lưới an toàn. **(4) Contact info + rating 2 chiều**: `LockerReportResponse` thêm `reporterName/reporterPhone` (tra `user-service` qua Feign mới, best-effort); khách đánh giá report sau khi RESOLVED (`locker_report_ratings` V9), maintenance xem điểm trung bình. **(5) Enforce size**: fallback sang size lớn hơn khi hết đúng size; mobile SEND thêm chọn kích thước. **(6) Device health dashboard**: `GET /api/manage/iot/device-status` (route gateway mới `/api/manage/iot/**`, RBAC MANAGER/ADMIN có sẵn) + section trong `Admin/maintenance`. **(7) Web admin "Lịch bảo trì định kỳ"**: tạo/đã-kiểm-tra/xóa — trước đó chỉ mobile có. Theo quyết định user: **bỏ qua** sync trạng thái ô→MQTT (chờ hardware) và payment gating (giữ miễn phí demo); không đụng `smart-locker-iot`. | BE branch `feat/maintenance-iot-sync-hardening`: `iot-service/.../{model,repository,dto,service,controller}/*`, `iot-service/.../V2__access_audit_and_lockout.sql`, `locker-service/.../{client,model,repository,dto,service,controller}/*`, `locker-service/.../V8__reserved_ttl.sql`, `V9__report_ratings.sql`, `api-gateway/.../application.yml`. Mobile branch cùng tên: `locker_ops_service.dart`, `maintenance_home_page.dart`, `my_reports_page.dart`, `send_parcel_page.dart`. Web FE branch cùng tên: `lockerOps.ts`, `layout-view.tsx`, `BoxSettingModal.tsx`, `Admin/maintenance/index.tsx`. | `mvn -B test` toàn reactor PASS (exit 0); `flutter analyze` 4 file PASS (0 issue); web FE `npm.cmd install` + `npm.cmd run build` PASS (0 lỗi TypeScript). Chưa chạy smoke runtime đầy đủ (docker-compose + emulator + browser thật) phiên này — xem ghi chú trong "Việc Còn Lại". |
| 2026-06-16 | Claude | **Đồng bộ luồng booking tủ: IoT↔mobile mô phỏng + đặt lại đơn + customer↔maintenance qua lại.** (1) **Mô phỏng IoT**: phát hiện `main.py` không bao giờ trả lời lệnh mở vì chờ handshake `iot/{mac}/command/setup` mà Java không gửi, và payload Java gửi (`box_id`) khác field Python đọc (`lockerId`/`slotIndex`) — viết script độc lập mới `simulate_demo_cabinet.py` (không đụng `main.py`/hardware-track) trả lời đúng contract Java thật đang dùng; mobile thêm action "Mở tủ" (gọi `/api/iot/unlock`) trong chi tiết đơn. (2) **Đặt lại đơn**: phát hiện `OrderService.reorder()` gọi `create()` generic với `sendBoxId=null` và không có `cellType`/`hours` → đơn SEND reorder không có ô, đơn RENTAL sai giá/thiếu hạn thuê — sửa để branch theo type và gọi lại `createSend()`/`createRental()`; mobile thêm action "Đặt lại đơn". (3) **Customer↔Maintenance qua lại**: thêm event `locker.report.claimed`/`locker.report.resolved` (publish từ `LockerService.claimReport`/`resolveReportAndClearFault`, kèm binding RabbitMQ mới vì exchange dùng binding theo từng routing key, không phải wildcard); generalize `NotificationService.consumeDomainEvent` đọc `referenceId`/`referenceType`/`message` từ payload (fallback hành vi cũ); mobile thêm màn mới "Báo cáo của tôi" (`/locker/my-reports`) — không sửa/xoá `report_list_page.dart`/`create_report_page.dart` legacy (kiến trúc khác, gọi API chết) theo đúng tiền lệ repoint tab Đơn hàng trước đây. | BE branch `feat/booking-iot-reorder-maintenance-loop`: `order-service/.../OrderService.java`, `common-lib/.../DomainEventNames.java`, `locker-service/.../LockerService.java`, `notification-service/.../{NotificationService,config/RabbitConfig}.java`. Mobile branch cùng tên: `locker_ops_service.dart`, `my_locker_orders_page.dart`, `my_reports_page.dart` (mới), `app_router.dart`. IoT branch `feat/demo-cabinet-simulator` (repo riêng): `simulate_demo_cabinet.py` (mới), `README.md`. | `mvn -pl common-lib,order-service,locker-service,notification-service -am test` PASS (exit 0, gồm Testcontainers Postgres thật); `flutter analyze --no-pub` 4 file PASS (0 issue); MQTT round-trip thật qua `broker.hivemq.com` PASS (publish payload Java thật → nhận đúng `status:SUCCESS` trong ~1.5s). **Chưa chạy**: docker-compose full stack + emulator click-through + xác nhận push notification thật (máy có Docker RAM hạn chế ~6.8GB, không đủ an toàn cho 12+ JVM đồng thời trong phiên này). |
| 2026-06-15 | Claude | **Seed demo đầy đủ cho toàn bộ MS DB** (`scripts/seed-full-demo-ms.sql`, idempotent). Tạo 4 tài khoản đặt sẵn (pw `12345678`, hash bcrypt tái dùng từ `seed-test-user.sql`): ADMIN `baohuy2k12k4@gmail.com` (uid 9001), CUSTOMER `nqbhuy2004nt@gmail.com` (9002), MAINTENANCE `se180211nguyenquocbaohuy@gmail.com` (9003), MANAGER `huynqbse180211@fpt.edu.vn` (9004) + 100 khách bulk. Seed ~100+ bản ghi/bảng cho 22 bảng nghiệp vụ (stores/lockers/boxes/orders/details/history/ratings/complaints/promotions/payments/refunds/notifications/fcm/loyalty/point_tx/reports/schedules/repair_logs/device_status), tham chiếu chéo DB nhất quán bằng dải id riêng (≥9001 / ≥90001) + marker `*-DEMO-*`. Bỏ qua `email_otps`/`refresh_tokens` (runtime). | branch `chore/full-demo-seed`: `scripts/seed-full-demo-ms.sql`; living docs + mirror. | Local PASS ×2 (idempotent); 4 account login thật qua gateway OK đúng role. **Droplet chưa áp** (harness chặn ghi prod DB) — user chạy lệnh psql trong báo cáo. |
| 2026-06-15 | Claude | **Khắc phục lỗi/cảnh báo + build chậm khi `flutter run` mobile.** (1) **401 → auto-logout**: token access hết hạn (chỉ sống 24h) làm app auto-login bằng token cũ bị 401 rồi `AuthInterceptor` logout thẳng. Thêm **refresh-on-401**: gọi `POST /api/auth/refresh-token` bằng Dio sạch (không gắn token cũ), serialize 1 lần/đợt (backend xoay vòng refresh token), retry request gốc với token mới, chỉ logout khi refresh fail/không có refresh token. (2) **Firebase `duplicate-app`**: `main.dart` gọi `Firebase.initializeApp` trong khi đã auto-init native → throw → catch nuốt lỗi → `FirebaseMessagingService.init()` bị bỏ qua. Guard `Firebase.apps.isEmpty` để messaging vẫn init. (3) **Build chậm/giật**: Gradle heap `-Xmx8G` quá lớn cho máy ~16GB chạy kèm Docker (12 JVM) + emulator → swap; giảm `-Xmx3G` + `MaxMetaspaceSize=1G` + bật `parallel`/`caching`. KGP warning là forward-compat (chưa breaking) — không migrate vội (phụ thuộc plugin bên thứ ba). | branch `fix/mobile-token-refresh-and-startup` (repo `smart-laundry-locker-mobile`): `lib/core/network/auth_interceptor.dart`, `lib/main.dart`, `android/gradle.properties`; living docs + mirror (repo backend). | `flutter analyze --no-pub` 2 file Dart **PASS** (0 error; `auth_interceptor` 0 issue, `main.dart` chỉ info-level cũ). Chưa chạy lại `flutter run` E2E (cần thiết bị/emulator của user). |
| 2026-06-15 | TruongNguyenThaiBinh77 | **UI revamp tối 2026-06-15 (branch `refactor/unify-home-and-locker-data-flow`)**: (1) **Bottom nav bar**: frosted-glass pill không label, QR icon ngang bằng icon khác. (2) **Filter chips MyLockerOrdersPage**: nền trong suốt. (3) **Home page**: revert layout gốc + thêm wallet card (navy→blue gradient, balance, nút nạp tiền). (4) **ProfilePage**: cấu trúc layout đổi từ `CustomScrollView + SliverAppBar` sang `Column → [BrandHeroHeader, Expanded(SingleChildScrollView)]` giống LockerPage; header dùng `softHeaderGradient + navy text + BrandCircleIconButton`; loading state cũng dùng `BrandHeroHeader`; gỡ 2 import thừa. (5) **MyLockerOrdersPage**: header đổi từ white Container "Hoạt động" sang `BrandHeroHeader(title='Đơn tủ', subtitle=..., trailing=refresh)`. (6) **Navigation bug fix**: "Đơn tủ" quick action `LockerUtilitiesRow` đổi từ `context.push(AppRouter.myLockerOrders)` (route ngoài ShellRoute → mất nav bar + không có nút quay lại) sang `context.go(AppRouter.orders)` (tab trong ShellRoute → nav bar visible, hành vi tab chuẩn). | `smart-laundry-locker-mobile`: `lib/shared/widgets/custom_bottom_navigation_bar.dart`, `lib/features/home/presentation/pages/home_page.dart`, `lib/features/profile/presentation/pages/profile_page.dart`, `lib/features/locker_ops/presentation/pages/my_locker_orders_page.dart`, `lib/features/locker_ops/presentation/widgets/location_services.dart`. | `flutter analyze` 3 file đã sửa: **3 issue info/warning pre-existing** (lucide unnecessary_import + deprecated `activeColor` + `withOpacity` — không liên quan thay đổi), 0 error mới. |
| 2026-06-15 | Claude | **Fix crash trang `/admin/dashboard`** (`Cannot read properties of undefined (reading 'toString')` tại `dash-board.tsx:60`). **Nguyên nhân gốc:** lệch contract — Dashboard + OverviewSection đọc 11 field (`totalOrders, ordersToday, pendingOrders, totalRevenue, revenueToday, totalUsers, totalStores, totalLockers, activeServices, availableBoxes, occupiedBoxes`) nhưng `OrderService.statistics()` (endpoint `/api/admin/dashboard/overview`) chỉ trả `totalOrders`+`byStatus` → các field còn lại `undefined` → `.toString()`/`.toLocaleString()` nổ. **Sửa:** (1) **FE** `useDashboard` normalize `overview` về object đủ 11 field default `0` (không crash dù backend trả thiếu/partial/lỗi). (2) **BE** `order-service` bổ sung các metric tự tính từ dữ liệu order (`ordersToday`, `pendingOrders`, `totalRevenue`, `revenueToday`) — không gọi chéo service. KPI cross-service (users/stores/lockers/boxes/services) tạm hiển thị 0 (chưa wire aggregation). | FE branch `fix/admin-login-2fa-response-normalization`: `fe/src/pages/Admin/dashboard/hooks/useDashboard.ts`; BE branch `fix/gateway-cors-globalcors-prefix`: `order-service/.../service/OrderService.java`; living docs + mirror. | FE `npm.cmd run build` **PASS**; BE `mvn -pl order-service -am package` **SUCCESS**; rebuild `ll-ms-order-service` UP; endpoint trả `{totalOrders:11, ordersToday:0, pendingOrders:2, totalRevenue:230000.00, revenueToday:0, byStatus}`. |
| 2026-06-15 | Claude | **Fix crash sau khi nhập OTP ở web admin login** (`Cannot read properties of undefined (reading 'id')`). Sau khi CORS được sửa, request `verify-2fa` đi qua nhưng FE crash khi chuẩn hoá response. **Nguyên nhân gốc:** backend `/api/admin/auth/verify-2fa` (qua `AuthService.authMap`) trả payload **phẳng** `{accountId,userId,accessToken,refreshToken,tokenType,expiresAt,roles,name,...}`, **không có** key `user` cũng không có `id`; nhưng FE `adminLoginStep2` đọc `const raw = data.user; raw.id` → `data.user` `undefined` → TypeError. **Sửa (FE):** chuẩn hoá đọc các field phẳng (`id ← userId/accountId`, `role ← roles`, `fullName ← name`), giữ email người dùng nhập ở bước 1 để điền `User.email` (response không echo email), vẫn tương thích nếu sau này backend đổi sang dạng `user` lồng. Không đổi backend. | branch `fix/admin-login-2fa-response-normalization` (repo `laundry-locker-frontend`): `fe/src/context/auth-context.tsx`; living docs + mirror (repo backend). | `npm.cmd run build` (`tsc -b && vite build`) **PASS** 0 lỗi type; trace luồng tới `/admin/dashboard`. Chưa E2E browser (cần password admin của user). |
| 2026-06-15 | Claude | **Fix web admin không đăng nhập được (CORS preflight bị chặn)**. Triệu chứng: web `http://localhost:3000` gọi `POST /api/admin/auth/login` qua gateway `:18080` báo "Failed to fetch" + console `No 'Access-Control-Allow-Origin' header`. **Nguyên nhân gốc:** khối `globalcors` trong gateway nằm ở prefix cũ `spring.cloud.gateway.globalcors` (đã **deprecated từ Spring Cloud Gateway 4.3.0**, không còn bind vào `GlobalCorsProperties`) trong khi `routes`/`discovery` đã ở `spring.cloud.gateway.server.webflux.*` — nên cấu hình CORS bị bỏ qua hoàn toàn, gateway không trả header CORS. Mobile/curl không bị vì không enforce CORS. **Sửa:** chuyển `globalcors` về đúng `spring.cloud.gateway.server.webflux.globalcors`, thêm method `PATCH`. | branch `fix/gateway-cors-globalcors-prefix`: `api-gateway/src/main/resources/application.yml`; living docs + mirror. | `mvn -pl api-gateway -am package -DskipTests` SUCCESS; rebuild image + recreate `ll-ms-api-gateway`; curl preflight **403→200** kèm đầy đủ header CORS; POST thật trả ACAO + `AUTH_INVALID` (sai mật khẩu cố ý). |
| 2026-06-15 | Claude | **PA3 đợt 2 — drop `audit_logs` + bỏ hẳn `staff-service`** (theo yêu cầu). (1) **audit_logs**: migration `user-service/V4` DROP TABLE; xóa entity `AuditLog` + `AuditLogRepository`; gỡ 4 endpoint `/api/admin/audit-logs*` + field khỏi `UserController`; gỡ seed + verify. (2) **staff-service** (gỡ toàn bộ): bỏ module khỏi `pom.xml`, block khỏi `docker-compose.yml`, 3 mục route/docs/health khỏi `api-gateway`, `staff_db` khỏi `init-databases.sql`, xóa thư mục `staff-service/`; thêm `staff_db` vào script ops drop. Không service nào phụ thuộc (đã verify), fe/mobile không gọi `/api/staff`. Giải phóng 1 JVM trên droplet. | branch `chore/drop-legacy-tables`: `user-service/.../V4__drop_audit_logs.sql` + `UserController.java` (−AuditLog/Repo), `pom.xml`, `docker-compose.yml`, `api-gateway/.../application.yml`, `docker/postgres/{init-databases,seed-demo-data,verify-demo-data}.sql`, `scripts/drop-legacy-databases.sql`, xóa `staff-service/`; living docs + mirror. | `mvn -pl user-service -am compile` **SUCCESS**; `mvn -o validate` reactor OK (pom không còn staff); `docker compose config` hợp lệ. Drop bảng/DB áp khi redeploy + chạy script ops. |
| 2026-06-15 | Claude | **Tối ưu DB — thêm index cho truy vấn nóng** (chỉ thêm index, không đổi dữ liệu; `CREATE INDEX IF NOT EXISTS`). Đối chiếu repository để bù các cột bị query/sort mà chưa có index: `orders(pin_code)` (tra cứu mở tủ — nóng nhất, trước full-scan), `orders(user_id, created_at DESC)` (đơn của tôi), `order_complaints(user_id)`, `order_ratings(user_id)`, `notifications(user_id, is_read, created_at DESC)` (đếm/đọc thông báo chưa đọc — mỗi lần mở app), `locker_boxes(status)` (list ô lỗi cho bảo trì). Các lookup auth/loyalty/fcm/device đã được UNIQUE-constraint phủ nên bỏ qua. | branch `chore/drop-legacy-tables` (stack cùng PA3): `order-service/.../V3__performance_indexes.sql`, `notification-service/.../V2__unread_index.sql`, `locker-service/.../V7__box_status_index.sql`. | Đối chiếu tên cột với `CREATE TABLE` (khớp); migration additive `IF NOT EXISTS`. Áp khi redeploy (Flyway). |
| 2026-06-15 | Claude | **PA3 — dọn bảng/DB thừa (audit toàn backend: 28 bảng/10 schema)**. (1) Drop 3 bảng RBAC orphan `roles/permissions/role_permissions` (không entity/repo; role dùng cột `user_profiles.roles` VARCHAR + JWT) — migration `user-service/V3`; gỡ seed + verify tương ứng. (2) Gỡ cột `stores.partner_id` (di sản PARTNER) — migration `store-service/V2` + entity/DTO/service. (3) Gỡ DB rỗng `partner_db` + `laundry_db` khỏi `init-databases.sql` + script ops `scripts/drop-legacy-databases.sql` để drop trên DB đã chạy. (4) Đánh dấu **deprecated** (giữ tạm) `audit_logs` (không có writer runtime) + `staff_assignments`/staff-service (không flow nào gọi). | branch `chore/drop-legacy-tables`: `user-service/.../V3__drop_legacy_rbac_tables.sql`, `store-service/.../V2__drop_partner_id.sql` + `StoreLocation/StoreRequest/StoreResponse/StoreService`, `docker/postgres/{init-databases,seed-demo-data,verify-demo-data}.sql`, `scripts/drop-legacy-databases.sql`, living docs + mirror. | `mvn -pl store-service,user-service -am compile` **BUILD SUCCESS**. Migration/drop chưa chạy trên DB thật (cần redeploy + chạy script ops). |
| 2026-06-15 | Claude | **L5 (slice 4, KHÉP L5) — Bảo trì phòng ngừa (lịch định kỳ)**, BE + mobile + admin. BE: migration **V6** `maintenance_schedules` + entity/repo/DTO + `createSchedule/listSchedules/completeSchedule/deleteSchedule` (cờ `due`=now≥next_due_at); endpoint `GET /api/maintenance/schedules`, `POST /api/maintenance/schedules/{id}/complete`, `POST/DELETE /api/admin/lockers/schedules[/{id}]`. Mobile: tab **'Định kỳ'** (nhóm Đến hạn/Sắp tới + nút 'Đã kiểm tra'). Admin: section **'Bảo trì định kỳ'** (tạo/liệt kê/đã-kiểm-tra/xóa). | BE `feat/locker-maintenance-ops` (`V6__maintenance_schedules.sql`, `MaintenanceSchedule*.java`, `LockerService.java`, `LockerController.java`); mobile `feat/locker-maintenance-ops` (`maintenance_home_page.dart`, `locker_ops_service.dart`); fe `feat/locker-cell-status-admin` (`lockerOps.ts`, `maintenance/MaintenanceSchedules.tsx`, `maintenance/index.tsx`); living docs + mirror. | `mvn compile` **SUCCESS**; `flutter analyze` **0 error**; `tsc -b` **0 error**. Chưa redeploy/smoke. **Khép L5** (4 slice: vòng đời ô · work-log · SLA · phòng ngừa). |
| 2026-06-15 | TruongNguyenThaiBinh77 | **UX revamp lưới ô tủ + DRONE cell type (mobile + backend)**. (1) **Mobile** (branch `feat/mobile-user-ui-revamp`): `StoreLockerGridPage` mới — lưới ô 2D lazy-load theo store (`GET /api/lockers?storeId=X` + `GET /api/lockers/{id}/layout`), màu theo status, ô DRONE hiển thị icon máy bay indigo + không tappable, legend cập nhật. Booking `RentLockerPage`/`SendParcelPage`: bỏ picker + skip API load khi `initialLockerId` có sẵn, hiện card read-only tủ + loại ô có icon khóa. Bridge `LockerLocation`→`Store` trong `locker_page.dart`. (2) **Backend** (`locker-service`): thêm `CellType.java` constants class với Javadoc đầy đủ (STANDARD/DRONE/XL + sơ đồ ASCII + business rules); cập nhật Javadoc `LockerBox.java` (default `CellType.STANDARD`) và `CellResponse.java` (mô tả đầy đủ hành vi mobile). Không có migration mới (V3 đã seed 3 DRONE). | Mobile: `store_lockers_page.dart` (mới), `locker_page.dart`, `rent_locker_page.dart`, `send_parcel_page.dart`, `locker_ops_service.dart`, `app_router.dart`, `home_page.dart`; BE: `CellType.java` (mới), `LockerBox.java`, `CellResponse.java`; docs: `merge-status.md`, `PROJECT_PROGRESS_TRACKER.md`. | `flutter analyze` **0 error** trên các file đã chỉnh; `mvn compile` PASS (không đổi logic Java). |
| 2026-06-15 | Claude | **L5 (slice 3) — SLA phiếu bảo trì**, BE + mobile + admin. BE: `LockerReportResponse` thêm `slaHours/slaDueAt/overdue` (tính trong `toReport` từ `createdAt + app.maintenance.sla-hours` mặc định 4h; `overdue` khi quá hạn & chưa RESOLVED) — không migration. Mobile: pill "Quá hạn SLA" đỏ + tô đỏ tuổi phiếu theo `overdue`. Admin: badge "Quá hạn SLA" + đếm số phiếu quá hạn ở tiêu đề. Cờ authoritative từ BE thay cho heuristic client. | BE `feat/locker-maintenance-ops` (`LockerReportResponse.java`, `LockerService.java`); mobile `feat/locker-maintenance-ops` (`maintenance_home_page.dart`); fe `feat/locker-cell-status-admin` (`lockerOps.ts`, `maintenance/index.tsx`); living docs + mirror. | `mvn compile` **SUCCESS**; `flutter analyze` **0 error**; `tsc -b` **0 error**. Chưa redeploy/smoke. |
| 2026-06-15 | Claude | **L5 (slice 2) — Nhật ký xử lý phiếu bảo trì (work-log)**, BE + Maintenance mobile + Admin web. BE: migration **V5** bảng `repair_logs` + entity/repo/DTO + `addRepairLog/repairLogs` + endpoint `GET/POST /api/maintenance/reports/{id}/logs`. Mobile: nút "Nhật ký" trên thẻ phiếu → bottom sheet xem + thêm bước xử lý. Admin: nút "Nhật ký" → Dialog xem + thêm ghi chú. | BE branch `feat/locker-maintenance-ops` (`V5__repair_logs.sql`, `RepairLog.java`, `RepairLogRepository.java`, `RepairLogResponse.java`, `LockerService.java`, `LockerController.java`); mobile branch `feat/locker-maintenance-ops` (`maintenance_home_page.dart`, `locker_ops_service.dart`); fe branch `feat/locker-cell-status-admin` (`lockerOps.ts`, `maintenance/RepairLogDialog.tsx`, `maintenance/index.tsx`); living docs + mirror. | `mvn -pl locker-service -am compile` **SUCCESS**; `flutter analyze` **0 error**; `tsc -b` **0 error**. Chưa redeploy/smoke. |
| 2026-06-15 | Claude | **L5 — Bảo trì vận hành (vòng đời ô)**, cuốn chiếu BE + Maintenance mobile + Admin web. (1) **Backend** (`locker-service`): thêm trạng thái ô `OUT_OF_SERVICE`/`CLEANING` + `setOutOfService/setCleaning/returnToService` (guard ô đang OCCUPIED/RESERVED; `releaseBox` không lật ô ngưng-dùng); 3 endpoint `/api/maintenance/boxes/{id}/{out-of-service\|cleaning\|return-to-service}`. Không cần migration (status là String, không CHECK). (2) **Maintenance mobile**: dialog ô → bottom sheet hành động theo trạng thái (Báo hỏng/Ngưng dùng/Vệ sinh/Khôi phục/Đã sửa) + 3 method service + màu/nhãn 2 trạng thái mới. (3) **Admin web** (`layout-view`): action theo từng ô + badge OUT_OF_SERVICE/CLEANING + 3 RTK mutation. | BE branch `feat/locker-maintenance-ops` (`LockerService.java`, `LockerController.java`); mobile branch `feat/locker-maintenance-ops` (`maintenance_home_page.dart`, `locker_ops_service.dart`, `ops_widgets.dart`); fe branch `feat/locker-cell-status-admin` (`lockerOps.ts`, `lockers/layout-view.tsx`); living docs + mirror. | `mvn -pl locker-service -am compile` **BUILD SUCCESS**; `flutter analyze lib/features/locker_ops` **0 error**; `tsc -b` (fe) **0 error**. Chưa redeploy/smoke runtime. |
| 2026-06-15 | AI | - Khắc phục lỗi `500/503` (Cold Start timeout) khi Đăng nhập trên mobile bằng cách tăng `timeout-duration` của Resilience4j TimeLimiter từ `1s` mặc định lên `10s` trong `auth-service/application.yml` và restart container để API Gateway quét lại.<br>- Xây dựng SQL seed script (`scripts/seed-test-user.sql`) chèn đầy đủ dữ liệu cho tài khoản test (`binhtntse182370@fpt.edu.vn`, mk `12345678`) đồng bộ xuyên 4 DB (`auth`, `user`, `order`, `loyalty`) chạy trực tiếp trên Postgres container.<br>- Sửa lỗi build/compile ở Flutter app (`home_page.dart` missing brackets, `isFavorite` parameter trong `StoreCard`). | `auth-service/application.yml`, `scripts/seed-test-user.sql`, `smart-laundry-locker-mobile/...` | PASS |
| 2026-06-14 | User/AI | Hoàn tất giao diện mobile cho role Maintenance: xem tủ, báo hỏng, mở lại ô. Commit và push vào develop. | `smart-laundry-locker-mobile/...` | PASS trên emulator |
| 2026-06-14 | Claude | Mobile batch 2: repoint tab Đơn hàng sang đơn tủ thật (locker_ops) và làm chi tiết tủ thân thiện khi thiếu toạ độ. **Đã push + merge vào `develop`**: rebase branch `fix/mobile-api-endpoints-alignment` lên `origin/develop` mới nhất (đã có PR #4 locker-ui-map; rebase không conflict), push branch, merge `--no-ff` vào develop (merge commit `23dbf76`) và push `origin/develop`. `env_config.g.dart` (18080 local) và `.env` vẫn KHÔNG commit/push. | `smart-laundry-locker-mobile` branch `fix/mobile-api-endpoints-alignment` (2 commit fix) → `develop`; `lib/core/routing/app_router.dart`, `lib/features/locker/infrastructure/data_sources/locker_remote_data_source_impl.dart`; living docs + mirror. | `flutter analyze` toàn project **0 error** (412 info debt cũ) sau rebase; on-device verify tab Đơn hàng + chi tiết tủ; push `develop` thành công (`1cb716b..23dbf76`). |
| 2026-06-14 | Claude | Audit toàn bộ mobile + sửa lệch endpoint (app gốc Revoland chỉ migrate một phần sang backend microservices hiện tại). Sửa 3 màn hỏng người dùng gặp: tab Hồ sơ (profile 404 + kẹt spinner do face-status AI), Đăng ký (sai path+payload), tab Tủ (gọi `/locations/customer` chết). Lập inventory đầy đủ feature legacy chưa wire (wallet/topup, courier/staff-application, face/QR AI, delegations legacy, advertisements/blogs, tab Đơn hàng order model cũ). | branch `fix/mobile-api-endpoints-alignment`: `profile_remote_data_source_impl.dart`, `profile_provider.dart`, `auth_remote_data_source_impl.dart`, `auth_bottom_sheet.dart`, `locker_remote_data_source_impl.dart`; living docs + mirror. | `flutter analyze` 0 error; rebuild APK PASS; on-device verify Tủ/Hồ sơ render + register API `AUTH_REGISTERED`. Chưa commit/push (chờ user). |
| 2026-06-14 | Claude | Cấu hình + chạy mobile với backend để manual test luồng tủ Customer/Maintenance. Phát hiện gateway local chạy port `18080` (host 8080 bị process khác chiếm) nhưng mobile hard-code `10.0.2.2:8080` → tạo `.env` (private, gitignored, không commit) + cập nhật generated `env_config.g.dart` sang `10.0.2.2:18080`, rebuild + cài APK debug, login customer vào đúng Customer Home. Maintenance login role PASS. | `smart-laundry-locker-mobile/.env` (local/private), `lib/core/config/env_config.g.dart` (generated, local không commit), living docs. | gateway `:18080` health UP; customer+maintenance login PASS; `flutter pub get`/`build apk --debug`/install/login Customer Home PASS. |
| 2026-06-14 | Codex | Hoàn tất hardening runtime notification còn dang dở: tách STOMP JWT auth thành interceptor có test, thêm handshake principal handler để Spring user registry nhận `userId`, thêm smoke script PowerShell cho CONNECT/subscribe/trigger/receive, và cấu hình RabbitMQ converter allow-list cho `DomainEvent` để hết lỗi deserialize. | `notification-service/src/main/java/com/huynqb/laundrylocker/notification/config/**`, `notification-service/src/test/java/com/huynqb/laundrylocker/notification/config/**`, `scripts/smoke-notification-stomp.ps1`, living docs/mirror. | `mvn -pl notification-service -am test` PASS; `mvn -pl notification-service -am package -DskipTests` PASS; Docker rebuild notification-service PASS; runtime smoke qua gateway `18080` PASS: health/login/list/count/FCM token save-delete/private STOMP MESSAGE/Rabbit conversion log clean. |
| 2026-06-14 | Codex | Sau khi merge/push vertical slice maintenance/user vào nhánh chính, tiếp tục harden realtime notification: notification-service giờ xác thực STOMP `CONNECT` bằng JWT access token và set user principal cho `/user/queue/notifications`; Flutter thêm `stomp_dart_client` và `RealtimeNotificationService` để subscribe realtime, insert notification mới và cập nhật unread badge. Runtime smoke STOMP/FCM còn bị chặn bởi Docker/gateway local không ổn định và deploy health 404. | `notification-service/pom.xml`, `notification-service/.../WebSocketConfig.java`, `notification-service/application.yml`, `smart-laundry-locker-mobile/lib/features/notifications/**`, `pubspec.yaml`, `pubspec.lock`, living docs/mirror. | `mvn -pl notification-service -am test` PASS; `flutter analyze --no-pub lib/features/notifications lib/core/services/firebase_messaging_service.dart` PASS; runtime smoke PARTIAL/FAIL do môi trường. |
| 2026-06-14 | Codex | Nâng cấp vertical slice maintenance/user realtime-readiness: maintenance API trả thêm locker address/toạ độ/thông tin ô; notification-service thêm public JWT endpoint `/api/notifications/fcm-tokens`; mobile sửa notification client sang `/api/notifications/**`, sync FCM token đúng gateway, thêm chỉ đường trong đơn tủ customer và màn Maintenance Home có dashboard ca trực/SLA/chỉ đường; web admin maintenance thêm stats và nút chỉ đường. | `locker-service`, `notification-service`, `smart-laundry-locker-mobile/lib/features/locker_ops/**`, `smart-laundry-locker-mobile/lib/features/notifications/**`, `smart-laundry-locker-mobile/lib/core/services/firebase_messaging_service.dart`, `laundry-locker-frontend/fe/src/pages/Admin/maintenance`, `fe/src/stores/apis/admin/lockerOps.ts`, living docs/mirror. | Backend Maven targeted PASS; Flutter targeted analyze PASS; Web FE build PASS. |
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
