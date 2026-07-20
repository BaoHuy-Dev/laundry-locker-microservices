# Drone Precision Landing — Raspberry Pi 5 + GS Camera

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho
> locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng
> Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`,
`RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

SSH vào Pi:

```
host: binh
pw:   binh123 || binh12345678
```

---

## Mô tả hệ thống

Camera **Raspberry Pi Global Shutter (IMX296)** gắn dưới drone nhìn xuống đất.  
Script đọc **ArUco marker** đặt tại bãi đỗ locker, tính toán:

| Thông số              | Ý nghĩa                                   |
|-----------------------|-------------------------------------------|
| `altitude`            | Khoảng cách thẳng đứng drone → marker (m) |
| `x_offset / y_offset` | Độ lệch ngang trái/phải, trước/sau (m)    |
| `radius`              | √(x²+y²) — bán kính lệch tâm (m)          |
| `angle_x / angle_y`   | Góc lệch (rad) gửi lên FC qua MAVLink     |
| `can_land`            | `True` nếu trong vùng đáp an toàn         |

### Điều kiện cho phép hạ cánh (`can_land = True`)

- `altitude  ≤ ALTITUDE_LAND_THRESHOLD_M` (mặc định **0.30 m**)
- `radius    ≤ RADIUS_LAND_THRESHOLD_M` (mặc định **0.15 m**)

---

## Cấu trúc file

```
drone/
├── main.py                  # Entry point, chạy file này
├── config.py                # Tất cả cấu hình (camera, ngưỡng, MAVLink)
├── aruco_detector.py        # Phát hiện marker + tính pose 6-DOF
├── mavlink_comm.py          # Gửi LANDING_TARGET tới Pixhawk/CubeOrange
├── camera_calibration.py    # Hiệu chỉnh camera (chạy 1 lần đầu)
├── requirements.txt
└── calibration_result.npz   # (tự sinh sau khi calibrate)
```

---

## Setup lần đầu trên Pi

```bash
# 1. Cài dependencies
pip install -r requirements.txt

# 2. Hiệu chỉnh camera (quan trọng, ảnh hưởng tới độ chính xác)
python camera_calibration.py --collect   # chụp ~25 ảnh checkerboard
python camera_calibration.py --calibrate # tính camera matrix
# → Dán kết quả in ra vào config.py

# 3. Chạy hệ thống
python main.py                           # kết nối FC qua UART
python main.py --no-mavlink              # chỉ test camera, không cần FC
python main.py --preview                 # hiện cửa sổ debug (cần màn hình/VNC)
```

---

## Cấu hình quan trọng trong `config.py`

```python
ARUCO_MARKER_SIZE_M      = 0.30   # Kích thước marker thực (mét)
ALTITUDE_LAND_THRESHOLD_M = 0.30  # Cao ≤ 30cm → đáp được
RADIUS_LAND_THRESHOLD_M   = 0.15  # Lệch ≤ 15cm → đáp được
MAVLINK_CONNECTION        = "/dev/ttyAMA0"  # UART Pi5 GPIO14/15
MAVLINK_BAUD              = 57600
```

---

## Cấu hình Flight Controller (ArduCopter)

| Parameter           | Giá trị | Mô tả                       |
|---------------------|---------|-----------------------------|
| `PLND_ENABLED`      | `1`     | Bật Precision Landing       |
| `PLND_TYPE`         | `1`     | Companion Computer          |
| `PLND_EST_TYPE`     | `0`     | Raw sensor (không dùng EKF) |
| `PLND_LAND_OFS_X/Y` | `0`     | Không offset                |

---

## Sơ đồ pipeline

```
[GS Camera IMX296]
      │  frame BGR
      ▼
[aruco_detector.py]
  ├─ cv2.aruco.ArucoDetector  → tìm marker
  ├─ cv2.solvePnP             → tính pose (rvec, tvec)
  ├─ tvec[2] = altitude (m)
  ├─ radius  = √(x²+y²)
  └─ can_land = alt≤0.3m AND radius≤0.15m
      │
      ▼  LandingResult
[mavlink_comm.py]
  └─ LANDING_TARGET (#149) → [Pixhawk/CubeOrange] → Precision Land
```
