# Drone Delivery Phase 1 Design

## Goal

Ship the first production-style slice of drone delivery using one real backend `orderId` end-to-end, replacing the
current fake mobile-only booking flow.

## Scope

Phase 1 covers only the order foundation:

- Create a real `DRONE_DELIVERY` order in `order-service`.
- Reserve a DRONE box through `locker-service` during order creation.
- Return a real backend `orderId` to mobile.
- Expose a customer detail/read endpoint for the drone order.
- Allow cancellation only while the order is still waiting for dispatch.
- Stop mobile from creating fake local `DRN-*` orders.
- Keep MAINTENANCE queue usable through backend-backed data, but do not implement mission/MAVLink/telemetry yet.

Out of scope for this phase:

- Drone mission control, preflight, launch, telemetry, live map.
- Locker deposit automation, parcel-present evidence, READY_FOR_PICKUP.
- Unified FCM/STOMP realtime contract.
- Hardware hardening and production MQTT/device security changes.

## Architecture Decisions

- `LockerOrder` becomes the public business aggregate for drone delivery.
- `order-service` owns create/query/cancel APIs for customer-facing drone delivery.
- `locker-service` still owns DRONE cell reservation and cell lifecycle.
- Existing `DroneDeliveryRequest` remains temporarily for backward compatibility, but new customer mobile flow must stop
  calling it.
- Mobile uses only the backend `orderId` from the new API.

## Phase 1 Backend Contract

### Create

`POST /api/orders/drone-deliveries`

Headers:

- `Authorization: Bearer <access-token>`
- `Idempotency-Key: <client-generated-key>`

Body:

```json
{
  "destinationLockerId": 1,
  "preferredBoxId": 2,
  "description": "Tai lieu can giao",
  "parcelWeightGrams": 1200,
  "paymentMethod": "CASH"
}
```

Behavior:

- `userId` and `receiverUserId` both come from JWT for this phase.
- Validate `destinationLockerId`.
- If `preferredBoxId` is absent, find an available `DRONE` cell.
- Reserve the DRONE box with explicit `channel=DRONE`.
- Persist a `LockerOrder` with type `DRONE_DELIVERY`.
- Persist idempotency info so duplicate retries return the same order.
- Return success only after both DB save and DRONE reservation succeed.

### Read

`GET /api/orders/{orderId}/drone-delivery`

Returns a read model with:

- `orderId`, `orderCode`
- `status`
- `deliveryStage`
- destination locker and reserved box summary
- `paymentStatus`
- customer-visible timeline entries

For Phase 1, `deliveryStage` starts at `PENDING_PAYMENT` or `AWAITING_DISPATCH` depending on payment outcome.

### Cancel

`PUT /api/orders/{orderId}/cancel`

Rules:

- Only owner can cancel.
- Allowed only while drone order is still pre-dispatch.
- Cancel releases the reserved DRONE box.

## Data Model Changes

Additive schema changes on `order_schema.orders`:

- `receiver_user_id`
- `destination_locker_id`
- `reserved_box_id`
- `parcel_weight_grams`
- `delivery_stage`
- `idempotency_key`

Initial order values:

- `type = DRONE_DELIVERY`
- `service_category = DRONE_DELIVERY`
- `status = INITIALIZED`
- `delivery_stage = PENDING_PAYMENT`

For Phase 1, reuse existing rating and completion mechanisms later, but do not force READY_FOR_PICKUP or pickup
automation yet.

## Mobile Changes

- Replace `POST /api/drone-deliveries` booking call with `POST /api/orders/drone-deliveries`.
- Remove fake local tracking bootstrap from booking.
- Wait for backend response before showing success.
- Use returned `orderId` for follow-up navigation.
- On failure, show error instead of pretending booking succeeded.

## Verification Target

Phase 1 is complete when:

- customer booking returns a real backend `orderId`
- double-tap/retry with same `Idempotency-Key` does not create duplicates
- reservation fails cleanly and no fake success appears on mobile
- customer can fetch the new drone-order detail endpoint
- customer can cancel before dispatch and the DRONE box is released
- mobile no longer creates or navigates using fake `DRN-*` ids from the booking sheet
