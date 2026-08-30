# Runbook — Đưa hệ thống lên Azure từ số 0

Hướng dẫn tuần tự từ lúc chưa có gì trên Azure đến khi web + mobile + kiosk chạy thật.
Làm đúng thứ tự; mỗi bước có **mốc kiểm tra** — chưa qua thì đừng sang bước sau.

Tổng thời gian: khoảng **60–90 phút** (chưa tính chờ DNS).

| | Bước | Thời gian | Chặn bước sau? |
|---|---|---|---|
| 0 | [Chuẩn bị đầu vào](#0--chuẩn-bị-đầu-vào) | 15' | ✅ |
| 1 | [Cài công cụ](#1--cài-công-cụ-trên-máy-bạn) | 10' | ✅ |
| 2 | [Tạo hạ tầng Azure](#2--tạo-hạ-tầng-azure) | 5' | ✅ |
| 3 | [Cài đặt VM](#3--cài-đặt-vm) | 10' | ✅ |
| 4 | [Trỏ DNS](#4--trỏ-dns) | 5' + chờ | ✅ |
| 5 | [Chứng chỉ TLS](#5--chứng-chỉ-tls) | 3' | ✅ |
| 6 | [Điền secret trên VM](#6--điền-secret-trên-vm) | 10' | ✅ |
| 7 | [Deploy lần đầu](#7--deploy-lần-đầu) | 15' | ✅ |
| 8 | [Dữ liệu](#8--dữ-liệu-migrate-hoặc-seed) | 10' | — |
| 9 | [Cấu hình client](#9--cấu-hình-client) | 15' | — |
| 10 | [OAuth & thanh toán](#10--oauth--thanh-toán) | 10' | — |
| 11 | [Nghiệm thu](#11--nghiệm-thu) | 10' | — |

---

## 0 — Chuẩn bị đầu vào

Đây là **những thứ chỉ bạn mới có**. Gom đủ trước khi bắt đầu, thiếu một cái là kẹt giữa chừng.

| # | Cần gì | Lấy ở đâu | Ghi chú |
|---|---|---|---|
| 1 | Tài khoản Azure + subscription còn credit | [azure.microsoft.com/free/students](https://azure.microsoft.com/free/students) | Azure for Students: 100 USD/năm, **không cần thẻ** |
| 2 | Cặp SSH key **RSA** | máy bạn (bước 1) | Azure không nhận ed25519 khi tạo VM. Public key nạp vào VM, private key nạp vào GitHub |
| 3 | Quyền sửa DNS `locker-drone.tech` | Cloudflare | Cần tạo/sửa A record `api` |
| 4 | Quyền Admin repo `laundry-locker-microservices` | GitHub | Để thêm 4 secret |
| 5 | Firebase service account JSON | Firebase Console → Project settings → Service accounts → *Generate new private key* | Backend dùng verify ID token khi đăng nhập Google/Facebook/SĐT |
| 6 | Gmail app password | [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords) | Gửi OTP email. Cần bật 2FA trước |
| 7 | *(tuỳ chọn)* VNPay TMN code + hash secret | VNPay sandbox/merchant | Bỏ trống = dùng sandbox mặc định |
| 8 | *(tuỳ chọn)* MoMo partner code / access key / secret key | MoMo developer | Bỏ trống = MoMo báo `MOMO_NOT_CONFIGURED` |
| 9 | **Quyết định**: có mang dữ liệu cũ sang không? | bạn | Xem [bước 8](#8--dữ-liệu-migrate-hoặc-seed) |

Vừa làm vừa điền bảng này — các bước sau sẽ cần:

```
Subscription ID : ______________________________
Resource group  : laundry-locker-rg
VM name         : laundry-locker-vm
Public IP       : ______________________________   ← có sau bước 2
Admin user      : azureuser
Domain API      : api.locker-drone.tech
```

---

## 1 — Cài công cụ trên máy bạn

### 1.1 Azure CLI

```powershell
# Windows
winget install --exact --id Microsoft.AzureCLI
```

Mở **cửa sổ terminal mới** rồi kiểm tra:

```bash
az version
```

### 1.2 Tạo SSH key (nếu chưa có)

```bash
ssh-keygen -t rsa -b 4096 -f ~/.ssh/laundry_azure_rsa -N "" -C "laundry-locker-azure-deploy"
```

> **Phải là RSA.** `az vm create` từ chối ed25519 với lỗi *"An RSA key file or key value
> must be supplied"*. Dùng cặp key riêng cho deploy cũng an toàn hơn key cá nhân, vì
> private key này sẽ nằm trong GitHub secret.
>
> Không đặt passphrase vì GitHub Actions phải dùng key tự động, không ai gõ mật khẩu được.

### 1.3 Đăng nhập Azure

```bash
az login                                    # mở trình duyệt
az account show --output table              # xem subscription đang chọn
az account set --subscription "<tên hoặc ID>"   # nếu có nhiều subscription
```

> ✅ **Mốc kiểm tra**: `az account show` in ra đúng subscription có credit, và `cat ~/.ssh/id_ed25519.pub` ra một dòng bắt đầu bằng `ssh-ed25519`.

---

## 2 — Tạo hạ tầng Azure

Script tạo: resource group → VM Ubuntu 22.04 → IP tĩnh → network security group chỉ mở 22/80/443.

```bash
cd G:/BigProject/laundry-locker-microservices
bash infra/azure/provision-vm.sh
```

Muốn đổi mặc định:

```bash
RG=laundry-locker-rg LOCATION=eastasia VM_SIZE=Standard_B2as_v2 bash infra/azure/provision-vm.sh
```

> ⚠️ **Đừng siết `SSH_SOURCE_PREFIX` theo IP nhà** nếu còn dùng auto-deploy: GitHub
> runner có IP động, khoá SSH theo IP sẽ làm mọi lần deploy thất bại. Bảo vệ ở đây
> là xác thực bằng key — VM không bật đăng nhập mật khẩu.

Chạy lại nhiều lần không sao — cái gì có rồi thì bỏ qua.

**Ghi lại Public IP** script in ra ở cuối, điền vào bảng ở bước 0.

> ✅ **Mốc kiểm tra**: `ssh azureuser@<IP>` vào được VM (lần đầu gõ `yes` để nhận host key).

<details>
<summary>Vì sao <code>eastasia</code> và <code>Standard_B2as_v2</code>? (đo thật, không phải giả định)</summary>

**Vùng.** Subscription *Azure for Students* bị gắn policy **"Allowed resource deployment
regions"**; deploy ra ngoài danh sách sẽ lỗi `RequestDisallowedByAzure`. Danh sách đo được
tháng 08/2026: `centralindia`, `eastasia`, `uaenorth`, `malaysiawest`, `koreacentral` —
**`southeastasia` KHÔNG có trong đó**. `eastasia` (Hong Kong) gần Việt Nam nhất.
Xem danh sách của bạn:

```bash
az policy assignment list --query "[].parameters.listOfAllowedLocations.value"
```

**Kích thước.** 12 service Spring Boot (mỗi cái `-Xmx320m`) + Postgres + RabbitMQ ≈
**6,3 GB RAM** → cần 8 GB; loại 4 GB bị OOM kill lúc build image. Quota thật tại `eastasia`:

| Hạn mức | Giới hạn |
|---|---|
| Total Regional vCPUs | **6** ← ràng buộc chính |
| Standard BS Family (B2ms) | 4 |
| Standard Basv2 / Bsv2 Family | 10 |

`B2as_v2` (2 vCPU / 8 GB, AMD) còn nhiều quota dự phòng nhất.
**Tránh SKU `*ps_v2`** — đó là ARM (Ampere), image của dự án build cho amd64.
</details>

---

## 3 — Cài đặt VM

Cài Docker, swap, Nginx, Certbot và tạo khung file `.env`.

```bash
scp infra/azure/bootstrap-vm.sh azureuser@<IP>:/tmp/
ssh azureuser@<IP> 'sudo bash /tmp/bootstrap-vm.sh'
```

Script này chạy lại được và **không ghi đè** `.env` nếu bạn đã điền.

> ✅ **Mốc kiểm tra**:
> ```bash
> ssh azureuser@<IP> 'docker --version && docker compose version && free -h && systemctl is-active nginx'
> ```
> Phải thấy Docker có, swap 4 GB (dòng `Swap:`), nginx `active`.

> Lúc này Nginx chỉ có vhost **HTTP**, và như thế là đúng. Đừng tự thêm block
> `listen 443 ssl` khi chưa có chứng chỉ: `nginx -t` sẽ fail với *"no ssl_certificate
> is defined for the listen ... ssl directive"*, và plugin nginx của certbot từ chối
> chạy vì nó kiểm tra config trước — thành vòng luẩn quẩn. Bước 5 để certbot tự thêm.

---

## 4 — Trỏ DNS

Vào **Cloudflare → chọn `locker-drone.tech` → DNS → Records**:

| Type | Name | Content | Proxy status | TTL |
|---|---|---|---|---|
| `A` | `api` | `<IP Azure của bạn>` | **DNS only** (mây **xám**) | Auto |

> ⚠️ Bắt buộc **mây xám**. Để mây cam (Proxied), Cloudflare chặn HTTP-01 challenge → certbot ở bước 5 sẽ thất bại.

Nếu record `api` đã trỏ về server cũ thì **sửa IP**, đừng tạo thêm record thứ hai.

> ✅ **Mốc kiểm tra** (chờ 1–5 phút):
> ```bash
> dig +short api.locker-drone.tech
> ```
> Phải in ra **đúng IP Azure**. Còn ra IP cũ thì chờ thêm, đừng sang bước 5.

---

## 5 — Chứng chỉ TLS

```bash
ssh azureuser@<IP> 'sudo certbot --nginx -d api.locker-drone.tech'
```

Certbot hỏi:
- **Email**: điền email thật (nhận cảnh báo trước khi cert hết hạn)
- **Terms**: `Y`
- **Chia sẻ email cho EFF**: `N` tuỳ ý

Certbot tự chèn `ssl_certificate` vào vhost và reload Nginx.

> ✅ **Mốc kiểm tra**:
> ```bash
> curl -I https://api.locker-drone.tech
> ```
> Trả về HTTP code (thường `502` vì backend **chưa** chạy — đúng như mong đợi ở bước này).
> Quan trọng là **không** có lỗi chứng chỉ. Gặp `SSL certificate problem` là cert chưa xong.

Gia hạn tự động đã bật sẵn; kiểm tra bằng `ssh azureuser@<IP> 'sudo certbot renew --dry-run'`.

---

## 6 — Điền secret trên VM

```bash
ssh azureuser@<IP>
sudo nano /opt/laundry-locker-microservices/.env
```

| Biến | Điền gì | Bắt buộc |
|---|---|---|
| `API_GATEWAY_PORT` | để nguyên `8080` | ✅ |
| `APP_CORS_ALLOWED_ORIGINS` | `https://admin.locker-drone.tech,http://localhost:3000` | ✅ |
| `APP_SECURITY_JWT_SECRET` | script đã sinh sẵn chuỗi ngẫu nhiên — giữ nguyên là được | ✅ |
| `MAIL_USERNAME` | Gmail của bạn | ✅ gửi OTP |
| `MAIL_PASSWORD` | app password 16 ký tự (mục 6 bước 0) | ✅ |
| `FIREBASE_CREDENTIALS_JSON` | **nguyên nội dung file JSON, một dòng** | ✅ đăng nhập Google/FB |
| `VNPAY_*`, `MOMO_*` | có thì điền, không thì để trống | ⬜ |

Dán JSON Firebase một dòng cho gọn:

```bash
# chạy trên máy bạn, rồi copy kết quả vào .env trên VM
tr -d '\n' < duong-dan/service-account.json
```

Lưu file (`Ctrl+O`, `Enter`, `Ctrl+X`).

> ✅ **Mốc kiểm tra**: `sudo grep -c '=' /opt/laundry-locker-microservices/.env` ra số > 10, và
> `sudo stat -c '%a' /opt/laundry-locker-microservices/.env` ra `600`.

> 🔒 File này **không nằm trong git** và được deploy script giữ lại qua mỗi lần deploy.

---

## 7 — Deploy lần đầu

### 7.1 Thêm secret vào GitHub

`github.com/BaoHuy-Dev/laundry-locker-microservices` → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Giá trị |
|---|---|
| `AZURE_VM_HOST` | `<IP Azure>` |
| `AZURE_VM_USER` | `azureuser` |
| `AZURE_VM_SSH_KEY` | **toàn bộ private key**, kể cả dòng `-----BEGIN…` và `-----END…` |
| `AZURE_VM_PORT` | `22` *(tuỳ chọn)* |

Lấy private key:

```bash
cat ~/.ssh/id_ed25519          # KHÔNG phải file .pub
```

### 7.2 Chạy deploy

Vào tab **Actions → Deploy to Azure VM → Run workflow** (hoặc chỉ cần merge vào `develop`).

Workflow sẽ: `mvn clean verify` → đóng gói tarball kèm checksum → `scp` lên VM →
`docker compose up -d --build` → chờ đủ 10 service đăng ký Eureka.
**Lần đầu mất 10–15 phút** vì phải build 12 image.

Deploy hỏng ở bất kỳ đâu, script **tự rollback** về bản `.previous`.

> ✅ **Mốc kiểm tra**:
> ```bash
> curl -fsS https://api.locker-drone.tech/actuator/health
> # {"status":"UP"}
>
> ssh azureuser@<IP> 'cd /opt/laundry-locker-microservices && docker compose ps'
> # 13 container Up; ll-ms-api-gateway phải map 0.0.0.0:8080->8080/tcp
> ```

> ⚠️ Nếu gateway map `18080` thay vì `8080` → thiếu `API_GATEWAY_PORT=8080` trong `.env`. Sửa rồi
> `docker compose up -d api-gateway`.

---

## 8 — Dữ liệu: migrate hoặc seed

Chọn **một** trong hai.

### Cách A — Mang dữ liệu từ server cũ sang

Chỉ làm được nếu server cũ còn SSH vào được.

```bash
# 1) Trên server CŨ: dump toàn bộ 9 database
ssh root@<IP-server-cũ> \
  'docker exec ll-ms-postgres pg_dumpall -U postgres' > all-databases.sql

# 2) Kiểm tra file có dữ liệu thật
ls -lh all-databases.sql && head -5 all-databases.sql

# 3) Đẩy lên VM Azure
scp all-databases.sql azureuser@<IP>:/tmp/

# 4) Nạp vào Postgres mới
ssh azureuser@<IP> \
  'docker exec -i ll-ms-postgres psql -U postgres < /tmp/all-databases.sql'

# 5) Xoá file dump (chứa dữ liệu người dùng)
ssh azureuser@<IP> 'rm /tmp/all-databases.sql'
rm all-databases.sql
```

> ⚠️ Nạp dump **sau** khi backend đã chạy ít nhất một lần (Flyway tạo schema xong), và
> nên `docker compose stop` các service trước khi nạp để tránh ghi đè lẫn nhau, xong `start` lại.

### Cách B — Bắt đầu sạch bằng seed demo

Chạy **sau khi** backend đã chạy ít nhất một lần (Flyway phải tạo schema trước).

```bash
ssh azureuser@<IP>
cd /opt/laundry-locker-microservices

# Bộ đầy đủ: 4 tài khoản có tên + 100 khách + tủ/đơn/thanh toán demo. Idempotent.
docker exec -i ll-ms-postgres psql -U postgres < scripts/seed-full-demo-ms.sql
```

Tài khoản do file này tạo — **mật khẩu đều là `12345678`**:

| Vai trò | Email |
|---|---|
| ADMIN | `baohuy2k12k4@gmail.com` |
| CUSTOMER | `nqbhuy2004nt@gmail.com` |
| MAINTENANCE | `se180211nguyenquocbaohuy@gmail.com` |
| MANAGER | `huynqbse180211@fpt.edu.vn` |

Muốn thêm bộ tài khoản test gọn (`admin@laundry.test` / `Admin@123456`,
`demo@laundry.test` / `secret123`, …):

```bash
docker exec -i ll-ms-postgres psql -U postgres < scripts/seed-demo-accounts-ms.sql
```

> ⚠️ **Đừng chạy** `seed-demo-data.sql`, `seed-vietnamese-data.sql`, `verify-demo-data.sql`,
> `seed-test-user.sql`, `migrate-from-monolith-template.sql` — chúng trỏ vào
> `laundry_schema` / `staff_schema` / `laundry_locker_schema` **đã bị xoá**, chạy là lỗi.

> ✅ **Mốc kiểm tra**: đăng nhập được (field là `identifier`, **không** phải `email`):
> ```bash
> curl -s -X POST https://api.locker-drone.tech/api/auth/login \
>   -H 'Content-Type: application/json' \
>   -d '{"identifier":"baohuy2k12k4@gmail.com","password":"12345678"}'
> ```

---

## 9 — Cấu hình client

### 9.1 Web admin

⚠️ **Cái bẫy hay gặp nhất.** Vite nướng URL API vào JS **lúc build**, đọc từ `fe/.env` chứ không
phải biến trên Cloudflare.

```bash
cd G:/BigProject/laundry-locker-frontend/fe
```

Sửa `fe/.env`:

```env
VITE_API_BASE_URL=https://api.locker-drone.tech
```

> Máy bạn đang để `http://localhost:18080` để chạy backend local. **Phải đổi lại dòng này
> trước khi build production**, nếu không web admin thật sẽ gọi localhost và không có dữ liệu.

```bash
npm ci
npm run build
npx wrangler deploy
```

Chi tiết + xử lý sự cố: [`03-frontend-web.md`](../../docs/project-artifacts/guides/deploy/03-frontend-web.md).

### 9.2 Mobile

```bash
cd G:/BigProject/smart-laundry-locker-mobile
```

Sửa `.env`:

```env
API_URL=https://api.locker-drone.tech/api
API_BASE_URL=https://api.locker-drone.tech
```

```bash
dart run build_runner build --delete-conflicting-outputs
flutter clean
flutter build apk --release
```

> ⚠️ **Bắt buộc** chạy `build_runner` sau khi đổi `.env` — `envied` nướng giá trị vào
> `env_config.g.dart`. Không chạy thì app vẫn gọi URL cũ.
> ⚠️ **Không dùng** `--build-filter` (từng làm mất 35 file `.g.dart`).

Chi tiết: [`04-mobile.md`](../../docs/project-artifacts/guides/deploy/04-mobile.md).

### 9.3 Kiosk (IoT)

`smart-locker-iot/ui/vite.config.js` đã trỏ domain sẵn. Chỉ cần build lại nếu đang chạy bản cũ:

```bash
cd G:/BigProject/smart-locker-iot/ui && npm ci && npm run build
```

---

## 10 — OAuth & thanh toán

**Firebase Console → Authentication → Settings → Authorized domains** — thêm:
`locker-drone.tech`, `admin.locker-drone.tech`

**Facebook app → Settings → Basic → App Domains** — thêm `locker-drone.tech`

**VNPay / MoMo** — đổi return/IPN URL sang domain mới:

```
VNPAY_RETURN_URL  = https://api.locker-drone.tech/api/payments/vnpay/callback
MOMO_REDIRECT_URL = https://api.locker-drone.tech/api/payments/momo/return
MOMO_IPN_URL      = https://api.locker-drone.tech/api/payments/momo/callback
```

Chi tiết: [`05-oauth-firebase.md`](../../docs/project-artifacts/guides/deploy/05-oauth-firebase.md) ·
[`PAYMENT_SETUP_CHECKLIST.md`](../../docs/project-artifacts/guides/PAYMENT_SETUP_CHECKLIST.md)

---

## 11 — Nghiệm thu

Chạy hết, tất cả phải xanh:

```bash
# 1. TLS hợp lệ, không cảnh báo cert
curl -I https://api.locker-drone.tech

# 2. Gateway sống
curl -fsS https://api.locker-drone.tech/actuator/health          # {"status":"UP"}

# 3. Endpoint public trả dữ liệu
curl -fsS https://api.locker-drone.tech/api/lockers | head -c 200

# 4. Endpoint cần quyền trả 401 (đúng — nghĩa là service sống và có bảo vệ)
curl -s -o /dev/null -w '%{http_code}\n' https://api.locker-drone.tech/api/admin/users

# 5. Đăng nhập được (tài khoản từ seed-full-demo-ms.sql)
curl -s -X POST https://api.locker-drone.tech/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"identifier":"baohuy2k12k4@gmail.com","password":"12345678"}'

# 6. Đủ container
ssh azureuser@<IP> 'cd /opt/laundry-locker-microservices && docker compose ps | grep -c Up'   # = 13
```

- [ ] Web admin `https://admin.locker-drone.tech` đăng nhập được, các trang list có dữ liệu
- [ ] Mobile build mới gọi API thật, đăng nhập được
- [ ] Đăng nhập Google/Facebook không báo "unauthorized domain"
- [ ] Merge một commit nhỏ vào `develop` → Actions tự deploy thành công

---

## 12 — Vận hành hằng ngày

```bash
ssh azureuser@<IP>
cd /opt/laundry-locker-microservices

docker compose ps                      # trạng thái
docker compose logs -f api-gateway     # xem log một service
docker compose logs --tail=200 order-service
docker compose restart order-service   # restart một service
docker compose up -d --build           # deploy tay (không qua Actions)
```

**Nối database từ máy bạn** — qua SSH tunnel, không mở port ra Internet:

```bash
ssh -L 15432:127.0.0.1:15432 -N azureuser@<IP>
# giữ cửa sổ này, mở DBeaver/psql trỏ vào localhost:15432, user postgres
```

**Sao lưu database**:

```bash
ssh azureuser@<IP> 'docker exec ll-ms-postgres pg_dumpall -U postgres' \
  > backup-$(date +%Y%m%d).sql
```

**Quay lại bản deploy trước** (deploy script tự giữ một bản):

```bash
ssh azureuser@<IP>
cd /opt
sudo rm -rf laundry-locker-microservices
sudo mv laundry-locker-microservices.previous laundry-locker-microservices
cd laundry-locker-microservices && docker compose up -d --build
```

---

## 13 — Sự cố thường gặp

| Triệu chứng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `502 Bad Gateway` | Backend chưa chạy, hoặc gateway nằm ở port 18080 | `docker compose ps` — gateway phải là `0.0.0.0:8080->8080`. Thiếu thì thêm `API_GATEWAY_PORT=8080` vào `.env` |
| Certbot thất bại | DNS chưa trỏ đúng, hoặc Cloudflare để **mây cam** | `dig +short api.locker-drone.tech` phải ra IP Azure; đổi record sang **DNS only** |
| Web admin trắng / không có dữ liệu | `fe/.env` còn `localhost` lúc build | Sửa `.env` → `npm run build` → `wrangler deploy` |
| Trình duyệt báo **Mixed Content** | Web HTTPS gọi API `http://` | API phải là `https://api.locker-drone.tech` |
| Lỗi **CORS** khi web gọi API | Thiếu domain trong `APP_CORS_ALLOWED_ORIGINS` | Thêm `https://admin.locker-drone.tech` vào `.env` rồi `docker compose up -d api-gateway` |
| Mobile vẫn gọi URL cũ | Chưa chạy lại `build_runner` | `dart run build_runner build --delete-conflicting-outputs` rồi `flutter clean` |
| Container bị kill, deploy dở dang | Hết RAM | `free -h` xem swap có bật; cân nhắc nâng lên `Standard_B4ms` |
| Deploy chạy mãi rồi timeout | Có service không đăng ký được Eureka | `docker compose logs discovery-server` và log service đó |
| Không SSH được sau khi đổi `SSH_SOURCE_PREFIX` | NSG chỉ cho IP cũ, mà IP nhà bạn đổi | Azure Portal → NSG `laundry-locker-vmNSG` → sửa rule `allow-ssh` |

Bộ sự cố đầy đủ đã gặp thời server cũ: [`06-troubleshooting.md`](../../docs/project-artifacts/guides/deploy/06-troubleshooting.md).

---

## 14 — Chi phí

| Hạng mục | Ước tính/tháng |
|---|---|
| VM `Standard_B2ms` (2 vCPU, 8 GB) | ~30–35 USD |
| Disk StandardSSD 64 GB | ~5 USD |
| IP tĩnh Standard | ~3 USD |
| **Tổng** | **~40 USD** |

Credit sinh viên 100 USD chạy được khoảng **2,5 tháng**. Tiết kiệm khi không demo:

```bash
az vm deallocate --resource-group laundry-locker-rg --name laundry-locker-vm   # ngừng tính tiền compute
az vm start      --resource-group laundry-locker-rg --name laundry-locker-vm   # bật lại
```

> IP tĩnh nên **không đổi** sau khi bật lại, DNS giữ nguyên. Disk vẫn tính tiền lúc tắt.

Theo dõi credit: [portal.azure.com](https://portal.azure.com) → *Cost Management + Billing*.
Nên đặt **budget alert** ở mức 50 USD.

---

## Phụ lục — Xoá sạch để làm lại

```bash
az group delete --name laundry-locker-rg --yes --no-wait
```

Xoá cả resource group: VM, disk, IP, NSG đi hết. Sau đó làm lại từ [bước 2](#2--tạo-hạ-tầng-azure).
