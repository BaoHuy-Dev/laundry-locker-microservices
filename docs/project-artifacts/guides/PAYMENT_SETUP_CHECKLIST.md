# Checklist Bổ Sung Cho Thanh Toán (làm khi cần)

> Mục đích: ghi lại **những gì BẠN cần tự cung cấp/cấu hình** để các hình thức thanh toán
> chạy thật. Code đã xong & deploy; phần còn lại là credential + cấu hình môi trường.
> Cập nhật: 2026-06-21.

## Trạng thái hiện tại

| Hình thức | Code | Cần bạn bổ sung |
|---|---|---|
| **Ví nội bộ (Wallet)** | ✅ | Không — chạy ngay |
| **Tiền mặt (Cash)** | ✅ | Không — chạy ngay |
| **VNPay** | ✅ | Đang chạy **sandbox demo**. Muốn tiền thật → credential merchant + return URL công khai |
| **MoMo** | ✅ (gated) | Cần merchant account → bật bằng ENV. Chưa cấu hình → nút MoMo báo `MOMO_NOT_CONFIGURED` |

Tất cả ENV đặt trong file `.env` trên droplet tại **`/opt/laundry-locker-microservices/.env`**
(deploy script giữ nguyên `.env` qua mỗi lần deploy). Sau khi sửa `.env`:
```bash
cd /opt/laundry-locker-microservices
docker compose up -d --force-recreate payment-service
```

---

## 1. VNPay (để nhận tiền thật)

1. Đăng ký merchant tại https://vnpay.vn → lấy **vnp_TmnCode** + **HashSecret**.
2. Thêm vào `.env` droplet:
   ```
   VNPAY_TMN_CODE=<mã merchant>
   VNPAY_HASH_SECRET=<hash secret>
   VNPAY_PAY_URL=https://pay.vnpay.vn/vpcpay.html
   VNPAY_RETURN_URL=http://146.190.84.136:8080/payments/vnpay/callback
   ```
3. ⚠️ `VNPAY_RETURN_URL` **phải công khai** (không `localhost`) để WebView trên điện thoại
   quay về được; dùng path `/payments/vnpay/callback` để app tự đóng WebView khi xong.
4. Recreate `payment-service`.

> Test sandbox: không cần merchant thật, chỉ cần đặt `VNPAY_RETURN_URL` công khai như trên.

## 2. MoMo (để bật nút MoMo)

1. Đăng ký **MoMo Business** (https://business.momo.vn — cần đăng ký doanh nghiệp);
   sandbox dev: https://developers.momo.vn. Lấy **partnerCode / accessKey / secretKey**.
2. Thêm vào `.env` droplet:
   ```
   MOMO_PARTNER_CODE=<...>
   MOMO_ACCESS_KEY=<...>
   MOMO_SECRET_KEY=<...>
   MOMO_ENDPOINT=https://payment.momo.vn/v2/gateway/api/create   # sandbox: https://test-payment.momo.vn/v2/gateway/api/create
   MOMO_REDIRECT_URL=http://146.190.84.136:8080/api/payments/momo/return
   MOMO_IPN_URL=http://146.190.84.136:8080/api/payments/momo/callback
   ```
3. ⚠️ **Không commit secret vào git**; chỉ đặt trên droplet `.env`.
4. Recreate `payment-service`.

## 3. Deploy & rebuild sau khi đổi code

- **Backend**: merge vào `develop` → workflow `Deploy to Droplet` tự chạy (build + Flyway migrate
  + `docker compose up -d --build`). Trong lúc deploy (~10–15 phút trên droplet 4GB), gateway
  **tạm thời down** (ERR_CONNECTION_REFUSED) — đợi workflow xanh là hết.
  Theo dõi: GitHub repo → tab **Actions** → "Deploy to Droplet".
- **Mobile**: build lại APK để có tính năng mới:
  ```
  cd smart-laundry-locker-mobile && flutter build apk --debug
  ```

## 4. Kiểm thử nhanh (không cần bổ sung gì)

1. Nạp ví qua VNPay sandbox → số dư tăng (`GET /api/wallet`).
2. Tạo đơn Thuê tủ/Gửi hàng → chi tiết đơn → **Thanh toán** → **Ví** → trả tức thì → đơn `PAID`.
3. Admin web → Users → nút **Ví** → cộng/trừ số dư.

## 5. Sự cố thường gặp

- **`ERR_CONNECTION_REFUSED` tới `146.190.84.136:8080`**: gateway đang down — thường do
  deploy đang chạy (đợi Actions xong) hoặc 1 service crash. Kiểm tra trên droplet:
  ```
  cd /opt/laundry-locker-microservices && docker compose ps
  docker compose logs --tail=80 api-gateway
  ```
  Nếu container `exited`: `docker compose up -d` (hoặc `--force-recreate <service>`).
- **Nút MoMo báo `MOMO_NOT_CONFIGURED`**: chưa đặt `MOMO_*` (mục 2) — bình thường, các
  phương thức khác vẫn chạy.
- **OTP admin 2FA không tới email**: xem cấu hình SMTP (Brevo/Gmail) trong
  `BUSINESS_FLOWS_CURRENT.md` mục 3; OTP luôn có trong log `auth-service`.
