# 01 — Domain + Cloudflare DNS

← [Về mục lục](README.md) · Tiếp: [02 — Backend HTTPS + CORS](02-backend-https-cors.md)

## 1. Lấy domain `.tech` miễn phí (GitHub Student Pack)

1. Kích hoạt Student Pack: https://education.github.com/pack → "Get student
   benefits" → xác minh bằng email sinh viên (`...@fpt.edu.vn`) hoặc thẻ SV.
2. Vào https://get.tech/github-student-developer-pack → đăng nhập + xác minh
   GitHub → chọn tên (vd `locker-drone.tech`) → checkout **0đ** (free năm đầu).

> - `.com` **không** miễn phí, kể cả Student Pack.
> - Muốn `.vn` thật free **2 năm** (công dân VN 18–23 tuổi): dùng **`.id.vn`** qua
    > VNNIC (Tenten/VinaHost/BKNS), cần CCCD. Các bước còn lại y hệt.
> - ⚠️ `.tech` chỉ free **năm đầu**, năm 2 trả phí → đặt nhắc lịch ~tháng 5/2027.

## 2. Đưa domain về Cloudflare (quản lý DNS + cấp CDN/SSL)

1. Tạo tài khoản https://dash.cloudflare.com (free).
2. **Add a site** → nhập `locker-drone.tech` → chọn plan **Free**.
3. Cloudflare cấp 2 nameserver dạng `xxx.ns.cloudflare.com`. Vào trang quản lý
   domain ở **get.tech** → mục **Nameservers** → xoá NS cũ, dán 2 NS Cloudflare → Save.
4. Chờ Cloudflare báo trạng thái **Active** (vài phút → vài giờ).

## 3. Bản ghi DNS cần tạo

Cloudflare → **DNS → Records → Add record**:

| Type         | Name    | Content          | Proxy status           | Ghi chú                                                                     |
|--------------|---------|------------------|------------------------|-----------------------------------------------------------------------------|
| `A`          | `api`   | `<AZURE_VM_IP>` | **DNS only** (mây xám) | Trỏ tới Azure VM để Nginx/Certbot xử lý                                      |
| `CNAME`/auto | `admin` | (Worker tự thêm) | Proxied                | Tạo tự động khi gắn custom domain cho Worker — xem [03](03-frontend-web.md) |

> ⚠️ **Record `api` bắt buộc để DNS only (mây xám) khi cấp SSL.** Let's Encrypt cần
> nối thẳng tới Azure VM. Nếu bật Proxied (mây cam) trước khi có cert → Certbot fail
> ("Timeout during connect"). Sau khi có cert, bật Proxied cũng được.

✅ **Xong bước này khi:** Cloudflare báo domain Active và record `api` trỏ đúng
`<AZURE_VM_IP>`.
