# Old mobile USER flow

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Scope

Project: `D:\BigProject\laundry-locker-frontend\mobile`

Only the USER/customer flow was analyzed. Partner/admin/staff screens were intentionally excluded.

## USER navigation

Old app uses Expo Router.

USER stack:

- `app/user/_layout.tsx`
- `app/user/(tabs)/_layout.tsx`
- `app/user/create-order.tsx`
- `app/user/confirm-order.tsx`
- `app/user/stores.tsx`
- `app/user/store-detail.tsx`
- `app/user/favorites.tsx`
- `app/user/vouchers.tsx`

USER tabs:

- `app/user/(tabs)/index.tsx`: Home
- `app/user/(tabs)/lockers.tsx`: Stores/lockers/boxes
- `app/user/(tabs)/orders.tsx`: Orders/history/status
- `app/user/(tabs)/notifications.tsx`: Notifications
- `app/user/(tabs)/profile.tsx`: Profile/account

## Login/register/OTP

Relevant files:

- `app/(auth)/login.tsx`
- `app/(auth)/register.tsx`
- `app/(auth)/forgot-password.tsx`
- `services/user/authService.ts`

Flow:

- Login by phone/email support.
- OTP verification support.
- New users complete registration after OTP/temp token.
- Existing users receive `accessToken` and `refreshToken`.
- Tokens are stored and attached by `services/api.ts`.

## Home

File: `app/user/(tabs)/index.tsx`

Main behavior:

- Fetch current profile with `userService.getProfile()`.
- Fetch stores with `storeService.getAllStores()`.
- Fetch unread notification count.
- Quick actions: orders, lockers, stores, notifications, offers/profile, create order.
- Favorite stores stored locally with AsyncStorage.

## Locker/store selection

File: `app/user/(tabs)/lockers.tsx`

Main behavior:

- Fetch stores.
- Fetch lockers by selected store.
- Fetch locker boxes.
- Search/filter stores.
- Favorite store/locker local state.
- Select available box and navigate to `/user/create-order`.
- Report locker problems.

## Create order

File: `app/user/create-order.tsx`

Main behavior:

- Receives `storeId`, `storeName`, `lockerId`, `lockerName`, `boxId`, `boxNumber`.
- Loads service list by category through `serviceService`.
- Category tabs: `LAUNDRY`, `STORAGE`.
- Select one or more service IDs.
- Calculates estimated total.
- Continues to `/user/confirm-order`.

## Confirm order and payment

File: `app/user/confirm-order.tsx`

Main behavior:

- Shows chosen locker/box/services.
- Supports customer note, estimated weight, receiver info for storage, intended receive time.
- Supports promotion/voucher/loyalty selection.
- Creates order with `orderService.createOrder`.
- Best-effort confirms order with `orderService.confirmOrder`.
- Creates payment with `paymentService.createPayment`.
- Shows MoMo QR/link and polls order status.

Important backend mapping adjustment:

- Old mobile sends `boxId`.
- Current backend DTO expects `sendBoxId` and optionally `boxIds`.
- Flutter merge maps selected box to `sendBoxId` and `boxIds: [sendBoxId]`.

## Orders

File: `app/user/(tabs)/orders.tsx`

Main behavior:

- Fetches order history with `orderService.getOrders()`.
- Filter tabs: all, initialized, waiting, processing, completed.
- Statuses include `INITIALIZED`, `WAITING`, `COLLECTED`, `PROCESSING`, `READY`, `RETURNED`, `COMPLETED`, `CANCELED`.
- Supports pay/cancel/track/rating/complaint/refund related actions.

## Notifications

File: `app/user/(tabs)/notifications.tsx`

Main behavior:

- Fetch all/unread notifications.
- Mark single/all as read.
- Delete single/all notifications.

## Profile

File: `app/user/(tabs)/profile.tsx`

Main behavior:

- Fetch/update profile.
- Update avatar.
- Change password.
- Fetch user statistics.
- Logout.
- Shows order/voucher/loyalty shortcuts.

## USER service layer

Relevant files under `services/user`:

- `authService.ts`
- `userService.ts`
- `orderService.ts`
- `lockerService.ts`
- `paymentService.ts`
- `notificationService.ts`
- `storeService.ts`
- `serviceService.ts`
- `promotionService.ts`
- `loyaltyService.ts`

These service contracts were used as the source for the Flutter USER gateway wrappers.
