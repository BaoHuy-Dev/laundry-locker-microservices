# ĐẶC TẢ LUỒNG NGHIỆP VỤ TỦ KHÓA — PHÂN TÍCH AS-IS + CHUẨN THỰC TẾ (TO-BE)

> Ngày tạo: 2026-06-14
> Loại tài liệu: phân tích + đặc tả (blueprint). KHÔNG phải tài liệu trạng thái runtime.
> Cặp tài liệu sống chuẩn: `docs/PROJECT_PROGRESS_TRACKER.md` + `docs/BUSINESS_FLOWS_CURRENT.md`.
> Tài liệu liên quan: `LOCKER_FLOW_PLAN.md` (kế hoạch luồng tủ Phase 1/2), `GAP_ANALYSIS_AND_PLAN.md` (đối chiếu đề tài DDSLMS).
>
> **Phạm vi:** chỉ luồng **tủ khóa (locker)**. Kênh **drone** chỉ được nhắc tới như một kênh deposit (reserve `channel=DRONE` + occupy); thiết kế drone đầy đủ thuộc tài liệu khác.

## 0. Cách đọc & phương pháp

Tài liệu này được viết bằng cách **đọc trực tiếp source code hiện tại** (không suy luận theo tài liệu cũ). Các file đã đối chiếu:

- `locker-service`: `model/LockerBox.java`, `model/LockerUnit.java`, `model/LockerReport.java`, `service/LockerService.java`, `controller/LockerController.java`, `db/migration/V1..V4`.
- `order-service`: `model/LockerOrder.java`, `service/OrderService.java`, `service/OrderScheduler.java`, `service/QrTokenService.java`, `controller/OrderController.java`, `db/migration/V1..V2`.
- `iot-service`: `service/IotService.java`, `controller/IotController.java`.

Quy ước trạng thái bổ sung (cột "Hiện trạng" ở các bảng):

- `CÓ` — đã có code chạy được.
- `PARTIAL` — có một phần / best-effort / chưa enforce.
- `THIẾU` — chưa có code.

---

# PHẦN A — PHÂN TÍCH LUỒNG TỦ HIỆN TẠI (AS-IS, BÁM CODE)

## A1. Mô hình vật lý & mô hình ô (cell)

Nguồn: `LockerBox.java`, `LockerUnit.java`, `V2__cell_model_upgrade.sql`.

**Locker (`locker_boxes` thuộc về `lockers`):**

| Thực thể | Trường chính | Ý nghĩa |
|---|---|---|
| `LockerUnit` | `id, storeId, code, name, status, address, latitude, longitude, landingPad, landingMarkerId` | Tủ vật lý; `status ∈ {ACTIVE, MAINTENANCE}` (chỉ 2 giá trị qua `setMaintenance`). |
| `LockerBox` (cell) | `id, lockerId, boxNumber, size, cellType, rowIndex, colIndex, status, faultReason, active, description` | Ô trong tủ. |

**`cellType`** (mặc định `STANDARD`):

- `DRONE` — hàng 1, **chỉ** reserve được khi `channel=DRONE` (guard ở `reserveBox`). Khách vẫn **lấy** hàng từ ô này bằng PIN/QR như ô thường.
- `STANDARD` — ô đa dịch vụ.
- `XL` — ô lớn (vali).

**`size`** (mặc định `MEDIUM`) — tồn tại nhưng **không bị enforce khi reserve** (chỉ dùng tùy chọn trong `findAvailableBox`).

## A2. State machine Ô TỦ (chính xác theo `LockerService`)

```
                ┌────────────────────────────────────────────────┐
   reserve      │  occupy (RESERVED|AVAILABLE → OCCUPIED)         │
AVAILABLE ─────────▶ RESERVED ─────────────▶ OCCUPIED            │
   ▲   ▲              │                          │                │
   │   │   release    │ release                  │ release        │
   │   └──────────────┴──────────────────────────┘  (→ AVAILABLE) │
   │                                                               │
   │  clearFault                          markFault (bất kỳ → FAULT)
   └──────────── FAULT ◀──────────────────────────────────────────┘
```

| Hàm | Chuyển | Guard / ghi chú |
|---|---|---|
| `reserveBox(boxId, channel)` | `AVAILABLE → RESERVED` | Chỉ từ `AVAILABLE` (`BOX_NOT_AVAILABLE` nếu khác). Ô `DRONE` cần `channel=DRONE` (`DRONE_CELL_RESTRICTED`). |
| `occupyBox(boxId)` | `RESERVED|AVAILABLE → OCCUPIED` | Cho phép cả từ `AVAILABLE` (best-effort cho legacy). |
| `releaseBox(boxId)` | `* → AVAILABLE` | **Bỏ qua nếu đang `FAULT`** (ô hỏng phải clear-fault, không release thường). |
| `markFault(boxId, reason, userId)` | `* → FAULT` | Set `faultReason`, tạo `LockerReport`, phát event `locker.box.fault`. |
| `clearFault(boxId)` | `FAULT → AVAILABLE` | Xóa `faultReason`. |

**Quan sát quan trọng (as-is):**

- Chỉ có **4 trạng thái ô**: `AVAILABLE / RESERVED / OCCUPIED / FAULT`. **Không có** `EXPIRED`, `OUT_OF_SERVICE`, `CLEANING`, `RESERVED_HOLD` ở cấp ô.
- `EXPIRED` chỉ tồn tại ở **cấp order** (qua `pickupDeadline` + cờ tính trong response), ô vẫn `OCCUPIED` tới khi có lệnh release.
- **Không có TTL cho RESERVED**: nếu order ở `INITIALIZED` mà không confirm, ô vẫn `RESERVED`. `autoCancelUnconfirmedOrders()` đổi order sang `CANCELED` nhưng **không gọi release** ⇒ ô có thể kẹt `RESERVED` (xem [Gap G2]).
- Trạng thái ô là **bản sao best-effort** của order; `occupyBoxQuietly` nuốt lỗi để không vỡ luồng order (comment trong `OrderService`).

## A3. State machine ĐƠN (theo `OrderService`)

`LockerOrder.status` (chuỗi, không phải enum) xuất hiện trong code: `INITIALIZED, STORING, COLLECTED, PROCESSING, READY, RETURNED, COMPLETED, CANCELED`. (Tập `CANCELABLE` còn nhắc `RESERVED, WAITING` nhưng **không có nhánh code nào sinh ra 2 trạng thái này** — di sản cũ.)

`type` / `serviceCategory`: `STORAGE` (mặc định), `SEND` (`PARCEL`), `RENTAL`, `LAUNDRY` (qua `create` tổng quát).

### A3.1 Luồng GIẶT ỦI / xử lý có staff (LAUNDRY/STORAGE đầy đủ)

```
INITIALIZED ──confirm──▶ STORING ──collect──▶ COLLECTED ──process──▶ PROCESSING
                                       │  (updateWeight tại COLLECTED/PROCESSING)
                                       ▼
   COMPLETED ◀──complete/checkout── RETURNED ◀──return(boxId)── READY ◀──ready──┘
```

| Bước | Endpoint | Hành vi ô tủ |
|---|---|---|
| Tạo | `POST /api/orders` | reserve `sendBoxId` (nếu có), PIN1, `INITIALIZED`. |
| Xác nhận bỏ đồ | `PUT /api/orders/{id}/confirm` | `occupy(sendBox)`, `STORING`. |
| Staff thu gom | `PUT /api/orders/{id}/collect` | `release(sendBox)`, `COLLECTED`. |
| Cân/định giá | `PUT /api/orders/{id}/weight` | cập nhật `actualWeight`, tính lại giá. |
| Giặt | `PUT /api/orders/{id}/process` | `PROCESSING`. |
| Xong | `PUT /api/orders/{id}/ready` | `READY`. |
| Trả vào ô | `PUT /api/orders/{id}/return?boxId=` | reserve+occupy `receiveBox`, PIN mới, `returnedAt`, deadline +`pickupHoursLimit`(24h), `RETURNED`. |
| Khách lấy | `PUT /api/orders/{id}/complete` | +phí quá hạn, `release` cả 2 ô, xóa PIN, `COMPLETED`. |
| Staff checkout | `POST /api/orders/{id}/checkout` | tương tự complete (từ `READY/RETURNED/STORING`). |

### A3.2 Luồng SEND (gửi C2C: khách A → khách B) — PIN 2 giai đoạn

Nguồn: `createSend`, `confirm` (nhánh SEND), `notifyParcelReadyForReceiver`, `complete`.

```
A: POST /api/orders/send  (boxId? → tự tìm ô STANDARD AVAILABLE; reserve; PIN1; fee 15000; INITIALIZED)
A: PUT  /confirm          (occupy; PIN1 "chết", sinh PIN2 cho người nhận; deadline +48h;
                           tìm user theo receiverPhone → set receiverId + notify in-app; STORING)
B: PUT  /complete         (owner HOẶC receiver; +phí quá hạn; release; COMPLETED)
```

- PIN2 được notify **in-app** cho receiver **chỉ khi** receiver có tài khoản (tra `getUserByPhone`). Nếu không có account ⇒ chỉ log; PIN nằm trong order của sender, chia sẻ **ngoài luồng** (SMS gateway là điểm tích hợp production — **chưa có**).
- `assertOwnerOrReceiver` cho phép cả owner và receiver gọi complete.
- Deadline SEND = `send-pickup-hours-limit` (mặc định 48h).

### A3.3 Luồng RENTAL (thuê ô theo giờ)

Nguồn: `createRental`, `extendRental`, `pickupStorage`.

```
POST /api/orders/rental    (cellType STANDARD|XL; reserve; giá = rate×hours; deadline +hours; INITIALIZED)
PUT  /confirm              (occupy; STORING; PIN dùng nhiều lần)
POST /{id}/extend-rental   (hours 1..720; cộng deadline + cộng phí theo rate×hours)
POST /{id}/pickup-storage  (kết thúc; +phí quá hạn; release; COMPLETED)
```

- `DRONE` không cho thuê (`DRONE_CELL_RESTRICTED`).
- Giá: `STANDARD` 5000đ/h, `XL` 10000đ/h (cấu hình `app.order.rental-rate-*`).
- PIN rental **không bị consume** sau mỗi lần mở (dùng lại trong kỳ thuê).

### A3.4 Luồng ỦY QUYỀN (delegate)

Nguồn: `delegate`. Chỉ khi `STORING|RETURNED`: sinh PIN mới, set `receiverPhone/name`, ghi history, notify **owner** (không notify người được ủy quyền vì có thể chưa có account). QR cũ tự vô hiệu (đổi PIN).

### A3.5 Báo lỗi & bảo trì (`locker-service`)

```
Khách/staff: POST /api/boxes/{id}/fault → markFault → FAULT + LockerReport(OPEN) + event
Maintenance: GET /api/maintenance/faults | /reports?mine= 
             PUT /reports/{id}/claim   (OPEN → IN_PROGRESS, gán assignee)
             PUT /reports/{id}/resolve (RESOLVED + tự clearFault ô → AVAILABLE)
             POST /maintenance/boxes/{id}/clear-fault
Admin:       GET /api/admin/lockers/reports | PUT .../resolve | POST .../clear-fault
Manager:     GET /api/manage/lockers/reports | /stats | /{id}/layout
```

Report status: `OPEN → IN_PROGRESS → RESOLVED`. Không có `REJECTED/DUPLICATE/CANNOT_REPRODUCE`, không có lịch bảo trì định kỳ, không có work-log nhiều bước.

### A3.6 Truy cập PIN/QR & IoT

- **PIN**: 6 chữ số ngẫu nhiên (`SecureRandom`). `resetPin` cấp PIN mới.
- **QR**: `QrTokenService` — `LLQR.<orderId-base36>.<HMAC_SHA256(orderId:pin)-base64url>`. **Stateless, ký số, gắn với PIN hiện tại** ⇒ đổi PIN (delegate/reset/SEND handover) làm vô hiệu mọi QR cũ.
- **Resolve access**: `getByAccess(code)` — nếu bắt đầu `LLQR.` thì verify QR, ngược lại tra theo PIN. Expose qua `GET /api/orders/access/{code}` và `GET /internal/orders/by-access`.
- **IoT verify**: `POST /api/iot/verify-access {boxId, pinCode}` → `IotService.verifyAccess` tra order theo code rồi kiểm `boxId == sendBoxId || receiveBoxId`. `POST /api/iot/unlock` → verify + `LockerMqttService.sendUnlockCommandAsync` (chờ 20s) + `openBox` (event `locker.box.opened`). `POST /api/iot/pickup` → gọi order `complete`.

### A3.7 Scheduler & phí quá hạn

- `OrderScheduler.remindOverduePickups` — `fixedDelay` 10 phút: quét `RETURNED`+`STORING` quá `pickupDeadline`, notify owner (+receiver), cooldown 60 phút.
- `OrderScheduler.releaseCompletedBoxes` — cron `0 15 3 * * *`: release ô của đơn `COMPLETED` (lưới an toàn cho client legacy dùng `PATCH /status`).
- `autoCancelUnconfirmedOrders` — **chỉ chạy khi gọi tay** `POST /api/admin/scheduler/auto-cancel`; đổi `INITIALIZED` >24h sang `CANCELED` nhưng **KHÔNG release ô** và **KHÔNG được @Scheduled**.
- Phí quá hạn: `calculatePickupOvertimeFee` — `overtimeFeePerHour`(500) × số giờ trễ, chặn trần `maxOvertimeFee`(50000) và `maxOvertimePercent`(50% tổng đơn).

## A4. Bảng kiểm kê chức năng tủ HIỆN CÓ

| # | Chức năng | Endpoint chính | Hiện trạng |
|---|---|---|---|
| 1 | Tạo/sửa/xóa locker, set maintenance | `/api/admin/lockers/**` | CÓ |
| 2 | Tạo ô, layout lưới, list/available | `/api/lockers/{id}/layout`, `/api/lockers/{id}/boxes` | CÓ |
| 3 | Reserve/occupy/release (internal) | `/internal/boxes/{id}/{reserve|occupy|release}` | CÓ |
| 4 | Guard ô DRONE theo channel | `reserveBox` | CÓ |
| 5 | Tìm ô trống auto-assign | `/internal/lockers/{id}/boxes/find` | CÓ |
| 6 | Stats/utilization theo tủ | `/api/manage/lockers/stats` | CÓ |
| 7 | Đơn LAUNDRY full lifecycle | `/api/orders/**` | CÓ |
| 8 | SEND C2C, PIN 2 giai đoạn | `/api/orders/send` + confirm/complete | CÓ |
| 9 | RENTAL theo giờ + extend/end | `/api/orders/rental`, `/extend-rental`, `/pickup-storage` | CÓ |
| 10 | Ủy quyền nhận hộ | `/api/orders/{id}/delegate` | CÓ |
| 11 | PIN + QR ký số, đổi PIN vô hiệu QR | `QrTokenService`, `/api/orders/access/{code}` | CÓ |
| 12 | IoT verify-access + unlock (MQTT) | `/api/iot/verify-access`, `/unlock` | CÓ |
| 13 | Báo lỗi → FAULT + report | `/api/boxes/{id}/fault` | CÓ |
| 14 | Maintenance claim/resolve/clear | `/api/maintenance/**` | CÓ |
| 15 | Nhắc quá hạn + phí quá hạn | scheduler + `calculatePickupOvertimeFee` | CÓ |
| 16 | Release ô sau hoàn tất (sweep đêm) | cron | CÓ |
| 17 | Thanh toán đơn (cash/VNPay/MoMo) | `payment-service` | PARTIAL (chưa gate vào luồng tủ) |
| 18 | Auto-cancel đơn chưa confirm | `/api/admin/scheduler/auto-cancel` | PARTIAL (gọi tay, không release ô) |

---

# PHẦN B — LUỒNG NGHIỆP VỤ TỦ KHÓA CHUẨN THỰC TẾ (TO-BE, ĐẦY ĐỦ)

Phần này liệt kê **toàn bộ** các luồng/nghiệp vụ mà một hệ thống smart locker vận hành thực tế cần có (tham chiếu các mô hình thị trường: tủ giặt ủi, tủ bưu kiện last-mile, tủ click-&-collect, tủ trả hàng e-commerce, tủ cho thuê). Mỗi mục ghi rõ **mục tiêu, actor, các bước, trạng thái, điểm khác biệt so với as-is**.

## B1. Khám phá tủ & Giữ chỗ (Discovery & Reservation Hold)

**Mục tiêu:** khách chọn đúng tủ/ô còn trống theo vị trí, kích thước, dịch vụ; giữ chỗ có thời hạn.

1. Tìm tủ theo vị trí (gần tôi), theo store, theo dịch vụ hỗ trợ, theo còn ô trống loại X.
2. Hiển thị **sức chứa thời gian thực** theo `cellType` + `size` (đang có `layout`/`stats`, nhưng chưa lọc theo size khả dụng).
3. **Giữ chỗ có TTL**: khi khách bắt đầu đơn, ô chuyển `RESERVED` kèm `reserved_until`; quá hạn (vd 10–15 phút) **tự release**. *(as-is: RESERVED không có TTL — [Gap G2]).*
4. Quy tắc **chống reserve trùng**: 1 ô = 1 đơn active (đã đúng), thêm khóa lạc quan/`@Version` để tránh race khi 2 khách cùng chọn 1 ô.
5. Chọn ô **đúng kích thước**: enforce `size` (S/M/L/XL) khi reserve, fallback ô lớn hơn nếu hết. *(as-is: size không enforce — [Gap G9]).*

## B2. Gửi đồ giặt (LAUNDRY) — đã có, bổ sung

Giữ nguyên lifecycle A3.1. Bổ sung chuẩn:

- Định giá theo **catalog dịch vụ thật** (`laundry-service` đang thiếu source) thay vì phí cố định 5000/đơn vị.
- **Pickup tận nơi / trả tận nơi** (tùy chọn) — hiện chỉ qua tủ.
- **Ảnh tình trạng đồ** lúc thu gom/trả (bằng chứng tranh chấp).
- Thông báo từng mốc (đã thu gom / đang giặt / đã trả vào ô) — đã có notify theo status, nên chuẩn hóa template.

## B3. Gửi bưu kiện C2C (SEND) — đã có, bổ sung

Giữ nguyên A3.2. Bổ sung chuẩn:

- **Gửi OTP/PIN cho người nhận qua SMS/email thật** (không chỉ in-app). *(as-is chỉ in-app nếu có account — [Gap G13]).*
- **Ảnh bằng chứng đã bỏ hàng** (sender) và **đã lấy hàng** (receiver).
- **COD / thu hộ** (tùy chọn): receiver thanh toán trước khi PIN mở.
- **Từ chối/hoàn gửi**: receiver từ chối → quy trình trả lại sender / chuyển kho.
- **Kích thước & cân nặng khai báo** để chọn ô đúng và tính phí.

## B4. NHẬN HÀNG QUA SHIPPER/COURIER (PARCEL_RECEIVE) — **THIẾU**

**Đây là luồng tủ bưu kiện last-mile kinh điển và hiện CHƯA có code** (`LOCKER_FLOW_PLAN` Luồng 2 mới mô tả, chưa implement).

**Mục tiêu:** shipper/đơn vị giao bỏ hàng vào ô đã được hệ thống chỉ định; khách lấy bằng PIN/QR.

```
Hệ thống/đơn e-com tạo đơn type=PARCEL_RECEIVE (gắn khách nhận, locker, size)
   → reserve ô STANDARD/đúng size  → sinh "courier access code" (mã shipper) + "customer pickup PIN/QR"
Shipper tới tủ → nhập courier code tại cabinet → mở ô → bỏ hàng → đóng cửa → occupy
   → hệ thống notify khách: PIN/QR + deadline
Khách: PIN/QR tại tủ/app → mở ô → lấy → release → DELIVERED
Quá hạn: nhắc → phí lưu kho → (chính sách) chuyển kho / trả người gửi
```

**Khác biệt với SEND:** người bỏ hàng là **shipper (không phải app-user A)**, cần **mã truy cập riêng cho shipper** tách biệt PIN của khách. Cần endpoint `deposit` cho cabinet/shipper và mã 2 chiều (courier code ≠ customer PIN).

## B5. Thuê tủ (RENTAL) — đã có, bổ sung

Giữ nguyên A3.3. Bổ sung chuẩn: gói thuê theo **ngày/tuần**, gia hạn tự động (auto-renew), đặt trước theo lịch (booking khung giờ), chính sách cọc.

## B6. Ủy quyền (DELEGATION) — đã có, bổ sung

Giữ nguyên A3.4. Bổ sung: notify **cả người được ủy quyền** qua SMS/email; giới hạn số lần ủy quyền; thu hồi ủy quyền (revoke).

## B7. Trả hàng / Reverse logistics (RETURN_DROP) — **THIẾU**

**Mục tiêu:** khách trả hàng e-commerce/đồ thuê vào tủ, đơn vị vận chuyển gom sau.

```
Khách tạo đơn RETURN (mã RMA) → reserve ô → PIN bỏ hàng → occupy
   → courier gom: courier code mở ô → lấy hàng → release → RETURNED_TO_MERCHANT
```

Tận dụng được hạ tầng SEND/PARCEL_RECEIVE đảo chiều. Hiện **chưa có**.

## B8. Thanh toán đầy đủ (Payment lifecycle) — PARTIAL

Chuẩn thực tế cho tủ:

1. **Trả trước (prepaid)**: SEND/RENTAL nên thu phí **trước khi** cấp quyền bỏ đồ (hiện `paymentRequired=true` ở `INITIALIZED` nhưng **không chặn** confirm/occupy — [Gap G5]).
2. **Tạm giữ (authorization hold)** cho rental/đặt cọc, **quyết toán** khi kết thúc (cộng phí phát sinh/quá hạn).
3. **Thu phí quá hạn thực tế**: hiện chỉ **cộng dồn vào totalPrice**, không có bước thu tiền/khóa pickup tới khi trả phí.
4. **Hoàn tiền (refund)**: hủy sau khi đã trả phí, hủy một phần, tranh chấp.
5. **Hóa đơn/biên nhận**, lịch sử giao dịch theo tủ/đơn.
6. Tích hợp `payment.completed` event để **mở khóa pickup** (gate).

## B9. Vòng đời ô đầy đủ (Full cell lifecycle) — PARTIAL

Bổ sung trạng thái ô chuẩn (mở rộng 4 trạng thái as-is):

| Trạng thái | Ý nghĩa | As-is |
|---|---|---|
| `AVAILABLE` | Trống, sẵn sàng | CÓ |
| `RESERVED` | Đã giữ chỗ (thêm `reserved_until` TTL) | CÓ (thiếu TTL) |
| `OCCUPIED` | Có đồ | CÓ |
| `EXPIRED` | Quá hạn lấy, chờ xử lý tồn (chuyển kho/hủy) | THIẾU (đang ở cấp order) |
| `FAULT` | Hỏng, chờ sửa | CÓ |
| `OUT_OF_SERVICE` | Ngừng dùng có chủ đích (bảo trì/đóng) | THIẾU |
| `CLEANING` | Đang vệ sinh/khử khuẩn | THIẾU |

Cần: chuyển `OCCUPIED → EXPIRED` tự động khi quá hạn; quy trình thoát `EXPIRED` (nhân viên mở ô, chuyển kho, chụp ảnh, đóng đơn).

## B10. Quá hạn & xử lý tồn (Overstay & Abandoned items) — PARTIAL/THIẾU

1. Grace period sau deadline (đã có deadline + nhắc).
2. Nấc leo thang: nhắc lần 1/2/3 → phí lưu kho lũy tiến → **chuyển kho (move-to-storage)** giải phóng ô → **trả người gửi / tiêu hủy** theo chính sách. *(as-is: chỉ nhắc + cộng phí, ô không bao giờ tự giải phóng — [Gap G3]).*
3. Báo cáo hàng tồn cho vận hành.

## B11. Vận hành tủ (Operations) — PARTIAL/THIẾU

- **Tiếp/lấy hàng hàng loạt** cho nhân viên (mở nhiều ô theo phiên).
- **Mở ô thủ công/cưỡng bức (force-open / master code)** có ghi log lý do + actor (an toàn & audit).
- **Vệ sinh định kỳ** → `CLEANING`.
- **Kiểm kê (audit)**: đối chiếu trạng thái ô vật lý vs hệ thống.
- **Đóng/mở tủ theo giờ hoạt động store** (operating hours).

## B12. Bảo trì (Maintenance) — PARTIAL

As-is có fault→claim→resolve. Bổ sung chuẩn:

- **Bảo trì phòng ngừa** theo thời gian/tần suất mở ô (preventive schedule).
- **Work-log nhiều bước**, vật tư thay thế, ảnh hiện trường.
- **Role TECHNICIAN** chuyên biệt (hiện gộp trong MAINTENANCE).
- `OUT_OF_SERVICE` cho ô/tủ khi bảo trì; loại khỏi mọi reserve.
- SLA & cảnh báo quá hạn xử lý fault.

## B13. Phương thức truy cập (Access methods) — PARTIAL

| Phương thức | As-is | Bổ sung |
|---|---|---|
| PIN 6 số | CÓ | hết hạn theo deadline, giới hạn số lần thử (brute-force lockout) |
| QR ký số | CÓ | thêm hạn dùng/nonce |
| App remote unlock | PARTIAL (`/api/iot/unlock` có, chưa là luồng mobile chính) | nút "Mở ô" trong app khi đứng trước tủ |
| BLE/NFC | THIẾU | mở ô khi không có mạng |
| Vân tay/khuôn mặt | THIẾU (xử lý tại RPi, backend nhận kết quả) | optional |
| Master/override code | THIẾU | cho vận hành/kỹ thuật, có audit |

## B14. Thông báo & nhắc lịch (Notifications) — PARTIAL

Chuẩn hóa **đa kênh** (in-app + push FCM + SMS + email) cho: cấp PIN/QR, sắp hết hạn, quá hạn, phí phát sinh, ô hỏng, ủy quyền, hoàn tất. *(as-is: chủ yếu in-app, SMS/email thật chưa nối).*

## B15. Màn hình cảm ứng tủ (Cabinet touchscreen / tablet-web) — THIẾU

Luồng tại tủ: chọn "Bỏ đồ / Lấy đồ" → nhập PIN hoặc quét QR → `verify-access` → mở ô → xác nhận đóng. Backend đã có `verify-access`/`unlock`; **thiếu UI thiết bị** (`tablet-web`).

## B16. Cảm biến IoT & đồng bộ trạng thái vật lý — THIẾU

- Cảm biến **cửa đóng/mở** + **trọng lượng/quang** → tự `occupy` khi có đồ, tự phát hiện **ô trống nhưng hệ thống nghĩ OCCUPIED** (drift).
- **Chống giả mạo (tamper)**, mất điện, nhiệt độ, heartbeat thiết bị → cảnh báo.
- Đối soát định kỳ trạng thái cảm biến vs DB.

## B17. Báo cáo & Dashboard vận hành — PARTIAL

Bổ sung: tỉ lệ sử dụng theo giờ/ngày, doanh thu theo tủ/dịch vụ, thời gian lưu trung bình, tỉ lệ quá hạn, SLA fault, heatmap ô hay hỏng.

## B18. Edge cases & xử lý lỗi (bắt buộc cho production)

- **Mở cửa lỗi phần cứng** (`unlock` trả FAILED/timeout) → không đổi trạng thái pickup; cho thử lại; cảnh báo.
- **Drift trạng thái** ô↔order: occupy/release best-effort hiện nuốt lỗi ([Gap G10]) → cần job đối soát + idempotency.
- **Double-pickup / nhầm ô**: kiểm tra `boxId` đã có (verify-access), thêm khóa lần mở, ghi audit từng lần mở.
- **Mất điện/khởi động lại** giữa lúc mở ô.
- **Hủy sau khi đã bỏ đồ**: chính sách hoàn/định phí (hiện chỉ hủy được trước `STORING`).

## B19. Bảo mật & Audit (Access audit) — PARTIAL

- **Nhật ký từng lần mở ô**: ai (actor), bằng chứng gì (PIN/QR/master), ô nào, lúc nào, kết quả. *(as-is chỉ có event `locker.box.opened` không kèm actor/credential — [Gap G11]).*
- RBAC đầy đủ; rà các POST public.
- Mã hóa/at-rest cho PIN nếu lưu lâu dài.

## B20. Đa tủ, ghép kích thước, tràn ô (Multi-locker & overflow) — THIẾU

- 1 đơn nhiều ô (đồ cồng kềnh).
- Chuyển ô (relocation) khi ô hỏng giữa chừng.
- Lấy chéo tủ (cross-locker) khi cần.
- Điều phối tràn ô sang tủ lân cận.

---

# PHẦN C — GAP MAP & BACKLOG ƯU TIÊN

Ưu tiên: **P0** = sửa lỗ hổng/đúng đắn luồng đang chạy; **P1** = hoàn thiện luồng chuẩn cốt lõi; **P2** = nâng cao vận hành; **P3** = mở rộng.

| ID | Gap | Mô tả ngắn | Ưu tiên | Service ảnh hưởng | Cần migration? |
|---|---|---|---|---|---|
| G1 | Auto-cancel chưa @Scheduled | `autoCancelUnconfirmedOrders` chỉ gọi tay | P0 | order | Không |
| G2 | RESERVED không TTL + không release khi auto-cancel | ô kẹt `RESERVED` vĩnh viễn | P0 | order, locker | Có (`reserved_until`) |
| G3 | Quá hạn không giải phóng ô | `OCCUPIED` mãi tới khi complete tay; chưa có `EXPIRED`/move-to-storage | P0/P1 | order, locker | Có (cell `EXPIRED`) |
| G4 | Drift trạng thái ô↔order | occupy/release best-effort nuốt lỗi, không đối soát | P1 | order, locker, iot | Không |
| G5 | Thanh toán không gate | `paymentRequired` không chặn confirm/pickup | P1 | order, payment | Không |
| G6 | PARCEL_RECEIVE (shipper deposit) | luồng nhận hàng courier chưa có | P1 | order, iot, locker | Có (courier code) |
| G7 | RETURN_DROP (reverse logistics) | trả hàng e-com chưa có | P2 | order | Có |
| G8 | Cabinet tablet-web UI | thiếu UX tại tủ | P2 | FE iot | Không |
| G9 | Size không enforce khi reserve | dễ gán sai ô | P1 | locker, order | Không |
| G10 | Sensor occupy/release tự động | chưa nối cảm biến cửa/trọng lượng | P2 | iot, locker | Không |
| G11 | Audit từng lần mở ô | thiếu actor/credential log | P1 | locker, iot | Có (bảng access_log) |
| G12 | Cell `OUT_OF_SERVICE`/`CLEANING` + preventive maintenance | bảo trì phòng ngừa/role technician | P2 | locker | Có |
| G13 | SMS/email OTP thật cho receiver | chỉ in-app | P1 | notification | Không |
| G14 | Brute-force PIN lockout | chưa giới hạn số lần thử | P1 | iot, order | Không |
| G15 | Hoàn tiền/hủy sau deposit | chính sách refund/cancel | P2 | order, payment | Không |
| G16 | Đa ô/relocation/overflow | 1 đơn nhiều ô | P3 | order, locker | Có |

---

# PHẦN D — ĐỀ XUẤT THAY ĐỔI DATA MODEL & API (để implement sau)

> Đây là đề xuất, **chưa triển khai**. Migration **thêm mới** (không sửa V1..V4 đã chạy), theo quy tắc Flyway của dự án.

## D1. locker-service

- `locker_boxes`: thêm `reserved_until TIMESTAMP`, `reserved_order_id BIGINT`, mở rộng `status` cho `EXPIRED/OUT_OF_SERVICE/CLEANING`.
- Bảng mới `box_access_logs(id, box_id, order_id, actor_user_id, credential_type, result, opened_at)` cho audit (G11).
- Bảng mới `maintenance_schedules` + `repair_logs` (G12).
- API mới: `POST /internal/boxes/{id}/reserve` thêm `reserved_until`; job release RESERVED quá hạn; `POST /api/maintenance/boxes/{id}/out-of-service`.

## D2. order-service

- Thêm order `type`: `PARCEL_RECEIVE`, `RETURN`. Thêm `status` `EXPIRED`, `MOVED_TO_STORAGE`, `RETURNED_TO_MERCHANT`.
- Trường `courier_access_code` (tách với `pin_code`) cho B4/B7.
- `@Scheduled` cho auto-cancel (G1) + job chuyển `EXPIRED`/move-to-storage (G3).
- Gate thanh toán: chặn `confirm`/pickup nếu chưa `payment.completed` (G5).
- `@Version` optimistic lock chống race reserve (B1).

## D3. iot-service

- `POST /api/iot/deposit {boxId, courierCode}` cho shipper bỏ hàng (B4).
- Brute-force lockout theo `boxId` (G14).
- Listener cảm biến cửa/trọng lượng → occupy/release + đối soát (G10).

---

# PHẦN E — LỘ TRÌNH IMPLEMENT (mỗi giai đoạn = 1 branch riêng)

| Giai đoạn | Nội dung | Gap đóng | Nhánh đề xuất |
|---|---|---|---|
| L1 — Vá đúng đắn luồng | Auto-cancel @Scheduled + release ô; RESERVED TTL; đối soát drift | G1,G2,G4 | `fix/locker-reservation-ttl-and-release` |
| L2 — Vòng đời & tồn | Cell `EXPIRED`/move-to-storage; enforce size | G3,G9 | `feat/locker-cell-lifecycle-expiry` |
| L3 — Nhận hàng courier | PARCEL_RECEIVE + courier code + cabinet deposit | G6 | `feat/locker-parcel-receive` |
| L4 — Thanh toán & audit | Gate payment; access log; SMS/email OTP; PIN lockout | G5,G11,G13,G14 | `feat/locker-payment-gate-and-audit` |
| L5 — Vận hành & bảo trì | OUT_OF_SERVICE/CLEANING; preventive maintenance; technician | G12 | `feat/locker-maintenance-ops` |
| L6 — Thiết bị & cảm biến | tablet-web cabinet UI; sensor occupy/release | G8,G10 | `feat/locker-cabinet-and-sensors` |
| L7 — Mở rộng | RETURN_DROP; refund; đa ô/relocation | G7,G15,G16 | `feat/locker-returns-and-multicell` |

> Mỗi giai đoạn phải: làm trên nhánh riêng, thêm migration mới (không sửa cũ), cập nhật `BUSINESS_FLOWS_CURRENT.md` + `PROJECT_PROGRESS_TRACKER.md` + mirror, có test (`mvn -pl <service> -am test`), và verify trước khi merge.

---

## Phụ lục — Tham chiếu nhanh code (as-is)

| Chủ đề | File |
|---|---|
| Cell entity & trạng thái | `locker-service/.../model/LockerBox.java` |
| Reserve/occupy/release/fault/layout | `locker-service/.../service/LockerService.java` |
| Endpoint locker/manage/maintenance | `locker-service/.../controller/LockerController.java` |
| Order entity & trường | `order-service/.../model/LockerOrder.java` |
| SEND/RENTAL/LAUNDRY/delegate/overtime | `order-service/.../service/OrderService.java` |
| Scheduler nhắc/release | `order-service/.../service/OrderScheduler.java` |
| QR ký số | `order-service/.../service/QrTokenService.java` |
| Verify-access/unlock/pickup | `iot-service/.../service/IotService.java` |
| Migration cell model / maintenance | `locker-service/.../db/migration/V2,V4` |
