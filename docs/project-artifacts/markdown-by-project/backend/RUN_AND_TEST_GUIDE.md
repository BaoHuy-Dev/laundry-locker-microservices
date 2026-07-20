# Run And Test Guide

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho
> locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng
> Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`,
`RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

Workspace: `G:\BigProject\laundry-locker-microservices`

This guide is for Windows PowerShell. Use `curl.exe` explicitly when using curl because PowerShell may alias `curl`.

## 1. Prerequisites

- Java 21
- Maven 3.9+ (`mvn.cmd` on PATH, or `C:\Maven\apache-maven-3.9.16\bin\mvn.cmd`)
- Docker Desktop
- Ports: `8080`, `8761`, `8081-8084`, `8086-8090`, `8092`, `15432`, `5672`, `15672`

Current note: `laundry-service` on `8085` and `partner-service` on `8091` do not have source folders.
`docker-compose.override.yml` skips them with profile `missing-source`.

Check tools:

```powershell
java -version
mvn.cmd -version
docker version
docker compose version
```

## 2. Build And Start

```powershell
Set-Location G:\BigProject\laundry-locker-microservices

# Full package is recommended before rebuilding Docker images.
mvn.cmd clean package -DskipTests

docker compose config
docker compose up --build -d
docker compose ps
```

If Maven is not on PATH:

```powershell
C:\Maven\apache-maven-3.9.16\bin\mvn.cmd clean package -DskipTests
```

Expected running containers include:

- `ll-ms-postgres`
- `ll-ms-rabbitmq`
- `ll-ms-discovery-server`
- `ll-ms-api-gateway`
- `ll-ms-auth-service`
- `ll-ms-user-service`
- `ll-ms-order-service`
- `ll-ms-locker-service`
- `ll-ms-payment-service`
- `ll-ms-notification-service`
- `ll-ms-iot-service`
- `ll-ms-store-service`
- `ll-ms-staff-service`
- `ll-ms-loyalty-service`

## 3. Health Checks

```powershell
curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/actuator/health
Start-Process http://localhost:8761
Start-Process http://localhost:15672
```

Expected:

- Gateway health returns `200`.
- Eureka shows the source-backed services.
- RabbitMQ login is `guest` / `guest`.

Direct service checks:

```powershell
$services = @{
  "discovery-server" = 8761
  "api-gateway" = 8080
  "auth-service" = 8081
  "user-service" = 8082
  "order-service" = 8083
  "locker-service" = 8084
  "payment-service" = 8086
  "notification-service" = 8087
  "iot-service" = 8088
  "store-service" = 8089
  "staff-service" = 8090
  "loyalty-service" = 8092
}

$services.GetEnumerator() | Sort-Object Name | ForEach-Object {
  $url = "http://localhost:$($_.Value)/actuator/health"
  Write-Host "`n[$($_.Name)] $url"
  curl.exe -s $url
}
```

## 4. Authentication Smoke Test

All client traffic goes through `http://localhost:8080`.

```powershell
$BASE = "http://localhost:8080"
$email = "customer$((Get-Random))@laundry.test"
$password = "secret123"

$register = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/auth/register" `
  -ContentType "application/json" `
  -Body (@{
    email = $email
    phoneNumber = "0900000000"
    firstName = "Demo"
    lastName = "Customer"
    password = $password
    roles = @("CUSTOMER")
  } | ConvertTo-Json)

$login = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/auth/login" `
  -ContentType "application/json" `
  -Body (@{
    identifier = $email
    password = $password
  } | ConvertTo-Json)

$TOKEN = $login.data.accessToken
$USER_ID = $login.data.userId
$AUTH = @{ Authorization = "Bearer $TOKEN" }

$login.data | Select-Object userId, roles, tokenType
```

Important: login uses `identifier`, not `email`.

## 5. Locker Layout

```powershell
$lockers = Invoke-RestMethod -Method Get -Uri "$BASE/api/lockers" -Headers $AUTH
$locker = @($lockers.data | Where-Object { $_.code -eq "CAB-DEMO-01" } | Select-Object -First 1)[0]

if ($null -eq $locker) {
  throw "CAB-DEMO-01 was not found. Check locker-service Flyway migrations."
}

$LOCKER_ID = $locker.id
$layout = Invoke-RestMethod -Method Get -Uri "$BASE/api/lockers/$LOCKER_ID/layout" -Headers $AUTH
$layout.data.cells | Select-Object boxId, boxNumber, cellType, rowIndex, colIndex, status
```

Expected demo layout:

- 3 `DRONE` cells.
- 6 `STANDARD` cells.
- 1 `XL` cell.

## 6. SEND Flow

```powershell
$send = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/orders/send" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body (@{
    lockerId = $LOCKER_ID
    receiverPhone = "0900000001"
    receiverName = "Receiver"
    note = "PowerShell SEND smoke test"
  } | ConvertTo-Json)

$SEND_ORDER_ID = $send.data.id
$send.data | Select-Object id, type, status, sendBoxId, pinCode, qrToken

$confirmed = Invoke-RestMethod `
  -Method Put `
  -Uri "$BASE/api/orders/$SEND_ORDER_ID/confirm" `
  -Headers $AUTH

$confirmed.data | Select-Object id, type, status, sendBoxId, pinCode, qrToken, pickupDeadline
```

Expected:

- Create returns a `SEND` order and an initial PIN for the sender to drop the parcel.
- Confirm rotates the active PIN for receiver pickup and returns a `qrToken`.

Verify active access:

```powershell
$boxId = $confirmed.data.sendBoxId
$pin = $confirmed.data.pinCode
$qr = $confirmed.data.qrToken

Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/iot/verify-access" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body (@{ boxId = $boxId; pinCode = $pin } | ConvertTo-Json)

Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/iot/verify-access" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body (@{ boxId = $boxId; pinCode = $qr } | ConvertTo-Json)
```

## 7. RENTAL Flow

```powershell
$rental = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/orders/rental" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body (@{
    lockerId = $LOCKER_ID
    cellType = "XL"
    hours = 2
    note = "PowerShell RENTAL smoke test"
  } | ConvertTo-Json)

$RENTAL_ORDER_ID = $rental.data.id
$rental.data | Select-Object id, type, status, sendBoxId, pinCode, qrToken, totalPrice, pickupDeadline

$extended = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/orders/$RENTAL_ORDER_ID/extend-rental" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body (@{ hours = 1 } | ConvertTo-Json)

$extended.data | Select-Object id, type, status, totalPrice, pickupDeadline

$closed = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/orders/$RENTAL_ORDER_ID/pickup-storage" `
  -Headers $AUTH

$closed.data | Select-Object id, type, status, completedAt
```

Expected:

- `STANDARD` rental rate defaults to `5000` per hour.
- `XL` rental rate defaults to `10000` per hour.
- Ending rental releases the cell.

## 8. Manager And Maintenance RBAC

Create a manager account:

```powershell
$manager = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/auth/register" `
  -ContentType "application/json" `
  -Body (@{
    email = "manager$((Get-Random))@laundry.test"
    phoneNumber = "0910000000"
    firstName = "Demo"
    lastName = "Manager"
    password = "secret123"
    roles = @("MANAGER")
  } | ConvertTo-Json)

$MANAGER_AUTH = @{ Authorization = "Bearer $($manager.data.accessToken)" }

Invoke-RestMethod -Method Get -Uri "$BASE/api/manage/lockers/stats" -Headers $MANAGER_AUTH
Invoke-RestMethod -Method Get -Uri "$BASE/api/manage/orders/statistics" -Headers $MANAGER_AUTH
```

Create a maintenance account:

```powershell
$maintenance = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/auth/register" `
  -ContentType "application/json" `
  -Body (@{
    email = "maintenance$((Get-Random))@laundry.test"
    phoneNumber = "0920000000"
    firstName = "Demo"
    lastName = "Maintenance"
    password = "secret123"
    roles = @("MAINTENANCE")
  } | ConvertTo-Json)

$MAINT_AUTH = @{ Authorization = "Bearer $($maintenance.data.accessToken)" }

Invoke-RestMethod -Method Get -Uri "$BASE/api/maintenance/faults" -Headers $MAINT_AUTH
Invoke-RestMethod -Method Get -Uri "$BASE/api/maintenance/reports" -Headers $MAINT_AUTH
```

Check RBAC denial examples:

```powershell
# Customer token should not access manager API.
try {
  Invoke-RestMethod -Method Get -Uri "$BASE/api/manage/orders/statistics" -Headers $AUTH
} catch {
  $_.Exception.Response.StatusCode.value__
}

# External clients must not call internal endpoints through the gateway.
try {
  Invoke-RestMethod -Method Get -Uri "$BASE/internal/orders/1" -Headers $AUTH
} catch {
  $_.Exception.Response.StatusCode.value__
}
```

Expected result for both denial checks: `403`.

## 9. Fault And Maintenance Flow

Report a fault from a customer token:

```powershell
$cells = (Invoke-RestMethod -Method Get -Uri "$BASE/api/lockers/$LOCKER_ID/layout" -Headers $AUTH).data.cells
$target = @($cells | Where-Object { $_.status -eq "AVAILABLE" -and $_.cellType -eq "STANDARD" } | Select-Object -First 1)[0]

$fault = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/boxes/$($target.boxId)/fault" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body (@{ reason = "Smoke test fault" } | ConvertTo-Json)

$fault.data | Select-Object boxId, status, faultReason
```

Claim and resolve:

```powershell
$reports = Invoke-RestMethod -Method Get -Uri "$BASE/api/maintenance/reports" -Headers $MAINT_AUTH
$report = @($reports.data | Select-Object -First 1)[0]

$claimed = Invoke-RestMethod -Method Put -Uri "$BASE/api/maintenance/reports/$($report.id)/claim" -Headers $MAINT_AUTH
$resolved = Invoke-RestMethod -Method Put -Uri "$BASE/api/maintenance/reports/$($report.id)/resolve" -Headers $MAINT_AUTH

$claimed.data | Select-Object id, status, assignedToUserId
$resolved.data | Select-Object id, status, resolvedAt
```

## 10. Debugging

Logs:

```powershell
docker compose logs --tail=200 api-gateway
docker compose logs --tail=200 order-service
docker compose logs --tail=200 locker-service
docker compose logs --tail=200 iot-service
docker compose logs --tail=200 postgres
docker compose logs --tail=200 rabbitmq
```

Restart one service:

```powershell
docker compose restart api-gateway
docker compose restart order-service
```

Rebuild one service after code changes:

```powershell
mvn.cmd clean package -DskipTests
docker compose up --build -d order-service
```

Full reset, including all local database data:

```powershell
docker compose down -v
docker compose up --build -d
```

Use `down -v` only when you are okay losing local test data.

## 11. Known Current Notes

- `laundry-service` and `partner-service` source modules are missing and intentionally skipped by
  `docker-compose.override.yml`.
- `/internal/**` endpoints are blocked through the gateway; use Feign/service-to-service calls or direct local service
  ports only for debugging.
- The current locker Phase 2 demo does not require real Firebase, VNPay, MoMo, or physical sensor credentials.
- See `docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, and `LOCKER_FLOW_PLAN.md` for the latest verified project
  state.
