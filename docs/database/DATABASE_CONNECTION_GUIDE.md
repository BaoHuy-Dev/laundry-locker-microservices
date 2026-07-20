# 📖 Hướng Dẫn Chi Tiết Kết Nối Database DigitalOcean

> **Tài liệu này giải thích chi tiết tại sao và cách thức kết nối tới PostgreSQL database đã deploy trên DigitalOcean
Droplet.**
>
> Nếu bạn chỉ cần hướng dẫn nhanh, xem [DATABASE_QUICK_CONNECT.md](./DATABASE_QUICK_CONNECT.md).

---

## Mục Lục

1. [Tổng Quan Kiến Trúc Kết Nối](#1-tổng-quan-kiến-trúc-kết-nối)
2. [Tại Sao Dùng SSH Tunnel?](#2-tại-sao-dùng-ssh-tunnel)
3. [Yêu Cầu Và Chuẩn Bị](#3-yêu-cầu-và-chuẩn-bị)
4. [Bước 1 — Chuẩn Bị SSH Key](#4-bước-1--chuẩn-bị-ssh-key)
5. [Bước 2 — Cấu Hình SSH Tunnel Trong IDE](#5-bước-2--cấu-hình-ssh-tunnel-trong-ide)
6. [Bước 3 — Cấu Hình Database Connection](#6-bước-3--cấu-hình-database-connection)
7. [Bước 4 — Kiểm Tra Kết Nối](#7-bước-4--kiểm-tra-kết-nối)
8. [Bước 5 — Hiển Thị Tất Cả Databases](#8-bước-5--hiển-thị-tất-cả-databases)
9. [Kiến Trúc Database Của Dự Án](#9-kiến-trúc-database-của-dự-án)
10. [Kết Nối Bằng Command Line (Tùy Chọn)](#10-kết-nối-bằng-command-line-tùy-chọn)
11. [Xử Lý Lỗi Chi Tiết](#11-xử-lý-lỗi-chi-tiết)
12. [Quy Tắc Bảo Mật](#12-quy-tắc-bảo-mật)
13. [FAQ — Câu Hỏi Thường Gặp](#13-faq--câu-hỏi-thường-gặp)

---

## 1. Tổng Quan Kiến Trúc Kết Nối

Database PostgreSQL của dự án được deploy trên một **DigitalOcean Droplet** (máy chủ ảo Linux). PostgreSQL chạy bên
trong **Docker container** trên Droplet đó.

```
┌─────────────────┐       SSH Tunnel        ┌──────────────────────────────────┐
│  Máy tính của    │ ───────────────────────► │  DigitalOcean Droplet            │
│  bạn (localhost) │    Port 22 (SSH)         │  IP: 146.190.84.136              │
│                  │                          │                                  │
│  IntelliJ/       │  localhost:15432 ◄──────►│  Docker: PostgreSQL 16           │
│  DataGrip        │  (qua SSH tunnel)        │  Container port: 5432            │
│                  │                          │  Mapped port: 15432              │
└─────────────────┘                          └──────────────────────────────────┘
```

### Luồng kết nối hoạt động như thế nào?

1. **IDE của bạn** tạo một kết nối SSH tới Droplet (`146.190.84.136:22`)
2. Qua SSH tunnel, IDE **chuyển tiếp (forward)** port `15432` từ Droplet về `127.0.0.1:15432` trên máy bạn
3. IDE kết nối tới PostgreSQL tại `127.0.0.1:15432` — nhưng thực chất traffic đang đi qua SSH tunnel tới PostgreSQL trên
   Droplet
4. Vì PostgreSQL trong Docker được map `5432 → 15432` nên kết nối thành công

---

## 2. Tại Sao Dùng SSH Tunnel?

### ❌ Không kết nối trực tiếp vì:

- **Bảo mật:** PostgreSQL trên Droplet chỉ lắng nghe trên `127.0.0.1` (localhost của server), **KHÔNG** mở ra internet.
  Điều này có nghĩa là không ai từ bên ngoài có thể truy cập trực tiếp vào database.
- **Cấu hình Docker:** Trong `docker-compose.yml`, port được bind là `127.0.0.1:15432:5432` — chú ý prefix `127.0.0.1`
  có nghĩa là chỉ chấp nhận kết nối từ localhost của server.

### ✅ SSH Tunnel giải quyết vấn đề:

- SSH tunnel tạo một "đường hầm" bảo mật từ máy bạn tới server
- Mọi dữ liệu truyền qua tunnel đều được **mã hóa end-to-end**
- Bạn không cần mở port PostgreSQL ra internet (rất nguy hiểm!)
- Xác thực bằng **SSH key** an toàn hơn nhiều so với password

### 🔑 Tại sao dùng Key Pair thay vì Password?

- **An toàn hơn:** SSH key sử dụng mã hóa bất đối xứng (asymmetric cryptography), gần như không thể brute-force
- **Tiện lợi:** Không cần nhớ hay nhập password mỗi lần kết nối
- **Quản lý tốt hơn:** Có thể cấp/thu hồi key cho từng thành viên mà không ảnh hưởng người khác
- **Ed25519:** Thuật toán hiện đại, nhanh và an toàn hơn RSA truyền thống

---

## 3. Yêu Cầu Và Chuẩn Bị

### Phần mềm cần có:

| Phần mềm                             | Mục đích                            | Tải về                                      |
|--------------------------------------|-------------------------------------|---------------------------------------------|
| IntelliJ IDEA Ultimate hoặc DataGrip | IDE có hỗ trợ Database Tools        | [jetbrains.com](https://www.jetbrains.com/) |
| OpenSSH client                       | Tạo SSH key (Windows 10+ đã có sẵn) | Có sẵn trong Windows                        |

### Thông tin cần xin từ trưởng nhóm:

1. ✅ **SSH private key file** (`id_ed25519`) — hoặc được thêm public key của bạn vào server
2. ✅ **Mật khẩu PostgreSQL** cho user `postgres`
3. ✅ **IP server** hiện tại (có thể thay đổi nếu tạo lại Droplet)

---

## 4. Bước 1 — Chuẩn Bị SSH Key

### Trường hợp A: Được trưởng nhóm cấp key

Nếu trưởng nhóm gửi cho bạn file private key:

1. Tạo thư mục `.ssh` nếu chưa có:
   ```powershell
   # Windows (PowerShell)
   mkdir -Force "$env:USERPROFILE\.ssh"
   ```
   ```bash
   # macOS/Linux
   mkdir -p ~/.ssh
   ```

2. Copy file key vào thư mục `.ssh`:
   ```powershell
   # Windows
   Copy-Item "đường_dẫn_tới_key\id_ed25519" "$env:USERPROFILE\.ssh\id_ed25519"
   ```
   ```bash
   # macOS/Linux
   cp đường_dẫn_tới_key/id_ed25519 ~/.ssh/id_ed25519
   ```

3. **Quan trọng — Đặt đúng quyền truy cập cho key file:**
   ```bash
   # macOS/Linux
   chmod 600 ~/.ssh/id_ed25519
   ```
   ```powershell
   # Windows (PowerShell) — chạy với quyền Admin
   icacls "$env:USERPROFILE\.ssh\id_ed25519" /inheritance:r /grant:r "$env:USERNAME:(R)"
   ```

> [!IMPORTANT]
> **Tại sao phải đặt quyền `600`?**
> SSH client sẽ **từ chối** sử dụng key file nếu file đó có quyền quá rộng (ví dụ ai cũng đọc được). Đây là cơ chế bảo
> mật để đảm bảo chỉ bạn mới có thể đọc private key.

### Trường hợp B: Tạo key mới và gửi public key cho trưởng nhóm

1. Tạo SSH key pair mới:
   ```bash
   ssh-keygen -t ed25519 -C "your.email@example.com"
   ```
    - Khi hỏi nơi lưu file, nhấn **Enter** để dùng đường dẫn mặc định
    - Passphrase: có thể để trống hoặc đặt passphrase tùy ý

2. Gửi **public key** (`id_ed25519.pub`) cho trưởng nhóm:
   ```powershell
   # Windows
   cat "$env:USERPROFILE\.ssh\id_ed25519.pub"
   ```
   ```bash
   # macOS/Linux
   cat ~/.ssh/id_ed25519.pub
   ```

3. Trưởng nhóm sẽ thêm public key vào server bằng lệnh:
   ```bash
   # Trên server DigitalOcean
   echo "nội_dung_public_key" >> ~/.ssh/authorized_keys
   ```

> [!WARNING]
> **KHÔNG BAO GIỜ** chia sẻ file **private key** (`id_ed25519` — file KHÔNG có đuôi `.pub`). Chỉ chia sẻ **public key
** (`id_ed25519.pub`).

---

## 5. Bước 2 — Cấu Hình SSH Tunnel Trong IDE

### 5.1. Mở Database Tool Window

- **IntelliJ IDEA:** Menu **View** → **Tool Windows** → **Database**
- **DataGrip:** Mặc định đã hiện sẵn

### 5.2. Tạo Data Source mới

1. Nhấn dấu **`+`** (Add) → **Data Source** → **PostgreSQL**
2. Nếu IDE yêu cầu download driver, nhấn **Download** (chỉ cần lần đầu)

### 5.3. Cấu hình SSH

1. Chuyển sang tab **SSH/SSL**
2. Tích chọn **☑ Use SSH tunnel**
3. Nhấn nút **`...`** (ba chấm) bên cạnh dropdown SSH configuration
4. Trong cửa sổ **SSH Configurations**, nhấn **`+`** để tạo cấu hình mới:

| Thuộc tính              | Giá trị                             | Giải thích                                                   |
|-------------------------|-------------------------------------|--------------------------------------------------------------|
| **Host**                | `146.190.84.136`                    | Địa chỉ IP của DigitalOcean Droplet                          |
| **Port**                | `22`                                | Port mặc định của SSH                                        |
| **Username**            | `root`                              | User trên server (DigitalOcean Droplet mặc định dùng `root`) |
| **Authentication type** | `Key pair (OpenSSH or PuTTY)`       | Xác thực bằng SSH key thay vì password                       |
| **Private key file**    | `C:\Users\<TenBan>\.ssh\id_ed25519` | Đường dẫn tới private key trên máy bạn                       |
| **Passphrase**          | *(để trống)*                        | Nếu key có passphrase thì nhập ở đây                         |

> [!NOTE]
> **Giải thích các giá trị:**
> - **Host `146.190.84.136`**: Đây là IP public của Droplet trên DigitalOcean. Mọi thành viên đều dùng chung IP này.
> - **Port `22`**: SSH sử dụng port 22 theo chuẩn. Server không thay đổi port mặc định.
> - **Username `root`**: DigitalOcean Droplet mặc định tạo user `root`. Trong production nên tạo user riêng, nhưng cho
    development thì `root` là ok.
> - **Key pair**: An toàn hơn password authentication. File `id_ed25519` là key thuật toán Ed25519 — nhỏ gọn và bảo mật
    cao.

5. Nhấn **Test Connection** trong cửa sổ SSH Configurations
6. Nếu thành công → Nhấn **OK** để lưu SSH configuration

### 5.4. Tại sao "Local port" để `<Dynamic>`?

Trong tab SSH/SSL, mục **Local port** hiển thị `<Dynamic>`. Điều này có nghĩa:

- IDE sẽ **tự động chọn** một port trống trên máy bạn để tạo tunnel
- Bạn **không cần** lo port bị conflict với ứng dụng khác
- Mỗi lần kết nối, IDE có thể dùng port khác nhau — hoàn toàn bình thường

---

## 6. Bước 3 — Cấu Hình Database Connection

Quay lại tab **General** và điền thông tin:

| Thuộc tính          | Giá trị                                      | Giải thích                                |
|---------------------|----------------------------------------------|-------------------------------------------|
| **Name**            | `postgres@127.0.0.1`                         | Tên hiển thị, đặt gì cũng được            |
| **Driver**          | `PostgreSQL`                                 | Driver kết nối PostgreSQL, IDE tự quản lý |
| **Connection type** | `default`                                    | Dùng kiểu kết nối mặc định (host + port)  |
| **Host**            | `127.0.0.1`                                  | Localhost — vì kết nối qua SSH tunnel     |
| **Port**            | `15432`                                      | Port PostgreSQL được map trong Docker     |
| **Authentication**  | `User & Password`                            | Xác thực bằng username + password         |
| **User**            | `postgres`                                   | User admin mặc định của PostgreSQL        |
| **Password**        | *(liên hệ trưởng nhóm)*                      | Mật khẩu database                         |
| **Save**            | `Forever`                                    | Lưu password để không phải nhập lại       |
| **Database**        | `postgres`                                   | Database mặc định, dùng làm điểm vào      |
| **URL**             | `jdbc:postgresql://127.0.0.1:15432/postgres` | Tự sinh từ các trường trên                |

### 🤔 Giải thích quan trọng:

#### Tại sao Host là `127.0.0.1` mà không phải IP server?

Vì bạn đang dùng **SSH tunnel**! SSH tunnel sẽ:

1. Kết nối SSH tới `146.190.84.136:22` (bước trước đã cấu hình)
2. Tạo một "cổng" từ máy bạn (127.0.0.1) tới server
3. Khi IDE kết nối tới `127.0.0.1:15432`, traffic thực chất được chuyển qua SSH tới `127.0.0.1:15432` **trên server**

Nói cách khác: `127.0.0.1` ở đây đề cập tới localhost **của server**, không phải máy bạn.

#### Tại sao Port là `15432` chứ không phải `5432`?

PostgreSQL mặc định chạy trên port `5432`. Tuy nhiên, trong cấu hình Docker (`docker-compose.yml`):

```yaml
ports:
  - "127.0.0.1:15432:5432"
```

- Port `5432` là port **bên trong** Docker container
- Port `15432` là port **bên ngoài** (trên server host)
- Kết nối từ bên ngoài container phải dùng `15432`
- Dùng port khác `5432` để tránh conflict với PostgreSQL local nếu bạn cũng cài PostgreSQL trên máy mình

#### Tại sao dùng user `postgres` thay vì user riêng từng service?

Mỗi microservice có database user riêng (ví dụ `auth_user`, `order_user`...), nhưng:

- User `postgres` là **superuser** — có quyền truy cập **tất cả** databases
- Khi dùng IDE để xem/debug data, bạn cần truy cập nhiều databases cùng lúc
- User riêng từng service (như `auth_user`) chỉ có quyền trên database tương ứng (như `auth_db`)

> [!CAUTION]
> Vì bạn đang dùng **superuser**, hãy **CỰC KỲ CẨN THẬN** khi chạy lệnh UPDATE/DELETE. Sai sót có thể ảnh hưởng tất cả
> databases.

---

## 7. Bước 4 — Kiểm Tra Kết Nối

1. Nhấn nút **Test Connection** (góc dưới bên trái cửa sổ Data Sources and Drivers)
2. IDE sẽ thực hiện tuần tự:
    - ✅ Kết nối SSH tới `146.190.84.136:22`
    - ✅ Tạo SSH tunnel
    - ✅ Kết nối PostgreSQL qua tunnel
    - ✅ Xác thực với user `postgres`
3. Nếu mọi thứ OK, bạn sẽ thấy:
    - Thông báo **"Succeeded"**
    - Phiên bản: **PostgreSQL 16.14**
4. Nhấn **OK** để lưu và đóng

---

## 8. Bước 5 — Hiển Thị Tất Cả Databases

Mặc định, IDE chỉ hiển thị database `postgres`. Để xem tất cả databases:

1. Trong panel **Database Explorer**, nhấn chuột phải vào connection `postgres@127.0.0.1`
2. Chọn **Database Tools** → **Manage Shown Schemas**
3. Tích chọn **All databases** hoặc chọn từng database bạn cần
4. Nhấn **OK**

### Danh sách databases trong dự án:

| Database          | Microservice tương ứng           | Mô tả                                                       |
|-------------------|----------------------------------|-------------------------------------------------------------|
| `auth_db`         | auth-service (port 8081)         | Quản lý xác thực, JWT tokens                                |
| `user_db`         | user-service (port 8082)         | Thông tin người dùng                                        |
| `order_db`        | order-service (port 8083)        | Quản lý đơn hàng giặt                                       |
| `locker_db`       | locker-service (port 8084)       | Quản lý tủ locker                                           |
| `laundry_db`      | laundry-service (port 8085)      | Quản lý dịch vụ giặt                                        |
| `payment_db`      | payment-service (port 8086)      | Quản lý thanh toán                                          |
| `notification_db` | notification-service (port 8087) | Quản lý thông báo                                           |
| `iot_db`          | iot-service (port 8088)          | Quản lý thiết bị IoT                                        |
| `store_db`        | store-service (port 8089)        | Quản lý cửa hàng                                            |
| `loyalty_db`      | loyalty-service (port 8092)      | Quản lý chương trình khách hàng thân thiết                  |
| `partner_db`      | *(chưa có service riêng)*        | Quản lý đối tác                                             |
| `staff_db`        | *(chưa có service riêng)*        | Quản lý nhân viên                                           |
| `postgres`        | —                                | Database mặc định của PostgreSQL (không chứa data ứng dụng) |

---

## 9. Kiến Trúc Database Của Dự Án

```
                          DigitalOcean Droplet
                        ┌──────────────────────────────────────┐
                        │   Docker Container: PostgreSQL 16    │
                        │   ┌──────────────────────────────┐   │
                        │   │  Instance PostgreSQL          │   │
                        │   │  ┌────────┐  ┌────────┐     │   │
                        │   │  │auth_db │  │user_db │     │   │
                        │   │  └────────┘  └────────┘     │   │
                        │   │  ┌────────┐  ┌─────────┐    │   │
                        │   │  │order_db│  │locker_db│    │   │
                        │   │  └────────┘  └─────────┘    │   │
                        │   │  ┌─────────┐ ┌──────────┐   │   │
                        │   │  │laundry  │ │payment_db│   │   │
                        │   │  │  _db    │ └──────────┘   │   │
                        │   │  └─────────┘                │   │
                        │   │  ┌──────────────┐ ┌──────┐  │   │
                        │   │  │notification  │ │iot_db│  │   │
                        │   │  │  _db         │ └──────┘  │   │
                        │   │  └──────────────┘           │   │
                        │   │  ┌────────┐ ┌──────────┐    │   │
                        │   │  │store_db│ │loyalty_db│    │   │
                        │   │  └────────┘ └──────────┘    │   │
                        │   │  ┌──────────┐ ┌────────┐    │   │
                        │   │  │partner_db│ │staff_db│    │   │
                        │   │  └──────────┘ └────────┘    │   │
                        │   └──────────────────────────────┘   │
                        │   Mapped port: 127.0.0.1:15432→5432  │
                        └──────────────────────────────────────┘
```

**Thiết kế "Database-per-service":** Mỗi microservice sở hữu một database riêng. Đây là best practice trong kiến trúc
microservices vì:

- Đảm bảo **loose coupling** giữa các services
- Mỗi service có thể **scale độc lập**
- Tránh **tight coupling** qua foreign keys giữa services
- Mỗi service có user riêng với quyền chỉ trên database của mình

---

## 10. Kết Nối Bằng Command Line (Tùy Chọn)

Nếu bạn muốn kết nối bằng command line thay vì IDE:

### Bước 1: Tạo SSH tunnel

```bash
ssh -L 15432:127.0.0.1:15432 -N -f root@146.190.84.136 -i ~/.ssh/id_ed25519
```

**Giải thích:**

- `-L 15432:127.0.0.1:15432`: Forward port 15432 từ remote tới local port 15432
- `-N`: Không chạy command trên remote (chỉ tạo tunnel)
- `-f`: Chạy SSH ở background
- `root@146.190.84.136`: User và IP server
- `-i ~/.ssh/id_ed25519`: Chỉ định private key

### Bước 2: Kết nối PostgreSQL

```bash
psql -h 127.0.0.1 -p 15432 -U postgres -d postgres
```

Hoặc kết nối tới database cụ thể:

```bash
psql -h 127.0.0.1 -p 15432 -U postgres -d auth_db
```

### Bước 3: Đóng tunnel khi xong

```bash
# Tìm PID của SSH tunnel
ps aux | grep "ssh -L 15432"

# Kill process
kill <PID>
```

---

## 11. Xử Lý Lỗi Chi Tiết

### ❌ Lỗi SSH

#### `Connection refused` hoặc `Connection timed out`

```
SSH: Connection to 146.190.84.136:22 refused
```

**Nguyên nhân có thể:**

1. IP server đã thay đổi (DigitalOcean có thể đổi IP khi rebuild Droplet)
2. Server đang tắt hoặc khởi động lại
3. Firewall chặn port 22

**Cách xử lý:**

1. Kiểm tra IP server: đăng nhập DigitalOcean dashboard hoặc hỏi trưởng nhóm
2. Thử ping server: `ping 146.190.84.136`
3. Kiểm tra port SSH: `telnet 146.190.84.136 22`

#### `Auth fail` hoặc `Permission denied (publickey)`

```
SSH: Auth fail for root
```

**Nguyên nhân có thể:**

1. Private key file sai đường dẫn
2. Private key không khớp với public key trên server
3. Quyền truy cập file key quá rộng

**Cách xử lý:**

1. Kiểm tra đường dẫn key: mở file explorer, navigate tới `C:\Users\<TenBan>\.ssh\`
2. Xác nhận file `id_ed25519` tồn tại (file KHÔNG có đuôi `.pub`)
3. Thử kết nối SSH bằng command line để xem lỗi chi tiết:
   ```bash
   ssh -v root@146.190.84.136 -i ~/.ssh/id_ed25519
   ```
   Flag `-v` (verbose) sẽ hiện chi tiết quá trình xác thực

### ❌ Lỗi PostgreSQL

#### `Connection refused` trên port 15432

```
Connection to 127.0.0.1:15432 refused
```

**Nguyên nhân:** Docker container PostgreSQL chưa chạy trên server.

**Cách xử lý:** Liên hệ trưởng nhóm để kiểm tra container:

```bash
# Trên server
docker ps | grep postgres
docker-compose up -d postgres
```

#### `Password authentication failed for user "postgres"`

**Nguyên nhân:** Mật khẩu sai.

**Cách xử lý:** Liên hệ trưởng nhóm để lấy lại mật khẩu đúng.

#### `FATAL: database "xxx" does not exist`

**Nguyên nhân:** Bạn đang cố kết nối tới database chưa được tạo.

**Cách xử lý:** Kết nối tới database `postgres` trước, sau đó xem danh sách databases:

```sql
SELECT datname FROM pg_database WHERE datistemplate = false;
```

---

## 12. Quy Tắc Bảo Mật

> [!CAUTION]
> **Những điều KHÔNG ĐƯỢC LÀM:**

### 🚫 TUYỆT ĐỐI KHÔNG:

1. **Commit SSH key vào Git** — Thêm vào `.gitignore`:
   ```
   *.pem
   id_ed25519
   id_rsa
   ```

2. **Chia sẻ private key qua Slack/Discord/Email** — Private key chỉ nên copy bằng USB hoặc chia sẻ trực tiếp

3. **Chia sẻ password database qua kênh công khai** — Dùng tin nhắn riêng tư hoặc secret manager

4. **Chạy `DROP DATABASE` hoặc `TRUNCATE TABLE` trên server** — Không có backup tự động, dữ liệu mất là mất

5. **Mở port PostgreSQL ra internet** — Luôn dùng SSH tunnel, không bao giờ bind `0.0.0.0:15432`

### ✅ NÊN LÀM:

1. Mỗi thành viên **tạo SSH key riêng** và gửi public key cho trưởng nhóm
2. **Lưu password** trong IDE (Save: Forever) để tránh ghi ra file
3. **Backup dữ liệu** trước khi thay đổi lớn
4. Sử dụng **transaction** khi chạy UPDATE/DELETE:
   ```sql
   BEGIN;
   UPDATE auth_db.public.users SET name = 'test' WHERE id = 1;
   -- Kiểm tra kết quả trước khi commit
   COMMIT;  -- hoặc ROLLBACK nếu sai
   ```

---

## 13. FAQ — Câu Hỏi Thường Gặp

### Q: Tôi dùng macOS/Linux, có khác gì không?

**A:** Không khác nhiều. Chỉ khác đường dẫn private key:

- **Windows:** `C:\Users\<TenBan>\.ssh\id_ed25519`
- **macOS/Linux:** `~/.ssh/id_ed25519` (tức `/Users/<TenBan>/.ssh/id_ed25519` hoặc `/home/<TenBan>/.ssh/id_ed25519`)

### Q: Tôi có thể dùng DBeaver/pgAdmin thay IntelliJ không?

**A:** Có! Mọi database client đều hỗ trợ SSH tunnel. Chỉ cần cấu hình tương tự:

- **DBeaver:** Tab SSH → Enable SSH Tunnel → điền thông tin tương tự
- **pgAdmin:** Servers → Register → SSH Tunnel tab → điền thông tin tương tự

### Q: Tại sao tôi thấy `PostgreSQL 16.14` chứ không phải phiên bản khác?

**A:** Vì Docker image được chỉ định là `postgres:16-alpine` trong `docker-compose.yml`. `16.14` là phiên bản minor mới
nhất của PostgreSQL 16.

### Q: Nhiều người kết nối cùng lúc có sao không?

**A:** Không sao! PostgreSQL hỗ trợ nhiều kết nối đồng thời. Tuy nhiên, hãy cẩn thận:

- Tránh cùng sửa một record
- Dùng transaction khi cần
- Nếu gặp lock conflict, đợi hoặc thông báo nhóm

### Q: Tôi chạy Spring Boot service locally, service kết nối tới database trên DigitalOcean được không?

**A:** Được, nhưng cần:

1. Tạo SSH tunnel trước (xem mục [Kết Nối Bằng Command Line](#10-kết-nối-bằng-command-line-tùy-chọn))
2. Cấu hình `application.yml` hoặc environment variable:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://127.0.0.1:15432/auth_db  # ví dụ cho auth-service
       username: auth_user  # hoặc postgres
       password: <mật_khẩu>
   ```
3. Lưu ý: SSH tunnel phải được mở trước khi start Spring Boot

### Q: IP server thay đổi thì phải làm gì?

**A:**

1. Mở SSH configuration trong IDE (tab SSH/SSL → nhấn `...`)
2. Đổi **Host** thành IP mới
3. Test Connection lại
4. Thông báo cho cả nhóm cập nhật

---

*Cập nhật lần cuối: 16/06/2026*
*Tác giả: Laundry Locker Team*
