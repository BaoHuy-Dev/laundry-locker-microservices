# Quy Trình Demo Cho Giảng Viên

Tài liệu hướng dẫn từng bước demo các luồng nghiệp vụ chính trên hệ thống Smart Locker, dành cho giảng viên khi đánh giá hoặc thuyết trình.

---

## 1. Thuê Ô Tủ (RENTAL)

### Kịch bản
Khách hàng thuê ô tủ trong 2 giờ, bỏ đồ vào, thanh toán sau, kết thúc sớm.

### Chuẩn bị
- Tài khoản customer đã đăng nhập trên mobile
- Kiosk đang ở màn hình chờ tại locker demo
- Ô tỉ STANDARD còn trống

### Các bước demo

| Bước | Thao tác | Màn hình / Kết quả mong đợi |
|------|----------|------------------------------|
| 1 | Customer mở app, vào **Home** → bấm **Thuê tủ** | Hiển thị danh sách locker/tủ |
| 2 | Chọn locker demo (CAB-DEMO-01), chọn loại ô **STANDARD**, nhập **2 giờ**, bấm **Thuê ngay** | Order được tạo, hiển thị PIN/QR, trạng thái `INITIALIZED`, chưa có deadline |
| 3 | **(Tùy chọn)** Bấm **Thanh toán ngay** | Chuyển hướng VNPay/MoMo, sau đó trạng thái thành `PAID` |
| 4 | Ra kiosk, bấm **Mở ô** hoặc nhập PIN trên kiosk → chọn đúng mã PIN của đơn | Cửa ô tủ mở ra |
| 5 | Bỏ đồ vào ô, đóng cửa, quay lại mobile bấm **Tôi đã bỏ đồ — bắt đầu kỳ thuê** | Order chuyển `STORING`, `pickupDeadline` hiện ra (hiện tại + 2 giờ), PIN vẫn còn hiệu lực |
| 6 | Trên mobile thấy banner "Hết hạn thuê: 20:00 24/07" kèm cảnh báo phí quá hạn | Banner cảnh báo xuất hiện |
| 7 | **(Nếu chưa thanh toán)** Bấm **Thanh toán** → chọn phương thức → hoàn tất | `paymentStatus` = `PAID` |
| 8 | **(Kết thúc sớm)** Bấm nút **Kết thúc thuê sớm** → xác nhận | Hệ thống cho phép kết thúc, không hoàn tiền giờ thừa |
| 9 | Ra kiosk lấy đồ → mobile bấm xác nhận đã lấy đồ và đóng tủ | Order chuyển `COMPLETED`, ô tủ được release về `AVAILABLE` |

### Lưu ý giảng viên
- Có thể demo **pay-later**: bỏ qua bước 3, thanh toán ở bước 7.
- Có thể demo **hủy đơn**: nếu chưa confirm (vẫn `INITIALIZED`), customer có thể hủy đơn → ô release, PIN hết hiệu lực.
- Deadline tính từ lúc confirm (bước 5), không tính từ lúc tạo đơn.

---

## 2. Gửi Hàng (SEND / C2C)

### Kịch bản
Người gửi (Sender) gửi kiện hàng qua locker cho người nhận (Receiver) ở xa.

### Chuẩn bị
- Tài khoản sender đã đăng nhập
- Kiosk tại locker gửi đang sẵn sàng
- Số điện thoại người nhận (ví dụ: `0900000001`)

### Các bước demo

| Bước | Thao tác | Màn hình / Kết quả mong đợi |
|------|----------|------------------------------|
| 1 | Mở app → **Gửi hàng** → chọn locker gửi | Hiển thị danh sách locker |
| 2 | Nhập số điện thoại người nhận (`0900000001`), tên người nhận, ghi chú (tùy chọn) | Form tạo đơn SEND |
| 3 | Bấm **Tạo đơn gửi** | Backend tìm ô `STANDARD` trống, tạo order `INITIALIZED`, trả PIN/QR |
| 4 | Ra kiosk, nhập PIN (hoặc quét QR) → ô mở ra | Cửa ô mở |
| 5 | Bỏ kiện hàng vào, đóng cửa → mobile bấm **Xác nhận đã bỏ hàng** | Order chuyển `STORING`, PIN được rotate (mới cho receiver) |
| 6 | Hệ thống gửi thông báo chứa PIN mới tới receiver (nếu tìm thấy user) | Receiver nhận notification |
| 7 | **(Receiver)** Mở app → vào chi tiết đơn → thấy PIN/QR mới | PIN đã đổi, PIN cũ hết hiệu lực |
| 8 | Receiver ra locker nhận hàng → nhập PIN → cửa mở, lấy hàng, đóng cửa | |
| 9 | Receiver bấm **Đã nhận hàng** (hoặc pickup-storage) | Order `COMPLETED`, ô về `AVAILABLE` |

### Lưu ý giảng viên
- Có thể demo **Uỷ quyền**: chủ đơn uỷ quyền cho người khác nhận hộ qua `POST /api/orders/{id}/delegate`.
- Nếu receiver chưa có tài khoản, hệ thống vẫn tạo PIN và gửi SMS (nếu có SMS gateway).
- SEND yêu cầu thanh toán trước khi confirm drop (khác với RENTAL).

---

## 3. Báo Lỗi & Bảo Trì (Fault Flow)

### Kịch bản
Customer phát hiện ô tủ bị lỗi → báo cáo → maintenance tiếp nhận, sửa và hoàn tất.

### Chuẩn bị
- Tài khoản customer + tài khoản maintenance
- Một ô tỉ có thể đánh dấu lỗi (hoặc dùng ô đang giữ của customer)

### Các bước demo

| Bước | Thao tác | Màn hình / Kết quả mong đợi |
|------|----------|------------------------------|
| 1 | **(Customer)** Vào chi tiết đơn → bấm **Báo lỗi ô tủ** | Form báo lỗi với lý do |
| 2 | Customer nhập lý do, bấm **Gửi** | Backend đánh dấu ô `FAULT`, tạo `locker_report` `OPEN` |
| 3 | **(Maintenance)** Mở app → tab **Bảo trì** → xem danh sách báo cáo | Thấy report mới, có tên/SĐT khách báo, tên tủ, địa chỉ, SLA |
| 4 | Bấm **Tiếp nhận** trên report đó | Report chuyển `IN_PROGRESS`, gán cho KTV này |
| 5 | Hệ thống gửi thông báo cho customer: "Đã có kỹ thuật viên tiếp nhận" | Customer nhận notification |
| 6 | KTV đến locker, sửa lỗi → bấm **Hoàn tất** trên report | Report chuyển `RESOLVED`, ô về `AVAILABLE` |
| 7 | Hệ thống gửi thông báo cho customer: "Lỗi đã được khắc phục" | Customer nhận notification |
| 8 | **(Customer)** Mở app → vào **Báo cáo của tôi** → đánh giá KTV 1-5 sao | Rating được lưu, KTV xem điểm TB qua `GET /api/maintenance/my-rating-average` |

### Lưu ý giảng viên
- Nếu order còn `INITIALIZED` & `UNPAID`, backend tự động hủy đơn sau khi báo lỗi.
- Có thể demo **Force-open**: KTV mở ô không cần PIN qua `POST /api/maintenance/boxes/{id}/force-open`.
- Có thể demo **Work log**: KTV thêm ghi chú xử lý qua repair_logs.

---

## 4. Bảo Mật Kiosk (Kiosk Security Flow)

### Kịch bản
Kiểm tra các cơ chế bảo mật của kiosk: nhập sai mã, truy cập sai kiosk, timeout, lockout.

### Chuẩn bị
- Kiosk đang ở màn hình chờ
- Một order RENTAL hoặc SEND đã tạo (có PIN)

### Các bước demo

| Bước | Thao tác | Màn hình / Kết quả mong đợi |
|------|----------|------------------------------|
| 1 | Trên kiosk, bấm **Mở ô** → nhập mã PIN sai | Kiosk báo "Mã không hợp lệ, vui lòng thử lại" |
| 2 | Nhập sai PIN nhiều lần (quá ngưỡng lockout) | Kiosk tạm khóa, yêu cầu chờ. Hệ thống ghi audit log `FAILED` |
| 3 | Đợi hết lockout → nhập PIN đúng | Cửa mở thành công |
| 4 | **(Thử mã ở sai kiosk)** Lấy PIN của locker A, nhập tại kiosk locker B | Kiosk báo "Mã không hợp lệ hoặc đã hết hạn", không tiết lộ thông tin ô đích |
| 5 | **(Thử access sai ô)** Nhập PIN đúng ở đúng kiosk, nhưng cố mở ô khác không phải ô của order | IoT service từ chối vì `boxId` không khớp |
| 6 | Hủy đơn RENTAL (còn `INITIALIZED`), sau đó thử mở tủ bằng PIN cũ | PIN hết hiệu lực, kiosk báo "Mã không hợp lệ" |

### Lưu ý giảng viên
- Thông báo lỗi chung chung ("Mã không hợp lệ hoặc đã hết hạn") — không tiết lộ mã đúng/sai hay ô nào.
- PIN không bị consume (có thể mở nhiều lần trong rental).
- Lockout chỉ áp dụng cho box thật (lockout là per-box), mã sai không làm lockout ô không đúng.
- Có thể demo thêm: `POST /api/iot/unlock-with-code` check `request.lockerId == order.lockerId`.

---

## 5. Quản Trị & Bảo Trì (Admin / Maintenance)

### Kịch bản
Admin web quản lý hệ thống; maintenance vận hành locker, drone, lịch bảo trì.

### Chuẩn bị
- Tài khoản ADMIN login web (React)
- Tài khoản MAINTENANCE login mobile

### Các bước demo — Admin Web

| Bước | Thao tác | Kết quả mong đợi |
|------|----------|------------------|
| 1 | Login admin → Dashboard | Thống kê tổng quan |
| 2 | **Quản lý người dùng** → Tạo user mới với role `MANAGER` hoặc `MAINTENANCE` | User có auth_account thật, login được ngay |
| 3 | **Quản lý tủ** → xem layout locker, xem trạng thái từng ô | Sơ đồ tủ trực quan, các ô `AVAILABLE/OCCUPIED/FAULT/OUT_OF_SERVICE` |
| 4 | **Bảo trì** → tạo lịch bảo trì định kỳ cho locker (`maintenance_schedules`) | Lịch hiện ra, có cờ `due` khi đến hạn |
| 5 | **Thông báo** → gửi thông báo broadcast | Customer nhận push notification |

### Các bước demo — Maintenance Mobile

| Bước | Thao tác | Kết quả mong đợi |
|------|----------|------------------|
| 1 | Login MAINTENANCE → mở app | Tab **Bảo trì** hiện danh sách fault/report |
| 2 | Xem chi tiết report → có địa chỉ tủ, bản đồ chỉ đường | Map integration hoạt động |
| 3 | **Quản lý drone** → tab **Drone** → xem 3 drone (IDLE/CHARGING/FAULT) | Badge màu theo trạng thái |
| 4 | Bấm vào drone FAULT → **Nhận xử lý** → đổi trạng thái | Drone chuyển `MAINTENANCE`, ghi log |
| 5 | **Lịch bảo trì** → xem danh sách lịch → đánh dấu đã kiểm tra | `next_due_at` dời lịch |
| 6 | **Box health** → xem tất cả ô trên locker → phát hiện ô cửa mở nhưng không `OCCUPIED` | `needsAttention = true`, banner cảnh báo |

### Lưu ý giảng viên
- `POST /api/admin/users` tạo cả `auth_account` (password login được) — không chỉ profile.
- Role routing: ADMIN → web, TECHNICIAN → technician-home, MAINTENANCE → maintenance-home.
- Drone status là nhập tay (không telemetry thật), có banner cảnh báo.

---

## 6. Phục Hồi Sau Sự Cố (Disaster Recovery)

### Kịch bản
Mô phỏng các tình huống sự cố và cách hệ thống phục hồi.

### 6.1 Mất kết nối IoT (Cabinet offline)

| Bước | Thao tác / Sự kiện | Kết quả mong đợi |
|------|---------------------|------------------|
| 1 | Cabinet mất kết nối MQTT | IoT device chuyển `OFFLINE` (last-seen hết hạn) |
| 2 | Customer thử mở tủ từ mobile | Backend không unlock được → báo "Tủ đang ngoại tuyến, vui lòng thử lại" |
| 3 | Maintenance vào `Box health` → thấy `hwState = null` | Best-effort, không crash UI |
| 4 | Cabinet kết nối lại | MQTT reconnect, device về `ONLINE`, unlock hoạt động trở lại |

### 6.2 Lỗi thanh toán (Payment gateway timeout)

| Bước | Thao tác / Sự kiện | Kết quả mong đợi |
|------|---------------------|------------------|
| 1 | Customer bấm thanh toán, VNPay timeout | Backend trả lỗi checkout, order giữ nguyên `UNPAID` |
| 2 | Customer thử lại | Checkout retry, không tạo duplicate payment |
| 3 | Thành công lần 2 | `paymentStatus = PAID`, không double-charge |

### 6.3 Order-service restart / Flyway migration

| Bước | Thao tác / Sự kiện | Kết quả mong đợi |
|------|---------------------|------------------|
| 1 | Deploy code mới, order-service restart | Các order đang active (INITIALIZED/STORING) giữ nguyên trạng thái |
| 2 | Flyway chạy migration mới | Dữ liệu cũ không bị ảnh hưởng |
| 3 | Customer gọi API sau restart | API hoạt động bình thường, order cũ vẫn access được |

### 6.4 Auto-cancel & dọn dẹp

| Bước | Thao tác / Sự kiện | Kết quả mong đợi |
|------|---------------------|------------------|
| 1 | Tạo order SEND nhưng không confirm sau 24h | Backend sweep auto-cancel, order `CANCELED`, PIN hết hiệu lực |
| 2 | LockerScheduler sweep expired reservations | Ô `RESERVED` quá hạn → release về `AVAILABLE` |
| 3 | Mobile thử mở ô bằng PIN cũ | PIN invalid → "Mã không hợp lệ" |

### 6.5 Notification failure (best-effort)

| Bước | Thao tác / Sự kiện | Kết quả mong đợi |
|------|---------------------|------------------|
| 1 | Tạo đơn thành công nhưng notification-service lỗi | Order vẫn tạo thành công, không rollback |
| 2 | Customer không nhận được push | Vẫn có thể xem order qua polling API |
| 3 | Notification-service hồi phục | Các notification queue được xử lý tiếp (nếu RabbitMQ persistent) |

### Lưu ý giảng viên
- Hệ thống thiết kế **best-effort notification**: không để lỗi push ảnh hưởng đến nghiệp vụ.
- **Stateless credential** (QR được ký HMAC): không cần DB check cho QR decode.
- **Resilience4j circuit breaker**: khi service chết, Feign call fail-fast thay vì treo.
- **Sweeper an toàn** (sweepExpiredReservations) chỉ là lưới phòng hờ — order-service đã tự release ô khi cancel.

---

## Phụ Lục: Tài Khoản Demo

| Vai trò | Email / SĐT | Mật khẩu | Ghi chú |
|---------|-------------|----------|---------|
| Customer | `binhtntse182370@fpt.edu.vn` | `12345678` | Full quyền customer |
| ADMIN | `admin@lockr.test` | `12345678` | Dashboard web |
| MAINTENANCE | (do admin tạo) | (do admin tạo) | Mobile maintenance |
| TECHNICIAN | `tech@lockr.test` | `12345678` | user_id 9006 |

---

## Phụ Lục: API Quan Trọng Cho Demo

| Mục đích | Endpoint |
|----------|----------|
| Tạo đơn thuê | `POST /api/orders/rental` |
| Xác nhận bỏ đồ (rental) | `POST /api/orders/{id}/confirm` |
| Kết thúc thuê | `POST /api/orders/{id}/pickup-storage` |
| Gia hạn thuê | `POST /api/orders/{id}/extend-rental` |
| Hủy đơn | `PUT /api/orders/{id}/cancel` |
| Tạo đơn gửi | `POST /api/orders/send` |
| Mở tủ bằng mã | `POST /api/iot/unlock-with-code` |
| Xác thực PIN/QR | `POST /api/iot/verify-access` |
| Báo lỗi ô tủ | `POST /api/orders/{id}/report-box-fault` |
| Xem báo cáo (maintenance) | `GET /api/maintenance/reports` |
| Force-open | `POST /api/maintenance/boxes/{id}/force-open` |
| Tạo user (admin) | `POST /api/admin/users` |
| Lịch bảo trì | `GET /api/maintenance/schedules` |
| Drone list | `GET /api/maintenance/drones` |
| Box health | `GET /api/maintenance/lockers/{id}/box-health` |

---

> Tài liệu này được cập nhật lần cuối: 2026-07-25. Khi có thay đổi nghiệp vụ hoặc API, cập nhật đồng bộ với `BUSINESS_FLOWS_CURRENT.md` và `PROJECT_PROGRESS_TRACKER.md`.
