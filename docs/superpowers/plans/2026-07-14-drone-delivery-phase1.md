# Drone Delivery Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace fake mobile drone booking with a real order-based backend flow that reserves a DRONE box and returns
one real `orderId`.

**Architecture:** `order-service` becomes the customer-facing owner of drone-delivery order creation, querying, and
cancellation. `locker-service` remains the source of truth for DRONE box reservation/release, while mobile stops
creating local fake tracking records and waits for a real backend response.

**Tech Stack:** Spring Boot, Spring MVC, Spring Data JPA, Flyway, OpenFeign, RabbitMQ, Flutter, Dio-style API client
wrappers, SmartDialog/UI flow code.

## Global Constraints

- Use one public backend `orderId` across the customer flow.
- Do not edit old Flyway migrations; add a new migration only.
- Do not expose `/internal/**` through gateway.
- Do not add new libraries.
- Keep Phase 1 limited to order foundation; no mission/MAVLink/telemetry implementation.
- Booking failure must not show success on mobile.

---

### Task 1: Backend Drone Order Foundation

**Files:**

- Create: `order-service/src/main/java/com/huynqb/laundrylocker/order/dto/CreateDroneDeliveryOrderRequest.java`
- Create: `order-service/src/main/java/com/huynqb/laundrylocker/order/dto/DroneDeliveryOrderResponse.java`
- Create: `order-service/src/main/resources/db/migration/V7__drone_delivery_order_foundation.sql`
- Modify: `order-service/src/main/java/com/huynqb/laundrylocker/order/model/LockerOrder.java`
- Modify: `order-service/src/main/java/com/huynqb/laundrylocker/order/client/LockerClient.java`
- Modify: `order-service/src/main/java/com/huynqb/laundrylocker/order/controller/OrderController.java`
- Modify: `order-service/src/main/java/com/huynqb/laundrylocker/order/service/OrderService.java`
- Test: `order-service/src/test/java/com/huynqb/laundrylocker/order/service/OrderServiceDroneDeliveryTest.java`

**Interfaces:**

- Consumes: `LockerClient.reserveBox(...)`, `LockerCellClient.findAvailable(...)`, existing `OrderService.cancel(...)`
- Produces:
    - `OrderService.createDroneDelivery(CreateDroneDeliveryOrderRequest request, Long userId, String idempotencyKey)`
    - `OrderService.getDroneDelivery(Long orderId, Long userId)`
    - `POST /api/orders/drone-deliveries`
    - `GET /api/orders/{orderId}/drone-delivery`

- [ ] **Step 1: Write the failing backend tests**

```java
@Test
void createDroneDeliveryReturnsExistingOrderForSameIdempotencyKey() {}

@Test
void createDroneDeliveryReservesDroneBoxAndPersistsOrder() {}

@Test
void cancelDroneDeliveryReleasesReservedBox() {}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl order-service -Dtest=OrderServiceDroneDeliveryTest test`
Expected: FAIL because the DTOs/service methods/test scaffolding do not exist yet.

- [ ] **Step 3: Add additive schema and model fields**

```sql
ALTER TABLE order_schema.orders ADD COLUMN IF NOT EXISTS receiver_user_id BIGINT;
ALTER TABLE order_schema.orders ADD COLUMN IF NOT EXISTS destination_locker_id BIGINT;
ALTER TABLE order_schema.orders ADD COLUMN IF NOT EXISTS reserved_box_id BIGINT;
ALTER TABLE order_schema.orders ADD COLUMN IF NOT EXISTS parcel_weight_grams INTEGER;
ALTER TABLE order_schema.orders ADD COLUMN IF NOT EXISTS delivery_stage VARCHAR(40);
ALTER TABLE order_schema.orders ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(120);
```

- [ ] **Step 4: Implement minimal backend order flow**

```java
public OrderResponse createDroneDelivery(
    CreateDroneDeliveryOrderRequest request,
    Long userId,
    String idempotencyKey) {
  // lookup existing by userId + idempotencyKey
  // resolve or find DRONE box
  // reserve with channel=DRONE
  // persist LockerOrder type DRONE_DELIVERY
  // return mapped response
}
```

- [ ] **Step 5: Expose customer APIs**

```java
@PostMapping("/api/orders/drone-deliveries")
@GetMapping("/api/orders/{orderId}/drone-delivery")
```

- [ ] **Step 6: Run backend tests**

Run: `mvn -pl order-service -Dtest=OrderServiceDroneDeliveryTest test`
Expected: PASS.

- [ ] **Step 7: Run module compile verification**

Run: `mvn -pl order-service -am compile`
Expected: BUILD SUCCESS.

### Task 2: Mobile Booking Cutover

**Files:**

- Modify: `mobile/lib/features/locker_ops/data/locker_ops_service.dart`
- Modify: `mobile/lib/features/drone_delivery/presentation/widgets/drone_booking_sheet.dart`
- Test: `mobile/test/features/drone_delivery/drone_booking_sheet_test.dart`

**Interfaces:**

- Consumes:
    - `POST /api/orders/drone-deliveries`
    - backend response with real `id`
- Produces:
    - `LockerOpsService.createDroneDeliveryOrder(...)`
    - booking UX that waits for backend response and no longer writes fake `DRN-*`

- [ ] **Step 1: Write the failing mobile test**

```dart
testWidgets('booking waits for backend success before navigating', (tester) async {})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `flutter test test/features/drone_delivery/drone_booking_sheet_test.dart`
Expected: FAIL because the widget still fire-and-forgets and uses local fake state.

- [ ] **Step 3: Replace the API call and remove local fake order creation**

```dart
final response = await LockerOpsService().createDroneDeliveryOrder(...);
final orderId = response['id'];
```

- [ ] **Step 4: Show real failure/success behavior**

```dart
if (orderId == null) {
  // surface error and stop
}
```

- [ ] **Step 5: Run targeted mobile test**

Run: `flutter test test/features/drone_delivery/drone_booking_sheet_test.dart`
Expected: PASS.

- [ ] **Step 6: Run static verification**

Run: `flutter analyze`
Expected: no new errors.

### Task 3: Docs And Flow Sync

**Files:**

- Modify: `docs/PROJECT_PROGRESS_TRACKER.md`
- Modify: `docs/BUSINESS_FLOWS_CURRENT.md`
- Modify: `../mobile/docs/merge-status.md`

**Interfaces:**

- Consumes: implemented backend/mobile Phase 1 contract
- Produces: updated living docs for future work

- [ ] **Step 1: Update tracker current-work line and component status**

```md
- add branch/task entry for Phase 1 drone order foundation
```

- [ ] **Step 2: Update business flows**

```md
- document POST /api/orders/drone-deliveries
- document GET /api/orders/{orderId}/drone-delivery
- document cancellation window and orderId-based mobile flow
```

- [ ] **Step 3: Update mobile merge status**

```md
- note that booking no longer creates fake DRN-* tracking ids
```

- [ ] **Step 4: Run minimal verification after docs sync**

Run: `git status --short`
Expected: only intended files changed.
