# UC6: Thanh Toán Online (VNPay / MoMo)

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Tổng quan

Đối với dịch vụ `LAUNDRY` (tính theo kg), khách thanh toán **sau khi** đồ được trả vào tủ (`RETURNED`). Hệ thống hỗ trợ 2 cổng thanh toán: **VNPay** và **MoMo**. Flow: tạo payment → redirect tới gateway → khách thanh toán → callback tự động → cập nhật trạng thái.

**Actor chính:** USER (khách hàng)
**Enum sử dụng:** `PaymentStatus`, `PaymentMethod`, [OrderStatus](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/order/service/OrderService.java#317-329)

---

## Bước 1: Tạo Payment Request

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | USER |
| **Endpoint** | `POST /api/payments/create` |
| **Authorization** | `Bearer {JWT token}` — `@PreAuthorize("isAuthenticated()")` |
| **Service** | [PaymentService.createPayment()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/payment/service/PaymentService.java#L50-L107) |

### Request Body — [CreatePaymentRequest](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/payment/dto/request/CreatePaymentRequest.java#11-29)

```json
{
  "orderId": 42,
  "paymentMethod": "VNPAY",
  "bankCode": "NCB",
  "language": "vn"
}
```

| Field | Type | Required | Mô tả |
|-------|------|----------|-------|
| `orderId` | `Long` | ✅ | ID đơn hàng cần thanh toán |
| `paymentMethod` | `PaymentMethod` | ✅ | `VNPAY` / `MOMO` / `ZALOPAY` / `CASH` / `WALLET` / `BANK_TRANSFER` |
| `bankCode` | `String` | ❌ | Mã ngân hàng VNPay (nếu trống → chọn trên trang VNPay) |
| `language` | `String` | ❌ | `"vn"` (mặc định) hoặc `"en"` |

### Response — `ApiResponse<PaymentUrlResponse>`

**VNPay:**
```json
{
  "code": "PAYMENT_CREATED",
  "data": {
    "paymentId": 78,
    "orderId": 42,
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=6750000&vnp_TxnRef=78_42_1740646800&...",
    "expireAt": "2026-02-27T16:15:00",
    "qrCodeUrl": null,
    "deeplink": null
  }
}
```

**MoMo:**
```json
{
  "code": "PAYMENT_CREATED",
  "data": {
    "paymentId": 79,
    "orderId": 42,
    "paymentUrl": "https://test-payment.momo.vn/v2/gateway/pay?...",
    "expireAt": "2026-02-27T16:15:00",
    "qrCodeUrl": "https://test-payment.momo.vn/v2/qr/...",
    "deeplink": "momo://app?action=payWithApp&..."
  }
}
```

### Logic xử lý

1. Tìm Order → validate `OrderStatus ∈ {RETURNED, READY}`
2. Kiểm tra chưa có payment `COMPLETED` cho order (tránh trùng)
3. Tạo record [Payment](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/payment/service/PaymentService.java#44-94) với `PaymentStatus = PENDING`
4. Theo `paymentMethod`:
   - **VNPay:** Gọi `VNPayService.createPaymentUrl()` → sinh URL redirect
   - **MoMo:** Gọi `MoMoService.createPayment()` → sinh URL + QR + deeplink
5. Trả `paymentUrl` cho client redirect

### State Transitions

```
PaymentStatus: [null] → PENDING
OrderStatus:   giữ nguyên (RETURNED)
```

---

## Bước 2: Khách Thanh Toán Trên Gateway

> Bước này xảy ra **ngoài hệ thống** — khách được redirect đến trang VNPay/MoMo, nhập thông tin thẻ/ví, xác nhận thanh toán.

---

## Bước 3: Xử Lý Callback (Server-to-Server)

### 3a: VNPay IPN Callback

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai gọi** | Server VNPay (callback tự động) |
| **Endpoint** | `GET /api/payments/vnpay/ipn` |
| **Authorization** | Không yêu cầu (public, validate bằng checksum) |
| **Service** | [PaymentService.processVNPayIpn()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/payment/service/PaymentService.java#L110-L160) |

#### Request (Query Parameters từ VNPay)

```
GET /api/payments/vnpay/ipn?
  vnp_ResponseCode=00&
  vnp_TxnRef=78_42_1740646800&
  vnp_Amount=6750000&
  vnp_BankCode=NCB&
  vnp_TransactionNo=14150063&
  vnp_SecureHash=abc123...
```

#### Response

```json
{
  "RspCode": "00",
  "Message": "Confirm Success"
}
```

#### Logic xử lý

1. Verify `vnp_SecureHash` bằng secret key
2. Parse `vnp_TxnRef` → lấy `paymentId`
3. Tìm Payment record
4. Nếu `vnp_ResponseCode == "00"` (thành công):
   - `PaymentStatus: PENDING → COMPLETED`
   - `OrderStatus: RETURNED → COMPLETED` (gọi [completeOrderPayment](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/payment/service/PaymentService.java#281-286))
5. Nếu lỗi:
   - `PaymentStatus: PENDING → FAILED`

---

### 3b: MoMo Callback

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai gọi** | Server MoMo (callback tự động) |
| **Endpoint** | `POST /api/payments/momo/callback` |
| **Authorization** | Không yêu cầu (public, validate bằng signature) |
| **Service** | [PaymentService.processMoMoCallback()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/payment/service/PaymentService.java#L162-L211) |

#### Request Body (từ MoMo)

```json
{
  "partnerCode": "MOMO_PARTNER",
  "orderId": "78_42",
  "requestId": "req_123456",
  "amount": 67500,
  "resultCode": 0,
  "message": "Thành công",
  "transId": 2650112985,
  "signature": "abc123..."
}
```

#### Response

```json
{
  "status": 0,
  "message": "success"
}
```

#### Logic xử lý

1. Verify `signature` bằng MoMo secret
2. Parse `orderId` → lấy `paymentId`
3. Nếu `resultCode == 0` (thành công):
   - `PaymentStatus: PENDING → COMPLETED`
   - `OrderStatus: RETURNED → COMPLETED`
4. Nếu lỗi:
   - `PaymentStatus: PENDING → FAILED`

### State Transitions (callback thành công)

```
PaymentStatus: PENDING → COMPLETED
OrderStatus:   RETURNED → COMPLETED
⮕ Giải phóng receiveBox: BoxStatus OCCUPIED → AVAILABLE
⮕ Xóa PIN code
📱 Notification: PAYMENT "Thanh toán thành công" gửi cho USER
```

---

## Bước 4: VNPay Return URL (Redirect về app)

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai gọi** | Client (redirect từ VNPay) |
| **Endpoint** | `GET /api/payments/vnpay/return` |
| **Authorization** | Không yêu cầu |
| **Service** | [PaymentService.processVNPayReturn()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/payment/service/PaymentService.java#L213-L260) |

#### Request (Query Parameters)

```
GET /api/payments/vnpay/return?
  vnp_ResponseCode=00&
  vnp_TxnRef=78_42_1740646800&
  vnp_Amount=6750000&
  vnp_SecureHash=abc123...
```

#### Response — `ApiResponse<PaymentResponse>`

```json
{
  "data": {
    "id": 78,
    "orderId": 42,
    "amount": 67500,
    "paymentMethod": "VNPAY",
    "status": "COMPLETED",
    "transactionId": "14150063",
    "paidAt": "2026-02-27T15:30:00"
  }
}
```

> [!NOTE]
> **Return URL** chỉ để hiển thị kết quả cho khách. Trạng thái đã được IPN callback cập nhật trước đó. Đây là bước "xác nhận lại" cho client.

---

## Tổng kết Flow VNPay

```
  USER                    Backend               VNPay Server
   │                        │                        │
   │── POST /payments/create ──→│                    │
   │                        │── Tạo Payment PENDING  │
   │←── paymentUrl ─────────│                        │
   │                        │                        │
   │── Redirect → paymentUrl ───────────────────────→│
   │                        │                        │
   │                        │←── GET /vnpay/ipn ─────│  (server-to-server)
   │                        │── Payment COMPLETED ──→│
   │                        │── Order COMPLETED      │
   │                        │                        │
   │←── Redirect /vnpay/return ──────────────────────│  (user redirect)
   │                        │── Hiển thị kết quả     │
```
