# TÍCH HỢP HỆ THỐNG NGOẠI VI (EXTERNAL INTEGRATIONS)

Hệ thống Rent-a-Girlfriend tích hợp với các dịch vụ bên thứ ba thông qua ranh giới Anti-Corruption Layer (ACL). Tài liệu này mô tả chi tiết chiến lược tích hợp.

## 1. TÍCH HỢP THANH TOÁN (VNPAY / VÍ ĐIỆN TỬ)
Cổng thanh toán xử lý luồng nạp tiền ảo (Kano-Coin) vào hệ thống.
*   **Service đảm nhiệm:** `Finance Service`
*   **Mô hình tích hợp:** Asynchronous Webhook (IPN).
*   **Chiến lược:**
    *   Cung cấp IPN/Webhook Endpoint độc lập để nhận kết quả từ VNPay.
    *   **Source of Truth:** Lấy IPN callback từ VNPay làm "Nguồn sự thật duy nhất". Không tin tưởng tuyệt đối vào request chuyển trang thành công từ phía Web/Mobile Client gửi lên.
    *   **Bảo mật:** Xác thực chữ ký `Signature` của VNPay gửi về.
    *   **Đối soát:** Chạy Job đối soát (Reconciliation) hàng ngày để phát hiện chênh lệch giữa IPN và đối soát của VNPay, tránh thất thoát dòng tiền.

## 2. TÍCH HỢP LƯU TRỮ MEDIA (AWS S3 / CLOUDINARY)
Quản lý việc tải lên và lưu trữ Ảnh đại diện, Album và Voice Intro.
*   **Service đảm nhiệm:** `Profile Service`
*   **Mô hình tích hợp:** Presigned URL Pattern (Synchronous cấp quyền, Asynchronous xử lý ảnh).
*   **Chiến lược:**
    *   Hệ thống backend **không trực tiếp nhận** luồng byte upload của User để tránh nghẽn băng thông.
    *   Khi Client muốn upload, gọi API lấy Presigned URL.
    *   Client tự tải trực tiếp tệp tin lên hạ tầng Cloud.
    *   Sau khi upload thành công, Client gửi lại URL cho Backend lưu vào Database.

## 3. TÍCH HỢP XÁC THỰC (GOOGLE OAUTH)
Xác thực người dùng tham gia nền tảng.
*   **Service đảm nhiệm:** `Identity Service`
*   **Mô hình tích hợp:** Synchronous Redirect & Callback (OAuth 2.0 Flow).
*   **Chiến lược:**
    *   Identity Context đóng vai trò xử lý luồng OAuth, tạo tài khoản nội bộ tự động nếu là đăng nhập lần đầu.
    *   Quy đổi Token Google thành Internal JWT Token dùng chung trong hệ thống để giảm phụ thuộc.

## 4. TÍCH HỢP THÔNG BÁO PUSH (FIREBASE / SES)
Phân phối thông báo đến người dùng.
*   **Service đảm nhiệm:** `Notification Service`
*   **Chiến lược:** 
    *   Sử dụng chiến lược Fallback. Cố gắng đẩy thông báo qua kênh Realtime (SSE - Server-Sent Events) kết nối nội bộ trước.
    *   Nếu client Offline hoặc timeout, gửi tiếp thông báo qua hạ tầng FCM (Firebase Cloud Messaging) cho Push Notification trên điện thoại hoặc AWS SES (Email).
