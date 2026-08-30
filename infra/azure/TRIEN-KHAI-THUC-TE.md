# Nhật ký dựng Azure — làm thật, từng lệnh một

Tài liệu này ghi lại **chính xác những gì đã được thực hiện** để đưa backend Smart Laundry
Locker từ DigitalOcean sang Azure, viết dưới dạng **thao tác thủ công** để bạn tự làm lại
được từ đầu, kèm giải thích vì sao mỗi bước tồn tại.

Khác với [RUNBOOK.md](RUNBOOK.md) (hướng dẫn tổng quát dùng script), file này là **bản ghi
thực tế**: có cả những chỗ đã vấp và cách xử lý.

- **Ngày thực hiện:** 30/08/2026
- **Kết quả:** `https://api.locker-drone.tech` đang chạy trên Azure VM, TLS hợp lệ, 13 container Up
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
| ⬜ | Gmail app password (để gửi OTP) | **cần bạn** |
| ⬜ | Push nhánh + PR để bật auto-deploy | **cần bạn quyết** |

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

`MAIL_USERNAME`, `MAIL_PASSWORD`, `VNPAY_*`, `MOMO_*`

---

## 13 — Việc bạn cần làm tiếp

### ① Gmail app password — để gửi OTP *(bắt buộc nếu muốn đăng ký/quên mật khẩu chạy)*

Vào https://myaccount.google.com/apppasswords (phải bật 2FA trước), tạo một app password 16
ký tự, rồi:

```bash
ssh -i ~/.ssh/laundry_azure_rsa azureuser@20.24.196.177
sudo nano /opt/laundry-locker-microservices/.env
#   MAIL_USERNAME=email-cua-ban@gmail.com
#   MAIL_PASSWORD=<16 ký tự, không có dấu cách>
cd /opt/laundry-locker-microservices && docker compose up -d auth-service
```

> Tôi **không** dùng lại mật khẩu trong `env.txt` cũ vì đã khuyến nghị bạn rotate nó — file
> đó nằm plaintext ngoài mọi repo.

### ② Bật auto-deploy — cần push code lên GitHub

Hiện toàn bộ công việc đang ở nhánh local `chore/cleanup-unused-assets-and-deps`:
**2 commit + 23 file sửa + thư mục `infra/` + `deploy-azure.yml`** — chưa push.

Workflow chỉ chạy khi file `deploy-azure.yml` có trên GitHub. Sau khi merge vào `develop`,
mỗi lần push sẽ tự build → đẩy lên VM → deploy → rollback nếu hỏng.

4 secret bạn đã thêm rồi (`AZURE_VM_HOST/USER/SSH_KEY/PORT`) nên chỉ còn thiếu code.

### ③ Web admin và mobile — trỏ về API mới

- **Web admin:** `fe/.env` đang là `http://localhost:18080`. Đổi thành
  `https://api.locker-drone.tech` **trước khi** `npm run build && npx wrangler deploy`,
  nếu không bản production sẽ gọi localhost.
- **Mobile:** sửa `.env` rồi **bắt buộc** chạy
  `dart run build_runner build --delete-conflicting-outputs` (envied nướng giá trị vào
  `env_config.g.dart`), sau đó `flutter clean && flutter build apk --release`.

### ④ Tuỳ chọn — giảm kích thước gói deploy

Tarball hiện **864 MB** vì đóng gói cả `*/target/` trong khi Dockerfile chỉ cần
`target/*.jar`. Mỗi lần deploy đẩy chừng đó qua mạng.

### ⑤ Quản lý chi phí

```bash
az vm deallocate -g laundry-locker-rg -n laundry-locker-vm   # ngừng tính tiền compute
az vm start      -g laundry-locker-rg -n laundry-locker-vm   # bật lại, IP giữ nguyên
```

Ước tính ~40 USD/tháng (VM ~30 + đĩa ~5 + IP ~3). Credit sinh viên 100 USD ≈ 2,5 tháng.
Nên đặt **budget alert** ở mức 50 USD trong *Cost Management*.

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
