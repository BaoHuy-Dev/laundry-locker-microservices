# Feature Parity Report

Generated from local scan of:

- Monolith: `D:\BigProject\laundry-locker-backend`
- Microservices: `D:\BigProject\laundry-locker-microservices`

This report tracks feature parity with the existing monolith only. `PARTIAL_SOURCE_TODO` means the monolith itself already had TODO/mock/baseline behavior and the microservice keeps the same maturity level.

## Summary

- Monolith controller endpoints scanned: 209
- Microservice controller endpoints scanned: 265
- Missing monolith endpoints in microservices: 0
- Monolith service classes scanned: 44

## API Parity

| Monolith API | Old controller/use case | Target service | Gateway endpoint | Status |
|---|---|---|---|---|
| `GET /api/admin/audit-logs/entity/{entityType}/{entityId}` | `admin/AdminAuditLogController.java#getEntityAuditLogs` | `user-service` | `/api/admin/audit-logs/entity/{entityType}/{entityId}` | MIGRATED |
| `GET /api/admin/audit-logs/statistics` | `admin/AdminAuditLogController.java#getActionStatistics` | `user-service` | `/api/admin/audit-logs/statistics` | MIGRATED |
| `GET /api/admin/audit-logs/user/{userId}` | `admin/AdminAuditLogController.java#getUserAuditLogs` | `user-service` | `/api/admin/audit-logs/user/{userId}` | MIGRATED |
| `GET /api/admin/audit-logs` | `admin/AdminAuditLogController.java#getAuditLogs` | `user-service` | `/api/admin/audit-logs` | MIGRATED |
| `POST /api/admin/auth/login` | `admin/AdminAuthController.java#login` | `auth-service` | `/api/admin/auth/login` | MIGRATED |
| `POST /api/admin/auth/refresh` | `admin/AdminAuthController.java#refreshToken` | `auth-service` | `/api/admin/auth/refresh` | MIGRATED |
| `POST /api/admin/auth/verify-2fa` | `admin/AdminAuthController.java#verify2fa` | `auth-service` | `/api/admin/auth/verify-2fa` | MIGRATED |
| `GET /api/admin/dashboard/overview` | `admin/DashboardController.java#getOverview` | `order-service` | `/api/admin/dashboard/overview` | MIGRATED |
| `GET /api/admin/hello` | `user/AdminController.java#helloAdmin` | `user-service` | `/api/admin/hello` | MIGRATED |
| `POST /api/admin/lockers/{id}/boxes` | `admin/AdminLockerController.java#addBox` | `locker-service` | `/api/admin/lockers/{id}/boxes` | MIGRATED |
| `PUT /api/admin/lockers/{id}/maintenance` | `admin/AdminLockerController.java#setMaintenance` | `locker-service` | `/api/admin/lockers/{id}/maintenance` | MIGRATED |
| `DELETE /api/admin/lockers/{id}` | `admin/AdminLockerController.java#deleteLocker` | `locker-service` | `/api/admin/lockers/{id}` | MIGRATED |
| `GET /api/admin/lockers/{id}` | `admin/AdminLockerController.java#getLockerById` | `locker-service` | `/api/admin/lockers/{id}` | MIGRATED |
| `PUT /api/admin/lockers/{id}` | `admin/AdminLockerController.java#updateLocker` | `locker-service` | `/api/admin/lockers/{id}` | MIGRATED |
| `PUT /api/admin/lockers/boxes/{boxId}/status` | `admin/AdminLockerController.java#updateBoxStatus` | `locker-service` | `/api/admin/lockers/boxes/{boxId}/status` | MIGRATED |
| `PUT /api/admin/lockers/reports/{id}/resolve` | `admin/AdminLockerController.java#resolveReport` | `locker-service` | `/api/admin/lockers/reports/{id}/resolve` | MIGRATED |
| `GET /api/admin/lockers/reports` | `admin/AdminLockerController.java#getAllReports` | `locker-service` | `/api/admin/lockers/reports` | MIGRATED |
| `GET /api/admin/lockers/store/{storeId}` | `admin/AdminLockerController.java#getLockersByStore` | `locker-service` | `/api/admin/lockers/store/{storeId}` | MIGRATED |
| `GET /api/admin/lockers` | `admin/AdminLockerController.java#getAllLockers` | `locker-service` | `/api/admin/lockers` | MIGRATED |
| `POST /api/admin/lockers` | `admin/AdminLockerController.java#createLocker` | `locker-service` | `/api/admin/lockers` | MIGRATED |
| `GET /api/admin/loyalty/statistics` | `admin/AdminLoyaltyController.java#getLoyaltyStatistics` | `loyalty-service` | `/api/admin/loyalty/statistics` | MIGRATED |
| `GET /api/admin/loyalty/users/{userId}/history` | `admin/AdminLoyaltyController.java#getUserPointsHistory` | `loyalty-service` | `/api/admin/loyalty/users/{userId}/history` | MIGRATED |
| `POST /api/admin/loyalty/users/{userId}/points` | `admin/AdminLoyaltyController.java#adjustUserPoints` | `loyalty-service` | `/api/admin/loyalty/users/{userId}/points` | MIGRATED |
| `GET /api/admin/loyalty/users/{userId}` | `admin/AdminLoyaltyController.java#getUserLoyaltySummary` | `loyalty-service` | `/api/admin/loyalty/users/{userId}` | MIGRATED |
| `POST /api/admin/notifications/broadcast` | `admin/AdminNotificationController.java#broadcastToAll` | `notification-service` | `/api/admin/notifications/broadcast` | MIGRATED |
| `POST /api/admin/notifications/send` | `admin/AdminNotificationController.java#sendToUser` | `notification-service` | `/api/admin/notifications/send` | MIGRATED |
| `GET /api/admin/notifications` | `admin/AdminNotificationController.java#getAllNotifications` | `notification-service` | `/api/admin/notifications` | MIGRATED |
| `PUT /api/admin/orders/{id}/status` | `admin/AdminOrderController.java#updateOrderStatus` | `order-service` | `/api/admin/orders/{id}/status` | MIGRATED |
| `GET /api/admin/orders/{id}` | `admin/AdminOrderController.java#getOrderById` | `order-service` | `/api/admin/orders/{id}` | MIGRATED |
| `GET /api/admin/orders/revenue` | `admin/AdminOrderController.java#getRevenueReport` | `order-service` | `/api/admin/orders/revenue` | MIGRATED |
| `GET /api/admin/orders/statistics` | `admin/AdminOrderController.java#getOrderStatistics` | `order-service` | `/api/admin/orders/statistics` | MIGRATED |
| `GET /api/admin/orders` | `admin/AdminOrderController.java#getAllOrders` | `order-service` | `/api/admin/orders` | MIGRATED |
| `POST /api/admin/partners/{partnerId}/approve` | `admin/AdminPartnerController.java#approvePartner` | `partner-service` | `/api/admin/partners/{partnerId}/approve` | MIGRATED |
| `POST /api/admin/partners/{partnerId}/reject` | `admin/AdminPartnerController.java#rejectPartner` | `partner-service` | `/api/admin/partners/{partnerId}/reject` | MIGRATED |
| `POST /api/admin/partners/{partnerId}/suspend` | `admin/AdminPartnerController.java#suspendPartner` | `partner-service` | `/api/admin/partners/{partnerId}/suspend` | MIGRATED |
| `GET /api/admin/partners/{partnerId}` | `admin/AdminPartnerController.java#getPartnerById` | `partner-service` | `/api/admin/partners/{partnerId}` | MIGRATED |
| `GET /api/admin/partners` | `admin/AdminPartnerController.java#getAllPartners` | `partner-service` | `/api/admin/partners` | MIGRATED |
| `PUT /api/admin/payments/{paymentId}/status` | `admin/AdminPaymentController.java#updatePaymentStatus` | `payment-service` | `/api/admin/payments/{paymentId}/status` | MIGRATED |
| `GET /api/admin/payments/{paymentId}` | `admin/AdminPaymentController.java#getPaymentById` | `payment-service` | `/api/admin/payments/{paymentId}` | MIGRATED |
| `GET /api/admin/payments` | `admin/AdminPaymentController.java#getAllPayments` | `payment-service` | `/api/admin/payments` | MIGRATED |
| `DELETE /api/admin/promotions/{promotionId}` | `admin/AdminPromotionController.java#deletePromotion` | `order-service` | `/api/admin/promotions/{promotionId}` | MIGRATED |
| `GET /api/admin/promotions/{promotionId}` | `admin/AdminPromotionController.java#getPromotion` | `order-service` | `/api/admin/promotions/{promotionId}` | MIGRATED |
| `PUT /api/admin/promotions/{promotionId}` | `admin/AdminPromotionController.java#updatePromotion` | `order-service` | `/api/admin/promotions/{promotionId}` | MIGRATED |
| `GET /api/admin/promotions/active` | `admin/AdminPromotionController.java#getActivePromotions` | `order-service` | `/api/admin/promotions/active` | MIGRATED |
| `GET /api/admin/promotions/search` | `admin/AdminPromotionController.java#searchPromotions` | `order-service` | `/api/admin/promotions/search` | MIGRATED |
| `GET /api/admin/promotions/status/{status}` | `admin/AdminPromotionController.java#getPromotionsByStatus` | `order-service` | `/api/admin/promotions/status/{status}` | MIGRATED |
| `GET /api/admin/promotions/validate/{code}` | `admin/AdminPromotionController.java#validatePromotionCode` | `order-service` | `/api/admin/promotions/validate/{code}` | MIGRATED |
| `GET /api/admin/promotions` | `admin/AdminPromotionController.java#getPromotions` | `order-service` | `/api/admin/promotions` | MIGRATED |
| `POST /api/admin/promotions` | `admin/AdminPromotionController.java#createPromotion` | `order-service` | `/api/admin/promotions` | MIGRATED |
| `POST /api/admin/scheduler/auto-cancel` | `admin/AdminSchedulerController.java#triggerAutoCancelJob` | `order-service` | `/api/admin/scheduler/auto-cancel` | MIGRATED |
| `POST /api/admin/scheduler/pickup-reminders` | `admin/AdminSchedulerController.java#triggerPickupReminderJob` | `order-service` | `/api/admin/scheduler/pickup-reminders` | MIGRATED |
| `POST /api/admin/scheduler/release-boxes` | `admin/AdminSchedulerController.java#triggerBoxReleaseJob` | `order-service` | `/api/admin/scheduler/release-boxes` | MIGRATED |
| `GET /api/admin/scheduler/status` | `admin/AdminSchedulerController.java#getSchedulerStatus` | `order-service` | `/api/admin/scheduler/status` | MIGRATED |
| `PUT /api/admin/services/{id}/image` | `admin/AdminServiceController.java#updateServiceImage` | `laundry-service` | `/api/admin/services/{id}/image` | MIGRATED |
| `PUT /api/admin/services/{id}/price` | `admin/AdminServiceController.java#updatePrice` | `laundry-service` | `/api/admin/services/{id}/price` | MIGRATED |
| `PUT /api/admin/services/{id}/status` | `admin/AdminServiceController.java#updateStatus` | `laundry-service` | `/api/admin/services/{id}/status` | MIGRATED |
| `DELETE /api/admin/services/{id}` | `admin/AdminServiceController.java#deleteService` | `laundry-service` | `/api/admin/services/{id}` | MIGRATED |
| `GET /api/admin/services/{id}` | `admin/AdminServiceController.java#getServiceById` | `laundry-service` | `/api/admin/services/{id}` | MIGRATED |
| `PUT /api/admin/services/{id}` | `admin/AdminServiceController.java#updateService` | `laundry-service` | `/api/admin/services/{id}` | MIGRATED |
| `GET /api/admin/services` | `admin/AdminServiceController.java#getAllServices` | `laundry-service` | `/api/admin/services` | MIGRATED |
| `POST /api/admin/services` | `admin/AdminServiceController.java#createService` | `laundry-service` | `/api/admin/services` | MIGRATED |
| `PUT /api/admin/stores/{id}/image` | `admin/AdminStoreController.java#updateStoreImage` | `store-service` | `/api/admin/stores/{id}/image` | MIGRATED |
| `PUT /api/admin/stores/{id}/status` | `admin/AdminStoreController.java#updateStoreStatus` | `store-service` | `/api/admin/stores/{id}/status` | MIGRATED |
| `DELETE /api/admin/stores/{id}` | `admin/AdminStoreController.java#deleteStore` | `store-service` | `/api/admin/stores/{id}` | MIGRATED |
| `GET /api/admin/stores/{id}` | `admin/AdminStoreController.java#getStoreById` | `store-service` | `/api/admin/stores/{id}` | MIGRATED |
| `PUT /api/admin/stores/{id}` | `admin/AdminStoreController.java#updateStore` | `store-service` | `/api/admin/stores/{id}` | MIGRATED |
| `GET /api/admin/stores` | `admin/AdminStoreController.java#getAllStores` | `store-service` | `/api/admin/stores` | MIGRATED |
| `POST /api/admin/stores` | `admin/AdminStoreController.java#createStore` | `store-service` | `/api/admin/stores` | MIGRATED |
| `GET /api/admin/system/health` | `admin/AdminSystemController.java#getSystemHealth` | `api-gateway` | `/api/admin/system/health` | MIGRATED |
| `PUT /api/admin/users/{id}/roles` | `admin/AdminUserController.java#updateUserRoles` | `user-service` | `/api/admin/users/{id}/roles` | MIGRATED |
| `PUT /api/admin/users/{id}/status` | `admin/AdminUserController.java#updateUserStatus` | `user-service` | `/api/admin/users/{id}/status` | MIGRATED |
| `DELETE /api/admin/users/{id}` | `admin/AdminUserController.java#deleteUser` | `user-service` | `/api/admin/users/{id}` | MIGRATED |
| `GET /api/admin/users/{id}` | `admin/AdminUserController.java#getUserById` | `user-service` | `/api/admin/users/{id}` | MIGRATED |
| `PUT /api/admin/users/{id}` | `admin/AdminUserController.java#updateUser` | `user-service` | `/api/admin/users/{id}` | MIGRATED |
| `GET /api/admin/users` | `admin/AdminUserController.java#getAllUsers` | `user-service` | `/api/admin/users` | MIGRATED |
| `POST /api/admin/users` | `admin/AdminUserController.java#createUser` | `user-service` | `/api/admin/users` | MIGRATED |
| `POST /api/auth/complete-registration` | `auth/AuthController.java#completeRegistration` | `auth-service` | `/api/auth/complete-registration` | MIGRATED |
| `POST /api/auth/email/complete-registration` | `auth/AuthController.java#emailCompleteRegistration` | `auth-service` | `/api/auth/email/complete-registration` | MIGRATED |
| `POST /api/auth/email/send-otp` | `auth/AuthController.java#sendEmailOtp` | `auth-service` | `/api/auth/email/send-otp` | MIGRATED |
| `POST /api/auth/email/verify-otp` | `auth/AuthController.java#verifyEmailOtp` | `auth-service` | `/api/auth/email/verify-otp` | MIGRATED |
| `POST /api/auth/forgot-password` | `auth/AuthController.java#forgotPassword` | `auth-service` | `/api/auth/forgot-password` | MIGRATED |
| `POST /api/auth/kiosk/quick-register` | `auth/AuthController.java#kioskQuickRegister` | `auth-service` | `/api/auth/kiosk/quick-register` | MIGRATED |
| `POST /api/auth/logout` | `auth/AuthController.java#logout` | `auth-service` | `/api/auth/logout` | MIGRATED |
| `POST /api/auth/phone-login` | `auth/AuthController.java#phoneLogin` | `auth-service` | `/api/auth/phone-login` | MIGRATED |
| `POST /api/auth/refresh-token` | `auth/AuthController.java#refreshToken` | `auth-service` | `/api/auth/refresh-token` | MIGRATED |
| `POST /api/auth/reset-password` | `auth/AuthController.java#resetPassword` | `auth-service` | `/api/auth/reset-password` | MIGRATED |
| `POST /api/iot/box-status` | `iot/IoTController.java#updateBoxStatus` | `iot-service` | `/api/iot/box-status` | MIGRATED |
| `POST /api/iot/pickup` | `iot/IoTController.java#confirmPickup` | `iot-service` | `/api/iot/pickup` | MIGRATED |
| `POST /api/iot/unlock-with-code` | `iot/IoTController.java#unlockWithCode` | `iot-service` | `/api/iot/unlock-with-code` | MIGRATED |
| `POST /api/iot/unlock` | `iot/IoTController.java#unlockBox` | `iot-service` | `/api/iot/unlock` | MIGRATED |
| `POST /api/iot/verify-pin` | `iot/IoTController.java#verifyPin` | `iot-service` | `/api/iot/verify-pin` | MIGRATED |
| `GET /api/lockers/{id}/boxes/available` | `locker/LockerController.java#getAvailableBoxes` | `locker-service` | `/api/lockers/{id}/boxes/available` | MIGRATED |
| `GET /api/lockers/{id}/boxes` | `locker/LockerController.java#getBoxesByLocker` | `locker-service` | `/api/lockers/{id}/boxes` | MIGRATED |
| `POST /api/lockers/{id}/report` | `locker/LockerController.java#reportLocker` | `locker-service` | `/api/lockers/{id}/report` | MIGRATED |
| `GET /api/lockers/{id}` | `locker/LockerController.java#getLockerById` | `locker-service` | `/api/lockers/{id}` | MIGRATED |
| `GET /api/lockers/my-reports` | `locker/LockerController.java#getMyReports` | `locker-service` | `/api/lockers/my-reports` | MIGRATED |
| `GET /api/lockers` | `locker/LockerController.java#getAllLockers` | `locker-service` | `/api/lockers` | MIGRATED |
| `GET /api/lockers` | `locker/LockerController.java#getLockersByStore` | `locker-service` | `/api/lockers` | MIGRATED |
| `GET /api/loyalty/points/expiring` | `loyalty/LoyaltyController.java#getExpiringPoints` | `loyalty-service` | `/api/loyalty/points/expiring` | MIGRATED |
| `GET /api/loyalty/points/history` | `loyalty/LoyaltyController.java#getPointsHistory` | `loyalty-service` | `/api/loyalty/points/history` | MIGRATED |
| `GET /api/loyalty/points` | `loyalty/LoyaltyController.java#getPointsAccount` | `loyalty-service` | `/api/loyalty/points` | MIGRATED |
| `POST /api/loyalty/redeem-points` | `loyalty/LoyaltyController.java#redeemPoints` | `loyalty-service` | `/api/loyalty/redeem-points` | MIGRATED |
| `POST /api/loyalty/redeem-stamp` | `loyalty/LoyaltyController.java#redeemStampReward` | `loyalty-service` | `/api/loyalty/redeem-stamp` | MIGRATED |
| `POST /api/loyalty/rewards/{rewardId}/redeem` | `loyalty/LoyaltyController.java#redeemReward` | `loyalty-service` | `/api/loyalty/rewards/{rewardId}/redeem` | MIGRATED |
| `GET /api/loyalty/rewards` | `loyalty/LoyaltyController.java#getAvailableRewards` | `loyalty-service` | `/api/loyalty/rewards` | MIGRATED |
| `GET /api/loyalty/stamps/{stampCardId}` | `loyalty/LoyaltyController.java#getStampCard` | `loyalty-service` | `/api/loyalty/stamps/{stampCardId}` | MIGRATED |
| `GET /api/loyalty/stamps` | `loyalty/LoyaltyController.java#getStampCards` | `loyalty-service` | `/api/loyalty/stamps` | MIGRATED |
| `GET /api/loyalty/summary` | `loyalty/LoyaltyController.java#getLoyaltySummary` | `loyalty-service` | `/api/loyalty/summary` | MIGRATED |
| `PUT /api/notifications/{id}/read` | `notification/NotificationController.java#markAsRead` | `notification-service` | `/api/notifications/{id}/read` | MIGRATED |
| `DELETE /api/notifications/{id}` | `notification/NotificationController.java#deleteNotification` | `notification-service` | `/api/notifications/{id}` | MIGRATED |
| `DELETE /api/notifications/all` | `notification/NotificationController.java#deleteAllNotifications` | `notification-service` | `/api/notifications/all` | MIGRATED |
| `GET /api/notifications/all` | `notification/NotificationController.java#getAllNotifications` | `notification-service` | `/api/notifications/all` | MIGRATED |
| `PUT /api/notifications/read-all` | `notification/NotificationController.java#markAllAsRead` | `notification-service` | `/api/notifications/read-all` | MIGRATED |
| `PUT /api/notifications/read-batch` | `notification/NotificationController.java#markBatchAsRead` | `notification-service` | `/api/notifications/read-batch` | MIGRATED |
| `GET /api/notifications/unread/count` | `notification/NotificationController.java#getUnreadCount` | `notification-service` | `/api/notifications/unread/count` | MIGRATED |
| `GET /api/notifications/unread` | `notification/NotificationController.java#getUnreadNotifications` | `notification-service` | `/api/notifications/unread` | MIGRATED |
| `GET /api/notifications` | `notification/NotificationController.java#getNotifications` | `notification-service` | `/api/notifications` | MIGRATED |
| `PUT /api/orders/{orderId}/cancel` | `order/OrderController.java#cancelOrder` | `order-service` | `/api/orders/{orderId}/cancel` | MIGRATED |
| `POST /api/orders/{orderId}/checkout` | `order/OrderController.java#checkoutOrder` | `order-service` | `/api/orders/{orderId}/checkout` | MIGRATED |
| `PUT /api/orders/{orderId}/collect` | `order/OrderController.java#collectOrder` | `order-service` | `/api/orders/{orderId}/collect` | MIGRATED |
| `POST /api/orders/{orderId}/complaint` | `order/OrderController.java#createComplaint` | `order-service` | `/api/orders/{orderId}/complaint` | MIGRATED |
| `GET /api/orders/{orderId}/complaints` | `order/OrderController.java#getOrderComplaints` | `order-service` | `/api/orders/{orderId}/complaints` | MIGRATED |
| `PUT /api/orders/{orderId}/complete` | `order/OrderController.java#completeOrder` | `order-service` | `/api/orders/{orderId}/complete` | MIGRATED |
| `PUT /api/orders/{orderId}/confirm` | `order/OrderController.java#confirmOrder` | `order-service` | `/api/orders/{orderId}/confirm` | MIGRATED |
| `POST /api/orders/{orderId}/pickup-storage` | `order/OrderController.java#pickupStorageOrder` | `order-service` | `/api/orders/{orderId}/pickup-storage` | MIGRATED |
| `PUT /api/orders/{orderId}/process` | `order/OrderController.java#processOrder` | `order-service` | `/api/orders/{orderId}/process` | MIGRATED |
| `POST /api/orders/{orderId}/rate` | `order/OrderController.java#createComplaint` | `order-service` | `/api/orders/{orderId}/rate` | MIGRATED |
| `GET /api/orders/{orderId}/rating` | `order/OrderController.java#createComplaint` | `order-service` | `/api/orders/{orderId}/rating` | MIGRATED |
| `PUT /api/orders/{orderId}/ready` | `order/OrderController.java#markOrderReady` | `order-service` | `/api/orders/{orderId}/ready` | MIGRATED |
| `POST /api/orders/{orderId}/reorder` | `order/OrderController.java#reorderFromExisting` | `order-service` | `/api/orders/{orderId}/reorder` | MIGRATED |
| `POST /api/orders/{orderId}/reset-pin` | `order/OrderController.java#resetOrderPin` | `order-service` | `/api/orders/{orderId}/reset-pin` | MIGRATED |
| `PUT /api/orders/{orderId}/return` | `order/OrderController.java#returnOrder` | `order-service` | `/api/orders/{orderId}/return` | MIGRATED |
| `GET /api/orders/{orderId}/status` | `order/OrderController.java#getOrderStatus` | `order-service` | `/api/orders/{orderId}/status` | MIGRATED |
| `GET /api/orders/{orderId}/timeline` | `order/OrderController.java#createComplaint` | `order-service` | `/api/orders/{orderId}/timeline` | MIGRATED |
| `PUT /api/orders/{orderId}/weight` | `order/OrderController.java#updateOrderWeight` | `order-service` | `/api/orders/{orderId}/weight` | MIGRATED |
| `GET /api/orders/{orderId}` | `order/OrderController.java#getOrderById` | `order-service` | `/api/orders/{orderId}` | MIGRATED |
| `GET /api/orders/code/{orderCode}` | `order/OrderController.java#getOrderByCode` | `order-service` | `/api/orders/code/{orderCode}` | MIGRATED |
| `GET /api/orders/my-complaints` | `order/OrderController.java#getMyComplaints` | `order-service` | `/api/orders/my-complaints` | MIGRATED |
| `GET /api/orders/my-orders` | `order/OrderController.java#getMyOrders` | `order-service` | `/api/orders/my-orders` | MIGRATED |
| `GET /api/orders/my-ratings` | `order/OrderController.java#createComplaint` | `order-service` | `/api/orders/my-ratings` | MIGRATED |
| `GET /api/orders/pin/{pinCode}` | `order/OrderController.java#getOrderByPinCode` | `order-service` | `/api/orders/pin/{pinCode}` | MIGRATED |
| `GET /api/orders` | `order/OrderController.java#getOrders` | `order-service` | `/api/orders` | MIGRATED |
| `POST /api/orders` | `order/OrderController.java#createOrder` | `order-service` | `/api/orders` | MIGRATED |
| `POST /api/partner/access-codes/{codeId}/cancel` | `partner/PartnerController.java#cancelAccessCode` | `partner-service` | `/api/partner/access-codes/{codeId}/cancel` | MIGRATED |
| `POST /api/partner/access-codes/generate` | `partner/PartnerController.java#generateAccessCode` | `partner-service` | `/api/partner/access-codes/generate` | MIGRATED |
| `GET /api/partner/access-codes/order/{orderId}` | `partner/PartnerController.java#getAccessCodesByOrder` | `partner-service` | `/api/partner/access-codes/order/{orderId}` | MIGRATED |
| `GET /api/partner/access-codes` | `partner/PartnerController.java#getAccessCodes` | `partner-service` | `/api/partner/access-codes` | MIGRATED |
| `GET /api/partner/dashboard` | `partner/PartnerController.java#getPartnerDashboard` | `partner-service` | `/api/partner/dashboard` | MIGRATED |
| `GET /api/partner/lockers/{lockerId}/boxes/available` | `partner/PartnerController.java#getAvailableBoxesByLocker` | `partner-service` | `/api/partner/lockers/{lockerId}/boxes/available` | MIGRATED |
| `GET /api/partner/lockers` | `partner/PartnerController.java#getPartnerLockers` | `partner-service` | `/api/partner/lockers` | MIGRATED |
| `POST /api/partner/orders/{orderId}/accept` | `partner/PartnerController.java#acceptOrder` | `partner-service` | `/api/partner/orders/{orderId}/accept` | MIGRATED |
| `POST /api/partner/orders/{orderId}/collect` | `partner/PartnerController.java#forceCollectOrder` | `partner-service` | `/api/partner/orders/{orderId}/collect` | MIGRATED |
| `POST /api/partner/orders/{orderId}/process` | `partner/PartnerController.java#processOrder` | `partner-service` | `/api/partner/orders/{orderId}/process` | MIGRATED |
| `POST /api/partner/orders/{orderId}/ready` | `partner/PartnerController.java#markOrderReady` | `partner-service` | `/api/partner/orders/{orderId}/ready` | MIGRATED |
| `PUT /api/partner/orders/{orderId}/weight` | `partner/PartnerController.java#updateOrderWeight` | `partner-service` | `/api/partner/orders/{orderId}/weight` | MIGRATED |
| `GET /api/partner/orders/{orderId}` | `partner/PartnerController.java#getOrderDetail` | `partner-service` | `/api/partner/orders/{orderId}` | MIGRATED |
| `GET /api/partner/orders/pending` | `partner/PartnerController.java#getPendingOrders` | `partner-service` | `/api/partner/orders/pending` | MIGRATED |
| `GET /api/partner/orders/statistics` | `partner/PartnerController.java#getOrderStatistics` | `partner-service` | `/api/partner/orders/statistics` | MIGRATED |
| `GET /api/partner/orders` | `partner/PartnerController.java#getPartnerOrders` | `partner-service` | `/api/partner/orders` | MIGRATED |
| `GET /api/partner/revenue` | `partner/PartnerController.java#getPartnerRevenue` | `partner-service` | `/api/partner/revenue` | MIGRATED |
| `DELETE /api/partner/staff/{staffId}` | `partner/PartnerController.java#removeStaffFromPartner` | `partner-service` | `/api/partner/staff/{staffId}` | MIGRATED |
| `POST /api/partner/staff/{staffId}` | `partner/PartnerController.java#addStaffToPartner` | `partner-service` | `/api/partner/staff/{staffId}` | MIGRATED |
| `GET /api/partner/staff` | `partner/PartnerController.java#getPartnerStaff` | `partner-service` | `/api/partner/staff` | MIGRATED |
| `GET /api/partner/stores` | `partner/PartnerController.java#getPartnerStores` | `partner-service` | `/api/partner/stores` | MIGRATED |
| `GET /api/partner` | `partner/PartnerController.java#getMyPartnerProfile` | `partner-service` | `/api/partner` | MIGRATED |
| `POST /api/partner` | `partner/PartnerController.java#registerAsPartner` | `partner-service` | `/api/partner` | MIGRATED |
| `PUT /api/partner` | `partner/PartnerController.java#updatePartnerProfile` | `partner-service` | `/api/partner` | MIGRATED |
| `POST /api/payments/{paymentId}/refund` | `payment/PaymentController.java#requestRefund` | `payment-service` | `/api/payments/{paymentId}/refund` | MIGRATED |
| `GET /api/payments/{paymentId}` | `payment/PaymentController.java#getPaymentById` | `payment-service` | `/api/payments/{paymentId}` | MIGRATED |
| `POST /api/payments/create` | `payment/PaymentController.java#createPayment` | `payment-service` | `/api/payments/create` | MIGRATED |
| `POST /api/payments/momo/callback` | `payment/PaymentController.java#momoCallback` | `payment-service` | `/api/payments/momo/callback` | MIGRATED |
| `GET /api/payments/momo/return` | `payment/PaymentController.java#momoReturn` | `payment-service` | `/api/payments/momo/return` | MIGRATED |
| `GET /api/payments/order/{orderId}/refunds` | `payment/PaymentController.java#getOrderRefunds` | `payment-service` | `/api/payments/order/{orderId}/refunds` | MIGRATED |
| `GET /api/payments/order/{orderId}` | `payment/PaymentController.java#getPaymentsByOrder` | `payment-service` | `/api/payments/order/{orderId}` | MIGRATED |
| `GET /api/payments/refund/{refundId}` | `payment/PaymentController.java#getRefundStatus` | `payment-service` | `/api/payments/refund/{refundId}` | MIGRATED |
| `GET /api/payments/vnpay/ipn` | `payment/PaymentController.java#vnPayIpn` | `payment-service` | `/api/payments/vnpay/ipn` | MIGRATED |
| `GET /api/payments/vnpay/return` | `payment/PaymentController.java#vnPayReturn` | `payment-service` | `/api/payments/vnpay/return` | MIGRATED |
| `GET /api/promotions/active` | `user/UserPromotionController.java#getActivePromotions` | `order-service` | `/api/promotions/active` | MIGRATED |
| `GET /api/promotions/validate/{code}` | `user/UserPromotionController.java#validatePromotionCode` | `order-service` | `/api/promotions/validate/{code}` | MIGRATED |
| `GET /api/services/{id}` | `laundry/ServiceController.java#getServiceById` | `laundry-service` | `/api/services/{id}` | MIGRATED |
| `GET /api/services` | `laundry/ServiceController.java#getAllServices` | `laundry-service` | `/api/services` | MIGRATED |
| `GET /api/services` | `laundry/ServiceController.java#getServicesByStore` | `laundry-service` | `/api/services` | MIGRATED |
| `GET /api/services` | `laundry/ServiceController.java#getServicesByCategory` | `laundry-service` | `/api/services` | MIGRATED |
| `GET /api/services` | `laundry/ServiceController.java#getServicesByStoreAndCategory` | `laundry-service` | `/api/services` | MIGRATED |
| `GET /api/services` | `laundry/ServiceController.java#getServicesByLocker` | `laundry-service` | `/api/services` | MIGRATED |
| `GET /api/services` | `laundry/ServiceController.java#getServicesByLockerAndCategory` | `laundry-service` | `/api/services` | MIGRATED |
| `GET /api/staff/lockers` | `staff/StaffController.java#getLockers` | `staff-service` | `/api/staff/lockers` | MIGRATED |
| `POST /api/staff/orders/{orderId}/assign` | `staff/StaffController.java#assignOrder` | `staff-service` | `/api/staff/orders/{orderId}/assign` | MIGRATED |
| `GET /api/staff/orders/my-assigned` | `staff/StaffController.java#getMyAssignedOrders` | `staff-service` | `/api/staff/orders/my-assigned` | MIGRATED |
| `GET /api/staff/orders/processing` | `staff/StaffController.java#getProcessingOrders` | `staff-service` | `/api/staff/orders/processing` | MIGRATED |
| `GET /api/staff/orders/ready` | `staff/StaffController.java#getReadyOrders` | `staff-service` | `/api/staff/orders/ready` | MIGRATED |
| `GET /api/staff/orders/waiting` | `staff/StaffController.java#getWaitingOrders` | `staff-service` | `/api/staff/orders/waiting` | MIGRATED |
| `GET /api/staff/orders` | `staff/StaffController.java#getOrderSummary` | `staff-service` | `/api/staff/orders` | MIGRATED |
| `POST /api/staff/unlock-box` | `staff/StaffController.java#unlockBox` | `staff-service` | `/api/staff/unlock-box` | MIGRATED |
| `GET /api/stores/{id}` | `store/StoreController.java#getStoreById` | `store-service` | `/api/stores/{id}` | MIGRATED |
| `GET /api/stores/{storeId}/ratings` | `store/StoreController.java#getStoreRatings` | `store-service` | `/api/stores/{storeId}/ratings` | MIGRATED |
| `GET /api/stores/nearby` | `store/StoreController.java#getNearbyStores` | `store-service` | `/api/stores/nearby` | MIGRATED |
| `GET /api/stores` | `store/StoreController.java#getAllStores` | `store-service` | `/api/stores` | MIGRATED |
| `PUT /api/user/avatar` | `user/UserController.java#updateAvatar` | `user-service` | `/api/user/avatar` | MIGRATED |
| `GET /api/user/dashboard` | `user/UserController.java#adminDashboard` | `user-service` | `/api/user/dashboard` | MIGRATED |
| `DELETE /api/user/fcm-token` | `user/UserController.java#removeFcmToken` | `user-service` | `/api/user/fcm-token` | MIGRATED |
| `POST /api/user/fcm-token` | `user/UserController.java#registerFcmToken` | `user-service` | `/api/user/fcm-token` | MIGRATED |
| `GET /api/user/me/statistics` | `user/UserController.java#getUserStatistics` | `user-service` | `/api/user/me/statistics` | MIGRATED |
| `PUT /api/user/password` | `user/UserController.java#changePassword` | `user-service` | `/api/user/password` | MIGRATED |
| `GET /api/user/profile` | `user/UserController.java#getUserProfile` | `user-service` | `/api/user/profile` | MIGRATED |
| `PUT /api/user/profile` | `user/UserController.java#updateProfile` | `user-service` | `/api/user/profile` | MIGRATED |
| `GET /api/user/read` | `user/UserController.java#readResource` | `user-service` | `/api/user/read` | MIGRATED |
| `GET /` | `user/HomeController.java#home` | `user-service` | `/` | MIGRATED |
| `GET /secured` | `user/HomeController.java#secured` | `user-service` | `/secured` | MIGRATED |

## Business Logic Parity

| Old service class | Target service/module | Public methods/use cases scanned | Status | Notes |
|---|---|---|---|---|
| `com/huynqb/laundrylockerbackend/core/email/EmailService.java` | `auth-service` | `sendSimpleEmail`, `sendHtmlEmail` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/core/email/SmtpEmailService.java` | `auth-service` | `sendSimpleEmail`, `sendHtmlEmail` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/core/firebase/FirebaseService.java` | `auth-service + notification-service` | `verifyIdToken`, `extractPhoneNumber` | PARTIAL_EXTERNAL | External provider/device/credential dependent logic is compileable and equivalent in surface; real credentials/protocol remain environment dependent. |
| `com/huynqb/laundrylockerbackend/core/i18n/MessageService.java` | `common-lib` | `get` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/core/scheduler/OrderSchedulerService.java` | `order-service` | `autoCancelUnconfirmedOrders`, `autoReleaseBoxesAfterCompletion`, `sendPickupReminders`, `triggerAutoCancelJob`, `triggerBoxReleaseJob` | PARTIAL_BOUNDARY | Use case is split across gateway/auth/service-owned APIs, Feign, or event boundaries. |
| `com/huynqb/laundrylockerbackend/core/security/oauth2/CustomOAuth2UserService.java` | `auth-service + api-gateway` | `loadUser` | PARTIAL_EXTERNAL | External provider/device/credential dependent logic is compileable and equivalent in surface; real credentials/protocol remain environment dependent. |
| `com/huynqb/laundrylockerbackend/core/security/oauth2/CustomOidcUserService.java` | `auth-service + api-gateway` | `loadUser` | PARTIAL_EXTERNAL | External provider/device/credential dependent logic is compileable and equivalent in surface; real credentials/protocol remain environment dependent. |
| `com/huynqb/laundrylockerbackend/core/security/service/CustomUserDetailsService.java` | `auth-service + api-gateway` | `loadUserByUsername` | PARTIAL_BOUNDARY | Use case is split across gateway/auth/service-owned APIs, Feign, or event boundaries. |
| `com/huynqb/laundrylockerbackend/module/admin/auth/service/AdminAuthService.java` | `auth-service` | `login`, `verify2fa`, `refreshToken` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/admin/service/AdminLockerService.java` | `locker-service` | `getAllLockers`, `getLockersByStore`, `getLockerById`, `getAllReports`, `resolveReport`, `createLocker`, `updateLocker`, `setMaintenance`, `addBox`, `updateBoxStatus`, `deleteLocker` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/admin/service/AdminNotificationService.java` | `notification-service` | `getAllNotifications`, `sendToUser`, `broadcastToAll` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/admin/service/AdminOrderService.java` | `order-service` | `getAllOrders`, `getOrderById`, `updateOrderStatus`, `getOrderStatistics`, `getRevenueReport` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/admin/service/AdminPaymentService.java` | `payment-service` | `getAllPayments`, `getPaymentById`, `updatePaymentStatus` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/admin/service/AdminServiceService.java` | `laundry-service` | `getAllServices`, `getServiceById`, `createService`, `updateService`, `updatePrice`, `updateStatus`, `updateServiceImage`, `deleteService` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/admin/service/AdminStoreService.java` | `store-service` | `getAllStores`, `getStoreById`, `createStore`, `updateStore`, `updateStoreStatus`, `updateStoreImage`, `deleteStore` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/admin/service/AdminUserService.java` | `user-service` | `createUser`, `getAllUsers`, `getUserById`, `updateUser`, `updateUserStatus`, `updateUserRoles`, `deleteUser` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/admin/service/AuditLogService.java` | `user-service` | `logAsync`, `log`, `logFailure`, `getAuditLogs`, `getEntityAuditLogs`, `getUserAuditLogs`, `getAuditLogsByDateRange`, `getActionCounts`, `logLogin`, `logLogout`, `logLoginFailed`, `logOrderCreated`, `logPaymentCompleted` | PARTIAL_BOUNDARY | Use case is split across gateway/auth/service-owned APIs, Feign, or event boundaries. |
| `com/huynqb/laundrylockerbackend/module/admin/service/DashboardService.java` | `order-service` | `getOverview` | PARTIAL_BOUNDARY | Use case is split across gateway/auth/service-owned APIs, Feign, or event boundaries. |
| `com/huynqb/laundrylockerbackend/module/admin/service/PromotionService.java` | `order-service` | `createPromotion`, `updatePromotion`, `getPromotion`, `getPromotions`, `getPromotionsByStatus`, `getActivePromotions`, `validatePromotionCode`, `deletePromotion`, `incrementUsageCount`, `searchPromotions` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/auth/service/AuthService.java` | `auth-service` | `phoneLogin`, `completeRegistration`, `sendEmailOtp`, `verifyEmailOtp`, `emailCompleteRegistration`, `kioskQuickRegister`, `refreshToken`, `logout`, `isTokenBlacklisted`, `sendPasswordResetOtp`, `resetPassword` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/auth/service/EmailOtpService.java` | `auth-service` | `sendOtp`, `verifyOtp`, `hasActiveOtp` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/auth/service/TokenService.java` | `auth-service` | `blacklistAccessToken`, `isAccessTokenBlacklisted`, `saveRefreshToken`, `getUserIdByRefreshToken`, `deleteRefreshToken`, `deleteAllUserRefreshTokens`, `refreshTokenExists`, `saveTempRegistrationToken`, `getIdentifierByTempToken`, `deleteTempToken` | PARTIAL_BOUNDARY | Use case is split across gateway/auth/service-owned APIs, Feign, or event boundaries. |
| `com/huynqb/laundrylockerbackend/module/auth/service/impl/RedisTokenService.java` | `auth-service` | `blacklistAccessToken`, `isAccessTokenBlacklisted`, `saveRefreshToken`, `getUserIdByRefreshToken`, `deleteRefreshToken`, `deleteAllUserRefreshTokens`, `refreshTokenExists`, `saveTempRegistrationToken`, `getIdentifierByTempToken`, `deleteTempToken` | PARTIAL_EXTERNAL | External provider/device/credential dependent logic is compileable and equivalent in surface; real credentials/protocol remain environment dependent. |
| `com/huynqb/laundrylockerbackend/module/iot/service/IoTService.java` | `iot-service` | `verifyPin`, `unlockBox`, `confirmPickup`, `updateBoxStatus` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/iot/service/LockerMqttService.java` | `iot-service` | `sendUnlockCommand`, `sendLockCommand` | PARTIAL_EXTERNAL | External provider/device/credential dependent logic is compileable and equivalent in surface; real credentials/protocol remain environment dependent. |
| `com/huynqb/laundrylockerbackend/module/laundry/model/LaundryService.java` | `laundry-service` | _none_ | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/laundry/service/LaundryServiceService.java` | `laundry-service` | `getAllServices`, `getServicesByStore`, `getServiceById`, `getServicesByCategory`, `getServicesByStoreAndCategory`, `getServicesByLocker`, `getServicesByLockerAndCategory` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/locker/service/LockerService.java` | `locker-service` | `getAllLockers`, `getLockersByStore`, `getLockerById`, `getBoxesByLocker`, `getAvailableBoxes`, `reportLocker`, `getUserReports` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/loyalty/service/LoyaltyService.java` | `loyalty-service` | `getOrCreateAccount`, `getAccountResponse`, `getLoyaltySummary`, `earnPointsFromOrder`, `redeemPoints`, `adjustPoints`, `getPointHistory`, `getPointTransactionHistory`, `getUserStampCards`, `getStampCardResponse`, `redeemStampReward`, `getLoyaltyStatistics`, `getOrCreateStampCard`, `earnStampsFromOrder`, `getStampCards`, `getStampCardsWithRewards`, `getAvailableRewards`, `redeemReward`, `getExpiringPoints`, `calculatePointsForAmount`, `calculateVndForPoints` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/notification/service/FcmPushNotificationService.java` | `notification-service` | `sendToUser`, `sendToToken`, `broadcastToAll` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/notification/service/NotificationService.java` | `notification-service` | `sendOrderStatusNotification`, `sendPaymentNotification`, `sendSystemNotification`, `getNotifications`, `getAllNotifications`, `getUnreadNotifications`, `getUnreadCount`, `markAsRead`, `markAllAsRead`, `deleteNotification`, `deleteAllNotifications`, `markBatchAsRead`, `createNotification` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/notification/service/WebSocketNotificationService.java` | `notification-service` | `sendToUser`, `broadcast`, `sendToDestination` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/order/service/OrderComplaintService.java` | `order-service` | `createComplaint`, `getOrderComplaints`, `getMyComplaints` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/order/service/OrderRatingService.java` | `order-service` | `createRating`, `getRating`, `getMyRatings`, `getStoreRatings`, `getStoreAverageRating`, `respondToRating`, `getOrderTimeline`, `recordStatusChange` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/order/service/OrderService.java` | `order-service` | `createOrder`, `checkoutOrder`, `collectOrder`, `updateOrderWeight`, `returnOrder`, `cancelOrder`, `getOrders`, `getOrderById`, `getOrderStatus`, `getOrderByPinCode`, `getOrderByCode`, `getMyOrders`, `completeOrderByCustomer`, `resetOrderPin`, `reorderFromExisting`, `pickupStorageOrder`, `confirmOrder`, `processOrder`, `markOrderReady`, `applyPromotionCode`, `removePromotion` | PARTIAL_SOURCE_TODO | Source monolith contains TODO/mock/baseline behavior; migrated parity keeps that maturity level. |
| `com/huynqb/laundrylockerbackend/module/partner/service/PartnerService.java` | `partner-service` | `registerPartner`, `getPartnerByUserId`, `getPartnerById`, `getPartnerIdByUserId`, `getPartnerDashboard`, `getPartnerStores`, `getPartnerStaff`, `addStaffToPartner`, `removeStaffFromPartner`, `getAllPartners`, `approvePartner`, `rejectPartner`, `suspendPartner`, `getPendingOrders`, `getPartnerOrders`, `acceptOrderAndGenerateCode`, `forceCollectOrder`, `updateOrderToProcessing`, `markOrderReadyAndGenerateCode`, `getOrderDetail`, `updateOrderWeight`, `updatePartner`, `getPartnerLockers`, `getPartnerLockerAvailableBoxes`, `getPartnerRevenue`, `getPartnerOrderStatistics` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/partner/service/StaffAccessCodeService.java` | `partner-service` | `generateAccessCode`, `unlockWithCode`, `getCodesByOrderId`, `getCodesByPartner`, `cancelAccessCode`, `cleanupExpiredCodes` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/payment/service/MoMoService.java` | `payment-service` | `createPaymentUrl`, `verifySignature`, `isPaymentSuccess`, `extractOrderId`, `getTransactionId` | PARTIAL_EXTERNAL | External provider/device/credential dependent logic is compileable and equivalent in surface; real credentials/protocol remain environment dependent. |
| `com/huynqb/laundrylockerbackend/module/payment/service/PaymentService.java` | `payment-service` | `createPayment`, `processVNPayIpn`, `processVNPayReturn`, `processMoMoCallback`, `getPaymentById`, `getPaymentsByOrder` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/payment/service/RefundService.java` | `payment-service` | `requestRefund`, `processRefund`, `getRefund`, `getRefundsByPayment`, `getRefundsByOrder`, `getPendingRefunds`, `getRefundsByUser`, `getRefundsByStatus` | PARTIAL_SOURCE_TODO | Source monolith contains TODO/mock/baseline behavior; migrated parity keeps that maturity level. |
| `com/huynqb/laundrylockerbackend/module/payment/service/VNPayService.java` | `payment-service` | `createPaymentUrl`, `verifyChecksum`, `processIpnCallback`, `isPaymentSuccess`, `getTxnRef`, `getTransactionNo`, `getResponseMessage` | PARTIAL_EXTERNAL | External provider/device/credential dependent logic is compileable and equivalent in surface; real credentials/protocol remain environment dependent. |
| `com/huynqb/laundrylockerbackend/module/staff/service/StaffService.java` | `staff-service` | `getOrdersByStatus`, `getWaitingOrders`, `getProcessingOrders`, `getReadyOrders`, `getMyAssignedOrders`, `assignOrderToStaff`, `getOrderSummary`, `getAllLockers`, `getLockersByStore`, `unlockBox` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/store/service/StoreService.java` | `store-service` | `getAllStores`, `getStoreById`, `findNearbyStores` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |
| `com/huynqb/laundrylockerbackend/module/user/service/UserService.java` | `user-service` | `updateProfile`, `changePassword`, `registerFcmToken`, `removeFcmToken`, `getUserById`, `getUserStatistics`, `updateAvatar` | MIGRATED | Microservice-safe implementation uses service-owned entities and ID references. |

## Remaining Missing Because Not Migrated

None from controller endpoint scan.

## Preserved Source TODO/Mock/Baseline Items

- `AdminSystemController.checkExternalServices`: monolith returned empty/mock external service checks; gateway health keeps a lightweight equivalent.
- `OrderService.calculatePromotionDiscount`: monolith TODO for service-specific `FREE_SERVICE` discount; order-service preserves the baseline behavior.
- `RefundService.processRefundWithGateway`: monolith simulated gateway refund success; payment-service keeps provider-bound refund behavior at the same maturity level.
- OAuth2/Firebase/VNPay/MoMo/MQTT behaviors remain environment/credential dependent, matching the monolith deployment assumptions rather than adding new production requirements.