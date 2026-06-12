# 📱 Hướng Dẫn Sử Dụng API — Luồng Order Đầy Đủ

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

> **Tài liệu chi tiết từng API** trong luồng đặt hàng giặt ủi, từ lúc đăng nhập cho đến khi nhận đồ và đánh giá.
> Áp dụng cho cả **Mobile App** và **Kiosk tại cửa hàng**.

---

## Mục lục

- [Tổng quan luồng Order](#tổng-quan-luồng-order)
- [Sơ đồ trạng thái đơn hàng](#sơ-đồ-trạng-thái-đơn-hàng)
- [PHASE 1: Đăng nhập / Đăng ký](#phase-1-đăng-nhập--đăng-ký)
- [PHASE 2: Khám phá cửa hàng & dịch vụ](#phase-2-khám-phá-cửa-hàng--dịch-vụ)
- [PHASE 3: Chọn tủ & tạo đơn hàng](#phase-3-chọn-tủ--tạo-đơn-hàng)
- [PHASE 4: Thanh toán](#phase-4-thanh-toán)
- [PHASE 5: Mở tủ bỏ đồ (Khách hàng)](#phase-5-mở-tủ-bỏ-đồ-khách-hàng)
- [PHASE 6: Xác nhận đã bỏ đồ](#phase-6-xác-nhận-đã-bỏ-đồ)
- [PHASE 7: Nhân viên lấy đồ từ tủ](#phase-7-nhân-viên-lấy-đồ-từ-tủ)
- [PHASE 8: Cân và cập nhật trọng lượng](#phase-8-cân-và-cập-nhật-trọng-lượng)
- [PHASE 9: Xử lý giặt](#phase-9-xử-lý-giặt)
- [PHASE 10: Đánh dấu sẵn sàng](#phase-10-đánh-dấu-sẵn-sàng)
- [PHASE 11: Trả đồ vào tủ](#phase-11-trả-đồ-vào-tủ)
- [PHASE 12: Khách lấy đồ](#phase-12-khách-lấy-đồ)
- [PHASE 13: Hoàn tất đơn hàng](#phase-13-hoàn-tất-đơn-hàng)
- [PHASE 14: Đánh giá](#phase-14-đánh-giá)
- [Các API bổ trợ](#các-api-bổ-trợ)
- [So sánh Mobile vs Kiosk](#so-sánh-mobile-vs-kiosk)

---

## Tổng quan luồng Order

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        KHÁCH HÀNG (Mobile / Kiosk)                      │
│                                                                         │
│  ① Đăng nhập → ② Xem dịch vụ → ③ Chọn tủ → ④ Tạo đơn → ⑤ Thanh toán │
│  → ⑥ Mở tủ bỏ đồ → ⑦ Xác nhận                                        │
└─────────────────────────┬───────────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────────────┐
│                     NHÂN VIÊN / PARTNER                                  │
│                                                                         │
│  ⑧ Lấy đồ → ⑨ Cân → ⑩ Giặt → ⑪ Sẵn sàng → ⑫ Trả vào tủ             │
└─────────────────────────┬───────────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────────────┐
│                        KHÁCH HÀNG                                        │
│                                                                         │
│  ⑬ Mở tủ lấy đồ → ⑭ Hoàn tất → ⑮ Đánh giá                            │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Sơ đồ trạng thái đơn hàng

```
INITIALIZED → WAITING → COLLECTED → PROCESSING → READY → RETURNED → COMPLETED
     │                                                                    
     └──────────────────── CANCELED (có thể hủy trước khi COLLECTED)
```

| Trạng thái | Ý nghĩa | Ai thực hiện |
|------------|---------|--------------|
| `INITIALIZED` | Đơn mới tạo, chờ khách bỏ đồ vào tủ | Hệ thống |
| `WAITING` | Khách đã bỏ đồ, chờ nhân viên lấy | Khách hàng |
| `COLLECTED` | Nhân viên đã lấy đồ từ tủ | Nhân viên |
| `PROCESSING` | Đang giặt/xử lý | Nhân viên |
| `READY` | Giặt xong, sẵn sàng trả | Nhân viên |
| `RETURNED` | Đã trả vào tủ, chờ khách lấy | Nhân viên |
| `COMPLETED` | Khách đã lấy đồ, hoàn tất | Khách hàng |
| `CANCELED` | Đơn bị hủy | Khách/Hệ thống |

---

## PHASE 1: Đăng nhập / Đăng ký

### 📱 Mobile App — Đăng nhập bằng SĐT

#### API 1.1: Gửi OTP qua Firebase (client-side)
```
Không gọi backend — xử lý trên client bằng Firebase SDK
→ signInWithPhoneNumber("+84912345678")
→ User nhập OTP → confirm → nhận idToken
```

#### API 1.2: Gửi idToken lên backend

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/auth/phone-login` |
| **Auth** | ❌ Public |

**Request:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIs..."
}
```

**Response — User CŨ:**
```json
{
  "success": true,
  "code": "AUTH_PHONE_LOGIN_SUCCESS",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "isNewUser": false,
    "phoneNumber": "+84912345678",
    "tempToken": null,
    "userInfo": {
      "id": 1,
      "firstName": "Nguyễn",
      "lastName": "Văn A",
      "phoneNumber": "+84912345678",
      "email": null
    }
  }
}
```
> ✅ Nhận `accessToken` → bỏ qua đăng ký → chuyển thẳng **Phase 2**

**Response — User MỚI:**
```json
{
  "success": true,
  "code": "AUTH_PHONE_NEW_USER",
  "data": {
    "accessToken": null,
    "refreshToken": null,
    "tokenType": "Bearer",
    "expiresIn": null,
    "isNewUser": true,
    "phoneNumber": "+84912345678",
    "tempToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "userInfo": null
  }
}
```
> ⚠️ `isNewUser = true` → cần đăng ký trước → **API 1.3 hoặc 1.4**

---

#### API 1.3: Đăng ký đầy đủ (Mobile App)

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/auth/complete-registration` |
| **Auth** | ❌ Public |
| **Khi nào** | Chỉ khi `isNewUser = true`, dùng trên **Mobile App** |

**Request:**
```json
{
  "tempToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "firstName": "Nguyễn",
  "lastName": "Văn A",
  "birthday": "1995-06-15"
}
```

**Response:**
```json
{
  "success": true,
  "code": "AUTH_REGISTRATION_COMPLETE",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "550e8400-e29b...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}
```

---

#### API 1.4: Đăng ký nhanh (Kiosk) ⚡

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/auth/kiosk/quick-register` |
| **Auth** | ❌ Public |
| **Khi nào** | Chỉ khi `isNewUser = true`, dùng trên **Kiosk** |

**Request:**
```json
{
  "tempToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Response:**
```json
{
  "success": true,
  "code": "AUTH_REGISTRATION_COMPLETE",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "550e8400-e29b...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}
```
> ℹ️ Backend tự tạo user: `firstName="Khách"`, không yêu cầu họ tên, ngày sinh.

---

### 📱 Mobile App & Kiosk — Đăng nhập bằng Email

#### API 1.5: Gửi OTP qua email

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/auth/email/send-otp` |
| **Auth** | ❌ Public |

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "code": "AUTH_OTP_SENT",
  "message": "OTP sent to your email."
}
```

---

#### API 1.6: Xác thực OTP email

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/auth/email/verify-otp` |
| **Auth** | ❌ Public |

**Request:**
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

**Response — User CŨ:**
```json
{
  "success": true,
  "code": "AUTH_EMAIL_LOGIN_SUCCESS",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "isNewUser": false,
    "otpVerified": true,
    "tempToken": null,
    "userInfo": { ... }
  }
}
```

**Response — User MỚI:**
```json
{
  "success": true,
  "code": "AUTH_EMAIL_NEW_USER",
  "data": {
    "accessToken": null,
    "isNewUser": true,
    "otpVerified": true,
    "tempToken": "uuid-temp-token",
    "userInfo": null
  }
}
```
> ⚠️ `isNewUser = true` → gọi **API 1.3** (Mobile) hoặc **API 1.4** (Kiosk)

---

## PHASE 2: Khám phá cửa hàng & dịch vụ

### API 2.1: Xem danh sách cửa hàng

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/stores` |
| **Auth** | ❌ Public |

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Tiệm giặt Quận 1",
      "address": "123 Nguyễn Huệ, Q.1, TP.HCM",
      "phone": "0901234567",
      "isActive": true
    }
  ]
}
```

---

### API 2.2: Tìm cửa hàng gần đây

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/stores/nearby?latitude=10.762&longitude=106.660&radiusMeters=5000&limit=10` |
| **Auth** | ❌ Public |

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Tiệm giặt Quận 1",
      "distance": 1200,
      "latitude": 10.763,
      "longitude": 106.661
    }
  ]
}
```

---

### API 2.3: Xem dịch vụ theo danh mục

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/services?category=LAUNDRY` |
| **Auth** | ❌ Public |
| **Params** | `category`: `LAUNDRY` (giặt) hoặc `STORAGE` (gửi đồ) |

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Giặt sấy thường",
      "price": 30000,
      "maxPrice": null,
      "unit": "kg",
      "description": "Giặt sấy quần áo thường",
      "category": "LAUNDRY",
      "serviceType": "MAIN",
      "isAddon": false,
      "estimatedHours": 24,
      "storeId": 1,
      "storeName": "Tiệm giặt Quận 1"
    },
    {
      "id": 2,
      "name": "Giặt hấp",
      "price": 50000,
      "unit": "kg",
      "category": "LAUNDRY",
      "serviceType": "MAIN",
      "estimatedHours": 48
    },
    {
      "id": 5,
      "name": "Nước xả vải cao cấp",
      "price": 10000,
      "unit": "lần",
      "category": "LAUNDRY",
      "serviceType": "ADDON",
      "isAddon": true
    }
  ]
}
```

---

### API 2.4: Xem dịch vụ theo cửa hàng + danh mục

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/services?storeId=1&category=LAUNDRY` |
| **Auth** | ❌ Public |

---

### API 2.5: Xem chi tiết 1 dịch vụ

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/services/{id}` |
| **Auth** | ❌ Public |

---

## PHASE 3: Chọn tủ & tạo đơn hàng

### API 3.1: Xem danh sách tủ theo cửa hàng

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/lockers?storeId=1` |
| **Auth** | ❌ Public |

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "code": "LOCKER-001",
      "name": "Tủ giặt tầng 1",
      "status": "ACTIVE",
      "address": "123 Nguyễn Huệ, Q.1",
      "storeId": 1
    }
  ]
}
```

---

### API 3.2: Xem box trống trong tủ

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/lockers/{id}/boxes/available` |
| **Auth** | ❌ Public |

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 5,
      "boxNumber": 3,
      "status": "AVAILABLE",
      "size": "MEDIUM",
      "isActive": true
    },
    {
      "id": 8,
      "boxNumber": 6,
      "status": "AVAILABLE",
      "size": "LARGE",
      "isActive": true
    }
  ]
}
```

---

### API 3.3: Tạo đơn hàng 🎯

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/orders` |
| **Auth** | 🔐 JWT (`Authorization: Bearer {accessToken}`) |

**Request:**
```json
{
  "type": "LAUNDRY",
  "lockerId": 1,
  "boxIds": [5],
  "serviceIds": [1, 5],
  "serviceCategory": "LAUNDRY",
  "customerNote": "Giặt nhẹ tay, không dùng nước nóng",
  "receiverName": "Nguyễn Văn A",
  "receiverPhone": "+84912345678",
  "estimatedWeight": 3.5,
  "promotionCode": "SALE10"
}
```

| Field | Bắt buộc | Mô tả |
|-------|:--------:|-------|
| `type` | ✅ | `LAUNDRY`, `DRY_CLEAN`, hoặc `STORAGE` |
| `lockerId` | ✅ | ID tủ đã chọn |
| `boxIds` | ❌ | ID box chọn (nếu để trống, hệ thống tự phân) |
| `serviceIds` | ❌ | Danh sách ID dịch vụ |
| `serviceCategory` | ❌ | `LAUNDRY` hoặc `STORAGE` |
| `customerNote` | ❌ | Ghi chú của khách |
| `receiverName` | ❌ | Tên người nhận (mặc định = người gửi) |
| `receiverPhone` | ❌ | SĐT người nhận |
| `estimatedWeight` | ❌ | Ước lượng cân nặng (kg) |
| `promotionCode` | ❌ | Mã khuyến mãi |

**Response:**
```json
{
  "success": true,
  "code": "ORDER_CREATED",
  "data": {
    "id": 101,
    "orderCode": "ORD-20260220-A1B2C3",
    "type": "LAUNDRY",
    "status": "INITIALIZED",
    "pinCode": "384721",
    "serviceCategory": "LAUNDRY",
    "senderId": 1,
    "senderName": "Nguyễn Văn A",
    "lockerId": 1,
    "lockerName": "Tủ giặt tầng 1",
    "lockerCode": "LOCKER-001",
    "sendBoxNumber": 3,
    "sendBoxNumbers": [3],
    "totalPrice": 115000,
    "isPaid": false,
    "paymentRequired": true,
    "nextAction": "PAYMENT",
    "nextActionMessage": "Vui lòng thanh toán để tiếp tục",
    "createdAt": "2026-02-20T10:00:00"
  }
}
```

> 📌 Lưu lại: `id` (orderId), `pinCode` (dùng để mở tủ), `lockerCode`

---

## PHASE 4: Thanh toán

### API 4.1: Tạo thanh toán online (VNPay / MoMo)

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/payments/create` |
| **Auth** | 🔐 JWT |

**Request:**
```json
{
  "orderId": 101,
  "paymentMethod": "VNPAY",
  "bankCode": "",
  "language": "vn"
}
```

| Field | Bắt buộc | Giá trị |
|-------|:--------:|---------|
| `orderId` | ✅ | ID đơn hàng |
| `paymentMethod` | ✅ | `VNPAY`, `MOMO`, `ZALOPAY`, `CASH`, `BANK_TRANSFER`, `WALLET` |
| `bankCode` | ❌ | Mã ngân hàng (chỉ cho VNPay) |
| `language` | ❌ | `vn` hoặc `en` |

**Response:**
```json
{
  "success": true,
  "code": "PAYMENT_CREATED",
  "data": {
    "paymentId": 50,
    "orderId": 101,
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_...",
    "expireAt": "2026-02-20T10:15:00",
    "qrCodeUrl": null,
    "deeplink": null
  }
}
```

> 📱 **Mobile**: Mở `paymentUrl` trong WebView hoặc trình duyệt
> 🖥️ **Kiosk**: Hiển thị QR code từ `paymentUrl` hoặc `qrCodeUrl`

---

### API 4.2: Kiểm tra trạng thái thanh toán

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/payments/order/{orderId}` |
| **Auth** | 🔐 JWT |

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 50,
      "orderId": 101,
      "amount": 115000,
      "method": "VNPAY",
      "status": "COMPLETED",
      "createdAt": "2026-02-20T10:05:00"
    }
  ]
}
```

> ⏳ Sau khi thanh toán, VNPay/MoMo callback về backend tự động cập nhật.
> Client nên **polling** endpoint này mỗi 3-5 giây cho đến khi `status = "COMPLETED"`.

---

## PHASE 5: Mở tủ bỏ đồ (Khách hàng)

### API 5.1: Mở khóa tủ bằng PIN 🔓

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/iot/unlock` |
| **Auth** | ❌ Public |

**Request:**
```json
{
  "boxId": 5,
  "pinCode": "384721"
}
```

**Response:**
```json
{
  "success": true,
  "code": "BOX_UNLOCKED",
  "data": {
    "success": true,
    "boxId": 5,
    "boxNumber": 3,
    "lockerCode": "LOCKER-001",
    "orderId": 101,
    "unlockToken": "uuid-unlock-token",
    "unlockTimestamp": 1740000000000,
    "message": null
  }
}
```

> 🔔 **MQTT**: Backend tự động gửi lệnh mở khóa vật lý tới ESP8266:
> ```
> Topic:   locker/commands/LOCKER-001
> Payload: {"box_id": 3, "action": "OPEN"}
> ```
> → ESP8266 kích hoạt relay → tủ mở → khách bỏ đồ vào

---

## PHASE 6: Xác nhận đã bỏ đồ

### API 6.1: Khách xác nhận đã bỏ đồ vào tủ

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/orders/{orderId}/confirm` |
| **Auth** | 🔐 JWT |
| **Trạng thái** | `INITIALIZED` → `WAITING` |

**Response:**
```json
{
  "success": true,
  "code": "ORDER_CONFIRMED",
  "data": {
    "id": 101,
    "status": "WAITING",
    "nextAction": "WAIT_STAFF",
    "nextActionMessage": "Đồ của bạn đang chờ nhân viên tới lấy"
  }
}
```

> 📱 Sau khi confirm, khách có thể đóng app/kiosk. Hệ thống sẽ gửi notification khi có cập nhật.

---

## PHASE 7: Nhân viên lấy đồ từ tủ

> 👷 Từ đây trở đi là **phía nhân viên/staff** thao tác

### API 7.1: Nhân viên mở tủ lấy đồ (bằng Staff Code)

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/iot/unlock-with-code` |
| **Auth** | ❌ Public |

**Request:**
```json
{
  "orderId": 101,
  "accessCode": "ABC123",
  "staffName": "Nhân viên Huy"
}
```

**Response:**
```json
{
  "success": true,
  "code": "BOX_UNLOCKED",
  "data": {
    "success": true,
    "orderId": 101,
    "orderStatus": "COLLECTED",
    "action": "COLLECT",
    "boxes": [{ "boxId": 5, "boxNumber": 3 }],
    "lockerCode": "LOCKER-001",
    "lockerName": "Tủ giặt tầng 1",
    "unlockToken": "uuid-token",
    "unlockTimestamp": 1740000000000,
    "message": "Box unlocked successfully"
  }
}
```

> 🔔 **MQTT**: Gửi lệnh mở khóa tới ESP8266 tương tự Phase 5

---

### API 7.2: Hoặc nhân viên đánh dấu "đã lấy" qua app

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/orders/{orderId}/collect` |
| **Auth** | 🔐 JWT (role: ADMIN hoặc STAFF) |
| **Trạng thái** | `WAITING` → `COLLECTED` |

**Response:**
```json
{
  "success": true,
  "code": "ORDER_COLLECTED",
  "data": {
    "id": 101,
    "status": "COLLECTED",
    "staffId": 10,
    "staffName": "Nhân viên Huy"
  }
}
```

---

## PHASE 8: Cân và cập nhật trọng lượng

### API 8.1: Cập nhật cân nặng thực tế

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/orders/{orderId}/weight` |
| **Auth** | 🔐 JWT (role: ADMIN hoặc STAFF) |

**Request:**
```json
{
  "actualWeight": 3.2,
  "weightUnit": "kg",
  "items": [
    { "serviceId": 1, "quantity": 1 },
    { "serviceId": 5, "quantity": 1 }
  ],
  "staffNote": "Có 1 áo trắng bị ố vàng sẵn"
}
```

**Response:**
```json
{
  "success": true,
  "code": "ORDER_WEIGHT_UPDATED",
  "data": {
    "id": 101,
    "actualWeight": 3.2,
    "weightUnit": "kg",
    "totalPrice": 106000,
    "staffNote": "Có 1 áo trắng bị ố vàng sẵn"
  }
}
```

---

## PHASE 9: Xử lý giặt

### API 9.1: Bắt đầu giặt

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/orders/{orderId}/process` |
| **Auth** | 🔐 JWT (role: ADMIN hoặc STAFF) |
| **Trạng thái** | `COLLECTED` → `PROCESSING` |

**Response:**
```json
{
  "success": true,
  "code": "ORDER_PROCESSING",
  "data": {
    "id": 101,
    "status": "PROCESSING"
  }
}
```

---

## PHASE 10: Đánh dấu sẵn sàng

### API 10.1: Giặt xong — đánh dấu sẵn sàng

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/orders/{orderId}/ready` |
| **Auth** | 🔐 JWT (role: ADMIN hoặc STAFF) |
| **Trạng thái** | `PROCESSING` → `READY` |

**Response:**
```json
{
  "success": true,
  "code": "ORDER_READY",
  "data": {
    "id": 101,
    "status": "READY"
  }
}
```

---

## PHASE 11: Trả đồ vào tủ

### API 11.1: Nhân viên trả đồ vào box

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/orders/{orderId}/return?boxId=8` |
| **Auth** | 🔐 JWT (role: ADMIN hoặc STAFF) |
| **Trạng thái** | `READY` → `RETURNED` |
| **Query param** | `boxId` — ID box nhận đồ (có thể khác box gửi ban đầu) |

**Response:**
```json
{
  "success": true,
  "code": "ORDER_RETURNED",
  "data": {
    "id": 101,
    "status": "RETURNED",
    "pinCode": "384721",
    "receiveBoxNumber": 6,
    "lockerName": "Tủ giặt tầng 1",
    "lockerCode": "LOCKER-001",
    "returnedAt": "2026-02-21T14:00:00",
    "pickupDeadline": "2026-02-22T14:00:00",
    "nextAction": "PICKUP",
    "nextActionMessage": "Đồ đã sẵn sàng. Vui lòng đến lấy trước 22/02/2026 14:00"
  }
}
```

> 📱 Hệ thống tự gửi notification tới khách: "Đồ đã sẵn sàng lấy, mã PIN: 384721"

---

## PHASE 12: Khách lấy đồ

### API 12.1: Xác thực PIN (tùy chọn — hiển thị thông tin trước khi mở)

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/iot/verify-pin` |
| **Auth** | ❌ Public |

**Request:**
```json
{
  "boxId": 8,
  "pinCode": "384721"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "valid": true,
    "orderId": 101,
    "boxId": 8,
    "boxNumber": 6,
    "lockerCode": "LOCKER-001",
    "orderStatus": "RETURNED",
    "message": null
  }
}
```

---

### API 12.2: Mở tủ lấy đồ 🔓

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/iot/unlock` |
| **Auth** | ❌ Public |

**Request:**
```json
{
  "boxId": 8,
  "pinCode": "384721"
}
```

**Response:** tương tự Phase 5

> 🔔 **MQTT**: `locker/commands/LOCKER-001` → `{"box_id": 6, "action": "OPEN"}`

---

## PHASE 13: Hoàn tất đơn hàng

### API 13.1: Xác nhận đã lấy đồ (qua IoT)

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/iot/pickup` |
| **Auth** | 🔐 JWT |
| **Trạng thái** | `RETURNED` → `COMPLETED` |

**Request:**
```json
{
  "orderId": 101,
  "boxId": 8
}
```

**Response:**
```json
{
  "success": true,
  "code": "PICKUP_CONFIRMED",
  "data": {
    "success": true,
    "orderId": 101,
    "orderStatus": "COMPLETED",
    "completedAt": "2026-02-21T16:30:00",
    "message": "Pickup confirmed. Order completed!"
  }
}
```

---

### API 13.2: Hoặc hoàn tất qua Order API

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/orders/{orderId}/complete` |
| **Auth** | 🔐 JWT |
| **Trạng thái** | `RETURNED` → `COMPLETED` |

**Response:**
```json
{
  "success": true,
  "code": "ORDER_COMPLETED",
  "data": {
    "id": 101,
    "status": "COMPLETED",
    "completedAt": "2026-02-21T16:30:00"
  }
}
```

---

## PHASE 14: Đánh giá

### API 14.1: Đánh giá đơn hàng ⭐

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/orders/{orderId}/rate` |
| **Auth** | 🔐 JWT |
| **Điều kiện** | Chỉ khi `status = COMPLETED` |

**Request:**
```json
{
  "rating": 5,
  "comment": "Giặt sạch sẽ, thơm tho. Rất hài lòng!",
  "serviceRating": 5,
  "speedRating": 4,
  "staffRating": 5
}
```

| Field | Bắt buộc | Mô tả |
|-------|:--------:|-------|
| `rating` | ✅ | Đánh giá tổng (1-5 ⭐) |
| `comment` | ❌ | Nhận xét (tối đa 500 ký tự) |
| `serviceRating` | ❌ | Chất lượng dịch vụ (1-5) |
| `speedRating` | ❌ | Tốc độ (1-5) |
| `staffRating` | ❌ | Nhân viên (1-5) |

**Response:**
```json
{
  "success": true,
  "code": "ORDER_RATED",
  "data": {
    "id": 1,
    "orderId": 101,
    "userId": 1,
    "userName": "Nguyễn Văn A",
    "rating": 5,
    "comment": "Giặt sạch sẽ, thơm tho. Rất hài lòng!",
    "serviceRating": 5,
    "speedRating": 4,
    "staffRating": 5,
    "createdAt": "2026-02-21T16:35:00"
  }
}
```

---

## Các API bổ trợ

### Xem trạng thái đơn hàng (polling/tracking)

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/orders/{orderId}/status` |
| **Auth** | 🔐 JWT |

**Response:**
```json
{
  "success": true,
  "data": {
    "orderId": 101,
    "status": "PROCESSING",
    "statusDescription": "Đang xử lý giặt",
    "pinCode": "384721",
    "lockerName": "Tủ giặt tầng 1",
    "lockerCode": "LOCKER-001",
    "boxNumber": 3,
    "isPaid": true,
    "nextAction": "Vui lòng chờ, đồ đang được giặt"
  }
}
```

---

### Xem lịch sử đơn hàng

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/orders/my-orders?status=COMPLETED&page=0&size=10` |
| **Auth** | 🔐 JWT |
| **Params** | `status` (tùy chọn): `INITIALIZED`, `WAITING`, `PROCESSING`, `COMPLETED`... |

---

### Xem timeline đơn hàng

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/orders/{orderId}/timeline` |
| **Auth** | 🔐 JWT |

**Response:**
```json
{
  "success": true,
  "data": {
    "orderId": 101,
    "currentStatus": "RETURNED",
    "progressPercentage": 85,
    "nextAction": "Đến lấy đồ tại tủ LOCKER-001",
    "nextActionActor": "CUSTOMER",
    "events": [
      { "status": "INITIALIZED", "timestamp": "2026-02-20T10:00:00", "description": "Đơn hàng đã tạo" },
      { "status": "WAITING", "timestamp": "2026-02-20T10:10:00", "description": "Khách đã bỏ đồ vào tủ" },
      { "status": "COLLECTED", "timestamp": "2026-02-20T11:00:00", "description": "Nhân viên đã lấy đồ" },
      { "status": "PROCESSING", "timestamp": "2026-02-20T11:30:00", "description": "Đang giặt" },
      { "status": "READY", "timestamp": "2026-02-21T10:00:00", "description": "Giặt xong" },
      { "status": "RETURNED", "timestamp": "2026-02-21T14:00:00", "description": "Đã trả vào tủ box 6" }
    ]
  }
}
```

---

### Hủy đơn hàng

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/orders/{orderId}/cancel?reason=1` |
| **Auth** | 🔐 JWT |
| **Điều kiện** | Chỉ hủy được trước trạng thái `COLLECTED` |

---

### Xem đánh giá của đơn hàng

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/orders/{orderId}/rating` |
| **Auth** | 🔐 JWT |

---

## So sánh Mobile vs Kiosk

### Thứ tự API gọi — Mobile App (User mới, SĐT)

```
 1. Firebase OTP                           (client-side)
 2. POST /api/auth/phone-login             → isNewUser=true, tempToken
 3. POST /api/auth/complete-registration   → accessToken (nhập họ tên, ngày sinh)
 4. GET  /api/stores/nearby                → tìm cửa hàng
 5. GET  /api/services?category=LAUNDRY    → xem dịch vụ
 6. GET  /api/lockers?storeId=1            → xem tủ
 7. GET  /api/lockers/1/boxes/available    → xem box trống
 8. POST /api/orders                       → tạo đơn → orderId, pinCode
 9. POST /api/payments/create              → paymentUrl
10. (Thanh toán)
11. POST /api/iot/unlock                   → mở tủ bỏ đồ + MQTT 🔓
12. PUT  /api/orders/{id}/confirm          → xác nhận bỏ đồ
    ... (chờ nhân viên xử lý) ...
13. POST /api/iot/verify-pin               → kiểm tra PIN
14. POST /api/iot/unlock                   → mở tủ lấy đồ + MQTT 🔓
15. PUT  /api/orders/{id}/complete         → hoàn tất
16. POST /api/orders/{id}/rate             → đánh giá ⭐
```

### Thứ tự API gọi — Kiosk (User mới, SĐT)

```
 1. Firebase OTP                           (client-side)
 2. POST /api/auth/phone-login             → isNewUser=true, tempToken
 3. POST /api/auth/kiosk/quick-register    → accessToken ⚡ (KHÔNG nhập gì)
 4. GET  /api/services?category=LAUNDRY    → xem dịch vụ (tủ cố định tại kiosk)
 5. GET  /api/lockers/{id}/boxes/available → xem box trống
 6. POST /api/orders                       → tạo đơn → orderId, pinCode
 7. POST /api/payments/create              → paymentUrl / QR code
 8. (Thanh toán)
 9. POST /api/iot/unlock                   → mở tủ bỏ đồ + MQTT 🔓
10. PUT  /api/orders/{id}/confirm          → xác nhận bỏ đồ
    ✅ XONG — Khách rời đi
```

### Thứ tự API — Nhân viên xử lý

```
 1. POST /api/iot/unlock-with-code         → mở tủ lấy đồ + MQTT 🔓
    (hoặc PUT /api/orders/{id}/collect)
 2. PUT  /api/orders/{id}/weight           → cập nhật cân nặng
 3. PUT  /api/orders/{id}/process          → bắt đầu giặt
 4. PUT  /api/orders/{id}/ready            → giặt xong
 5. PUT  /api/orders/{id}/return?boxId=8   → trả đồ vào tủ
```

### Thứ tự API — Khách lấy đồ (qua Kiosk hoặc Mobile)

```
 1. POST /api/iot/verify-pin               → xác thực PIN (tùy chọn)
 2. POST /api/iot/unlock                   → mở tủ lấy đồ + MQTT 🔓
 3. POST /api/iot/pickup                   → hoàn tất
    (hoặc PUT /api/orders/{id}/complete)
```

---

## Bảng tổng hợp toàn bộ API

| # | Phase | Method | Endpoint | Auth | Ai dùng |
|---|-------|--------|----------|:----:|---------|
| 1 | Auth | POST | `/api/auth/phone-login` | ❌ | Mobile + Kiosk |
| 2 | Auth | POST | `/api/auth/complete-registration` | ❌ | Mobile |
| 3 | Auth | POST | `/api/auth/kiosk/quick-register` | ❌ | Kiosk |
| 4 | Auth | POST | `/api/auth/email/send-otp` | ❌ | Mobile + Kiosk |
| 5 | Auth | POST | `/api/auth/email/verify-otp` | ❌ | Mobile + Kiosk |
| 6 | Store | GET | `/api/stores` | ❌ | Mobile |
| 7 | Store | GET | `/api/stores/nearby` | ❌ | Mobile |
| 8 | Service | GET | `/api/services?category=LAUNDRY` | ❌ | Mobile + Kiosk |
| 9 | Locker | GET | `/api/lockers?storeId=1` | ❌ | Mobile + Kiosk |
| 10 | Locker | GET | `/api/lockers/{id}/boxes/available` | ❌ | Mobile + Kiosk |
| 11 | Order | POST | `/api/orders` | 🔐 | Mobile + Kiosk |
| 12 | Payment | POST | `/api/payments/create` | 🔐 | Mobile + Kiosk |
| 13 | Payment | GET | `/api/payments/order/{orderId}` | 🔐 | Mobile + Kiosk |
| 14 | IoT | POST | `/api/iot/verify-pin` | ❌ | Mobile + Kiosk |
| 15 | IoT | POST | `/api/iot/unlock` | ❌ | Mobile + Kiosk |
| 16 | Order | PUT | `/api/orders/{id}/confirm` | 🔐 | Mobile + Kiosk |
| 17 | Order | GET | `/api/orders/{id}/status` | 🔐 | Mobile |
| 18 | Order | GET | `/api/orders/my-orders` | 🔐 | Mobile |
| 19 | Order | GET | `/api/orders/{id}/timeline` | 🔐 | Mobile |
| 20 | IoT | POST | `/api/iot/unlock-with-code` | ❌ | Staff |
| 21 | Order | PUT | `/api/orders/{id}/collect` | 🔐 | Staff |
| 22 | Order | PUT | `/api/orders/{id}/weight` | 🔐 | Staff |
| 23 | Order | PUT | `/api/orders/{id}/process` | 🔐 | Staff |
| 24 | Order | PUT | `/api/orders/{id}/ready` | 🔐 | Staff |
| 25 | Order | PUT | `/api/orders/{id}/return?boxId=X` | 🔐 | Staff |
| 26 | IoT | POST | `/api/iot/pickup` | 🔐 | Mobile + Kiosk |
| 27 | Order | PUT | `/api/orders/{id}/complete` | 🔐 | Mobile |
| 28 | Order | POST | `/api/orders/{id}/rate` | 🔐 | Mobile |
| 29 | Order | PUT | `/api/orders/{id}/cancel` | 🔐 | Mobile + Kiosk |

