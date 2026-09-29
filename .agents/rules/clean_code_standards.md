# Chuẩn Mực Lập Trình (Clean Code & Architectural Standards)

Tài liệu quy chuẩn bắt buộc áp dụng cho toàn bộ mã nguồn của dự án (cả Backend Spring Boot và Frontend Vue.js). Bất kỳ thay đổi mã nguồn nào cũng phải tuân thủ nghiêm ngặt các quy định sau.

---

## 1. Nguyên Tắc "Single Source of Truth For Constants" (Quản Lý Hằng Số Tập Trung Duy Nhất)

### A. Phía Backend (Java / Spring Boot):
- **Class duy nhất lưu trữ hằng số**: Toàn bộ hằng số trong toàn hệ thống Backend **BẮT BUỘC** phải được định nghĩa tập trung duy nhất tại class `com.forum.utils.Constants`.
- **CẤM khai báo hằng số rải rác**: Tuyệt đối **KHÔNG** khai báo `public static final` hay `private static final` mang tính chất cấu hình/nghiệp vụ ở bất kỳ Class nào khác (như Service, Controller, Config, Component, Initializer, Filter, v.v.). Ngoại lệ kỹ thuật duy nhất là `serialVersionUID` hoặc logger `log`.
- **Phạm vi hằng số cần tập trung vào `Constants.java`**:
  - Tên quyền & Role: `ROLE_USER`, `ROLE_ADMIN`, `ROLE_SUPER_ADMIN`, `ROLE_NON_OFFICIAL_USER`.
  - Biểu thức phân quyền SpEL: `PRE_AUTH_ADMIN_OR_SUPER_ADMIN = "hasAnyRole('ADMIN', 'SUPER_ADMIN')"`, v.v.
  - Setting Keys & Default Values: `SETTING_NEWS_BOT_ENABLED`, `DEFAULT_NEWS_BOT_ENABLED`, `SETTING_PROFANITY_FILTER_ENABLED`, v.v.
  - Bot User Configs: `NEWS_BOT_USERNAME`, `NEWS_BOT_DISPLAY_NAME`, `NEWS_BOT_EMAIL`, `NEWS_BOT_TITLE_NAME`, `NEWS_BOT_AVATAR_BASE_URL`, v.v.
  - Menu & Route URLs mặc định: `MENU_NEWS_URL = "/diem-tin"`, `MENU_NEWS_TITLE = "Điểm tin"`, v.v.
  - Label & Category IDs / Names: `NEWS_LABEL_NAME`, `CATEGORY_AI_PROGRAMMING_ID`, v.v.
  - Scope, Type, Member sorting keys, v.v.

### B. Phía Frontend (Vue.js / JavaScript):
- **File duy nhất lưu trữ hằng số**: Toàn bộ hằng số trong toàn hệ thống Frontend **BẮT BUỘC** phải được định nghĩa tập trung duy nhất tại file `@/shared/utils/constants.js`.
- **Phạm vi `constants.js`**: **CHỈ CHỨA GIÁ TRỊ HẰNG SỐ & OBJECTS CẤU HÌNH CỐ ĐỊNH**.
  - `ROLES`, `ROLE_BADGE_CLASSES`, `ROLE_NAMES`.
  - `THREAD_SCOPES`.
  - `NOTIFICATION_TYPES`, `NOTIFICATION_TAB_KEYS`, `NOTIFICATION_LABEL_STYLES`, `NOTIFICATION_TEXTS`.
  - `NEWS_TABS`, `NEWS_LABEL_NAME`, `TITLE_TYPES`, `MEMBER_KEYS`, `SETTINGS`, `FORGOT_PASSWORD_STEPS`.
- **CẤM VIẾT HÀM XỬ LÝ TRONG `constants.js`**: `constants.js` tuyệt đối không chứa hàm xử lý logic (no helper functions, no methods).

---

## 2. Phân Định Rõ Ràng Trách Nhiệm Tiện Ích Frontend (`constants.js` vs `utils.js` vs `mixins`)

1. **`@/shared/utils/constants.js`**:
   - Chỉ chứa giá trị hằng số (numbers, strings, booleans, objects/dictionaries cấu hình tĩnh).
   - Không chứa bất kỳ câu lệnh logic thực thi hay hàm `function (...)` / arrow function nào.
2. **`@/shared/utils/utils.js`**:
   - Chứa các **Hàm Xử Lý Logic Đơn Thuần (Pure Helper Functions)** dùng chung cho toàn hệ thống:
     - Xử lý chuyển đổi, tra cứu (ví dụ: `getRoleBadgeClass(role)`, `getRoleName(role)`, `getNotifLabelStyle(notif)`, `shouldShowExtraThreadPostHint(type)`).
     - Định dạng chuỗi, avatar, thời gian, kiểm tra role/quyền (ví dụ: `isNonOfficialUser()`, `formatAvatarUrl()`, `truncateString()`).
   - Các hàm này không chứa gọi API trực tiếp và không phụ thuộc vào Vue component instance (`this`).
3. **`@/shared/mixins/`**:
   - Chứa các **Hàm Dùng Chung Có Gọi API** hoặc chia sẻ vòng đời/state reactive giữa các Vue Components.
   - Ví dụ: `searchHistoryMixin.js` (quản lý lịch sử tìm kiếm, gọi api lưu/xóa history), các mixin tải dữ liệu lặp lại.

---

## 3. Quy Chuẩn Đồng Bộ Trạng Thái Loading Toàn Hệ Thống (Unified Loading Standard)

- **Bắt buộc dùng chung component `Loading.vue`**:
  - Mọi màn hình, modal hoặc khu vực cần trạng thái chờ tải dữ liệu **BẮT BUỘC** phải sử dụng component chuẩn:
    ```vue
    import Loading from '@/shared/components/Loading.vue'
    // Sử dụng:
    <Loading :visible="loading" text="Đang tải dữ liệu..." />
    ```
  - Đối với các bảng dữ liệu `DataTable.vue`: Sử dụng prop `:loading="loading"` đã được tích hợp sẵn component `Loading`.
- **CẤM TỰ CHẾ LOADING RIÊNG**:
  - Tuyệt đối không tự viết thẻ html loading rải rác: cấm tự tạo `<div class="spinner-border">`, spinner tự chế, hoặc text thô `Đang tải...` nằm trần trụi ngoài giao diện mà không qua component chuẩn.
  - Toàn bộ trải nghiệm chờ đợi của người dùng phải nhất quán về giao diện spinner, màu sắc và typography theo đúng chuẩn Design System của diễn đàn.

---

## 4. Nguyên Tắc "Lean Controller - Rich Service" (Controller Mỏng, Service Dày)

### Quy định đối với Controller:
- **Nhiệm vụ duy nhất**: Tiếp nhận HTTP Request, xác thực cú pháp (validation annotations), ủy quyền xử lý hoàn toàn cho tầng `@Service`, và đóng gói kết quả trả về `ResponseEntity<ResponseDTO<...>>`.
- **CẤM inject Repository vào Controller**: Controller tuyệt đối không được inject trực tiếp bất kỳ `*Repository` nào. Mọi thao tác truy xuất dữ liệu phải thông qua `@Service`.
- **CẤM viết logic nghiệp vụ trong Controller**:
  - Không viết các khối phân nhánh nghiệp vụ (`switch-case`, `if-else` quyết định luồng xử lý hoặc tính toán số liệu).
  - Không tự truy vấn Entity phụ trợ (ví dụ tìm `Label`, tìm `User`, mapping ID) trong Controller.
- **Controller mẫu chuẩn**:
  ```java
  @RestController
  @RequestMapping("/api/example")
  @RequiredArgsConstructor
  public class ExampleController {
      private final ExampleService exampleService;

      @GetMapping("/data")
      public ResponseEntity<ResponseDTO<PageResponseDTO<DataDTO>>> getData(...) {
          return ResponseEntity.ok(exampleService.getData(...));
      }
  }
  ```

---

## 5. Nguyên Tắc "Zero Magic Values" (Nói Không Với Magic Strings & Magic Numbers)

- Tuyệt đối không so sánh chuỗi thô hay dùng giá trị số không tên trong logic điều kiện:
  - **Sai**: `switch(role) { case 'ROLE_ADMIN': ... }`
  - **Đúng**: Dùng helper `getRoleBadgeClass(role)` từ `utils.js` (tra cứu từ `ROLE_BADGE_CLASSES` trong `constants.js`).
  - **Sai**: `if (notif.type === 'MENTION')`
  - **Đúng**: `if (notif.type === NOTIFICATION_TYPES.MENTION)`.
  - **Sai (Backend)**: `@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")`
  - **Đúng (Backend)**: `@PreAuthorize(Constants.PRE_AUTH_ADMIN_OR_SUPER_ADMIN)`.
  - **Sai (Backend)**: `initSettingIfAbsent("news_bot_enabled", "true")`
  - **Đúng (Backend)**: `initSettingIfAbsent(Constants.SETTING_NEWS_BOT_ENABLED, Constants.DEFAULT_NEWS_BOT_ENABLED)`.

---

## 6. Quy Chuẩn Xử Lý Lỗi & Thông Báo Người Dùng

- **Backend**:
  - Khi từ chối yêu cầu do vi phạm nghiệp vụ (dữ liệu sai, từ cấm, chưa đủ điều kiện): Ném ngoại lệ cụ thể như `IllegalArgumentException` kèm thông điệp rõ ràng, chỉ rõ nguyên nhân (và từ khóa vi phạm nếu có).
  - Tránh ném `RuntimeException` chung chung gây trả về HTTP 500 lỗi máy chủ.
  - Tầng `GlobalExceptionHandler` bắt và chuyển thành mã `400 Bad Request` mang `ResponseDTO.fail(null, message)`.
- **Frontend**:
  - Luôn trích xuất thông điệp từ máy chủ: `error.response?.data?.message || 'Thông báo lỗi mặc định'`.
  - Không bao giờ nuốt thông điệp từ Backend để hiển thị một câu mơ hồ chung chung.
