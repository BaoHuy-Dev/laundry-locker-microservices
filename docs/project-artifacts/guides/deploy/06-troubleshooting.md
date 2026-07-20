# 06 — Troubleshooting (mọi lỗi đã gặp & cách xử lý)

← [05 — OAuth](05-oauth-firebase.md) · [Mục lục](README.md) · Tiếp: [07 — Cheat-sheet](07-redeploy-cheatsheet.md)

> 🔑 **Quy tắc vàng:** lỗi hiện trên trình duyệt thường KHÔNG phải nguyên nhân thật.
> `502` của Nginx và lỗi `CORS` hay bị lẫn lộn. **Luôn `curl` thẳng API để biết
> tầng nào hỏng** trước khi sửa.

---

## 8.1. `502 Bad Gateway` (hay gặp nhất)

Kiểm theo thứ tự:

**a) Nginx trỏ sai port gateway** (nguyên nhân số 1)

```bash
docker port ll-ms-api-gateway                                    # vd: 8080/tcp -> 0.0.0.0:8080
grep proxy_pass /etc/nginx/sites-available/api.locker-drone.tech # phải khớp port trên
```

Lệch nhau → sửa `proxy_pass http://127.0.0.1:<port>;` rồi `sudo systemctl restart nginx`.

**b) Port gateway nhảy sau recreate** → để Nginx proxy về `8080` (port auto-deploy ép); xem 02 mục A
(xem [02 mục A](02-backend-https-cors.md)). Đây là lý do 502 "lúc được lúc không".

**c) Service phía sau chết** (gateway sống nhưng route 502)

```bash
docker compose ps                                                # service nào Exited/Restarting?
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8081/actuator/health   # auth-service
docker compose up -d                                             # bật lại service đã tắt
```

**d) Gateway đang khởi động** (Spring Boot ~20–30s sau recreate) → đợi rồi thử lại.

> ⚠️ **502 hay bị trình duyệt báo nhầm thành lỗi CORS** (`No
> Access-Control-Allow-Origin`) vì trang lỗi 502 của Nginx không có header CORS.
> Thấy "CORS error" → kiểm 502 TRƯỚC: `curl -is https://api.locker-drone.tech/actuator/health`.

---

## 8.2. Lỗi CORS thật (`No 'Access-Control-Allow-Origin'`)

Chỉ kết luận CORS sau khi chắc chắn API trả `200` (không phải 502). Sửa: set
`APP_CORS_ALLOWED_ORIGINS` đủ origin web rồi recreate gateway — xem
[02 mục D](02-backend-https-cors.md). Nếu image cũ chưa đọc biến → thêm `--build`.

---

## 8.3. `Mixed Content … has been blocked` (web)

Web HTTPS nhưng gọi API `http://...`. Sửa `fe/.env`:

```
VITE_API_BASE_URL=https://api.locker-drone.tech
```

rồi **build lại** + `npx wrangler deploy` (xem [03 mục A/C](03-frontend-web.md)).
Biến trên dashboard Cloudflare KHÔNG áp cho `npm run build` ở máy.

---

## 8.4. Sửa code web mà `admin.locker-drone.tech` không đổi

Worker này **deploy tay**, không tự build khi merge GitHub:

- Cloudflare Build command đang để **None**; Git nối repo
  `BaoHuy-Dev/laundry-locker-frontend-1` (khác repo code `LeThiYenVi/...`).
- → Cứ deploy tay: `npm run build && npx wrangler deploy`.
- Muốn auto: xem [03 mục E](03-frontend-web.md).

---

## 8.5. Certbot fail `Timeout during connect (likely firewall problem)`

- DNS chưa propagate / record `api` chưa trỏ đúng IP / đang Proxied (mây cam) →
  để **DNS only**.
- Mở firewall: `sudo ufw allow 80/tcp; sudo ufw allow 443/tcp`.
- Đợi vài phút rồi **chạy lại** `sudo certbot --nginx -d api.locker-drone.tech`
  (thực tế lần 1–3 fail, lần 4 thành công).

---

## 8.6. Mobile đổi `.env` mà app vẫn gọi URL cũ

`envied` nướng giá trị vào `env_config.g.dart` → phải `dart run build_runner build`.
**KHÔNG dùng `--build-filter`** (xoá các `.g.dart` khác — đã gặp mất 35 file).

---

## 8.7. Backend tự redeploy làm rớt kết nối / 502 lại

Merge vào `develop` → GitHub Actions tự build + recreate container (downtime ngắn
~1–2 phút) và **có thể làm port gateway nhảy lại** nếu `.env` chưa ghim. Sau mỗi đợt
deploy lớn, kiểm lại [02 mục A](02-backend-https-cors.md). **Tránh merge backend
lúc đang demo.**

---

## 8.8. Tạo user admin báo `409 / Data violates a unique or integrity constraint`

`email` và `phone_number` là cột UNIQUE. Để trống SĐT → gửi `""` trùng với user
khác cũng trống → 409. FE đã sửa: bỏ field optional rỗng khỏi payload (lưu NULL).
Nếu vẫn 409 → email/SĐT đó **đã tồn tại thật**, đổi giá trị khác.

---

## Bộ lệnh chẩn đoán nhanh (chạy trên droplet)

```bash
cd /opt/laundry-locker-microservices
docker compose ps                                                  # service sống/chết
docker port ll-ms-api-gateway                                      # port gateway thật
grep proxy_pass /etc/nginx/sites-available/api.locker-drone.tech   # Nginx trỏ đâu
curl -is https://api.locker-drone.tech/actuator/health | head -1   # API qua HTTPS
docker compose logs --tail=40 api-gateway                          # log gateway
free -h                                                            # RAM (OOM?)
```
