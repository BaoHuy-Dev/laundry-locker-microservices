# Migration Mapping

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

Source monolith: `D:\BigProject\laundry-locker-backend`

Target workspace: `D:\BigProject\laundry-locker-microservices`

Status legend:

- `MIGRATED`: implemented in the target service with microservice-safe IDs instead of cross-service JPA relations.
- `PARTIAL_BOUNDARY`: compileable implementation exists, but behavior is split through gateway, Feign, or events.
- `PARTIAL_EXTERNAL`: compileable implementation exists, but provider/device credentials or external protocols are environment dependent.
- `PARTIAL_SOURCE_TODO`: the monolith itself contains TODO/mock/baseline behavior, and the microservice keeps that same maturity level.

Feature parity pass on 2026-05-29:

- Monolith controller endpoints scanned: 209.
- Microservice controller endpoints scanned: 265.
- Missing monolith endpoints in microservices: 0.
- Full API and service/use-case parity table: `PARITY_REPORT.md`.

## Ownership Rules

| Old reference | New storage rule |
|---|---|
| `User` JPA relation | `userId`, `senderId`, `receiverId`, `staffId`, `createdByUserId` |
| `Order` JPA relation | `orderId` |
| `Store` JPA relation | `storeId` |
| `Partner` JPA relation | `partnerId` |
| `Staff` JPA relation | `staffId` |
| `Locker` / `Box` JPA relation | `lockerId`, `boxId`, `sendBoxId`, `receiveBoxId` |
| `LaundryService` JPA relation | `serviceId` |

## Module Mapping Summary

| Old path | Target module | Target package/files | Status |
|---|---|---|---|
| `core/dto/*` | `common-lib` | `common/dto/*` | MIGRATED |
| `core/exception/*` | `common-lib` | `common/exception/*` | MIGRATED |
| `core/constant/UriParamConstants.java` | service-owned controllers/gateway | route constants in controllers and `api-gateway/application.yml` | MIGRATED |
| `core/util/CodeGenerator.java` | `common-lib` | `common/util/CodeGenerator.java` | MIGRATED |
| `core/security/*` | `auth-service`, `api-gateway`, service headers | JWT issuance in auth, route enforcement at gateway, `X-User-*` headers downstream | PARTIAL_BOUNDARY |
| `core/firebase/*` | `auth-service`, `notification-service` | Firebase token verification surface in auth, FCM push in notification | PARTIAL_EXTERNAL |
| `core/email/*` | `auth-service` | SMTP email OTP/reset flow | MIGRATED |
| `core/i18n/MessageService.java` | `common-lib` | `common/i18n/MessageService.java` | MIGRATED |
| `core/scheduler/OrderSchedulerService.java` | `order-service`, `locker-service`, `notification-service` | scheduler endpoints and service-owned actions | PARTIAL_BOUNDARY |
| `core/config/MqttConfig.java` | `iot-service` | MQTT config/client | PARTIAL_EXTERNAL |
| `core/config/WebSocketConfig.java` | `notification-service` | STOMP `/ws`, `/topic`, `/queue`, WebSocket notification sender | MIGRATED |

## Per Module Mapping

| Old module/file group | Target service | Target file/group | Status |
|---|---|---|---|
| `module/auth/controller/AuthController.java` | `auth-service` | `auth/controller/AuthController.java` | MIGRATED |
| `module/auth/dto/request/*` | `auth-service` | `auth/dto/*` | MIGRATED |
| `module/auth/dto/response/*` | `auth-service` | `auth/dto/*` | MIGRATED |
| `module/auth/service/AuthService.java` | `auth-service` | `auth/service/AuthService.java` | MIGRATED |
| `module/auth/service/EmailOtpService.java` | `auth-service` | `auth/service/EmailOtpService.java` plus SMTP sender | MIGRATED |
| `module/auth/service/TokenService.java` / `RedisTokenService.java` | `auth-service` | DB refresh-token/session model, temp tokens; Redis not required for parity | PARTIAL_EXTERNAL |
| `module/auth/mapper/AuthMapper.java` | `auth-service` | manual DTO mapping | MIGRATED |
| `module/user/model/User.java` | `user-service` | `user/model/UserProfile.java` plus role join tables | MIGRATED |
| `module/user/model/Role.java`, `Permission.java` | `user-service` | `Role`, `Permission`, join tables | MIGRATED |
| `module/user/controller/UserController.java` | `user-service` | `user/controller/UserController.java`; FCM delegated to notification-service; password delegated to auth-service | MIGRATED |
| `module/user/controller/UserPromotionController.java` | `order-service` | promotion endpoints under `/api/promotions` | MIGRATED |
| `module/user/service/UserService.java` | `user-service` | `user/service/UserProfileService.java` plus auth/notification Feign adapters | MIGRATED |
| `module/notification/*` | `notification-service` | notification entities, FCM token, WebSocket, internal/public/admin APIs | MIGRATED |
| `module/store/*` | `store-service` | stores, nearby query, ratings facade, admin store APIs | MIGRATED |
| `module/laundry/*` | `laundry-service` | service catalog, estimate, locker-filter facade, admin service APIs | MIGRATED |
| `module/locker/*` | `locker-service` | lockers, boxes, reports, reserve/release/open box APIs | MIGRATED |
| `module/order/model/*` | `order-service` | orders, details, status history, ratings, complaints with ID refs | MIGRATED |
| `module/order/controller/*` | `order-service` | order lifecycle, rating, complaint, admin order, scheduler APIs | MIGRATED |
| `module/order/service/*` | `order-service` | lifecycle/pricing/status history/events; source `FREE_SERVICE` TODO preserved | PARTIAL_SOURCE_TODO |
| `module/payment/*` | `payment-service` | payments, refunds, VNPay/MoMo baseline, admin payment APIs | PARTIAL_EXTERNAL |
| `module/iot/*` | `iot-service` | verify PIN, unlock, pickup, box-status, MQTT command publishing | PARTIAL_EXTERNAL |
| `module/staff/*` | `staff-service` | staff assignments and operational order views through Feign | MIGRATED |
| `module/partner/*` | `partner-service` | partner profile, staff access codes, dashboards, order/locker/store facades | MIGRATED |
| `module/loyalty/*` | `loyalty-service` | points, stamps, rewards, redemption/admin APIs | MIGRATED |
| `module/admin/auth/*` | `auth-service` | `/api/admin/auth/*` | MIGRATED |
| `module/admin/controller/AdminUserController.java` | `user-service` | `/api/admin/users/*` | MIGRATED |
| `module/admin/controller/AdminStoreController.java` | `store-service` | `/api/admin/stores/*` | MIGRATED |
| `module/admin/controller/AdminServiceController.java` | `laundry-service` | `/api/admin/services/*` | MIGRATED |
| `module/admin/controller/AdminLockerController.java` | `locker-service` | `/api/admin/lockers/*` | MIGRATED |
| `module/admin/controller/AdminOrderController.java` | `order-service` | `/api/admin/orders/*` | MIGRATED |
| `module/admin/controller/AdminPaymentController.java` | `payment-service` | `/api/admin/payments/*` | MIGRATED |
| `module/admin/controller/AdminPromotionController.java` | `order-service` | `/api/admin/promotions/*` | MIGRATED |
| `module/admin/controller/AdminNotificationController.java` | `notification-service` | `/api/admin/notifications/*` | MIGRATED |
| `module/admin/controller/AdminLoyaltyController.java` | `loyalty-service` | `/api/admin/loyalty/*` | MIGRATED |
| `module/admin/controller/AdminPartnerController.java` | `partner-service` | `/api/admin/partners/*` | MIGRATED |
| `module/admin/controller/DashboardController.java` | `order-service` | `/api/admin/dashboard/overview` | PARTIAL_BOUNDARY |
| `module/admin/model/AuditLog.java` | `user-service` | audit log table/controller; monolith write helpers were not wired outside audit controller | PARTIAL_BOUNDARY |
| `module/admin/model/Promotion.java`, `PromotionUsage.java` | `order-service` | promotion and promotion usage tables | MIGRATED |

## Entity Conversion Checklist

| Old entity | Target service | Cross-service conversion |
|---|---|---|
| `User` | `user-service` | owns `roles`, `permissions`; other services store user IDs only |
| `Role`, `Permission` | `user-service` | not shared with auth; auth receives roles via Feign |
| `Store` | `store-service` | `partner` becomes `partnerId` |
| `LaundryService` | `laundry-service` | `store` becomes `storeId` |
| `Locker`, `Box`, `LockerReport` | `locker-service` | `store`/`user` become `storeId`/`userId` |
| `Order`, `OrderDetail`, `OrderRating`, `OrderComplaint`, `OrderStatusHistory` | `order-service` | user/box/locker/service/staff refs become IDs |
| `Payment`, `Refund` | `payment-service` | order/customer/processor refs become IDs |
| `Notification`, `FcmToken` | `notification-service` | user ref becomes `userId` |
| `Partner`, `StaffAccessCode` | `partner-service` | user/order refs become IDs |
| `LoyaltyAccount`, `PointTransaction`, `StampCard`, `StampTransaction` | `loyalty-service` | user/order/service refs become IDs |
| `Promotion`, `PromotionUsage` | `order-service` | user/order refs become IDs |
