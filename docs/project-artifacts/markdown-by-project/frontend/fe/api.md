# Admin API Documentation

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Base URL
```
/api/admin
```

## Authentication
Tất cả API Admin đều yêu cầu xác thực với role `ADMIN`.
```
Authorization: Bearer <jwt_token>
```

---

# 1. Admin User Management APIs

**Base URL:** `/api/admin/users`

## 1.1 Get All Users
Lấy danh sách tất cả users với phân trang.

**Request:**
```http
GET /api/admin/users?page=0&size=20&sort=id,desc
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| page | Integer | No | Số trang (default: 0) |
| size | Integer | No | Số lượng items mỗi trang (default: 20) |
| sort | String | No | Sắp xếp (vd: id,desc) |

**Response (200 OK):**
```json
{
  "success": true,
  "message": "USERS_RETRIEVED",
  "data": {
    "content": [
      {
        "id": 1,
        "email": "user@example.com",
        "name": "John Doe",
        "imageUrl": "https://example.com/avatar.jpg",
        "provider": "LOCAL",
        "emailVerified": true,
        "enabled": true,
        "roles": ["USER", "STAFF"],
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-20T14:45:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 100,
    "totalPages": 5
  }
}
```

## 1.2 Create User
Tạo user mới (ví dụ: Staff account).

**Request:**
```http
POST /api/admin/users
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "staff@example.com",
  "password": "password123",
  "firstName": "Jane",
  "lastName": "Smith",
  "phoneNumber": "+84123456789",
  "roles": ["USER", "STAFF"],
  "enabled": true
}
```

**Validation:**
| Field | Constraints |
|-------|-------------|
| email | Required, valid email format |
| password | 6-100 characters |
| firstName | Required, max 100 characters |
| lastName | Max 100 characters |
| phoneNumber | Max 20 characters |

**Response (201 Created):**
```json
{
  "success": true,
  "message": "USER_CREATED",
  "data": {
    "id": 2,
    "email": "staff@example.com",
    "name": "Jane Smith",
    "imageUrl": null,
    "provider": "LOCAL",
    "emailVerified": false,
    "enabled": true,
    "roles": ["USER", "STAFF"],
    "createdAt": "2024-01-30T10:00:00",
    "updatedAt": "2024-01-30T10:00:00"
  }
}
```

## 1.3 Get User By ID
Lấy thông tin user theo ID.

**Request:**
```http
GET /api/admin/users/{id}
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | Long | User ID |

**Response (200 OK):**
```json
{
  "success": true,
  "message": "USER_RETRIEVED",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe",
    "imageUrl": "https://example.com/avatar.jpg",
    "provider": "GOOGLE",
    "emailVerified": true,
    "enabled": true,
    "roles": ["USER"],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-20T14:45:00"
  }
}
```

## 1.4 Update User
Cập nhật thông tin user.

**Request:**
```http
PUT /api/admin/users/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Updated Name",
  "email": "newemail@example.com",
  "imageUrl": "https://example.com/new-avatar.jpg"
}
```

**Validation:**
| Field | Constraints |
|-------|-------------|
| name | 2-100 characters |
| email | Valid email format |

**Response (200 OK):**
```json
{
  "success": true,
  "message": "USER_UPDATED",
  "data": {
    "id": 1,
    "email": "newemail@example.com",
    "name": "Updated Name",
    "imageUrl": "https://example.com/new-avatar.jpg",
    "provider": "LOCAL",
    "emailVerified": true,
    "enabled": true,
    "roles": ["USER"],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-30T10:30:00"
  }
}
```

## 1.5 Update User Status
Kích hoạt/Vô hiệu hóa user.

**Request:**
```http
PUT /api/admin/users/{id}/status
Content-Type: application/json
```

**Request Body:**
```json
{
  "enabled": false
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "USER_STATUS_UPDATED",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe",
    "enabled": false,
    "roles": ["USER"],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-30T10:35:00"
  }
}
```

## 1.6 Update User Roles
Cập nhật roles của user.

**Request:**
```http
PUT /api/admin/users/{id}/roles
Content-Type: application/json
```

**Request Body:**
```json
{
  "roles": ["USER", "STAFF", "ADMIN"]
}
```

**Available Roles:** `USER`, `STAFF`, `ADMIN`, `MODERATOR`, `PARTNER`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "USER_ROLES_UPDATED",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe",
    "enabled": true,
    "roles": ["USER", "STAFF", "ADMIN"],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-30T10:40:00"
  }
}
```

## 1.7 Delete User
Xóa user.

**Request:**
```http
DELETE /api/admin/users/{id}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "USER_DELETED",
  "data": null
}
```

---

# 2. Admin Store Management APIs

**Base URL:** `/api/admin/stores`

## 2.1 Get All Stores
Lấy danh sách tất cả stores với phân trang.

**Request:**
```http
GET /api/admin/stores?page=0&size=20
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "STORES_RETRIEVED",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Laundry Store 1",
        "address": "123 Main St, District 1",
        "phone": "+84123456789",
        "imageUrl": "https://example.com/store.jpg",
        "latitude": 10.762622,
        "longitude": 106.660172,
        "description": "Best laundry service in town",
        "openTime": "08:00",
        "closeTime": "20:00",
        "active": true,
        "lockerCount": 3,
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-20T14:45:00"
      }
    ],
    "totalElements": 10,
    "totalPages": 1
  }
}
```

## 2.2 Get Store By ID
Lấy thông tin store theo ID.

**Request:**
```http
GET /api/admin/stores/{id}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "STORE_RETRIEVED",
  "data": {
    "id": 1,
    "name": "Laundry Store 1",
    "address": "123 Main St, District 1",
    "phone": "+84123456789",
    "imageUrl": "https://example.com/store.jpg",
    "latitude": 10.762622,
    "longitude": 106.660172,
    "description": "Best laundry service in town",
    "openTime": "08:00",
    "closeTime": "20:00",
    "active": true,
    "lockerCount": 3,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-20T14:45:00"
  }
}
```

## 2.3 Create Store
Tạo store mới.

**Request:**
```http
POST /api/admin/stores
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "New Store",
  "address": "456 New St, District 2",
  "phone": "+84987654321",
  "imageUrl": "https://example.com/new-store.jpg",
  "latitude": 10.771234,
  "longitude": 106.691234,
  "description": "New laundry store",
  "openTime": "07:00",
  "closeTime": "22:00"
}
```

**Validation:**
| Field | Constraints |
|-------|-------------|
| name | Required |

**Response (201 Created):**
```json
{
  "success": true,
  "message": "STORE_CREATED",
  "data": {
    "id": 2,
    "name": "New Store",
    "address": "456 New St, District 2",
    "phone": "+84987654321",
    "imageUrl": "https://example.com/new-store.jpg",
    "latitude": 10.771234,
    "longitude": 106.691234,
    "description": "New laundry store",
    "openTime": "07:00",
    "closeTime": "22:00",
    "active": true,
    "lockerCount": 0,
    "createdAt": "2024-01-30T10:00:00",
    "updatedAt": "2024-01-30T10:00:00"
  }
}
```

## 2.4 Update Store
Cập nhật thông tin store.

**Request:**
```http
PUT /api/admin/stores/{id}
Content-Type: application/json
```

**Request Body:** (giống Create Store)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "STORE_UPDATED",
  "data": { ... }
}
```

## 2.5 Update Store Status
Kích hoạt/Vô hiệu hóa store.

**Request:**
```http
PUT /api/admin/stores/{id}/status
Content-Type: application/json
```

**Request Body:**
```json
{
  "enabled": false
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "STORE_STATUS_UPDATED",
  "data": { ... }
}
```

## 2.6 Delete Store
Xóa store (soft delete).

**Request:**
```http
DELETE /api/admin/stores/{id}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "STORE_DELETED",
  "data": null
}
```

---

# 3. Admin Locker & Box Management APIs

**Base URL:** `/api/admin/lockers`

## 3.1 Get All Lockers
Lấy danh sách tất cả lockers với phân trang.

**Request:**
```http
GET /api/admin/lockers?page=0&size=20
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "LOCKERS_RETRIEVED",
  "data": {
    "content": [
      {
        "id": 1,
        "code": "LOCKER-001",
        "name": "Main Locker",
        "address": "123 Main St",
        "storeId": 1,
        "storeName": "Laundry Store 1",
        "status": "ACTIVE",
        "totalBoxes": 20,
        "availableBoxes": 15,
        "boxes": [
          {
            "id": 1,
            "boxNumber": 1,
            "status": "AVAILABLE",
            "description": "Small box"
          }
        ],
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-20T14:45:00"
      }
    ],
    "totalElements": 5,
    "totalPages": 1
  }
}
```

## 3.2 Get Lockers By Store
Lấy danh sách lockers theo store.

**Request:**
```http
GET /api/admin/lockers/store/{storeId}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "LOCKERS_RETRIEVED",
  "data": [
    {
      "id": 1,
      "code": "LOCKER-001",
      "name": "Main Locker",
      "address": "123 Main St",
      "storeId": 1,
      "storeName": "Laundry Store 1",
      "status": "ACTIVE",
      "totalBoxes": 20,
      "availableBoxes": 15,
      "boxes": [...],
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-20T14:45:00"
    }
  ]
}
```

## 3.3 Get Locker By ID
Lấy thông tin locker theo ID.

**Request:**
```http
GET /api/admin/lockers/{id}
```

**Response (200 OK):** (giống Get All Lockers)

## 3.4 Create Locker
Tạo locker mới.

**Request:**
```http
POST /api/admin/lockers
Content-Type: application/json
```

**Request Body:**
```json
{
  "code": "LOCKER-002",
  "name": "Second Locker",
  "address": "456 Second St",
  "storeId": 1
}
```

**Validation:**
| Field | Constraints |
|-------|-------------|
| code | Required |
| name | Required |
| storeId | Required |

**Response (201 Created):**
```json
{
  "success": true,
  "message": "LOCKER_CREATED",
  "data": { ... }
}
```

## 3.5 Update Locker
Cập nhật thông tin locker.

**Request:**
```http
PUT /api/admin/lockers/{id}
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "LOCKER_UPDATED",
  "data": { ... }
}
```

## 3.6 Set Maintenance Mode
Bật/Tắt chế độ bảo trì cho locker.

**Request:**
```http
PUT /api/admin/lockers/{id}/maintenance
Content-Type: application/json
```

**Request Body:**
```json
{
  "maintenance": true
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "LOCKER_MAINTENANCE_UPDATED",
  "data": { ... }
}
```

## 3.7 Add Box to Locker
Thêm box mới vào locker.

**Request:**
```http
POST /api/admin/lockers/{id}/boxes
Content-Type: application/json
```

**Request Body:**
```json
{
  "boxNumber": 21,
  "description": "Extra large box"
}
```

**Validation:**
| Field | Constraints |
|-------|-------------|
| boxNumber | Required |

**Response (201 Created):**
```json
{
  "success": true,
  "message": "BOX_ADDED",
  "data": { ... }
}
```

## 3.8 Update Box Status
Cập nhật trạng thái box.

**Request:**
```http
PUT /api/admin/lockers/boxes/{boxId}/status
Content-Type: application/json
```

**Request Body:**
```json
{
  "status": "MAINTENANCE"
}
```

**Box Status Values:** `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "BOX_STATUS_UPDATED",
  "data": null
}
```

## 3.9 Delete Locker
Xóa locker (soft delete).

**Request:**
```http
DELETE /api/admin/lockers/{id}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "LOCKER_DELETED",
  "data": null
}
```

---

# 4. Admin Service Management APIs

**Base URL:** `/api/admin/services`

## 4.1 Get All Services
Lấy danh sách tất cả services với phân trang.

**Request:**
```http
GET /api/admin/services?page=0&size=20
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "SERVICES_RETRIEVED",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Wash & Fold",
        "description": "Standard wash and fold service",
        "price": 50000.00,
        "unit": "kg",
        "imageUrl": "https://example.com/service.jpg",
        "estimatedMinutes": 1440,
        "storeId": null,
        "storeName": null,
        "active": true,
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-20T14:45:00"
      }
    ],
    "totalElements": 10,
    "totalPages": 1
  }
}
```

## 4.2 Get Service By ID
Lấy thông tin service theo ID.

**Request:**
```http
GET /api/admin/services/{id}
```

**Response (200 OK):** (giống Get All Services)

## 4.3 Create Service
Tạo service mới.

**Request:**
```http
POST /api/admin/services
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Dry Cleaning",
  "description": "Professional dry cleaning",
  "price": 100000.00,
  "unit": "item",
  "imageUrl": "https://example.com/dry-clean.jpg",
  "estimatedMinutes": 2880,
  "storeId": 1
}
```

**Validation:**
| Field | Constraints |
|-------|-------------|
| name | Required |
| price | Required |

**Response (201 Created):**
```json
{
  "success": true,
  "message": "SERVICE_CREATED",
  "data": { ... }
}
```

## 4.4 Update Service
Cập nhật thông tin service.

**Request:**
```http
PUT /api/admin/services/{id}
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "SERVICE_UPDATED",
  "data": { ... }
}
```

## 4.5 Update Price
Cập nhật giá service.

**Request:**
```http
PUT /api/admin/services/{id}/price
Content-Type: application/json
```

**Request Body:**
```json
{
  "price": 75000.00
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "SERVICE_PRICE_UPDATED",
  "data": { ... }
}
```

## 4.6 Update Status
Kích hoạt/Vô hiệu hóa service.

**Request:**
```http
PUT /api/admin/services/{id}/status
Content-Type: application/json
```

**Request Body:**
```json
{
  "enabled": false
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "SERVICE_STATUS_UPDATED",
  "data": { ... }
}
```

## 4.7 Delete Service
Xóa service (soft delete).

**Request:**
```http
DELETE /api/admin/services/{id}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "SERVICE_DELETED",
  "data": null
}
```

---

# 5. Admin Order Management APIs

**Base URL:** `/api/admin/orders`

## 5.1 Get All Orders
Lấy danh sách tất cả orders với filter theo status.

**Request:**
```http
GET /api/admin/orders?status=WAITING&page=0&size=20
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | OrderStatus | No | Lọc theo status |

**Order Status Values:** `INITIALIZED`, `RESERVED`, `WAITING`, `COLLECTED`, `PROCESSING`, `READY`, `RETURNED`, `COMPLETED`, `CANCELED`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "ORDERS_RETRIEVED",
  "data": {
    "content": [
      {
        "id": 1,
        "type": "LAUNDRY",
        "status": "WAITING",
        "pinCode": "123456",
        "senderId": 1,
        "senderName": "John Doe",
        "senderPhone": "+84123456789",
        "receiverId": null,
        "receiverName": null,
        "lockerId": 1,
        "lockerName": "Main Locker",
        "lockerCode": "LOCKER-001",
        "sendBoxNumber": 1,
        "receiveBoxNumber": null,
        "sendBoxNumbers": [1],
        "receiveBoxNumbers": [],
        "staffId": null,
        "staffName": null,
        "actualWeight": null,
        "weightUnit": "kg",
        "extraFee": 0.00,
        "discount": 0.00,
        "reservationFee": 10000.00,
        "storagePrice": 0.00,
        "shippingFee": 0.00,
        "totalPrice": 50000.00,
        "description": null,
        "customerNote": null,
        "staffNote": null,
        "deliveryAddress": null,
        "intendedReceiveAt": null,
        "receiveAt": null,
        "completedAt": null,
        "createdAt": "2024-01-30T09:00:00",
        "updatedAt": "2024-01-30T09:00:00",
        "orderDetails": [
          {
            "id": 1,
            "serviceId": 1,
            "serviceName": "Wash & Fold",
            "serviceImage": "https://example.com/service.jpg",
            "quantity": 2.5,
            "unit": "kg",
            "price": 50000.00,
            "description": null
          }
        ]
      }
    ],
    "totalElements": 50,
    "totalPages": 3
  }
}
```

## 5.2 Get Order By ID
Lấy thông tin order theo ID.

**Request:**
```http
GET /api/admin/orders/{id}
```

**Response (200 OK):** (giống Get All Orders)

## 5.3 Update Order Status
Force update order status (Admin override).

**Request:**
```http
PUT /api/admin/orders/{id}/status?status=PROCESSING
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | OrderStatus | Yes | Status mới |

**Response (200 OK):**
```json
{
  "success": true,
  "message": "ORDER_STATUS_UPDATED",
  "data": { ... }
}
```

## 5.4 Get Order Statistics
Lấy thống kê orders.

**Request:**
```http
GET /api/admin/orders/statistics
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "STATISTICS_RETRIEVED",
  "data": {
    "totalOrders": 150,
    "completedOrders": 120,
    "canceledOrders": 10,
    "pendingOrders": 20,
    "totalRevenue": 7500000.00,
    "averageOrderValue": 50000.00,
    "ordersToday": 5,
    "ordersThisWeek": 25,
    "ordersThisMonth": 100
  }
}
```

## 5.5 Get Revenue Report
Lấy báo cáo doanh thu.

**Request:**
```http
GET /api/admin/orders/revenue
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "REVENUE_REPORT_RETRIEVED",
  "data": {
    "totalRevenue": 15000000.00,
    "revenueToday": 500000.00,
    "revenueThisWeek": 2500000.00,
    "revenueThisMonth": 10000000.00,
    "totalTransactions": 300,
    "successfulTransactions": 280,
    "failedTransactions": 20,
    "revenueByPaymentMethod": {
      "MOMO": 5000000.00,
      "VNPAY": 6000000.00,
      "CASH": 4000000.00
    },
    "dailyRevenue": [
      {
        "date": "2024-01-29",
        "revenue": 450000.00,
        "orderCount": 9
      },
      {
        "date": "2024-01-30",
        "revenue": 500000.00,
        "orderCount": 10
      }
    ]
  }
}
```

---

# 6. Admin Payment Management APIs

**Base URL:** `/api/admin/payments`

## 6.1 Get All Payments
Lấy danh sách tất cả payments với filter theo status.

**Request:**
```http
GET /api/admin/payments?status=COMPLETED&page=0&size=20
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | PaymentStatus | No | Lọc theo status |

**Payment Status Values:** `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `REFUNDED`, `CANCELED`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "PAYMENTS_RETRIEVED",
  "data": {
    "content": [
      {
        "id": 1,
        "orderId": 1,
        "customerId": 1,
        "customerName": "John Doe",
        "amount": 50000.00,
        "method": "MOMO",
        "status": "COMPLETED",
        "content": "Payment for order #1",
        "referenceId": "MOMO123456",
        "referenceTransactionId": "TXN789012",
        "qr": null,
        "url": null,
        "deeplink": null,
        "description": null,
        "createdAt": "2024-01-30T09:05:00"
      }
    ],
    "totalElements": 200,
    "totalPages": 10
  }
}
```

## 6.2 Get Payment By ID
Lấy thông tin payment theo ID.

**Request:**
```http
GET /api/admin/payments/{paymentId}
```

**Response (200 OK):** (giống Get All Payments)

## 6.3 Update Payment Status
Force update payment status (Admin override).

**Request:**
```http
PUT /api/admin/payments/{paymentId}/status?status=REFUNDED
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | PaymentStatus | Yes | Status mới |

**Response (200 OK):**
```json
{
  "success": true,
  "message": "PAYMENT_STATUS_UPDATED",
  "data": { ... }
}
```

---

# 7. Admin Dashboard APIs

**Base URL:** `/api/admin/dashboard`

## 7.1 Get Dashboard Overview
Lấy tổng quan dashboard với các key metrics.

**Request:**
```http
GET /api/admin/dashboard/overview
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "DASHBOARD_RETRIEVED",
  "data": {
    "totalUsers": 500,
    "totalStores": 10,
    "totalLockers": 25,
    "totalOrders": 1000,
    "ordersToday": 15,
    "pendingOrders": 20,
    "totalRevenue": 50000000.00,
    "revenueToday": 750000.00,
    "activeServices": 15,
    "availableBoxes": 300,
    "occupiedBoxes": 50
  }
}
```

---

# 8. Admin Scheduler Management APIs

**Base URL:** `/api/admin/scheduler`

## 8.1 Trigger Auto-Cancel Job
Kích hoạt thủ công job auto-cancel orders chưa confirmed.

**Request:**
```http
POST /api/admin/scheduler/auto-cancel
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "JOB_EXECUTED",
  "data": {
    "jobName": "auto-cancel-unconfirmed-orders",
    "status": "completed",
    "message": "Auto-cancel job executed successfully"
  }
}
```

## 8.2 Trigger Box Release Job
Kích hoạt thủ công job release boxes từ completed orders.

**Request:**
```http
POST /api/admin/scheduler/release-boxes
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "JOB_EXECUTED",
  "data": {
    "jobName": "release-boxes-after-completion",
    "status": "completed",
    "message": "Box release job executed successfully"
  }
}
```

## 8.3 Trigger Pickup Reminder Job
Kích hoạt thủ công job gửi pickup reminders.

**Request:**
```http
POST /api/admin/scheduler/pickup-reminders
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "JOB_EXECUTED",
  "data": {
    "jobName": "send-pickup-reminders",
    "status": "completed",
    "message": "Pickup reminder job executed successfully"
  }
}
```

## 8.4 Get Scheduler Status
Lấy trạng thái và cấu hình scheduler.

**Request:**
```http
GET /api/admin/scheduler/status
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "SCHEDULER_STATUS",
  "data": {
    "schedulerEnabled": true,
    "jobs": [
      "auto-cancel-unconfirmed-orders",
      "release-boxes-after-completion",
      "send-pickup-reminders"
    ],
    "message": "Scheduler is running"
  }
}
```

---

# 9. Admin Loyalty Management APIs

**Base URL:** `/api/admin/loyalty`

## 9.1 Get User Loyalty Summary
Lấy tổng quan loyalty của user.

**Request:**
```http
GET /api/admin/loyalty/users/{userId}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "LOYALTY_SUMMARY_RETRIEVED",
  "data": {
    "pointsAccount": {
      "id": 1,
      "userId": 1,
      "userName": "John Doe",
      "pointsBalance": 5000,
      "pointsValueVnd": 5000.00,
      "totalPointsEarned": 10000,
      "totalPointsRedeemed": 5000,
      "totalAmountSpent": 5000000.00,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-30T10:00:00"
    },
    "stampCards": [
      {
        "id": 1,
        "userId": 1,
        "stampType": "SERVICE",
        "serviceId": 1,
        "serviceName": "Wash & Fold",
        "boxSize": null,
        "stampsRequired": 10,
        "currentStamps": 7,
        "freeRewardsAvailable": 0,
        "totalStampsEarned": 17,
        "totalRewardsRedeemed": 1,
        "progressPercentage": 70,
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-30T10:00:00"
      }
    ],
    "totalRedeemableValue": 5000.00,
    "totalFreeRewards": 0
  }
}
```

## 9.2 Adjust User Points
Điều chỉnh điểm của user (dùng số âm để trừ điểm).

**Request:**
```http
POST /api/admin/loyalty/users/{userId}/points
Content-Type: application/json
```

**Request Body:**
```json
{
  "userId": 1,
  "points": 1000,
  "reason": "Bonus for promotion"
}
```

**Validation:**
| Field | Constraints |
|-------|-------------|
| userId | Required |
| points | Required |

**Response (200 OK):**
```json
{
  "success": true,
  "message": "POINTS_ADJUSTED",
  "data": {
    "id": 1,
    "userId": 1,
    "orderId": null,
    "type": "ADJUST",
    "points": 1000,
    "relatedAmount": null,
    "balanceAfter": 6000,
    "description": "Bonus for promotion",
    "referenceId": null,
    "createdAt": "2024-01-30T10:30:00"
  }
}
```

## 9.3 Get User Points History
Lấy lịch sử điểm của user.

**Request:**
```http
GET /api/admin/loyalty/users/{userId}/history?page=0&size=20
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "POINTS_HISTORY_RETRIEVED",
  "data": {
    "content": [
      {
        "id": 1,
        "userId": 1,
        "orderId": 5,
        "type": "EARN",
        "points": 500,
        "relatedAmount": 50000.00,
        "balanceAfter": 5500,
        "description": "Points earned from order #5",
        "referenceId": "ORDER_5",
        "createdAt": "2024-01-29T15:00:00"
      },
      {
        "id": 2,
        "userId": 1,
        "orderId": null,
        "type": "ADJUST",
        "points": 1000,
        "relatedAmount": null,
        "balanceAfter": 6000,
        "description": "Bonus for promotion",
        "referenceId": null,
        "createdAt": "2024-01-30T10:30:00"
      }
    ],
    "totalElements": 20,
    "totalPages": 1
  }
}
```

**Point Transaction Types:** `EARN`, `REDEEM`, `EXPIRE`, `ADJUST`, `BONUS`, `REFUND`

## 9.4 Get Loyalty Statistics
Lấy thống kê loyalty program.

**Request:**
```http
GET /api/admin/loyalty/statistics
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "LOYALTY_STATISTICS_RETRIEVED",
  "data": {
    "totalMembers": 300,
    "totalPointsIssued": 150000,
    "totalPointsRedeemed": 80000,
    "totalStampCards": 500,
    "totalRewardsRedeemed": 50
  }
}
```

---

# 10. Admin Partner Management APIs

**Base URL:** `/api/admin/partners`

## 10.1 Get All Partners
Lấy danh sách tất cả partners với filter theo status.

**Request:**
```http
GET /api/admin/partners?status=PENDING&page=0&size=20
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | PartnerStatus | No | Lọc theo status |

**Partner Status Values:** `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "PARTNERS_RETRIEVED",
  "data": {
    "content": [
      {
        "id": 1,
        "userId": 5,
        "userName": "Partner User",
        "businessName": "ABC Laundry Co.",
        "businessRegistrationNumber": "BRN123456",
        "taxId": "TAX789012",
        "businessAddress": "789 Business St",
        "contactPhone": "+84987654321",
        "contactEmail": "partner@example.com",
        "status": "PENDING",
        "approvedAt": null,
        "approvedBy": null,
        "rejectionReason": null,
        "revenueSharePercent": 70.00,
        "storeCount": 0,
        "staffCount": 0,
        "notes": null,
        "createdAt": "2024-01-25T10:00:00",
        "updatedAt": "2024-01-25T10:00:00"
      }
    ],
    "totalElements": 5,
    "totalPages": 1
  }
}
```

## 10.2 Get Partner By ID
Lấy thông tin partner theo ID.

**Request:**
```http
GET /api/admin/partners/{partnerId}
```

**Response (200 OK):** (giống Get All Partners)

## 10.3 Approve Partner
Phê duyệt partner application.

**Request:**
```http
POST /api/admin/partners/{partnerId}/approve
Authorization: Bearer <jwt_token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "PARTNER_APPROVED",
  "data": {
    "id": 1,
    "userId": 5,
    "userName": "Partner User",
    "businessName": "ABC Laundry Co.",
    "status": "APPROVED",
    "approvedAt": "2024-01-30T10:30:00",
    "approvedBy": 1,
    "rejectionReason": null,
    ...
  }
}
```

## 10.4 Reject Partner
Từ chối partner application.

**Request:**
```http
POST /api/admin/partners/{partnerId}/reject?reason=Invalid business registration
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| reason | String | Yes | Lý do từ chối |

**Response (200 OK):**
```json
{
  "success": true,
  "message": "PARTNER_REJECTED",
  "data": {
    "id": 1,
    "status": "REJECTED",
    "rejectionReason": "Invalid business registration",
    ...
  }
}
```

## 10.5 Suspend Partner
Tạm ngưng partner.

**Request:**
```http
POST /api/admin/partners/{partnerId}/suspend
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "PARTNER_SUSPENDED",
  "data": {
    "id": 1,
    "status": "SUSPENDED",
    ...
  }
}
```

---

# Enums Reference

## OrderStatus
| Value | Description |
|-------|-------------|
| INITIALIZED | Order created, waiting for customer to put items |
| RESERVED | Order reserved but not yet started |
| WAITING | Items placed, waiting for staff to collect |
| COLLECTED | Staff collected items from locker |
| PROCESSING | Items being processed at store |
| READY | Items ready to be returned |
| RETURNED | Items returned to locker |
| COMPLETED | Customer received items, order completed |
| CANCELED | Order canceled |

## OrderType
| Value | Description |
|-------|-------------|
| LAUNDRY | Regular laundry service |
| DRY_CLEAN | Dry cleaning |
| STORAGE | Storage only |

## PaymentMethod
| Value | Description |
|-------|-------------|
| CASH | Cash payment |
| WALLET | Wallet payment |
| BANK_TRANSFER | Bank transfer |
| MOMO | Momo e-wallet |
| VNPAY | VNPay |
| ZALOPAY | ZaloPay |

## PaymentStatus
| Value | Description |
|-------|-------------|
| PENDING | Payment pending |
| PROCESSING | Payment processing |
| COMPLETED | Payment completed |
| FAILED | Payment failed |
| REFUNDED | Payment refunded |
| CANCELED | Payment canceled |

## PartnerStatus
| Value | Description |
|-------|-------------|
| PENDING | Initial status when partner registers |
| APPROVED | Partner has been approved by admin |
| REJECTED | Partner application was rejected |
| SUSPENDED | Partner account has been suspended |

## BoxStatus
| Value | Description |
|-------|-------------|
| AVAILABLE | Box available for use |
| OCCUPIED | Box occupied |
| RESERVED | Box reserved |
| MAINTENANCE | Box under maintenance |

## LockerStatus
| Value | Description |
|-------|-------------|
| ACTIVE | Locker active |
| INACTIVE | Locker inactive |
| MAINTENANCE | Locker under maintenance |
| DISCONNECTED | Locker disconnected |

## RoleName
| Value | Description |
|-------|-------------|
| USER | Regular user |
| STAFF | Staff member |
| ADMIN | Administrator |
| MODERATOR | Moderator |
| PARTNER | Partner |

## AuthProvider
| Value | Description |
|-------|-------------|
| LOCAL | Local authentication |
| GOOGLE | Google OAuth |
| FACEBOOK | Facebook OAuth |
| GITHUB | GitHub OAuth |
| ZALO | Zalo OAuth |
| PHONE | Phone OTP |
| EMAIL | Email OTP |

## PointTransactionType
| Value | Description |
|-------|-------------|
| EARN | Points earned from order payment |
| REDEEM | Points redeemed for discount |
| EXPIRE | Points expired |
| ADJUST | Points adjusted by admin |
| BONUS | Bonus points from promotions |
| REFUND | Points refunded from cancelled order |

## StampType
| Value | Description |
|-------|-------------|
| BOX | Stamp card for box usage |
| SERVICE | Stamp card for laundry service |

---

# Common Response Format

## Success Response
```json
{
  "success": true,
  "message": "SUCCESS_CODE",
  "data": { ... }
}
```

## Error Response
```json
{
  "success": false,
  "message": "ERROR_CODE",
  "data": null,
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format"
    }
  ]
}
```

## Pagination Response
```json
{
  "success": true,
  "message": "RETRIEVED",
  "data": {
    "content": [ ... ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": { ... }
    },
    "totalElements": 100,
    "totalPages": 5,
    "last": false,
    "first": true,
    "empty": false
  }
}
```
