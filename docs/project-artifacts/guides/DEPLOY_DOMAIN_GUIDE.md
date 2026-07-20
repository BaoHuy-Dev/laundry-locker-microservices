# Deploy Production — đã chuyển sang thư mục `deploy/`

Tài liệu deploy đầy đủ giờ nằm trong thư mục **[`deploy/`](deploy/README.md)**,
chia theo từng phần cho dễ theo dõi.

👉 **Bắt đầu tại: [deploy/README.md](deploy/README.md)**

| File                                                                 | Nội dung                                                              |
|----------------------------------------------------------------------|-----------------------------------------------------------------------|
| [deploy/README.md](deploy/README.md)                                 | Tổng quan, sơ đồ, bảng thông tin, giá trị cấu hình chuẩn              |
| [deploy/01-domain-dns.md](deploy/01-domain-dns.md)                   | Domain `.tech` + Cloudflare DNS                                       |
| [deploy/02-backend-https-cors.md](deploy/02-backend-https-cors.md)   | Nginx + Let's Encrypt + **port gateway 8080 (auto-deploy ép)** + CORS |
| [deploy/03-frontend-web.md](deploy/03-frontend-web.md)               | Web admin — Cloudflare Worker + `wrangler`                            |
| [deploy/04-mobile.md](deploy/04-mobile.md)                           | Mobile `.env` + envied                                                |
| [deploy/05-oauth-firebase.md](deploy/05-oauth-firebase.md)           | Firebase / Facebook domains                                           |
| [deploy/06-troubleshooting.md](deploy/06-troubleshooting.md)         | Mọi lỗi đã gặp + cách xử lý                                           |
| [deploy/07-redeploy-cheatsheet.md](deploy/07-redeploy-cheatsheet.md) | Cheat-sheet deploy lại + checklist                                    |

Domain thật: **`locker-drone.tech`** · API: `https://api.locker-drone.tech` · Web:
`https://admin.locker-drone.tech`.
