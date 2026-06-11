# Run And Test Guide

Workspace: `D:\BigProject\laundry-locker-microservices`

This guide is for Windows PowerShell. Use `curl.exe` explicitly if you prefer curl, because Windows PowerShell may alias `curl` to `Invoke-WebRequest`.

## 1. Prerequisites

- Java 21
- Maven installed and available as `mvn`
- Docker Desktop installed
- Ports below must be free before starting the stack

Check tools:

```powershell
java -version
mvn -version
docker version
docker compose version
```

If `docker version` cannot connect to the daemon, Docker Desktop is not running yet.

## 2. Start Docker Desktop

1. Open Windows Start menu.
2. Search `Docker Desktop`.
3. Click `Docker Desktop`.
4. Wait until the whale icon says Docker Desktop is running.
5. Keep Docker Desktop open while running the stack.

Optional PowerShell launch:

```powershell
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
```

If Docker asks to use WSL 2, accept it. The compose file uses Linux images: `postgres:16-alpine` and `rabbitmq:3-management-alpine`.

## 3. Ports

| Component | Container | Host URL / port |
|---|---|---|
| api-gateway | `ll-ms-api-gateway` | `http://localhost:8080` |
| discovery-server / Eureka | `ll-ms-discovery-server` | `http://localhost:8761` |
| auth-service | `ll-ms-auth-service` | `http://localhost:8081` |
| user-service | `ll-ms-user-service` | `http://localhost:8082` |
| order-service | `ll-ms-order-service` | `http://localhost:8083` |
| locker-service | `ll-ms-locker-service` | `http://localhost:8084` |
| laundry-service | `ll-ms-laundry-service` | `http://localhost:8085` |
| payment-service | `ll-ms-payment-service` | `http://localhost:8086` |
| notification-service | `ll-ms-notification-service` | `http://localhost:8087` |
| iot-service | `ll-ms-iot-service` | `http://localhost:8088` |
| store-service | `ll-ms-store-service` | `http://localhost:8089` |
| staff-service | `ll-ms-staff-service` | `http://localhost:8090` |
| partner-service | `ll-ms-partner-service` | `http://localhost:8091` |
| loyalty-service | `ll-ms-loyalty-service` | `http://localhost:8092` |
| PostgreSQL | `ll-ms-postgres` | `localhost:15432` -> container `5432` |
| RabbitMQ AMQP | `ll-ms-rabbitmq` | `localhost:5672` |
| RabbitMQ UI | `ll-ms-rabbitmq` | `http://localhost:15672` (`guest` / `guest`) |
| WebSocket/STOMP via gateway | notification-service | `ws://localhost:8080/ws` |

Databases:

| Service | Database | User | Password | Schema |
|---|---|---|---|---|
| auth-service | `auth_db` | `auth_user` | `auth_pass` | `auth_schema` |
| user-service | `user_db` | `user_user` | `user_pass` | `user_schema` |
| order-service | `order_db` | `order_user` | `order_pass` | `order_schema` |
| locker-service | `locker_db` | `locker_user` | `locker_pass` | `locker_schema` |
| laundry-service | `laundry_db` | `laundry_user` | `laundry_pass` | `laundry_schema` |
| payment-service | `payment_db` | `payment_user` | `payment_pass` | `payment_schema` |
| notification-service | `notification_db` | `notification_user` | `notification_pass` | `notification_schema` |
| iot-service | `iot_db` | `iot_user` | `iot_pass` | `iot_schema` |
| store-service | `store_db` | `store_user` | `store_pass` | `store_schema` |
| staff-service | `staff_db` | `staff_user` | `staff_pass` | `staff_schema` |
| partner-service | `partner_db` | `partner_user` | `partner_pass` | `partner_schema` |
| loyalty-service | `loyalty_db` | `loyalty_user` | `loyalty_pass` | `loyalty_schema` |

## 4. Start From Scratch

Open PowerShell:

```powershell
Set-Location D:\BigProject\laundry-locker-microservices
```

Build and validate:

```powershell
mvn clean package
mvn test
docker compose config
```

Start all containers:

```powershell
docker compose up --build -d
```

Watch startup:

```powershell
docker compose ps
docker compose logs -f discovery-server
```

In another PowerShell window, watch all service logs if needed:

```powershell
docker compose logs -f
```

## 5. Check Services Are Up

Container state:

```powershell
docker compose ps
```

Eureka UI:

```powershell
Start-Process http://localhost:8761
```

Expected registered apps include:

- `API-GATEWAY`
- `AUTH-SERVICE`
- `USER-SERVICE`
- `ORDER-SERVICE`
- `LOCKER-SERVICE`
- `LAUNDRY-SERVICE`
- `PAYMENT-SERVICE`
- `NOTIFICATION-SERVICE`
- `IOT-SERVICE`
- `STORE-SERVICE`
- `STAFF-SERVICE`
- `PARTNER-SERVICE`
- `LOYALTY-SERVICE`

Gateway health:

```powershell
curl.exe http://localhost:8080/actuator/health
curl.exe http://localhost:8080/
```

Direct actuator health checks:

```powershell
$services = @{
  "discovery-server" = 8761
  "api-gateway" = 8080
  "auth-service" = 8081
  "user-service" = 8082
  "order-service" = 8083
  "locker-service" = 8084
  "laundry-service" = 8085
  "payment-service" = 8086
  "notification-service" = 8087
  "iot-service" = 8088
  "store-service" = 8089
  "staff-service" = 8090
  "partner-service" = 8091
  "loyalty-service" = 8092
}

$services.GetEnumerator() | Sort-Object Name | ForEach-Object {
  $url = "http://localhost:$($_.Value)/actuator/health"
  Write-Host "`n[$($_.Name)] $url"
  curl.exe -s $url
}
```

RabbitMQ UI:

```powershell
Start-Process http://localhost:15672
```

Login with `guest` / `guest`.

PostgreSQL database list:

```powershell
docker exec -it ll-ms-postgres psql -U postgres -d postgres -c "\l"
```

Check one service schema:

```powershell
docker exec -it ll-ms-postgres psql -U auth_user -d auth_db -c "\dn"
docker exec -it ll-ms-postgres psql -U auth_user -d auth_db -c "\dt auth_schema.*"
```

## 6. End-To-End Test With PowerShell

All API calls below go through `api-gateway` at `http://localhost:8080`.

Set base URL:

```powershell
$BASE = "http://localhost:8080"
```

### 6.1 Register And Login User

Use a unique email each run:

```powershell
$email = "demo$((Get-Random))@laundry.test"
$password = "secret123"

$registerBody = @{
  email = $email
  phoneNumber = "0900000000"
  firstName = "Demo"
  lastName = "User"
  password = $password
  roles = @("USER")
}

$register = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/auth/register" `
  -ContentType "application/json" `
  -Body ($registerBody | ConvertTo-Json)

$TOKEN = $register.data.accessToken
$USER_ID = $register.data.userId
$AUTH = @{ Authorization = "Bearer $TOKEN" }

$register.data
```

Login again:

```powershell
$loginBody = @{
  identifier = $email
  password = $password
}

$login = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/auth/login" `
  -ContentType "application/json" `
  -Body ($loginBody | ConvertTo-Json)

$TOKEN = $login.data.accessToken
$USER_ID = $login.data.userId
$AUTH = @{ Authorization = "Bearer $TOKEN" }
```

### 6.2 User Profile

```powershell
Invoke-RestMethod -Method Get -Uri "$BASE/api/user/profile" -Headers $AUTH

$profileBody = @{
  firstName = "Demo"
  lastName = "Updated"
  email = $email
  phoneNumber = "0900000000"
}

Invoke-RestMethod `
  -Method Put `
  -Uri "$BASE/api/user/profile" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body ($profileBody | ConvertTo-Json)
```

### 6.3 Store

```powershell
$storeBody = @{
  name = "District 1 Store"
  contactPhone = "0901111222"
  address = "HCMC District 1"
  latitude = 10.7769
  longitude = 106.7009
  description = "Smoke test store"
  active = $true
  status = "ACTIVE"
}

$store = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/stores" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body ($storeBody | ConvertTo-Json)

$STORE_ID = $store.data.id

Invoke-RestMethod -Method Get -Uri "$BASE/api/stores"
Invoke-RestMethod -Method Get -Uri "$BASE/api/stores/$STORE_ID"
```

### 6.4 Laundry Service

```powershell
$laundryBody = @{
  storeId = $STORE_ID
  name = "Wash and Fold"
  category = "LAUNDRY"
  serviceType = "WASH"
  unitPrice = 50000
  maxPrice = 100000
  unit = "KG"
  description = "Smoke test laundry service"
  estimatedHours = 24
  status = "ACTIVE"
}

$laundry = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/services" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body ($laundryBody | ConvertTo-Json)

$SERVICE_ID = $laundry.data.id

Invoke-RestMethod -Method Get -Uri "$BASE/api/services?storeId=$STORE_ID"
```

### 6.5 Locker And Box

```powershell
$lockerBody = @{
  storeId = $STORE_ID
  code = "LCK-$((Get-Random))"
  name = "Locker 001"
  status = "ACTIVE"
  address = "HCMC District 1"
  latitude = 10.7769
  longitude = 106.7009
}

$locker = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/lockers" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body ($lockerBody | ConvertTo-Json)

$LOCKER_ID = $locker.data.id

$boxBody = @{
  lockerId = $LOCKER_ID
  boxNumber = 1
  size = "MEDIUM"
  status = "AVAILABLE"
}

$box = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/boxes" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body ($boxBody | ConvertTo-Json)

$BOX_ID = $box.data.id

Invoke-RestMethod -Method Get -Uri "$BASE/api/lockers/$LOCKER_ID/boxes"
Invoke-RestMethod -Method Get -Uri "$BASE/api/lockers/$LOCKER_ID/boxes/available"
```

### 6.6 Order

```powershell
$orderBody = @{
  userId = $USER_ID
  storeId = $STORE_ID
  lockerId = $LOCKER_ID
  sendBoxId = $BOX_ID
  serviceCategory = "LAUNDRY"
  totalPrice = 50000
  items = @(
    @{
      serviceId = $SERVICE_ID
      quantity = 1
      description = "Wash and Fold"
    }
  )
}

$order = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/orders" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body ($orderBody | ConvertTo-Json -Depth 5)

$ORDER_ID = $order.data.id

Invoke-RestMethod -Method Get -Uri "$BASE/api/orders/$ORDER_ID" -Headers $AUTH

$statusBody = @{
  status = "READY"
  staffId = $USER_ID
  receiveBoxId = $BOX_ID
}

Invoke-RestMethod `
  -Method Patch `
  -Uri "$BASE/api/orders/$ORDER_ID/status" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body ($statusBody | ConvertTo-Json)

Invoke-RestMethod -Method Get -Uri "$BASE/api/orders/$ORDER_ID/status" -Headers $AUTH
```

### 6.7 Payment

```powershell
$paymentBody = @{
  orderId = $ORDER_ID
  userId = $USER_ID
  amount = 50000
  method = "CASH"
  description = "Smoke test payment"
  referenceId = "PAY-$((Get-Random))"
}

$payment = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/payments" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body ($paymentBody | ConvertTo-Json)

$PAYMENT_ID = $payment.data.id

Invoke-RestMethod `
  -Method Patch `
  -Uri "$BASE/api/payments/$PAYMENT_ID/status" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body (@{ status = "COMPLETED" } | ConvertTo-Json)

Invoke-RestMethod -Method Get -Uri "$BASE/api/payments/$PAYMENT_ID" -Headers $AUTH
Invoke-RestMethod -Method Get -Uri "$BASE/api/payments?orderId=$ORDER_ID" -Headers $AUTH
```

### 6.8 Notification

Create a notification through the internal endpoint:

```powershell
$notificationBody = @{
  userId = $USER_ID
  title = "Smoke test notification"
  message = "Notification created through api-gateway"
  type = "SYSTEM"
  referenceId = $ORDER_ID
  referenceType = "ORDER"
}

$notification = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/internal/notifications" `
  -ContentType "application/json" `
  -Body ($notificationBody | ConvertTo-Json)

$NOTIFICATION_ID = $notification.data.id

Invoke-RestMethod -Method Get -Uri "$BASE/api/notifications/user/$USER_ID" -Headers $AUTH
Invoke-RestMethod -Method Patch -Uri "$BASE/api/notifications/$NOTIFICATION_ID/read" -Headers $AUTH
```

### 6.9 FCM Token

This stores a token record. Real Firebase push requires Firebase credentials, which are not required for local smoke testing.

```powershell
$fcmBody = @{
  token = "local-dev-fcm-token-$((Get-Random))"
  deviceType = "WINDOWS_POSTMAN"
}

Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/user/fcm-token" `
  -Headers $AUTH `
  -ContentType "application/json" `
  -Body ($fcmBody | ConvertTo-Json)
```

Delete token if needed:

```powershell
curl.exe -X DELETE "$BASE/api/user/fcm-token?fcmToken=$($fcmBody.token)" -H "Authorization: Bearer $TOKEN"
```

### 6.10 WebSocket / STOMP

The gateway routes `/ws` to `notification-service`.

Option A: Postman

1. Open Postman.
2. New request -> WebSocket.
3. Connect to `ws://localhost:8080/ws`.
4. Send a STOMP `CONNECT` frame:

```text
CONNECT
accept-version:1.2
host:localhost

\0
```

5. Subscribe to broadcast notifications:

```text
SUBSCRIBE
id:sub-0
destination:/topic/notifications

\0
```

6. Trigger a broadcast:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/internal/notifications/broadcast" `
  -ContentType "application/json" `
  -Body (@{
    title = "Broadcast test"
    message = "Hello WebSocket"
    type = "SYSTEM"
  } | ConvertTo-Json)
```

Option B: `wscat`

```powershell
npm install -g wscat
wscat -c ws://localhost:8080/ws
```

Then send the same STOMP frames above.

### 6.11 Admin Dashboard

Create an admin account and use its token:

```powershell
$adminEmail = "admin$((Get-Random))@laundry.test"
$adminPassword = "secret123"

$adminRegister = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE/api/auth/register" `
  -ContentType "application/json" `
  -Body (@{
    email = $adminEmail
    phoneNumber = "0911111111"
    firstName = "Admin"
    lastName = "User"
    password = $adminPassword
    roles = @("ADMIN")
  } | ConvertTo-Json)

$ADMIN_TOKEN = $adminRegister.data.accessToken
$ADMIN_AUTH = @{ Authorization = "Bearer $ADMIN_TOKEN" }

Invoke-RestMethod -Method Get -Uri "$BASE/api/admin/dashboard/overview" -Headers $ADMIN_AUTH
Invoke-RestMethod -Method Get -Uri "$BASE/api/admin/system/health" -Headers $ADMIN_AUTH
Invoke-RestMethod -Method Get -Uri "$BASE/api/admin/users" -Headers $ADMIN_AUTH
```

## 7. curl.exe Samples

Register:

```powershell
curl.exe -X POST "http://localhost:8080/api/auth/register" `
  -H "Content-Type: application/json" `
  -d '{"email":"curl-demo@laundry.test","phoneNumber":"0900000001","firstName":"Curl","lastName":"User","password":"secret123","roles":["USER"]}'
```

Login:

```powershell
curl.exe -X POST "http://localhost:8080/api/auth/login" `
  -H "Content-Type: application/json" `
  -d '{"identifier":"curl-demo@laundry.test","password":"secret123"}'
```

Use the returned `data.accessToken`:

```powershell
$TOKEN = "paste-access-token-here"
curl.exe "http://localhost:8080/api/user/profile" -H "Authorization: Bearer $TOKEN"
```

## 8. Postman Checklist

Create a Postman environment:

| Variable | Value |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `token` | set after login |
| `adminToken` | set after admin register/login |
| `userId` | set from auth response |
| `storeId` | set from create store response |
| `serviceId` | set from create laundry service response |
| `lockerId` | set from create locker response |
| `boxId` | set from create box response |
| `orderId` | set from create order response |
| `paymentId` | set from create payment response |

Recommended request order:

1. `GET {{baseUrl}}/actuator/health`
2. `POST {{baseUrl}}/api/auth/register`
3. `POST {{baseUrl}}/api/auth/login`
4. `GET {{baseUrl}}/api/user/profile` with Bearer `{{token}}`
5. `POST {{baseUrl}}/api/stores`
6. `POST {{baseUrl}}/api/services`
7. `POST {{baseUrl}}/api/lockers`
8. `POST {{baseUrl}}/api/boxes` with Bearer `{{token}}`
9. `POST {{baseUrl}}/api/orders` with Bearer `{{token}}`
10. `PATCH {{baseUrl}}/api/orders/{{orderId}}/status` with Bearer `{{token}}`
11. `POST {{baseUrl}}/api/payments` with Bearer `{{token}}`
12. `PATCH {{baseUrl}}/api/payments/{{paymentId}}/status` with Bearer `{{token}}`
13. `POST {{baseUrl}}/api/user/fcm-token` with Bearer `{{token}}`
14. `POST {{baseUrl}}/internal/notifications`
15. `GET {{baseUrl}}/api/notifications/user/{{userId}}` with Bearer `{{token}}`
16. `GET {{baseUrl}}/api/admin/dashboard/overview` with Bearer `{{adminToken}}`
17. WebSocket connect to `ws://localhost:8080/ws`

## 9. Debug Docker Issues

Check status:

```powershell
docker compose ps
```

Logs:

```powershell
docker compose logs -f api-gateway
docker compose logs -f auth-service
docker compose logs -f user-service
docker compose logs -f order-service
docker compose logs -f postgres
docker compose logs -f rabbitmq
```

Recent logs without following:

```powershell
docker compose logs --tail=200 api-gateway
```

Restart one service:

```powershell
docker compose restart api-gateway
docker compose restart auth-service
```

Rebuild one service:

```powershell
mvn clean package
docker compose up --build -d auth-service
```

Full reset, including deleting all local PostgreSQL data:

```powershell
docker compose down -v
docker compose up --build -d
```

Use `down -v` only when you are okay losing all local test data.

## 10. Port Conflict Debug

Check ports:

```powershell
$ports = 8080,8761,8081,8082,8083,8084,8085,8086,8087,8088,8089,8090,8091,8092,15432,5672,15672
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
  Where-Object { $ports -contains $_.LocalPort } |
  Select-Object LocalAddress,LocalPort,OwningProcess
```

Find process name:

```powershell
Get-Process -Id <PID>
```

Stop a process only if you know it is safe:

```powershell
Stop-Process -Id <PID>
```

Common conflicts:

- `8080`: another backend/gateway
- `8761`: another Eureka server
- `15432`: local PostgreSQL or old compose stack
- `5672` / `15672`: local RabbitMQ

## 11. Common Failure Notes

Docker daemon unavailable:

```powershell
docker version
```

Fix: open Docker Desktop and wait until it is running.

Missing JAR during Docker build:

```powershell
mvn clean package
docker compose up --build -d
```

Database init did not run:

```powershell
docker compose down -v
docker compose up --build -d
```

Service cannot connect to Eureka:

```powershell
docker compose logs -f discovery-server
docker compose logs -f api-gateway
```

Then open:

```powershell
Start-Process http://localhost:8761
```

401 Unauthorized:

- Login again.
- Make sure header is exactly `Authorization: Bearer <token>`.
- Admin endpoints need an `ADMIN` role token.

403 Forbidden:

- You are using a non-admin token on `/api/admin/**`.

RabbitMQ connection issue:

```powershell
docker compose ps rabbitmq
docker compose logs -f rabbitmq
Start-Process http://localhost:15672
```

PostgreSQL connection issue:

```powershell
docker compose ps postgres
docker compose logs -f postgres
docker exec -it ll-ms-postgres psql -U postgres -d postgres -c "\l"
```

## 12. Stop The Stack

Stop containers but keep DB data:

```powershell
docker compose down
```

Stop containers and delete DB volume:

```powershell
docker compose down -v
```

