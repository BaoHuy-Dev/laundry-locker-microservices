# Migration Mapping

Source monolith: `D:\BigProject\laundry-locker-backend`

Target workspace: `D:\BigProject\laundry-locker-microservices`

Status legend:

- `MIGRATED`: implemented in the target service with microservice-safe IDs instead of cross-service JPA relations.
- `PARTIAL`: compileable implementation exists, but behavior is intentionally completed through Feign/events or has documented TODOs.
- `TODO`: mapped owner is known; migration still requires a follow-up implementation pass.

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
| `core/constant/UriParamConstants.java` | service-owned controllers/gateway | route constants in controllers and `api-gateway/application.yml` | PARTIAL |
| `core/util/CodeGenerator.java` | service-owned utilities | auth/order/partner local generators | PARTIAL |
| `core/security/*` | `auth-service`, `api-gateway`, service headers | JWT issuance in auth, route enforcement at gateway, `X-User-*` headers downstream | PARTIAL |
| `core/firebase/*` | `auth-service`, `notification-service` | Firebase token verification TODO in auth, FCM push in notification | PARTIAL |
| `core/email/*` | `auth-service` | SMTP email OTP/reset flow | PARTIAL |
| `core/scheduler/OrderSchedulerService.java` | `order-service`, `locker-service`, `notification-service` | scheduler endpoints/TODO events | PARTIAL |
| `core/config/MqttConfig.java` | `iot-service` | MQTT config/client | PARTIAL |
| `core/config/WebSocketConfig.java` | `notification-service` | WebSocket push TODO | TODO |

## Per Module Mapping

| Old module/file group | Target service | Target file/group | Status |
|---|---|---|---|
| `module/auth/controller/AuthController.java` | `auth-service` | `auth/controller/AuthController.java` | PARTIAL |
| `module/auth/dto/request/*` | `auth-service` | `auth/dto/*` | PARTIAL |
| `module/auth/dto/response/*` | `auth-service` | `auth/dto/*` | PARTIAL |
| `module/auth/service/AuthService.java` | `auth-service` | `auth/service/AuthService.java` | PARTIAL |
| `module/auth/service/EmailOtpService.java` | `auth-service` | `auth/service/EmailOtpService.java` | PARTIAL |
| `module/auth/service/TokenService.java` / `RedisTokenService.java` | `auth-service` | DB refresh-token/session model, Redis TODO | PARTIAL |
| `module/auth/mapper/AuthMapper.java` | `auth-service` | manual DTO mapping | PARTIAL |
| `module/user/model/User.java` | `user-service` | `user/model/UserProfile.java` plus role join tables | PARTIAL |
| `module/user/model/Role.java`, `Permission.java` | `user-service` | `Role`, `Permission`, join tables | PARTIAL |
| `module/user/controller/UserController.java` | `user-service` | `user/controller/UserController.java`; FCM delegated to notification-service | PARTIAL |
| `module/user/controller/UserPromotionController.java` | `order-service` | promotion endpoints under `/api/promotions` | PARTIAL |
| `module/user/service/UserService.java` | `user-service` | `user/service/UserProfileService.java` | PARTIAL |
| `module/notification/*` | `notification-service` | notification entities, FCM token, internal/public/admin APIs | PARTIAL |
| `module/store/*` | `store-service` | stores, nearby query, admin store APIs | PARTIAL |
| `module/laundry/*` | `laundry-service` | service catalog, estimate, admin service APIs | PARTIAL |
| `module/locker/*` | `locker-service` | lockers, boxes, reports, reserve/release/open box APIs | PARTIAL |
| `module/order/model/*` | `order-service` | orders, details, status history, ratings, complaints with ID refs | PARTIAL |
| `module/order/controller/*` | `order-service` | order lifecycle, rating, complaint, admin order APIs | PARTIAL |
| `module/order/service/*` | `order-service` | lifecycle/pricing/overtime/status history/events | PARTIAL |
| `module/payment/*` | `payment-service` | payments, refunds, VNPay/MoMo baseline, admin payment APIs | PARTIAL |
| `module/iot/*` | `iot-service` | verify PIN, unlock, pickup, box-status, MQTT command publishing | PARTIAL |
| `module/staff/*` | `staff-service` | staff assignments and operational order views through Feign | PARTIAL |
| `module/partner/*` | `partner-service` | partner profile, staff access codes, dashboards via Feign/TODO report aggregation | PARTIAL |
| `module/loyalty/*` | `loyalty-service` | points, stamps, rewards, admin loyalty APIs | PARTIAL |
| `module/admin/auth/*` | `auth-service` | `/api/admin/auth/*` | PARTIAL |
| `module/admin/controller/AdminUserController.java` | `user-service` | `/api/admin/users/*` | PARTIAL |
| `module/admin/controller/AdminStoreController.java` | `store-service` | `/api/admin/stores/*` | PARTIAL |
| `module/admin/controller/AdminServiceController.java` | `laundry-service` | `/api/admin/services/*` | PARTIAL |
| `module/admin/controller/AdminLockerController.java` | `locker-service` | `/api/admin/lockers/*` | PARTIAL |
| `module/admin/controller/AdminOrderController.java` | `order-service` | `/api/admin/orders/*` | PARTIAL |
| `module/admin/controller/AdminPaymentController.java` | `payment-service` | `/api/admin/payments/*` | PARTIAL |
| `module/admin/controller/AdminPromotionController.java` | `order-service` | `/api/admin/promotions/*` | PARTIAL |
| `module/admin/controller/AdminNotificationController.java` | `notification-service` | `/api/admin/notifications/*` | PARTIAL |
| `module/admin/controller/AdminLoyaltyController.java` | `loyalty-service` | `/api/admin/loyalty/*` | PARTIAL |
| `module/admin/controller/AdminPartnerController.java` | `partner-service` | `/api/admin/partners/*` | PARTIAL |
| `module/admin/controller/DashboardController.java` | `order-service` plus Feign aggregation TODO | `/api/admin/dashboard/overview` | PARTIAL |
| `module/admin/model/AuditLog.java` | `user-service` | audit log TODO table/controller | TODO |
| `module/admin/model/Promotion.java`, `PromotionUsage.java` | `order-service` | promotion and promotion usage tables | PARTIAL |

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

