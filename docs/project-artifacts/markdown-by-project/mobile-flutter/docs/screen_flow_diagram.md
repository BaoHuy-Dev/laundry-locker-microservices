# AISL App Screen Flow Diagrams

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

Tài liệu này cung cấp các sơ đồ luồng màn hình (Screen Flow Diagrams) cho cả 2 vai trò khách hàng (Customer) và người giao hàng (Courier) được quét trực tiếp từ cấu trúc file source code của ứng dụng.

**Quá trình scan trực tiếp từ codebase `/lib/features/` đã xác định chính xác các màn hình thực tế (không dùng thông tin mẫu từ Internet):**
- **Hình chữ nhật `[]`**: Tượng trưng cho các màn hình (Screens / Pages) vật lý thực tế được định nghĩa trong codebase (đuôi `_page.dart` hoặc `_screen.dart`).
- **Hình oval (viên thuốc) `(())`**: Tượng trưng cho các actions (Hành động).

---

## 1. Customer Screen Flow (Luồng Màn Hình Khách Hàng)

Trang `Home Page` là trung tâm để khách hàng rẽ nhánh sang việc tra cứu vị trí tủ (Map Location), xem đơn hàng (Order/Order Detail), ví tiền (Transactions), báo cáo lỗi (Report List) cũng như đăng ký làm nhân viên (`Staff Application Page`).

```mermaid
flowchart TD
    %% Auth
    Splash[Splash Screen] --> Onboarding[Onboarding Page]
    Onboarding --> OTP[OTP Page]
    OTP --> Home[Home Page]
    
    %% Home Navigation
    Home --> Locker[Locker Page]
    Home --> Order[Order Page]
    Home --> Notification[Notification List Page]
    Home --> Profile[Profile Page]
    
    %% Locker & Location Flow
    Locker --> LockerMap[Locker Map Page]
    LockerMap --> LockerDetailMap[Locker Detail Map Page]
    LockerDetailMap --> LockerAction[Locker Action Page]
    
    LockerAction --> RentLocker((Action: Rent/Deposit/Retrieve))
    LockerAction --> LockerOTP[Locker OTP Page]
    LockerAction --> SearchDelegate[Search Delegatee Page]
    
    %% Order Flow
    Order --> CustOrderDetail[Customer Order Detail Page]
    
    %% Profile & Settings Flow
    Profile --> ProfDet[Profile Detail Page]
    ProfDet --> EditProf[Edit Profile Page]
    ProfDet --> FaceVer[Face Verify Page]
    
    Profile --> Trans[Transactions Page]
    Trans --> TopUp[Top Up Page]
    
    Profile --> RepList[Report List Page]
    RepList --> CreateRep[Create Report Page]
    
    Profile --> Policy[Policy Page]
    Profile --> StaffApp[Staff Application Page]
```

---

## 2. Courier Screen Flow (Luồng Màn Hình Người Giao Hàng)

Luồng giao hàng (Courier) bắt đầu từ việc đăng ký để trở thành Courier, xem Dashboard & Map để nhận chuyến giao hàng (Active Delivery), và vào các chi tiết đơn hàng dành riêng cho Courier.

```mermaid
flowchart TD
    %% Registration
    OTP[OTP Page] --> CourReg[Courier Registration Page]
    CourReg --> CourRegStatus[Courier Registration Status Page]
    CourRegStatus --> Dash[Courier Dashboard Page]
    
    %% Dashboard
    Dash --> CourMap[Courier Map Screen]
    Dash --> ActDel[Active Delivery Page]
    Dash --> Order[Order Page]
    Dash --> Profile[Profile Page]
    Dash --> Notification[Notification List Page]
    
    %% Delivery & Action
    CourMap --> ActDel
    ActDel --> UpdateLoc((Action: Update Location))
    ActDel --> VerifyOpen((Action: Deposit Open/Confirm))
    
    %% Order Detail for Courier
    Order --> CourOrderDetail[Courier Order Detail Page]
    
    %% Profile
    Profile --> CourDet[Courier Detail Page]
    Profile --> ProfDet[Profile Detail Page]
    ProfDet --> EditProf[Edit Profile Page]
    ProfDet --> FaceVer[Face Verify Page]
    
    Profile --> Trans[Transactions Page]
    Trans --> TopUp[Top Up Page]
    
    Profile --> Policy[Policy Page]
```
