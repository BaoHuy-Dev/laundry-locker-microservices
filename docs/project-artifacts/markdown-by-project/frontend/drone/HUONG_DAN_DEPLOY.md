# Hướng dẫn Deploy Code lên Raspberry Pi 5

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho
> locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng
> Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`,
`RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

> Công cụ: **WinSCP** (upload file) + **PuTTY** (chạy lệnh SSH)

---

## BƯỚC 1 — Tìm IP của Raspberry Pi

Trước khi làm bất cứ điều gì, cần biết IP của Pi trong mạng LAN.

**Cách 1:** Cắm màn hình + bàn phím vào Pi, mở Terminal rồi gõ:

```bash
hostname -I
```

→ Ví dụ kết quả: `192.168.1.105`

**Cách 2:** Vào trang quản lý router (thường là `192.168.1.1`),
tìm thiết bị tên `raspberrypi` hoặc `binh`.

> Ghi nhớ IP này, dùng xuyên suốt các bước dưới.

---

## BƯỚC 2 — Kết nối WinSCP (Upload file)

1. Mở **WinSCP**
2. Hộp thoại **Login** hiện ra → điền:

   | Trường        | Giá trị                         |
      | ------------- | ------------------------------- |
   | File protocol | **SFTP**                        |
   | Host name     | IP của Pi (VD: `192.168.1.105`) |
   | Port number   | `22`                            |
   | User name     | `binh`                          |
   | Password      | `binh123`                       |

3. Nhấn **Login**
4. Lần đầu kết nối sẽ hỏi xác nhận host key → nhấn **Accept**

---

## BƯỚC 3 — Upload thư mục `drone/` lên Pi

Sau khi WinSCP kết nối thành công:

1. **Cửa sổ trái** = máy Windows của bạn  
   → Điều hướng tới:  
   `D:\capstone-laundry-locker\laundry-locker-frontend\drone\`

2. **Cửa sổ phải** = Raspberry Pi  
   → Điều hướng tới thư mục home:  
   `/home/binh/`

3. **Tạo thư mục mới** trên Pi (cửa sổ phải):  
   Chuột phải → **New → Directory** → đặt tên `drone`

4. Mở thư mục `drone` vừa tạo trên Pi

5. **Chọn tất cả file** ở cửa sổ trái (Ctrl+A) → kéo thả sang cửa sổ phải  
   hoặc nhấn **F5 (Copy)**

6. Chờ upload xong — các file cần có:

   ```
   /home/binh/drone/
   ├── main.py
   ├── config.py
   ├── aruco_detector.py
   ├── mavlink_comm.py
   ├── camera_calibration.py
   ├── generate_markers.py
   └── requirements.txt
   ```

---

## BƯỚC 4 — Kết nối PuTTY (SSH vào Pi)

1. Mở **PuTTY**
2. Điền vào ô **Host Name (or IP address)**:  
   IP của Pi (VD: `192.168.1.105`)
3. **Port**: `22`
4. **Connection type**: SSH
5. _(Tuỳ chọn)_ Nhập tên vào **Saved Sessions** → nhấn **Save** để lưu dùng lại sau
6. Nhấn **Open**
7. Cửa sổ terminal đen hiện ra → đăng nhập:
   ```
   login as: binh
   password: binh123
   ```
   _(Gõ password sẽ không hiện ký tự — bình thường)_

---

## BƯỚC 5 — Cài dependencies trên Pi

Trong cửa sổ PuTTY, gõ lần lượt:

```bash
# Di chuyển vào thư mục vừa upload
cd /home/binh/drone

# Cài thư viện Python cần thiết
pip install -r requirements.txt
```

> Bước này chỉ cần làm **1 lần duy nhất**.  
> Nếu báo lỗi `pip not found` → dùng `pip3 install -r requirements.txt`

---

## BƯỚC 6 — Hiệu chỉnh Camera (Calibration) — làm 1 lần

```bash
cd /home/binh/drone

# Chụp ảnh checkerboard để hiệu chỉnh
python camera_calibration.py --collect
```

- In tờ checkerboard 10×8 ô ra giấy
- Giữ tờ trước camera, nhấn **SPACE** để chụp ~25 ảnh
- Chụp từ nhiều góc độ, khoảng cách khác nhau

```bash
# Tính toán camera matrix từ ảnh đã chụp
python camera_calibration.py --calibrate
```

- Copy kết quả in ra → dán vào `config.py` phần `CAMERA_MATRIX` và `DIST_COEFFS`
- Upload lại `config.py` bằng WinSCP

---

## BƯỚC 7 — Chạy hệ thống

### Test camera (không cần kết nối Flight Controller):

```bash
cd /home/binh/drone
python main.py --no-mavlink
```

### Chạy thật với Flight Controller (Pixhawk):

```bash
python main.py
```

### Xem log realtime:

```bash
python main.py --no-mavlink 2>&1 | tee drone.log
```

---

## BƯỚC 8 — Chạy tự động khi Pi khởi động

Để Pi tự chạy script khi bật nguồn (quan trọng khi gắn lên drone):

```bash
# Mở file autostart
sudo nano /etc/rc.local
```

Thêm dòng này vào **trước** dòng `exit 0`:

```bash
cd /home/binh/drone && python main.py >> /home/binh/drone/drone.log 2>&1 &
```

Lưu: **Ctrl+O** → **Enter** → **Ctrl+X**

Khởi động lại Pi để kiểm tra:

```bash
sudo reboot
```

---

## Tóm tắt nhanh (sau lần đầu setup)

```
Mỗi lần sửa code:
  1. Sửa file trên VS Code (Windows)
  2. WinSCP → kéo thả file đã sửa lên Pi
  3. PuTTY → python main.py --no-mavlink
```

---

## Các lỗi thường gặp

| Lỗi                         | Nguyên nhân             | Cách sửa                                          |
|-----------------------------|-------------------------|---------------------------------------------------|
| `Connection refused`        | Pi chưa bật hoặc sai IP | Kiểm tra lại IP bằng `hostname -I` trên Pi        |
| `Authentication failed`     | Sai username/password   | Kiểm tra lại `binh` / `binh123`                   |
| `ModuleNotFoundError: cv2`  | Chưa cài opencv         | `pip install opencv-contrib-python`               |
| `No module named picamera2` | Thiếu picamera2         | `sudo apt install python3-picamera2`              |
| Camera không mở được        | Chưa enable camera      | `sudo raspi-config` → Interface → Camera → Enable |
| MAVLink không kết nối       | Sai cổng UART           | Kiểm tra `MAVLINK_CONNECTION` trong `config.py`   |
