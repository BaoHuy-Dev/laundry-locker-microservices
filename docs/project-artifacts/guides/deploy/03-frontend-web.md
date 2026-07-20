# 03 — Frontend web admin (Cloudflare Worker)

← [02 — Backend](02-backend-https-cors.md) · [Mục lục](README.md) · Tiếp: [04 — Mobile](04-mobile.md)

Web admin là **Cloudflare Worker** static assets (`laundry-locker-frontend-1`),
phục vụ bản build React SPA. **Deploy thủ công bằng `wrangler`** (không tự build
khi push Git — xem [Troubleshooting 8.4](06-troubleshooting.md)).

Repo: `LeThiYenVi/laundry-locker-frontend`, code trong thư mục con `fe/`.

---

## A. ⚠️ API URL nằm trong `fe/.env`

Vite **nướng** `VITE_API_BASE_URL` vào JS **lúc build**. `npm run build` (local) đọc
file **`fe/.env`** — KHÔNG đọc biến trên dashboard Cloudflare. File phải là:

```env
# fe/.env
VITE_API_BASE_URL=https://api.locker-drone.tech
```

> ⚠️ Để `http://146.190.84.136:8080` ở đây → web chạy HTTPS gọi API qua HTTP →
> trình duyệt chặn **Mixed Content**. Bắt buộc HTTPS domain.

---

## B. File cấu hình Worker — `fe/wrangler.jsonc`

```jsonc
{
  "name": "laundry-locker-frontend-1",
  "compatibility_date": "2026-06-01",
  "assets": {
    "directory": "./dist",
    "not_found_handling": "single-page-application"   // F5 ở /admin/users vẫn chạy (client-side routing)
  }
}
```

---

## C. Build + Deploy

```bash
cd /g/BigProject/laundry-locker-frontend/fe   # đường dẫn repo FE
npm install
npm run build                # tạo fe/dist (đã nướng URL HTTPS)
npx wrangler login           # lần đầu — mở browser đăng nhập Cloudflare
npx wrangler deploy          # đẩy lên Worker laundry-locker-frontend-1
```

Thành công sẽ in: `Deployed laundry-locker-frontend-1 … Current Version ID: …`.

> **Mỗi lần sửa code web về sau** chỉ cần 2 lệnh:
> ```bash
> npm run build && npx wrangler deploy
> ```

---

## D. Gắn custom domain (chỉ làm 1 lần)

Cloudflare → Workers & Pages → `laundry-locker-frontend-1` → tab **Domains** →
**Add** → `admin.locker-drone.tech`. Cloudflare tự thêm DNS + cấp SSL.

---

## E. (Tùy chọn) Auto-deploy khi push Git

Hiện đang TẮT (Build command = None, và Git nối nhầm repo). Muốn bật:

1. Settings → **Build** → Build command = `npm run build`, Root directory = `fe`.
2. **Disconnect** rồi **Connect to Git** chọn đúng repo code
   `LeThiYenVi/laundry-locker-frontend`, branch `main`.
3. Đặt biến **VITE_API_BASE_URL = https://api.locker-drone.tech** trong build env.

✅ **Xong khi:** mở `https://admin.locker-drone.tech`, Console **không** có lỗi
Mixed Content / CORS / 502, đăng nhập được. Lỗi → xem [06](06-troubleshooting.md).
