# UC4: Staff Trả Đồ Vào Tủ (RETURN)

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Tổng quan

Khi đồ giặt xong (`READY`), Staff dùng mã RETURN (do Partner tạo ở UC3 bước 3) để mở tủ locker, bỏ đồ sạch vào. Hệ thống tự động cập nhật trạng thái, đánh dấu box `OCCUPIED`, và sinh mã PIN mới cho khách đến lấy.

**Actor chính:** Staff (actor ngoài)
**Enum sử dụng:** [OrderStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/service/OrderService.java#317-329), [BoxStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/iot/service/IoTService.java#167-187), `AccessCodeStatus`, `AccessCodeAction`

---

## Bước 1: Staff Nhập Mã RETURN Mở Tủ

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | Staff (nhân viên thực địa, không có tài khoản) |
| **Endpoint** | `POST /api/iot/unlock-with-code` |
| **Authorization** | Không yêu cầu (public API cho tablet IoT) |
| **Service** | [StaffAccessCodeService.unlockWithCode()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/service/StaffAccessCodeService.java#L84-L144) |

### Request Body — [StaffCodeUnlockRequest](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/dto/request/StaffCodeUnlockRequest.java#11-26)

```json
{
  "orderId": 42,
  "accessCode": "RT9N4W",
  "staffName": "Trần Văn Minh"
}
```

| Field | Type | Required | Mô tả |
|-------|------|----------|-------|
| `orderId` | `Long` | ✅ | ID đơn hàng |
| `accessCode` | `String` | ✅ | Mã RETURN do Partner tạo |
| `staffName` | `String` | ❌ | Tên nhân viên (tracking) |

### Response — `ApiResponse<StaffCodeUnlockResponse>`

```json
{
  "code": "BOX_UNLOCKED",
  "data": {
    "success": true,
    "message": "Box unlocked successfully",
    "orderId": 42,
    "orderStatus": "RETURNED",
    "action": "RETURN",
    "boxes": [
      { "boxId": 12, "boxNumber": "12", "size": "MEDIUM" }
    ],
    "lockerCode": "ESP8266_LOCKER_01",
    "lockerName": "Tủ KTX A",
    "lockerAddress": "Tầng 1, KTX Khu A, ĐHQG",
    "unlockToken": "a9b8c7d6-e5f4-3210-fedc-ba0987654321",
    "unlockTimestamp": 1740733500000
  }
}
```

### Logic xử lý chi tiết

1. **Tìm AccessCode** theo `accessCode` → validate `ACTIVE`, chưa hết hạn
2. **Validate** code khớp `orderId`
3. **Validate** `OrderStatus == READY` (required cho RETURN)
4. **Lấy boxes** → `receiveBox` + `receiveBoxes` (ngăn trả đồ)
5. **Đánh dấu code:** `AccessCodeStatus: ACTIVE → USED`, lưu `staffName`, `usedAt`
6. **Cập nhật order:**
   - `OrderStatus: READY → RETURNED`
   - Sinh **PIN mới 6 chữ số** cho khách lấy đồ
   - Set `pinCodeIssuedAt = now`
7. **Đánh dấu receive boxes:** `BoxStatus: AVAILABLE → OCCUPIED`
8. **🔌 Gửi MQTT OPEN:**

```
Topic:   locker/commands/ESP8266_LOCKER_01
Payload: {"box_id": 12, "action": "OPEN"}
QoS:     1
```

9. **Gửi notification** cho khách:
   *"Đơn hàng #42 đã được trả lại tủ. Mã PIN: 593817"*

### State Transitions

```
AccessCodeStatus: ACTIVE → USED
OrderStatus:      READY → RETURNED
BoxStatus:        AVAILABLE → OCCUPIED  (receiveBox)
PIN Code:         sinh mã mới 6 chữ số

🔌 MQTT: Backend gửi "OPEN" → ESP8266 mở tủ cho Staff bỏ đồ
📱 Notification: gửi cho USER "Đồ đã trả vào tủ, PIN: XXXXXX"
```

> [!IMPORTANT]
> Sau bước này, khách có **24 giờ** để đến lấy đồ. Nếu quá hạn, scheduler [sendPickupReminders()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/core/scheduler/OrderSchedulerService.java#199-238) gửi notification nhắc nhở mỗi giờ, và khi lấy đồ sẽ bị tính **phí quá hạn**.

---

## Sơ đồ so sánh COLLECT vs RETURN

| Tiêu chí | COLLECT (UC2) | RETURN (UC4) |
|----------|---------------|--------------|
| **AccessCodeAction** | `COLLECT` | `RETURN` |
| **OrderStatus cần** | `WAITING` | `READY` |
| **OrderStatus sau** | `COLLECTED` | `RETURNED` |
| **Box liên quan** | `sendBox` (ngăn gửi) | `receiveBox` (ngăn nhận) |
| **BoxStatus sau** | `OCCUPIED → AVAILABLE` | `AVAILABLE → OCCUPIED` |
| **Sinh PIN mới?** | ❌ | ✅ |
| **Notification** | "Đang xử lý giặt" | "Đồ sẵn sàng + PIN" |
