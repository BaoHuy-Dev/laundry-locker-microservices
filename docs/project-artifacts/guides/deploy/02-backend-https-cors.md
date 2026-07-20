# 02 — Backend HTTPS (Nginx + Let's Encrypt) + Port gateway + CORS

← [01 — Domain/DNS](01-domain-dns.md) · [Mục lục](README.md) · Tiếp: [03 — Frontend](03-frontend-web.md)

Đây là phần **hay lỗi nhất** (502). Làm đúng thứ tự: port 8080 → Nginx → SSL → CORS.

---

## A. ⚠️ Port gateway trên droplet = 8080 (KHỚP với auto-deploy)

Gateway publish ra host theo biến `API_GATEWAY_PORT`. **Auto-deploy ÉP port này =
`8080`**: `scripts/deploy-from-artifact.sh` có dòng
`export API_GATEWAY_PORT="${API_GATEWAY_PORT:-8080}"` — biến shell này **ĐÈ** giá
trị trong `.env` khi `docker compose up` (shell env ưu tiên hơn file `.env`). Lý do:
firewall DigitalOcean chỉ mở inbound 22 + 8080.

Hệ quả **bắt buộc nhớ**:

- **Nginx PHẢI proxy về `8080`** (mục B). Trỏ 18080 → lệch port → **502 sau mỗi lần
  auto-deploy** (đây chính là lỗi "502 lúc được lúc không" đã gặp).
- Local dev: `docker-compose.yml` mặc định `18080` (host 8080 hay bận). **Chỉ trên
  droplet mới là `8080`.**

Cho `.env` khớp luôn `8080` để lần `docker compose up` thủ công không bị lệch:

```bash
cd /opt/laundry-locker-microservices
grep -q '^API_GATEWAY_PORT=' .env \
  && sed -i 's/^API_GATEWAY_PORT=.*/API_GATEWAY_PORT=8080/' .env \
  || echo 'API_GATEWAY_PORT=8080' >> .env
docker port ll-ms-api-gateway        # PHẢI ra: 8080/tcp -> 0.0.0.0:8080
```

> ❌ **ĐỪNG ghim 18080 cho droplet** — auto-deploy sẽ ép lại 8080 và gây 502.

---

## B. Cài Nginx + Certbot, tạo reverse proxy

```bash
sudo apt update
sudo apt install -y nginx certbot python3-certbot-nginx

sudo tee /etc/nginx/sites-available/api.locker-drone.tech >/dev/null <<'NGINX'
server {
    listen 80;
    server_name api.locker-drone.tech;

    client_max_body_size 20m;               # cho upload ảnh (face/avatar)

    location / {
        proxy_pass http://127.0.0.1:8080;   # KHỚP port auto-deploy ép (mục A) — KHÔNG dùng 18080
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Upgrade $http_upgrade;          # WebSocket (thông báo realtime)
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 120s;
    }
}
NGINX

sudo ln -sf /etc/nginx/sites-available/api.locker-drone.tech /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

---

## C. Cấp SSL (HTTPS)

```bash
sudo ufw allow 80/tcp; sudo ufw allow 443/tcp     # nếu có bật ufw
sudo certbot --nginx -d api.locker-drone.tech     # tự sửa Nginx sang 443 + auto-renew
```

> Nếu báo *"Timeout during connect (likely firewall problem)"*: DNS chưa propagate,
> record `api` chưa trỏ đúng IP, hoặc đang Proxied (mây cam). Đợi vài phút rồi
> **chạy lại đúng lệnh certbot** (thực tế lần 1–3 fail, lần 4 OK).

**Kiểm tra:**

```bash
curl -s -o /dev/null -w "gw 8080 -> %{http_code}\n" http://127.0.0.1:8080/actuator/health     # 200
curl -is https://api.locker-drone.tech/actuator/health | head -1                              # HTTP/2 200
```

---

## D. Mở CORS cho domain web

CORS của gateway đọc từ biến `APP_CORS_ALLOWED_ORIGINS` (code đã env-driven trong
`api-gateway/src/main/resources/application.yml`).

```bash
cd /opt/laundry-locker-microservices
grep -q '^APP_CORS_ALLOWED_ORIGINS=' .env \
  && sed -i 's#^APP_CORS_ALLOWED_ORIGINS=.*#APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,https://locker-drone.tech,https://admin.locker-drone.tech#' .env \
  || echo 'APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,https://locker-drone.tech,https://admin.locker-drone.tech' >> .env

docker compose up -d --force-recreate api-gateway
```

**Xác minh** (phải có dòng `access-control-allow-origin`):

```bash
curl -is -X OPTIONS https://api.locker-drone.tech/api/admin/auth/login \
  -H 'Origin: https://admin.locker-drone.tech' \
  -H 'Access-Control-Request-Method: POST' | grep -iE 'HTTP/|access-control-allow'
```

> Nếu container gateway dùng **image cũ** (chưa có code env-driven CORS), set biến
> không ăn → rebuild: `docker compose up -d --build --force-recreate api-gateway`.

✅ **Xong khi:** `curl …/actuator/health` trả `200` qua HTTPS, và preflight trả
`access-control-allow-origin: https://admin.locker-drone.tech`.

Gặp 502 / CORS / lỗi khác → xem [06 — Troubleshooting](06-troubleshooting.md).
