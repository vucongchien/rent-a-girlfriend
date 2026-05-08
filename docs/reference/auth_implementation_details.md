# CHI TIẾT TRIỂN KHAI AUTHENTICATION & AUTHORIZATION

Tài liệu này mô tả chi tiết các cơ chế kỹ thuật nội bộ của Identity Service liên quan đến việc quản lý Token và Khóa ký (Signing Keys). Các service khác thường không cần quan tâm đến logic xoay vòng này, chỉ cần sử dụng Public Key từ JWKS để xác thực.

## 1. CƠ CHẾ TOKEN (JWT)

Hệ thống sử dụng cặp Token để duy trì phiên đăng nhập và bảo mật:

### 1.1. Access Token
*   **Thời hạn:** Ngắn (ví dụ: 15 - 60 phút).
*   **Mục đích:** Đính kèm vào Header `Authorization: Bearer <token>` để gọi API.
*   **Nội dung (Payload):** Chứa `sub` (userId), `role`, `email`, `exp`, `iat`, và `iss`.

### 1.2. Refresh Token & Rotation (Xoay vòng)
*   **Thời hạn:** Dài (ví dụ: 7 - 30 ngày).
*   **Cơ chế Xoay vòng (Rotation):** 
    *   Mỗi khi Client sử dụng Refresh Token để lấy Access Token mới, Identity Service sẽ **hủy bỏ** Refresh Token cũ và cấp một Refresh Token **mới**.
    *   **Phát hiện tấn công:** Nếu một Refresh Token cũ (đã bị hủy) được sử dụng lại, Identity Service sẽ lập tức thu hồi toàn bộ các Refresh Token liên quan đến User đó để đảm bảo an toàn.
*   **Lưu trữ:** Refresh Token được lưu trong Database (Identity DB) kèm trạng thái và ID thiết bị/phiên.

---

## 2. QUẢN LÝ KHÓA (KEY MANAGEMENT - JWKS)

Identity Service sử dụng thuật toán ký bất đối xứng (ví dụ: RS256 hoặc ES256).

### 2.1. JWKS Endpoint
*   **URL:** `GET /.well-known/jwks.json`
*   **Định dạng:** JSON Web Key Set (JWKS).
*   **Nội dung:**
    ```json
    {
      "keys": [
        {
          "kty": "RSA",
          "use": "sig",
          "kid": "v1.0.1",
          "alg": "RS256",
          "n": "...",
          "e": "AQAB"
        }
      ]
    }
    ```
*   **Key ID (kid):** Mỗi khóa được gán một `kid` duy nhất. Header của JWT sẽ chứa `kid` tương ứng để các Service/Gateway biết nên dùng khóa nào để verify.

### 2.2. Chiến lược xoay khóa (Key Rotation)
*   **Tần suất:** Định kỳ (ví dụ: 30 - 90 ngày) hoặc khi nghi ngờ lộ khóa.
*   **Cơ chế Grace Period:**
    1.  Tạo khóa mới và thêm vào danh sách `keys` trong JWKS.
    2.  Dùng khóa mới để ký các JWT mới cấp phát.
    3.  Giữ lại khóa cũ trong JWKS một khoảng thời gian (ví dụ: 24h) để các JWT cũ vẫn có thể được xác thực thành công trước khi hết hạn.
    4.  Xóa khóa cũ khỏi JWKS.

