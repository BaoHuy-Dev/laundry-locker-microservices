# USER flow merge plan

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Goal

Merge only the USER/customer flow from old React Native mobile into the new Flutter project.

Do not change courier/staff/admin/moderator flows.

## Merge strategy

1. Keep the current Flutter role and routing model.
2. Add USER laundry flow as a separate feature module: `lib/features/user_laundry`.
3. Reuse the existing `ApiClient`, `DioClient`, `AuthInterceptor`, and `TokenService`.
4. Do not create duplicate token storage.
5. Do not refactor existing locker/order/profile providers used by other roles.
6. Add a USER-only shortcut in `HomePage` non-courier branch.
7. Add a standalone route `/user/laundry-order`.

## What was merged into Flutter

New USER module:

- Models for store, locker, box, service, order, payment, notification.
- Gateway response unwrapping helper.
- USER API services:
  - `UserAuthService`
  - `UserService`
  - `UserOrderService`
  - `UserLockerService`
  - `UserPaymentService`
  - `UserNotificationService`
  - plus `UserLaundryService` for laundry service catalog.
- USER create-order UI:
  - choose store
  - choose locker
  - choose available box
  - choose category `LAUNDRY` or `STORAGE`
  - choose services
  - add note/promotion code
  - create order
  - best-effort confirm order
  - create payment
  - open external payment URL/deeplink
  - refresh order status

## Existing Flutter screens reused

Kept as-is:

- Login/onboarding shell.
- Existing USER tabs.
- Existing order list/detail.
- Existing profile/logout.
- Existing notification page.
- Existing smart locker rent/send/pickup flows.

## USER entry point

`HomePage` non-courier quick actions now includes:

- `Giat do` -> `AppRouter.userLaundryOrder` -> `/user/laundry-order`.

This is inside the non-courier branch only, so courier mode UI does not see the new shortcut.

## Role protection decision

The current Flutter router does not fully gate every route by role. The merge preserves that behavior.

Role dispatch after login remains owned by current Flutter auth/token logic. The merge changes only the USER branch by adding the new route and shortcut.

## Backend/API decision

All new USER services call gateway paths under `/api/...`.

The new services do not hardcode mock data. If backend returns no data or a route is missing, the UI shows an unavailable state.

## Known TODOs

1. Service catalog backend route:
   - Flutter calls `/api/services`.
   - Gateway filter mentions services/laundry-services, but no confirmed gateway route/controller source was found in the backend tree.
   - Until backend publishes service catalog, service selection cannot complete without real data.

2. Auth UI integration:
   - `UserAuthService` is connected to gateway auth endpoints.
   - Existing Flutter login/register/OTP UI is shared by all roles and was not rewritten in this USER-only pass.
   - A follow-up should decide whether to replace common auth data source with `/api/auth/*` or add a USER-specific auth flow.

3. Order history screen:
   - Existing Flutter `OrderPage` was not replaced to avoid impacting courier order switching.
   - New `UserOrderService.getMyOrders()` is ready for a future USER history tab update if needed.

4. Promotions/loyalty:
   - Old mobile has voucher/loyalty flows.
   - This pass only carries a promotion code field into create order.
   - Full voucher picker/loyalty rewards should be added after confirming backend endpoints and UX ownership.
