# USER flow merge status

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-15:** UX/UI revamp luồng tủ khóa cho Customer đã hoàn tất trên branch `feat/mobile-user-ui-revamp` (develop). Các thay đổi chính: (1) Tab Tủ + Xem tất cả home đều dẫn đến lưới ô tủ thực tế thay vì danh sách địa điểm; (2) Ô DRONE hiển thị icon máy bay + màu indigo + không cho đặt dịch vụ thường; (3) Booking (Thuê tủ/Gửi hàng) bỏ picker khi đến từ lưới ô; (4) CellType.java constants class được thêm vào backend với Javadoc đầy đủ. `flutter analyze` 0 error.
<!-- CURRENT_STATUS_END -->

## Files changed

### 2026-06-15 — UX revamp lưới ô tủ + DRONE cell type (branch `feat/mobile-user-ui-revamp`)

Routing và home:

- `lib/core/routing/app_router.dart` — thêm route `/stores/lockers` (future deep link); "Xem tất cả" home → LockerPage
- `lib/features/home/presentation/pages/home_page.dart` — `onSeeAll` → `context.go(AppRouter.lockers)` thay vì StoresPage

Lưới ô tủ (mới):

- `lib/features/stores/presentation/pages/store_lockers_page.dart` — **tạo mới**: `StoreLockerGridPage`, lưới ô 2D, `_LockerCard` lazy-load, `_CellGrid`, `_CellTile` (DRONE icon + màu indigo + non-tappable), `_BookingSheet`, `_GridLegend` (thêm Drone), `_CellPalette` (thêm drone = `0xFF6366F1`), `_cellTypeForBooking()` helper

Locker list tab:

- `lib/features/locker/presentation/pages/locker_page.dart` — `_navigateToMap` → `StoreLockerGridPage` thay `LockerDetailMapPage`; bridge `LockerLocation` → `Store` qua `int.tryParse`

Chi tiết cửa hàng:

- `lib/features/stores/presentation/pages/store_detail_page.dart` — "Xem tủ" dùng `Navigator.push` → `StoreLockerGridPage`

Booking pages (bỏ picker khi đến từ lưới ô):

- `lib/features/locker_ops/presentation/pages/rent_locker_page.dart` — thêm `initialLockerName`, `initialCellType`; bỏ picker + skip API load khi `initialLockerId != null`; card read-only tủ + card read-only loại ô
- `lib/features/locker_ops/presentation/pages/send_parcel_page.dart` — thêm `initialLockerName`; bỏ picker + skip API load khi `initialLockerId != null`; card read-only tủ

API service:

- `lib/features/locker_ops/data/locker_ops_service.dart` — thêm `lockersByStore(int storeId)`

Brand colors:

- `lib/features/locker/presentation/pages/locker_detail_map_page.dart` — AppBar/FAB/fallback dùng `AislBrand.navy`
- `lib/features/locker/presentation/widgets/locker_info_modal.dart` — size card header dùng `AislBrand.brandGradient`; "Đặt dịch vụ" → `AislBrand.navy`

### 2026-06-13 (trước đó)

Routing and USER entry:

- `lib/core/routing/app_router.dart`
- `lib/features/home/presentation/pages/home_page.dart`

New USER laundry module:

- `lib/features/user_laundry/infrastructure/models/user_laundry_models.dart`
- `lib/features/user_laundry/infrastructure/services/user_gateway_client.dart`
- `lib/features/user_laundry/infrastructure/services/user_auth_service.dart`
- `lib/features/user_laundry/infrastructure/services/user_service.dart`
- `lib/features/user_laundry/infrastructure/services/user_locker_service.dart`
- `lib/features/user_laundry/infrastructure/services/user_laundry_service.dart`
- `lib/features/user_laundry/infrastructure/services/user_order_service.dart`
- `lib/features/user_laundry/infrastructure/services/user_payment_service.dart`
- `lib/features/user_laundry/infrastructure/services/user_notification_service.dart`
- `lib/features/user_laundry/presentation/pages/user_laundry_order_page.dart`

Docs:

- `docs/flutter-current-role-flow.md`
- `docs/old-mobile-user-flow.md`
- `docs/user-flow-merge-plan.md`
- `docs/user-api-mapping.md`
- `docs/merge-status.md`

## USER flows merged

| Flow | Status | Notes |
| --- | --- | --- |
| Login/register | Partial | Shared Flutter login now calls `POST /api/auth/login` with `identifier/password`. USER `UserAuthService` gateway methods added. Register UI still needs a focused follow-up because it is shared by all roles. |
| Verify OTP | Partial | Email OTP service methods added. Existing shared OTP UI still needs integration with backend temp-token completion if used for new users. |
| Home USER | Merged | Added USER-only `Giat do` shortcut in non-courier `HomePage` branch. "Xem tất cả" locker → LockerPage (nhất quán với tab Tủ). |
| Create laundry order | Merged | New `UserLaundryOrderPage`. |
| Choose service | Partial | UI/service added, but backend `/api/services` route/controller was not confirmed. No mock data added. |
| Xem lưới ô tủ tại địa điểm | **Merged (2026-06-15)** | `StoreLockerGridPage` mới: load tủ theo store (`GET /api/lockers?storeId=X`), lazy-load layout từng tủ (`GET /api/lockers/{id}/layout`), lưới ô 2D theo rowIndex/colIndex, màu theo status, ô DRONE hiện icon máy bay + không cho đặt dịch vụ. Vào từ: tab Tủ → tap địa điểm, cửa hàng → "Xem tủ", home → "Xem tất cả". |
| Booking thuê tủ (từ lưới ô) | **Merged (2026-06-15)** | Khi vào `RentLockerPage` từ lưới ô (`initialLockerId` có sẵn): bỏ picker chọn tủ, bỏ API load danh sách tủ, hiện card read-only tủ + loại ô đã chọn (có icon khóa). |
| Booking gửi hàng (từ lưới ô) | **Merged (2026-06-15)** | Khi vào `SendParcelPage` từ lưới ô: bỏ picker chọn tủ, bỏ API load, hiện card read-only. |
| Choose locker | Merged | Uses `/api/stores`, `/api/lockers`, `/api/lockers/{lockerId}/boxes/available`. Nay thêm `/api/lockers?storeId=X` và `/api/lockers/{id}/layout`. |
| Payment | Merged | Creates payment via `/api/payments` and opens payment URL/deeplink if returned. |
| Track status | Merged | Refreshes order status via `/api/orders/{id}/status`. |
| Order history | Service ready | `UserOrderService.getMyOrders()` added. Existing Flutter `OrderPage` not replaced to protect courier/customer switching. |
| Notifications | Service ready | USER notification service added. Existing notification page not replaced. |
| Profile | Service ready | USER profile service added. Existing profile UI not replaced. |
| Logout | Service ready | USER auth service logout added. Existing profile logout not replaced. |

## Other roles impact

No non-USER role flow was intentionally changed.

Kept unchanged:

- Courier mode provider and courier dispatch listener.
- Courier dashboard, courier order, active delivery, courier map.
- Staff/technician profile/application/maintenance screens.
- Admin/backend-only role behavior.
- Existing bottom navigation tabs.
- Existing order/profile/notification providers.

The only visible UI change is in USER Home when courier mode is off.

## APIs connected

Connected through new USER services:

- `/api/auth/login`
- `/api/auth/register`
- `/api/auth/email/send-otp`
- `/api/auth/email/verify-otp`
- `/api/auth/email/complete-registration`
- `/api/auth/logout`
- `/api/user/profile`
- `/api/user/me/statistics`
- `/api/user/fcm-token`
- `/api/user/password`
- `/api/stores`
- `/api/lockers`
- `/api/lockers/{lockerId}/boxes`
- `/api/lockers/{lockerId}/boxes/available`
- `/api/lockers?storeId={storeId}` *(thêm 2026-06-15 — load tủ theo cửa hàng)*
- `/api/lockers/{id}/layout` *(thêm 2026-06-15 — lưới ô vật lý, trả về danh sách CellResponse)*
- `/api/lockers/{id}/report`
- `/api/orders`
- `/api/orders/{id}/confirm`
- `/api/orders/{id}/status`
- `/api/orders/{id}`
- `/api/orders/my-orders`
- `/api/orders/{id}/cancel`
- `/api/payments`
- `/api/payments/order/{orderId}`
- `/api/notifications/all`
- `/api/notifications/unread`
- `/api/notifications/unread/count`
- `/api/notifications/{id}/read`
- `/api/notifications/read-all`
- `/api/notifications/{id}`

## APIs still TODO

- `/api/services`: Flutter service selection calls this, but backend route/controller was not confirmed in the current microservice tree.
- Full promotion/voucher picker: old mobile uses promotion and loyalty services; only raw promotion code passthrough was merged.
- Shared register/OTP UI integration: requires a focused change because current Flutter auth screens are shared by all roles.

## Checks

Completed:

- `flutter pub get`: passed.
- `flutter analyze`: ran. Plain command exits with code 1 because the project currently has 443 info-level lint findings. No error/warning-level issue was introduced by the merge.
- `flutter analyze --no-fatal-infos`: passed.
- `flutter build apk --debug --dart-define=API_BASE_URL=http://10.0.2.2:8080`: passed after wiring shared login to `/api/auth/login`, generated `build/app/outputs/flutter-apk/app-debug.apk`.

Blocked:

- `flutter run` on Android emulator: blocked because the available AVD `Pixel_8` exits during startup with `Your device does not have enough disk space to run avd: Pixel_8`.
- Web/desktop fallback is not usable for this repo because only `android/` and `ios/` platform folders exist; `web/` and `windows/` are not configured.

## Local seed data created

- Account: `nqbhuy2004nt@gmail.com`
- Role: `CUSTOMER`
- User id: `1`
- Temporary password: `Huy@123456`
- Store: `Lockerly Demo Store`, id `1`
- Locker: `Demo Locker 01`, id `1`
- Available boxes: ids `1`, `2`, `3`
