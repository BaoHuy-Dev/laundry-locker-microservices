# 04 — Mobile app → domain HTTPS

← [03 — Frontend](03-frontend-web.md) · [Mục lục](README.md) · Tiếp: [05 — OAuth](05-oauth-firebase.md)

Repo: `smart-laundry-locker-mobile` (Flutter).

## A. Đổi `.env`

File `smart-laundry-locker-mobile/.env` (gitignored):

```env
API_URL=https://api.locker-drone.tech/api
API_BASE_URL=https://api.locker-drone.tech
```

> Chỉ `API_BASE_URL` được code đọc (qua `EnvConfig.apiBaseUrl`), nhưng đặt cả hai
> cho nhất quán. Lợi ích HTTPS: hết lỗi cleartext HTTP trên Android máy thật, OAuth
> chạy chuẩn.

## B. ⚠️ Regenerate envied rồi mới build

`envied` **nướng** giá trị `.env` vào `lib/core/config/env_config.g.dart` lúc build.
Đổi `.env` xong **bắt buộc** chạy lại build_runner:

```bash
cd smart-laundry-locker-mobile
dart run build_runner build      # KHÔNG dùng --build-filter
flutter clean
flutter run                      # hoặc: flutter build apk --release
```

> ⚠️ **Tuyệt đối KHÔNG dùng `--build-filter`** với build_runner — nó xoá hàng loạt
> file `.g.dart` khác (thực tế từng mất 35 file, phải `git checkout` khôi phục).
> Cứ chạy build_runner đầy đủ.

## C. Kiểm tra

- Mở app → đăng nhập / gọi API thành công qua `https://api.locker-drone.tech`.
- Không còn lỗi cleartext / network trên máy thật.

> Login Google/Facebook/SĐT đi qua Firebase SDK native (không phụ thuộc domain API).
> Cấu hình SHA-1 / Facebook key hash xem `../MOBILE_SELF_REGISTER_PLAN.md`.

✅ **Xong khi:** app build lại gọi API HTTPS OK.
