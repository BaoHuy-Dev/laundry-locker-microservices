# 📋 Tài liệu API theo luồng cho role PARTNER

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

> **Mô tả:** Tài liệu chi tiết toàn bộ API mà Partner sử dụng, từ lúc đăng ký → quản lý đơn hàng → kết thúc đơn hàng.

---

## 1) Chuẩn chung

- **Base URL:** `/api`
- **Header bắt buộc** (trừ API đăng nhập):
  - `Authorization: Bearer <access_token>`
  - `Content-Type: application/json`
- **Response envelope:**

```json
{
  "success": true,
  "code": "SOME_CODE",
  "message": "Localized message",
  "data": {}
}
```

- Các API phân trang trả về `Page<T>` trong `data` (có `content`, `totalElements`, `totalPages`, `number`, `size`).

---

## 2) Flow A — Đăng ký & Đăng nhập Partner

### API A1 — Đăng nhập (Phone)
- **Method + URL:** `POST /api/auth/phone-login`
- **Mô tả:** Đăng nhập bằng Firebase phone token.
- **Role:** Public

**Request:**
```json
{
  "idToken": "firebase_id_token_xxx"
}
```

**Response (user cũ):**
```json
{
  "success": true,
  "code": "AUTH_PHONE_LOGIN_SUCCESS",
  "data": {
    "isNewUser": false,
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "2f6f1c1d-..."
  }
}
```

### API A2 — Đăng nhập (Email OTP)
- **Method + URL:** `POST /api/auth/email/send-otp`
- **Mô tả:** Gửi OTP tới email.

**Request:**
```json
{
  "email": "partner@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "code": "AUTH_OTP_SENT"
}
```

### API A3 — Xác thực Email OTP
- **Method + URL:** `POST /api/auth/email/verify-otp`
- **Mô tả:** Xác thực OTP email, đăng nhập/đăng ký.

**Request:**
```json
{
  "email": "partner@example.com",
  "otp": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "code": "AUTH_EMAIL_LOGIN_SUCCESS",
  "data": {
    "isNewUser": false,
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "2f6f1c1d-..."
  }
}
```

### API A4 — Đăng ký làm Partner
- **Method + URL:** `POST /api/partner`
- **Mô tả:** User đã có tài khoản đăng ký trở thành Partner (chờ Admin duyệt).
- **Role:** `isAuthenticated()`

**Request:**
```json
{
  "businessName": "Giặt Sấy ABC",
  "businessType": "LAUNDRY_SERVICE",
  "businessAddress": "123 Nguyễn Văn Linh, Quận 7, TP.HCM",
  "businessPhone": "0901234567",
  "taxCode": "0123456789",
  "description": "Dịch vụ giặt sấy chuyên nghiệp"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "code": "PARTNER_REGISTERED",
  "data": {
    "id": 5,
    "userId": 12,
    "businessName": "Giặt Sấy ABC",
    "status": "PENDING",
    "createdAt": "2026-03-06T10:00:00"
  }
}
```

> ⚠️ **Lưu ý:** Sau khi đăng ký, Partner ở trạng thái `PENDING`. Admin phải duyệt (`APPROVED`) thì Partner mới có thể sử dụng các API quản lý.

### API A5 — Xem profile Partner
- **Method + URL:** `GET /api/partner`
- **Mô tả:** Lấy thông tin profile Partner hiện tại.
- **Role:** `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "PARTNER_RETRIEVED",
  "data": {
    "id": 5,
    "userId": 12,
    "businessName": "Giặt Sấy ABC",
    "status": "APPROVED",
    "businessAddress": "123 Nguyễn Văn Linh, Quận 7, TP.HCM",
    "businessPhone": "0901234567"
  }
}
```

### API A6 — Cập nhật profile Partner
- **Method + URL:** `PUT /api/partner`
- **Mô tả:** Cập nhật thông tin kinh doanh.
- **Role:** `PARTNER`

**Request:**
```json
{
  "businessName": "Giặt Sấy ABC Premium",
  "businessAddress": "456 Lê Văn Việt, Quận 9, TP.HCM",
  "businessPhone": "0907654321",
  "description": "Dịch vụ giặt sấy cao cấp"
}
```

### API A7 — Refresh Token
- **Method + URL:** `POST /api/auth/refresh-token`

**Request:**
```json
{
  "refreshToken": "2f6f1c1d-..."
}
```

---

## 3) Flow B — Dashboard & Thống kê

### API B1 — Dashboard tổng quan
- **Method + URL:** `GET /api/partner/dashboard`
- **Mô tả:** Lấy thống kê tổng quan cho Partner (số đơn, doanh thu, v.v.).
- **Role:** `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "DASHBOARD_RETRIEVED",
  "data": {
    "totalOrders": 350,
    "pendingOrders": 12,
    "processingOrders": 8,
    "completedOrders": 320,
    "totalRevenue": 45000000,
    "todayOrders": 5,
    "todayRevenue": 750000
  }
}
```

### API B2 — Thống kê đơn hàng
- **Method + URL:** `GET /api/partner/orders/statistics`
- **Role:** `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "STATISTICS_RETRIEVED",
  "data": {
    "totalOrders": 350,
    "ordersByStatus": {
      "WAITING": 12,
      "COLLECTED": 3,
      "PROCESSING": 5,
      "READY": 2,
      "RETURNED": 8,
      "COMPLETED": 320
    }
  }
}
```

### API B3 — Doanh thu theo khoảng thời gian
- **Method + URL:** `GET /api/partner/revenue?fromDate=2026-03-01T00:00:00&toDate=2026-03-06T23:59:59`
- **Role:** `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "REVENUE_RETRIEVED",
  "data": {
    "totalRevenue": 12500000,
    "orderCount": 45,
    "fromDate": "2026-03-01T00:00:00",
    "toDate": "2026-03-06T23:59:59"
  }
}
```

---

## 4) Flow C — Quản lý Store & Locker

### API C1 — Xem danh sách Store
- **Method + URL:** `GET /api/partner/stores`
- **Role:** `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "STORES_RETRIEVED",
  "data": [
    {
      "id": 1,
      "name": "Chi nhánh Quận 7",
      "address": "123 Nguyễn Văn Linh",
      "isActive": true
    }
  ]
}
```

### API C2 — Xem danh sách Locker
- **Method + URL:** `GET /api/partner/lockers`
- **Role:** `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "LOCKERS_RETRIEVED",
  "data": [
    {
      "id": 10,
      "code": "LOC-01",
      "storeId": 1,
      "totalBoxes": 20,
      "availableBoxes": 14
    }
  ]
}
```

### API C3 — Xem Box trống của Locker
- **Method + URL:** `GET /api/partner/lockers/{lockerId}/boxes/available`
- **Role:** `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "BOXES_RETRIEVED",
  "data": [
    { "id": 101, "boxNumber": "A01", "size": "MEDIUM", "status": "AVAILABLE" },
    { "id": 102, "boxNumber": "A02", "size": "LARGE", "status": "AVAILABLE" }
  ]
}
```

---

## 5) Flow D — Quản lý nhân viên (Staff)

### API D1 — Xem danh sách Staff
- **Method + URL:** `GET /api/partner/staff`
- **Role:** `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "STAFF_RETRIEVED",
  "data": [
    { "id": 20, "name": "Nguyễn Văn A", "email": "a@example.com", "phone": "0901111111" },
    { "id": 21, "name": "Trần Thị B", "email": "b@example.com", "phone": "0902222222" }
  ]
}
```

### API D2 — Thêm Staff vào Partner
- **Method + URL:** `POST /api/partner/staff/{staffId}`
- **Role:** `PARTNER`

**Response (201 Created):**
```json
{
  "success": true,
  "code": "STAFF_ADDED",
  "data": {
    "id": 22,
    "name": "Lê Văn C",
    "email": "c@example.com"
  }
}
```

### API D3 — Xóa Staff khỏi Partner
- **Method + URL:** `DELETE /api/partner/staff/{staffId}`
- **Role:** `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "STAFF_REMOVED"
}
```

---

## 6) Flow E — ⭐ Vòng đời đơn hàng (ORDER LIFECYCLE) — Trọng tâm

> Đây là luồng chính mô tả toàn bộ quá trình xử lý đơn hàng từ góc nhìn Partner.

### 📊 Sơ đồ trạng thái đơn hàng:
```
Customer tạo đơn → INITIALIZED
Customer bỏ đồ vào tủ → WAITING
  ↓
Partner accept đơn → WAITING (đã accept, tạo Access Code cho Staff)
  ↓
Staff dùng Access Code mở tủ lấy đồ → COLLECTED
  ↓
Partner đánh dấu đang xử lý → PROCESSING
  ↓
Partner cập nhật cân nặng thực tế
  ↓
Partner đánh dấu hoàn thành giặt → READY (tạo Access Code cho Staff trả đồ)
  ↓
Staff dùng Access Code mở tủ trả đồ → RETURNED
  ↓
Customer nhận đồ → COMPLETED
```

### Bước 1: Xem đơn hàng chờ xử lý

### API E1 — Danh sách đơn chờ (WAITING)
- **Method + URL:** `GET /api/partner/orders/pending?page=0&size=20`
- **Mô tả:** Lấy các đơn hàng đang ở trạng thái WAITING, chờ Partner chấp nhận.
- **Role:** `PARTNER`

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
        "customerName": "Nguyễn Văn Khách",
        "storeName": "Chi nhánh Quận 7",
        "lockerCode": "LOC-01",
        "boxNumber": "A05",
        "estimatedWeight": 3.5,
        "services": ["Giặt sấy thường", "Ủi"],
        "totalAmount": 120000,
        "createdAt": "2026-03-06T08:30:00"
      }
    ],
    "totalElements": 12,
    "totalPages": 1
  }
}
```

### API E2 — Xem tất cả đơn hàng (filter theo status)
- **Method + URL:** `GET /api/partner/orders?status=PROCESSING&page=0&size=20`
- **Role:** `PARTNER`

### API E3 — Xem chi tiết đơn hàng
- **Method + URL:** `GET /api/partner/orders/{orderId}`
- **Role:** `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "ORDER_RETRIEVED",
  "data": {
    "id": 100,
    "orderCode": "ORD-20260306-ABC123",
    "status": "WAITING",
    "customerName": "Nguyễn Văn Khách",
    "customerPhone": "0901234567",
    "storeName": "Chi nhánh Quận 7",
    "lockerCode": "LOC-01",
    "boxId": 105,
    "boxNumber": "A05",
    "estimatedWeight": 3.5,
    "actualWeight": null,
    "services": [
      { "name": "Giặt sấy thường", "price": 80000 },
      { "name": "Ủi", "price": 40000 }
    ],
    "totalAmount": 120000,
    "pinCode": "456789",
    "createdAt": "2026-03-06T08:30:00"
  }
}
```

---

### Bước 2: Chấp nhận đơn hàng & Tạo mã Access Code cho Staff

### API E4 — ⭐ Accept đơn hàng (tạo OTP/Access Code cho Staff)
- **Method + URL:** `POST /api/partner/orders/{orderId}/accept?expirationHours=24&notes=Lấy đồ tại tủ LOC-01`
- **Mô tả:** Partner chấp nhận đơn → Hệ thống tự động tạo **mã Access Code (OTP)** cho Staff đến locker mở tủ lấy đồ.
- **Role:** `PARTNER`

**Response (201 Created):**
```json
{
  "success": true,
  "code": "ORDER_ACCEPTED",
  "data": {
    "id": 1,
    "orderId": 100,
    "accessCode": "STAFF-8A3F2B",
    "purpose": "COLLECT",
    "expiresAt": "2026-03-07T08:30:00",
    "status": "ACTIVE",
    "notes": "Lấy đồ tại tủ LOC-01",
    "createdAt": "2026-03-06T08:35:00"
  }
}
```

> 🔑 **Quan trọng:** `accessCode` = "STAFF-8A3F2B" chính là mã OTP mà Staff sẽ dùng để mở tủ locker. Partner gửi mã này cho Staff (qua app, SMS, hoặc chat).

---

### Bước 3: Staff dùng Access Code mở tủ lấy đồ

> ⚡ **Xem chi tiết tại [staff-api-flows.md](staff-api-flows.md) — API E1 (Master PIN) hoặc E2 (Access Code/OTP)**

Sau khi Staff mở tủ thành công, đơn hàng chuyển sang `COLLECTED`.

---

### Bước 4: Xử lý đơn hàng

### API E5 — Đánh dấu đơn đang xử lý
- **Method + URL:** `POST /api/partner/orders/{orderId}/process`
- **Mô tả:** Chuyển đơn sang trạng thái PROCESSING (đang giặt/ủi).
- **Role:** `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "ORDER_PROCESSING",
  "data": {
    "id": 100,
    "orderCode": "ORD-20260306-ABC123",
    "status": "PROCESSING",
    "processedAt": "2026-03-06T10:00:00"
  }
}
```

### API E6 — Cập nhật cân nặng thực tế
- **Method + URL:** `PUT /api/partner/orders/{orderId}/weight`
- **Mô tả:** Sau khi cân thực tế, cập nhật lại trọng lượng (có thể ảnh hưởng giá tiền).
- **Role:** `PARTNER`

**Request:**
```json
{
  "actualWeight": 4.2,
  "weightNotes": "Bao gồm 2 áo khoác dày"
}
```

**Response:**
```json
{
  "success": true,
  "code": "ORDER_WEIGHT_UPDATED",
  "data": {
    "id": 100,
    "estimatedWeight": 3.5,
    "actualWeight": 4.2,
    "totalAmount": 140000
  }
}
```

---

### Bước 5: Giặt xong — Đánh dấu Ready & Tạo mã Access Code cho Staff trả đồ

### API E7 — ⭐ Mark Ready (tạo OTP/Access Code cho Staff trả đồ)
- **Method + URL:** `POST /api/partner/orders/{orderId}/ready?expirationHours=24&notes=Trả đồ tại tủ LOC-01 box A05`
- **Mô tả:** Partner đánh dấu đơn đã hoàn thành giặt → Hệ thống tự động tạo **mã Access Code (OTP)** cho Staff mở tủ trả đồ.
- **Role:** `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "ORDER_READY",
  "data": {
    "id": 2,
    "orderId": 100,
    "accessCode": "STAFF-7D9E4C",
    "purpose": "RETURN",
    "expiresAt": "2026-03-07T16:00:00",
    "status": "ACTIVE",
    "notes": "Trả đồ tại tủ LOC-01 box A05",
    "createdAt": "2026-03-06T16:00:00"
  }
}
```

> 🔑 **Quan trọng:** `accessCode` = "STAFF-7D9E4C" là mã OTP để Staff mở tủ trả đồ cho khách.

---

### Bước 6: Staff trả đồ vào tủ

> ⚡ **Xem chi tiết tại [staff-api-flows.md](staff-api-flows.md) — API E1 (Master PIN) hoặc E2 (Access Code/OTP)**

Sau khi Staff mở tủ trả đồ, đơn hàng chuyển sang `RETURNED`.

---

### Bước 7: Khách nhận đồ → COMPLETED

> Customer tự nhận đồ bằng PIN code. Đơn hàng chuyển sang `COMPLETED`.

---

## 7) Flow F — Quản lý Access Code (OTP cho Staff)

### API F1 — Tạo Access Code thủ công
- **Method + URL:** `POST /api/partner/access-codes/generate`
- **Mô tả:** Tạo mã Access Code cho Staff (ngoài việc tạo tự động khi accept/ready).
- **Role:** `PARTNER`

**Request:**
```json
{
  "orderId": 100,
  "purpose": "COLLECT",
  "expirationHours": 12,
  "notes": "Mã backup nếu Staff quên mã cũ"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "code": "CODE_GENERATED",
  "data": {
    "id": 3,
    "orderId": 100,
    "accessCode": "STAFF-2K8M5N",
    "purpose": "COLLECT",
    "expiresAt": "2026-03-06T22:00:00",
    "status": "ACTIVE"
  }
}
```

### API F2 — Xem tất cả Access Code
- **Method + URL:** `GET /api/partner/access-codes?page=0&size=20`
- **Role:** `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "CODES_RETRIEVED",
  "data": {
    "content": [
      {
        "id": 1,
        "orderId": 100,
        "accessCode": "STAFF-8A3F2B",
        "purpose": "COLLECT",
        "status": "USED",
        "usedAt": "2026-03-06T09:00:00"
      },
      {
        "id": 2,
        "orderId": 100,
        "accessCode": "STAFF-7D9E4C",
        "purpose": "RETURN",
        "status": "ACTIVE",
        "expiresAt": "2026-03-07T16:00:00"
      }
    ]
  }
}
```

### API F3 — Xem Access Code theo đơn hàng
- **Method + URL:** `GET /api/partner/access-codes/order/{orderId}`
- **Role:** `PARTNER`

### API F4 — Hủy Access Code
- **Method + URL:** `POST /api/partner/access-codes/{codeId}/cancel`
- **Role:** `PARTNER`

**Response:**
```json
{
  "success": true,
  "code": "CODE_CANCELLED"
}
```

---

## 8) Tóm tắt tất cả API của Partner

| # | Method | URL | Mô tả | Role |
|---|--------|-----|--------|------|
| A1 | POST | `/api/auth/phone-login` | Đăng nhập phone | Public |
| A2 | POST | `/api/auth/email/send-otp` | Gửi OTP email | Public |
| A3 | POST | `/api/auth/email/verify-otp` | Xác thực OTP email | Public |
| A4 | POST | `/api/partner` | Đăng ký Partner | Authenticated |
| A5 | GET | `/api/partner` | Xem profile | PARTNER |
| A6 | PUT | `/api/partner` | Cập nhật profile | PARTNER |
| A7 | POST | `/api/auth/refresh-token` | Refresh token | Authenticated |
| B1 | GET | `/api/partner/dashboard` | Dashboard | PARTNER |
| B2 | GET | `/api/partner/orders/statistics` | Thống kê đơn hàng | PARTNER |
| B3 | GET | `/api/partner/revenue` | Doanh thu | PARTNER |
| C1 | GET | `/api/partner/stores` | Danh sách Store | PARTNER |
| C2 | GET | `/api/partner/lockers` | Danh sách Locker | PARTNER |
| C3 | GET | `/api/partner/lockers/{lockerId}/boxes/available` | Box trống | PARTNER |
| D1 | GET | `/api/partner/staff` | Danh sách Staff | PARTNER |
| D2 | POST | `/api/partner/staff/{staffId}` | Thêm Staff | PARTNER |
| D3 | DELETE | `/api/partner/staff/{staffId}` | Xóa Staff | PARTNER |
| E1 | GET | `/api/partner/orders/pending` | Đơn chờ accept | PARTNER |
| E2 | GET | `/api/partner/orders` | Tất cả đơn hàng | PARTNER |
| E3 | GET | `/api/partner/orders/{orderId}` | Chi tiết đơn hàng | PARTNER |
| **E4** | **POST** | **`/api/partner/orders/{orderId}/accept`** | **Accept đơn + tạo OTP cho Staff lấy đồ** | **PARTNER** |
| E5 | POST | `/api/partner/orders/{orderId}/process` | Đánh dấu đang xử lý | PARTNER |
| E6 | PUT | `/api/partner/orders/{orderId}/weight` | Cập nhật cân nặng | PARTNER |
| **E7** | **POST** | **`/api/partner/orders/{orderId}/ready`** | **Mark Ready + tạo OTP cho Staff trả đồ** | **PARTNER** |
| F1 | POST | `/api/partner/access-codes/generate` | Tạo Access Code thủ công | PARTNER |
| F2 | GET | `/api/partner/access-codes` | Danh sách Access Code | PARTNER |
| F3 | GET | `/api/partner/access-codes/order/{orderId}` | Access Code theo đơn | PARTNER |
| F4 | POST | `/api/partner/access-codes/{codeId}/cancel` | Hủy Access Code | PARTNER |

---

## 9) Lưu ý quan trọng

### 🔑 Về Access Code (OTP cho Staff mở tủ):
1. **Access Code được tạo tự động** khi Partner gọi API **E4 (accept)** và **E7 (ready)**.
2. **Access Code cũng có thể tạo thủ công** qua API **F1**.
3. Mỗi Access Code có **thời gian hết hạn** (mặc định 24h).
4. Staff bên ngoài (không có tài khoản hệ thống) dùng mã này tại IoT API `POST /api/iot/unlock-with-code` để mở tủ.
5. Mã có các purpose: `COLLECT` (lấy đồ bẩn) và `RETURN` (trả đồ sạch).



