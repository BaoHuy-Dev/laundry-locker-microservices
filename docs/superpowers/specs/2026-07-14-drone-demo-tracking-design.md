# Drone Demo Tracking Design

## Goal

Provide a production-shaped demo flow where one real `orderId` progresses from maintenance launch to pickup-ready without MAVLink hardware, while customer and maintenance screens read the same backend state.

## Scope

- Store `fulfillmentMode=DEMO|STANDARD` on each drone order. `APP_DRONE_DEMO_ENABLED` and `APP_DRONE_DEMO_ALLOWED_USER_IDS` control who may create DEMO orders; an omitted mode defaults to DEMO only for an allowed user, otherwise STANDARD.
- A configured demo source locker replaces hardware source discovery. The customer-selected locker remains the destination.
- Demo acceptance still requires an active destination locker but bypasses landing-pad hardware checks.
- After launch, an order-service scheduler advances `LAUNCHING -> DEPARTED -> EN_ROUTE -> APPROACHING -> ARRIVED -> READY_FOR_PICKUP` using a configurable delay.
- At the final stage, the order becomes `STORING`, receives a pickup PIN/deadline, and remains subject to `DRONE_PAYMENT_REQUIRED_BEFORE_PICKUP` until paid.
- `GET /api/orders/{orderId}/drone-delivery` returns current order stage plus mission/drone/source/destination/ETA data.
- Mobile polls the endpoint every three seconds and renders the backend stage in a Grab-like timeline.
- Customer notifications are emitted only for `ACCEPTED`, `DEPARTED`, `APPROACHING`, `ARRIVED`, and `READY_FOR_PICKUP`.
- New-order notification fans out to active `MAINTENANCE` users through an internal user lookup.

## Non-Goals

- Real MAVLink/SITL mission upload or telemetry.
- Live map and coordinate interpolation.
- Real landing-pad, door, or parcel sensors.
- Separate demo-only order or mission tables.
- Removing legacy drone-delivery endpoints.

## Failure Rules

- Scheduler updates are idempotent: only the exact current mission stage may advance.
- Notification failures are best-effort and never roll back mission progression.
- STANDARD mode keeps strict landing-pad validation and does not use the demo scheduler.
- Customer ownership remains mandatory on the read endpoint.

## Verification

- Unit tests cover demo acceptance, launch scheduling, stage transition, final pickup-ready state, read-model mapping, and notification selection.
- Mobile tests cover stage parsing and polling refresh.
- Targeted Maven tests, Flutter tests, analyze, and debug APK build must pass.
