# KẾ HOẠCH LUỒNG TỦ (LOCKER FLOW) — Tách biệt với luồng Drone

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho
> locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng
> Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`,
`RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

> Ngày: 2026-06-12 • Căn cứ: ảnh thiết kế tủ demo + `GAP_ANALYSIS_AND_PLAN.md` + bản đăng ký DDSLMS
> Nguyên tắc: **luồng tủ độc lập hoàn toàn với luồng drone** — drone chỉ là MỘT kênh deposit vào hàng ô đầu tiên; mọi
> nghiệp vụ tủ khác chạy không cần drone.

---

## 1. MÔ HÌNH VẬT LÝ (theo ảnh thiết kế)

```
                    ┌──── Bãi đáp drone (nóc, ArUco 0.6×0.6m) ────┐
┌──────────────┬────┴──────────────────────────────────────────────┤
│ Màn hình     │  Ô D1 (DRONE)  │  Ô D2 (DRONE)  │  Ô D3 (DRONE)  │ ← Hàng 1: CHỈ nhận hàng drone thả
│ cảm ứng      ├────────────────┼────────────────┼────────────────┤
│ 10–15"       │  Ô S4          │  Ô S5          │  Ô S6          │ ← Hàng 2: ô thường, đa dịch vụ
├──────────────┼────────────────┼────────────────┼────────────────┤
│ Ô XL (VALI)  │  Ô S7          │  Ô S8          │  Ô S9          │ ← Hàng 3: ô thường, đa dịch vụ
│ 0.3×0.8×0.4  │                                                   │
└──────────────┴───────────────────────────────────────────────────┘
  Tổng: 1.5m rộng × 1.2m cao × 0.5m sâu
  Ô thường: 0.45 × 0.30 × 0.50  •  Mở bằng PIN / QR / vân tay / khuôn mặt / access code
```

**Quy tắc nền tảng:**

- Ô hàng 1 (`cellType = DRONE`): chỉ luồng drone-delivery được reserve. Khách vẫn **lấy hàng** từ ô này bằng PIN/QR như
  mọi ô khác.
- Ô thường (`cellType = STANDARD`) + ô vali (`cellType = XL`): phục vụ mọi dịch vụ, không phân biệt.
- Một ô tại một thời điểm chỉ thuộc về một đơn (order) — trạng thái ô là nguồn sự thật.

## 2. MÔ HÌNH DỮ LIỆU MỚI (locker-service)

### 2.1 Bảng `locker_boxes` — cột bổ sung

| Cột mới        | Kiểu                   | Ý nghĩa                          |
|----------------|------------------------|----------------------------------|
| `cell_type`    | VARCHAR(30) = STANDARD | `DRONE` \| `STANDARD` \| `XL`    |
| `row_index`    | INT                    | Hàng trong tủ (1 = trên cùng)    |
| `col_index`    | INT                    | Cột trong tủ (0 = khu trái/vali) |
| `fault_reason` | VARCHAR(500)           | Lý do hỏng khi status = FAULT    |

### 2.2 Trạng thái ô (mở rộng theo bản đăng ký DDSLMS)

```
AVAILABLE ──reserve──▶ RESERVED ──occupy(bỏ đồ xong)──▶ OCCUPIED ──release──▶ AVAILABLE
    │                      │                                │
    │                      └────release (hủy đơn)───────────┘
    └──markFault──▶ FAULT ──clearFault──▶ AVAILABLE          (EXPIRED do order-service quyết
                                                              định qua pickup deadline; ô vẫn
                                                              OCCUPIED tới khi release)
```

> Thay đổi so với cũ: trước đây `reserve` nhảy thẳng sang OCCUPIED. Nay tách 2 bước RESERVED → OCCUPIED để màn hình
> tủ/dashboard phân biệt "đã giữ chỗ" và "đang có đồ". `release` giữ nguyên hợp đồng với order-service (không phá luồng
> giặt ủi cũ).

### 2.3 Bảng `lockers` — cột bổ sung

| Cột mới             | Kiểu            | Ý nghĩa                                  |
|---------------------|-----------------|------------------------------------------|
| `landing_pad`       | BOOLEAN = false | Tủ có bãi đáp drone trên nóc             |
| `landing_marker_id` | VARCHAR(50)     | ID ArUco marker trên nóc (vd "ARUCO-23") |

## 3. LUỒNG NGHIỆP VỤ TỦ (5 luồng, drone chỉ là kênh deposit của luồng 2)

### Luồng 1 — GIẶT ỦI (giữ nguyên như hiện tại)

`Tạo đơn LAUNDRY → reserve ô thường → PIN mở ô gửi đồ → staff collect → giặt → trả ô + PIN mới → khách lấy → release`

### Luồng 2 — NHẬN HÀNG (PARCEL_RECEIVE: shipper/drone bỏ hàng, khách lấy)

```
Kênh người giao (shipper):  đơn type=PARCEL → reserve ô STANDARD → shipper nhập access code → bỏ hàng → occupy → PIN/QR gửi khách
Kênh drone (luồng drone riêng): drone-delivery reserve ô cellType=DRONE (channel=DRONE) → drone thả qua nóc → occupy → PIN/QR gửi khách
Khách: nhập PIN/quét QR tại tủ hoặc app → mở ô → lấy hàng → release → đơn COMPLETED
Quá hạn 24h: scheduler nhắc → tính phí quá giờ → (chính sách) chuyển kho
```

**Điểm tách biệt then chốt: từ lúc hàng nằm trong ô, hai kênh dùng CHUNG một luồng pickup — drone không xuất hiện trong
logic tủ.**

### Luồng 3 — GỬI HÀNG (PARCEL_SEND: khách A gửi, khách B nhận)

`A tạo đơn SEND (nhập SĐT người nhận) → reserve ô → A bỏ hàng bằng PIN → occupy → hệ thống sinh PIN mới gửi B (SMS/notification) → B lấy hàng → release`
*(order-service đã có receiverPhone/receiverName/receiverId — chỉ cần luồng PIN 2 giai đoạn)*

### Luồng 4 — THUÊ TỦ (RENTAL: giữ đồ cá nhân/vali theo giờ-ngày)

`Tạo đơn RENTAL (chọn ô XL hoặc thường, thời hạn) → thanh toán → PIN dùng nhiều lần trong kỳ thuê → hết hạn: nhắc + phí quá giờ + release`
*(tận dụng pickupDeadline + overtime fee sẵn có; khác biệt: PIN không bị xóa sau mỗi lần mở)*

### Luồng 5 — ỦY QUYỀN (DELEGATION: chủ đơn cho người khác lấy hộ)

`Chủ đơn (đang RETURNED/STORING/OCCUPIED) → app: "Ủy quyền" nhập SĐT người nhận hộ → sinh PIN mới + notify cả 2 → người nhận hộ dùng PIN → lịch sử ghi rõ ai mở`

### Luồng phụ — VẬN HÀNH TỦ

- Báo hỏng ô (khách/staff/cảm biến) → `FAULT` + lý do → loại khỏi mọi reserve → event `locker.box.fault` → dashboard
  cảnh báo → kỹ thuật sửa → clear.
- Màn hình cảm ứng tủ: gọi `GET /api/lockers/{id}/layout` để vẽ lưới ô đúng vị trí vật lý (row/col/type/status) — cùng
  API cho web dashboard.

## 4. THIẾT KẾ API MỚI (locker-service)

| API                                                     | Mô tả                                                                                 |
|---------------------------------------------------------|---------------------------------------------------------------------------------------|
| `GET /api/lockers/{id}/layout`                          | Trả lưới ô theo row/col + type + status — cho màn hình tủ & dashboard                 |
| `POST /internal/boxes/{id}/reserve?channel=`            | Reserve có guard: ô DRONE chỉ nhận `channel=DRONE`                                    |
| `POST /internal/boxes/{id}/occupy`                      | RESERVED → OCCUPIED (xác nhận đã bỏ đồ)                                               |
| `POST /api/boxes/{id}/fault`                            | Đánh dấu hỏng + lý do (event RabbitMQ `locker.box.fault`)                             |
| `POST /api/admin/lockers/boxes/{id}/clear-fault`        | Kỹ thuật xác nhận sửa xong (path nằm trong route `/api/admin/lockers/**` của gateway) |
| `GET /internal/lockers/{id}/boxes/find?size=&cellType=` | Tìm ô trống phù hợp (auto-assign)                                                     |

order-service bổ sung: `POST /api/orders/{id}/delegate {phone, note}` — ủy quyền (Luồng 5).

## 5. LỘ TRÌNH THỰC THI

### Phase 1 — Backend luồng tủ (LÀM NGAY trong phiên này) ✅

1. Migration V2 (cột mới) + V3 (seed tủ demo đúng ảnh: 1 tủ landing-pad, 3 ô DRONE hàng 1, 6 ô STANDARD hàng 2–3, 1 ô XL
   vali).
2. Entity/DTO/Repository cập nhật.
3. Service: reserve 2 bước + guard kênh DRONE, occupy, fault/clear-fault + event, layout, find-available.
4. Controller endpoints mới; giữ nguyên hợp đồng cũ với order-service.
5. order-service: endpoint `delegate` (Luồng 5 — tính năng mới đầu tiên ngoài giặt ủi).
6. Build, deploy, test end-to-end qua gateway.

### Phase 2 — Trải nghiệm khách ✅ (hoàn tất 2026-06-13)

- [x] QR code ký số (sinh theo PIN active, verify ở iot-service song song PIN).
- [x] Luồng SEND hoàn chỉnh (PIN 2 giai đoạn + notify người nhận theo SĐT).
- [x] Luồng RENTAL (PIN nhiều lần + tính phí theo giờ/loại ô; extend/end rental).
- [x] Mobile: màn Gửi hàng / Thuê tủ / Đơn tủ của tôi / Manager dashboard / Maintenance queue nối API thật.
- [x] FE dashboard: vẽ layout tủ + utilization + fault alert + maintenance reports.
- [x] RBAC gateway: `/api/manage/**` cho MANAGER/ADMIN, `/api/maintenance/**` cho MAINTENANCE/ADMIN, chặn `/internal/**`
  qua gateway.
- [x] Scheduler reminder quá hạn + cleanup release ô cho đơn đã hoàn tất.

### Phase 3 — Phần cứng & kênh drone (sau khi luồng tủ ổn định)

- Màn hình cảm ứng tủ: build `laundry-locker-frontend/iot/tablet-web` trỏ API layout + verify-pin.
- iot-service: occupy tự động khi cảm biến cửa đóng + có vật (weight/door sensor qua MQTT).
- Vân tay/khuôn mặt: xử lý tại RPi, backend chỉ nhận kết quả verify (ngoài scope đăng ký — làm nếu dư thời gian).
- **Kênh drone cắm vào**: drone-delivery service (kế hoạch riêng trong GAP_ANALYSIS_AND_PLAN.md) chỉ cần gọi
  `reserve?channel=DRONE` + `occupy` — luồng tủ không đổi gì thêm.

## 6. TIÊU CHÍ NGHIỆM THU PHASE 1

- [x] `GET /api/lockers/{id}/layout` trả đúng lưới 3×3 + XL như ảnh thiết kế. *(PASS 2026-06-12)*
- [x] Reserve ô DRONE bằng channel thường → bị từ chối `DRONE_CELL_RESTRICTED`; channel=DRONE → OK. *(PASS)*
- [x] Vòng đời AVAILABLE → RESERVED → OCCUPIED → AVAILABLE chạy đúng; luồng giặt ủi cũ không hỏng — đã test end-to-end:
  tạo đơn → confirm (ô OCCUPIED) → collect (trả ô) → return ô mới (OCCUPIED) → complete (trả ô) → cancel qua PATCH
  /status cũng trả ô. *(PASS)*
- [x] Báo hỏng → ô FAULT, không reserve được, event phát lên RabbitMQ; clear-fault (admin) → AVAILABLE. *(PASS)*
- [x] Ủy quyền đơn → PIN mới + notification, người được ủy quyền lấy đồ được bằng PIN. *(PASS — PIN đổi 979417→609648)*

> Hoàn thiện thêm trong quá trình thực thi: order-service nay gọi `occupy` khi khách confirm bỏ đồ và khi staff trả đồ
> vào ô (đúng mô hình 2 bước RESERVED→OCCUPIED), và `PATCH /api/orders/{id}/status` sang CANCELED/COMPLETED nay tự trả ô (
> vá lỗ hổng ô kẹt RESERVED vĩnh viễn). Tài khoản admin dev: `admin@laundry.test` / `Admin@123456`.
