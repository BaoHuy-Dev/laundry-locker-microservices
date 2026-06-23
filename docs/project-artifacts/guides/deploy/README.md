# Hướng dẫn Deploy — Smart Laundry Locker (Production)

Bộ tài liệu deploy thật của hệ thống, gồm cấu hình đang chạy + **mọi lỗi đã gặp và
cách xử lý**. Domain thật: **`locker-drone.tech`**.

> Nếu đổi domain khác: thay tất cả `locker-drone.tech` trong các file dưới đây.

## 📑 Mục lục

| File | Nội dung |
|---|---|
| [01-domain-dns.md](01-domain-dns.md) | Lấy domain `.tech` (GitHub Student) + đưa về Cloudflare quản lý DNS |
| [02-backend-https-cors.md](02-backend-https-cors.md) | **Nginx + Let's Encrypt + port gateway 8080 (auto-deploy ép) + CORS** (phần hay lỗi nhất) |
| [03-frontend-web.md](03-frontend-web.md) | Web admin — Cloudflare Worker, `fe/.env`, deploy bằng `wrangler` |
| [04-mobile.md](04-mobile.md) | App mobile — `.env` + regenerate envied |
| [05-oauth-firebase.md](05-oauth-firebase.md) | Firebase / Facebook authorized domains |
| [06-troubleshooting.md](06-troubleshooting.md) | **Tất cả lỗi đã gặp: 502, CORS giả, Mixed Content, certbot timeout…** |
| [07-redeploy-cheatsheet.md](07-redeploy-cheatsheet.md) | Cheat-sheet deploy lại + checklist nghiệm thu |

## 🗺️ Sơ đồ hệ thống

```
                         ┌──────────────────── Cloudflare (DNS + CDN) ────────────────────┐
Người dùng (web)  ──────▶ admin.locker-drone.tech → Worker "laundry-locker-frontend-1"    │
                         │   (static React SPA, build từ fe/dist)                          │
                         │        └── gọi API ──▶ https://api.locker-drone.tech            │
Mobile app        ──────────────────────────────▶ https://api.locker-drone.tech           │
                         └─────────────────────────────────────────────────────────────────┘
                                                       │  (A record "api" → IP, DNS only)
                                                       ▼
                           Droplet 146.190.84.136 ── Nginx (TLS Let's Encrypt)
                                                       └─ proxy_pass 127.0.0.1:8080
                                                                    ▼
                                                       api-gateway (Docker) ──▶ 11 microservices
```

## ℹ️ Bảng thông tin hệ thống

| Hạng mục | Giá trị |
|---|---|
| Domain | `locker-drone.tech` (DNS ở Cloudflare) |
| Web admin | `https://admin.locker-drone.tech` (Cloudflare **Worker** static assets) |
| **API** | **`https://api.locker-drone.tech`** |
| Droplet | `146.190.84.136` (DigitalOcean) — **dùng chung** với project `aisl.io.vn` |
| Code backend (droplet) | `/opt/laundry-locker-microservices` (có `…​.previous` backup — đừng đụng) |
| Gateway container | `ll-ms-api-gateway` · port nội bộ `8080` → **host `8080` (auto-deploy ép)** |
| Repo FE (code) | `LeThiYenVi/laundry-locker-frontend` (code trong `fe/`) |
| Repo BE (code) | `BaoHuy-Dev/laundry-locker-microservices` (merge `develop` → auto-deploy) |
| Worker FE | `laundry-locker-frontend-1` (account `nqbhuy2004nt@gmail.com`) |
| Cert | `/etc/letsencrypt/live/api.locker-drone.tech/` — hết hạn 2026-09-20 (auto-renew) |

## 🔑 Giá trị cấu hình chuẩn (copy nhanh)

```bash
# API
https://api.locker-drone.tech

# FE — fe/.env (build local nướng vào JS)  /  cũng đặt ở Cloudflare build env nếu nối Git
VITE_API_BASE_URL=https://api.locker-drone.tech

# CORS — .env droplet (cạnh docker-compose.yml)
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,https://locker-drone.tech,https://admin.locker-drone.tech

# Gateway port — .env droplet (ghim chống 502)
API_GATEWAY_PORT=8080

# Mobile — smart-laundry-locker-mobile/.env
API_BASE_URL=https://api.locker-drone.tech
API_URL=https://api.locker-drone.tech/api
```

## 👤 Tài khoản test (mật khẩu `12345678`)
ADMIN `baohuy2k12k4@gmail.com` · CUSTOMER `nqbhuy2004nt@gmail.com` · MAINTENANCE
`se180211nguyenquocbaohuy@gmail.com` · MANAGER `huynqbse180211@fpt.edu.vn`.

## ⚠️ 3 điều dễ sai nhất (đọc trước)
1. **Port gateway phải khớp Nginx.** Gateway luôn ở host `8080` (auto-deploy ép),
   Nginx `proxy_pass …:8080`. Sai port → **502** (mà trình duyệt báo nhầm thành CORS).
2. **`fe/.env` phải là HTTPS domain.** Để `http://IP` → web HTTPS bị chặn **Mixed Content**.
3. **FE deploy TAY** bằng `npm run build && npx wrangler deploy` — không tự build khi push Git.
