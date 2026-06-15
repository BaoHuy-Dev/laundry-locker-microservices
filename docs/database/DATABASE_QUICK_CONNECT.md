# 🔌 Kết Nối Database DigitalOcean — Hướng Dẫn Nhanh

> **Mục đích:** Hướng dẫn nhanh để các thành viên trong nhóm kết nối tới PostgreSQL database đã deploy trên DigitalOcean.
>
> **Xem thêm:** [DATABASE_CONNECTION_GUIDE.md](./DATABASE_CONNECTION_GUIDE.md) để hiểu chi tiết từng bước.

---

## 📋 Yêu Cầu Trước Khi Bắt Đầu

- [ ] Cài đặt **IntelliJ IDEA Ultimate** hoặc **DataGrip**
- [ ] Có **SSH private key** (file `id_ed25519`) — liên hệ trưởng nhóm để được cấp
- [ ] Private key đã được đặt tại: `C:\Users\<TenBan>\.ssh\id_ed25519` (Windows) hoặc `~/.ssh/id_ed25519` (macOS/Linux)

---

## ⚡ Các Bước Kết Nối (5 phút)

### Bước 1 — Mở Data Sources

Trong IntelliJ/DataGrip: **View → Tool Windows → Database** → Nhấn dấu **`+`** → **Data Source** → **PostgreSQL**

### Bước 2 — Cấu hình SSH Tunnel

Chuyển sang tab **SSH/SSL** và điền:

| Thuộc tính           | Giá trị                                          |
|----------------------|--------------------------------------------------|
| ☑ Use SSH tunnel     | **Bật (tích chọn)**                              |
| Host                 | `146.190.84.136`                                 |
| Port                 | `22`                                             |
| Username             | `root`                                           |
| Authentication type  | `Key pair (OpenSSH or PuTTY)`                    |
| Private key file     | `C:\Users\<TenBan>\.ssh\id_ed25519`              |
| Passphrase           | *(để trống nếu key không có passphrase)*         |

> Nhấn **Test Connection** trong cửa sổ SSH để kiểm tra kết nối SSH thành công.

### Bước 3 — Cấu hình Database Connection

Quay lại tab **General** và điền:

| Thuộc tính     | Giá trị                                        |
|----------------|-------------------------------------------------|
| Driver         | `PostgreSQL`                                    |
| Host           | `127.0.0.1`                                     |
| Port           | `15432`                                         |
| Authentication | `User & Password`                               |
| User           | `postgres`                                      |
| Password       | *(liên hệ trưởng nhóm để lấy mật khẩu)*       |
| Database       | `postgres`                                      |
| URL            | `jdbc:postgresql://127.0.0.1:15432/postgres`    |

### Bước 4 — Test & Lưu

1. Nhấn **Test Connection** (góc dưới bên trái)
2. Nếu thấy ✅ **"Succeeded"** → Nhấn **OK** để lưu

### Bước 5 — Xem Tất Cả Databases

Sau khi kết nối thành công, bạn sẽ thấy các databases sau trong Database Explorer:

```
postgres@127.0.0.1
├── auth_db
├── iot_db
├── laundry_db
├── locker_db
├── loyalty_db
├── notification_db
├── order_db
├── partner_db
├── payment_db
├── postgres
├── staff_db
├── store_db
└── user_db
```

> **Mẹo:** Nếu không thấy đủ databases, nhấn chuột phải vào connection → **Database Tools** → **Manage Shown Schemas** → tích chọn **All databases**.

---

## 🚨 Xử Lý Lỗi Thường Gặp

| Lỗi | Cách xử lý |
|-----|-------------|
| `Connection refused` trên SSH | Kiểm tra IP server `146.190.84.136` có đúng không, hoặc hỏi trưởng nhóm xem IP có thay đổi |
| `Auth fail` trên SSH | Kiểm tra private key file đúng đường dẫn, đúng file |
| `Connection refused` trên PostgreSQL | Kiểm tra port `15432` và host `127.0.0.1` |
| `Password authentication failed` | Liên hệ trưởng nhóm để lấy lại mật khẩu đúng |
| Không thấy databases | Xem mục **"Xem Tất Cả Databases"** ở trên |

---

## 🔑 Thông Tin Quan Trọng

> [!CAUTION]
> - **KHÔNG** commit SSH key hoặc mật khẩu database vào Git
> - **KHÔNG** chia sẻ thông tin kết nối qua kênh công khai
> - Database này là **shared environment** — hãy cẩn thận khi thao tác trực tiếp trên dữ liệu

---

*Cập nhật lần cuối: 16/06/2026*
