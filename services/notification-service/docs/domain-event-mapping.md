# 🗺️ ÁNH XẠ SỰ KIỆN DOMAIN (DOMAIN EVENT MAPPING)

Tài liệu này định nghĩa cách **Notification Service** (với vai trò Smart Consumer) xử lý các sự kiện từ các service khác để tạo ra thông báo cho người dùng.

> [!NOTE]
> Đây là việc thực hiện hóa chiến lược Hybrid quy định tại [ADR-0005](../adr/0005-hybrid-notification-triggering-strategy.md).

---

## 1. LỚP DỊCH SỰ KIỆN (EVENT TRANSLATOR LAYER)

Mỗi sự kiện dưới đây khi được nhận từ Message Broker sẽ được chuyển qua một bộ lọc (Translator) để xác định:
- **Người nhận (RecipientId)**: Lấy từ Payload của sự kiện.
- **Loại thông báo (Type)**: TRANSACTIONAL, INTERACTION, hoặc PROMOTIONAL.
- **Nội dung (Content)**: Tự động tạo dựa trên Template.

---

## 2. DANH SÁCH MAPPING CHI TIẾT

### 2.1. Booking Context
| Sự kiện Domain | Loại | Người nhận | Tiêu đề (Title) | Nội dung (Body Template) |
| :--- | :--- | :--- | :--- | :--- |
| `rentagf.booking.requested.v1` | TRANSACTIONAL | Companion | Yêu cầu mới! | Bạn có một yêu cầu đặt lịch mới từ Client. |
| `rentagf.booking.accepted.v1` | TRANSACTIONAL | Client | Đã chấp nhận! | Companion đã chấp nhận yêu cầu đặt lịch #{bookingId} của bạn. |
| `rentagf.booking.rejected.v1` | TRANSACTIONAL | Client | Bị từ chối | Rất tiếc, yêu cầu đặt lịch #{bookingId} đã bị từ chối. |
| `rentagf.booking.cancelled.v1` | TRANSACTIONAL | Đối phương | Lịch hẹn bị hủy | Lịch hẹn #{bookingId} đã bị hủy bởi {actorRole}. |

### 2.2. Finance Context
| Sự kiện Domain | Loại | Người nhận | Tiêu đề (Title) | Nội dung (Body Template) |
| :--- | :--- | :--- | :--- | :--- |
| `rentagf.finance.topup.completed.v1` | TRANSACTIONAL | Client | Nạp tiền thành công | Bạn vừa nạp thành công {amount} Kano-Coin vào ví. |
| `rentagf.finance.payout.processed.v1` | TRANSACTIONAL | Companion | Thu nhập mới | Bạn đã nhận được thanh toán cho booking #{bookingId}. |

### 2.3. Interaction Context
| Sự kiện Domain | Loại | Người nhận | Tiêu đề (Title) | Nội dung (Body Template) |
| :--- | :--- | :--- | :--- | :--- |
| `rentagf.chat.message.sent.v1` | INTERACTION | RecipientId | Tin nhắn mới | {{sender_name}}: {{snippet}} |
| `rentagf.interaction.review.submitted.v1` | INTERACTION | CompanionId | Đánh giá mới! | Bạn vừa nhận được đánh giá {{rating}}⭐ từ khách hàng. |

### 2.4. Profile Context
| Sự kiện Domain | Loại | Người nhận | Tiêu đề (Title) | Nội dung (Body Template) |
| :--- | :--- | :--- | :--- | :--- |
| `rentagf.profile.companion.approved.v1` | PROMOTIONAL | CompanionId | Chúc mừng! | Hồ sơ Companion của bạn đã được duyệt. Hãy bắt đầu nhận lịch ngay! |
| `rentagf.profile.companion.rejected.v1` | TRANSACTIONAL | CompanionId | Hồ sơ bị từ chối | Rất tiếc, hồ sơ Companion của bạn chưa được duyệt. Lý do: {{reason}}. |
| `rentagf.profile.scenario.rejected.v1` | TRANSACTIONAL | CompanionId | Kịch bản bị từ chối | Kịch bản của bạn đã bị từ chối. Lý do: {{reason}}. |
| `rentagf.profile.voice_intro.rejected.v1` | TRANSACTIONAL | CompanionId | Voice Intro bị từ chối | Giới thiệu giọng nói của bạn đã bị từ chối. Lý do: {{reason}}. |
| `rentagf.profile.album_image.rejected.v1` | TRANSACTIONAL | CompanionId | Ảnh bị từ chối | Ảnh album của bạn đã bị từ chối. Lý do: {{reason}}. |

### 2.5. Identity Context
| Sự kiện Domain | Loại | Người nhận | Tiêu đề (Title) | Nội dung (Body Template) |
| :--- | :--- | :--- | :--- | :--- |
| `rentagf.identity.upgrade.approved.v1` | TRANSACTIONAL | UserId | Yêu cầu nâng cấp được duyệt! | Bạn đã được nâng cấp thành Companion. Hãy hoàn thiện hồ sơ ngay! |
| `rentagf.identity.upgrade.rejected.v1` | TRANSACTIONAL | UserId | Yêu cầu nâng cấp bị từ chối | Rất tiếc, yêu cầu nâng cấp Companion của bạn đã bị từ chối. Lý do: {{reason}}. |
| `rentagf.identity.account.locked.v1` | TRANSACTIONAL | UserId | Tài khoản bị khóa | Tài khoản của bạn đã bị khóa. Lý do: {{reason}}. |

### 2.6. Dispute Context
| Sự kiện Domain | Loại | Người nhận | Tiêu đề (Title) | Nội dung (Body Template) |
| :--- | :--- | :--- | :--- | :--- |
| `rentagf.dispute.report.created.v1` | TRANSACTIONAL | AccusedId | Bạn bị báo cáo | Bạn đã bị báo cáo vi phạm. Lý do: {{reason}}. |
| `rentagf.dispute.resolved.v1` | TRANSACTIONAL | Cả ClientId và CompanionId | Kết quả khiếu nại | Khiếu nại cho booking đã được giải quyết: {{resolution}}. |

---

## 3. QUẢN LÝ TEMPLATE QUA YAML (TEMPLATE MANAGEMENT)

Để đảm bảo tính linh hoạt, toàn bộ nội dung thông báo được tách rời khỏi mã nguồn và quản lý tập trung tại file:
`[config/templates.yaml](../config/templates.yaml)`

### Cấu trúc file YAML:
- **`events`**: Chứa danh sách các Domain Event Type.
- **`recipient_field`**: Chỉ định trường nào trong Event Payload chứa ID người nhận (giúp hệ thống tự động trích xuất).
- **`channels`**: Danh sách các kênh ưu tiên gửi.
- **`template`**: Chứa nội dung đa ngôn ngữ (`vi`, `en`). Hỗ trợ Placeholder dạng `{{field_name}}`.

---

## 4. LOGIC XỬ LÝ (PROCESSING LOGIC)

1.  **Giải mã (Consume)**: Nhận CloudEvent từ Broker.
2.  **Tra cứu (Lookup)**: Tìm cấu hình trong `templates.yaml` dựa trên trường `type` của sự kiện.
3.  **Trích xuất Recipient**: Lấy ID người nhận từ payload dựa trên cấu hình `recipient_field`.
4.  **Ráp nội dung (Interpolate)**: 
    - Thay thế các Placeholder `{{...}}` bằng dữ liệu thực tế từ payload.
    - Chọn ngôn ngữ dựa trên cấu hình của User (Mặc định là `vi`).
5.  **Lưu trữ (Persistence)**: 
    - Tạo một bản ghi `Notification` trong Database với trạng thái `PENDING`. 
    - Việc này đảm bảo User có thể thấy thông báo trong phần "Inbox" kể cả khi việc gửi Realtime thất bại.
6.  **Định tuyến & Gửi (Route & Deliver)**: 
    - Kiểm tra trạng thái kết nối SSE. Nếu Online, đẩy tin qua SSE.
    - Nếu Offline hoặc SSE thất bại, chuyển sang gửi Push Notification qua FCM.
7.  **Cập nhật (Update)**: Đánh dấu trạng thái `SENT` hoặc `FAILED` vào Database để phục vụ việc Audit và Retry.

---

## 5. QUẢN LÝ THAY ĐỔI (SCHEMA CHANGES)

- Nếu một service core thay đổi Payload (ví dụ: thêm trường `amount`), chúng ta chỉ cần cập nhật nội dung Placeholder trong file YAML và logic trích xuất ở lớp Translator.
- Khuyến nghị: Sử dụng **Schema Registry** để phát hiện sớm các thay đổi gây gãy (Breaking Changes).
