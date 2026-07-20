# Current Project Status

> Last updated: 2026-06-13
> Workspace: `G:\BigProject`
> Backend repo: `laundry-locker-microservices`

This document is the current source of truth for the capstone project state. Older Markdown files may describe earlier
design assumptions, mocks, or partially completed phases. Use this file together with `BUSINESS_FLOWS_CURRENT.md`,
`PROJECT_PROGRESS_TRACKER.md`, `RUN_RESULT.md`, and `LOCKER_FLOW_PLAN.md` when deciding what is actually implemented
now.

## Living Docs For Future Work

These two files must be updated whenever business behavior or project progress changes:

- `docs/BUSINESS_FLOWS_CURRENT.md`: detailed current business flows, roles, endpoints, screens, events, and known
  partial/future flows.
- `docs/PROJECT_PROGRESS_TRACKER.md`: implementation progress, component matrix, remaining work, verification log, and
  recent change log.

When handing the project to another developer or AI coding agent, ask them to read `PROJECT_PROGRESS_TRACKER.md` first,
then `BUSINESS_FLOWS_CURRENT.md`.

## System Scope

The project is a smart laundry locker platform with separate backend, web frontend, Flutter mobile app, and IoT
simulation/runtime code.

Main folders:

- `laundry-locker-microservices/`: Java 21, Spring Boot 3.5.14 microservices backend.
- `laundry-locker-frontend/fe/`: React 19 + Vite admin/frontend app.
- `smart-laundry-locker-mobile/`: Flutter mobile app.
- `smart-locker-iot/`: Python IoT simulation/runtime.

## Backend

Current backend branch during the latest work: `develop`.

Core services run through Docker Compose:

- Gateway: `http://localhost:8080`
- Eureka: `http://localhost:8761`
- PostgreSQL host port: `15432`
- RabbitMQ: `5672`, management UI `15672`

Important note: `laundry-service` and `partner-service` source folders are not present in the workspace.
`docker-compose.override.yml` keeps those missing-source services behind a profile so normal local compose runs do not
try to build them.

### Locker Phase 1

Implemented and tested:

- Physical locker cell model with `DRONE`, `STANDARD`, and `XL` cell types.
- Row/column cell layout.
- Landing pad and marker metadata.
- Cell lifecycle: `AVAILABLE -> RESERVED -> OCCUPIED -> AVAILABLE`.
- Sticky `FAULT` status with report/clear flow.
- Guard: `DRONE` cells may only be reserved through `channel=DRONE`.
- Demo cabinet `CAB-DEMO-01`.

### Locker Phase 2

Implemented and tested:

- SEND flow with two-stage PIN behavior.
- RENTAL flow with hourly pricing, extend/end rental, and multi-use PIN during rental.
- Stateless signed QR token tied to active PIN.
- IoT verify-access support for PIN or QR token.
- Maintenance reports and assignment flow.
- Manager endpoints under `/api/manage/**`.
- Maintenance endpoints under `/api/maintenance/**`.
- Gateway RBAC:
    - `/api/admin/**`: `ADMIN`
    - `/api/manage/**`: `MANAGER` or `ADMIN`
    - `/api/maintenance/**`: `MAINTENANCE` or `ADMIN`
    - `/internal/**`: blocked through gateway, service-to-service only.
- Scheduler for overdue reminders and cleanup of completed order cells.

Backend health was verified through:

```powershell
curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/actuator/health
```

Expected result: `200`.

### Backend Production Hardening

Implemented and verified:

- Phase 1: correlation ID through gateway/services, Prometheus metrics, OpenAPI runtime docs, Dependency Review/CodeQL
  workflow, deploy build with `mvn -B clean verify`.
- Phase 2: OpenFeign Resilience4j circuit breaker/timeouts, `/actuator/sbom`, CycloneDX SBOM generation, Trivy image
  scan gate, and locker-service Testcontainers smoke test.
- Phase 3/4: gateway RBAC/access-token unit tests, Swagger UI/OpenAPI aggregation through gateway, Spring Boot build
  metadata, full 12-image Trivy matrix for existing Dockerfiles, deploy artifact SHA-256 checksum, and deploy script
  checksum verification.
- Phase 4 continuation: GitHub artifact attestation/provenance for deploy artifacts, tag-based backend release workflow,
  release tarball/SBOM/checksum, GitHub Release publishing, and a release artifact verification helper script.

Verification:

```powershell
cd G:\BigProject\laundry-locker-microservices
mvn -pl api-gateway -am test
mvn -B test
mvn -B clean verify
& 'C:\Program Files\Git\bin\bash.exe' -n scripts/deploy-from-artifact.sh
& 'C:\Program Files\Git\bin\bash.exe' -n scripts/verify-release-artifact.sh
```

Expected result: Maven passes. Testcontainers locker smoke can be skipped locally when Docker daemon is not available.

## Web Frontend

Current web frontend repo root: `laundry-locker-frontend`.
Main app folder: `laundry-locker-frontend/fe`.
Current branch during latest work: `main`.

Implemented and verified:

- Admin sidebar entries for lockers and locker maintenance.
- Locker list wired to real API.
- Locker layout page at `/admin/lockers/:lockerId`.
- Maintenance page at `/admin/maintenance`.
- RTK Query API slice for locker operations.
- Build-time TypeScript fixes for old examples and order timeline.

Verification:

```powershell
cd G:\BigProject\laundry-locker-frontend\fe
npm.cmd run build
```

Expected result: `tsc -b && vite build` passes.

Local dev server was also verified at `http://localhost:3000`.

## Flutter Mobile

Current mobile branch during latest work: `develop`.

Implemented and verified:

- Real login screen using `POST /api/auth/login` with `identifier` and `password`.
- Role-based routing:
    - `MANAGER` or `ADMIN` -> `/manager`
    - `MAINTENANCE` -> `/maintenance-home`
    - other users -> `/home`
- Customer home quick actions:
    - `Thuê tủ` -> rental flow
    - `Gửi hàng` -> send parcel flow
    - `Đơn tủ` -> locker orders page
- New locker operations module under `lib/features/locker_ops/`.
- Manager home with stats/layout/orders tabs.
- Maintenance home with fault/report queues.
- QR rendering with `qr_flutter`.

Verification:

```powershell
cd G:\BigProject\smart-laundry-locker-mobile
C:\flutter\bin\flutter.bat pub get
C:\flutter\bin\flutter.bat analyze lib/features/locker_ops lib/core/routing lib/features/auth/presentation/pages/login_screen.dart lib/features/auth/presentation/pages/splash_screen.dart lib/features/home/presentation/pages/home_page.dart
C:\flutter\bin\flutter.bat build apk --debug
```

Expected results:

- `flutter pub get`: pass
- `flutter analyze`: no issues
- `flutter build apk --debug`: pass

Smoke test completed on emulator:

- Customer login with `demo@laundry.test`.
- Home renders the new locker quick actions.
- Rental route renders.
- Send parcel route renders.
- My locker orders route calls `/api/orders/my-orders` and receives `200 OK`.

Known non-blocking notes:

- Home still calls some older endpoints such as `/advertisements`, `/blogs`, and `/wallet/balance`; these may return
  `404` in the current local backend and do not block the locker operations flow.
- Android build warns about Kotlin Gradle Plugin migration for future Flutter versions; current debug APK build passes.
- Manual ADB typing for manager/maintenance UI smoke was unreliable because of emulator input/launcher behavior. Backend
  role login and code routing were verified.

## IoT

Implemented backend and app-side access verification:

- PIN and signed QR token can be verified through the IoT access flow.
- Drone remains a separate channel that only reserves `DRONE` cells through `channel=DRONE`.

Still future/Phase 3:

- Tablet-web locker screen.
- Automatic occupy from real door/weight sensors.
- Real drone service integration.
- Biometric verification on Raspberry Pi.

## Current Remaining Work

Main remaining items are not blockers for the current locker Phase 2 demo:

- Build real `laundry-service` and `partner-service` source modules if those scopes are required.
- Implement tablet-web locker screen.
- Integrate real device sensors and real drone service.
- Add real payment provider flow for SEND/RENTAL if required.
- Decide whether to replace or update old home APIs used by mobile (`advertisements`, `blogs`, `wallet`).

## Files That Must Not Be Committed

The following root workspace files are intentionally not committed and should stay outside project commits:

- `env.txt`
- `pro.txt`
- `Application.txt`

They may contain local notes, environment details, or private information.
