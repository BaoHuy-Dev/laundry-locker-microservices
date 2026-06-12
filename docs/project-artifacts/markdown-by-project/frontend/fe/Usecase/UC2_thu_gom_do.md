# UC2: Staff Thu Gom Đồ Từ Tủ (COLLECT)

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Tổng quan

Khi đơn hàng ở trạng thái `WAITING` (khách đã bỏ đồ), Partner chấp nhận đơn và tạo mã AccessCode loại COLLECT cho Staff. Staff (nhân viên thực địa, không có tài khoản) đến tủ, nhập mã trên tablet IoT, tủ mở ra, Staff lấy đồ mang về cửa hàng.

**Actor chính:** PARTNER + Staff (actor ngoài)
**Enum sử dụng:** [OrderStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/controller/OrderController.java#81-93), [BoxStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/helper/OrderBoxHelper.java#117-121), `AccessCodeStatus`, `AccessCodeAction`

---

## Bước 1: Partner Xem Đơn Hàng Đang Chờ

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | PARTNER (đã đăng nhập, status = APPROVED) |
| **Endpoint** | `GET /api/partner/orders/pending` |
| **Authorization** | `Bearer {JWT token}` — `@PreAuthorize("hasRole('PARTNER')")` |
| **Service** | [PartnerService.getPendingOrders()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/service/PartnerService.java#L219-L222) |

### Request

```
GET /api/partner/orders/pending?page=0&size=20
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Response — `ApiResponse<Page<OrderResponse>>`

```json
{
  "code": "SUCCESS",
  "data": {
    "content": [
      {
        "id": 42,
        "orderCode": "ORD-20260227-A1B2C3",
        "status": "WAITING",
        "senderName": "Nguyễn Văn A",
        "lockerName": "Tủ KTX A",
        "lockerCode": "ESP8266_LOCKER_01",
        "sendBoxNumber": 5,
        "totalPrice": 75000,
        "createdAt": "2026-02-27T15:00:00"
      }
    ],
    "totalElements": 3,
    "totalPages": 1
  }
}
```

### Logic xử lý

1. Lấy Partner từ userId (JWT) → validate `PartnerStatus == APPROVED`
2. Lấy danh sách storeIds thuộc Partner
3. Query orders có `OrderStatus == WAITING` thuộc các store đó

### State Transitions

```
Không thay đổi (read-only)
```

---

## Bước 2: Partner Chấp Nhận Đơn + Tạo AccessCode COLLECT

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | PARTNER |
| **Endpoint** | `POST /api/partner/orders/{orderId}/accept` |
| **Authorization** | `Bearer {JWT token}` — `@PreAuthorize("hasRole('PARTNER')")` |
| **Service** | [PartnerService.acceptOrderAndGenerateCode()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/service/PartnerService.java#L247-L259) |

### Request

```
POST /api/partner/orders/42/accept?expirationHours=12&notes=Anh Minh đi lấy
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

| Param | Type | Required | Mô tả |
|-------|------|----------|-------|
| `expirationHours` | `Integer` | ❌ | Số giờ có hiệu lực (mặc định: 24) |
| `notes` | `String` | ❌ | Ghi chú cho staff |

### Response — `ApiResponse<StaffAccessCodeResponse>`

```json
{
  "code": "SUCCESS",
  "data": {
    "id": 15,
    "code": "XK7M2P",
    "orderId": 42,
    "partnerId": 3,
    "action": "COLLECT",
    "status": "ACTIVE",
    "expiresAt": "2026-02-28T03:00:00",
    "staffName": null,
    "notes": "Anh Minh đi lấy",
    "createdAt": "2026-02-27T15:05:00",
    "orderLockerCode": "ESP8266_LOCKER_01",
    "orderLockerName": "Tủ KTX A",
    "orderBoxNumbers": "5, 8",
    "customerName": "Nguyễn Văn A"
  }
}
```

### Logic xử lý

1. Validate Partner `APPROVED` + đơn hàng thuộc store của Partner
2. Validate `OrderStatus == WAITING` → nếu không throw lỗi
3. Nếu đã có AccessCode ACTIVE cũ cho đơn + action `COLLECT` → hủy code cũ (`CANCELLED`)
4. Sinh mã code duy nhất (ví dụ: `XK7M2P`)
5. Tạo [StaffAccessCode](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/service/StaffAccessCodeService.java#37-305) với `status = ACTIVE`, `expiresAt = now + expirationHours`
6. Partner gửi mã này cho Staff (qua tin nhắn, Zalo, v.v.)

### State Transitions

```
AccessCodeStatus: [null] → ACTIVE
AccessCodeAction: COLLECT
⮕ Nếu có code ACTIVE cũ → ACTIVE → CANCELLED
```

---

## Bước 3: Staff Nhập Mã Mở Tủ Lấy Đồ

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | Staff (actor ngoài, không có tài khoản) |
| **Endpoint** | `POST /api/iot/unlock-with-code` |
| **Authorization** | Không yêu cầu (public API cho tablet IoT) |
| **Service** | [StaffAccessCodeService.unlockWithCode()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/service/StaffAccessCodeService.java#L84-L144) |
| **Controller** | [IoTController.unlockWithCode()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/iot/controller/IoTController.java#L96-L107) |

### Request Body — [StaffCodeUnlockRequest](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/dto/request/StaffCodeUnlockRequest.java#11-26)

```json
{
  "orderId": 42,
  "accessCode": "XK7M2P",
  "staffName": "Trần Văn Minh"
}
```

| Field | Type | Required | Mô tả |
|-------|------|----------|-------|
| `orderId` | `Long` | ✅ | ID đơn hàng |
| `accessCode` | `String` | ✅ | Mã truy cập do Partner tạo |
| `staffName` | `String` | ❌ | Tên staff (lưu tracking) |

### Response — `ApiResponse<StaffCodeUnlockResponse>`

**Thành công:**
```json
{
  "code": "BOX_UNLOCKED",
  "data": {
    "success": true,
    "message": "Box unlocked successfully",
    "orderId": 42,
    "orderStatus": "COLLECTED",
    "action": "COLLECT",
    "boxes": [
      { "boxId": 5, "boxNumber": "5", "size": "MEDIUM" },
      { "boxId": 8, "boxNumber": "8", "size": "MEDIUM" }
    ],
    "lockerCode": "ESP8266_LOCKER_01",
    "lockerName": "Tủ KTX A",
    "lockerAddress": "Tầng 1, KTX Khu A, ĐHQG",
    "unlockToken": "f1e2d3c4-b5a6-7890-1234-567890abcdef",
    "unlockTimestamp": 1740647100000
  }
}
```

**Thất bại:**
```json
{
  "code": "UNLOCK_FAILED",
  "data": {
    "success": false,
    "orderId": 42,
    "message": "Invalid or expired access code"
  }
}
```

### Logic xử lý chi tiết

1. **Tìm AccessCode** → validate ACTIVE + chưa hết hạn
2. **Validate** code khớp orderId
3. **Validate** `OrderStatus == WAITING` (required cho COLLECT)
4. **Lấy boxes** → `sendBox` + `sendBoxes` (có thể nhiều ngăn)
5. **Đánh dấu code đã dùng:** `AccessCodeStatus: ACTIVE → USED`, lưu `staffName`, `usedAt`
6. **Cập nhật order:**
   - `OrderStatus: WAITING → COLLECTED`
   - Giải phóng tất cả send boxes: `BoxStatus: OCCUPIED → AVAILABLE`
7. **🔌 Gửi MQTT OPEN cho từng box:**

```
Topic:   locker/commands/ESP8266_LOCKER_01
Payload: {"box_id": 5, "action": "OPEN"}    ← box #5
         {"box_id": 8, "action": "OPEN"}    ← box #8
QoS:     1
```

8. **Gửi notification** cho khách: *"Đơn hàng #42 đã được nhận và đang xử lý giặt."*

### State Transitions

```
AccessCodeStatus: ACTIVE → USED
OrderStatus:      WAITING → COLLECTED
BoxStatus:        OCCUPIED → AVAILABLE  (tất cả sendBox)

🔌 MQTT: Backend gửi "OPEN" → ESP8266 mở khóa tủ vật lý cho từng box
📱 Notification: gửi cho USER "Đơn hàng đang được xử lý"
```

> [!IMPORTANT]
> **Validate chặt:** Mã AccessCode phải `ACTIVE`, chưa hết hạn (`expiresAt > now`), action phải `COLLECT`, và [OrderStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/controller/OrderController.java#81-93) phải `WAITING`. Bất kỳ điều kiện nào không thỏa → trả `success: false`.
