# Deploy với tên miền miễn phí (.tech) + Cloudflare Pages

Hướng dẫn đưa hệ thống lên domain thật, HTTPS đầy đủ.

- **Domain**: `.tech` miễn phí 1 năm qua **GitHub Student Pack** (get.tech).
- **Frontend (admin web)**: **Cloudflare Pages** (free, auto-deploy từ GitHub, HTTPS + CDN).
- **Backend (microservices)**: giữ trên **droplet** `146.190.84.136`, thêm **Nginx + Let's Encrypt** cho `api.<domain>`.

> Toàn bộ ví dụ dùng `lockerly.tech`. **Thay bằng tên miền thật của bạn** ở mọi nơi.

```
Người dùng ─┬─ https://lockerly.tech         →  Cloudflare Pages (FE admin)
            └─ https://api.lockerly.tech     →  Droplet (Nginx → gateway :8080)
Mobile app  ───────────────────────────────→  https://api.lockerly.tech
```

---

## Bước 1 — Lấy domain .tech (GitHub Student Pack)

1. Kích hoạt Student Pack: https://education.github.com/pack → "Get student benefits" → xác minh bằng email sinh viên (`...@fpt.edu.vn`) hoặc ảnh thẻ SV.
2. Vào https://get.tech/github-student-developer-pack → đăng nhập, xác minh GitHub → chọn tên (vd `lockerly.tech`) → checkout **0đ** (free năm đầu).
3. Sau khi có domain, **đổi nameserver sang Cloudflare** (Bước 2) để quản lý DNS + Pages một chỗ.

---

## Bước 2 — Đưa domain về Cloudflare (quản lý DNS)

1. Tạo tài khoản https://dash.cloudflare.com (free).
2. **Add a site** → nhập `lockerly.tech` → chọn plan **Free**.
3. Cloudflare cho 2 nameserver (vd `xxx.ns.cloudflare.com`). Vào trang quản lý của get.tech → **đổi nameserver** của domain sang 2 cái này.
4. Chờ DNS propagate (vài phút–vài giờ). Khi Cloudflare báo "Active" là xong.

---

## Bước 3 — Deploy Frontend lên Cloudflare Pages

Repo FE: `laundry-locker-frontend` (branch `main`), code thật nằm trong thư mục con `fe/`.

1. Cloudflare dash → **Workers & Pages → Create → Pages → Connect to Git** → chọn repo `laundry-locker-frontend`.
2. Cấu hình build:
   - **Production branch**: `main`
   - **Root directory (Advanced)**: `fe`
   - **Build command**: `npm run build`
   - **Build output directory**: `dist`
3. **Environment variables (Production)** → thêm:
   - `VITE_API_BASE_URL = https://api.lockerly.tech`
   - (Vite chỉ đọc biến tiền tố `VITE_` lúc build; đặt ở đây là đủ, không cần sửa file `.env` trong repo.)
4. **Save and Deploy**. Pages sẽ build và cho URL `*.pages.dev`.
5. **Gắn domain**: tab **Custom domains** của project Pages → **Set up a custom domain** → nhập `lockerly.tech` (và/hoặc `admin.lockerly.tech`). Cloudflare tự thêm DNS + cấp SSL.

> Từ giờ mỗi lần merge vào `main` của repo FE, Cloudflare tự build & deploy.

---

## Bước 4 — HTTPS cho Backend trên droplet (Nginx + Certbot)

SSH vào droplet rồi chạy. Gateway đang chạy ở host port **8080** (qua docker-compose).

```bash
# 1) Cài nginx + certbot
sudo apt update
sudo apt install -y nginx certbot python3-certbot-nginx

# 2) Tạo site cho api.lockerly.tech  (THAY domain)
sudo tee /etc/nginx/sites-available/api.lockerly.tech >/dev/null <<'NGINX'
server {
    listen 80;
    server_name api.lockerly.tech;          # <-- THAY domain

    client_max_body_size 20m;               # cho upload ảnh (face/avatar)

    location / {
        proxy_pass http://127.0.0.1:8080;   # <-- gateway host port (mặc định 8080)
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        # WebSocket (thông báo realtime)
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 120s;
    }
}
NGINX

sudo ln -sf /etc/nginx/sites-available/api.lockerly.tech /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx

# 3) Cấp SSL Let's Encrypt (tự sửa nginx sang HTTPS + auto-renew)
sudo certbot --nginx -d api.lockerly.tech    # <-- THAY domain
```

DNS cho `api`: ở Cloudflare → **DNS → Add record**:
- Type `A`, Name `api`, IPv4 `146.190.84.136`, **Proxy status: DNS only** (tắt đám mây cam — để Certbot/Let's Encrypt cấp cert trực tiếp; bật proxy sau cũng được nhưng lúc cấp cert nên để DNS only).

Sau bước này: `https://api.lockerly.tech` chạy, có ổ khóa xanh.

---

## Bước 5 — Mở CORS cho domain mới (backend)

Đã làm sẵn trong code: CORS của gateway giờ đọc từ biến `APP_CORS_ALLOWED_ORIGINS`.
Trên droplet, sửa file `.env` của microservices (cạnh `docker-compose.yml`), thêm:

```env
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,https://lockerly.tech,https://admin.lockerly.tech
```

Rồi restart gateway:
```bash
docker compose up -d --force-recreate api-gateway
```

> Nếu không set, mặc định vẫn là `http://localhost:3000` (dev). Thiếu bước này → FE deploy gọi API sẽ bị chặn CORS.

---

## Bước 6 — Trỏ Mobile app sang domain HTTPS

File `smart-laundry-locker-mobile/.env`:
```env
API_URL=https://api.lockerly.tech/api
API_BASE_URL=https://api.lockerly.tech
```
Rồi build lại: `flutter clean && flutter run` (hoặc build APK release).

> Lợi ích: hết lỗi cleartext HTTP trên Android máy thật, và OAuth chạy chuẩn.

---

## Bước 7 — Cập nhật OAuth/Firebase cho domain mới

- **Firebase Console → Authentication → Settings → Authorized domains**: thêm `lockerly.tech`, `admin.lockerly.tech`.
- **Facebook app → Settings → Basic → App Domains**: thêm `lockerly.tech`. Trong **Facebook Login → Settings → Valid OAuth Redirect URIs** thêm domain nếu dùng web login.
- (VNPay/MoMo nếu test thật: cập nhật return URL sang `https://api.lockerly.tech/...` — xem [PAYMENT_SETUP_CHECKLIST.md](PAYMENT_SETUP_CHECKLIST.md).)

---

## Checklist nghiệm thu

- [ ] `https://api.lockerly.tech/api/...` trả JSON, ổ khóa xanh.
- [ ] `https://lockerly.tech` mở được admin web, đăng nhập OK (không lỗi CORS trong Console).
- [ ] Mobile app (đã đổi `.env`) gọi API qua HTTPS OK.
- [ ] Google/Facebook login OK với domain mới (đã thêm authorized domain).

## Ghi chú chi phí
- `.tech` free **năm đầu**, năm 2 gia hạn theo giá thường → cân nhắc nhắc lịch gia hạn/đổi.
- Cloudflare Pages + Let's Encrypt: free. Droplet: chi phí hiện tại không đổi.
- Nếu bạn 18–23 tuổi và muốn `.vn` thật free 2 năm: xem `.id.vn` (VNNIC) làm phương án thay thế cho Bước 1.
