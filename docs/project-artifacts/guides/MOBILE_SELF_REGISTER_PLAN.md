# Kế Hoạch: Đăng Ký User Trên Mobile (Phone / Google / Facebook qua Firebase) + Admin Web Cấp Role Khác

> Tạo: 2026-06-20
> Trạng thái: **PLAN — chưa code**. Đây là blueprint để thực thi khi được duyệt.
> Cặp tài liệu sống: `docs/PROJECT_PROGRESS_TRACKER.md` + `docs/BUSINESS_FLOWS_CURRENT.md` (cập nhật sau khi code).
> Quyết định đã chốt với user: **dùng Firebase** cho phone OTP và làm identity broker thống nhất cho cả social login.

## 0. Mục Tiêu Nghiệp Vụ

1. Khách hàng tự đăng ký nhanh trên app mobile bằng: **số điện thoại (OTP)**, **Google**, **Facebook** (mở rộng được cho
   provider khác).
2. Self-register trên mobile **chỉ tạo role CUSTOMER**.
3. Các role vận hành (`ADMIN`, `MANAGER`, `MAINTENANCE`) **do admin web tạo và cấp credential** để đăng nhập
   app/portal — không cho tự đăng ký.

## 1. Hiện Trạng (đã đọc code, không suy đoán)

### auth-service (`laundry-locker-microservices/auth-service`)

- `POST /api/auth/register` tạo `auth_account` + provision `user_profile`.
  **Lỗ hổng:** honor `roles` do client gửi (`defaultRoles` chỉ fallback khi rỗng) → ai cũng tự đăng ký `ADMIN`. Cần ép
  CUSTOMER.
  File: `service/AuthService.java` (`register`, dòng ~48-71), `dto/RegisterRequest.java`.
- `POST /api/auth/phone-login` đã có nhưng **chỉ tin số điện thoại thô** (`phoneNumber`/`idToken`), **không verify OTP
  **. File: `AuthService.java` (`phoneLogin`, ~118-143).
- Email OTP đầy đủ (`email/send-otp`, `email/verify-otp`, `email/complete-registration`), `kioskQuickRegister`,
  `completeRegistration`.
- **Không có** Google/Facebook verify. `auth_accounts.auth_provider` tồn tại (default `LOCAL`) nhưng chưa dùng; chưa có
  cột/bảng `provider_user_id`. `password_hash` đang `NOT NULL` (phone/email hiện lách bằng random UUID password).
- Migration hiện tại chỉ có `V1__create_auth_tables.sql`.

### Gateway (`api-gateway`)

- `/api/auth/**` và `/api/admin/auth/**` đã **public** (`JwtGatewayFilter.isPublic`, ~166). Endpoint `/api/auth/...` mới
  không cần đụng gateway.
- `/internal/**` bị chặn từ ngoài; chỉ service-to-service.
- RBAC: `/api/admin/**` = ADMIN, `/api/manage/**` = MANAGER|ADMIN, `/api/maintenance/**` = MAINTENANCE|ADMIN.

### user-service (`user-service`)

- **Lỗ hổng lớn:** `POST /api/admin/users` (`UserController.adminCreate` → `UserProfileService.create`) **chỉ
  tạo `user_profile`, KHÔNG tạo `auth_account`** → manager/maintenance admin tạo ra **không đăng nhập được**.
- `AuthClient` (Feign tới auth-service) hiện chỉ có `changePassword`.

### Mobile (`smart-laundry-locker-mobile`)

- `pubspec.yaml`: đã có `google_sign_in: ^7.2.0`, `firebase_core`, `firebase_messaging`. **Chưa có** `firebase_auth`,
  `flutter_facebook_auth`.
- `AuthBottomSheet` có 2 tab Login/Register (email+phone+password); Register gọi thẳng `/api/auth/register`. **Chưa có**
  nút social, chưa có phone-OTP UI.
- `auth_remote_data_source_impl.dart`: register/login dùng path đầy đủ `/api/auth/...`; nhưng vài method khác dùng
  `_basePath='/auth'` (thiếu `/api`) — endpoint mới phải dùng `/api/auth/...`.
- Role routing `homeForRoles` đã phân MANAGER/ADMIN → managerHome, MAINTENANCE → maintenanceHome, còn lại customer home.

### Web admin (`laundry-locker-frontend/fe`)

- `CreateUserModal.tsx`: `ALL_ROLES = ["USER","STAFF","ADMIN","MODERATOR","PARTNER"]` — **stale** (PARTNER/STAFF đã gỡ,
  thiếu MANAGER/MAINTENANCE). Gọi `useCreateUserMutation` → `POST /api/admin/users`.
- Schema Zod `CreateUserRequestSchema` (trong `stores/apis/admin/users.ts` + `types`) cần kiểm tra có `password`/`roles`
  enum đúng.

## 2. Kiến Trúc Đã Chốt: Firebase Làm Identity Broker Thống Nhất

Thay vì verify riêng từng provider ở backend, **dùng Firebase Auth ở mobile cho cả phone + Google + Facebook**; mọi
provider đều sinh **một Firebase ID token**. Backend chỉ cần **một endpoint verify duy nhất** bằng Firebase Admin SDK,
rồi đọc `firebase.sign_in_provider` để biết nguồn.

```
[Mobile]
  Phone:     firebase_auth.verifyPhoneNumber → nhập OTP → signInWithCredential
  Google:    google_sign_in → GoogleAuthProvider.credential → signInWithCredential
  Facebook:  flutter_facebook_auth → FacebookAuthProvider.credential → signInWithCredential
        ↓ (mọi nhánh)
  userCredential.user.getIdToken()  → gửi { idToken } lên backend
        ↓
[Backend]  POST /api/auth/firebase  { idToken }
  FirebaseAuth.verifyIdToken(idToken)
    → uid, sign_in_provider (phone|google.com|facebook.com), phone_number?, email?, name?
    → upsert auth_account (ép roles = CUSTOMER), link social_identities
    → issue AuthResponse (accessToken/refreshToken/roles)
```

**Lợi ích:** backend không phải gọi Google token endpoint / Facebook Graph API; chỉ giữ 1 secret (Firebase service
account); thêm provider mới chỉ là việc cấu hình phía Firebase + mobile.

**Lưu trữ social:** giữ `password_hash NOT NULL` (social/phone dùng random hash như hiện tại) + thêm bảng
`social_identities(account_id, provider, provider_user_id)` unique `(provider, provider_user_id)`. `auth_provider` set
theo `sign_in_provider`. Tránh migration đổi cột NOT NULL (rủi ro).

## 3. Quyết Định Còn Mở (xác nhận khi bắt đầu code)

| # | Vấn đề                               | Mặc định sẽ làm                                                                                                                                                            |
|---|--------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | Phone OTP                            | **Firebase Phone Auth** (đã chốt).                                                                                                                                         |
| 2 | Google/Facebook                      | Qua Firebase Auth (thống nhất 1 endpoint backend).                                                                                                                         |
| 3 | Facebook ra cùng lúc hay giai đoạn 2 | Khung backend nhận mọi provider ngay; mobile có thể bật Facebook sau (chỉ thêm nút + package).                                                                             |
| 4 | Password khi admin tạo role khác     | Admin tự nhập trong modal; để trống → sinh random + hiển thị 1 lần.                                                                                                        |
| 5 | Credential                           | User cấp: Firebase service account (backend), `google-services.json`/`GoogleService-Info.plist` + cấu hình Facebook (mobile). Backend đọc qua ENV, không hard-code/commit. |

## 4. Kế Hoạch Theo Phase (file-by-file)

### Phase 0 — Sync code + nhánh

- `git fetch` + `git pull` nhánh chính từng repo.
- Nhánh riêng (không code trên develop/main):
    - BE: `feat/auth-firebase-self-register`
    - Mobile: `feat/auth-quick-register-firebase`
    - Web: `feat/admin-create-staff-accounts`
- Cập nhật mục "Đang Làm" trong `PROJECT_PROGRESS_TRACKER.md` trước khi sửa.
- Push develop/main bị harness chặn → merge qua PR.

### Phase 1 — BE: Khóa self-register về CUSTOMER (bắt buộc, làm trước)

- `AuthService.register`: tách 2 đường:
    - public `register(...)` → **ép `Set.of("CUSTOMER")`**, bỏ qua roles client.
    - internal `provisionWithRoles(...)` → cho phép roles tùy ý (dùng ở Phase 4).
- Verify: unit test "register roles=[ADMIN] → kết quả CUSTOMER".

### Phase 2 — BE: Verify Firebase ID token (1 endpoint cho phone/Google/Facebook)

- `auth-service/pom.xml`: thêm `com.google.firebase:firebase-admin`.
- Config Firebase Admin qua ENV (không commit): `FIREBASE_CREDENTIALS_JSON` (nội dung) hoặc
  `GOOGLE_APPLICATION_CREDENTIALS` (path). Bean khởi tạo `FirebaseApp` lazy/optional (không có credential → tắt tính
  năng, không crash app — theo phong cách fail-soft như SmtpEmailService).
- Migration `V2__auth_social_identities.sql`: bảng
  `social_identities(id, account_id BIGINT, provider VARCHAR(40), provider_user_id VARCHAR(255), created_at)` + unique
  `(provider, provider_user_id)` + index `account_id`.
- `AuthService.firebaseLogin(idToken)`:
    1. `verifyIdToken` → lấy uid, `sign_in_provider`, `phone_number`, `email`, `name`.
    2. Tìm account theo `social_identities(provider, uid)`; nếu chưa có → tìm theo email/phone để **link** vào account
       cũ; nếu vẫn chưa → tạo mới (provision user CUSTOMER + auth_account random password, set `auth_provider`,
       `phone_verified`/`email_verified` theo provider).
    3. Issue `AuthResponse`.
- `AuthController`: `POST /api/auth/firebase` (public) `{ idToken }`.
- (Tùy chọn dọn dẹp) đánh dấu `phone-login` cũ deprecated hoặc route nó qua firebase verify.
- Verify: idToken sai → 401; tạo mới + login lại ra cùng account.

### Phase 3 — BE: Admin cấp tài khoản role khác (đăng nhập được)

- auth-service: `POST /internal/auth/accounts` `{ userId, email, phone, password, roles }` → tạo `auth_account` (
  password hash thật) cho userId. Validate roles ∈ {CUSTOMER, ADMIN, MANAGER, MAINTENANCE}.
- user-service `AuthClient`: thêm `createAccount(...)`.
- `UserController.adminCreate`: tạo profile → gọi `authClient.createAccount` (password admin nhập; trống → random trả về
  cho UI hiển thị). Compensate/xóa profile nếu tạo account lỗi (tránh profile mồ côi không login được).
- Verify: admin tạo MANAGER → login bằng password đó → JWT role MANAGER → mobile vào managerHome.

### Phase 4 — Mobile: UI đăng ký nhanh + Firebase

- `pubspec.yaml`: thêm `firebase_auth`; (Facebook) `flutter_facebook_auth`.
- Cấu hình native: Android `google-services.json` + SHA-1/SHA-256 trong Firebase; iOS `GoogleService-Info.plist`; (
  Facebook) khai báo app id/redirect.
- `auth_remote_data_source(_impl)`: thêm `firebaseLogin(idToken)` → `POST /api/auth/firebase` (path `/api/auth/...` đầy
  đủ).
- Service mới `FirebaseAuthService` (mobile): bọc verifyPhoneNumber/OTP, Google, Facebook → trả Firebase idToken.
- Use case + provider: `social_login_use_case`, `phone_otp_provider`.
- UI:
    - `auth_bottom_sheet.dart`: thêm khối "Hoặc tiếp tục với" + nút Google/Facebook + nút "Đăng ký bằng số điện thoại".
    - Màn nhập OTP dùng `pinput` (đã có).
    - Sau thành công: lưu token (TokenService) → `homeForRoles` → customer home.
- Verify: `flutter analyze` 0 error + `flutter build apk --debug`.

### Phase 5 — Web admin: sửa role list + tạo account login được

- `CreateUserModal.tsx`: `ALL_ROLES = ["CUSTOMER","ADMIN","MANAGER","MAINTENANCE"]` + cập nhật `ROLE_STYLES`.
- Đảm bảo payload createUser gửi `password` + `roles`; cập nhật `CreateUserRequestSchema`/`types` nếu thiếu password
  hoặc enum role.
- UX: nếu BE trả password sinh tự động → hiển thị 1 lần kèm nút copy + ghi chú "cấp cho nhân sự đăng nhập".
- Verify: `npm run build`.

### Phase 6 — Docs + verification tổng

- `BUSINESS_FLOWS_CURRENT.md`: mục 2 (role), mục 3 (auth: thêm Firebase phone/Google/Facebook; self-register ép
  CUSTOMER; admin-create tạo credential), mục 4 (route mới).
- `PROJECT_PROGRESS_TRACKER.md`: nhật ký, bảng component (Auth), việc còn lại, verification PASS/FAIL.
- Sync mirror `docs/project-artifacts/markdown-by-project/backend/docs/` nếu có.
- Kiểm tra không commit secret: Firebase service account JSON, `google-services.json`/`GoogleService-Info.plist` (nếu
  chứa key nhạy cảm), Facebook secret, mobile `.env`.

## 5. Endpoint Mới / Thay Đổi

- `POST /api/auth/firebase` `{ idToken }` — public — verify Firebase, login/đăng ký CUSTOMER.
- `POST /internal/auth/accounts` `{ userId, email, phone, password, roles }` — service-to-service — admin cấp account.
- Sửa hành vi: `POST /api/auth/register` (ép CUSTOMER); `POST /api/admin/users` (kèm tạo credential).
- Migration mới: `auth-service V2__auth_social_identities.sql` (không sửa migration cũ).

## 6. Rủi Ro / Phụ Thuộc

- Cần Firebase project + service account (backend) + cấu hình app Android/iOS (mobile). Facebook cần Facebook App + bật
  provider trong Firebase.
- Backend khởi tạo Firebase Admin phải fail-soft khi thiếu credential (không chặn boot service khác).
- Deploy: merge `develop` auto-deploy + Flyway migrate V2 lúc service khởi động (dữ liệu giữ qua volume Postgres).
- Mobile build cần file cấu hình Firebase trên máy build/CI.

## 7. Thứ Tự Thực Thi Đề Xuất

1. Phase 1 (khóa CUSTOMER) — nhỏ, bảo mật, không phụ thuộc credential.
2. Phase 3 (admin cấp account) — mở khóa manager/maintenance login, không cần Firebase.
3. Phase 2 (Firebase verify BE) — cần Firebase service account.
4. Phase 4 (mobile UI) — cần cấu hình Firebase app.
5. Phase 5 (web role list).
6. Phase 6 (docs + verify).
