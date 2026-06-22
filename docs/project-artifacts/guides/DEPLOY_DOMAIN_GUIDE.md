# Deploy Production — Domain + HTTPS (locker-drone.tech)

Tài liệu deploy thật của hệ thống Smart Laundry Locker, tổng hợp đầy đủ cấu hình
đang chạy + **mọi lỗi đã gặp và cách xử lý** (mục [Troubleshooting](#troubleshooting)).

> Domain thật đang dùng: **`locker-drone.tech`** (`.tech` free 1 năm qua GitHub
> Student Pack). Mọi giá trị dưới đây là thật — nếu đổi domain, thay tất cả chỗ
> `locker-drone.tech`.

---

## 0. Sơ đồ & thông tin hệ thống

```
                         ┌─────────────────────────── Cloudflare (DNS + CDN) ──────────────────────────┐
Người dùng (web)  ──────▶ admin.locker-drone.tech  →  Cloudflare Worker "laundry-locker-frontend-1"
                         │   (static React SPA, build từ fe/dist)                                      │
                         │        └── gọi API ──▶ https://api.locker-drone.tech                        │
Mobile app        ──────────────────────────────▶ https://api.locker-drone.tech                       │
                         └──────────────────────────────────────────────────────────────────────────┘
                                                          │  (DNS only A record → IP droplet)
                                                          ▼
                              Droplet 146.190.84.136  ──  Nginx (TLS Let's Encrypt)
                                                          └─ proxy_pass 127.0.0.1:18080
                                                                       ▼
                                                          api-gateway (Docker) ──▶ 11 microservices
```

| Hạng mục | Giá trị thật |
|---|---|
| Domain | `locker-drone.tech` (Cloudflare quản lý DNS) |
| Web admin | `https://admin.locker-drone.tech` (Cloudflare **Worker** static assets) |
| API | `https://api.locker-drone.tech` (Nginx + Let's Encrypt trên droplet) |
| Droplet | `146.190.84.136` (DigitalOcean, Ubuntu) — **dùng chung** với project `aisl.io.vn` |
| Code backend trên droplet | `/opt/laundry-locker-microservices` (có backup `…​.previous` — **đừng đụng**) |
| Gateway container | `ll-ms-api-gateway`, port nội bộ `8080` → **host `18080` (đã ghim)** |
| Repo FE (code) | `LeThiYenVi/laundry-locker-frontend` (code ở thư mục con `fe/`) |
| Repo BE (code) | `BaoHuy-Dev/laundry-locker-microservices` (auto-deploy khi merge `develop`) |
| Cloudflare Worker FE | `laundry-locker-frontend-1` (account `nqbhuy2004nt@gmail.com`) |
| Cert Let's Encrypt | `/etc/letsencrypt/live/api.locker-drone.tech/` — hết hạn 2026-09-20 (tự gia hạn) |

**Tài khoản test** (mật khẩu `12345678`): ADMIN `baohuy2k12k4@gmail.com` ·
CUSTOMER `nqbhuy2004nt@gmail.com` · MAINTENANCE
`se180211nguyenquocbaohuy@gmail.com` · MANAGER `huynqbse180211@fpt.edu.vn`.

---

## 1. Domain `.tech` (GitHub Student Pack)

1. Kích hoạt Student Pack: https://education.github.com/pack → xác minh bằng email
   sinh viên (`...@fpt.edu.vn`).
2. https://get.tech/github-student-developer-pack → đăng nhập + xác minh GitHub →
   chọn tên → checkout **0đ** (free năm đầu).
3. `.com` **không free** kể cả Student Pack. Muốn `.vn` thật free 2 năm (18–23
   tuổi): dùng `.id.vn` (VNNIC, qua Tenten/VinaHost) — các bước còn lại y hệt.

> ⚠️ `.tech` chỉ free **năm đầu**; năm 2 gia hạn theo giá thường. Đặt nhắc lịch
> ~tháng 5/2027 để gia hạn hoặc đổi domain.

---

## 2. Đưa domain về Cloudflare (quản lý DNS)

1. Tạo tài khoản https://dash.cloudflare.com (free) → **Add a site** →
   `locker-drone.tech` → plan **Free**.
2. Cloudflare cho 2 nameserver `xxx.ns.cloudflare.com`. Vào trang quản lý get.tech →
   **đổi nameserver** sang 2 cái này → Save.
3. Chờ Cloudflare báo **Active** (vài phút → vài giờ).

**Bản ghi DNS cần có** (Cloudflare → DNS → Records):

| Type | Name | Content | Proxy |
|---|---|---|---|
| `A` | `api` | `146.190.84.136` | **DNS only** (mây xám) |
| (Worker tự thêm) | `admin` | → Worker | Proxied |

> ⚠️ Record `api` **bắt buộc DNS only (mây xám)** lúc cấp SSL — để Let's Encrypt
> nối thẳng tới droplet. Bật Proxied (mây cam) trước khi có cert → Certbot fail.

---

## 3. Backend HTTPS trên droplet (Nginx + Let's Encrypt)

> ⚠️⚠️ **QUAN TRỌNG NHẤT — port gateway.** Gateway publish ra host theo
> `API_GATEWAY_PORT`. Trên droplet này nó từng **nhảy giữa 8080 và 18080** mỗi
> lần recreate → gây 502 liên miên. **Phải ghim cố định** rồi mới cấu hình Nginx
> đúng port. Ngoài ra port 8080 đang bị project `aisl` dùng chung → ta ghim
> **18080** cho gateway của mình.

### 3a. Ghim port gateway = 18080
```bash
cd /opt/laundry-locker-microservices
grep -q '^API_GATEWAY_PORT=' .env \
  && sed -i 's/^API_GATEWAY_PORT=.*/API_GATEWAY_PORT=18080/' .env \
  || echo 'API_GATEWAY_PORT=18080' >> .env
docker compose up -d --force-recreate api-gateway
sleep 30
docker port ll-ms-api-gateway        # PHẢI ra: 8080/tcp -> 0.0.0.0:18080
```

### 3b. Cài Nginx + Certbot, tạo site
```bash
sudo apt update
sudo apt install -y nginx certbot python3-certbot-nginx

sudo tee /etc/nginx/sites-available/api.locker-drone.tech >/dev/null <<'NGINX'
server {
    listen 80;
    server_name api.locker-drone.tech;

    client_max_body_size 20m;               # cho upload ảnh (face/avatar)

    location / {
        proxy_pass http://127.0.0.1:18080;  # KHỚP với API_GATEWAY_PORT đã ghim
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Upgrade $http_upgrade;          # WebSocket (realtime)
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 120s;
    }
}
NGINX

sudo ln -sf /etc/nginx/sites-available/api.locker-drone.tech /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

### 3c. Cấp SSL
```bash
sudo ufw allow 80/tcp; sudo ufw allow 443/tcp    # nếu có bật ufw
sudo certbot --nginx -d api.locker-drone.tech
```
> Nếu Certbot báo *"Timeout during connect (likely firewall problem)"*: thường do
> DNS chưa kịp propagate hoặc record `api` chưa trỏ đúng IP / đang Proxied. Đợi
> vài phút rồi **chạy lại đúng lệnh certbot** (đã gặp: lần 1–3 fail, lần 4 OK).

### 3d. Kiểm tra
```bash
curl -s -o /dev/null -w "gw 18080 -> %{http_code}\n" http://127.0.0.1:18080/actuator/health   # 200
curl -is https://api.locker-drone.tech/actuator/health | head -1                              # HTTP/2 200
```

---

## 4. Mở CORS cho domain web (backend)

CORS của gateway đọc từ biến `APP_CORS_ALLOWED_ORIGINS` (đã code env-driven trong
`api-gateway/.../application.yml`). Trên droplet:

```bash
cd /opt/laundry-locker-microservices
grep -q '^APP_CORS_ALLOWED_ORIGINS=' .env \
  && sed -i 's#^APP_CORS_ALLOWED_ORIGINS=.*#APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,https://locker-drone.tech,https://admin.locker-drone.tech#' .env \
  || echo 'APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,https://locker-drone.tech,https://admin.locker-drone.tech' >> .env
docker compose up -d --force-recreate api-gateway
```

Xác minh (phải có `access-control-allow-origin`):
```bash
curl -is -X OPTIONS https://api.locker-drone.tech/api/admin/auth/login \
  -H 'Origin: https://admin.locker-drone.tech' \
  -H 'Access-Control-Request-Method: POST' | grep -iE 'HTTP/|access-control-allow'
```

> Nếu container gateway dùng **image cũ** (chưa có code env-driven), set biến không
> ăn → phải rebuild: `docker compose up -d --build --force-recreate api-gateway`
> (image build từ `target/*.jar` — auto-deploy/CI lo việc tạo jar).

---

## 5. Frontend — Cloudflare Worker (static SPA)

FE deploy **thủ công bằng wrangler** (KHÔNG auto từ Git — xem [Troubleshooting](#tb-fe)).
Repo đã có `fe/wrangler.jsonc`:

```jsonc
{
  "name": "laundry-locker-frontend-1",
  "compatibility_date": "2026-06-01",
  "assets": { "directory": "./dist", "not_found_handling": "single-page-application" }
}
```

### 5a. Quan trọng: API URL nằm trong `fe/.env`
Vite "nướng" `VITE_API_BASE_URL` vào JS **lúc build**. Build local (`npm run build`)
đọc file **`fe/.env`**, KHÔNG đọc biến trên dashboard Cloudflare. File phải là:
```env
VITE_API_BASE_URL=https://api.locker-drone.tech
```
> ⚠️ Để `http://146.190.84.136:8080` ở đây → web HTTPS gọi HTTP → trình duyệt chặn
> **Mixed Content**. Bắt buộc HTTPS domain.

### 5b. Build + deploy
```bash
cd /g/BigProject/laundry-locker-frontend/fe   # hoặc đường dẫn repo FE của bạn
npm install
npm run build                # tạo fe/dist
npx wrangler login           # lần đầu — mở browser đăng nhập Cloudflare
npx wrangler deploy          # đẩy lên Worker laundry-locker-frontend-1
```
Thành công sẽ in: `Deployed laundry-locker-frontend-1 ... Current Version ID: …`.

### 5c. Gắn custom domain (chỉ làm 1 lần)
Cloudflare → Worker `laundry-locker-frontend-1` → tab **Domains** → add
`admin.locker-drone.tech`.

> **Mỗi lần sửa code web về sau**: chỉ cần `npm run build && npx wrangler deploy`.

---

## 6. Mobile app → domain HTTPS

File `smart-laundry-locker-mobile/.env` (gitignored):
```env
API_URL=https://api.locker-drone.tech/api
API_BASE_URL=https://api.locker-drone.tech
```
Envied "nướng" giá trị vào `lib/core/config/env_config.g.dart` lúc build →
**phải regenerate**:
```bash
cd smart-laundry-locker-mobile
dart run build_runner build      # KHÔNG dùng --build-filter (xem Troubleshooting)
flutter clean && flutter run     # hoặc flutter build apk --release
```

---

## 7. OAuth / Firebase cho domain mới (cho web)

- **Firebase Console → Authentication → Settings → Authorized domains**: thêm
  `locker-drone.tech`, `admin.locker-drone.tech`.
- **Facebook app → Settings → Basic → App Domains**: thêm `locker-drone.tech`.
- Login Google/Facebook/SĐT trên **mobile** đi qua Firebase SDK native → không phụ
  thuộc domain API. Cấu hình SHA-1 / key hash xem `MOBILE_SELF_REGISTER_PLAN.md`.
- VNPay/MoMo (nếu test thật): đổi return URL sang `https://api.locker-drone.tech/...`
  — xem [PAYMENT_SETUP_CHECKLIST.md](PAYMENT_SETUP_CHECKLIST.md).

---

<a name="troubleshooting"></a>
## 8. TROUBLESHOOTING — các lỗi đã gặp & cách xử lý

### 8.1. `502 Bad Gateway` (hay bị nhất)
Nguyên nhân & cách sửa theo thứ tự:

1. **Nginx trỏ sai port gateway.** Gateway publish 18080 nhưng Nginx trỏ 8080 (hoặc
   ngược lại). Kiểm tra & khớp:
   ```bash
   docker port ll-ms-api-gateway                                   # vd 8080/tcp -> 0.0.0.0:18080
   grep proxy_pass /etc/nginx/sites-available/api.locker-drone.tech
   ```
   Sai thì sửa `proxy_pass http://127.0.0.1:<port>;` → `sudo systemctl restart nginx`.
2. **Port gateway nhảy sau recreate.** Ghim `API_GATEWAY_PORT=18080` trong `.env`
   (mục 3a) để không đổi nữa.
3. **Service phía sau chết** (gateway 200 nhưng route 502). Kiểm tra:
   ```bash
   docker compose ps                                  # service nào Exited/Restarting?
   curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8081/actuator/health   # auth-service
   docker compose up -d                               # bật lại
   ```
4. **Gateway đang khởi động** (Spring Boot ~20–30s sau recreate). Đợi rồi thử lại.

> ⚠️ **502 thường bị trình duyệt báo nhầm thành "CORS error"** (`No
> Access-Control-Allow-Origin`), vì trang lỗi 502 của Nginx không có header CORS.
> Thấy CORS error → kiểm tra 502 TRƯỚC bằng `curl` (xem mục 3d/4), đừng vội nghĩ do CORS.

### 8.2. Lỗi CORS thật (`No 'Access-Control-Allow-Origin'`)
Sau khi chắc chắn API trả 200 (không phải 502): set `APP_CORS_ALLOWED_ORIGINS`
gồm đúng origin web (mục 4) → recreate gateway. Xác minh bằng `curl -X OPTIONS`.

### 8.3. `Mixed Content ... blocked` trên web
Web HTTPS nhưng gọi API `http://...`. Sửa `VITE_API_BASE_URL=https://api.locker-drone.tech`
trong `fe/.env` → **build lại** → `npx wrangler deploy` (mục 5a/5b). Biến trên
dashboard Cloudflare KHÔNG áp cho build local.

<a name="tb-fe"></a>
### 8.4. Sửa code web mà không thấy đổi trên `admin.locker-drone.tech`
Worker này **deploy thủ công**, KHÔNG tự build khi merge GitHub:
- Build command trên Cloudflare đang để **None**, và Git nối repo
  `BaoHuy-Dev/laundry-locker-frontend-1` (khác repo code `LeThiYenVi/...`).
- → Cứ deploy tay: `npm run build && npx wrangler deploy`.
- Muốn auto: Settings → Build → đặt Build command `npm run build`, Disconnect rồi
  Connect đúng repo code (`LeThiYenVi/laundry-locker-frontend`, branch `main`, root `fe`).

### 8.5. Certbot fail "Timeout during connect (likely firewall problem)"
- DNS chưa propagate / record `api` chưa trỏ đúng IP / đang Proxied (mây cam).
- Mở firewall: `sudo ufw allow 80/tcp; sudo ufw allow 443/tcp`.
- Đợi vài phút, **chạy lại** `sudo certbot --nginx -d api.locker-drone.tech`.

### 8.6. Mobile: đổi `.env` mà app vẫn gọi URL cũ
Envied nướng giá trị vào `env_config.g.dart` → phải `dart run build_runner build`.
**Tuyệt đối KHÔNG dùng `--build-filter`** — nó xoá hàng loạt `.g.dart` khác (đã gặp:
mất 35 file, phải `git checkout` khôi phục).

### 8.7. Backend tự redeploy làm rớt kết nối
Merge vào `develop` → GitHub Actions tự build + recreate container (downtime ngắn
~1–2 phút) và **có thể làm port gateway nhảy lại** nếu `.env` chưa ghim. Sau mỗi
đợt deploy lớn, kiểm tra lại mục 3a + 3d. Tránh merge backend lúc đang demo.

---

## 9. Cheat-sheet deploy lại

| Việc | Lệnh |
|---|---|
| Deploy lại **web** | `cd fe && npm run build && npx wrangler deploy` |
| Deploy lại **backend** | merge vào `develop` (CI tự lo) — hoặc trên droplet: `cd /opt/laundry-locker-microservices && git pull origin develop && docker compose up -d --build` |
| Build lại **mobile** | sửa `.env` → `dart run build_runner build` → `flutter build apk --release` |
| Restart 1 service | `docker compose up -d --force-recreate <service>` |
| Xem service sống/chết | `docker compose ps` |
| Xem log | `docker compose logs --tail=50 <service>` |

## 10. Checklist nghiệm thu
- [ ] `curl https://api.locker-drone.tech/actuator/health` → `200`, ổ khóa xanh.
- [ ] `docker port ll-ms-api-gateway` → `-> 0.0.0.0:18080`; Nginx `proxy_pass …:18080`.
- [ ] Preflight `OPTIONS …/api/admin/auth/login` trả `access-control-allow-origin`.
- [ ] `admin.locker-drone.tech` đăng nhập được (Console không 502 / không CORS / không Mixed Content).
- [ ] Mobile (đã build lại) gọi API HTTPS OK.
- [ ] Firebase/Facebook đã thêm `locker-drone.tech` vào authorized domains.
