# 02 — Backend HTTPS (Nginx + Let's Encrypt) + Port gateway + CORS

← [01 — Domain/DNS](01-domain-dns.md) · [Mục lục](README.md) · Tiếp: [03 — Frontend](03-frontend-web.md)

Đây là phần **hay lỗi nhất** (502). Làm đúng thứ tự: ghim port → Nginx → SSL → CORS.

---

## A. ⚠️ Ghim port gateway = 18080 (làm TRƯỚC)

Gateway publish ra host theo biến `API_GATEWAY_PORT`. Trên droplet này nó từng
**nhảy giữa 8080 ↔ 18080** mỗi lần recreate → gây **502 lúc được lúc không**. Phải
ghim cố định. Port 8080 còn bị project `aisl` (dùng chung droplet) chiếm → ta ghim
**18080** cho gateway của mình.

```bash
cd /opt/laundry-locker-microservices
grep -q '^API_GATEWAY_PORT=' .env \
  && sed -i 's/^API_GATEWAY_PORT=.*/API_GATEWAY_PORT=18080/' .env \
  || echo 'API_GATEWAY_PORT=18080' >> .env

docker compose up -d --force-recreate api-gateway
sleep 30
docker port ll-ms-api-gateway        # PHẢI ra: 8080/tcp -> 0.0.0.0:18080
```

> Nhờ ghim trong `.env`, các lần recreate/redeploy về sau giữ nguyên 18080.

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
        proxy_pass http://127.0.0.1:18080;  # KHỚP với API_GATEWAY_PORT đã ghim ở mục A
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
curl -s -o /dev/null -w "gw 18080 -> %{http_code}\n" http://127.0.0.1:18080/actuator/health   # 200
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
