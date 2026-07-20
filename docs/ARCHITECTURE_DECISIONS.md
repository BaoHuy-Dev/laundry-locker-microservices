# Quyết Định Kiến Trúc (Architecture Decision Records — ADR)

> Cập nhật lần cuối: 2026-06-14
> Phạm vi: backend `laundry-locker-microservices`.
> Mục đích: ghi chính thức các quyết định kiến trúc "nặng" đã được cân nhắc — **chủ yếu là HOÃN/CHƯA áp dụng ở giai đoạn
hiện tại** — kèm lý do và **điều kiện kích hoạt xem lại**, để team và agent (CodeX) không phải tranh luận lại và biết
> chính xác *khi nào* mới nên làm.
> Tài liệu liên quan: `docs/BUSINESS_FLOWS_CURRENT.md`, `docs/PROJECT_PROGRESS_TRACKER.md`,
`docs/project-artifacts/guides/LOCKER_FLOWS_STANDARD_SPEC.md`.

## Cách dùng tài liệu này

- Mỗi quyết định là một **ADR** có trạng thái: `ACCEPTED (defer)` = chấp nhận **hoãn**; `ACCEPTED (keep current)` = giữ
  phương án hiện tại; `SUPERSEDED` = đã bị thay thế.
- "Hoãn" **không phải** "không bao giờ". Mỗi ADR có mục **Điều kiện xem lại** — khi một điều kiện chạm ngưỡng, mở lại
  ADR đó và làm theo mục **Khi áp dụng (phác thảo cho CodeX)**.
- Nguyên tắc nền: **giữ độ phức tạp tối thiểu cần thiết** cho quy mô hiện tại (1 droplet, traffic vừa, đội nhỏ). Không "
  mạ vàng" kiến trúc.

## Bối cảnh kiến trúc hiện tại (baseline 2026-06-14)

| Hạng mục           | Hiện trạng                                                                                                                                                            |
|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Ngôn ngữ/Framework | Java 21, Spring Boot 3.5.14, Spring Cloud Gateway                                                                                                                     |
| Service discovery  | Eureka (`discovery-server`)                                                                                                                                           |
| API style          | **REST** qua API Gateway (`:8080`); OpenAPI/Swagger gom ở gateway                                                                                                     |
| Service-to-service | OpenFeign + **Resilience4j** circuit breaker + timeout cấu hình env                                                                                                   |
| Messaging          | **RabbitMQ**, 1 topic exchange `laundry.events` (order/payment/locker/iot events)                                                                                     |
| Lưu trữ            | PostgreSQL (mỗi service một schema/db), Flyway migration                                                                                                              |
| Bảo mật            | JWT (`tokenUse=access`) + RBAC tại gateway; chặn `/internal/**`                                                                                                       |
| Triển khai         | **Docker Compose trên 1 droplet**; GitHub Actions CI/CD (test, CodeQL, Trivy, SBOM, provenance, release theo tag)                                                     |
| Observability      | Spring Boot Actuator: `/health`, `/metrics`, `/prometheus`; correlation id xuyên service (MDC)                                                                        |
| Services có source | gateway, discovery, auth, user, order, locker, iot, payment, notification, store, staff, loyalty, common-lib (`laundry-service`/`partner-service` đã bỏ/không source) |

Đặc điểm quy mô: **đơn node, traffic chưa lớn, chưa có SLA cứng, đội nhỏ**. Mọi quyết định dưới đây bám vào bối cảnh
này.

## Bảng tổng hợp quyết định

| ADR | Chủ đề                              | Quyết định                                                         | Trạng thái              | Ngưỡng xem lại (rút gọn)                                                                    |
|-----|-------------------------------------|--------------------------------------------------------------------|-------------------------|---------------------------------------------------------------------------------------------|
| 001 | Kubernetes / Helm / GitOps          | **Hoãn** — giữ Docker Compose 1 droplet                            | ACCEPTED (defer)        | Cần >1 node, zero-downtime, autoscale, hoặc SLA/uptime cam kết                              |
| 002 | Kafka thay RabbitMQ                 | **Hoãn** — giữ RabbitMQ                                            | ACCEPTED (keep current) | Cần event replay, streaming/analytics, throughput cao, nhiều consumer group đọc lại lịch sử |
| 003 | CQRS / Event Sourcing toàn hệ thống | **Không áp dụng (now)** — giữ CRUD + transactional + domain events | ACCEPTED (keep current) | Báo cáo/đọc nặng làm hại OLTP, yêu cầu audit/temporal phức tạp                              |
| 004 | GraphQL                             | **Không áp dụng (now)** — giữ REST                                 | ACCEPTED (keep current) | Client over/under-fetch nhiều round-trip; nhu cầu field linh hoạt theo client               |
| 005 | Service mesh (Istio/Linkerd)        | **Hoãn** — gated sau Kubernetes                                    | ACCEPTED (defer)        | Đã chạy K8s + nhiều service + cần mTLS toàn cục / traffic policy / canary                   |

---

## ADR-001 — Kubernetes / Helm / GitOps

**Trạng thái:** ACCEPTED (defer). **Ngày:** 2026-06-14.

**Bối cảnh.** Hiện deploy bằng Docker Compose trên **một droplet**. K8s/Helm/GitOps cho khả năng tự phục hồi, rolling
update, autoscale, multi-node — nhưng kéo theo chi phí vận hành lớn (control plane, networking, storage class, RBAC
cluster, observability stack, học phí team).

**Quyết định.** **Hoãn.** Giữ Docker Compose + GitHub Actions deploy. Tốt nhưng "hơi nặng nếu hiện chỉ deploy một
droplet".

**Phương án nhẹ đang dùng NOW (làm cho chắc thay vì lên K8s):**

- `docker-compose.yml` + `docker-compose.override.yml`, `restart: unless-stopped`, **healthcheck** cho từng service (dựa
  `/actuator/health`), `depends_on: condition: service_healthy`.
- Cấu hình qua `.env` (không commit secret); image build + scan (Trivy) + SBOM đã có trong CI.
- Deploy "GitOps-lite": merge vào nhánh deploy → GitHub Actions build/verify (`mvn -B clean verify`, không skip test) →
  ship artifact có checksum + provenance → script deploy trên droplet (`scripts/`).
- **Runbook** vận hành 1 trang: cách restart, xem log, rollback (giữ N image cũ), backup Postgres.

**Hệ quả.** Không có self-healing đa node, không rolling zero-downtime (chấp nhận downtime ngắn khi deploy). Bù lại: đơn
giản, rẻ, dễ debug.

**Điều kiện xem lại (làm K8s khi chạm 1 trong số):**

- Cần **>1 node** (HA, vượt giới hạn 1 droplet) hoặc **autoscale** theo tải.
- Cần **zero-downtime rolling/canary** có cam kết uptime/SLA.
- Vận hành >~12 service hoặc nhiều môi trường (staging/prod/region) cần chuẩn hoá.
- Traffic thật ổn định cao (vd > vài chục RPS kéo dài) khiến 1 droplet thành điểm nghẽn.

**Khi áp dụng (phác thảo cho CodeX):**

1. Bắt đầu **managed K8s** (DigitalOcean DOKS) để khỏi tự vận hành control plane.
2. Đóng gói mỗi service thành **Helm chart** (hoặc 1 umbrella chart + values theo env); chuyển `.env` → `ConfigMap`/
   `Secret` (xài External Secrets nếu cần).
3. Mang healthcheck Compose → `readiness/liveness probe` (đã có `/actuator/health`).
4. **GitOps** bằng Argo CD: repo `deploy/` chứa manifests/values; Argo sync theo branch env.
5. Eureka có thể giữ, hoặc thay bằng **K8s Service DNS** (giảm 1 thành phần) — quyết định riêng khi migrate.
6. Postgres: dùng **managed DB** thay vì pod (an toàn dữ liệu).
7. Giữ Resilience4j; thêm HPA; ingress (NGINX) trước gateway.

---

## ADR-002 — Kafka thay RabbitMQ

**Trạng thái:** ACCEPTED (keep current). **Ngày:** 2026-06-14.

**Bối cảnh.** Hệ đang dùng RabbitMQ với 1 exchange `laundry.events` cho các event tích hợp (order/payment/locker/iot →
notification, loyalty). Kafka mạnh ở **log bền + replay + throughput rất cao + stream processing**, nhưng nặng (broker,
ZooKeeper/KRaft, vận hành, schema registry).

**Quyết định.** **Giữ RabbitMQ.** "Chưa cần nếu chưa cần event replay/streaming lớn."

**Phương án nhẹ đang dùng NOW (làm RabbitMQ cho chắc):**

- Mô hình hiện tại là **event-notify** (fire-and-forget) — RabbitMQ phù hợp.
- Bổ sung độ bền nếu chưa có: **DLQ + retry** (dead-letter exchange, TTL/backoff), **consumer idempotent** (khử trùng
  theo event id), publisher confirms cho event quan trọng.
- Versioning payload nhẹ (thêm `eventVersion`), tránh phá vỡ consumer.

**Hệ quả.** Không có "đọc lại lịch sử" như Kafka (RabbitMQ tiêu thụ là mất). Với nghiệp vụ hiện tại (thông báo, cập nhật
loyalty) không cần replay → chấp nhận.

**Điều kiện xem lại (cân nhắc Kafka khi):**

- Cần **replay** sự kiện (rebuild read model, onboard consumer mới đọc lại lịch sử).
- **Throughput** vượt khả năng RabbitMQ / cần phân vùng (partition) để scale ngang theo key.
- Cần **stream processing/analytics** thời gian thực (Kafka Streams/ksqlDB), hoặc nhiều **consumer group** độc lập trên
  cùng dòng sự kiện.
- Triển khai **Event Sourcing** (xem ADR-003) cần log bền làm nguồn sự thật.

**Khi áp dụng (phác thảo cho CodeX):**

1. Không "thay nóng" — **chạy song song**: thêm Kafka cho các luồng cần replay/analytics, giữ RabbitMQ cho phần còn lại;
   hoặc dùng cầu nối.
2. Chuẩn hoá event (Avro/JSON + **Schema Registry**), khoá phân vùng theo `orderId`/`lockerId`.
3. Consumer idempotent + offset management; cân nhắc outbox pattern (transactional outbox) để publish chắc chắn từ DB.
4. Quan sát: lag theo consumer group.

---

## ADR-003 — CQRS / Event Sourcing toàn hệ thống

**Trạng thái:** ACCEPTED (keep current). **Ngày:** 2026-06-14.

**Bối cảnh.** Hiện mỗi service dùng CRUD + JPA + transaction, phát domain events để tích hợp. CQRS/ES tách read/write
model và lưu trạng thái dưới dạng chuỗi sự kiện — mạnh cho audit/temporal/scale đọc, nhưng **rất nặng** (eventual
consistency, projections, rebuild, versioning sự kiện, độ phức tạp nhận thức).

**Quyết định.** **Không áp dụng toàn hệ thống.** "Quá nặng so với trạng thái hiện tại."

**Phương án nhẹ đang dùng NOW:**

- CRUD + `@Transactional` + **domain events cho tích hợp** (đủ tách rời service).
- Đã có `order_status_history` (lịch sử trạng thái đơn) — đáp ứng nhu cầu audit/timeline cơ bản **mà không cần** ES.
- Nếu một module đọc nặng: ưu tiên **read replica / view / cache** trước, không nhảy thẳng CQRS.

**Hệ quả.** Không có "time-travel"/replay trạng thái toàn cục. Chấp nhận, vì nhu cầu hiện tại là vận hành tủ/đơn, không
phải hệ tài chính cần audit bất biến toàn phần.

**Điều kiện xem lại (cân nhắc CQRS/ES cục bộ — KHÔNG toàn hệ thống):**

- Tải **đọc/báo cáo** làm hại đường ghi OLTP (dashboard/analytics nặng).
- Yêu cầu **audit bất biến / temporal query** phức tạp (ai đổi gì, trạng thái tại thời điểm T) vượt `*_status_history`.
- Một aggregate có quy tắc nghiệp vụ phức tạp cần lịch sử sự kiện làm nguồn sự thật.

**Khi áp dụng (phác thảo cho CodeX):**

1. **Phạm vi hẹp**: chỉ CQRS cho một bounded context cụ thể (vd reporting), không "big bang".
2. Read model riêng (materialized view/bảng projection) cập nhật qua event; chấp nhận eventual consistency có kiểm soát.
3. Nếu ES: chọn 1 aggregate, event store (có thể Postgres append-only hoặc Kafka — gắn ADR-002), snapshot để tăng tốc
   rebuild.

---

## ADR-004 — GraphQL

**Trạng thái:** ACCEPTED (keep current). **Ngày:** 2026-06-14.

**Bối cảnh.** Client (web admin, Flutter mobile) đang gọi **REST** qua gateway; OpenAPI gom tại gateway. GraphQL linh
hoạt field/giảm round-trip nhưng thêm tầng schema/resolver/N+1/caching/security phức tạp.

**Quyết định.** **Giữ REST.** "Chưa thấy nhu cầu rõ, REST hiện phù hợp hơn."

**Phương án nhẹ đang dùng NOW:**

- REST tài nguyên rõ ràng + `ApiResponse` chuẩn; OpenAPI/Swagger sẵn.
- Nếu mobile cần gộp dữ liệu nhiều service trong 1 màn → làm **endpoint tổng hợp (BFF/aggregation)** ở
  gateway/order-service thay vì cả GraphQL.

**Hệ quả.** Một số màn có thể gọi vài request (chấp nhận); không có truy vấn field tuỳ biến.

**Điều kiện xem lại (cân nhắc GraphQL khi):**

- Client liên tục **over/under-fetch**, phải gọi nhiều round-trip cho 1 màn (đo được).
- Nhiều loại client với nhu cầu field **khác nhau rõ rệt**, REST sinh quá nhiều biến thể endpoint.
- Cần một API gateway dữ liệu hợp nhất nhiều service cho client.

**Khi áp dụng (phác thảo cho CodeX):** dựng GraphQL như **BFF** (không thay REST nội bộ); schema-first; chống N+1 bằng
DataLoader; giữ RBAC theo field; chỉ phủ các use case thực sự cần.

---

## ADR-005 — Service mesh (Istio / Linkerd)

**Trạng thái:** ACCEPTED (defer). **Ngày:** 2026-06-14.

**Bối cảnh.** Service mesh cho mTLS tự động, traffic policy, retry/timeout/circuit-breaking ở tầng hạ tầng,
observability mesh-wide — nhưng chỉ hợp lý **trên Kubernetes** và khi có nhiều service + traffic thật.

**Quyết định.** **Hoãn**, phụ thuộc ADR-001. "Chỉ nên tính khi đã có Kubernetes và nhiều traffic thật."

**Phương án nhẹ đang dùng NOW (mesh-lite ở tầng app):**

- **Spring Cloud Gateway** (routing, RBAC, header forwarding) + **Eureka** (discovery/LB) + **Resilience4j** (circuit
  breaker, timeout) + **correlation id** xuyên service — đã cung cấp phần lớn giá trị của mesh ở quy mô này.

**Hệ quả.** Chưa có **mTLS service-to-service** tự động, chưa có traffic-shaping/canary tầng hạ tầng. Chấp nhận ở quy mô
1 node.

**Điều kiện xem lại (cân nhắc mesh khi):**

- **Đã chạy Kubernetes** (ADR-001 đã kích hoạt) **và** số service nhiều + traffic thật.
- Yêu cầu **mTLS toàn cục** / zero-trust nội bộ; traffic policy mịn (canary, mirroring, rate-limit per route).
- Cần observability thống nhất (trace/metrics) toàn mesh.

**Khi áp dụng (phác thảo cho CodeX):** chọn **Linkerd** trước (nhẹ hơn Istio) nếu chỉ cần mTLS + metrics; chỉ chọn Istio
khi cần traffic policy nâng cao. Bật mesh **sau** khi K8s ổn định; giữ Resilience4j hay chuyển dần sang policy mesh là
quyết định riêng lúc đó.

---

## Việc nên đầu tư NGAY (thay vì các thứ nặng ở trên)

Vì đã hoãn các hạng mục nặng, ưu tiên củng cố nền tảng hiện tại cho "vững ở quy mô 1 droplet":

1. **Deploy chắc tay**: healthcheck + restart policy đầy đủ trong compose; script deploy + rollback; runbook 1 trang.
2. **Sao lưu Postgres** định kỳ + thử restore (quan trọng nhất khi chạy 1 node).
3. **Độ bền RabbitMQ**: DLQ + retry/backoff + consumer idempotent (ADR-002).
4. **Quan sát**: đã có Actuator/Prometheus — thêm dashboard + alert cơ bản (CPU/mem/disk, health, queue depth).
5. **Bảo mật**: rà các endpoint POST public; secret qua env (prod fail-fast đã có); xoay JWT/QR secret.
6. **Tải & giới hạn**: đo latency (mục tiêu p95 < 300ms) để có số liệu trước khi bàn scale.
7. Tiếp tục lộ trình **luồng tủ** L1–L7 (xem `LOCKER_FLOWS_STANDARD_SPEC.md`).

## Ghi chú handoff cho CodeX

- 5 ADR trên là **quyết định "không/chưa làm bây giờ"** — CodeX **không cần triển khai** K8s/Kafka/CQRS/GraphQL/mesh
  trong giai đoạn này. Việc của CodeX (nếu được giao) là: (a) thực hiện mục **"Việc nên đầu tư NGAY"**, và (b) **chỉ**
  mở lại một ADR khi **Điều kiện xem lại** của nó chạm ngưỡng, rồi theo mục **"Khi áp dụng"**.
- Khi một ADR được kích hoạt: tạo branch riêng theo quy ước dự án, thêm migration mới (không sửa cũ), cập nhật
  `BUSINESS_FLOWS_CURRENT.md` + `PROJECT_PROGRESS_TRACKER.md` + mirror, có test, verify trước khi merge.
- Nếu một quyết định bị đảo ngược trong tương lai, đổi trạng thái ADR thành `SUPERSEDED` và thêm ADR mới tham chiếu nó (
  không xoá lịch sử quyết định).
