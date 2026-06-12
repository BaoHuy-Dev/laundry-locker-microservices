# 📋 Tài liệu API theo luồng cho role STAFF (Bên ngoài)

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

> **Mô tả:** Tài liệu chi tiết toàn bộ API mà Staff (nhân viên bên ngoài của Partner) sử dụng, bao gồm cả Staff có tài khoản hệ thống và Staff không có tài khoản (chỉ dùng Access Code/OTP).

---

## 1) Chuẩn chung

- **Base URL:** `/api`
- **Header bắt buộc** (cho Staff có tài khoản):
  - `Authorization: Bearer <access_token>`
  - `Content-Type: application/json`
- **Staff không có tài khoản:** Chỉ cần Access Code (OTP) do Partner cấp, sử dụng IoT API.

---

## 2) Hai loại Staff

### 🅰️ Staff có tài khoản hệ thống (Role: PARTNER/ADMIN)
- Được Partner thêm vào hệ thống qua `POST /api/partner/staff/{staffId}`
- Sử dụng StaffController (`/api/staff/*`) — yêu cầu role `ADMIN` hoặc `PARTNER`
- Mở tủ bằng **Master PIN**

### 🅱️ Staff bên ngoài (không có tài khoản)
- Nhận **Access Code (OTP)** từ Partner
- Mở tủ bằng IoT API `POST /api/iot/unlock-with-code`
- **Không cần đăng nhập, không cần token**

---

## 3) Flow A — Đăng nhập Staff (có tài khoản)

### API A1 — Đăng nhập Phone
- **Method + URL:** `POST /api/auth/phone-login`
- **Mô tả:** Staff đăng nhập bằng Firebase phone token.

### API A2 — Đăng nhập Email
- **Method + URL:** `POST /api/auth/email/send-otp` → `POST /api/auth/email/verify-otp`

### API A3 — Refresh Token
- **Method + URL:** `POST /api/auth/refresh-token`

---

## 4) Flow B — Dashboard & Xem đơn hàng

### API B1 — Tổng quan đơn hàng
- **Method + URL:** `GET /api/staff/orders`
- **Mô tả:** Lấy thống kê tổng quan đơn hàng theo status.
- **Role:** `ADMIN` hoặc `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "SUMMARY_RETRIEVED",
  "data": {
    "waitingOrders": 12,
    "processingOrders": 5,
    "readyOrders": 3,
    "totalOrders": 350
  }
}
```

### API B2 — Đơn chờ lấy đồ (WAITING)
- **Method + URL:** `GET /api/staff/orders/waiting?page=0&size=20`
- **Mô tả:** Danh sách đơn đang chờ Staff đến locker lấy đồ.
- **Role:** `ADMIN` hoặc `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "ORDERS_RETRIEVED",
  "data": {
    "content": [
      {
        "id": 100,
        "orderCode": "ORD-20260306-ABC123",
        "status": "WAITING",
        "lockerCode": "LOC-01",
        "boxNumber": "A05",
        "customerName": "Nguyễn Văn Khách",
        "createdAt": "2026-03-06T08:30:00"
      }
    ],
    "totalElements": 12
  }
}
```

### API B3 — Đơn đang xử lý (PROCESSING)
- **Method + URL:** `GET /api/staff/orders/processing?page=0&size=20`
- **Role:** `ADMIN` hoặc `PARTNER`

### API B4 — Đơn sẵn sàng trả (READY)
- **Method + URL:** `GET /api/staff/orders/ready?page=0&size=20`
- **Role:** `ADMIN` hoặc `PARTNER`

### API B5 — Đơn hàng được giao cho mình
- **Method + URL:** `GET /api/staff/orders/my-assigned?page=0&size=20`
- **Mô tả:** Các đơn Staff tự gán cho mình.
- **Role:** `ADMIN` hoặc `PARTNER`

---

## 5) Flow C — Nhận đơn & Gán đơn cho mình

### API C1 — Gán đơn cho mình
- **Method + URL:** `POST /api/staff/orders/{orderId}/assign`
- **Mô tả:** Staff tự gán đơn hàng cho mình để xử lý.
- **Role:** `ADMIN` hoặc `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "ORDER_ASSIGNED",
  "data": {
    "id": 100,
    "orderCode": "ORD-20260306-ABC123",
    "status": "WAITING",
    "assignedStaffId": 20
  }
}
```

---

## 6) Flow D — Xem Locker

### API D1 — Danh sách Locker
- **Method + URL:** `GET /api/staff/lockers`
- **Mô tả:** Xem tất cả locker với thông tin box.
- **Role:** `ADMIN` hoặc `PARTNER`
- **Query param tùy chọn:** `?storeId=1`

**Response:**
```json
{
  "success": true,
  "code": "LOCKERS_RETRIEVED",
  "data": [
    {
      "id": 10,
      "code": "LOC-01",
      "storeName": "Chi nhánh Quận 7",
      "totalBoxes": 20,
      "availableBoxes": 14,
      "occupiedBoxes": 6
    }
  ]
}
```

---

## 7) Flow E — ⭐ MỞ TỦ LOCKER (Trọng tâm)

> Đây là phần quan trọng nhất — Staff mở tủ để lấy đồ bẩn hoặc trả đồ sạch.

### 🔓 Cách 1: Staff có tài khoản — Mở tủ bằng Master PIN

### API E1 — ⭐ Unlock Box bằng Master PIN
- **Method + URL:** `POST /api/staff/unlock-box`
- **Mô tả:** Staff có tài khoản hệ thống mở tủ bằng Master PIN.
- **Role:** `ADMIN` hoặc `PARTNER`
- **🔐 SỬ DỤNG MASTER PIN (không phải OTP)**

**Request:**
```json
{
  "boxId": 105,
  "masterPin": "999999",
  "orderId": 100,
  "purpose": "COLLECT"
}
```

> **Giải thích các trường:**
> - `boxId`: ID của box cần mở
> - `masterPin`: Mã PIN chủ (master) — mã cố định dành cho Staff có tài khoản
> - `orderId`: (Tùy chọn) Gắn với đơn hàng cụ thể
> - `purpose`: `COLLECT` (lấy đồ bẩn) | `RETURN` (trả đồ sạch) | `MAINTENANCE` (bảo trì)

**Response:**
```json
{
  "success": true,
  "code": "BOX_UNLOCKED",
  "data": {
    "success": true,
    "boxId": 105,
    "boxNumber": "A05",
    "lockerCode": "LOC-01",
    "message": "Box unlocked successfully"
  }
}
```

---

### 🔓 Cách 2: Staff bên ngoài — Mở tủ bằng Access Code (OTP)

### API E2 — ⭐⭐⭐ Unlock Box bằng Access Code (OTP)
- **Method + URL:** `POST /api/iot/unlock-with-code`
- **Mô tả:** Staff bên ngoài (không có tài khoản) mở tủ bằng Access Code do Partner cấp.
- **Role:** **KHÔNG CẦN ĐĂNG NHẬP** (Public endpoint)
- **🔑 SỬ DỤNG ACCESS CODE (OTP) DO PARTNER TẠO**

**Request:**
```json
{
  "orderId": 100,
  "accessCode": "STAFF-8A3F2B",
  "staffName": "Anh Minh"
}
```

> **Giải thích các trường:**
> - `orderId`: ID đơn hàng cần xử lý
> - `accessCode`: Mã OTP do Partner tạo (qua API accept/ready hoặc generate)
> - `staffName`: (Tùy chọn) Tên Staff để tracking

**Response (thành công):**
```json
{
  "success": true,
  "code": "BOX_UNLOCKED",
  "data": {
    "success": true,
    "orderId": 100,
    "orderCode": "ORD-20260306-ABC123",
    "boxId": 105,
    "boxNumber": "A05",
    "lockerCode": "LOC-01",
    "purpose": "COLLECT",
    "message": "Box unlocked successfully using staff access code"
  }
}
```

**Response (mã hết hạn/sai):**
```json
{
  "success": true,
  "code": "UNLOCK_FAILED",
  "data": {
    "success": false,
    "message": "Access code is invalid or expired"
  }
}
```

---

## 8) Flow F — ⭐ Vòng đời đơn hàng đầy đủ (Staff View)

### 📊 Sơ đồ luồng Staff:
```
1. Xem đơn chờ         → GET /api/staff/orders/waiting
2. Gán đơn cho mình     → POST /api/staff/orders/{orderId}/assign
3. ⭐ MỞ TỦ LẤY ĐỒ   → POST /api/iot/unlock-with-code (OTP)
                          HOẶC POST /api/staff/unlock-box (Master PIN)
4. Mang đồ về cửa hàng  → (offline)
5. Giặt xử lý           → (offline, Partner đánh dấu PROCESSING)
6. Giặt xong             → (Partner đánh dấu READY + tạo OTP mới)
7. ⭐ MỞ TỦ TRẢ ĐỒ    → POST /api/iot/unlock-with-code (OTP)
                          HOẶC POST /api/staff/unlock-box (Master PIN)
8. Đơn hoàn thành        → Chờ khách nhận
```

### Bước chi tiết:

#### Bước 1: Xem đơn chờ
- **API:** `GET /api/staff/orders/waiting`

#### Bước 2: Gán đơn
- **API:** `POST /api/staff/orders/{orderId}/assign`

#### Bước 3: ⭐ Đi tới Locker, mở tủ LẤY ĐỒ BẨN
- **API (OTP):** `POST /api/iot/unlock-with-code`
  ```json
  { "orderId": 100, "accessCode": "STAFF-8A3F2B", "staffName": "Anh Minh" }
  ```
- **API (Master PIN):** `POST /api/staff/unlock-box`
  ```json
  { "boxId": 105, "masterPin": "999999", "orderId": 100, "purpose": "COLLECT" }
  ```

#### Bước 4-5-6: Mang đồ về, giặt, Partner xử lý (offline)

#### Bước 7: ⭐ Đi tới Locker, mở tủ TRẢ ĐỒ SẠCH
- **API (OTP):** `POST /api/iot/unlock-with-code`
  ```json
  { "orderId": 100, "accessCode": "STAFF-7D9E4C", "staffName": "Anh Minh" }
  ```
- **API (Master PIN):** `POST /api/staff/unlock-box`
  ```json
  { "boxId": 105, "masterPin": "999999", "orderId": 100, "purpose": "RETURN" }
  ```

#### Bước 8: Chờ khách nhận đồ → COMPLETED

---

## 9) Tóm tắt tất cả API của Staff

| # | Method | URL | Mô tả | Role/Auth |
|---|--------|-----|--------|-----------|
| A1 | POST | `/api/auth/phone-login` | Đăng nhập phone | Public |
| A2 | POST | `/api/auth/email/send-otp` | Gửi OTP email | Public |
| A3 | POST | `/api/auth/refresh-token` | Refresh token | Authenticated |
| B1 | GET | `/api/staff/orders` | Tổng quan đơn hàng | ADMIN/PARTNER |
| B2 | GET | `/api/staff/orders/waiting` | Đơn chờ lấy đồ | ADMIN/PARTNER |
| B3 | GET | `/api/staff/orders/processing` | Đơn đang xử lý | ADMIN/PARTNER |
| B4 | GET | `/api/staff/orders/ready` | Đơn sẵn sàng trả | ADMIN/PARTNER |
| B5 | GET | `/api/staff/orders/my-assigned` | Đơn được giao | ADMIN/PARTNER |
| C1 | POST | `/api/staff/orders/{orderId}/assign` | Gán đơn cho mình | ADMIN/PARTNER |
| D1 | GET | `/api/staff/lockers` | Danh sách Locker | ADMIN/PARTNER |
| **E1** | **POST** | **`/api/staff/unlock-box`** | **🔐 Mở tủ bằng Master PIN** | **ADMIN/PARTNER** |
| **E2** | **POST** | **`/api/iot/unlock-with-code`** | **🔑 Mở tủ bằng Access Code (OTP)** | **Không cần auth** |

---

## 10) ⭐ Tổng hợp: API nào sử dụng mã OTP/Access Code để mở tủ?

### 🔑 Có **3 API** liên quan đến mã OTP/PIN để mở tủ Locker:

| # | API | Mã sử dụng | Ai dùng? | Mô tả |
|---|-----|------------|----------|--------|
| **1** | `POST /api/iot/unlock-with-code` | **Access Code (OTP)** do Partner tạo | **Staff bên ngoài** (không có tài khoản) | Staff nhận mã OTP từ Partner → nhập mã + orderId → mở tủ |
| **2** | `POST /api/staff/unlock-box` | **Master PIN** (mã cố định) | **Staff có tài khoản** (role ADMIN/PARTNER) | Staff đăng nhập → nhập Master PIN + boxId → mở tủ |
| **3** | `POST /api/iot/unlock` | **PIN Code 6 số** (của đơn hàng) | **Customer** (khách hàng) | Khách dùng PIN nhận từ đơn hàng → mở tủ lấy/bỏ đồ |

### 📌 Chi tiết từng loại mã:

#### 1️⃣ Access Code (OTP) — `POST /api/iot/unlock-with-code`
- **Ai tạo:** Partner tạo tự động khi accept/ready đơn, hoặc tạo thủ công
- **Định dạng:** `STAFF-XXXXXX` (6 ký tự alphanumeric)
- **Thời hạn:** Có hạn (mặc định 24h)
- **Ai dùng:** Staff bên ngoài (shipper, nhân viên thời vụ)
- **Không cần đăng nhập**

#### 2️⃣ Master PIN — `POST /api/staff/unlock-box`
- **Ai tạo:** Admin cấu hình sẵn
- **Định dạng:** Mã PIN cố định (VD: `999999`)
- **Thời hạn:** Không hết hạn
- **Ai dùng:** Staff chính thức có tài khoản hệ thống
- **Cần đăng nhập** (Bearer token)

#### 3️⃣ PIN Code — `POST /api/iot/unlock`
- **Ai tạo:** Hệ thống tự tạo khi tạo đơn hàng
- **Định dạng:** 6 chữ số (VD: `456789`)
- **Thời hạn:** Theo đơn hàng
- **Ai dùng:** Customer (khách hàng)
- **Không cần đăng nhập** (dùng tại kiosk/locker)

---

## 11) Lưu ý quan trọng

### ⚡ Staff bên ngoài chỉ cần biết:
1. Nhận **Access Code** từ Partner (qua app, SMS, Zalo...)
2. Tới Locker, nhập mã vào kiosk/app → gọi `POST /api/iot/unlock-with-code`
3. Tủ mở → lấy đồ/trả đồ

### ⚡ Staff có tài khoản:
1. Đăng nhập app Staff
2. Xem đơn hàng → gán đơn cho mình
3. Tới Locker → gọi `POST /api/staff/unlock-box` với Master PIN
4. Tủ mở → lấy đồ/trả đồ

