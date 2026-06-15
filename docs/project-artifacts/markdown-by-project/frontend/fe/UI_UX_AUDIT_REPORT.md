# 📊 Báo Cáo Kiểm Tra Admin Portal - UI/UX Audit

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

**Ngày kiểm tra:** 2026-02-27  
**Ngườii thực hiện:** AI Assistant  
**Phạm vi:** Laundry Locker Frontend - Admin Portal

---

## 🎯 Tổng Quan

Báo cáo này đánh giá mức độ hoàn thiện của Admin Portal so với các Use Cases đã định nghĩa, và xác định các vấn đề UX cần cải thiện.

### Điểm Tổng Quan

| Category | Score | Status |
|----------|-------|--------|
| **Use Case Coverage** | 60% | ⚠️ Cần cải thiện |
| **UX Completeness** | 70% | ⚠️ Placeholders cần implement |
| **Visual Design** | 90% | ✅ Tốt |
| **Data Presentation** | 75% | ⚠️ Thiếu detail views |

---

## ✅ Danh Sách Pages Đã Implement

| Page | Status | Chức năng chính |
|------|--------|-----------------|
| **Dashboard** | ✅ | Overview, charts, stats |
| **Users** | ✅ | CRUD users, status toggle |
| **Stores** | ✅ | CRUD stores, modals |
| **Lockers** | ✅ | Status mgmt, box visualization |
| **Services** | ✅ | CRUD services, pricing |
| **Orders** | ✅ | List view, status filter |
| **Payments** | ✅ | List view, stats |
| **Loyalty** | ✅ | Members list |
| **Partners** | ✅ | Approve/Reject/Suspend |
| **Feedback** | ✅ | List view |

---

## 🔴 Critical Issues (Cần Fix Ngay)

### 1. Thiếu Order Detail Page

**Mô tả:** Orders page chỉ hiển thị list view với thông tin cơ bản. Không có trang chi tiết để xem và quản lý đơn hàng.

**Thông tin hiện tại:**
- Mã đơn, Khách hàng, Dịch vụ, Tổng tiền, Trạng thái, Ngày tạo

**Thông tin thiếu:**
- Chi tiết items trong đơn
- Locker và Box info
- PIN code (để support khách)
- Timeline trạng thái
- Payment status
- Customer note, Staff note
- Action history

**Use Cases bị ảnh hưởng:**
- UC1: Không thể view order details, cancel orders
- UC5: Không thể xem pickup status, overtime fee
- UC6: Không thể view payment history

**Đề xuất giải pháp:**
```
Tạo trang Order Detail với layout:
├── Order Header (ID, Status, Date)
├── Customer Info Card
├── Locker & Box Info Card  
├── Items List (table)
├── Payment Info Card
├── Status Timeline (vertical)
└── Actions (Cancel, Update Status)
```

**Priority:** P0 - Critical  
**Estimated Effort:** 2-3 days

---

## 🟡 Medium Issues (Nên Fix)

### 2. Action Buttons Là Placeholders

**Vị trí:** Nhiều trang có action buttons chỉ log ra console

**Ví dụ:**
```typescript
// src/pages/Admin/orders/index.tsx
action={{
  label: "Tạo đơn hàng",
  onClick: () => console.log("Create order"), // Placeholder!
}}
```

**Các buttons bị ảnh hưởng:**
- "Tạo đơn hàng" trong Orders page
- "Thêm người dùng" trong Users page
- "Cập nhật trạng thái" trong OrderTable
- "Xem chi tiết" trong nhiều tables

**Đề xuất:**
- Implement modals cho Create/Edit
- Navigate to detail pages cho "Xem chi tiết"
- Connect API mutations cho status updates

**Priority:** P1  
**Estimated Effort:** 1-2 days

---

### 3. Scheduler Management Chỉ Có API, Không Có UI

**APIs đã có:**
```typescript
POST /api/admin/scheduler/auto-cancel
POST /api/admin/scheduler/release-boxes
POST /api/admin/scheduler/pickup-reminders
GET  /api/admin/scheduler/status
```

**Vấn đề:** Admin không có giao diện để:
- Xem trạng thái các scheduler tasks
- Trigger manually các tasks
- Xem lịch sử chạy

**Use Case liên quan:** UC8

**Đề xuất:**
Thêm section trong Dashboard hoặc tạo page riêng:
```
Scheduler Management
├── Auto-Cancel: Last run 5 mins ago [Run Now]
├── Release Boxes: Last run 2 mins ago [Run Now]
├── Pickup Reminders: Last run 1 hour ago [Run Now]
└── Overall Status: ✅ Running
```

**Priority:** P2  
**Estimated Effort:** 1 day

---

### 4. Box-level Management Thiếu

**Vấn đề:** Locker page chỉ hiển thị aggregate stats (12/20 available). Không có view chi tiết từng box.

**Thiếu:**
- Danh sách tất cả boxes trong locker
- Trạng thái từng box (AVAILABLE/OCCUPIED/MAINTENANCE)
- Đơn hàng đang dùng box (nếu OCCUPIED)
- Force open box cho support

**Use Cases bị ảnh hưởng:**
- UC1: Không thể support mở tủ manually
- UC4: Không thể kiểm tra box status chi tiết

**Đề xuất:**
Thêm "View Boxes" button trong LockerTable:
```
Locker Detail: ESP8266_LOCKER_01
├── Info Card
└── Boxes Grid (20 boxes)
    ├── Box #1: 🟢 AVAILABLE
    ├── Box #2: 🟠 OCCUPIED - Order #1234 [Force Open]
    └── Box #3: 🔧 MAINTENANCE
```

**Priority:** P2  
**Estimated Effort:** 1-2 days

---

### 5. Thiếu Breadcrumb Navigation

**Vấn đề:** Không có breadcrumb, khó navigate khi vào detail pages.

**Ví dụ flow:**
```
Dashboard → Orders → Order #12345 → Customer Profile
                ↑ Không có cách nào để back về Orders ngoài sidebar
```

**Đề xuất:**
```
Home / Orders / Order #12345
```

**Priority:** P3  
**Estimated Effort:** 0.5 day

---

## 🟢 Low Priority (Nice to Have)

### 6. Real-time Updates

**Vấn đề:** Dữ liệu static, phải refresh page để thấy thay đổi.

**Nên có:**
- WebSocket hoặc polling cho order status changes
- Notifications khi có partner registration mới
- Real-time locker status updates

**Priority:** P3  
**Estimated Effort:** 2-3 days

---

### 7. Advanced Filtering

**Vấn đề:** Filter cơ bản (search + status), thiếu:
- Date range filter (Orders, Payments)
- Store filter (Orders, Lockers)
- Price range filter (Orders, Payments)

**Priority:** P3  
**Estimated Effort:** 1-2 days

---

## 📋 Use Case Mapping Chi Tiết

### ✅ Fully Mapped

| Use Case | Features | Status |
|----------|----------|--------|
| **UC7: Partner Registration** | Partners page với approve/reject/suspend | ✅ Complete |

### ⚠️ Partially Mapped

| Use Case | Implemented | Missing |
|----------|-------------|---------|
| **UC1: Create Order** | List view | Detail view, Cancel action |
| **UC8: Scheduler** | API endpoints | Management UI |

### ❌ Not Mapped

| Use Case | Missing Features |
|----------|-----------------|
| **UC2-UC6** | Order detail management, Status updates |

---

## 🎨 UX Observations

### Điểm Tốt ✅

1. **Consistent Design System**
   - Color palette nhất quán (blue primary, gray neutrals)
   - Typography rõ ràng
   - Card-based layout dễ đọc

2. **Visual Indicators**
   - Status badges với màu sắc rõ ràng
   - Progress bars cho locker occupancy
   - Icons phù hợp (Lucide)

3. **Responsive Layout**
   - Sidebar collapsible
   - Tables scrollable trên mobile
   - Grid layouts adaptive

4. **Loading States**
   - Skeleton screens
   - Loading indicators
   - Empty states

### Cần Cải Thiện ⚠️

1. **Interactive Feedback**
   - Toast notifications khi update (đã có sonner nhưng chưa dùng hết)
   - Confirmation dialogs cho destructive actions
   - Loading states cho mutations

2. **Data Density**
   - Orders table có thể bị cramped trên small screens
   - Nên cho phép column resizing hoặc hide/show

3. **Search Experience**
   - Chưa có search suggestions
   - Chưa có recent searches
   - Chưa có filter chips (e.g., "Status: PENDING ✕")

---

## 🛠️ Recommendations

### Immediate Actions (Tuần 1)

1. **Implement Order Detail Page**
   - Route: `/admin/orders/:id`
   - Layout: 2-column (Info + Timeline)
   - Actions: Cancel, Update Status

2. **Connect Placeholder Actions**
   - Create Order modal
   - Update Status dropdown
   - View Details navigation

### Short Term (Tuần 2-3)

3. **Scheduler Management**
   - Add to Dashboard hoặc tạo page mới
   - Trigger buttons + status display

4. **Box-level Locker View**
   - Grid view của boxes
   - Force open functionality

### Long Term (Tuần 4+)

5. **Real-time Updates**
   - WebSocket integration
   - Notification center

6. **Advanced Features**
   - Export data (CSV/PDF)
   - Bulk actions
   - Advanced filters

---

## 📊 Tóm Tắt

### Đã Hoàn Thành Tốt ✅
- Visual design system
- Component consistency
- Responsive layout
- Basic CRUD cho Users, Stores, Services
- Partner approval workflow

### Cần Cải Thiện Gấp 🔴
- Order detail management (Critical)
- Connect placeholder actions

### Nên Thêm 🟡
- Scheduler UI
- Box-level management
- Breadcrumb navigation

---

**Kết luận:** Admin portal có nền tảng tốt với UI/UX đẹp và nhất quán. Tuy nhiên, **thiếu critical feature là Order Detail View** để admin có thể quản lý đơn hàng effectively. Cần ưu tiên implement các features này để hoàn thành use cases.

---

*Report generated: 2026-02-27*  
*Next review: Sau khi implement Order Detail Page*
