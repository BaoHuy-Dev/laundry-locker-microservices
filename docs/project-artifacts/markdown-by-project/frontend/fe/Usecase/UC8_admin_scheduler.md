# UC8: Quản Lý Hệ Thống — Admin & Scheduler & Staff

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Tổng quan

Hệ thống có 3 **scheduled task** chạy tự động để duy trì trạng thái nhất quán, cùng với các API cho Admin trigger thủ công và Staff module cho nhân viên hệ thống.

**Actor chính:** Hệ thống (Scheduler), ADMIN, Staff accounts
**Enum sử dụng:** [OrderStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/service/OrderService.java#317-329), [BoxStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/helper/OrderBoxHelper.java#117-121), `NotificationType`

---

## Phần A: Scheduler Tự Động

### Task 1: Auto-Cancel Đơn Hàng Chưa Xác Nhận

| Thông tin | Chi tiết |
|-----------|----------|
| **Tần suất** | Mỗi **5 phút** |
| **Config** | `app.scheduler.auto-cancel-rate-ms: 300000` |
| **Timeout** | `app.scheduler.order-confirm-timeout-minutes: 30` |
| **Service** | [OrderSchedulerService.autoCancelUnconfirmedOrders()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/core/scheduler/OrderSchedulerService.java#L61-L91) |

#### Logic xử lý

1. Query: `OrderStatus == INITIALIZED` AND `createdAt < (now - 30 phút)`
2. Với mỗi đơn:
   - Giải phóng `sendBox`: `BoxStatus: OCCUPIED → AVAILABLE`
   - `OrderStatus: INITIALIZED → CANCELED`, `cancelReason = 99`
   - Gửi notification `AUTO_CANCEL` cho khách

#### State Transitions

```
OrderStatus:  INITIALIZED → CANCELED  (cancelReason = 99 = timeout)
BoxStatus:    OCCUPIED → AVAILABLE  (sendBox)
📱 Notification: AUTO_CANCEL → USER "Đơn hàng bị hủy do quá thời gian"
```

#### Notification gửi cho khách

```
Title: "Đơn hàng bị hủy tự động"
Body:  "Đơn hàng #42 đã bị hủy do quá thời gian 30 phút không xác nhận."
Type:  AUTO_CANCEL
```

---

### Task 2: Auto-Release Box Sau Hoàn Thành

| Thông tin | Chi tiết |
|-----------|----------|
| **Tần suất** | Mỗi **2 phút** |
| **Config** | `app.scheduler.box-release-rate-ms: 120000` |
| **Grace period** | `app.scheduler.box-release-delay-minutes: 5` |
| **Service** | [OrderSchedulerService.autoReleaseBoxesAfterCompletion()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/core/scheduler/OrderSchedulerService.java#L134-L161) |

#### Logic xử lý

1. Query: `OrderStatus == COMPLETED` AND vẫn còn box reference AND `completedAt < (now - 5 phút)`
2. Với mỗi đơn:
   - `sendBox`: nếu `OCCUPIED/RESERVED` → `AVAILABLE`, rồi set reference = `null`
   - `receiveBox`: nếu `OCCUPIED/RESERVED` → `AVAILABLE`, rồi set reference = `null`
   - Xóa `pinCode`, `pinCodeIssuedAt` (bảo mật)

#### State Transitions

```
BoxStatus:    OCCUPIED/RESERVED → AVAILABLE  (cả sendBox + receiveBox)
Order fields: sendBox, receiveBox → null
              pinCode, pinCodeIssuedAt → null
```

---

### Task 3: Nhắc Nhở Lấy Đồ

| Thông tin | Chi tiết |
|-----------|----------|
| **Tần suất** | Mỗi **1 giờ** |
| **Config** | `app.scheduler.reminder-rate-ms: 3600000` |
| **Threshold** | `app.scheduler.pickup-reminder-hours: 24` |
| **Service** | [OrderSchedulerService.sendPickupReminders()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/core/scheduler/OrderSchedulerService.java#L207-L237) |

#### Logic xử lý

1. Query: `OrderStatus == RETURNED` AND `updatedAt < (now - 24h)`
2. Gửi notification cho từng khách

#### Notification

```
Title: "Nhắc nhở lấy đồ"
Body:  "Đơn hàng #42 đã sẵn sàng hơn 24 giờ. Vui lòng đến lấy đồ tại tủ ESP8266_LOCKER_01, ô số 12. Mã PIN: 593817"
Type:  PICKUP_REMINDER
```

#### State Transitions

```
Không thay đổi trạng thái (chỉ gửi notification)
```

---

## Phần B: Admin API — Trigger Scheduler Thủ Công

### Trigger Auto-Cancel

| Thông tin | Chi tiết |
|-----------|----------|
| **Endpoint** | `POST /api/admin/scheduler/auto-cancel` |
| **Authorization** | `@PreAuthorize("hasRole('ADMIN')")` |

#### Request

```
POST /api/admin/scheduler/auto-cancel
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

#### Response

```json
{
  "data": {
    "message": "Auto-cancel job completed",
    "canceledCount": 3
  }
}
```

---

### Trigger Release Boxes

| Thông tin | Chi tiết |
|-----------|----------|
| **Endpoint** | `POST /api/admin/scheduler/release-boxes` |
| **Authorization** | `@PreAuthorize("hasRole('ADMIN')")` |

---

### Trigger Pickup Reminders

| Thông tin | Chi tiết |
|-----------|----------|
| **Endpoint** | `POST /api/admin/scheduler/pickup-reminders` |
| **Authorization** | `@PreAuthorize("hasRole('ADMIN')")` |

---

## Phần C: Staff Module APIs

Staff module cung cấp API cho nhân viên hệ thống (có account, khác với "Staff actor ngoài" dùng AccessCode).

### Xem đơn theo trạng thái

| Endpoint | Service | Mô tả |
|----------|---------|-------|
| `GET /api/staff/orders/waiting` | `StaffService.getWaitingOrders()` | Đơn chờ thu gom |
| `GET /api/staff/orders/processing` | `StaffService.getProcessingOrders()` | Đơn đang xử lý (`COLLECTED` + `PROCESSING`) |
| `GET /api/staff/orders/ready` | `StaffService.getReadyOrders()` | Đơn sẵn sàng trả |

### Tự gán đơn hàng

| Thông tin | Chi tiết |
|-----------|----------|
| **Endpoint** | `PUT /api/staff/orders/{orderId}/assign` |
| **Authorization** | `@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")` |
| **Service** | [StaffService.assignOrderToStaff()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/staff/service/StaffService.java#L86-L115) |

#### Request

```
PUT /api/staff/orders/42/assign
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Không có Request Body.** Staff ID lấy từ JWT.

#### Response — `ApiResponse<OrderResponse>`

```json
{
  "data": {
    "id": 42,
    "status": "WAITING",
    "staffId": 20,
    "staffName": "Trần Minh"
  }
}
```

#### Logic xử lý

1. Validate `OrderStatus == WAITING`
2. Validate chưa có staff gán (`order.staff == null`)
3. Set `order.staff = staffUser`

### Dashboard Staff

| Thông tin | Chi tiết |
|-----------|----------|
| **Endpoint** | `GET /api/staff/orders/summary` |
| **Service** | [StaffService.getOrderSummary()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/staff/service/StaffService.java#L117-L141) |

#### Response — `ApiResponse<StaffOrderSummaryResponse>`

```json
{
  "data": {
    "waitingCount": 5,
    "processingCount": 3,
    "collectedCount": 2,
    "readyCount": 1,
    "recentOrders": [
      {
        "id": 42,
        "status": "WAITING",
        "senderName": "Nguyễn Văn A",
        "createdAt": "2026-02-27T15:00:00"
      }
    ]
  }
}
```

### Staff Mở Tủ Bằng Master PIN

| Thông tin | Chi tiết |
|-----------|----------|
| **Endpoint** | `POST /api/staff/unlock-box` |
| **Service** | [StaffService.unlockBox()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/staff/service/StaffService.java#L157-L180) |

#### Request Body — `StaffUnlockBoxRequest`

```json
{
  "boxId": 5,
  "masterPin": "999999",
  "orderId": 42
}
```

| Field | Type | Required | Mô tả |
|-------|------|----------|-------|
| `boxId` | `Long` | ✅ | ID box cần mở |
| `masterPin` | `String` | ✅ | Master PIN (config: `app.staff.master-pin`) |
| `orderId` | `Long` | ❌ | Liên kết với đơn hàng |

#### Response — `ApiResponse<StaffUnlockBoxResponse>`

```json
{
  "data": {
    "success": true,
    "boxId": 5,
    "boxNumber": 5,
    "lockerName": "Tủ KTX A",
    "orderId": 42,
    "unlockToken": "uuid-here",
    "message": "Box unlocked successfully"
  }
}
```

> [!WARNING]
> **Master PIN** (`999999` mặc định) cho phép mở BẤT KỲ box nào. Config qua `app.staff.master-pin` trong `application.properties`. Chỉ nên dùng cho nhân viên hệ thống đã xác thực.

---

## Tổng kết: Bảng Scheduler

| Task | Tần suất | Query | Action | Notification |
|------|----------|-------|--------|-------------|
| Auto-Cancel | 5 phút | `INITIALIZED` > 30 phút | `→ CANCELED`, release box | `AUTO_CANCEL` |
| Auto-Release | 2 phút | `COMPLETED` > 5 phút, still has box | Release box, clear PIN | Không |
| Pickup Reminder | 1 giờ | `RETURNED` > 24 giờ | Gửi nhắc nhở | `PICKUP_REMINDER` |
