# ĐỘ TIN CẬY SAGA (SAGA RELIABILITY PATTERNS)

Để kiến trúc phân tán (Microservices) không gặp các lỗi hệ thống trầm trọng và đảm bảo tính nhất quán dữ liệu (Eventual Consistency) mà không cần 2-Phase Commit (2PC), hệ thống áp dụng các Design Pattern sau.

## 1. TRANSACTIONAL OUTBOX PATTERN (CHỐNG MẤT SỰ KIỆN)
Khi một Aggregate thay đổi trạng thái, **Domain Event KHÔNG được gửi thẳng vào Message Broker** (vì nếu ghi DB thành công nhưng lỗi mạng gửi Message, hệ thống sẽ mất đồng bộ vĩnh viễn).

*   **Giải pháp:**
    1.  Context (VD: Booking) lưu sự kiện vào một bảng `Outbox` cục bộ cùng một Database Transaction với việc lưu nghiệp vụ. (ACID đảm bảo nếu lưu trạng thái thành công thì lưu Outbox thành công).
    2.  Một tiến trình Background Worker (ví dụ: Debezium hoặc Polling Job) sẽ đọc bảng `Outbox` và publish các Event sang Kafka/RabbitMQ.
    3.  Đảm bảo sự kiện được gửi đi ít nhất một lần (At-Least-Once Delivery).

## 2. IDEMPOTENCY PATTERN (TÍNH LŨY ĐẲNG - CHỐNG TRÙNG LẶP)
Vì mạng có thể rớt gói tin hoặc Worker gửi lại (Retry), Consumer có thể nhận được một sự kiện đến 2 lần.

*   **Giải pháp:**
    1.  Mỗi Consumer duy trì một bảng `ProcessedEvents` (lưu vết `eventId` hoặc `sagaId`).
    2.  Khi nhận một Event, kiểm tra Idempotency Key trong DB.
    3.  Nếu Key đã tồn tại (đã xử lý trước đó), bỏ qua thực thi nghiệp vụ và trả về `ACK` (Thành công) ngay lập tức cho Broker. Không ném lỗi để Broker xóa message khỏi Queue.

## 3. CQRS (COMMAND AND QUERY RESPONSIBILITY SEGREGATION)
Trong hệ thống chia nhỏ Database, việc Join dữ liệu qua lại rất tốn kém và bất khả thi.

*   **Giải pháp:**
    1.  Tách biệt mô hình Ghi (Write Model - Aggregate) và mô hình Đọc (Read Model - Projections).
    2.  **Projection:** Xây dựng các Read Model riêng biệt chuyên phục vụ View của người dùng.
        *   *Ví dụ 1:* Trang chủ Catalogue (Elasticsearch tại Profile Service) lắng nghe `ProfileUpdated`, `ReviewSubmitted` để tổng hợp thành một document search nhanh chóng chứa cả Info, Price và Rating.
        *   *Ví dụ 2:* Admin Dashboard (MongoDB tại Dispute/BFF) lắng nghe `ReportCreated`, `DepositFailed` để tạo To-do list tập trung cho Admin không phải nhảy qua từng module tìm lỗi.

## 4. CÁC CƠ CHẾ BẢO VỆ KHÁC
*   **Infinite Retry & DLQ:** Nếu quá trình xử lý bù trừ (Compensation/Rollback) thất bại, hệ thống thử lại vô hạn lần (Exponential Backoff). Nếu quá giới hạn cấu hình, đẩy Event vào Dead Letter Queue (DLQ) để Admin xử lý thủ công.
*   **Optimistic Locking (Versioning):** Sử dụng trường `version` trong Database khi cập nhật các dữ liệu nhạy cảm (như Số dư Ví). Nếu 2 phiên bản SAGA cùng update 1 lúc, giao dịch chậm hơn sẽ bị văng lỗi và tự Retry.
*   **Saga State Persistence:** Các Orchestrator (Booking Saga, Dispute Saga) lưu trạng thái tiến trình (VD: `WAITING_FOR_ESCROW`) vào DB. Nếu service sập (Crash), khi khởi động lại, nó sẽ đọc DB để chạy tiếp quá trình dang dở.
