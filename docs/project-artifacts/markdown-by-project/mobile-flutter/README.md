# Smart Laundry Locker Mobile

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

Flutter mobile app for the Smart Laundry Locker project.

## Current Status

Implemented and verified in the latest pass:

- Real login through `POST /api/auth/login` using `identifier` and `password`.
- Role-based routing after login and splash restore:
  - `MANAGER` or `ADMIN` -> `/manager`
  - `MAINTENANCE` -> `/maintenance-home`
  - other users -> `/home`
- Customer locker quick actions on home:
  - `Thuê tủ` -> rental flow
  - `Gửi hàng` -> send parcel flow
  - `Đơn tủ` -> locker orders page
- New locker operations module under `lib/features/locker_ops/`.
- Customer SEND/RENTAL/my locker orders screens.
- Manager home with stats, locker layout, and order tabs.
- Maintenance home with fault and report queues.
- PIN plus QR rendering through `qr_flutter`.

## Environment

Local Android emulator should call the backend gateway with:

```env
API_BASE_URL=http://10.0.2.2:8080
```

For a physical phone, change `API_BASE_URL` to the LAN IP of the backend machine, for example:

```env
API_BASE_URL=http://192.168.1.10:8080
```

After changing `.env`, regenerate envied output:

```powershell
dart run build_runner build --delete-conflicting-outputs
```

Do not commit local secret/env files unless they are intentionally sanitized.

## Commands

```powershell
Set-Location G:\BigProject\smart-laundry-locker-mobile

C:\flutter\bin\flutter.bat pub get

C:\flutter\bin\flutter.bat analyze `
  lib/features/locker_ops `
  lib/core/routing `
  lib/features/auth/presentation/pages/login_screen.dart `
  lib/features/auth/presentation/pages/splash_screen.dart `
  lib/features/home/presentation/pages/home_page.dart

C:\flutter\bin\flutter.bat build apk --debug
```

Run on emulator:

```powershell
C:\flutter\bin\flutter.bat emulators --launch Pixel_8
C:\flutter\bin\flutter.bat run
```

## Important Files

| File/folder | Purpose |
|---|---|
| `lib/core/routing/app_router.dart` | Main GoRouter configuration and locker routes. |
| `lib/core/routing/role_routes.dart` | Role-to-home routing helper. |
| `lib/features/auth/presentation/pages/login_screen.dart` | Real identifier/password login screen. |
| `lib/features/auth/presentation/pages/splash_screen.dart` | Restores token and routes by role. |
| `lib/features/home/presentation/pages/home_page.dart` | Customer home and locker quick actions. |
| `lib/features/locker_ops/data/locker_ops_service.dart` | Dio service for locker/order/manager/maintenance APIs. |
| `lib/features/locker_ops/presentation/pages/send_parcel_page.dart` | Customer SEND flow. |
| `lib/features/locker_ops/presentation/pages/rent_locker_page.dart` | Customer RENTAL flow. |
| `lib/features/locker_ops/presentation/pages/my_locker_orders_page.dart` | Customer locker order history/actions. |
| `lib/features/locker_ops/presentation/pages/manager_home_page.dart` | Manager dashboard. |
| `lib/features/locker_ops/presentation/pages/maintenance_home_page.dart` | Maintenance queue. |

## Backend Requirements

Run the backend first:

```powershell
Set-Location G:\BigProject\laundry-locker-microservices
mvn.cmd clean package -DskipTests
docker compose up --build -d
```

The app expects the gateway at `http://10.0.2.2:8080` on Android emulator.

## Verification From Latest Run

Passing checks:

- `flutter pub get`
- targeted `flutter analyze`
- `flutter build apk --debug`
- Debug APK installed on emulator
- Customer login `demo@laundry.test` / `secret123`
- Customer home rendered new `Thuê tủ`, `Gửi hàng`, `Đơn tủ` actions
- Rental route rendered
- Send parcel route rendered
- My locker orders called `/api/orders/my-orders` and received `200 OK`

Role backend logins were also verified for:

- `manager@laundry.test` / `Manager@123456` -> `MANAGER`
- `maintenance@laundry.test` / `Maint@123456` -> `MAINTENANCE`

Manual manager/maintenance UI smoke on the emulator was limited by ADB/launcher input behavior, but role routing code and backend login responses are wired.

## Known Notes

- Some older home widgets still call legacy endpoints such as `/advertisements`, `/blogs`, or `/wallet/balance`. These may return `404` on the current local backend and do not block the locker operations flow.
- Older auth/register/OTP data sources still exist for legacy screens. The current login screen uses the real `/api/auth/login` flow.
- Kotlin Gradle Plugin migration warnings may appear on debug build; current APK build passes.
