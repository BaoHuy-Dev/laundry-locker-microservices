# Flow tests — end-to-end mô phỏng (không cần phần cứng)

Bộ test này chạy các luồng nghiệp vụ thật qua API gateway, với tủ IoT được
**mô phỏng** bằng script MQTT (`smart-locker-iot/simulate_demo_cabinet.py`).

## Vai trò hệ thống (sau đợt gộp role 2026-07-06)

| Role | Kênh | Phạm vi |
|---|---|---|
| `CUSTOMER` | mobile | đặt tủ, thanh toán, mở tủ, báo hỏng, yêu cầu giao drone |
| `ADMIN` | web | dashboard/thống kê, quản lý user/tủ/drone, scheduler |
| `TECHNICIAN` | mobile | bảo trì vật lý tủ (sự cố, ô, định kỳ, bãi đáp) + thiết bị IoT |
| `MAINTENANCE` | mobile | đội bay drone (hàng đợi giao hàng, fleet, pin, nhật ký) |

`MANAGER`/`STAFF` đã bỏ — `/api/manage/**`, `/api/staff/**` không còn tồn tại.

## Chuẩn bị

1. **Backend** (thư mục repo này):

   ```powershell
   mvn.cmd clean package -DskipTests
   docker compose up --build -d        # gateway: http://localhost:18080
   ```

   Tài khoản ADMIN đầu tiên được seed tự động lúc khởi động
   (`admin@lockerly.local` / `Admin@123456` — đổi qua `BOOTSTRAP_ADMIN_*`
   trong `.env`; production bắt buộc override).

2. **Simulator IoT** (repo `smart-locker-iot`):

   ```powershell
   python -u simulate_demo_cabinet.py
   ```

   Mặc định cả iot-service lẫn simulator dùng broker public
   `broker.hivemq.com:1883` → **cần internet**. Muốn chạy broker nội bộ, đặt
   `MQTT_BROKER_URL` cho cả iot-service (docker-compose) và simulator.

3. Đợi ~1–2 phút cho các service đăng ký Eureka (script tự chờ tới 180s).

## Chạy

```powershell
powershell -ExecutionPolicy Bypass -File .\docs\flow-tests\test-flows.ps1
# Luồng kiosk (màn hình tủ mô phỏng — mở tủ bằng PIN/QR không cần JWT):
powershell -ExecutionPolicy Bypass -File .\docs\flow-tests\kiosk-test.ps1
```

Script tự tạo user mới mỗi lần chạy (suffix theo giờ) nên chạy lặp lại được.
Exit code khác 0 khi có check FAIL.

## Các luồng được phủ

- **2.1** Booking SEND/RENTAL: đặt ô → thanh toán ví → xác nhận bỏ đồ → mở tủ
  qua MQTT (simulator trả lời) → nhận đồ → admin xem thống kê/doanh thu.
- **2.2** Báo hư hại: khách báo hỏng ô → TECHNICIAN nhận phiếu/ghi log/hoàn
  tất → khách chấm điểm → admin xem. Kèm kiểm tra RBAC 403 đúng vai.
- **2.3** Giao drone: khách tạo yêu cầu `/api/drone-deliveries` → đội bay
  (MAINTENANCE) điều phối drone trong fleet (IN_FLIGHT) → đã thả hàng
  (DELIVERED, drone về IDLE) → admin xem fleet. Đặt SEND trực tiếp vào ô
  DRONE bị từ chối 400 `DRONE_CELL_RESTRICTED` (đúng thiết kế).
- **2.4** Luồng phụ: hủy, đặt lại, ủy quyền nhận hộ, sai PIN, ngưng
  dùng/vệ sinh/khôi phục ô, mở khẩn cấp (audit MASTER), scheduler.
