# 05 — OAuth / Firebase cho domain mới

← [04 — Mobile](04-mobile.md) · [Mục lục](README.md) · Tiếp: [06 — Troubleshooting](06-troubleshooting.md)

Cần khi dùng đăng nhập Google/Facebook trên **web**, và để OAuth chạy chuẩn.

## Firebase

**Firebase Console → Authentication → Settings → Authorized domains** → thêm:

- `locker-drone.tech`
- `admin.locker-drone.tech`

## Facebook

**Facebook app → Settings → Basic → App Domains** → thêm `locker-drone.tech`.
Nếu dùng web login: **Facebook Login → Settings → Valid OAuth Redirect URIs** thêm
domain tương ứng.

## Lưu ý

- Login Google/Facebook/SĐT trên **mobile** đi qua Firebase SDK native → **không**
  phụ thuộc domain API, nên không cần đổi gì cho mobile ở bước này.
- SHA-1 (Firebase) + Facebook **key hash** cho Android: xem
  `../MOBILE_SELF_REGISTER_PLAN.md` (giá trị debug keystore + cách tính lại).
- **VNPay/MoMo** (nếu test thật): đổi return URL sang
  `https://api.locker-drone.tech/...` — xem `../PAYMENT_SETUP_CHECKLIST.md`.

✅ **Xong khi:** đăng nhập Google/Facebook trên web với domain mới không báo lỗi
"unauthorized domain".
