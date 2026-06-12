# UC1: Khách Hàng Tạo Đơn và Gửi Đồ Vào Tủ

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Tổng quan

Khách hàng (USER) mở app, chọn locker, chọn dịch vụ, tạo đơn hàng. Hệ thống tự động gán box và cấp mã PIN 6 số. Khách đến tủ, nhập PIN trên tablet (thiết bị IoT), tủ mở ra, khách bỏ đồ vào rồi xác nhận trên app.

**Actor chính:** USER (khách hàng)
**Enum sử dụng:** [OrderStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/service/OrderService.java#317-329), [BoxStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/helper/OrderBoxHelper.java#117-121), `ServiceCategory`, `PricingType`, `OrderType`

---

## Bước 1: Tạo Đơn Hàng

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | USER (khách hàng, đã đăng nhập) |
| **Endpoint** | `POST /api/orders` |
| **Authorization** | `Bearer {JWT token}` — `@PreAuthorize("isAuthenticated()")` |
| **Service** | [OrderService.createOrder()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/service/OrderService.java#L97-L145) |
| **Controller** | [OrderController.createOrder()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/controller/OrderController.java#L48-L59) |

### Request Body — [CreateOrderRequest](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/dto/request/CreateOrderRequest.java#15-77)

```json
{
  "type": "LAUNDRY",
  "lockerId": 1,
  "boxIds": [5, 8],
  "serviceCategory": "LAUNDRY",
  "serviceIds": [1, 3],
  "estimatedWeight": 5.0,
  "customerNote": "Giặt nhẹ tay, đồ trắng riêng",
  "receiverPhone": "0901234567",
  "receiverName": "Nguyễn Văn A",
  "intendedReceiveAt": "2026-02-28T14:00:00",
  "promotionCode": "GIAM20K",
  "promotionCodes": ["GIAM20K", "FREESHIP"]
}
```

| Field | Type | Required | Mô tả |
|-------|------|----------|-------|
| `type` | `OrderType` | ✅ | `LAUNDRY` / `DRY_CLEAN` / `STORAGE` |
| `lockerId` | `Long` | ✅ | ID tủ locker |
| `boxIds` | `Set<Long>` | ❌ | ID các ngăn tủ. `null` → hệ thống tự chọn |
| `serviceCategory` | `ServiceCategory` | ❌ | `STORAGE` (giá cố định) / `LAUNDRY` (theo kg) |
| `serviceIds` | `List<Long>` | ❌ | Danh sách ID dịch vụ |
| `estimatedWeight` | `Double` | ❌ | Ước lượng cân nặng (kg), dùng tính giá ước tính |
| `customerNote` | `String` | ❌ | Ghi chú của khách |
| `receiverId` | `Long` | ❌ | ID người nhận (nếu khác người gửi) |
| `receiverPhone` | `String` | ❌ | SĐT người nhận |
| `receiverName` | `String` | ❌ | Tên người nhận |
| `intendedReceiveAt` | `LocalDateTime` | ❌ | Thời gian dự kiến nhận |
| `promotionCode` | `String` | ❌ | Mã khuyến mãi |
| `promotionCodes` | `List<String>` | ❌ | Nhiều mã KM (stackable) |

### Response — `ApiResponse<OrderResponse>`

```json
{
  "code": "ORDER_CREATED",
  "message": "Success",
  "data": {
    "id": 42,
    "orderCode": "ORD-20260227-A1B2C3",
    "type": "LAUNDRY",
    "status": "INITIALIZED",
    "pinCode": "847291",
    "serviceCategory": "LAUNDRY",
    "senderId": 10,
    "senderName": "Nguyễn Quốc Bảo Huy",
    "lockerId": 1,
    "lockerName": "Tủ KTX A",
    "lockerCode": "ESP8266_LOCKER_01",
    "sendBoxNumber": 5,
    "sendBoxNumbers": [5, 8],
    "totalPrice": 75000,
    "originalPrice": 95000,
    "promotionCode": "GIAM20K",
    "promotionDiscount": 20000,
    "createdAt": "2026-02-27T15:00:00",
    "orderDetails": [
      {
        "serviceId": 1,
        "serviceName": "Giặt đồ thường",
        "quantity": 5.0,
        "price": 75000
      }
    ]
  }
}
```

### Logic xử lý chi tiết

1. **Tìm User** từ JWT token → lấy `senderId`
2. **Tìm Locker** theo `lockerId` → validate tồn tại
3. **Xử lý Box:**
   - Nếu `boxIds` có giá trị → validate từng box `AVAILABLE` → set `BoxStatus.OCCUPIED`
   - Nếu `boxIds` rỗng → auto-assign box `AVAILABLE` đầu tiên trong locker → set `BoxStatus.OCCUPIED`
4. **Build Order** với `OrderStatus.INITIALIZED`, sinh `pinCode` 6 số, sinh `orderCode` format `ORD-YYYYMMDD-XXXXXX`
5. **Tính giá:**
   - Nếu dịch vụ đơn vị `kg` + có `estimatedWeight` → `price × estimatedWeight`
   - Nếu dịch vụ giá cố định → `price × 1`
6. **Áp dụng Promotion** nếu có → tính discount, cập nhật `totalPrice`
7. **Lưu Order** vào database

### State Transitions

```
OrderStatus:  [null] → INITIALIZED
BoxStatus:    AVAILABLE → OCCUPIED  (1 hoặc nhiều box)
```

> [!WARNING]
> Nếu khách **không xác nhận** bỏ đồ (bước 4) trong **30 phút**, scheduler [autoCancelUnconfirmedOrders()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/core/scheduler/OrderSchedulerService.java#51-92) tự động:
> - `OrderStatus: INITIALIZED → CANCELED` (cancelReason = 99)
> - `BoxStatus: OCCUPIED → AVAILABLE`
> - Gửi notification `AUTO_CANCEL` cho khách

---

## Bước 2: Xác Thực PIN Tại Tủ (Tablet IoT)

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | USER (tại tablet trên tủ locker) |
| **Endpoint** | `POST /api/iot/verify-pin` |
| **Authorization** | Không yêu cầu (public API cho tablet IoT) |
| **Service** | [IoTService.verifyPin()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/iot/service/IoTService.java#L42-L68) |

### Request Body — [VerifyPinRequest](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/iot/dto/request/VerifyPinRequest.java#12-25)

```json
{
  "boxId": 5,
  "pinCode": "847291"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|-----------|
| `boxId` | `Long` | ✅ | Box phải tồn tại |
| `pinCode` | `String` | ✅ | Regex: `^\d{6}$` — đúng 6 chữ số |

### Response — `ApiResponse<VerifyPinResponse>`

**Thành công:**
```json
{
  "code": "PIN_VALID",
  "data": {
    "valid": true,
    "orderId": 42,
    "boxId": 5,
    "boxNumber": 5,
    "lockerCode": "ESP8266_LOCKER_01",
    "orderStatus": "INITIALIZED",
    "message": null
  }
}
```

**Thất bại:**
```json
{
  "code": "PIN_INVALID",
  "data": {
    "valid": false,
    "boxId": 5,
    "message": "Invalid PIN code"
  }
}
```

### Logic xử lý

1. Tìm Box theo `boxId` → nếu không tìm thấy → trả lỗi
2. Tìm Order theo `pinCode` → nếu không tìm thấy → "Invalid PIN code"
3. Kiểm tra PIN khớp box:
   - `OrderStatus == INITIALIZED` → kiểm tra `sendBox` (khách bỏ đồ vào)
   - `OrderStatus == RETURNED` → kiểm tra `receiveBox` (khách lấy đồ)
   - Trạng thái khác → kiểm tra cả hai
4. Nếu khớp → trả `valid: true` + thông tin order

### State Transitions

```
Không thay đổi trạng thái (read-only)
```

---

## Bước 3: Mở Tủ Bằng PIN

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | USER (tại tablet trên tủ locker) |
| **Endpoint** | `POST /api/iot/unlock` |
| **Authorization** | Không yêu cầu (public API cho tablet IoT) |
| **Service** | [IoTService.unlockBox()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/iot/service/IoTService.java#L71-L109) |

### Request Body — [UnlockBoxRequest](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/iot/dto/request/UnlockBoxRequest.java#12-28)

```json
{
  "boxId": 5,
  "pinCode": "847291",
  "actionType": "DROP_OFF"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|-----------|
| `boxId` | `Long` | ✅ | Box phải tồn tại |
| `pinCode` | `String` | ✅ | Regex: `^\d{6}$` |
| `actionType` | `String` | ❌ | `DROP_OFF` hoặc `PICKUP` |

### Response — `ApiResponse<UnlockBoxResponse>`

```json
{
  "code": "BOX_UNLOCKED",
  "data": {
    "success": true,
    "boxId": 5,
    "boxNumber": 5,
    "lockerCode": "ESP8266_LOCKER_01",
    "orderId": 42,
    "message": null,
    "unlockToken": "a1b2c3d4-e5f6-7890-abcd-1234567890ef",
    "unlockTimestamp": 1740646800000
  }
}
```

### Logic xử lý

1. Validate PIN và box (giống bước 2)
2. Sinh `unlockToken` = UUID ngẫu nhiên
3. **🔌 Gửi MQTT OPEN command đến ESP8266:**

```
Topic:   locker/commands/ESP8266_LOCKER_01
Payload: {"box_id": 5, "action": "OPEN"}
QoS:     1
```

4. Trả response thành công (MQTT là best-effort, nếu lỗi không rollback)

### State Transitions

```
Không thay đổi trạng thái database
⮕ MQTT: Backend gửi lệnh "OPEN" → ESP8266 mở servo/solenoid tủ vật lý
```

> [!NOTE]
> **MQTT Flow:** Backend → Broker (HiveMQ) → ESP8266 subscriber mở khóa vật lý. Nếu MQTT fail, API vẫn trả success — tủ không mở nhưng đơn hàng vẫn ở đúng trạng thái.

---

## Bước 4: Xác Nhận Đã Bỏ Đồ Vào Tủ

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | USER (trên mobile app) |
| **Endpoint** | `PUT /api/orders/{orderId}/confirm` |
| **Authorization** | `Bearer {JWT token}` — `@PreAuthorize("isAuthenticated()")` |
| **Service** | [OrderService.confirmOrder()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/service/OrderService.java#L530-L547) |

### Request

```
PUT /api/orders/42/confirm
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Không có Request Body.**

### Response — `ApiResponse<OrderResponse>`

```json
{
  "code": "ORDER_CONFIRMED",
  "data": {
    "id": 42,
    "orderCode": "ORD-20260227-A1B2C3",
    "status": "WAITING",
    "pinCode": "847291",
    "...": "..."
  }
}
```

### Logic xử lý

1. Tìm Order theo `orderId` → validate tồn tại
2. **Validate:** [OrderStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/service/OrderService.java#317-329) phải là `INITIALIZED` → nếu không → throw `E_ORDER006`
3. Set `OrderStatus = WAITING`
4. Gửi notification cho khách: *"Đã bỏ đồ, chờ nhân viên thu gom"*

### State Transitions

```
OrderStatus:  INITIALIZED → WAITING
BoxStatus:    giữ nguyên OCCUPIED
⮕ Notification: ORDER_STATUS gửi cho USER
```

---

## Luồng Phụ: Hủy Đơn Hàng

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | USER hoặc ADMIN |
| **Endpoint** | `PUT /api/orders/{orderId}/cancel?reason=1` |
| **Authorization** | `Bearer {JWT token}` — `@PreAuthorize("isAuthenticated()")` |
| **Service** | [OrderService.cancelOrder()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/service/OrderService.java#L282-L303) |

### Request

```
PUT /api/orders/42/cancel?reason=1
```

| Param | Type | Required | Mô tả |
|-------|------|----------|-------|
| `reason` | `Integer` | ❌ | Mã lý do hủy (99 = system auto-cancel) |

### Điều kiện hủy

Chỉ hủy được khi [OrderStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/service/OrderService.java#317-329) ∈ `{INITIALIZED, RESERVED, WAITING}`

### State Transitions

```
OrderStatus:  INITIALIZED / RESERVED / WAITING → CANCELED
BoxStatus:    OCCUPIED → AVAILABLE  (cả sendBox lẫn receiveBox)
⮕ Notification: ORDER_STATUS → CANCELED gửi cho USER
```
