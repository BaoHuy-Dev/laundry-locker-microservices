# 07 — Cheat-sheet deploy lại + Checklist nghiệm thu

← [06 — Troubleshooting](06-troubleshooting.md) · [Mục lục](README.md)

## 🚀 Deploy lại nhanh

| Việc | Lệnh |
|---|---|
| **Web** (sau khi sửa code FE) | `cd fe && npm run build && npx wrangler deploy` |
| **Backend** (cách chuẩn) | merge vào `develop` → GitHub Actions tự build + deploy |
| **Backend** (thủ công trên droplet) | `cd /opt/laundry-locker-microservices && git pull origin develop && docker compose up -d --build` |
| **Mobile** | sửa `.env` → `dart run build_runner build` → `flutter build apk --release` |
| Restart 1 service | `docker compose up -d --force-recreate <service>` |
| Xem service sống/chết | `docker compose ps` |
| Xem log 1 service | `docker compose logs --tail=50 <service>` |
| Đổi origin CORS | sửa `APP_CORS_ALLOWED_ORIGINS` trong `.env` → recreate `api-gateway` |

## 🔁 Sau mỗi lần backend redeploy — kiểm 2 thứ
```bash
docker port ll-ms-api-gateway        # phải: 8080/tcp -> 0.0.0.0:8080
curl -is https://api.locker-drone.tech/actuator/health | head -1   # HTTP/2 200
```
Nếu port không phải 8080 → xem [02 mục A](02-backend-https-cors.md) (ghim lại).

## ✅ Checklist nghiệm thu toàn hệ thống
- [ ] `curl https://api.locker-drone.tech/actuator/health` → `200`, ổ khóa xanh.
- [ ] `docker port ll-ms-api-gateway` → `-> 0.0.0.0:8080`; Nginx `proxy_pass …:8080`.
- [ ] Preflight `OPTIONS …/api/admin/auth/login` trả `access-control-allow-origin`.
- [ ] `admin.locker-drone.tech` đăng nhập được (Console không 502 / CORS / Mixed Content).
- [ ] Tạo user mới OK (để trống SĐT vẫn được).
- [ ] Mobile (đã build lại) gọi API HTTPS OK.
- [ ] Firebase/Facebook đã thêm `locker-drone.tech` vào authorized domains.

## 💰 Chi phí / gia hạn
- `.tech`: free **năm đầu**, năm 2 trả phí → nhắc lịch ~tháng 5/2027 (gia hạn hoặc
  đổi sang `.id.vn` free 2 năm).
- Cloudflare (DNS + Worker) + Let's Encrypt: **free**.
- Droplet DigitalOcean: chi phí hiện tại không đổi.
