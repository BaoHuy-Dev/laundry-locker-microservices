# 🔑 Tổng hợp các API sử dụng mã OTP/PIN/Access Code để mở tủ Locker

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

> **Tài liệu này liệt kê đầy đủ tất cả API liên quan đến mã OTP, PIN Code, Access Code dùng để mở tủ Locker trong hệ thống Laundry Locker.**

---

## 📊 Tổng quan: Có 4 API sử dụng mã để mở tủ

| # | API Endpoint | Loại mã | Ai sử dụng | Mục đích |
|---|-------------|---------|-------------|----------|
| **1** | `POST /api/iot/unlock-with-code` | **Access Code (OTP)** | Staff bên ngoài | Mở tủ lấy đồ bẩn / trả đồ sạch |
| **2** | `POST /api/staff/unlock-box` | **Master PIN** | Staff có tài khoản | Mở tủ lấy đồ / trả đồ / bảo trì |
| **3** | `POST /api/iot/unlock` | **PIN Code (6 số)** | Customer (khách hàng) | Mở tủ bỏ đồ vào / lấy đồ ra |
| **4** | `POST /api/iot/verify-pin` | **PIN Code (6 số)** | Kiosk / IoT device | Xác thực PIN trước khi mở tủ |

---

## 1️⃣ API: Mở tủ bằng Access Code (OTP) — Staff bên ngoài

### `POST /api/iot/unlock-with-code`

- **Mô tả:** Staff bên ngoài (không có tài khoản hệ thống) sử dụng Access Code do Partner cấp để mở tủ locker.
- **Auth:** ❌ **KHÔNG CẦN ĐĂNG NHẬP** (Public endpoint)
- **Loại mã:** Access Code dạng `STAFF-XXXXXX`
- **Ai tạo mã:** Partner tạo tự động khi:
  - Accept đơn hàng → `POST /api/partner/orders/{orderId}/accept` (purpose: COLLECT)
  - Mark Ready → `POST /api/partner/orders/{orderId}/ready` (purpose: RETURN)
  - Hoặc tạo thủ công → `POST /api/partner/access-codes/generate`

**Request:**
```json
{
  "orderId": 100,
  "accessCode": "STAFF-8A3F2B",
  "staffName": "Anh Minh"
}
```

**Response thành công:**
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

**Response thất bại (mã sai/hết hạn):**
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

### 📌 Khi nào Staff nhận mã OTP này?
1. Partner chấp nhận đơn hàng → hệ thống tạo Access Code (purpose: `COLLECT`) → Partner gửi cho Staff
2. Partner đánh dấu đơn Ready → hệ thống tạo Access Code (purpose: `RETURN`) → Partner gửi cho Staff
3. Partner tạo mã thủ công (backup) qua API generate

---

## 2️⃣ API: Mở tủ bằng Master PIN — Staff có tài khoản

### `POST /api/staff/unlock-box`

- **Mô tả:** Staff chính thức (có tài khoản hệ thống, role ADMIN/PARTNER) mở tủ bằng Master PIN.
- **Auth:** ✅ **CẦN ĐĂNG NHẬP** — Role: `ADMIN` hoặc `PARTNER`
- **Loại mã:** Master PIN (mã cố định, không hết hạn)

**Request:**
```json
{
  "boxId": 105,
  "masterPin": "999999",
  "orderId": 100,
  "purpose": "COLLECT"
}
```

| Field | Type | Bắt buộc | Mô tả |
|-------|------|----------|--------|
| `boxId` | Long | ✅ | ID box cần mở |
| `masterPin` | String | ✅ | Mã PIN chủ (Master PIN) |
| `orderId` | Long | ❌ | ID đơn hàng (nếu mở cho đơn cụ thể) |
| `purpose` | String | ❌ | `COLLECT` / `RETURN` / `MAINTENANCE` |

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

## 3️⃣ API: Mở tủ bằng PIN Code — Customer

### `POST /api/iot/unlock`

- **Mô tả:** Khách hàng sử dụng PIN Code 6 số (nhận khi tạo đơn) để mở tủ bỏ đồ vào hoặc lấy đồ ra.
- **Auth:** ❌ **KHÔNG CẦN ĐĂNG NHẬP** (dùng tại kiosk/locker)
- **Loại mã:** PIN Code 6 chữ số (VD: `456789`)
- **Ai tạo:** Hệ thống tự tạo khi đơn hàng được tạo

**Request:**
```json
{
  "boxId": 105,
  "pinCode": "456789",
  "actionType": "DROP_OFF"
}
```

| Field | Type | Bắt buộc | Mô tả |
|-------|------|----------|--------|
| `boxId` | Long | ✅ | ID box cần mở |
| `pinCode` | String | ✅ | PIN 6 chữ số (regex: `^\d{6}$`) |
| `actionType` | String | ❌ | `DROP_OFF` (bỏ đồ) / `PICKUP` (lấy đồ) |

**Response:**
```json
{
  "success": true,
  "code": "BOX_UNLOCKED",
  "data": {
    "success": true,
    "boxId": 105,
    "boxNumber": "A05",
    "message": "Box unlocked successfully"
  }
}
```

---

## 4️⃣ API: Xác thực PIN trước khi mở tủ

### `POST /api/iot/verify-pin`

- **Mô tả:** Kiểm tra PIN có hợp lệ hay không (không mở tủ, chỉ xác thực).
- **Auth:** ❌ **KHÔNG CẦN ĐĂNG NHẬP**
- **Loại mã:** PIN Code 6 chữ số

**Request:**
```json
{
  "boxId": 105,
  "pinCode": "456789"
}
```

**Response (hợp lệ):**
```json
{
  "success": true,
  "code": "PIN_VALID",
  "data": {
    "valid": true,
    "orderId": 100,
    "orderCode": "ORD-20260306-ABC123"
  }
}
```

**Response (không hợp lệ):**
```json
{
  "success": true,
  "code": "PIN_INVALID",
  "data": {
    "valid": false,
    "message": "PIN code is invalid or expired"
  }
}
```

---

## 📌 So sánh chi tiết 3 loại mã

| Tiêu chí | Access Code (OTP) | Master PIN | PIN Code |
|----------|-------------------|------------|----------|
| **API** | `/api/iot/unlock-with-code` | `/api/staff/unlock-box` | `/api/iot/unlock` |
| **Ai dùng** | Staff bên ngoài | Staff có tài khoản | Customer |
| **Ai tạo** | Partner (tự động/thủ công) | Admin cấu hình | Hệ thống (tự động) |
| **Định dạng** | `STAFF-XXXXXX` | Mã cố định (VD: `999999`) | 6 chữ số (VD: `456789`) |
| **Có hết hạn?** | ✅ Có (mặc định 24h) | ❌ Không | ✅ Theo đơn hàng |
| **Cần đăng nhập?** | ❌ Không | ✅ Có (Bearer token) | ❌ Không |
| **Mục đích** | Lấy đồ / Trả đồ | Lấy đồ / Trả đồ / Bảo trì | Bỏ đồ / Lấy đồ |
| **Số lần dùng** | 1 lần (USED sau khi dùng) | Nhiều lần | Theo đơn hàng |

---

## 🔄 Luồng sử dụng mã trong vòng đời đơn hàng

```
┌─────────────────────────────────────────────────────────────────┐
│                    VÒNG ĐỜI ĐƠN HÀNG                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Customer tạo đơn → Hệ thống tạo PIN Code (6 số)           │
│                                                                 │
│  2. Customer dùng PIN Code mở tủ bỏ đồ vào                     │
│     → POST /api/iot/unlock  {pinCode: "456789"}                │
│     → Đơn chuyển sang WAITING                                   │
│                                                                 │
│  3. Partner accept đơn → Tạo Access Code (OTP) cho Staff        │
│     → POST /api/partner/orders/{id}/accept                      │
│     → Trả về accessCode: "STAFF-8A3F2B" (purpose: COLLECT)     │
│                                                                 │
│  4. ⭐ Staff mở tủ LẤY ĐỒ BẨN bằng OTP                       │
│     → POST /api/iot/unlock-with-code                            │
│       {orderId: 100, accessCode: "STAFF-8A3F2B"}               │
│     → Đơn chuyển sang COLLECTED                                 │
│                                                                 │
│  5. Partner xử lý (PROCESSING) → Giặt xong (READY)            │
│     → POST /api/partner/orders/{id}/ready                       │
│     → Tạo Access Code mới: "STAFF-7D9E4C" (purpose: RETURN)   │
│                                                                 │
│  6. ⭐ Staff mở tủ TRẢ ĐỒ SẠCH bằng OTP                      │
│     → POST /api/iot/unlock-with-code                            │
│       {orderId: 100, accessCode: "STAFF-7D9E4C"}               │
│     → Đơn chuyển sang RETURNED                                  │
│                                                                 │
│  7. Customer dùng PIN Code mở tủ lấy đồ sạch                   │
│     → POST /api/iot/unlock  {pinCode: "456789"}                │
│     → Customer xác nhận pickup                                  │
│     → Đơn chuyển sang COMPLETED                                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## ⚠️ Lưu ý quan trọng

1. **Access Code (OTP) cho Staff** chỉ dùng được **1 lần**. Sau khi mở tủ, mã chuyển sang trạng thái `USED`.
2. Nếu mã hết hạn hoặc bị hủy, Partner cần tạo mã mới qua `POST /api/partner/access-codes/generate`.
3. **Master PIN** không bị giới hạn số lần sử dụng, nhưng chỉ Staff có tài khoản mới dùng được.
4. **PIN Code** của Customer có thể dùng nhiều lần trong vòng đời đơn hàng (bỏ đồ + lấy đồ).
5. API `POST /api/iot/verify-pin` chỉ kiểm tra PIN, **không mở tủ** — dùng để validate trước khi gọi unlock.

