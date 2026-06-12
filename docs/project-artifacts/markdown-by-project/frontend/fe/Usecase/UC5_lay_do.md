# UC5: Khách Hàng Lấy Đồ (Pickup)

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Tổng quan

Sau khi nhận notification "Đồ sẵn sàng", khách đến tủ locker, nhập PIN mới trên tablet IoT, mở tủ, lấy đồ. Có **2 cách** hoàn thành đơn: (a) xác nhận qua IoT tablet, (b) xác nhận qua mobile app.

**Actor chính:** USER (khách hàng)
**Enum sử dụng:** [OrderStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/controller/OrderController.java#81-93), [BoxStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/iot/service/IoTService.java#167-187), `PaymentStatus`

---

## Bước 1: Xác Thực PIN Tại Tủ

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | USER (tại tablet trên tủ locker) |
| **Endpoint** | `POST /api/iot/verify-pin` |
| **Authorization** | Không yêu cầu |
| **Service** | [IoTService.verifyPin()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/iot/service/IoTService.java#L42-L68) |

### Request Body — [VerifyPinRequest](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/iot/dto/request/VerifyPinRequest.java#12-25)

```json
{
  "boxId": 12,
  "pinCode": "593817"
}
```

### Response — `ApiResponse<VerifyPinResponse>`

```json
{
  "data": {
    "valid": true,
    "orderId": 42,
    "boxId": 12,
    "boxNumber": 12,
    "lockerCode": "ESP8266_LOCKER_01",
    "orderStatus": "RETURNED",
    "message": null
  }
}
```

### Logic xử lý

1. Tìm Box theo `boxId`
2. Tìm Order theo `pinCode`
3. Kiểm tra `OrderStatus == RETURNED` + PIN khớp `receiveBox`

### State Transitions

```
Không thay đổi (read-only)
```

---

## Bước 2: Mở Tủ Lấy Đồ

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | USER (tại tablet) |
| **Endpoint** | `POST /api/iot/unlock` |
| **Authorization** | Không yêu cầu |
| **Service** | [IoTService.unlockBox()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/iot/service/IoTService.java#L71-L109) |

### Request Body — [UnlockBoxRequest](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/iot/dto/request/UnlockBoxRequest.java#12-28)

```json
{
  "boxId": 12,
  "pinCode": "593817",
  "actionType": "PICKUP"
}
```

### Response — `ApiResponse<UnlockBoxResponse>`

```json
{
  "data": {
    "success": true,
    "boxId": 12,
    "boxNumber": 12,
    "lockerCode": "ESP8266_LOCKER_01",
    "orderId": 42,
    "unlockToken": "d4c3b2a1-9876-5432-fedc-ba0987654321",
    "unlockTimestamp": 1740820500000
  }
}
```

### Logic xử lý

1. Validate PIN + box (như bước 1)
2. Sinh `unlockToken`
3. **🔌 Gửi MQTT OPEN:**

```
Topic:   locker/commands/ESP8266_LOCKER_01
Payload: {"box_id": 12, "action": "OPEN"}
QoS:     1
```

### State Transitions

```
Không thay đổi database
🔌 MQTT: Backend → "OPEN" → ESP8266 mở khóa tủ
```

---

## Bước 3: Xác Nhận Đã Lấy Đồ

Có **2 cách** xác nhận pickup:

### Cách A: Qua IoT Tablet

| Thông tin | Chi tiết |
|-----------|----------|
| **Endpoint** | `POST /api/iot/pickup` |
| **Authorization** | Không yêu cầu |
| **Service** | [IoTService.confirmPickup()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/iot/service/IoTService.java#L112-L149) |

#### Request Body — [PickupRequest](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/iot/dto/request/PickupRequest.java#10-22)

```json
{
  "orderId": 42,
  "boxId": 12
}
```

| Field | Type | Required |
|-------|------|----------|
| `orderId` | `Long` | ✅ |
| `boxId` | `Long` | ✅ |

#### Response — `ApiResponse<PickupResponse>`

```json
{
  "data": {
    "success": true,
    "orderId": 42,
    "orderStatus": "COMPLETED",
    "message": "Pickup confirmed successfully",
    "overtimeFee": 0,
    "totalPrice": 67500
  }
}
```

---

### Cách B: Qua Mobile App

| Thông tin | Chi tiết |
|-----------|----------|
| **Endpoint** | `PUT /api/orders/{orderId}/complete` |
| **Authorization** | `Bearer {JWT token}` — `@PreAuthorize("isAuthenticated()")` |
| **Service** | [OrderService.completeOrderByCustomer()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/service/OrderService.java#L242-L280) |

#### Request

```
PUT /api/orders/42/complete
Authorization: Bearer eyJhbGciOiJ...
```

**Không có Request Body.** userId được extract từ JWT.

#### Response — `ApiResponse<OrderResponse>`

```json
{
  "code": "ORDER_COMPLETED",
  "data": {
    "id": 42,
    "status": "COMPLETED",
    "totalPrice": 67500,
    "isOvertime": false,
    "overtimeHours": 0,
    "completedAt": "2026-02-28T10:00:00",
    "isPaid": true,
    "...": "..."
  }
}
```

---

### Logic xử lý chung (cả 2 cách)

1. **Validate** `OrderStatus == RETURNED`
2. **Tính phí quá hạn** (nếu lấy trễ hơn `pickupDeadline`):

```java
overtimeHours = ChronoUnit.HOURS.between(pickupDeadline, now);
overtimeFee = overtimeHours × 500đ/giờ;
overtimeFee = min(overtimeFee, maxOvertimeFee(50000đ));
overtimeFee = min(overtimeFee, totalPrice × 50%);
```

3. Set `OrderStatus = COMPLETED`, `completedAt = now`
4. Giải phóng `receiveBox`: `BoxStatus: OCCUPIED → AVAILABLE`
5. Xóa `pinCode`
6. Gửi notification cho khách

### State Transitions

```
OrderStatus:  RETURNED → COMPLETED
BoxStatus:    OCCUPIED → AVAILABLE  (receiveBox)
PIN Code:     xóa (null)

📱 Notification: ORDER_STATUS → COMPLETED gửi cho USER
```

> [!CAUTION]
> **Phí quá hạn:** Nếu khách lấy đồ trễ hơn 24 giờ kể từ khi staff trả vào tủ:
> - Tính 500đ/giờ trễ
> - Cap tối đa **50.000đ** hoặc **50% totalPrice** (lấy giá trị nhỏ hơn)
> - OvertimeFee cộng vào `totalPrice`

---

## Scheduler liên quan

| Scheduler | Tần suất | Tác động vào UC5 |
|-----------|----------|------------------|
| [sendPickupReminders()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/core/scheduler/OrderSchedulerService.java#199-238) | Mỗi 1 giờ | Nếu đơn `RETURNED` > 24h → gửi notification nhắc nhở |
| [autoReleaseBoxesAfterCompletion()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/core/scheduler/OrderSchedulerService.java#126-162) | Mỗi 2 phút | Sau khi `COMPLETED` 5 phút → giải phóng box, xóa PIN |
