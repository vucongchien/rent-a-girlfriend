# API & EVENT BUS (CHIẾN LƯỢC GIAO TIẾP LIÊN DỊCH VỤ)

Tài liệu này định nghĩa cách các Microservices giao tiếp nội bộ thông qua API đồng bộ và nền tảng sự kiện bất đồng bộ.

## 1. GIAO TIẾP ĐỒNG BỘ (SYNCHRONOUS API)

Sử dụng **RESTful API** hoặc **gRPC**. Phương pháp này áp dụng cho:
*   **Các hành vi truy vấn (Query):** Dữ liệu trả về là bắt buộc để có thể tiếp tục luồng xử lý và không làm thay đổi trạng thái của bên cấp dữ liệu (ví dụ: lấy hồ sơ người dùng).
*   **Các lệnh (Command) đặc biệt quan trọng:** Chỉ áp dụng cho các hành động yêu cầu tính **Atomicity** (nguyên tử) và **Strong Consistency** (nhất quán tức thì) ngay tại thời điểm bắt đầu luồng nghiệp vụ để đảm bảo trải nghiệm người dùng hoặc an toàn tài chính.

**Trường hợp sử dụng điển hình:**
1.  `Booking Service` gọi `Profile Service` để lấy *Snapshot* Scenario (Query).
2.  `Booking Service` gọi `Finance Service` (qua gRPC) để thực hiện lệnh `FreezeCoin` (Command). Nếu không khóa được tiền, Booking sẽ không được tạo để tránh trạng thái "treo" chờ đợi.

*Lưu ý:* Hạn chế lạm dụng Sync Command cho các bước sau của quy trình (như Payout/Refund) - những bước này nên sử dụng SAGA/Async để tăng khả năng chịu lỗi.

## 2. GIAO TIẾP BẤT ĐỒNG BỘ (ASYNCHRONOUS EVENT BUS)

Sử dụng **Message Broker (RabbitMQ / Kafka)** làm nền tảng truyền tải **Domain Events**. Đây là phương thức giao tiếp chủ đạo của hệ thống.

*   **Mô hình:** Publish/Subscribe (Pub/Sub). Producer phát Event và không cần chờ phản hồi, Consumer tự do subscribe theo nhu cầu.
*   **Trường hợp sử dụng:** Khi `Booking Service` bắn event `BookingCompleted`, nó không cần biết service nào đang nhận. `Finance Service` tự bắt lấy để chia hoa hồng, `Interaction Service` bắt lấy để khóa khung chat, và `Notification Service` bắt để gửi thông báo.

## 3. CHUẨN ĐỊNH DẠNG SỰ KIỆN (CLOUDEVENTS FORMAT)

Để các Bounded Context giao tiếp an toàn, mọi Event đẩy lên Kafka/RabbitMQ phải tuân thủ chuẩn "Envelope" của **CloudEvents**.

**Cấu trúc ví dụ cho `BookingAccepted`:**
```json
{
  "specversion": "1.0",
  "id": "evt_abc123", 
  "source": "/rent-a-gf/booking-context/booking/bk_999",
  "type": "com.rentagf.booking.BookingAccepted.v1",
  "datacontenttype": "application/json",
  "time": "2023-10-27T10:00:00Z",
  "data": {
    "bookingId": "bk_999",
    "sagaId": "saga_555",
    "companionId": "cmp_123",
    "price": 500
  },
  "extensions": {
    "correlationId": "req_xyz789"
  }
}
```

**Chi tiết các trường bắt buộc:**
*   `id`: UUID duy nhất cho từng event. Dùng để làm Idempotency Key ở Consumer.
*   `type`: Có định dạng `[domain].[context].[EventName].[version]`. Versioning (`.v1`) hỗ trợ nâng cấp cấu trúc payload mà không làm gãy các Consumer cũ.
*   `data`: Payload nghiệp vụ thực tế chứa các thông tin cần thiết.
*   `correlationId` (Extensions): Truyền xuyên suốt từ API Gateway đến tất cả các Context để Debug/Trace log trên hệ thống (Kibana/Datadog).
*   `sagaId` (Trong data hoặc extensions): Định danh của phiên giao dịch phân tán nếu event thuộc một phần của luồng SAGA.
