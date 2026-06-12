# Flutter current role flow

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Scope

Project: `D:\BigProject\smart-laundry-locker-flutter`

This document records the current Flutter role/navigation shape before and during the USER-only merge. The merge keeps non-USER role flows unchanged.

## Roles found

| Role name in code/token | Current Flutter usage | Notes |
| --- | --- | --- |
| `CUSTOMER` / USER | Main app shell: Home, Lockers, QR, Orders, Profile. | This is the default customer/user experience. The new laundry order flow was added here only. |
| `COURIER` | Courier mode toggle, courier dashboard widgets, courier order screens, active delivery, courier map. | This is the app's SHIPPER-equivalent role. No direct `SHIPPER` string was found. |
| `TECHNICIAN` | Can see courier-mode entry points in profile and is treated as staff/ops in some checks. | Kept unchanged. |
| `STAFF` | Profile detail checks and staff application related UI. | Kept unchanged. |
| `ADMIN` | No full admin UI flow found in Flutter. Role is used in repository/API routing checks and backend has admin endpoints. | Kept unchanged. |
| `MODERATOR` | Not found in Flutter code. | No current Flutter flow. |

## Navigation/router

Flutter uses `go_router` in `lib/core/routing/app_router.dart`.

Main pieces:

- Initial route: `/` -> `SplashScreen`.
- Login/onboarding route: `/onboarding`.
- OTP route: `/otp`.
- USER shell route: `ShellRoute` with `MainNavigationWrapper`.
- USER shell tabs: `/home`, `/lockers`, `/orders`, `/profile`; QR scan is launched from bottom navigation.
- Courier routes are outside the USER shell, e.g. `/courier-dashboard`, `/active-delivery`, `/courier-map`, `/active-order-details`.
- Profile/security/subscription/transaction/maintenance/delegation routes are standalone routes.

New USER-only route added:

- `/user/laundry-order` -> `UserLaundryOrderPage`.

## USER flow currently in Flutter

Files/routes:

- `HomePage`: normal CUSTOMER home when `CourierModeProvider.isCourierModeActive == false`.
- `LockerPage`: customer locker/location view.
- `LockerActionPage`: rent/send locker flows.
- `LockerOtpPage`: pickup/open locker with OTP.
- `OrderPage`: customer order list when courier mode is off.
- `CustomerOrderDetailPage`: customer order detail.
- `NotificationListPage`: notification list.
- `ProfilePage`, `ProfileDetailPage`, `EditProfilePage`, `SecurityPage`: profile/account flow.
- New: `UserLaundryOrderPage`: USER laundry order creation via gateway APIs.

## Courier / shipper-equivalent flow

Files/routes:

- `CourierModeProvider` reads roles from JWT and enables courier mode for `COURIER`.
- `HomePage` switches to courier widgets when courier mode is active.
- `OrderPage` switches to courier order data/views when courier mode is active.
- `CourierDashboardPage`, `ActiveDeliveryPage`, `CourierMapScreen`, `ActiveOrderDetailsPage`.
- `GlobalDispatchListener` in `main.dart` listens for incoming courier dispatch events.

Not changed by the merge.

## Staff / technician flow

Files/routes:

- Staff application/profile related UI in `features/profile`.
- Maintenance/report routes: `/maintenance/create-report`, `/maintenance/my-reports`.
- `TECHNICIAN` role participates in profile/courier-mode eligibility checks.

Not changed by the merge.

## Admin/moderator flow

- No dedicated admin or moderator Flutter screen flow was found.
- Backend has admin endpoints, and Flutter repository code has admin/technician routing checks.

Not changed by the merge.
