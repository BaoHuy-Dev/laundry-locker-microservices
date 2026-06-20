# AGENTS.md — laundry-locker-microservices

Tài liệu bắt buộc cho AI coding agents làm việc trong repo này.

---

## BỐI CẢNH BẮT BUỘC

Trước khi phân tích hoặc sửa bất kỳ thứ gì, bắt buộc đọc kỹ 2 file sống chính:

1. `docs/PROJECT_PROGRESS_TRACKER.md` — tiến độ hiện tại, task đang làm, blockers
2. `docs/BUSINESS_FLOWS_CURRENT.md` — roles, endpoints, flows nghiệp vụ (nguồn sự thật)

Nếu cần thêm context, đọc tiếp:
3. `docs/CURRENT_PROJECT_STATUS.md`
4. `docs/project-artifacts/guides/HANDOFF_CODEX.md`

**Không suy luận theo tài liệu cũ nếu mâu thuẫn với 2 file sống chính.**

---

## QUY TẮC LÀM VIỆC

### Trước khi code

Xác định rõ:
- Task đang làm là gì
- Khu vực/file dự kiến sẽ sửa
- Khu vực/file không được sửa
- Có ảnh hưởng nghiệp vụ/API/database/event/UI/mobile/IoT không

Nếu có mục "Đang Làm" trong `PROJECT_PROGRESS_TRACKER.md`, cập nhật task hiện tại vào đó trước khi sửa.

### Branch

Luôn làm trên branch riêng, không code trực tiếp trên `develop`/`main`:

```
feat/<area>-<short-task>
fix/<area>-<short-task>
docs/<short-task>
```

### Không được vi phạm

```
❌ KHÔNG commit: env.txt, pro.txt, Application.txt, Host*.txt, file credential/secret
❌ KHÔNG sửa migration cũ đã chạy — chỉ thêm V<N+1>
❌ KHÔNG expose /internal/** qua gateway (bị chặn 403)
❌ KHÔNG tự ý revert/xóa thay đổi có sẵn nếu không chắc là của mình
❌ KHÔNG tự thêm library mới khi chưa hỏi
❌ KHÔNG tái tạo: laundry-service, partner-service, staff-service (đã gỡ)
```

### Database / Flyway

- Ưu tiên thêm migration mới (`V<N+1>__description.sql`)
- Nếu đổi schema/seed, ghi vào `PROJECT_PROGRESS_TRACKER.md`
- JPA `ddl-auto: validate` — Flyway owns schema, không để Hibernate tạo bảng

### Backend / Gateway

- Client chỉ gọi qua API Gateway (`localhost:18080`)
- Không expose `/internal/**` qua gateway
- Nếu đổi route/RBAC/service contract → cập nhật docs

### Build (thứ tự bắt buộc)

```bash
mvn clean package -DskipTests   # PHẢI clean trước — jar hỏng làm container crash
docker compose build <service>
docker compose up -d <service>
```

---

## SAU KHI LÀM XONG — BẮT BUỘC CẬP NHẬT

### 1. `docs/PROJECT_PROGRESS_TRACKER.md`

Ghi:
- Đã làm gì
- File/khu vực đã thay đổi
- Trạng thái component/flow
- Việc còn lại
- Test/verification đã chạy — kết quả PASS/FAIL/PARTIAL
- Blocker/rủi ro mới nếu có

### 2. `docs/BUSINESS_FLOWS_CURRENT.md`

Cập nhật nếu thay đổi ảnh hưởng tới:
- Luồng nghiệp vụ, roles, quyền
- Endpoint/API contract
- Database/migration/seed
- RabbitMQ events, WebSocket, Scheduler
- Thanh toán, thông báo, loyalty
- Locker cell lifecycle, PIN/QR
- Mobile/web/admin/manager/maintenance/IoT flow

### 3. Mirror (nếu có)

Nếu có bản mirror tại `docs/project-artifacts/markdown-by-project/backend/docs/`, sync lại 2 file sống.

---

## TRƯỚC KHI COMMIT/PUSH

```bash
git status                          # kiểm tra working tree
# chỉ stage đúng file thuộc task
mvn clean package -DskipTests       # hoặc test phù hợp nếu sửa code
```

Commit message:
```
feat(<scope>): mô tả ngắn
fix(<scope>): mô tả ngắn
docs: mô tả ngắn
chore: mô tả ngắn
```

---

## FORMAT BÁO CÁO CUỐI

```
✅ Đã làm: ...
📁 File chính đã sửa: ...
🧪 Test/verification: ... (PASS/FAIL/PARTIAL)
📝 Đã cập nhật 2 file sống: có/chưa
⚠️ Còn lại / rủi ro: ...
🌿 Branch/commit/push: ...
```

---

## THÔNG TIN NHANH

| Thứ | Giá trị |
|---|---|
| Gateway local | `http://localhost:18080` |
| Eureka | `http://localhost:8761` |
| RabbitMQ UI | `http://localhost:15672` (guest/guest) |
| PostgreSQL | `localhost:15432` (postgres/postgres) |
| Backend deployed | `http://146.190.84.136:8080` |
| Auto-deploy | merge → `develop` → GitHub Actions → deploy |

Login body dùng `identifier`, không phải `email`:
```json
{ "identifier": "email@example.com", "password": "12345678" }
```

Xem chi tiết đầy đủ: `../AGENTS.md` (root workspace)
