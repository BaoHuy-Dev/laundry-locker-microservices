# Quy trình Thuê tủ & Gửi hàng (Đồng bộ Mobile & Kiosk)

Tài liệu này mô tả chi tiết luồng nghiệp vụ (Workflow) từ khi Khách hàng thao tác chọn tủ trên ứng dụng di động (Mobile
App) hoặc Kiosk, cho đến khi gửi đồ và hoàn tất thanh toán. Đặc biệt tập trung vào **cơ chế đồng bộ thời gian thực (
Real-time Sync)** giữa 3 thành phần: Mobile App - Backend - Kiosk.

---

## Cơ chế đồng bộ (Real-time Sync)

Hệ thống đảm bảo tính đồng bộ tức thì giữa Mobile và Kiosk bằng các công nghệ:

1. **Kiosk Screen:** Sử dụng cơ chế **Short Polling** (cứ 3 giây gọi API `getLockerLayout` một lần) để cập nhật trạng
   thái các ô tủ liên tục lên màn hình.
2. **Mobile App:** Lắng nghe tín hiệu từ Backend thông qua **WebSocket (STOMP)**. Khi tủ vật lý có thay đổi (mở cửa/đóng
   cửa), Backend sẽ push message realtime về điện thoại.
3. **IoT Hardware:** Tủ vật lý giao tiếp 2 chiều với Backend qua giao thức **MQTT**.

---

## Sơ đồ Sequence: Quy trình Thuê & Mở tủ

Quy trình dưới đây mô tả kịch bản Khách hàng **đặt tủ trên Mobile** và **ra trước Kiosk để mở tủ gửi đồ**.

```mermaid
sequenceDiagram
    autonumber
    actor User as Khách hàng
    participant Mobile as Mobile App
    participant Kiosk as Kiosk Screen
    participant API as Backend (Order & Locker)
    participant IoT as Tủ vật lý (MQTT)

    %% BƯỚC 1: ĐẶT TỦ TRÊN MOBILE
    Note over User, IoT: BƯỚC 1: ĐẶT TỦ (RESERVE)
    User->>Mobile: Chọn ô "Trống" & Nhấn "Thuê ngay"
    Mobile->>API: POST /api/orders/rental
    API-->>Mobile: Trả về OrderID & PIN (Trạng thái: PENDING)
    
    %% Kiosk tự động đồng bộ trạng thái ô tủ
    API->>API: Chuyển ô thành RESERVED / IN_USE
    loop Mỗi 3 giây
        Kiosk->>API: GET /layout
        API-->>Kiosk: Ô vừa thuê đã bị khóa (Có người đặt)
        Kiosk->>Kiosk: Đổi màu ô trên màn hình Kiosk
    end

    %% BƯỚC 2: MỞ TỦ (LỰA CHỌN A HOẶC B)
    Note over User, IoT: BƯỚC 2: MỞ TỦ ĐỂ GỬI ĐỒ
    
    alt Lựa chọn A: Mở bằng Mobile App
        User->>Mobile: Bấm "Mở tủ"
        Mobile->>API: Lệnh mở tủ (Order ID)
    else Lựa chọn B: Mở bằng mã PIN trên Kiosk
        User->>Kiosk: Nhập mã PIN
        Kiosk->>API: Xác thực PIN
    end

    %% Backend ra lệnh mở phần cứng
    API->>IoT: Gửi lệnh MQTT (Topic: unlock/box_id)
    IoT-->>API: Trả về MQTT (Door Opened)
    
    %% Đồng bộ ngược lại cho cả 2 thiết bị
    par Cập nhật Kiosk
        API-->>Kiosk: Cập nhật màn hình Kiosk "Tủ đã mở"
    and Cập nhật Mobile (Real-time)
        API-)Mobile: Push WebSocket (Status: DOOR_OPENED)
        Mobile->>Mobile: Thanh tiến độ (Stepper) nhích sang bước "Đang mở cửa"
    end

    %% BƯỚC 3: GỬI HÀNG & ĐÓNG CỬA
    Note over User, IoT: BƯỚC 3: GỬI HÀNG & ĐÓNG TỦ
    User->>IoT: Bỏ quần áo vào & Đóng cửa tủ cạch!
    IoT-->>API: Trả về MQTT (Door Closed)
    
    API->>API: Cập nhật Order = DEPOSITED (Đã gửi)
    
    par Kiosk hoàn tất
        API-->>Kiosk: Trạng thái ô = OCCUPIED
        Kiosk->>Kiosk: Màn hình Kiosk hiện "Cảm ơn quý khách" -> Về trang chủ
    and Mobile yêu cầu thanh toán
        API-)Mobile: Push WebSocket (Status: DEPOSITED)
        Mobile->>Mobile: Thanh tiến độ nhảy sang bước "Thanh toán"
    end

    %% BƯỚC 4: THANH TOÁN
    Note over User, IoT: BƯỚC 4: THANH TOÁN
    User->>Mobile: Thanh toán VNPay/MoMo
    Mobile->>API: Xác nhận thanh toán
    API->>API: Order = PAID
    API-)Mobile: Push WebSocket (Giao dịch thành công)
    Mobile->>Mobile: Hiển thị màn hình Hoàn Tất!
```

---

## Chi tiết các thao tác đồng bộ

### 1. Đồng bộ khi Chọn / Đặt tủ

- **Thao tác Mobile:** Khách hàng thấy ô số #3 (Trống). Nhấn chọn và bấm **Xác nhận thuê**.
- **Đồng bộ:** Dưới Backend, ô số #3 bị khóa lại. Kiosk ngoài đời thực đang đếm lùi chu kỳ 3 giây, ngay lập tức nó fetch
  được layout mới và chuyển ô số #3 từ màu Trống sang **Đã đặt (Reserved)** hoặc **Có đồ (Occupied)**, chặn mọi người
  khác thao tác vào ô này trên Kiosk.

### 2. Đồng bộ khi Mở tủ

Khách hàng có thể linh hoạt dùng **Điện thoại** HOẶC bấm **Kiosk** để mở. Hệ thống không quan tâm luồng nào kích hoạt,
chỉ quan tâm tủ vật lý báo về:

- Nếu khách nhập PIN ở Kiosk: Cửa vật lý mở -> MQTT báo về Backend -> Backend bắn thông báo (Push Notification / STOMP
  WebSocket) xuống điện thoại khách -> **Thanh Stepper ở màn hình Mobile tự động nhảy sang bước "Cửa đang mở"** mà khách
  không cần chạm vào điện thoại.
- Ngược lại, nếu khách đứng xa và bấm "Mở tủ" trên app: Cửa bật ra -> Kiosk tự động nhảy sang màn hình hướng dẫn "Vui
  lòng cho đồ vào tủ số #3".

### 3. Đồng bộ khi Đóng cửa tủ & Thanh toán

- Kiosk và Điện thoại đều ở trạng thái "Chờ khách bỏ đồ vào".
- Khách hàng đẩy cửa tủ vật lý đóng lại. Cảm biến cửa tủ IoT ghi nhận và gửi MQTT `CLOSED`.
- Backend tự động hiểu khách đã bỏ đồ xong, chốt đơn hàng sang trạng thái `DEPOSITED`.
- **Kiosk:** Không có chức năng thanh toán tại tủ, Kiosk sẽ hiển thị "Đã nhận đồ, vui lòng thanh toán trên ứng dụng" rồi
  tự động quay về màn hình chờ (Home).
- **Mobile:** App nhận được tín hiệu WebSocket, Stepper tự động trượt qua màn hình Thanh toán (Checkout). Khách chọn ví
  VNPay/MoMo và trả tiền. Sau khi thanh toán xong, hệ thống chốt Order và tạo Job cho máy bay Drone (nếu có).

> [!TIP]
> Việc ứng dụng kết hợp cả 2 hình thức: **WebSocket (Push)** cho Mobile (để có trải nghiệm Realtime mượt mà, ít tốn pin)
> và **Polling** cho Kiosk (để đơn giản hóa thiết bị màn hình tại chỗ) giúp hệ thống luôn đồng bộ trạng thái một cách
> chuẩn xác nhất, không lo bị nghẽn mạng!
