# USER API mapping

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

Base URL should point to API Gateway, for example:

- Android emulator: `http://10.0.2.2:8080`
- Windows host/browser: `http://localhost:8080`

Flutter uses `EnvConfig.apiBaseUrl` / `API_BASE_URL` and the new USER services call gateway paths under `/api/...`.

## AuthService

Flutter file: `lib/features/user_laundry/infrastructure/services/user_auth_service.dart`

| Flow | Flutter method | Backend endpoint | Status |
| --- | --- | --- | --- |
| Login | `login(identifier, password)` | `POST /api/auth/login` | Service connected |
| Register direct | `register(...)` | `POST /api/auth/register` | Service connected |
| Send email OTP | `sendEmailOtp(email)` | `POST /api/auth/email/send-otp` | Service connected |
| Verify email OTP | `verifyEmailOtp(email, otp)` | `POST /api/auth/email/verify-otp` | Service connected |
| Complete email registration | `completeEmailRegistration(...)` | `POST /api/auth/email/complete-registration` | Service connected |
| Logout | `logout()` | `POST /api/auth/logout` | Service connected |

Note: existing shared Flutter auth UI has not been switched in this USER-only merge because it is shared across roles.

## UserService

Flutter file: `lib/features/user_laundry/infrastructure/services/user_service.dart`

| Flow | Flutter method | Backend endpoint | Status |
| --- | --- | --- | --- |
| Profile | `getProfile()` | `GET /api/user/profile` | Connected |
| Update profile | `updateProfile(data)` | `PUT /api/user/profile` | Connected |
| Statistics | `getStatistics()` | `GET /api/user/me/statistics` | Connected |
| FCM token | `updateFcmToken(token)` | `POST /api/user/fcm-token` | Connected |
| Change password | `changePassword(...)` | `PUT /api/user/password` | Connected |

## LockerService

Flutter file: `lib/features/user_laundry/infrastructure/services/user_locker_service.dart`

| Flow | Flutter method | Backend endpoint | Status |
| --- | --- | --- | --- |
| List stores | `getStores()` | `GET /api/stores` | Connected |
| Lockers by store | `getLockersByStore(storeId)` | `GET /api/lockers?storeId=` | Connected |
| Boxes | `getBoxes(lockerId)` | `GET /api/lockers/{lockerId}/boxes` | Connected |
| Available boxes | `getAvailableBoxes(lockerId)` | `GET /api/lockers/{lockerId}/boxes/available` | Connected |
| Report locker | `reportLocker(...)` | `POST /api/lockers/{id}/report` | Connected |

## Laundry service catalog

Flutter file: `lib/features/user_laundry/infrastructure/services/user_laundry_service.dart`

| Flow | Flutter method | Backend endpoint | Status |
| --- | --- | --- | --- |
| List services | `getServices(category, storeId)` | `GET /api/services` | TODO backend route/controller not confirmed |

No mock service data was added. The UI blocks order submission when real service data is unavailable.

## OrderService

Flutter file: `lib/features/user_laundry/infrastructure/services/user_order_service.dart`

| Flow | Flutter method | Backend endpoint | Status |
| --- | --- | --- | --- |
| Create order | `createLaundryOrder(...)` | `POST /api/orders` | Connected |
| Confirm order | `confirmOrder(orderId)` | `POST /api/orders/{id}/confirm` | Connected |
| Order status | `getOrderStatus(orderId)` | `GET /api/orders/{id}/status` | Connected |
| Order detail | `getOrder(orderId)` | `GET /api/orders/{id}` | Connected |
| My orders/history | `getMyOrders(...)` | `GET /api/orders/my-orders` | Connected |
| Cancel order | `cancelOrder(orderId)` | `POST /api/orders/{id}/cancel` | Connected |

Create order mapping:

- selected store -> `storeId`
- selected locker -> `lockerId`
- selected box -> `sendBoxId`
- selected box list -> `boxIds: [sendBoxId]`
- selected services -> `serviceIds`
- category -> `type` and `serviceCategory`

## PaymentService

Flutter file: `lib/features/user_laundry/infrastructure/services/user_payment_service.dart`

| Flow | Flutter method | Backend endpoint | Status |
| --- | --- | --- | --- |
| Create payment | `createPayment(...)` | `POST /api/payments` | Connected |
| Payments by order | `getPaymentsByOrder(orderId)` | `GET /api/payments/order/{orderId}` | Connected |

Payment create sends `orderId`, `userId` from token, `amount`, `method`, `language`, and description.

## NotificationService

Flutter file: `lib/features/user_laundry/infrastructure/services/user_notification_service.dart`

| Flow | Flutter method | Backend endpoint | Status |
| --- | --- | --- | --- |
| All notifications | `getNotifications()` | `GET /api/notifications/all` | Connected |
| Unread notifications | `getUnread()` | `GET /api/notifications/unread` | Connected |
| Unread count | `getUnreadCount()` | `GET /api/notifications/unread/count` | Connected |
| Mark read | `markAsRead(id)` | `PATCH /api/notifications/{id}/read` | Connected |
| Mark all read | `markAllAsRead()` | `PATCH /api/notifications/read-all` | Connected |
| Delete | `deleteNotification(id)` | `DELETE /api/notifications/{id}` | Connected |
