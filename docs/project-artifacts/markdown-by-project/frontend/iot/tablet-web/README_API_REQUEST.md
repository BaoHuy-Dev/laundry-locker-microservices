# Yêu cầu bổ sung API cho Backend (Laundry Locker)

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Bối cảnh (Context)
Hiện tại, trên ứng dụng Kiosk (Tablet Web) đặt cố định tại một tủ khoá (Locker) cụ thể. Người dùng trên UI sẽ thực hiện luồng:
1. Màn hình chọn ô tủ trống của Locker hiện tại.
2. Màn hình chọn Dịch vụ (gửi giặt).
3. Màn hình Tạo đơn hàng.

Hiệu tại ở màn hình chọn Dịch vụ, Kiosk đang gọi API `GET /api/services?category=STORAGE`. Tuy nhiên, vì Kiosk đặt cố định tại 1 tòa nhà/địa điểm (Store), danh sách dịch vụ hiển thị nên được filter theo cửa hàng (Store) mà Locker đó thuộc về. 

Mặc dù backend đã có API `GET /api/services?storeId={storeId}`, tuy nhiên Kiosk chỉ biết được `Locker ID` (hoặc Locker Code) thông qua cấu hình biến môi trường (`.env`). Việc Kiosk phải gọi thêm API lấy chi tiết Locker để lấy `storeId` sau đó mới gọi tiếp API lấy Service sẽ làm tăng số lượng request không cần thiết.

## Yêu cầu API (API Request)
Vui lòng bổ sung thêm API để lấy trực tiếp danh sách các dịch vụ (Services) thông qua Locker ID.

### Thông tin API đề xuất
- **HTTP Method:** `GET`
- **Endpoint 1 (Mới):** `/api/lockers/{lockerId}/services`
- **Hoặc Endpoint 2 (Update ServiceController):** `GET /api/services?lockerId={lockerId}`
- **Query Params (Tùy chọn):** `category` (vd: `STORAGE` hoặc `LAUNDRY`)

### Logic xử lý dự kiến tại Backend
1. Từ `lockerId`, tìm ra `Locker` và `Store` tương ứng (`locker.getStore()`).
2. Lấy ra danh sách các `LaundryService` thuộc về hệ thống và áp dụng bảng giá (Pricing) riêng biệt của `Store` đó (nếu có).
3. Trả về danh sách `ServiceResponse` tương tự như API `GET /api/services?storeId={storeId}`.

### Tiêu chí nghiệm thu (Acceptance Criteria)
- [x] Kiosk có thể gọi trực tiếp API này bằng `lockerId` để lấy danh sách dịch vụ gửi đồ của địa điểm đó.
- [x] Response format giống với `ServiceResponse` hiện tại để frontend không cần sửa lại data model.

## ✅ Implementation Completed

### API Endpoints đã triển khai:

#### 1. Get Services By Locker
- **Method:** `GET`
- **URL:** `/api/services?lockerId={lockerId}`
- **Description:** Lấy tất cả dịch vụ tại cửa hàng có locker này

#### 2. Get Services By Locker and Category  
- **Method:** `GET`
- **URL:** `/api/services?lockerId={lockerId}&category={category}`
- **Query Params:**
  - `lockerId`: ID của locker (required)
  - `category`: `STORAGE` hoặc `LAUNDRY` (optional)
- **Description:** Lấy dịch vụ theo category tại cửa hàng có locker này

### Example Usage for Kiosk:
```bash
# Lấy tất cả dịch vụ gửi đồ (STORAGE) theo lockerId
GET /api/services?lockerId=1&category=STORAGE

# Lấy tất cả dịch vụ tại store của locker
GET /api/services?lockerId=1
```

### Files Modified:
- `ServiceController.java` - Added 2 new endpoints
- `LaundryServiceService.java` - Added 2 new methods with locker lookup logic
