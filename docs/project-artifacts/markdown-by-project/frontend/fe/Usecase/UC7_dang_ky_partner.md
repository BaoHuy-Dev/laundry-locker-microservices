# UC7: Đăng Ký & Duyệt Partner

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Tổng quan

User đăng ký trở thành Partner (chủ cửa hàng giặt). Admin xem danh sách chờ duyệt, duyệt/từ chối/đình chỉ. Sau khi duyệt, Partner được gán role `PARTNER` và có thể quản lý đơn hàng, nhân viên, cửa hàng.

**Actor chính:** USER → PARTNER, ADMIN
**Enum sử dụng:** `PartnerStatus`, `RoleName`

---

## Bước 1: User Đăng Ký Partner

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | USER (đã đăng nhập) |
| **Endpoint** | `POST /api/partner` |
| **Authorization** | `Bearer {JWT token}` — `@PreAuthorize("isAuthenticated()")` |
| **Service** | [PartnerService.registerPartner()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/service/PartnerService.java#L75-L93) |

### Request Body — [PartnerRegistrationRequest](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/dto/request/PartnerRegistrationRequest.java#12-37)

```json
{
  "businessName": "Giặt Ủi Sạch 24H",
  "businessRegistrationNumber": "0312345678",
  "taxId": "0312345678-001",
  "businessAddress": "123 Nguyễn Văn Cừ, Q.5, TP.HCM",
  "contactPhone": "0901234567",
  "contactEmail": "giatui24h@gmail.com",
  "notes": "Kinh nghiệm 5 năm, 3 chi nhánh"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|-----------|
| `businessName` | `String` | ✅ | 3–200 ký tự |
| `businessRegistrationNumber` | `String` | ❌ | Số đăng ký kinh doanh |
| `taxId` | `String` | ❌ | Mã số thuế |
| `businessAddress` | `String` | ✅ | Địa chỉ |
| `contactPhone` | `String` | ✅ | SĐT liên hệ |
| `contactEmail` | `String` | ❌ | Email (validate format) |
| `notes` | `String` | ❌ | Ghi chú |

### Response — `ApiResponse<PartnerResponse>`

```json
{
  "code": "SUCCESS",
  "data": {
    "id": 5,
    "userId": 10,
    "userName": "Nguyễn Quốc Bảo Huy",
    "businessName": "Giặt Ủi Sạch 24H",
    "businessRegistrationNumber": "0312345678",
    "taxId": "0312345678-001",
    "businessAddress": "123 Nguyễn Văn Cừ, Q.5, TP.HCM",
    "contactPhone": "0901234567",
    "contactEmail": "giatui24h@gmail.com",
    "status": "PENDING",
    "approvedAt": null,
    "approvedBy": null,
    "rejectionReason": null,
    "revenueSharePercent": null,
    "storeCount": 0,
    "staffCount": 0,
    "notes": "Kinh nghiệm 5 năm, 3 chi nhánh",
    "createdAt": "2026-02-27T15:00:00"
  }
}
```

### Logic xử lý

1. Kiểm tra user chưa đăng ký Partner chưa → nếu rồi throw `alreadyExists()`
2. Tạo Partner entity với `PartnerStatus = PENDING`
3. Nếu không cung cấp `contactEmail` → dùng email của user

### State Transitions

```
PartnerStatus: [null] → PENDING
Role:          giữ nguyên USER (chưa gán PARTNER)
```

---

## Bước 2: Admin Xem Danh Sách Chờ Duyệt

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | ADMIN |
| **Endpoint** | `GET /api/admin/partners?status=PENDING` |
| **Authorization** | `Bearer {JWT token}` — `@PreAuthorize("hasRole('ADMIN')")` |
| **Service** | [PartnerService.getAllPartners()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/service/PartnerService.java#L170-L177) |

### Request

```
GET /api/admin/partners?status=PENDING&page=0&size=20
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Response — `ApiResponse<Page<PartnerResponse>>`

```json
{
  "data": {
    "content": [
      {
        "id": 5,
        "businessName": "Giặt Ủi Sạch 24H",
        "status": "PENDING",
        "contactPhone": "0901234567",
        "createdAt": "2026-02-27T15:00:00"
      }
    ],
    "totalElements": 2,
    "totalPages": 1
  }
}
```

---

## Bước 3a: Admin Duyệt Partner

| Thông tin | Chi tiết |
|-----------|----------|
| **Ai thực hiện** | ADMIN |
| **Endpoint** | `PUT /api/admin/partners/{partnerId}/approve` |
| **Authorization** | `Bearer {JWT token}` — `@PreAuthorize("hasRole('ADMIN')")` |
| **Service** | [PartnerService.approvePartner()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/service/PartnerService.java#L179-L192) |

### Request

```
PUT /api/admin/partners/5/approve
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Không có Request Body.**

### Response — `ApiResponse<PartnerResponse>`

```json
{
  "data": {
    "id": 5,
    "status": "APPROVED",
    "approvedAt": "2026-02-27T16:00:00",
    "approvedBy": 1,
    "...": "..."
  }
}
```

### Logic xử lý

1. Validate `PartnerStatus == PENDING`
2. **Gán role PARTNER** cho user (thêm vào `user.roles`)
3. Set `PartnerStatus = APPROVED`, `approvedAt = now`, `approvedBy = adminUserId`

### State Transitions

```
PartnerStatus: PENDING → APPROVED
Role:          USER → USER + PARTNER  (thêm role)
```

---

## Bước 3b: Admin Từ Chối Partner

| Thông tin | Chi tiết |
|-----------|----------|
| **Endpoint** | `PUT /api/admin/partners/{partnerId}/reject` |
| **Service** | [PartnerService.rejectPartner()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/service/PartnerService.java#L194-L205) |

### Request

```
PUT /api/admin/partners/5/reject?reason=Thiếu giấy phép kinh doanh
```

### Response

```json
{
  "data": {
    "id": 5,
    "status": "REJECTED",
    "rejectionReason": "Thiếu giấy phép kinh doanh"
  }
}
```

### State Transitions

```
PartnerStatus: PENDING → REJECTED
```

---

## Bước 3c: Admin Đình Chỉ Partner

| Thông tin | Chi tiết |
|-----------|----------|
| **Endpoint** | `PUT /api/admin/partners/{partnerId}/suspend` (custom admin endpoint) |
| **Service** | [PartnerService.suspendPartner()](file:///d:/BigProject/laundry-locker-backend/laundry-locker-backend/src/main/java/com/huynqb/laundrylockerbackend/module/partner/service/PartnerService.java#L207-L215) |

### State Transitions

```
PartnerStatus: APPROVED → SUSPENDED
⮕ Partner bị chặn truy cập tất cả API yêu cầu APPROVED
⮕ Kiểm tra: getApprovedPartner() throw PartnerException.suspended()
```

---

## Tổng kết: Vòng đời Partner

```mermaid
stateDiagram-v2
    [*] --> PENDING: User đăng ký
    PENDING --> APPROVED: Admin duyệt
    PENDING --> REJECTED: Admin từ chối
    APPROVED --> SUSPENDED: Admin đình chỉ
```

| Trạng thái | Quyền truy cập |
|-----------|---------------|
| `PENDING` | Chỉ xem thông tin partner, không quản lý đơn hàng |
| `APPROVED` | Full access: quản lý đơn, staff, cửa hàng, tạo AccessCode |
| `REJECTED` | Không thể hoạt động |
| `SUSPENDED` | Bị chặn toàn bộ API partner |

---

## Luồng phụ: Partner Quản Lý Staff

### Thêm Staff

```
POST /api/partner/staff
Body: { "staffUserId": 15 }
```

**Logic:** Partner thêm user vào danh sách `staff` của mình. Staff **không** được gán role riêng — chỉ là user thường thuộc nhóm `partner.staff`.

### Xóa Staff

```
DELETE /api/partner/staff/{staffId}
```

> [!NOTE]
> Staff trong hệ thống là **user accounts** được Partner quản lý nội bộ. Khi tương tác với tủ locker ngoài thực địa, nhân viên (có thể khác với staff accounts) sử dụng **AccessCode** do Partner tạo — không cần đăng nhập.
