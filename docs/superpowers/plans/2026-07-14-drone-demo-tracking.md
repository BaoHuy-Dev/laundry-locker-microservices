# Drone Demo Tracking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Advance launched demo drone orders through a backend-owned simulated delivery timeline and expose that
timeline to mobile by `orderId`.

**Architecture:** `order-service` remains the owner of order/mission state. A scheduled simulator advances only DEMO
missions; a query service joins order and mission into the customer read model. Mobile polls that read model and renders
the same stages used by maintenance.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, Flyway, JUnit 5/Mockito, Flutter, Riverpod.

## Global Constraints

- Keep one public `orderId` across customer and maintenance flows.
- Do not expose `/internal/**` through the gateway.
- Do not edit existing Flyway migrations; add the next migration only.
- Do not add new libraries.
- Preserve the pay-before-pickup gate.

---

### Task 1: Backend demo mode and simulator

**Files:**

- Modify: `order-service/src/main/java/com/huynqb/laundrylocker/order/model/LockerOrder.java`
- Modify: `order-service/src/main/java/com/huynqb/laundrylocker/order/model/DroneMission.java`
- Modify: `order-service/src/main/java/com/huynqb/laundrylocker/order/service/DroneOrderMaintenanceService.java`
- Create: `order-service/src/main/java/com/huynqb/laundrylocker/order/service/DroneDeliverySimulator.java`
- Create: `order-service/src/main/resources/db/migration/V9__drone_demo_tracking.sql`
- Test: `order-service/src/test/java/com/huynqb/laundrylocker/order/service/DroneDeliverySimulatorTest.java`

- [ ] Write failing tests for demo preflight bypass, launch registration, stage progression, and final pickup-ready
  state.
- [ ] Run targeted Maven tests and confirm they fail for missing behavior.
- [ ] Add `fulfillmentMode`, mission drone code, repository queries, scheduler, and configuration.
- [ ] Run targeted tests and confirm they pass.

### Task 2: Backend read model and notifications

**Files:**

- Modify: `order-service/src/main/java/com/huynqb/laundrylocker/order/dto/DroneDeliveryOrderResponse.java`
- Create: `order-service/src/main/java/com/huynqb/laundrylocker/order/service/DroneDeliveryQueryService.java`
- Modify: `order-service/src/main/java/com/huynqb/laundrylocker/order/controller/OrderController.java`
- Modify: `order-service/src/main/java/com/huynqb/laundrylocker/order/client/UserClient.java`
- Modify: `user-service/src/main/java/com/huynqb/laundrylocker/user/controller/UserController.java`
- Test: `order-service/src/test/java/com/huynqb/laundrylocker/order/service/DroneDeliveryQueryServiceTest.java`

- [ ] Write failing tests for ownership-safe mission read model and milestone notification selection.
- [ ] Implement query mapping and best-effort owner/MAINTENANCE notifications.
- [ ] Run targeted backend tests.

### Task 3: Mobile timeline polling

**Files:**

- Modify: `mobile/lib/core/config/feature_flags.dart`
- Modify: `mobile/lib/features/drone_delivery/domain/entities/drone_delivery_stage.dart`
- Modify: `mobile/lib/features/drone_delivery/infrastructure/models/drone_delivery_response.dart`
- Modify: `mobile/lib/features/drone_delivery/presentation/providers/drone_delivery_providers.dart`
- Modify: `mobile/lib/features/drone_delivery/presentation/widgets/drone_delivery_timeline.dart`
- Test: `mobile/test/features/drone_delivery/drone_delivery_response_test.dart`

- [ ] Write failing parser/stage tests for all backend stages.
- [ ] Enable backend datasource and add three-second polling.
- [ ] Expand timeline labels and progress ordering.
- [ ] Run Flutter tests, targeted analyze, and debug APK build.

### Task 4: Living documentation

**Files:**

- Modify: `docs/BUSINESS_FLOWS_CURRENT.md`
- Modify: `docs/PROJECT_PROGRESS_TRACKER.md`
- Modify: backend mirror copies of both files
- Modify: `mobile/docs/merge-status.md`

- [ ] Document endpoint shape, simulator timing, notification policy, and remaining hardware gaps.
- [ ] Record exact verification commands and results.
