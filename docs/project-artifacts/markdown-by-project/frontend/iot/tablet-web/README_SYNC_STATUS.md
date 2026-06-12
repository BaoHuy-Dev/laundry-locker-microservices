# 📋 Trạng thái đồng bộ: Kiosk Frontend ↔ Backend ↔ ESP8266

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

> **Cập nhật:** 2026-02-23  
> **Mục đích:** Tài liệu tổng hợp cho team Frontend & Backend biết trạng thái đồng bộ API, những gì đang hoạt động, và những gì cần sửa/thêm.

---

## ✅ Ưu điểm (Đã hoạt động tốt)

### 1. Toàn bộ 19 API calls đều khớp với Backend
| # | Frontend API | Backend Endpoint | Status |
|---|---|---|---|
| 1 | `sendOtp(email)` | `POST /api/auth/email/send-otp` | ✅ |
| 2 | `verifyOtp(email, otp)` | `POST /api/auth/email/verify-otp` | ✅ |
| 3 | `completeRegistration(...)` | `POST /api/auth/email/complete-registration` | ✅ |
| 4 | `phoneLogin(idToken)` | `POST /api/auth/phone-login` | ✅ |
| 5 | `phoneCompleteRegistration(...)` | `POST /api/auth/complete-registration` | ✅ |
| 6 | `kioskQuickRegister(tempToken)` | `POST /api/auth/kiosk/quick-register` | ✅ |
| 7 | `getServices(token, lockerId)` | `GET /api/services?lockerId={id}&category=STORAGE` | ✅ |
| 8 | `getAvailableBoxes(lockerId)` | `GET /api/lockers/{id}/boxes/available` | ✅ |
| 9 | `getLockerById(lockerId)` | `GET /api/lockers/{id}` | ✅ |
| 10 | `createOrder(token, data)` | `POST /api/orders` | ✅ |
| 11 | `createPayment(token, ...)` | `POST /api/payments/create` | ✅ |
| 12 | `verifyPin(pinCode, boxId)` | `POST /api/iot/verify-pin` | ✅ |
| 13 | `unlockBox(pinCode, boxId, actionType)` | `POST /api/iot/unlock` | ✅ |
| 14 | `unlockWithCode(orderId, accessCode, staffName)` | `POST /api/iot/unlock-with-code` | ✅ |
| 15 | `getOrderByPin(pinCode)` | `GET /api/orders/pin/{pinCode}` | ✅ |
| 16 | `confirmOrder(token, orderId)` | `PUT /api/orders/{id}/confirm` | ✅ |
| 17 | `getOrderById(token, orderId)` | `GET /api/orders/{id}` | ✅ |
| 18 | `getOrderStatus(token, orderId)` | `GET /api/orders/{id}/status` | ✅ |
| 19 | `getAllBoxes(lockerId, token)` | `GET /api/lockers/{id}/boxes` | ✅ |

### 2. Kiến trúc triển khai linh hoạt
- Kiosk đọc `lockerId` từ **URL Params** (`?lockerId=1`) hoặc **`.env`**
- **1 bản build** phục vụ N kiosk — chỉ cần đổi URL param
- Tên cửa hàng + địa chỉ hiển thị tự động từ backend (qua `GET /api/lockers/{id}`)

### 3. MQTT tích hợp đầy đủ
- Backend `IoTService.unlockBox()` → publish MQTT `{"box_id": N, "action": "OPEN"}`
- ESP8266 subscribe topic MQTT → nhận lệnh → relay mở solenoid lock
- Broker: `broker.hivemq.com:1883` (public, không cần auth)

### 4. PinScreen tra cứu thông minh
- Gọi `GET /api/orders/pin/{pinCode}` để lấy `boxId` trước
- Rồi mới gọi `verify-pin` và `unlock` với boxId chính xác
- Tránh lỗi `@NotNull boxId` từ backend

---

## ⚠️ Khuyết điểm & Vấn đề cần sửa

### 🔴 CRITICAL — Cần sửa ngay

#### 1. MQTT Topic Mismatch (ĐÃ SỬA ở ESP8266)
- **Vấn đề:** Backend dùng `box.getLocker().getCode()` (= `LOC-01-001`) làm deviceId, ESP trước đây dùng `ESP8266_LOCKER_01`
- **Hậu quả:** Tín hiệu MQTT gửi đi nhưng ESP8266 không nhận được
- **Đã sửa:** `config.h` → `DEVICE_ID = "LOC-01-001"`

#### 2. API `GET /api/lockers/{id}` yêu cầu JWT Token (401)
- **Vấn đề:** Kiosk muốn hiện tên cửa hàng trên HomeScreen (trước khi đăng nhập), nhưng API trả 401
- **Hiện tại:** App retry sau khi JWT có → hiện tên cửa hàng từ BoxSelectionScreen
- **Đề xuất Backend:** Thêm `/api/lockers/{id}` vào whitelist security, vì thông tin locker là public

#### 3. API `POST /api/iot/verify-pin` bắt buộc `boxId` (@NotNull)
- **Vấn đề:** Khi user nhập PIN ở màn hình PinScreen, frontend chưa biết boxId
- **Hiện tại:** Frontend gọi `GET /api/orders/pin/{pinCode}` trước để lấy boxId → rồi mới verify
- **Đề xuất Backend:** Bỏ `@NotNull` trên `boxId` trong `VerifyPinRequest.java` và `UnlockBoxRequest.java`, để backend tự tìm box qua PIN code (giảm 1 API call)

### 🟡 MEDIUM — Nên sửa

#### 4. ESP8266 mỗi thiết bị chỉ quản lý 1 box
- **Vấn đề:** `BOX_ID = 1` hardcoded trong `config.h`. Backend thì 1 locker có nhiều box
- **Hậu quả:** Cần 1 ESP8266 cho mỗi ô tủ (không scale tốt)
- **Đề xuất:** Sửa ESP8266 để quản lý nhiều relay (1 ESP = 1 locker = N boxes), hoặc dùng bảng mapping
- **Ghi chú MQTT:** Backend gửi `{"box_id": 3, "action": "OPEN"}` — ESP chỉ xử lý nếu `box_id == BOX_ID`, nên sẽ bỏ qua box khác

#### 5. Payment callback flow chưa hoàn thiện
- **Vấn đề:** Sau khi thanh toán VNPay/MoMo, redirect về Kiosk nhưng chưa auto-detect
- **Hiện tại:** Kiosk hiện nút "Mở tủ" → user phải bấm thủ công sau khi thanh toán
- **Đề xuất:** Thêm polling hoặc WebSocket để tự động detect payment success → auto unlock

#### 6. `LOCKER_CODE` không còn được sử dụng
- **Vấn đề:** Biến `LOCKER_CODE` ở `.env` và `App.jsx` không còn tham chiếu ở đâu
- **Đề xuất:** Có thể xoá bỏ LOCKER_CODE khỏi `.env` nếu không dùng

### 🟢 LOW — Cải thiện nếu có thời gian

#### 7. Không có cơ chế refresh token
- **Vấn đề:** JWT hết hạn → kiosk phải login lại
- **Đề xuất:** Thêm auto-refresh token trước khi hết hạn (Backend đã có endpoint `POST /api/auth/refresh-token`)

#### 8. ESP8266 `BACKEND_URL` hardcoded
- **Vấn đề:** `config.h` dùng `http://192.168.1.10:8080` — phải flash lại firmware mỗi lần đổi IP
- **Đề xuất:** Dùng mDNS hoặc biến cấu hình runtime

---

## 📊 Sơ đồ luồng tín hiệu: Mở tủ

```
Kiosk (Browser)                    Backend (Spring Boot)               ESP8266
     │                                    │                                │
     │ POST /api/iot/unlock               │                                │
     │ {pinCode, boxId, actionType}       │                                │
     │──────────────────────────────────▶ │                                │
     │                                    │  1. Validate PIN + boxId       │
     │                                    │  2. Generate unlockToken       │
     │                                    │  3. MQTT publish               │
     │                                    │     topic: locker/commands/    │
     │                                    │            LOC-01-001          │
     │                                    │     payload: {"box_id":1,     │
     │                                    │      "action":"OPEN"}          │
     │                                    │──────────────────────────────▶ │
     │                                    │                                │ 4. mqttCallback()
     │  ◀──────── JSON response ─────────│                                │    box_id match?
     │  {success:true, unlockToken:...}   │                                │    → unlockBox()
     │                                    │                                │    → relay ON 5s
     │                                    │                                │    → relay OFF
```

---

## 🗂️ File Structure

```
iot/
├── include/
│   └── config.h          ← WiFi, MQTT, GPIO, DEVICE_ID config
├── src/
│   ├── main.cpp          ← ESP8266 entry point (HTTP + MQTT + relay)
│   └── locker_controller.cpp ← Relay control functions
└── tablet-web/
    ├── .env              ← VITE_LOCKER_ID, VITE_API_URL
    ├── src/
    │   ├── api.js        ← All API calls (19 functions)
    │   └── App.jsx       ← Kiosk UI (all screens)
    └── README_SYNC_STATUS.md ← (this file)
```
