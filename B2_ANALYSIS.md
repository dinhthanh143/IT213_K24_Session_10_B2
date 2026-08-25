# BÁO CÁO PHÂN TÍCH & TỐI ƯU CODE TÍCH HỢP LANGFUSE TRACING
**Dự án:** Rikkei Intelligent Banking & Assistant Suite (RikkeiPay)  
**Phân hệ:** Trợ lý ảo giao dịch ngân hàng thông minh (RikkeiPay Assistant)  
**Tác vụ:** Dò lỗi bảo mật, logic & Refactor mã nguồn tích hợp Langfuse Java SDK

---

## 1. Phân Tích Lỗ Hổng Bảo Mật & Lỗi Logic Trong Đoạn Code Cũ

### 1.1. Lỗ hổng bảo mật (Security Vulnerabilities)
1. **Hardcode API Keys trong mã nguồn (`LangfuseConfig.java`):**
   * **Vấn đề:** Khóa công khai (`pk-lf-...`) và khóa bí mật (`sk-lf-...`) bị ghi cứng (hardcoded) trực tiếp trong file Java code.
   * **Rủi ro:** Khi commit code lên Git repository (GitHub/GitLab), bất kỳ ai có quyền đọc kho mã nguồn đều có thể đánh cắp API keys, chiếm quyền truy cập dashboard Langfuse, đọc trộm dữ liệu trace hoặc gửi telemetry giả mạo làm sai lệch số liệu giám sát.
   * **Vi phạm chuẩn:** Vi phạm nghiêm trọng chuẩn an toàn thông tin OWASP Top 10 (A07: Identification and Authentication Failures) và tiêu chuẩn bảo mật ngân hàng PCI-DSS.

2. **Rò rỉ dữ liệu nhạy cảm của khách hàng PII / Financial Data (`TransferService.java`):**
   * **Vấn đề:** Gửi trực tiếp số tài khoản đích (`toAccount`), tên/ID người dùng (`user`), và số tiền giao dịch (`amount`) dưới dạng plain-text lên trường `input` và `output` của trace.
   * **Rủi ro:** Nền tảng quan sát (Observability Platform) không được phép lưu trữ số tài khoản ngân hàng hoặc thông tin định danh cá nhân mà không được mã hóa/che mờ (Masking). Nếu dashboard Langfuse bị rò rỉ hoặc nhân viên hỗ trợ xem logs, thông tin giao dịch tài chính của khách hàng sẽ bị lộ.

### 1.2. Lỗi logic & Kiến trúc vận hành (Logic & Operational Flaws)
1. **Thiếu định danh Session và User (`sessionId`, `userId`):**
   * **Vấn đề:** Trace chỉ được gán thuộc tính `.name("bank-transfer")` và text, hoàn toàn thiếu `userId`, `sessionId`, `tags`, `metadata`.
   * **Rủi ro:** Không thể nhóm (group) các lượt giao dịch theo từng phiên hội thoại (Chat Session) của trợ lý RikkeiPay Assistant, không thể phân tích hành vi theo từng người dùng (User Analytics), và không thể tính toán chi phí LLM theo từng User/Session.

2. **Thiếu xử lý ngoại lệ và trạng thái giao dịch (Error Handling & Trace Status):**
   * **Vấn đề:** Giao dịch được coi là "Thành công" và gán output ngay sau lệnh `System.out.println` mà không có khối `try-catch`.
   * **Rủi ro:** Nếu tiến trình chuyển tiền nghiệp vụ gặp lỗi ngoại lệ (Insufficient Balance, Timeout, System Error), trace trên Langfuse vẫn hiển thị là thành công hoặc bị bỏ dở (orphan trace), gây sai lệch dữ liệu giám sát và không phát hiện được sự cố.

3. **Sử dụng `@Autowired` trên field thay vì Constructor Injection:**
   * **Vấn đề:** Khó viết Unit Test (cần dùng reflection/mock phức tạp) và vi phạm best practices của Spring Boot.

---

## 2. Kế Hoạch & Giải Pháp Refactor

1. **Quản lý cấu hình qua `application.yml` & `@ConfigurationProperties`:**
   * Đưa toàn bộ cấu hình `public-key`, `secret-key`, `base-url`, `enabled` vào file `application.yml`.
   * Hỗ trợ nạp cấu hình qua Biến môi trường (Environment Variables) để bảo mật khi triển khai CI/CD và môi trường Production.
2. **Cơ chế che mờ dữ liệu nhạy cảm (PII Masking):**
   * Xây dựng `PiiMaskingUtils` để che mờ số tài khoản ngân hàng (ví dụ: `1903****4567`) và ẩn bớt định danh trước khi ghi telemetry lên Langfuse.
3. **Định danh Trace đa chiều (Multi-dimensional Tracing):**
   * Bổ sung `userId`, `sessionId`, `metadata`, `tags`, `release/version` và `level` vào Trace.
4. **Bổ sung quản lý vòng đời và xử lý ngoại lệ:**
   * Dùng khối `try-catch-finally` để cập nhật trạng thái `output` khi thành công hoặc ghi nhận `error`/`exception` khi thất bại.

---

## 3. Cấu Trúc Mã Nguồn Sau Khi Refactor

```
Session10/B2/
├── B2_ANALYSIS.md                          # Bản phân tích chi tiết lỗ hổng và giải pháp
├── application.yml                         # Tệp cấu hình Spring Boot an toàn
└── src/main/java/com/rikkeipay/
    ├── config/
    │   ├── LangfuseProperties.java         # @ConfigurationProperties nạp cấu hình
    │   └── LangfuseConfig.java             # Bean LangfuseClient an toàn
    ├── util/
    │   └── PiiMaskingUtils.java            # Tiện ích che mờ dữ liệu PII/Tài khoản
    └── service/
        └── TransferService.java            # Service giao dịch tích hợp Tracing chuẩn hóa
```
