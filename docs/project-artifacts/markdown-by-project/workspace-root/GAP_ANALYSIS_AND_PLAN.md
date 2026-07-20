# GAP ANALYSIS & KẾ HOẠCH — Đối chiếu dự án hiện tại với đề tài DDSLMS

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho
> locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng
> Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`,
`RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

> Ngày phân tích: 2026-06-12
> Tài liệu đối chiếu: `SU26SE181xxx_DDSLMS_Register_v4.pdf` (bản đăng ký capstone — **chuẩn pháp lý của đề tài**),
`Capstone_Plan_Vi_AI SmartLocker Drone.pdf` (kế hoạch DDFMS v2.1), `LOCKR_Report_v3_VI.pdf` (nghiên cứu LOCK.R)
> Code đối chiếu: `laundry-locker-microservices`, `laundry-locker-frontend/fe`, `smart-laundry-locker-mobile`,
`smart-locker-iot`, `laundry-locker-frontend/drone`

---

## PHẦN 0 — CẬP NHẬT SAU LOCKER PHASE 2 (2026-06-13)

Tài liệu gốc được phân tích ngày 2026-06-12. Sau phiên triển khai tiếp theo, một số gap đã được đóng:

- Backend locker Phase 2 đã có SEND, RENTAL, PIN/QR signed token, `/api/manage/**`, `/api/maintenance/**`, fault/report
  claim-resolve, scheduler reminder/cleanup.
- FE admin đã có trang locker list/layout/maintenance và `npm.cmd run build` PASS.
- Flutter mobile đã có login thật, routing theo role Customer/Manager/Maintenance, màn SEND/RENTAL/My Locker Orders,
  Manager Home, Maintenance Home; targeted analyze và debug APK build PASS.
- `laundry-service` và `partner-service` vẫn chưa có source; đã được skip bằng `docker-compose.override.yml`.

Kết luận capstone vẫn giữ nguyên ở cấp đề tài DDSLMS: phần **drone delivery service**, **battery-aware assignment**, *
*drone simulator/realtime tracking**, và **AI/RAG support** vẫn là gap chính so với bản đăng ký. Các nhận định cũ bên
dưới về mobile login mock/maintenance backend thiếu đã được thay thế bởi trạng thái mới trong phần này.

## PHẦN 1 — KẾT LUẬN TỔNG QUAN (đọc 30 giây)

**Luồng nghiệp vụ hiện tại CHƯA hoàn chỉnh so với đề tài đã đăng ký.**

Đề tài đăng ký là **DDSLMS — Drone Delivery and Smart Locker Management System** (mô hình **Hub → Smart Locker**, drone
bay từ kho đến tủ, khách lấy hàng bằng PIN/QR). Code hiện tại là **hệ thống giặt ủi qua tủ khóa** (khách gửi đồ → staff
giặt → trả vào tủ). Hai hệ chia sẻ ~40% nền tảng (locker, auth, notification, IoT mở tủ, mobile app) nhưng **toàn bộ
trục chính của đề tài — drone — chưa có một dòng code backend nào**:

| Trục chính đề tài                                                       | Trạng thái trong code                                                                                                                 |
|-------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| Drone Fleet Management                                                  | ❌ Không tồn tại (0 kết quả tìm "drone" trong backend)                                                                                 |
| Battery-aware Drone Assignment (3 chiến lược — research question)       | ❌ Không tồn tại                                                                                                                       |
| Drone Simulator Service (telemetry 3–5s qua WebSocket)                  | ❌ Không tồn tại                                                                                                                       |
| Realtime tracking dashboard + bản đồ (Leaflet/Google Maps)              | ❌ FE không có thư viện map nào                                                                                                        |
| Maintenance Management backend                                          | ✅ Đã có maintenance/fault/report claim-resolve cho locker Phase 2; preventive maintenance cho drone/thiết bị vẫn là Phase 3/tương lai |
| AI Knowledge Support (RAG)                                              | ❌ Không tồn tại                                                                                                                       |
| Luồng đơn Pending → Assigned → In-flight → Deposited → Delivered/Failed | ❌ Đang là luồng giặt ủi INITIALIZED → STORING → COLLECTED → PROCESSING → READY → RETURNED → COMPLETED                                 |
| Smart Locker (cells, PIN, deposit/pickup, expiry)                       | ✅ Có ~70% (locker-service + iot-service + RPi)                                                                                        |
| Auth JWT + RBAC, Notification (FCM/WebSocket), Web + Mobile app         | ✅ Có nền tảng tốt                                                                                                                     |

**Rủi ro lớn nhất**: nếu giữ nguyên hướng "giặt ủi", sản phẩm sẽ **không khớp bản đăng ký capstone** (sai tên đề tài,
sai mô hình nghiệp vụ, thiếu research question). Cần chuyển trục ngay từ bây giờ theo kế hoạch ở Phần 5.

---

## PHẦN 2 — LUỒNG NGHIỆP VỤ HIỆN TẠI CỦA CODE (as-is)

### 2.1 Luồng giặt ủi (luồng chính đang chạy được)

```
Khách (Web/Mobile) → Gateway:8080 → auth-service: đăng ký/đăng nhập (JWT)
  → order-service: tạo đơn giặt (chọn store, locker, box gửi) → sinh PIN
  → Khách nhập PIN tại tủ → iot-service verify → MQTT → RPi mở ô → bỏ đồ → confirm (STORING)
  → Staff collect (COLLECTED) → cân/định giá (updateWeight) → giặt (PROCESSING) → xong (READY)
  → Staff trả đồ vào ô (RETURNED, PIN mới, deadline 24h) → notification (FCM/WebSocket)
  → Khách thanh toán (CASH/VNPay/MoMo qua payment-service) → nhập PIN lấy đồ (COMPLETED)
  → Scheduler: tự hủy đơn quá hạn, nhả box, nhắc lấy đồ, phí quá giờ
```

Trạng thái đơn: `INITIALIZED → STORING → COLLECTED → PROCESSING → READY → RETURNED → COMPLETED / CANCELED`

### 2.2 Các luồng phụ đang có

- **Locker/Box**: reserve/release/open box, trạng thái box, admin CRUD — locker-service.
- **IoT mở tủ**: iot-service ⇄ MQTT (HiveMQ) ⇄ RPi Python (`smart-locker-iot`) ⇄ RS485 Arduino ⇄ servo. Topic:
  `cabinet/{id}/command/open`, heartbeat, status. **Đây chính là "Locker IoT API" mà đề tài cần — tái sử dụng được
  nguyên vẹn.**
- **Notification**: RabbitMQ event (`order.created`, `order.status.changed`, `payment.completed`...) → lưu DB + FCM +
  WebSocket `/ws`.
- **Loyalty/Promotion/Rating/Complaint**: phục vụ domain giặt ủi (ngoài scope DDSLMS — giữ hoặc bỏ tùy quyết định).
- **Drone (prototype rời)**: `laundry-locker-frontend/drone/` có ArUco detector + MAVLink (Python) — khớp nghiên cứu hạ
  cánh chính xác trong LOCK.R chương 2.4, **chưa tích hợp gì với backend** và theo bản đăng ký thì "physical drone
  hardware control" nằm **ngoài scope** (chỉ cần Simulator).

### 2.3 Tình trạng kỹ thuật từng thành phần (kiểm chứng 2026-06-12)

| Thành phần                                                    | Trạng thái                                                                                                                                                                                             |
|---------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Backend source-backed services + Postgres + RabbitMQ (Docker) | ✅ 10 service nghiệp vụ có source chạy qua compose; gateway health 200                                                                                                                                  |
| laundry-service, partner-service                              | ❌ Khai trong README/compose nhưng **không có source**                                                                                                                                                  |
| Web FE (React 19, :3000)                                      | ✅ Chạy, trỏ đúng gateway; **chưa có map, chưa có màn hình drone**                                                                                                                                      |
| Mobile Flutter (LOCKERLY)                                     | ✅ Login thật qua `/api/auth/login`, routing theo role Customer/Manager/Maintenance, locker ops SEND/RENTAL/my-orders đã nối API; một số legacy endpoint như advertisements/blogs/wallet vẫn có thể 404 |
| IoT RPi                                                       | ✅ Chạy mô phỏng được (SIMULATION=true), nối cùng MQTT broker với backend                                                                                                                               |
| Số liệu test                                                  | ✅ Tài khoản `nqbhuy2004nt@gmail.com` + 4 đơn + thanh toán (script `seed-test-data.ps1`)                                                                                                                |

---

## PHẦN 3 — ĐỐI CHIẾU CHI TIẾT VỚI BẢN ĐĂNG KÝ DDSLMS

### Module 1 — Delivery Management (đăng ký) vs hiện tại

| Yêu cầu đăng ký                                                                                                        | Hiện có                                                           | Gap                                                                                                                                        |
|------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| Status flow `Pending → Assigned → In-flight → Deposited → Delivered / Failed`                                          | Luồng giặt ủi khác hoàn toàn                                      | **Thiết kế lại state machine đơn hàng** (map: INITIALIZED→Pending; bổ sung Assigned/In-flight/Deposited; COMPLETED→Delivered; thêm Failed) |
| ETA theo route + tốc độ drone                                                                                          | Không có                                                          | Cần Haversine + drone_speed + buffer                                                                                                       |
| Realtime order tracking qua WebSocket                                                                                  | Notification WebSocket có, nhưng không có vị trí drone            | Cần kênh telemetry                                                                                                                         |
| **Battery-aware Drone Assignment** (Nearest + battery ≥30% + loại drone bảo trì; so sánh Random/Nearest/Battery-aware) | Không có                                                          | **Đây là RESEARCH QUESTION của đề tài — bắt buộc.** Cần assignment engine + 3 strategy + số liệu thí nghiệm                                |
| **Drone Simulator Service** (telemetry lat/lng/speed/battery/status mỗi 3–5s)                                          | Không có                                                          | Bắt buộc (thay cho drone thật)                                                                                                             |
| PIN/QR notification khi deposit                                                                                        | PIN có (order + iot verify); QR chưa; SMS/Email OTP chưa gửi thật | Bổ sung QR (sinh + verify chữ ký), nối SMTP thật                                                                                           |

### Module 2 — Smart Locker Management (đăng ký) vs hiện tại

| Yêu cầu đăng ký                                                          | Hiện có                                            | Gap                                                           |
|--------------------------------------------------------------------------|----------------------------------------------------|---------------------------------------------------------------|
| Cell states `Available · Reserved · Occupied · Expired · Fault`          | Box có reserve/release/open                        | Bổ sung trạng thái Expired + Fault, chuẩn hóa enum            |
| Deposit flow: IoT API mở ô → drone deposit → sinh PIN/QR → notify        | Có mở ô qua MQTT + PIN theo đơn                    | Cần API `deposit` gắn với drone mission (thay vì khách bỏ đồ) |
| Pickup flow: PIN/QR → mở ô → Delivered → release cell                    | Có (PIN → mở ô → COMPLETED → release)              | Gần đủ — thêm QR, đổi tên trạng thái                          |
| Auto-expiry @Scheduled + grace period                                    | Có (pickup deadline 24h + scheduler + phí quá giờ) | ✅ Tốt hơn yêu cầu (có cả overtime fee)                        |
| Locker dashboard realtime: utilization, transaction history, fault alert | Admin API có CRUD/report cơ bản                    | FE cần màn hình utilization + fault alert                     |

### Module 3 — Maintenance Management (đăng ký) vs hiện tại

| Yêu cầu                                         | Hiện có                                                                                                                                            | Gap                                                         |
|-------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| Maintenance schedule (time-based + usage-based) | ❌                                                                                                                                                  | Tạo mới hoàn toàn (service hoặc module trong drone-service) |
| Repair log + technician role                    | ⚠️ Locker fault/report claim-resolve đã có; preventive repair log/schedule và technician role chuyên biệt cho drone/thiết bị vẫn cần thiết kế thêm |
| Drone đang bảo trì bị loại khỏi assignment      | ❌                                                                                                                                                  | Ràng buộc trong assignment engine                           |
| Alert trước hạn / overdue qua WebSocket         | Notification framework có sẵn                                                                                                                      | Chỉ cần phát event mới                                      |

### Module AI Knowledge Support (đăng ký) vs hiện tại

| Yêu cầu                                                        | Hiện có | Gap                                                                                                       |
|----------------------------------------------------------------|---------|-----------------------------------------------------------------------------------------------------------|
| RAG Q&A trên SRS/SOP/API doc/Maintenance guide; relevance ≥80% | ❌       | Tạo ai-service: ingest → chunk → embedding (pgvector) → retrieve → LLM; RBAC; fallback "không đủ dữ liệu" |
| Report summarization (KPI tuần/tháng)                          | ❌       | @Scheduled aggregate → snapshot → AI summary                                                              |

### Mobile App (đăng ký: tracking realtime, PIN/QR pickup, locker map, notifications, history)

| Yêu cầu                               | Hiện có                                                                      | Gap                                                         |
|---------------------------------------|------------------------------------------------------------------------------|-------------------------------------------------------------|
| Login thật                            | ✅ Đã có màn hình login thật dùng `identifier/password` qua `/api/auth/login` | Legacy register/OTP datasource còn cần rà soát nếu dùng lại |
| Realtime delivery tracking            | UI courier_dispatch có khung WebSocket                                       | Nối kênh telemetry simulator                                |
| Locker map                            | ❌                                                                            | Thêm map (google_maps_flutter/flutter_map)                  |
| PIN/QR pickup, notifications, history | PIN có khung; FCM có khung                                                   | Nối backend + QR                                            |

### Non-functional (đăng ký)

| Yêu cầu                                     | Hiện trạng                                                                                        |
|---------------------------------------------|---------------------------------------------------------------------------------------------------|
| API latency <300ms (95%), uptime >95%       | Chưa đo — cần load test (G9/W11)                                                                  |
| WebSocket delay <2s; telemetry ổn định 3–5s | Chưa có telemetry                                                                                 |
| JWT + RBAC 100% endpoint                    | Gateway có JWT/RBAC nhưng nhiều endpoint public (stores/lockers POST không cần token) — cần audit |
| Docker + CI/CD GitHub Actions               | Docker ✅; CI/CD ❌ (`.github` folder có nhưng cần kiểm tra/bổ sung workflow)                       |

---

## PHẦN 4 — DANH SÁCH VIỆC CHƯA HOÀN THÀNH (ưu tiên giảm dần)

**Nhóm A — Bắt buộc để đề tài đúng đăng ký (P0):**

1. `drone-service` mới: CRUD drone (model, specs, battery, status:
   Available/Delivering/Charging/Maintenance/OutOfService), drone_status_logs, drone_assignments.
2. **Assignment engine 3 chiến lược** (Random / Nearest-Haversine / Battery-aware ≥30% + maintenance-aware) + config +
   log kết quả để chạy thí nghiệm.
3. **Drone Simulator Service**: sinh telemetry (lat/lng/speed/battery/status) mỗi 3–5s, mô phỏng mission Hub→Locker (lấy
   tọa độ locker từ locker-service), đẩy qua WebSocket + RabbitMQ.
4. Tái cấu trúc luồng đơn theo `Pending → Assigned → In-flight → Deposited → Delivered / Failed` (đề xuất: thêm
   `delivery-service` mới hoặc mở rộng order-service với `type=DRONE_DELIVERY` để không phá luồng giặt ủi cũ).
5. Realtime tracking dashboard trên FE: map (Leaflet — miễn phí, không cần API key) + vị trí drone live + route + ETA.
6. `maintenance` module: schedule + repair log + technician role + loại khỏi assignment.
7. Thí nghiệm so sánh 3 strategy (Average Delivery Time, Assignment Success Rate, Utilization, Failed Rate) — xuất số
   liệu cho paper.

**Nhóm B — Theo đăng ký, làm sau Nhóm A (P1):**

8. ai-service (RAG): pgvector + ingest tài liệu SRS/SOP + endpoint `/api/ai/chat`, `/api/ai/reports/analyze`;
   guardrails.
9. Mobile: login thật và locker ops đã có; phần còn lại là tracking drone realtime, locker map, rà legacy register/OTP
   path nếu dùng lại.
10. QR code: sinh QR ký số cho pickup (bên cạnh PIN), verify tại iot-service.
11. Locker đã có `FAULT`, dashboard utilization và cleanup; nếu cần trạng thái `EXPIRED` riêng ở cấp cell thì bổ sung ở
    Phase 3.
12. CI/CD GitHub Actions (build + test + docker build); security audit RBAC 100% endpoint.

**Nhóm C — Nợ kỹ thuật hiện hữu cần dọn (P2):**

13. `laundry-service`/`partner-service`: viết thật hoặc **gỡ khỏi README/compose** để khớp tài liệu (hiện đã override để
    bỏ qua).
14. Mobile `verifyOtp/register/faceVerify` sai base path `/auth` → `/api/auth`.
15. SMTP thật cho OTP/notification email (Docker đang trỏ localhost:1025 không có mailserver).
16. Load test + đo latency/uptime theo non-functional target.
17. (Tùy chọn, ngoài scope đăng ký) tích hợp prototype ArUco/MAVLink trong `drone/` làm demo phần cứng — chỉ làm nếu dư
    thời gian, vì registration ghi rõ out-of-scope.

---

## PHẦN 5 — KẾ HOẠCH HÀNH ĐỘNG (map theo Task Package trong bản đăng ký)

> Giả định nhóm 4 người theo phân công DDFMS: SV1 Backend lead, SV2 Frontend, SV3 DB/DevOps, SV4 Optimization/QA. Bạn (
> Huy — SE180211) đang giữ codebase nên ở vai trò tích hợp.

### Giai đoạn 1 (≈ tuần 1–2) — Chốt kiến trúc & nền móng drone  *(Task package 1)*

- [ ] **Quyết định kiến trúc** (khuyến nghị): giữ skeleton microservices hiện tại, THÊM `drone-service` (8093),
  `delivery-service` (8094, hoặc mở rộng order-service), `ai-service` (8095), simulator chạy như module trong
  drone-service. Cập nhật gateway routes + init-databases.sql + compose.
- [ ] ERD mới: drones, drone_status_logs, drone_assignments, delivery_orders (hoặc cột mới trong locker_orders),
  maintenance_schedules, repair_logs, ai_documents/ai_chunks (pgvector).
- [ ] Định nghĩa state machine delivery + sự kiện RabbitMQ mới (`delivery.assigned`, `drone.telemetry`,
  `delivery.deposited`...).
- [ ] Sửa mobile login thật + path `/api` (việc nhỏ, mở khóa demo end-to-end sớm).
- **Deliverable**: ERD + API contract + gateway route mới + mobile login chạy thật.

### Giai đoạn 2 (≈ tuần 3–5) — Drone core + Simulator  *(Task package 2)*

- [ ] drone-service: CRUD + status lifecycle + Flyway.
- [ ] Assignment engine: interface `AssignmentStrategy` với 3 implementation (Random/Nearest/BatteryAware) chọn qua
  config — log mọi quyết định vào drone_assignments để chấm thí nghiệm.
- [ ] Drone Simulator: mission Hub→Locker, nội suy vị trí theo tốc độ, pin tụt theo quãng đường, phát telemetry 3–5s (
  WebSocket topic `/topic/drones/{id}` + event RabbitMQ).
- [ ] delivery flow: create order → auto-assign → simulator bay → tới locker → gọi iot-service deposit (tái dùng MQTT mở
  ô!) → sinh PIN/QR → notify → khách pickup → Delivered; Failed khi pin cạn/locker Fault.
- **Deliverable**: demo backend end-to-end bằng Postman + simulator.

### Giai đoạn 3 (≈ tuần 6–8) — Tracking UI + Maintenance + Locker nâng cấp  *(Task package 3)*

- [ ] FE: trang Delivery Tracking (Leaflet map, drone marker live, route, ETA, status timeline) + trang Fleet (danh sách
  drone, pin, trạng thái) + Locker dashboard (utilization, fault).
- [ ] Maintenance: schedule (time/usage trigger) + repair log + alert WebSocket + ràng buộc assignment.
- [ ] Locker: thêm cell states Expired/Fault + QR pickup.
- [ ] Mobile: tracking screen + locker map + QR.
- [ ] **Chạy thí nghiệm 3 strategy** (script seed ~100–500 đơn mô phỏng, thu metrics) → bảng số liệu cho paper draft v1.
- **Deliverable**: demo đầy đủ 3 vai trò (Operator/Customer/Technician) + số liệu thí nghiệm + paper draft.

### Giai đoạn 4 (≈ tuần 9–12) — AI + DevOps + Hoàn thiện  *(Task package 4)*

- [ ] ai-service RAG (pgvector + LLM API): ingest SRS/SOP/API doc, `/api/ai/chat`, report summarization; test relevance
  ≥80% trên bộ 20–30 câu hỏi.
- [ ] CI/CD GitHub Actions: build matrix các module, test, docker build; security audit JWT/RBAC từng endpoint (đặc biệt
  các POST đang public).
- [ ] Load test (k6/JMeter): chứng minh latency <300ms p95, WebSocket <2s.
- [ ] Dọn nợ: laundry/partner-service (viết hoặc gỡ tài liệu), SMTP thật, cập nhật README/PROJECT_FLOW theo kiến trúc
  mới.
- **Deliverable**: hệ thống final + báo cáo thí nghiệm + tài liệu capstone.

### Việc nên làm NGAY tuần này (không chờ team)

1. Chốt với nhóm + giảng viên: **mô hình dữ liệu drone & quyết định delivery-service riêng hay mở rộng order-service** (
   ảnh hưởng mọi thứ phía sau).
2. Sửa mobile login thật (≤1 ngày, mở khóa demo).
3. Scaffold `drone-service` từ template store-service (service đơn giản nhất hiện có) — copy cấu trúc, đổi domain.
4. Viết Drone Simulator dạng đơn giản nhất (1 class @Scheduled nội suy tọa độ) để FE/mobile có data làm UI song song.

---

## PHẦN 6 — NHỮNG GÌ TÁI SỬ DỤNG ĐƯỢC TỪ CODE HIỆN TẠI (đỡ ~40% công sức)

| Tài sản có sẵn                                     | Dùng cho DDSLMS                                                                                                           |
|----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| auth-service + gateway JWT/RBAC                    | Nguyên vẹn (3 role: Admin/Operator, Customer, Technician — thêm role TECHNICIAN)                                          |
| locker-service (locker/box/reserve/release)        | Nền của Smart Locker module — chỉ thêm cell states + QR                                                                   |
| iot-service + MQTT + RPi `smart-locker-iot`        | **Điểm khác biệt của nhóm**: deposit/pickup mở ô THẬT qua phần cứng mô phỏng — demo ấn tượng hơn các nhóm chỉ có software |
| notification-service (FCM + WebSocket + RabbitMQ)  | Telemetry + PIN/QR notify + maintenance alert                                                                             |
| order-service scheduler (auto-cancel, expiry, fee) | Logic expiry locker đã vượt yêu cầu đăng ký                                                                               |
| Web FE (React + Redux + STOMP)                     | Thêm trang mới, không phải làm lại                                                                                        |
| Mobile Flutter (20 feature modules sẵn UI)         | courier_dispatch/maintenance/locker UI có sẵn khung                                                                       |
| `drone/` ArUco + MAVLink prototype                 | Phụ lục nghiên cứu LOCK.R / demo bonus                                                                                    |
| Hạ tầng Docker + run-all.ps1 + seed script         | Dev environment hoàn chỉnh                                                                                                |

---

*File liên quan: `PROJECT_FLOW.md` (luồng kỹ thuật hiện tại), `RUN_RESULT.md` (trạng thái chạy), `RUN_ALL_GUIDE.md` (
cách chạy).*
