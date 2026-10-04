# Quy Chuẩn Kiến Trúc & Vận Hành Microservice AI Lab (Lab Service Standards)

> [!IMPORTANT]
> **QUY TẮC BẮT BUỘC KHI PHÁT TRIỂN & TRIỂN KHAI LAB SERVICE:**
> Toàn bộ kỹ sư / AI Assistant khi thao tác với hệ thống Lab Service (AI Playground, Code Runner, MongoDB) **BẮT BUỘC** phải tuân thủ các quy tắc kiến trúc, quy chuẩn biến môi trường và quy trình vận hành VPS được quy định tại đây.

---

## 1. Tổng Quan Kiến Trúc Đa Dịch Vụ (Multi-Service Architecture)

Hệ thống diễn đàn HTXSL Forum bao gồm các thành phần sau:

| Dịch vụ | Công nghệ & Lưu trữ | Cổng nội bộ | Tên miền / Định tuyến | Trách nhiệm |
|:---|:---|:---|:---|:---|
| **Frontend** | Vue 3 / Vite SPA (Vercel CDN) | 80/443 | `<domain>` & `www.<domain>` | Giao diện người dùng, định tuyến SPA |
| **Forum Base** | Spring Boot 3 + MySQL 8.0 + Redis 7 | 8080 | `api.<domain>` | Auth, Threads, Posts, Categories, Notifications, News |
| **Lab Service** | Spring Boot 3 + MongoDB 7.0 | 8081 | `lab-api.<domain>` | AI Lab, LLM Streaming, Code Runner Sandbox, Lab Products |
| **MongoDB** | Docker container `forum-mongodb` | 27017 | Nội bộ (`mongo:27017`) | Lưu trữ bài tập Lab, template code, submission, lịch sử chat |

---

## 2. Chuỗi LLM Provider & Cấu Hình Khóa API (LLM Fallback Chain)

`lab-service` sử dụng mô hình thiết kế **Chain of Responsibility** (`LlmProviderChain.java`) với cơ chế tự động chuyển vùng dự phòng (Fallback) khi gặp lỗi hạn ngạch (Rate Limit/Quota Exceeded) hoặc sự cố nhà cung cấp:

```
[Người dùng gửi Prompt]
         │
         ▼
[1. Google Gemini Provider] (gemini-1.5-flash)
         │ (Thất bại / Hết Quota / Không có Key)
         ▼
[2. Groq Provider] (llama-3.3-70b-versatile - Tốc độ cực nhanh)
         │ (Thất bại / Hết Quota / Không có Key)
         ▼
[3. OpenRouter Provider] (meta-llama/llama-3.3-70b-instruct:free)
         │ (Thất bại / Hết Quota / Không có Key)
         ▼
[4. Mock LLM Provider] (Giả lập phản hồi thử nghiệm, không làm gián đoạn hệ thống)
```

### Danh Sách Biến Môi Trường Cần Khai Báo Trong `/var/www/forum/.env`:

1. **`LAB_GEMINI_API_KEY`**: Khóa API Google Gemini. Lấy miễn phí tại [Google AI Studio](https://aistudio.google.com/app/apikey).
2. **`LAB_GROQ_API_KEY`**: Khóa API Groq. Lấy miễn phí tại [Groq Console](https://console.groq.com/keys).
3. **`LAB_OPENROUTER_API_KEY`**: Khóa API OpenRouter. Lấy miễn phí tại [OpenRouter Settings Keys](https://openrouter.ai/settings/keys).
4. **`LAB_CORS_ALLOWED_ORIGINS`**: Danh sách Origin được phép gọi API Lab (VD: `https://htxslvn.com,https://www.htxslvn.com`).
5. **`LAB_SHARED_SECRET`**: Khóa bí mật dùng để xác thực cuộc gọi nội bộ giữa forum-base-service và lab-service.

---

## 3. Quy Chuẩn Đổi Tên Miền & Nginx Reverse Proxy Cho Subdomain `lab-api`

### A. Cấu hình DNS Cloudflare:
Khi cấu hình tên miền mới, **BẮT BUỘC** phải có cả 2 bản ghi `A` trỏ về IP VPS:
- `api` -> `<IP_VPS>` (DNS only lúc đầu)
- `lab-api` -> `<IP_VPS>` (DNS only lúc đầu)

### B. Vận hành script `scripts/update-domain.sh`:
Script đã được tối ưu hóa để **tự động trích xuất cấu hình từ file `/var/www/forum/.env`**:
- Nếu chạy `sudo bash /var/www/forum/scripts/update-domain.sh` mà không truyền tham số, script sẽ tự đọc `APP_FRONTEND_URL` và tự sinh ra các tên miền:
  - `NEW_API_DOMAIN="api.<domain>"`
  - `NEW_LAB_API_DOMAIN="lab-api.<domain>"`
  - `CERTBOT_EMAIL` từ cấu hình email quản trị.
- Nếu muốn chỉ định thủ công:
  ```bash
  sudo NEW_API_DOMAIN="api.htxslvn.com" NEW_FRONTEND_DOMAIN="htxslvn.com" CERTBOT_EMAIL="admin@htxslvn.com" bash /var/www/forum/scripts/update-domain.sh
  ```
- Nginx sẽ sinh ra 2 khối Virtual Host độc lập:
  - `api.<domain>` -> Proxy sang `http://127.0.0.1:8080` (Forum Base)
  - `lab-api.<domain>` -> Proxy sang `http://127.0.0.1:8081` (Lab Service) kèm cấu hình SSE / WebSocket buffer off.

---

## 4. Chuẩn Mực Code Phía Frontend Đối Với Lab Service

1. **Endpoint cấu hình tập trung**:
   - `VUE_APP_LAB_API_BASE_URL` trong `.env` và `.env.production`.
2. **Không hardcode đường dẫn Lab**:
   - Khai báo route hằng số trong `frontend/src/shared/utils/constants.js`:
     ```javascript
     export const LAB_ROUTES = Object.freeze({
       ROOT: '/phong-thi-nghiem',
       PLAYGROUND: '/lab/playground',
       DETAIL: '/lab/product/:id'
     });
     ```
   - Xử lý nhận diện URL qua helper thuần tại `frontend/src/shared/utils/utils.js`:
     ```javascript
     export function isLabMenu(menu) { ... }
     export function isLabPath(path) { ... }
     ```
