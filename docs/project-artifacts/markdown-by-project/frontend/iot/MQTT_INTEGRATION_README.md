# MQTT Integration & Tablet Kiosk — Backend Guide

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho
> locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng
> Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`,
`RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

Hướng dẫn tích hợp MQTT và API endpoints cho hệ thống tủ giặt IoT.

## Kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────────────┐
│ TABLET WEB (React + Firebase Auth)                          │
│ ┌───────────────────────────┐  ┌──────────────────────────┐ │
│ │ Luồng 1: Đặt dịch vụ mới  │  │ Luồng 2: Mở bằng mã     │ │
│ │ Phone/Email → OTP          │  │ PIN → verify-pin → unlock│ │
│ │ → Services → Order         │  │ Staff → unlock-with-code │ │
│ │ → Payment → Unlock         │  │                          │ │
│ └───────────┬───────────────┘  └──────────┬───────────────┘ │
└─────────────┼─────────────────────────────┼─────────────────┘
              │ HTTP API                    │ HTTP API
              ▼                             ▼
┌─────────────────────────────────────────────────────────────┐
│ BACKEND SERVER (Spring Boot)                                 │
│ - Auth: phone-login, email/send-otp, verify-otp,            │
│         complete-registration, email/complete-registration   │
│ - Services: GET /api/services                                │
│ - Orders: POST /api/orders                                   │
│ - Payments: POST /api/payments/create                        │
│ - IoT: verify-pin, unlock, unlock-with-code                  │
│                                                              │
│ Sau khi unlock thành công → Publish MQTT                     │
└──────────────────────────┬──────────────────────────────────┘
                           │ MQTT Publish
                           ▼
              ┌────────────────────────┐
              │    MQTT Broker         │
              │ (HiveMQ / Mosquitto)   │
              └────────────┬───────────┘
                           │ MQTT Subscribe
                           ▼
              ┌────────────────────────────┐
              │ ESP8266                     │
              │ Subscribe: locker/commands/ │
              │ → Kích hoạt GPIO → Mở khóa │
              └────────────────────────────┘
```

## Tablet Kiosk API Endpoints (Backend cần hỗ trợ)

### Luồng 1: Đặt dịch vụ mới (cần đăng nhập)

User có thể chọn đăng nhập bằng **Số điện thoại** (Firebase OTP) hoặc **Email** (Backend OTP).

#### 1a. Đăng nhập bằng SĐT (Firebase Phone Auth)

| Bước | Method | Endpoint                                   | Auth   | Request / Response                                                    |
|------|--------|--------------------------------------------|--------|-----------------------------------------------------------------------|
| 1    | —      | Firebase `signInWithPhoneNumber()`         | —      | Client-side: gửi OTP qua Firebase SDK                                 |
| 2    | —      | Firebase `confirmationResult.confirm(otp)` | —      | Client-side: verify OTP → lấy `idToken`                               |
| 3    | POST   | `/api/auth/phone-login`                    | Public | `{ idToken }` → `{ accessToken, isNewUser, tempToken, phoneNumber }`  |
| 3b   | POST   | `/api/auth/complete-registration`          | Public | `{ tempToken, firstName, lastName, birthday }` (nếu `isNewUser=true`) |

#### 1b. Đăng nhập bằng Email (Backend OTP)

| Bước | Method | Endpoint                                | Auth   | Request / Response                                                    |
|------|--------|-----------------------------------------|--------|-----------------------------------------------------------------------|
| 1    | POST   | `/api/auth/email/send-otp`              | Public | `{ email }`                                                           |
| 2    | POST   | `/api/auth/email/verify-otp`            | Public | `{ email, otp }` → `{ accessToken, isNewUser, tempToken }`            |
| 2b   | POST   | `/api/auth/email/complete-registration` | Public | `{ tempToken, firstName, lastName, birthday }` (nếu `isNewUser=true`) |

#### Các bước sau khi đăng nhập (chung cho cả SĐT và Email)

| Bước | Method | Endpoint                         | Auth   | Request / Response                                                                 |
|------|--------|----------------------------------|--------|------------------------------------------------------------------------------------|
| 4    | GET    | `/api/services?category=LAUNDRY` | JWT    | —                                                                                  |
| 5    | POST   | `/api/orders`                    | JWT    | `{ type, lockerId, boxId, serviceIds, customerNote, receiverName, receiverPhone }` |
| 6    | POST   | `/api/payments/create`           | JWT    | `{ orderId, paymentMethod }` → `paymentUrl`                                        |
| 7    | POST   | `/api/iot/unlock`                | Public | `{ pinCode, boxId, lockerCode }`                                                   |

### Luồng 2: Mở bằng mã (không cần đăng nhập)

| Bước  | Method | Endpoint                    | Auth   | Request Body                         |
|-------|--------|-----------------------------|--------|--------------------------------------|
| PIN   | POST   | `/api/iot/verify-pin`       | Public | `{ pinCode, boxId, lockerCode }`     |
| PIN   | POST   | `/api/iot/unlock`           | Public | `{ pinCode, boxId, lockerCode }`     |
| Staff | POST   | `/api/iot/unlock-with-code` | Public | `{ orderId, accessCode, staffName }` |

> [!IMPORTANT]
> Sau khi xử lý unlock thành công (ở bất kỳ endpoint unlock nào), backend cần **publish MQTT message** để ESP8266 thực
> hiện mở relay. Xem phần MQTT bên dưới.

---

## ⚡ YÊU CẦU BACKEND: Đăng ký nhanh cho Kiosk

### Vấn đề

Trên **kiosk tại cửa hàng**, user chỉ cần nhập **số điện thoại** → xác thực OTP → chọn dịch vụ → bỏ đồ. Phải nhanh nhất
có thể.

Hiện tại nếu user mới, backend bắt buộc gọi `complete-registration` với `firstName`, `lastName`, `birthday` (
`@NotBlank`/`@NotNull`). Trên kiosk, user **không muốn và không cần** nhập các thông tin này. Họ có thể cập nhật thông
tin sau trên mobile app.

### Yêu cầu: Tạo endpoint đăng ký nhanh

```
POST /api/auth/kiosk/quick-register
```

**Mục đích:** Khi user mới xác thực SĐT/Email trên kiosk (`isNewUser=true`), frontend gọi endpoint này **thay vì**
`complete-registration`. Backend tự tạo user với thông tin mặc định, trả về JWT để user tiếp tục đặt dịch vụ ngay.

#### Request

```json
{
  "tempToken": "uuid-token-from-phone-login-or-verify-otp"
}
```

#### Response (thành công)

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "abc-def-ghi",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}
```

### Files backend cần thay đổi

| # | File                             | Thay đổi                                              |
|---|----------------------------------|-------------------------------------------------------|
| 1 | `UriParamConstants.java`         | Thêm `KIOSK_QUICK_REGISTER = "/kiosk/quick-register"` |
| 2 | `KioskQuickRegisterRequest.java` | **[NEW]** DTO chỉ có 1 field `tempToken`              |
| 3 | `AuthController.java`            | Thêm endpoint `@PostMapping(KIOSK_QUICK_REGISTER)`    |
| 4 | `AuthService.java`               | Thêm method `kioskQuickRegister()`                    |

### Code mẫu

#### `KioskQuickRegisterRequest.java` (NEW)

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class KioskQuickRegisterRequest {
    @NotBlank(message = "Temp token is required")
    private String tempToken;
}
```

#### `AuthController.java` (thêm endpoint)

```java
/** Kiosk quick register - tạo user nhanh không cần nhập thông tin */
@Operation(summary = "Kiosk Quick Register",
    description = "Quick register for kiosk users - only needs tempToken, no personal info required")
@PostMapping(UriParamConstants.KIOSK_QUICK_REGISTER)
public ResponseEntity<ApiResponse<AuthResponse>> kioskQuickRegister(
    @Valid @RequestBody KioskQuickRegisterRequest request) {
    AuthResponse response = authService.kioskQuickRegister(request);
    return ResponseEntity.ok(
        responseHelper.success(response, MessageConstants.AUTH_REGISTRATION_COMPLETE));
}
```

#### `AuthService.java` (thêm method)

```java
/**
 * Đăng ký nhanh từ kiosk. Tự tạo user với thông tin mặc định.
 * User có thể cập nhật thông tin sau trên mobile app.
 */
@Transactional
public AuthResponse kioskQuickRegister(KioskQuickRegisterRequest request) {
    // 1. Validate tempToken → lấy identifier (SĐT hoặc email) từ Redis
    String identifier = tokenService.getIdentifierByTempToken(request.getTempToken());
    if (identifier == null) {
        throw new AuthenticationException(E_AUTH008);
    }
    tokenService.deleteTempToken(request.getTempToken());

    // 2. Xác định loại identifier
    boolean isEmail = identifier.contains("@");
    boolean isPhone = !isEmail;

    // 3. Kiểm tra user đã tồn tại chưa
    if (isEmail && userRepository.findByEmail(identifier).isPresent()) {
        throw new AuthenticationException(E_AUTH005);
    }
    if (isPhone && userRepository.findByPhoneNumber(identifier).isPresent()) {
        throw new AuthenticationException(E_AUTH005);
    }

    // 4. Tạo user với thông tin tối thiểu
    User newUser = User.builder()
        .email(isEmail ? identifier : null)
        .phoneNumber(isPhone ? identifier : null)
        .firstName("Khách")
        .lastName("")
        .name("Khách")
        .birthday(null)                      // Không bắt buộc
        .provider(isPhone ? AuthProvider.PHONE : AuthProvider.EMAIL)
        .emailVerified(isEmail)
        .phoneVerified(isPhone)
        .roles(getDefaultRoles())
        .build();

    User savedUser = userRepository.save(newUser);
    log.info("Kiosk quick register: {}", identifier);

    // 5. Generate JWT tokens
    String accessToken = jwtTokenProvider.generateTokenFromUser(savedUser);
    String refreshToken = createRefreshToken(savedUser);

    return authMapper.toAuthResponse(accessToken, refreshToken, jwtExpirationMs / 1000);
}
```

### Lưu ý

- `birthday` column trong bảng `users` đã nullable → **không cần migration DB**
- `firstName`/`lastName` cũng nullable → set mặc định `"Khách"` / `""` là OK
- Endpoint này **không ảnh hưởng** flow đăng ký hiện tại trên mobile app
- User đăng ký qua kiosk có thể mở mobile app → vào Profile → cập nhật họ tên, ngày sinh sau

### Flow kiosk sau khi có API này

```
User nhập SĐT → Firebase gửi OTP → Verify OTP → Backend phone-login
  ├─ User cũ  → trả JWT → chọn dịch vụ ngay
  └─ User mới → trả tempToken → gọi /kiosk/quick-register
                → trả JWT → chọn dịch vụ ngay (KHÔNG CẦN NHẬP GÌ THÊM)
```

---

## MQTT Broker

Sử dụng một trong các broker sau:

| Broker                                                    | Loại                    | Phù hợp          |
|-----------------------------------------------------------|-------------------------|------------------|
| [HiveMQ Cloud](https://www.hivemq.com/mqtt-cloud-broker/) | Cloud (free tier)       | Production       |
| [Mosquitto](https://mosquitto.org/)                       | Self-hosted             | Production       |
| `broker.hivemq.com:1883`                                  | Public (không cần auth) | **Testing only** |

> [!WARNING]
> Broker public (`broker.hivemq.com`) **KHÔNG an toàn** cho production. Chỉ dùng để test.

## Topics

| Topic                         | Direction     | Mô tả                                    |
|-------------------------------|---------------|------------------------------------------|
| `locker/commands/{DEVICE_ID}` | Backend → ESP | Gửi lệnh mở/khóa tủ                      |
| `locker/status/{DEVICE_ID}`   | ESP → Backend | Trạng thái tủ (ONLINE, UNLOCKED, LOCKED) |

`DEVICE_ID` mặc định: `ESP8266_LOCKER_01`

## Command JSON Format

### Mở khóa box

```json
{
  "box_id": 1,
  "action": "OPEN"
}
```

### Khóa box

```json
{
  "box_id": 1,
  "action": "LOCK"
}
```

## Status JSON Format (từ ESP8266)

```json
{
  "box_id": 1,
  "status": "UNLOCKED",
  "device": "ESP8266_LOCKER_01"
}
```

Giá trị `status`: `ONLINE` | `UNLOCKED` | `LOCKED`

---

## Spring Boot Integration

### 1. Thêm dependency

**Maven** (`pom.xml`):

```xml
<dependency>
    <groupId>org.eclipse.paho</groupId>
    <artifactId>org.eclipse.paho.mqttv5.client</artifactId>
    <version>1.2.5</version>
</dependency>
```

Hoặc dùng **Spring Integration MQTT**:

```xml
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-mqtt</artifactId>
</dependency>
```

### 2. Cấu hình `application.yml`

```yaml
mqtt:
  broker-url: tcp://broker.hivemq.com:1883   # Thay bằng broker riêng
  client-id: laundry-backend-${random.uuid}
  username:                                    # Để trống nếu public
  password:
  topic-prefix: locker
```

### 3. MQTT Config Bean

```java
@Configuration
public class MqttConfig {
    
    @Value("${mqtt.broker-url}")
    private String brokerUrl;
    
    @Value("${mqtt.client-id}")
    private String clientId;
    
    @Bean
    public MqttClient mqttClient() throws MqttException {
        MqttClient client = new MqttClient(brokerUrl, clientId);
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);
        client.connect(options);
        return client;
    }
}
```

### 4. Service gửi lệnh mở khóa

```java
@Service
@RequiredArgsConstructor
public class LockerMqttService {
    
    private final MqttClient mqttClient;
    
    /**
     * Gửi lệnh mở khóa tới ESP8266 qua MQTT
     * 
     * @param deviceId ID thiết bị ESP (ví dụ: "ESP8266_LOCKER_01")
     * @param boxId    ID box cần mở
     */
    public void sendUnlockCommand(String deviceId, int boxId) {
        String topic = "locker/commands/" + deviceId;
        String payload = String.format(
            "{\"box_id\": %d, \"action\": \"OPEN\"}", boxId
        );
        
        try {
            mqttClient.publish(topic, 
                new MqttMessage(payload.getBytes(StandardCharsets.UTF_8)));
            log.info("[MQTT] Sent OPEN to {} box {}", deviceId, boxId);
        } catch (MqttException e) {
            log.error("[MQTT] Failed to send command", e);
            throw new RuntimeException("MQTT publish failed", e);
        }
    }
    
    /**
     * Gọi method này sau khi xác nhận thanh toán thành công
     */
    public void unlockAfterPayment(Order order) {
        String deviceId = order.getLocker().getDeviceId(); // hoặc mapping từ lockerId
        int boxId = order.getBox().getId();
        sendUnlockCommand(deviceId, boxId);
    }
}
```

### 5. Tích hợp vào Payment Flow

Sau khi thanh toán thành công (VNPay/MoMo callback):

```java
@Service
public class PaymentService {
    
    @Autowired
    private LockerMqttService lockerMqttService;
    
    public void handlePaymentCallback(PaymentResult result) {
        if (result.isSuccess()) {
            // ... cập nhật trạng thái đơn hàng ...
            
            // Gửi lệnh mở khóa qua MQTT
            lockerMqttService.unlockAfterPayment(order);
        }
    }
}
```

---

## Testing nhanh với CLI

Cài `mosquitto-clients` hoặc dùng [MQTTX](https://mqttx.app/):

```bash
# Subscribe (xem status từ ESP):
mosquitto_sub -h broker.hivemq.com -t "locker/status/ESP8266_LOCKER_01"

# Publish (gửi lệnh mở):
mosquitto_pub -h broker.hivemq.com -t "locker/commands/ESP8266_LOCKER_01" -m '{"box_id":1,"action":"OPEN"}'
```

Hoặc dùng MQTTX GUI → kết nối `broker.hivemq.com:1883` → publish tới topic trên.

---

## Lưu ý

- ESP8266 sẽ tự reconnect MQTT mỗi 5 giây nếu mất kết nối
- Mỗi ESP chỉ xử lý lệnh có `box_id` khớp với `BOX_ID` của nó
- ESP publish status `ONLINE` khi kết nối, `UNLOCKED`/`LOCKED` khi thay đổi trạng thái
- Relay tự khóa lại sau 5 giây (cấu hình `UNLOCK_DURATION` trong `config.h`)
- Tablet Web sử dụng **Firebase Phone Auth** cho đăng nhập SĐT (cần cấu hình Firebase project)
