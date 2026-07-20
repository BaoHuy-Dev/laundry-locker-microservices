# RUN_ALL_GUIDE.md — Hướng dẫn chạy toàn bộ dự án trên Windows PowerShell

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho
> locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng
> Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`,
`RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

> Workspace: `G:\BigProject` (lưu ý: ổ **G:**, không phải D:)

## 0. Yêu cầu môi trường

| Công cụ                | Phiên bản tối thiểu | Kiểm tra             | Trạng thái máy hiện tại                                    |
|------------------------|---------------------|----------------------|------------------------------------------------------------|
| Java JDK               | 21                  | `java -version`      | ✅ 21.0.11                                                  |
| Maven                  | 3.9+                | `mvn -version`       | ✅ 3.9.16                                                   |
| Docker Desktop         | 24+                 | `docker info`        | ✅ 29.5.3                                                   |
| Node.js (frontend)     | 20 LTS+             | `node -v`            | ✅ v24.16.0 (winget; mở terminal mới để có PATH)            |
| Flutter (mobile)       | 3.x                 | `flutter --version`  | ✅ 3.44.2 tại `C:\flutter` (đã thêm vào user PATH)          |
| Android SDK + emulator | API 34+             | `flutter doctor`     | ✅ SDK 36.1.0 + AVD `Pixel_8` có sẵn; `ANDROID_HOME` đã set |
| Python                 | 3.12+               | `py -3.12 --version` | ✅ 3.12.10 (winget); IoT dùng Python 3.13.5 do uv quản lý   |
| uv (IoT)               | 0.5+                | `uv --version`       | ✅ 0.11.21 (winget); đã `uv sync` xong cho smart-locker-iot |

## 1. Cách nhanh nhất — script tổng

```powershell
cd G:\BigProject
.\run-all.ps1            # build + chạy backend (Docker) + frontend
.\run-all.ps1 -SkipBuild # nếu đã build JAR rồi
.\run-all.ps1 -BackendOnly
```

Dừng toàn bộ:

```powershell
.\stop-all.ps1           # giữ data Postgres
.\stop-all.ps1 -Purge    # xóa luôn volume DB
```

## 2. Chạy thủ công từng bước

### Bước 1 — Hạ tầng (Postgres + RabbitMQ)

```powershell
cd G:\BigProject\laundry-locker-microservices
docker compose up -d postgres rabbitmq
docker ps   # chờ cả 2 ở trạng thái (healthy)
```

### Bước 2 — Build backend

```powershell
mvn clean package -DskipTests -T 1C
```

### Bước 3 — Chạy backend source-backed services

```powershell
docker compose up --build -d
```

Lưu ý: `laundry-service` và `partner-service` có trong docker-compose.yml nhưng **chưa có source code** — file
`docker-compose.override.yml` (đã tạo sẵn) tự loại chúng khỏi `docker compose up`. Khi nào 2 module này được viết, xóa
file override đi.

Thứ tự tự xử lý qua `depends_on`: postgres/rabbitmq → discovery-server (8761) → api-gateway (8080) → 10 service nghiệp
vụ có source. Chờ ~60–90 giây để các service đăng ký Eureka (máy chậm có thể tới 3 phút).

Kiểm tra:

```powershell
docker ps                                               # tất cả Up
Invoke-RestMethod http://localhost:8080/actuator/health # {"status":"UP"}
start http://localhost:8761                             # Eureka dashboard
```

### Bước 4 — Frontend web

```powershell
cd G:\BigProject\laundry-locker-frontend\fe
npm install     # lần đầu
npm run dev     # http://localhost:3000 (vite.config.ts đặt port 3000, strictPort)
```

API URL đã trỏ đúng gateway trong `fe\.env`: `VITE_API_BASE_URL=http://localhost:8080`.

### Bước 5 — Mobile Flutter

`flutter pub get` và `build_runner` **đã được chạy sẵn**. Để chạy app (mở terminal PowerShell MỚI để có PATH):

```powershell
cd G:\BigProject\smart-laundry-locker-mobile
flutter emulators --launch Pixel_8   # khởi động Android emulator có sẵn
flutter run                          # lần đầu Gradle sẽ tải dependency, hơi lâu
```

Nếu đổi `.env` thì chạy lại: `dart run build_runner build --delete-conflicting-outputs`.

`.env` đã được bổ sung `API_BASE_URL=http://10.0.2.2:8080` (alias localhost cho Android emulator — code envied đọc biến
này, không phải `API_URL`). Máy thật: đổi thành IP LAN của PC, ví dụ `http://192.168.1.x:8080`.

### Bước 6 — IoT (chạy thật cần Raspberry Pi + Arduino)

Môi trường Python đã sẵn (`uv sync` đã chạy, Python 3.13.5 managed bởi uv):

```powershell
cd G:\BigProject\smart-locker-iot
uv run python main.py   # cần serial port Arduino; trên PC không có phần cứng sẽ báo lỗi serial
```

IoT kết nối MQTT broker (mặc định `localhost:8883` TLS, override bằng env `MQTT_BROKER`). Backend iot-service mặc định
dùng `tcp://broker.hivemq.com:1883` (public broker) — hai bên phải trỏ cùng broker để lệnh mở tủ hoạt động.

## 3. Smoke test nhanh

```powershell
# Đăng ký + đăng nhập (login dùng field "identifier", không phải "email")
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/register -ContentType "application/json" -Body '{"email":"demo@laundry.test","phoneNumber":"0900000000","firstName":"Demo","lastName":"User","password":"secret123","roles":["CUSTOMER"]}'
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -ContentType "application/json" -Body '{"identifier":"demo@laundry.test","password":"secret123"}'
$token = $login.data.accessToken
Invoke-RestMethod -Uri http://localhost:8080/api/orders/my-orders -Headers @{Authorization="Bearer $token"}
```

Thêm kịch bản đầy đủ (store → locker → box → service → order → payment) trong `laundry-locker-microservices\README.md`
mục "Main API Smoke Test".

## 4. Xử lý sự cố thường gặp

| Triệu chứng                             | Nguyên nhân                           | Cách xử lý                                                                                              |
|-----------------------------------------|---------------------------------------|---------------------------------------------------------------------------------------------------------|
| Port 8080/8761/15432/5672/3000 bị chiếm | Process khác đang dùng                | `Get-NetTCPConnection -LocalPort 8080 -State Listen \| Select OwningProcess` → `Stop-Process -Id <PID>` |
| Container service restart liên tục      | DB/Rabbit chưa healthy hoặc thiếu RAM | `docker compose logs <service> --tail 50`; Docker Desktop cần ≥ 6 GB RAM cho 14 container               |
| Gateway trả 503                         | Service chưa đăng ký Eureka           | Chờ thêm 30–60s, xem http://localhost:8761                                                              |
| `mvn package` lỗi                       | Code thay đổi                         | Xem module lỗi trong log; build lại từng module: `mvn package -pl <module> -am -DskipTests`             |
| Frontend gọi API bị CORS/401            | Sai token hoặc gateway chưa chạy      | Kiểm tra `fe\.env` trỏ `http://localhost:8080`; login lại                                               |
| `npm`/`node` not found sau khi cài      | PATH chưa refresh                     | Mở terminal PowerShell **mới**                                                                          |

## 5. Tệp báo cáo liên quan

- `laundry-locker-microservices\docs\CURRENT_PROJECT_STATUS.md` — nguồn trạng thái chuẩn hiện tại của toàn dự án.
- `PROJECT_FLOW.md` — luồng nghiệp vụ + kỹ thuật chi tiết.
- `RUN_RESULT.md` — kết quả lần chạy gần nhất, danh sách file đã sửa.
- `LOCKER_FLOW_PLAN.md` — tiến độ Phase 1/2/3 của locker flow.
