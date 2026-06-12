# USER flow merge status

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Files changed

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
| Home USER | Merged | Added USER-only `Giat do` shortcut in non-courier `HomePage` branch. |
| Create laundry order | Merged | New `UserLaundryOrderPage`. |
| Choose service | Partial | UI/service added, but backend `/api/services` route/controller was not confirmed. No mock data added. |
| Choose locker | Merged | Uses `/api/stores`, `/api/lockers`, `/api/lockers/{lockerId}/boxes/available`. |
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
