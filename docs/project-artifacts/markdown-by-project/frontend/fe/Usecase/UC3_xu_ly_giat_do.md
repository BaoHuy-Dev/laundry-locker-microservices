# UC3: Partner Xử Lý Giặt Đồ

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Tổng quan

Sau khi Staff thu gom đồ (`COLLECTED`), Partner cập nhật trạng thái sang `PROCESSING`, cân đồ thực tế (cập nhật giá chính xác cho dịch vụ PER_WEIGHT), và khi giặt xong đánh dấu `READY` + tự động tạo mã AccessCode RETURN cho Staff.

**Actor chính:** PARTNER
**Enum sử dụng:** [OrderStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/controller/OrderController.java#81-93), `PricingType`, `AccessCodeAction`

---

## Bước 1: Cập Nhật Trạng Thái → PROCESSING

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | PARTNER |
| **Endpoint** | `PUT /api/partner/orders/{orderId}/process` |
| **Authorization** | `Bearer {JWT token}` — `@PreAuthorize("hasRole('PARTNER')")` |
| **Service** | [PartnerService.updateOrderToProcessing()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/service/PartnerService.java#L261-L274) |

### Request

```
PUT /api/partner/orders/42/process
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Không có Request Body.**

### Response — `ApiResponse<OrderResponse>`

```json
{
  "code": "SUCCESS",
  "data": {
    "id": 42,
    "status": "PROCESSING",
    "orderCode": "ORD-20260227-A1B2C3",
    "...": "..."
  }
}
```

### Logic xử lý

1. Lấy Partner → validate `APPROVED` + đơn thuộc store của Partner
2. **Validate:** `OrderStatus == COLLECTED` → nếu không throw lỗi
3. Set `OrderStatus = PROCESSING`
4. Gửi notification cho khách: *"Đồ đang được giặt/xử lý"*

### State Transitions

```
OrderStatus: COLLECTED → PROCESSING
📱 Notification: ORDER_STATUS gửi cho USER
```

---

## Bước 2: Cập Nhật Cân Nặng Thực Tế

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | PARTNER |
| **Endpoint** | `PUT /api/partner/orders/{orderId}/weight` |
| **Authorization** | `Bearer {JWT token}` — `@PreAuthorize("hasRole('PARTNER')")` |
| **Service** | [PartnerService.updateOrderWeight()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/service/PartnerService.java#L305-L335) |

### Request Body — `UpdateOrderWeightRequest`

```json
{
  "actualWeight": 4.5,
  "weightUnit": "kg",
  "staffNote": "Đồ nhiều vải cotton, giặt riêng",
  "items": [
    {
      "serviceId": 1,
      "quantity": 4.5,
      "description": "Quần áo thường"
    }
  ]
}
```

| Field | Type | Required | Mô tả |
|-------|------|----------|-------|
| `actualWeight` | `BigDecimal` | ✅ | Cân nặng thực tế (kg) |
| `weightUnit` | `String` | ❌ | Đơn vị (mặc định: "kg") |
| `staffNote` | `String` | ❌ | Ghi chú của staff |
| `items` | `List<OrderItemRequest>` | ❌ | Cập nhật danh sách dịch vụ/số lượng |

### Response — `ApiResponse<OrderResponse>`

```json
{
  "code": "SUCCESS",
  "data": {
    "id": 42,
    "status": "PROCESSING",
    "actualWeight": 4.5,
    "weightUnit": "kg",
    "totalPrice": 67500,
    "staffNote": "Đồ nhiều vải cotton, giặt riêng",
    "orderDetails": [
      {
        "serviceId": 1,
        "serviceName": "Giặt đồ thường",
        "quantity": 4.5,
        "price": 67500
      }
    ]
  }
}
```

### Logic xử lý

1. Validate Partner + đơn thuộc store
2. **Validate:** `OrderStatus ∈ {COLLECTED, PROCESSING}` → cập nhật cả 2 trạng thái
3. Set `actualWeight`, `weightUnit`, `staffNote`
4. Nếu `items` được gửi → xóa orderDetails cũ, tạo mới với giá tính lại
5. **Tính lại giá:** Nếu dịch vụ tính theo kg → `price × actualWeight`

### State Transitions

```
OrderStatus: giữ nguyên (COLLECTED hoặc PROCESSING)
⮕ Cập nhật dữ liệu: actualWeight, totalPrice (recalculated)
```

> [!NOTE]
> Đây là bước quan trọng cho dịch vụ `LAUNDRY` (PER_WEIGHT). Giá ước tính ban đầu (từ `estimatedWeight`) được thay bằng giá chính xác từ `actualWeight`.

---

## Bước 3: Đánh Dấu Hoàn Thành + Tạo Mã RETURN

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | PARTNER |
| **Endpoint** | `PUT /api/partner/orders/{orderId}/ready` |
| **Authorization** | `Bearer {JWT token}` — `@PreAuthorize("hasRole('PARTNER')")` |
| **Service** | [PartnerService.markOrderReadyAndGenerateCode()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/service/PartnerService.java#L276-L291) |

### Request

```
PUT /api/partner/orders/42/ready?expirationHours=24&notes=Trả tủ ô số 12
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

| Param | Type | Required | Mô tả |
|-------|------|----------|-------|
| `expirationHours` | `Integer` | ❌ | Hiệu lực mã (mặc định: 24h) |
| `notes` | `String` | ❌ | Ghi chú cho staff |

### Response — `ApiResponse<StaffAccessCodeResponse>`

```json
{
  "code": "SUCCESS",
  "data": {
    "id": 18,
    "code": "RT9N4W",
    "orderId": 42,
    "partnerId": 3,
    "action": "RETURN",
    "status": "ACTIVE",
    "expiresAt": "2026-02-28T16:00:00",
    "notes": "Trả tủ ô số 12",
    "orderLockerCode": "ESP8266_LOCKER_01",
    "orderLockerName": "Tủ KTX A",
    "customerName": "Nguyễn Văn A"
  }
}
```

### Logic xử lý

1. Validate Partner + đơn thuộc store
2. **Validate:** `OrderStatus ∈ {PROCESSING, COLLECTED}` → nếu không throw lỗi
3. Set `OrderStatus = READY`
4. Tạo AccessCode mới với `action = RETURN`, `status = ACTIVE`
5. Gửi notification: *"Đồ đã giặt xong, chờ trả vào tủ"*
6. Partner gửi mã RETURN cho Staff

### State Transitions

```
OrderStatus:      PROCESSING → READY  (hoặc COLLECTED → READY)
AccessCodeStatus: [null] → ACTIVE (action = RETURN)
📱 Notification:  ORDER_STATUS → READY gửi cho USER
```
