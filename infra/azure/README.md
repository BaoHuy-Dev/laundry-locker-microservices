# Deploy backend lên Azure VM

Hạ tầng production của Smart Laundry Locker: **một Azure VM chạy `docker compose`**,
Nginx + Let's Encrypt đứng trước làm TLS, GitHub Actions tự deploy khi merge vào `develop`.

Kiến trúc này cố ý giữ nguyên `docker-compose.yml` đang dùng ở local — Eureka và
`lb://` của Spring Cloud Gateway hoạt động y hệt, không phải sửa dòng code Java nào.

```
                    Cloudflare (DNS)
   admin.locker-drone.tech ──▶ Worker (web admin, deploy bằng wrangler)
                                    └── gọi API ──▶ https://api.locker-drone.tech
   Mobile app ──────────────────────────────────▶ https://api.locker-drone.tech
                                                            │ A record → IP tĩnh (DNS only)
                                                            ▼
                                            Azure VM (Ubuntu 22.04, Standard_B2as_v2, eastasia)
                                              └── Nginx :443 (TLS Let's Encrypt)
                                                    └── proxy_pass 127.0.0.1:8080
                                                          └── api-gateway (Docker)
                                                                └── 11 service + Postgres + RabbitMQ
```

Cổng `8080` **không** mở ra Internet — NSG chỉ cho 22 / 80 / 443, mọi request đi qua Nginx.

## Cài lần đầu

```bash
# 0. Trên máy bạn
az login
az account set --subscription "<tên hoặc id subscription>"

# 1. Tạo resource group + VM + IP tĩnh + NSG
cd laundry-locker-microservices
bash infra/azure/provision-vm.sh          # in ra Public IP ở cuối

# Muốn đổi mặc định:
#   RG=laundry-rg LOCATION=eastasia VM_SIZE=Standard_B2as_v2 \
#   SSH_SOURCE_PREFIX=<IP nhà bạn>/32 bash infra/azure/provision-vm.sh

# 2. Cài Docker + Nginx + Certbot trên VM
scp infra/azure/bootstrap-vm.sh azureuser@<IP>:/tmp/
ssh azureuser@<IP> 'sudo bash /tmp/bootstrap-vm.sh'

# 3. Trỏ DNS: A record  api  →  <IP>   (Cloudflare, DNS only / mây xám)
#    Chờ phân giải xong: dig +short api.locker-drone.tech

# 4. Xin chứng chỉ TLS
ssh azureuser@<IP> 'sudo certbot --nginx -d api.locker-drone.tech'

# 5. Điền secret vận hành
ssh azureuser@<IP> 'sudo nano /opt/laundry-locker-microservices/.env'
```

## Secret cần thêm vào GitHub

`Settings → Secrets and variables → Actions` của repo `laundry-locker-microservices`:

| Secret             | Giá trị                                                  |
|--------------------|----------------------------------------------------------|
| `AZURE_VM_HOST`    | IP public của VM                                         |
| `AZURE_VM_USER`    | `azureuser` (hoặc `ADMIN_USER` bạn đã đặt)               |
| `AZURE_VM_SSH_KEY` | **Private key** khớp với public key đã nạp lúc tạo VM    |
| `AZURE_VM_PORT`    | `22` — tùy chọn, bỏ trống cũng được                      |

Sau đó push/merge vào `develop` là workflow [`deploy-azure.yml`](../../.github/workflows/deploy-azure.yml)
tự chạy: `mvn clean verify` → đóng gói tarball (kèm checksum + provenance) → `scp` lên VM →
chạy [`scripts/deploy-from-artifact.sh`](../../scripts/deploy-from-artifact.sh) để
`docker compose up -d --build`, chờ Eureka đăng ký đủ 10 service rồi mới coi là thành công.
Deploy lỗi thì script **tự rollback** về bản `.previous`.

## File `.env` trên VM

Nằm ở `/opt/laundry-locker-microservices/.env`, do `bootstrap-vm.sh` tạo sẵn khung.
Deploy script **giữ lại file này** qua mỗi lần deploy (artifact không chứa `.env`),
nên đây là chỗ đặt secret thật:

| Biến                          | Ghi chú                                                        |
|-------------------------------|----------------------------------------------------------------|
| `API_GATEWAY_PORT`            | Phải là `8080` — Nginx proxy vào `127.0.0.1:8080`              |
| `APP_CORS_ALLOWED_ORIGINS`    | Thêm `https://admin.locker-drone.tech`                         |
| `APP_SECURITY_JWT_SECRET`     | Chuỗi ngẫu nhiên ≥ 32 ký tự (bootstrap sinh sẵn một cái)       |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Gmail app password để gửi OTP                              |
| `FIREBASE_CREDENTIALS_JSON`   | Nguyên JSON service account (auth-service verify ID token)     |
| `VNPAY_*` / `MOMO_*`          | Để trống = dùng sandbox mặc định                               |

## Vận hành

```bash
ssh azureuser@<IP>
cd /opt/laundry-locker-microservices

docker compose ps                        # trạng thái 13 container
docker compose logs -f api-gateway       # log một service
docker compose restart order-service     # restart một service
curl -fsS http://127.0.0.1:8080/actuator/health

# Deploy lại thủ công (không qua GitHub Actions)
docker compose up -d --build

# Postgres (chỉ nghe trong VM, không mở ra Internet)
docker compose exec postgres psql -U postgres -l
```

Nối DB từ máy bạn thì đi qua SSH tunnel, không mở port:

```bash
ssh -L 15432:127.0.0.1:15432 -N azureuser@<IP>
# rồi trỏ DBeaver/psql vào localhost:15432
```

## Chi phí ước tính

| Hạng mục                     | Ước tính / tháng |
|------------------------------|------------------|
| VM `Standard_B2as_v2` (2 vCPU, 8 GB) | ~28–33 USD  |
| Managed disk StandardSSD 64 GB    | ~5 USD      |
| IP tĩnh Standard                  | ~3 USD      |
| **Tổng**                          | **~40 USD**  |

Azure for Students có 100 USD credit/năm → đủ chạy khoảng 2,5 tháng.
Vùng bị giới hạn bởi policy của subscription sinh viên — xem [RUNBOOK.md](RUNBOOK.md).
Muốn rẻ hơn: hạ xuống loại 4 GB nhưng phải giảm `-Xmx` của các service,
hoặc `az vm deallocate` khi không demo (không tính tiền compute lúc tắt).

## Vì sao chọn VM thay vì Container Apps / AKS

12 service đang dùng **Eureka + `lb://service-name`**. Trên một VM với compose,
service discovery chạy nguyên trạng. Chuyển sang Container Apps hoặc AKS thì phải
thay Eureka bằng DNS nội bộ của nền tảng, sửa toàn bộ route trong gateway, và tự host
lại RabbitMQ — nhiều việc hơn hẳn mà lợi ích (autoscale, HA) chưa cần ở quy mô hiện tại.
Quyết định này ghi trong [ADR 001](../../docs/ARCHITECTURE_DECISIONS.md).
