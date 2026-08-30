# Nhật ký dựng Azure — làm thật, từng lệnh một

Tài liệu này ghi lại **chính xác những gì đã được thực hiện** để đưa backend Smart Laundry
Locker từ DigitalOcean sang Azure, viết dưới dạng **thao tác thủ công** để bạn tự làm lại
được từ đầu, kèm giải thích vì sao mỗi bước tồn tại.

Khác với [RUNBOOK.md](RUNBOOK.md) (hướng dẫn tổng quát dùng script), file này là **bản ghi
thực tế**: có cả những chỗ đã vấp và cách xử lý.

- **Ngày thực hiện:** 30/08/2026
- **Kết quả:** `https://api.locker-drone.tech` chạy trên Azure VM, TLS hợp lệ, 13 container Up;
  web admin, landing page và bản web của app mobile đều tự deploy khi push nhánh chính
- **Tài khoản Azure:** `nhatkse182290@fpt.edu.vn` · subscription *Azure for Students*

---

## Tóm tắt nhanh

| | Hạng mục | Trạng thái |
|---|---|---|
| ✅ | Azure CLI, đăng nhập, đăng ký resource provider | xong |
| ✅ | VM + IP tĩnh + NSG (chỉ 22/80/443) | xong |
| ✅ | Docker, swap 4 GB, Nginx, Certbot, ufw | xong |
| ✅ | DNS `api.locker-drone.tech` → `20.24.196.177` | xong (bạn làm) |
| ✅ | TLS Let's Encrypt + redirect HTTP→HTTPS | xong |
| ✅ | Firebase service-account trong `.env` | xong |
| ✅ | Build + deploy 12 service | xong |
| ✅ | Seed dữ liệu đầy đủ 36 bảng | xong |
| ✅ | 4 secret `AZURE_VM_*` trên GitHub | xong (bạn làm) |
| ✅ | Gmail app password — OTP gửi được | xong |
| ✅ | Push nhánh + mở PR ở cả 4 repo | xong |
| ✅ | CI/CD: push nhánh chính là tự deploy cả 3 phần | xong |
| ✅ | Mọi lần deploy tự ghi vào `DEPLOY-LOG.md` | xong |
| ✅ | App mobile xem được trên trình duyệt (Flutter Web) | xong |
| ✅ | Merge cả 7 PR — pipeline đã chạy thật một vòng | xong |
| ✅ | Vá 2 CVE mức CRITICAL trong ảnh container | xong |
| ✅ | 2 secret Cloudflare + Worker `laundry-locker-mobile-web` | xong |
| ✅ | `https://app.locker-drone.tech` — bản web của app mobile | xong |
| ⬜ | Bật Dependency graph cho repo backend | **cần bạn** |
| ⬜ | Chọn ngưỡng cho `container-scan` | **cần bạn quyết** |

---

## 1 — Chuẩn bị máy của bạn

### 1.1 Cài Azure CLI

```powershell
winget install --exact --id Microsoft.AzureCLI
```

Sau khi cài phải **mở terminal mới** thì `az` mới vào PATH. Kiểm tra: `az version`
(bản đã dùng: 2.89.1).

### 1.2 Tạo cặp khoá SSH **RSA**

```bash
ssh-keygen -t rsa -b 4096 -f ~/.ssh/laundry_azure_rsa -N "" -C "laundry-locker-azure-deploy"
```

> **Vì sao RSA mà không phải ed25519?** Máy này đã có sẵn `~/.ssh/id_ed25519` nhưng
> `az vm create` từ chối với lỗi *"An RSA key file or key value must be supplied to SSH Key
> Value"*. Azure chỉ nhận RSA khi tạo VM.
>
> **Vì sao khoá riêng, không dùng khoá cá nhân?** Khoá riêng (private key) của cặp này sẽ
> được dán vào GitHub secret để CI tự SSH. Tách khỏi khoá cá nhân nghĩa là nếu cần thu hồi
> quyền deploy thì chỉ xoá khoá này, không ảnh hưởng các máy khác bạn đang SSH.
>
> **Vì sao `-N ""` (không đặt passphrase)?** GitHub Actions chạy không người trực, không ai
> gõ được mật khẩu khoá.

### 1.3 Đăng nhập Azure

```bash
az login --use-device-code
```

Lệnh in ra một mã, mở https://login.microsoft.com/device rồi nhập mã và chọn tài khoản.
Dùng `--use-device-code` vì cách này không cần trình duyệt chạy cùng máy với terminal.

---

## 2 — Kiểm tra subscription **trước khi** tạo gì

Đây là phần quan trọng nhất mà một hướng dẫn chung chung hay bỏ qua. Subscription sinh viên
có ba ràng buộc có thể làm hỏng cả kế hoạch, và cả ba đều kiểm tra được trong 2 phút.

### 2.1 Resource provider — mặc định CHƯA bật

```bash
az provider show -n Microsoft.Compute --query registrationState -o tsv
az provider show -n Microsoft.Network --query registrationState -o tsv
az provider show -n Microsoft.Storage --query registrationState -o tsv
```

Subscription mới trả về `NotRegistered` cả ba → **tạo VM sẽ lỗi**. Bật lên:

```bash
az provider register -n Microsoft.Compute
az provider register -n Microsoft.Network
az provider register -n Microsoft.Storage
```

Mất khoảng 1–3 phút. Chờ tới khi cả ba thành `Registered` rồi mới đi tiếp.

### 2.2 Vùng được phép — subscription sinh viên bị giới hạn

```bash
az policy assignment list --query "[].parameters.listOfAllowedLocations.value"
```

Kết quả thực tế:

```
centralindia · eastasia · uaenorth · malaysiawest · koreacentral
```

> **`southeastasia` KHÔNG có trong danh sách.** Lần đầu tôi tạo VM ở `southeastasia` và bị
> chặn với lỗi `RequestDisallowedByAzure: This policy maintains a set of best available
> regions where your subscription can deploy resources`. Đã chuyển sang **`eastasia`**
> (Hong Kong) — gần Việt Nam nhất trong danh sách được phép.

### 2.3 Hạn mức vCPU — quyết định chọn máy nào

```bash
az vm list-usage --location eastasia -o table
```

Số thực tế:

| Hạn mức | Giới hạn |
|---|---|
| Total Regional vCPUs | **6** ← ràng buộc chính |
| Standard BS Family (B2ms, B4ms) | 4 |
| Standard Basv2 Family (B2as_v2) | 10 |
| Standard Bsv2 Family (B2s_v2) | 10 |

Xem máy nào thật sự có ở vùng đó:

```bash
az vm list-sizes --location eastasia -o table
```

> **Vì sao chọn `Standard_B2as_v2` (2 vCPU / 8 GB)?**
> 12 service Spring Boot, mỗi cái `-Xmx320m`, cộng Postgres và RabbitMQ ≈ **6,3 GB RAM**.
> Loại 4 GB sẽ bị OOM kill ngay lúc build image. Trong các loại 8 GB thì `B2as_v2` (AMD)
> nằm ở họ Basv2 có quota 10 vCPU — rộng nhất, còn chỗ nếu sau này cần thêm máy.
>
> **Tuyệt đối tránh các SKU đuôi `ps_v2`** (`B2ps_v2`, `B4ps_v2`): đó là ARM (Ampere), trong
> khi image của dự án build cho amd64 — container sẽ không chạy.

---

## 3 — Tạo hạ tầng

Script [`provision-vm.sh`](provision-vm.sh) gói trọn phần này. Dưới đây là **các lệnh thủ
công tương đương** nếu bạn muốn tự gõ.

```bash
RG=laundry-locker-rg
LOCATION=eastasia
VM=laundry-locker-vm

# 3.1 Resource group — chỉ là "thư mục" gom tài nguyên, xoá nó là xoá sạch mọi thứ bên trong
az group create --name $RG --location $LOCATION

# 3.2 Máy ảo
az vm create \
  --resource-group $RG \
  --name $VM \
  --image Ubuntu2204 \
  --size Standard_B2as_v2 \
  --admin-username azureuser \
  --ssh-key-values ~/.ssh/laundry_azure_rsa.pub \
  --public-ip-sku Standard \
  --os-disk-size-gb 64 \
  --storage-sku StandardSSD_LRS
```

> `--ssh-key-values` nạp **khoá công khai** vào VM. Azure không bật đăng nhập bằng mật khẩu
> khi tạo VM theo cách này — nghĩa là chỉ ai giữ khoá riêng mới SSH được.
> `--os-disk-size-gb 64` vì 30 GB mặc định sẽ chật khi Docker chứa 12 image.

```bash
# 3.3 Đổi IP public sang TĨNH
NIC_ID=$(az vm show -g $RG -n $VM --query 'networkProfile.networkInterfaces[0].id' -o tsv)
IP_ID=$(az network nic show --ids "$NIC_ID" --query 'ipConfigurations[0].publicIPAddress.id' -o tsv)
az network public-ip update --ids "$IP_ID" --allocation-method Static
```

> **Vì sao phải tĩnh?** IP động sẽ đổi mỗi lần `az vm deallocate` rồi bật lại — và bạn sẽ
> phải sửa DNS lẫn GitHub secret mỗi lần như vậy. IP tĩnh tốn ~3 USD/tháng, đáng.
>
> ⚠️ Nếu bạn chạy từ **Git Bash trên Windows**, hai lệnh trên sẽ hỏng với lỗi
> `invalid resource ID: C:/Program Files/Git/subscriptions/...`. MSYS tự đổi tham số bắt đầu
> bằng `/` thành đường dẫn Windows. Khắc phục: `export MSYS_NO_PATHCONV=1` trước khi chạy.

```bash
# 3.4 Tường lửa Azure (NSG) — chỉ mở 3 cổng
NSG=$(az network nic show --ids "$NIC_ID" --query 'networkSecurityGroup.id' -o tsv | awk -F/ '{print $NF}')
az network nsg rule create -g $RG --nsg-name $NSG --name allow-ssh   --priority 1001 \
  --access Allow --protocol Tcp --direction Inbound --destination-port-ranges 22
az network nsg rule create -g $RG --nsg-name $NSG --name allow-http  --priority 1002 \
  --access Allow --protocol Tcp --direction Inbound --destination-port-ranges 80
az network nsg rule create -g $RG --nsg-name $NSG --name allow-https --priority 1003 \
  --access Allow --protocol Tcp --direction Inbound --destination-port-ranges 443
```

> **Vì sao không mở 8080?** Nginx trên VM nhận 443 rồi chuyển tiếp vào `127.0.0.1:8080`.
> Gateway chỉ nghe bên trong máy, không lộ ra Internet — đây là điểm khác biệt so với server
> DigitalOcean cũ (nó mở 8080 công khai).
>
> **Vì sao SSH mở cho mọi IP?** Vì workflow deploy chạy trên GitHub-hosted runner với IP
> động. Khoá SSH theo IP nhà sẽ làm mọi lần auto-deploy thất bại. Bảo vệ ở đây là xác thực
> bằng khoá, không phải mật khẩu.

Lấy IP để dùng cho các bước sau:

```bash
az network public-ip show --ids "$IP_ID" --query ipAddress -o tsv
# 20.24.196.177
```

---

## 4 — Cài đặt bên trong VM

Script [`bootstrap-vm.sh`](bootstrap-vm.sh) làm trọn phần này:

```bash
scp -i ~/.ssh/laundry_azure_rsa infra/azure/bootstrap-vm.sh azureuser@20.24.196.177:/tmp/
ssh -i ~/.ssh/laundry_azure_rsa azureuser@20.24.196.177 'sudo bash /tmp/bootstrap-vm.sh'
```

Nội dung nó làm và lý do:

| Việc | Vì sao |
|---|---|
| Cài Docker Engine + compose plugin | Chạy nguyên `docker-compose.yml` của dự án |
| Giới hạn log Docker (10 MB × 3 file) | Không có giới hạn thì log 12 service sẽ ăn hết đĩa sau vài tuần |
| **Tạo swap 4 GB** | RAM 8 GB vừa đủ; swap là lưới an toàn để lúc build image không bị OOM kill giữa chừng |
| Cài Nginx + Certbot | Nginx làm lớp TLS; certbot xin và tự gia hạn chứng chỉ |
| Tạo `/opt/laundry-locker-microservices` + `.env` (chmod 600) | Nơi chứa code và secret vận hành |
| `chgrp azureuser /opt && chmod 2775 /opt` | Deploy script hoán đổi thư mục kiểu atomic — cần quyền tạo `.new`/`.previous` **trong** `/opt` |
| `ufw allow OpenSSH` + `Nginx Full` | Lớp tường lửa thứ hai bên trong máy |

Kiểm tra:

```bash
ssh -i ~/.ssh/laundry_azure_rsa azureuser@20.24.196.177 \
  'docker compose version && free -h && systemctl is-active nginx'
```

---

## 5 — DNS (phần bạn đã làm)

Cloudflare → `locker-drone.tech` → DNS → Records:

| Type | Name | Content | Proxy |
|---|---|---|---|
| `A` | `api` | `20.24.196.177` | **DNS only** (mây xám) |

> **Bắt buộc mây xám.** Nếu để Proxied (mây cam), Cloudflare đứng giữa và chặn HTTP-01
> challenge, certbot sẽ không xin được chứng chỉ.

Kiểm tra trước khi sang bước sau:

```bash
nslookup api.locker-drone.tech 8.8.8.8   # phải ra 20.24.196.177
```

---

## 6 — Chứng chỉ TLS

```bash
ssh -i ~/.ssh/laundry_azure_rsa azureuser@20.24.196.177 \
  'sudo certbot --nginx -d api.locker-drone.tech --non-interactive --agree-tos \
     -m nqbhuy2004nt@gmail.com --redirect'
```

Certbot tự: xin chứng chỉ → chèn `ssl_certificate` vào vhost → thêm block `listen 443 ssl`
→ đổi block 80 thành redirect sang HTTPS → đặt lịch tự gia hạn.

Kết quả: chứng chỉ hết hạn **28/11/2026**, tự gia hạn nền.

Kiểm tra:

```bash
curl -I https://api.locker-drone.tech        # TLS hợp lệ (502 lúc này là đúng, backend chưa chạy)
curl -I http://api.locker-drone.tech         # 301 sang https
```

---

## 7 — Secret vận hành trên VM

File `/opt/laundry-locker-microservices/.env` (chmod 600, **không nằm trong git**).
Deploy script cố ý giữ lại file này qua mỗi lần deploy, nên đây là nơi đặt secret thật.

**Firebase service-account** (đã nạp): dùng file có sẵn trong workspace
`secrets/laundry-locker-19a9d-firebase-adminsdk.json` — đúng project `laundry-locker-19a9d`
mà app mobile đang dùng (đối chiếu `android/app/google-services.json`).

Cách nạp — chuyển file lên rồi ép về một dòng ngay trên VM, để private key không lọt vào
lịch sử lệnh:

```bash
scp -i ~/.ssh/laundry_azure_rsa secrets/laundry-locker-19a9d-firebase-adminsdk.json \
    azureuser@20.24.196.177:/tmp/fb.json

ssh -i ~/.ssh/laundry_azure_rsa azureuser@20.24.196.177 '
ENVF=/opt/laundry-locker-microservices/.env
ONE=$(python3 -c "import json;print(json.dumps(json.load(open(\"/tmp/fb.json\")),separators=(\",\",\":\")))")
sudo sed -i "/^FIREBASE_CREDENTIALS_JSON=/d" $ENVF
printf "FIREBASE_CREDENTIALS_JSON='"'"'%s'"'"'\n" "$ONE" | sudo tee -a $ENVF >/dev/null
rm -f /tmp/fb.json'
```

> **Vì sao bọc nháy đơn?** File `.env` của docker compose sẽ nội suy `$` và cắt chuỗi ở `#`
> nếu để trần. Nháy đơn giữ nguyên toàn bộ JSON.

---

## 8 — Build và deploy

Đây chính là những gì workflow [`deploy-azure.yml`](../../.github/workflows/deploy-azure.yml)
làm tự động; dưới đây là bản thủ công.

```bash
cd G:/BigProject/laundry-locker-microservices

# 8.1 Build 12 jar
mvn clean package -DskipTests

# 8.2 Đóng gói (Dockerfile chỉ COPY jar sẵn, không build từ nguồn -> build trên VM rất nhanh)
tar --exclude='./.git' --exclude='./docs' -czf /tmp/ll.tar.gz .

# 8.3 Đẩy lên VM
scp -i ~/.ssh/laundry_azure_rsa /tmp/ll.tar.gz \
    azureuser@20.24.196.177:/tmp/laundry-locker-microservices.tar.gz
ssh -i ~/.ssh/laundry_azure_rsa azureuser@20.24.196.177 \
    'cd /tmp && sha256sum laundry-locker-microservices.tar.gz > laundry-locker-microservices.tar.gz.sha256'

# 8.4 Chạy script deploy trên VM
tr -d '\r' < scripts/deploy-from-artifact.sh \
  | ssh -i ~/.ssh/laundry_azure_rsa azureuser@20.24.196.177 'bash -s'
```

> **Vì sao `tr -d '\r'`?** Working tree trên Windows là CRLF. Đẩy thẳng vào `bash -s` sẽ lỗi
> `set: pipefail: invalid option name` vì bash đọc phải `pipefail\r`. Trên GitHub Actions
> (runner Linux) không dính lỗi này vì git lưu LF.

Script deploy làm: kiểm checksum → giải nén ra `.new` → **giữ lại `.env` cũ** → `docker
compose config` để bắt lỗi cú pháp → đổi tên thư mục cũ thành `.previous` → đưa `.new` vào
chỗ cũ → `docker compose up -d --build` → chờ discovery-server và gateway healthy → chờ đủ
10 service đăng ký Eureka. Hỏng ở bất kỳ đâu thì **tự rollback** về `.previous`.

---

## 9 — Nạp dữ liệu

```bash
tr -d '\r' < scripts/seed-local-complete.sql \
  | ssh -i ~/.ssh/laundry_azure_rsa azureuser@20.24.196.177 \
      'docker exec -i ll-ms-postgres psql -U postgres -v ON_ERROR_STOP=1'
```

Bộ seed xoá sạch rồi nạp lại **36 bảng** với dữ liệu liên kết chéo nhất quán. Kiểm chứng
bằng [`verify-local-seed.sql`](../../scripts/verify-local-seed.sql).

---

## 10 — Nghiệm thu

```bash
curl -fsS https://api.locker-drone.tech/actuator/health          # {"status":"UP"}
curl -fsS https://api.locker-drone.tech/api/lockers | head -c 120
curl -s -o /dev/null -w '%{http_code}\n' https://api.locker-drone.tech/api/admin/users   # 401 = đúng
curl -s -o /dev/null -w '%{http_code}\n' https://api.locker-drone.tech/user-service/api/admin/users  # 404 = đúng

curl -s -X POST https://api.locker-drone.tech/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"identifier":"baohuy2k12k4@gmail.com","password":"12345678"}'
```

> Field đăng nhập là `identifier`, **không phải** `email`.

---

## 11 — Năm lỗi đã gặp và cách xử lý

Ghi lại để lần sau không mất thời gian, và vì **hai trong số đó là bug thật trong repo**.

### 11.1 `RequestDisallowedByAzure` khi tạo VM
**Triệu chứng:** mọi tài nguyên (VNET, NSG, IP, VM) đều bị từ chối.
**Nguyên nhân:** subscription sinh viên bị policy giới hạn vùng; `southeastasia` không được phép.
**Xử lý:** đọc danh sách bằng `az policy assignment list`, chuyển sang `eastasia`.

### 11.2 `An RSA key file or key value must be supplied`
**Nguyên nhân:** `az vm create` không nhận khoá ed25519.
**Xử lý:** tạo cặp RSA-4096 riêng cho deploy.

### 11.3 `invalid resource ID: C:/Program Files/Git/subscriptions/...`
**Nguyên nhân:** Git Bash/MSYS tự đổi tham số bắt đầu bằng `/` thành đường dẫn Windows.
**Xử lý:** `export MSYS_NO_PATHCONV=1` — đã thêm sẵn vào đầu `provision-vm.sh`.

### 11.4 🐞 Certbot không chạy được — **bug trong `bootstrap-vm.sh`**
**Triệu chứng:** `nginx: [emerg] no "ssl_certificate" is defined for the "listen ... ssl"
directive` → plugin nginx của certbot từ chối làm việc.
**Nguyên nhân:** script viết sẵn block `listen 443 ssl` khi chưa có chứng chỉ, làm `nginx -t`
fail; mà certbot lại kiểm tra config trước khi chạy → vòng luẩn quẩn.
**Đã sửa:** vhost ban đầu chỉ có block HTTP; certbot tự thêm 443 + redirect.

### 11.5 🐞 Deploy luôn rollback oan — **bug trong `deploy-from-artifact.sh`**
**Triệu chứng:** `sha256sum: laundry-locker-microservices.tar.gz: No such file or directory`
→ `Deploy failed. Rolling back...`
**Nguyên nhân:** `sha256sum -c` giải tên file **theo thư mục hiện tại**. Script chạy qua
`ssh 'bash -s'` nên cwd là `$HOME`, còn artifact nằm ở `/tmp`.
**Đã sửa:** kiểm tra checksum trong đúng thư mục chứa artifact.
**Lưu ý:** bug này sẽ xảy ra **y hệt trên GitHub Actions** — nếu chưa sửa thì auto-deploy
không bao giờ chạy được.

### 11.6 Không tạo được `/opt/...new`
**Nguyên nhân:** `azureuser` sở hữu thư mục app nhưng không có quyền ghi vào `/opt`, trong
khi deploy hoán đổi thư mục kiểu atomic cần tạo hai thư mục anh em trong `/opt`.
**Đã sửa:** `chgrp azureuser /opt && chmod 2775 /opt` (cấp qua nhóm, không đổi chủ `/opt`).

---

## 12 — Hiện trạng

### Tài nguyên Azure — resource group `laundry-locker-rg` (eastasia)

| Tài nguyên | Giá trị |
|---|---|
| VM | `laundry-locker-vm` · `Standard_B2as_v2` · Ubuntu 22.04 LTS gen2 |
| Tài nguyên | 2 vCPU · 7,8 GB RAM · 62 GB đĩa |
| IP tĩnh | `20.24.196.177` |
| NSG | 22 / 80 / 443 |
| Đang dùng | RAM 3,8/7,8 GB · swap 0/4 GB · đĩa 15/62 GB |

### Đang chạy

13 container: `postgres`, `rabbitmq`, `discovery-server`, `api-gateway` + 9 service nghiệp vụ.
Gateway ở host `8080`, chỉ Nginx gọi tới.

### Dữ liệu

| Bảng | Số bản ghi |
|---|---|
| `user_profiles` | 12 |
| `orders` | 24 |
| `lockers` / `locker_boxes` | 4 / 37 |
| `payments` | 29 |
| `notifications` | 35 |

### Tài khoản (mật khẩu đều `12345678`)

| Vai trò | Email | Dùng ở |
|---|---|---|
| ADMIN | `baohuy2k12k4@gmail.com` | web admin |
| CUSTOMER | `nqbhuy2004nt@gmail.com` | mobile khách |
| MAINTENANCE | `se180211nguyenquocbaohuy@gmail.com` | mobile đội drone |
| TECHNICIAN | `huynqbse180211@fpt.edu.vn` | mobile kỹ thuật / quản lý vận hành |

### Biến `.env` còn trống

`VNPAY_*`, `MOMO_*` — hai cổng thanh toán thật. Thiếu chúng thì luồng ví nội bộ và
tiền mặt vẫn chạy đủ; chỉ VNPay/MoMo là không gọi được.

`SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` **đã điền** và OTP gửi được thật.
Tên biến phải có tiền tố `SPRING_` — `docker-compose.yml` đọc đúng tên đó, đặt
`MAIL_USERNAME` không có tác dụng.

---

## 13 — Việc bạn cần làm tiếp

Cập nhật 30/08/2026, sau khi cả bốn PR đã merge và pipeline chạy thật một vòng.
Ba việc ở bản trước (Gmail app password, push code bật auto-deploy, trỏ web/mobile
về API mới) **đã xong**, không còn phải làm.

### ✔ Đã xong — bản web của app mobile đã lên sóng

Hai việc đầu của bản trước không còn phải làm:

| | Kết quả |
|---|---|
| Hai secret Cloudflare | đã thêm vào repo `smart-laundry-locker-mobile` |
| Worker | `laundry-locker-mobile-web`, 72 file |
| Địa chỉ | **https://app.locker-drone.tech** |
| SPA fallback | `/orders` trả 200 (không phải 404) |
| CORS | preflight 200, đăng nhập từ origin đó trả 200 |

Từ giờ push vào `develop`/`main` của repo mobile là bản web tự cập nhật.

> **Token đó đã lộ trong lịch sử chat.** Nên vào Cloudflare tạo token mới rồi thu hồi
> token cũ, và cập nhật lại secret `CLOUDFLARE_API_TOKEN` ở **cả hai** repo mobile và
> frontend nếu dùng chung. Account ID không phải bí mật, không cần đổi.

### ① Bật Dependency graph để check `dependency-review` hết đỏ

Check này đỏ với thông báo *"Dependency review is not supported on this repository"* —
là thiếu cài đặt chứ không phải lỗi code. Tôi thử bật qua API nhưng không được, đây
là công tắc trong giao diện:

repo `laundry-locker-microservices` → **Settings** → **Advanced Security**
(hoặc *Code security and analysis*) → **Dependency graph** → *Enable*.

Check này chỉ chạy trên pull request, không ảnh hưởng gì tới deploy.

### ② Quyết định về ngưỡng của `container-scan`

Xem mục 17.4 — cần bạn chọn hướng, không nên để tôi tự quyết.

### ③ Hai cổng thanh toán thật *(khi nào cần demo VNPay/MoMo)*

Điền `VNPAY_*` và `MOMO_*` vào `/opt/laundry-locker-microservices/.env` trên VM rồi
`docker compose up -d payment-service`.

### ④ Quản lý chi phí

```bash
az vm deallocate -g laundry-locker-rg -n laundry-locker-vm   # ngừng tính tiền compute
az vm start      -g laundry-locker-rg -n laundry-locker-vm   # bật lại, IP giữ nguyên
```

Ước tính ~40 USD/tháng (VM ~30 + đĩa ~5 + IP ~3). Credit sinh viên 100 USD ≈ 2,5 tháng.
Gói Azure for Students không cho tạo budget alert, nhưng `spendingLimit: On` đang bật
— hết credit là dịch vụ dừng chứ không phát sinh hoá đơn.

### ⑤ Tuỳ chọn — giảm kích thước gói deploy

Tarball hiện ~962 MB. Đã đo: 962 trong 971 MB là các fat jar mà Dockerfile thật sự
cần, nên cắt `*/target/` không giúp được bao nhiêu. Muốn nhỏ hơn thật thì phải đổi
cách đóng gói (build ảnh trong CI rồi push registry), là việc lớn hơn hẳn.

---

## 14 — Vận hành hằng ngày

```bash
ssh -i ~/.ssh/laundry_azure_rsa azureuser@20.24.196.177
cd /opt/laundry-locker-microservices

docker compose ps                      # 13 container
docker compose logs -f api-gateway
docker compose restart order-service
docker compose up -d --build           # deploy tay

# Nối DB từ máy bạn — qua SSH tunnel, không mở port
ssh -i ~/.ssh/laundry_azure_rsa -L 15432:127.0.0.1:15432 -N azureuser@20.24.196.177
#   rồi trỏ DBeaver/psql vào localhost:15432, user postgres

# Sao lưu
ssh -i ~/.ssh/laundry_azure_rsa azureuser@20.24.196.177 \
  'docker exec ll-ms-postgres pg_dumpall -U postgres' > backup-$(date +%Y%m%d).sql

# Quay lại bản deploy trước
cd /opt && sudo rm -rf laundry-locker-microservices \
  && sudo mv laundry-locker-microservices.previous laundry-locker-microservices \
  && cd laundry-locker-microservices && docker compose up -d --build
```

### Sự cố thường gặp

| Triệu chứng | Xử lý |
|---|---|
| `502 Bad Gateway` | `docker compose ps` — gateway phải map `0.0.0.0:8080`. Thiếu thì thêm `API_GATEWAY_PORT=8080` vào `.env` |
| Web admin trắng | `fe/.env` còn localhost lúc build → sửa, build lại, deploy lại |
| Lỗi CORS | Thêm domain vào `APP_CORS_ALLOWED_ORIGINS` rồi `docker compose up -d api-gateway` |
| Mobile gọi URL cũ | Chưa chạy `build_runner` sau khi đổi `.env` |
| Login trả 500 ngay sau deploy | Feign cold-start — thử lại sau ~30 giây |
| Deploy timeout | `docker compose logs discovery-server` xem service nào không đăng ký được |

---

## 15 — Tự động hoá: push là mọi thứ tự cập nhật

Từ 30/08/2026, **push vào nhánh chính là toàn bộ hệ thống tự cập nhật**. Không còn bước tay nào.

| Repo | Workflow | Kích hoạt | Làm gì |
|---|---|---|---|
| `laundry-locker-microservices` | `deploy-azure.yml` | push `main`/`develop` | `mvn clean verify` → đóng gói → SCP lên VM → `docker compose up -d --build` → chờ Eureka đủ 10 service → **nghiệm thu qua domain** |
| `laundry-locker-frontend` | `deploy.yml` | push `main` | build + deploy web admin và landing page lên Cloudflare Worker |
| `smart-laundry-locker-mobile` | `deploy-web.yml` | push `main`/`develop` | `flutter test` → `flutter build web` → deploy lên Cloudflare Worker |

### Nghiệm thu tự động, không chỉ "container đã Up"

Sau khi deploy, workflow backend gọi đúng đường mà client đi (domain → TLS → Nginx → gateway):

| Kiểm tra | Mong đợi |
|---|---|
| `/actuator/health` | 200 |
| `/api/lockers` (public) | 200 |
| `/api/admin/users` không token | 401 |
| `/user-service/api/admin/users` (đường vòng) | 404 |

Một mục sai là job đỏ — và deploy script trên VM đã tự rollback về bản `.previous` trước đó.

### Mọi lần deploy đều được ghi lại

Hai nơi, đều tự động:

1. **Trang Actions của từng run** — bảng tóm tắt: commit, người đẩy, kết quả nghiệm thu, và
   khi thất bại thì liệt kê nguyên nhân thường gặp kèm lệnh chẩn đoán.
2. **File trong repo** — `infra/azure/DEPLOY-LOG.md` (backend), `DEPLOY-LOG.md` (frontend và
   mobile web). Bản ghi mới nhất nằm trên cùng, có link tới commit và tới run. Đây là chỗ để
   theo dõi lịch sử mà không cần mở tab Actions.

Workflow tự commit file nhật ký với `[skip ci]`, và chính file đó nằm trong `paths-ignore`,
nên không có vòng lặp.

---

## 16 — Xem app mobile trên trình duyệt

`smart-laundry-locker-mobile` giờ build được cho web. Giao diện **giống hệt** bản Android vì
Flutter dùng renderer **CanvasKit** — nó tự vẽ toàn bộ widget lên canvas thay vì dịch sang
HTML/CSS, nên không có chuyện "gần giống".

```bash
# xem ngay tại máy, không cần máy ảo
cd smart-laundry-locker-mobile
flutter build web --release
cd build/web && python -m http.server 8899
# mở http://localhost:8899
```

Bản deploy: Cloudflare Worker `laundry-locker-mobile-web`, cấu hình trong `wrangler.jsonc`.

**Ba điều cần biết:**

- **CORS.** Trình duyệt chặn mọi lời gọi API từ origin không nằm trong
  `APP_CORS_ALLOWED_ORIGINS` của gateway. Đã thêm `https://app.locker-drone.tech`. Nếu bạn
  dùng địa chỉ `*.workers.dev` thì phải thêm origin đó vào `.env` trên VM rồi
  `docker compose up -d api-gateway`.
- **Tính năng cần phần cứng** không chạy trên web: quét QR bằng camera, vân tay
  (`local_auth`), thông báo đẩy nền. Chúng dùng plugin không có bản web nên sẽ báo lỗi khi
  gọi — phần còn lại của app hoạt động bình thường.
- **`dart:io`.** 13 file trong `lib/` có import nó, nhưng không file nào đi tới được từ
  `main.dart` nên build web sạch. Nếu sau này thêm màn hình mới dùng `File`, build web sẽ vỡ —
  dùng `XFile`/`Uint8List` thay thế.

---

## 17 — Ngày merge: bốn lỗi đã lộ ra và cách xử lý

Ghi lại ngày 30/08/2026, lúc merge bốn PR và cho pipeline chạy thật lần đầu. Ba lỗi
đầu là lỗi trong chính phần tự động hoá tôi viết ở mục 15 — chúng chỉ lộ ra khi chạy
thật, không cách nào thấy được lúc viết.

### 17.1 — `paths` và `paths-ignore` không được đứng cạnh nhau

**Triệu chứng.** Merge PR #52 xong, workflow frontend kết thúc **failure** với **0 job**
và không có log nào để đọc. Không có bước nào đỏ vì không bước nào từng chạy.

**Nguyên nhân.** GitHub Actions không cho một event khai báo đồng thời `paths` và
`paths-ignore`; workflow bị từ chối ngay lúc khởi động. Tôi thêm `paths-ignore` để
chống vòng lặp khi job ghi nhật ký tự commit, mà quên rằng `paths` đã có sẵn ở đó.

**Xử lý.** Bỏ `paths-ignore`. Hoá ra còn **thừa**: `DEPLOY-LOG.md` nằm ở gốc repo nên
vốn đã không khớp `paths` (`fe/**`, `landingPage/**`, workflow). Chốt chống vòng lặp
thứ hai là `[skip ci]` vẫn còn nguyên. Backend và mobile chỉ dùng `paths-ignore` một
mình nên không dính.

> **Cách nhận ra.** Run failure mà **0 job** thì gần như luôn là lỗi cú pháp hoặc lỗi
> cấu hình trigger, không phải lỗi code. Đừng mất công đọc log — không có log.

### 17.2 — Một dấu `\` làm hỏng bước ghi nhật ký của mobile

**Triệu chứng.** Bước có `if: always()` nên sẽ chạy và đỏ **kể cả khi deploy thành công**.

**Nguyên nhân.** Dòng dựng bản ghi viết `cut -c1-7\)`. Dấu `\` khiến `$( )` không đóng
đúng chỗ; bash báo `syntax error near unexpected token '('`.

**Xử lý.** Bỏ dấu `\`. Sau đó kiểm cả ba workflow bằng cách rút từng khối `run:` ra
file rồi chạy `bash -n` — cả ba đều sạch. Đáng làm thành thói quen trước khi push
workflow, vì lỗi cú pháp bash trong YAML không có gì bắt được lúc viết:

```bash
python - <<'EOF'
import yaml, io, re, subprocess, tempfile, os, shutil

# Tren Windows, 'bash' goi tu Python thuong tro vao WSL chu khong phai Git Bash.
BASH = shutil.which('bash') or r'C:\Program Files\Git\usr\bin\bash.exe'
if 'System32' in BASH or 'WindowsApps' in BASH:
    BASH = r'C:\Program Files\Git\usr\bin\bash.exe'

d = yaml.safe_load(io.open('.github/workflows/deploy.yml', encoding='utf-8'))
for job in d['jobs'].values():
    for st in job.get('steps', []):
        if not st.get('run'): continue
        body = re.sub(r'\$\{\{[^}]*\}\}', 'XX', st['run'])   # thay ${{ }} bằng chuỗi giả
        f = tempfile.NamedTemporaryFile('w', suffix='.sh', delete=False,
                                        encoding='utf-8', newline='\n')
        f.write(body); f.close()
        r = subprocess.run([BASH, '-n', f.name], capture_output=True, text=True)
        os.unlink(f.name)
        print(st.get('name'), 'ok' if r.returncode == 0 else 'LỖI: ' + r.stderr.strip())
EOF
```

### 17.3 — Thiếu secret thì bỏ qua, đừng đỏ

Repo mobile chưa có secret Cloudflare nên bước deploy chắc chắn hỏng, kéo cả run đỏ
dù test và build web đều tốt — một run đỏ không nói lên điều gì thì chẳng ai đọc nữa.

Workflow nay kiểm tra trước rồi bỏ qua **riêng** bước deploy. Trang tóm tắt và
`DEPLOY-LOG.md` ghi rõ *"build xong, CHƯA deploy — thiếu secret Cloudflare"* để không
nhầm với deploy thật. Thêm secret vào là bước deploy tự bật.

Secret không dùng được trực tiếp trong `if:` của step, phải ánh xạ qua output trước:

```yaml
- name: Có đủ secret Cloudflare chưa?
  id: co_secret
  env:
    TOKEN: ${{ secrets.CLOUDFLARE_API_TOKEN }}
  run: |
    if [ -n "$TOKEN" ]; then echo 'du=true'  >> "$GITHUB_OUTPUT"
    else                     echo 'du=false' >> "$GITHUB_OUTPUT"; fi

- name: Deploy lên Cloudflare
  if: steps.co_secret.outputs.du == 'true'
```

### 17.4 — Hai CVE mức CRITICAL trong ảnh container

Workflow `backend-security` đỏ ở cả 10 service **từ 26/07**, không liên quan gì tới
đợt dọn dẹp. Trivy chặn vì hai thư viện, cả hai đều vào qua đường transitive nên không
service nào khai báo trực tiếp:

| Thư viện | CVE | Đang dùng | Đã ghim | Từ đâu vào |
|---|---|---|---|---|
| `org.bouncycastle:bcprov-jdk18on` | CVE-2025-14813 | 1.80 | **1.80.2** | `spring-cloud-context` |
| `org.apache.tomcat.embed:*` | CVE-2026-41293 | 10.1.54 | **10.1.55** | Spring Boot 3.5.14 |

Cả hai ghim ở `dependencyManagement` / property của POM gốc. Property `tomcat.version`
nâng cùng lúc cả ba artifact `embed-core`, `embed-el`, `embed-websocket` — đúng con số
*"CRITICAL: 3"* Trivy đếm ở mỗi service dùng MVC (api-gateway chạy WebFlux/Netty nên
vốn không dính).

Chọn bản vá nhỏ nhất trong các bản đã sửa CVE, cùng dòng với bản Spring đang dựa vào,
để hạn chế rủi ro hồi quy. `mvn -B clean verify` SUCCESS cả 13 module, và đã kiểm tra
lại trong container đang chạy trên VM:

```bash
docker exec ll-ms-user-service sh -lc \
  'unzip -l /app/app.jar | grep -E "bcprov|tomcat-embed"'
#   BOOT-INF/lib/tomcat-embed-core-10.1.55.jar
#   BOOT-INF/lib/tomcat-embed-websocket-10.1.55.jar
#   BOOT-INF/lib/tomcat-embed-el-10.1.55.jar
#   BOOT-INF/lib/bcprov-jdk18on-1.80.2.jar
```

**Kết quả:** CRITICAL từ 3 mỗi service xuống **0 trên toàn bộ 11 service**.

**Nhưng `container-scan` vẫn đỏ, và đây là chỗ cần bạn quyết.** Workflow đặt
`severity: HIGH,CRITICAL` với `exit-code: 1`, trong khi mỗi ảnh còn 14–28 lỗi mức HIGH:

| Ảnh | HIGH còn lại | | Ảnh | HIGH còn lại |
|---|---|---|---|---|
| `notification-service` | 28 | | `iot`/`locker`/`order`/`payment` | 17 |
| `auth-service` | 25 | | `loyalty`/`store`/`user` | 14 |
| `api-gateway` | 24 | | `discovery-server` | 10 |
| | | | *(mọi ảnh)* tầng OS nền | 3 |

Ba hướng, tôi không tự chọn thay bạn:

1. **Hạ ngưỡng xuống `CRITICAL`.** Check xanh ngay, vẫn chặn được lỗi nghiêm trọng
   nhất. Nhanh nhất, nhưng bỏ qua khá nhiều thứ.
2. **Giữ ngưỡng, xử lý dần số HIGH.** Đúng đắn nhất nhưng là việc dài: phần lớn là
   transitive, nâng lên có thể vỡ tương thích, phải build và test lại từng bước.
3. **Giữ ngưỡng nhưng bỏ `exit-code: 1`.** Kết quả vẫn hiện ở tab Security để theo dõi
   mà không chặn PR. Dung hoà, nhưng dễ thành không ai nhìn nữa.

Ba lỗi HIGH ở tầng OS (`libcrypto3`, CVE-2026-14456) đến từ ảnh nền, chỉ hết khi ảnh
nền ra bản mới — không sửa được từ phía POM.

### 17.5 — `cloudflare/wrangler-action` không deploy được Worker chỉ có assets

**Triệu chứng.** Secret đã có, chốt kiểm secret nhận đúng, `flutter test` và
`build web` đều xanh, riêng bước deploy đỏ:

```
npm error npx canceled due to missing packages and no YES option: ["wrangler@4.127.1"]
✘ [ERROR] Missing entry-point
```

**Nguyên nhân.** Hai lỗi nối nhau. Action chạy `npx` mà thiếu `--yes` nên bị huỷ ngay
ở bước tải gói; nó rơi về một bản wrangler cũ hơn có sẵn, và bản cũ đó không hiểu cấu
hình *assets-only* — `wrangler.jsonc` chỉ có `assets.directory`, không có `main` — nên
đòi entry-point.

**Xử lý.** Gọi wrangler thẳng, ghim phiên bản:

```yaml
- name: Deploy lên Cloudflare
  run: npx --yes wrangler@4.114.0 deploy
  env:
    CLOUDFLARE_API_TOKEN: ${{ secrets.CLOUDFLARE_API_TOKEN }}
    CLOUDFLARE_ACCOUNT_ID: ${{ secrets.CLOUDFLARE_ACCOUNT_ID }}
```

Repo `laundry-locker-frontend` đã vấp đúng lỗi này và chuyển sang cách gọi này từ
trước — `wrangler.jsonc` của nó cũng assets-only y hệt và deploy được. Khi một repo
trong workspace đã có cách làm chạy được cho cùng một việc, đối chiếu sang đó nhanh
hơn nhiều so với đọc log.

### 17.6 — Gắn custom domain cho Worker bằng API

Không cần vào giao diện Cloudflare. Lấy `zone_id` rồi gọi một lệnh:

```bash
CF_TOKEN=<token Cloudflare>
CF_ACCOUNT=<account id>
AUTH="Authorization: Bearer $CF_TOKEN"

# 1) lấy zone_id của locker-drone.tech
curl -s -H "$AUTH" "https://api.cloudflare.com/client/v4/zones?name=locker-drone.tech" | python -m json.tool

# 2) gắn hostname vào Worker (thay <zone_id> bằng giá trị vừa lấy)
curl -s -X PUT "https://api.cloudflare.com/client/v4/accounts/$CF_ACCOUNT/workers/domains" -H "$AUTH" -H 'Content-Type: application/json' -d '{"environment":"production","hostname":"app.locker-drone.tech","service":"laundry-locker-mobile-web","zone_id":"<zone_id>"}'
```

Cloudflare tự tạo bản ghi DNS và cấp chứng chỉ; sau khoảng một phút là truy cập được.

Nghiệm thu nên gồm cả ba mục, vì mỗi mục hỏng theo một kiểu khác nhau:

| Kiểm tra | Ý nghĩa nếu sai |
|---|---|
| `GET /` trả 200 và có `flutter_bootstrap` | Worker chưa phục vụ đúng thư mục build |
| `GET /orders` trả 200 chứ không phải 404 | thiếu `not_found_handling: single-page-application`, F5 giữa chừng sẽ vỡ |
| preflight `OPTIONS` từ origin đó trả 200 | origin chưa có trong `APP_CORS_ALLOWED_ORIGINS`, app mở được nhưng không gọi được API |
