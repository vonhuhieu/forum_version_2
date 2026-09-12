# Tài Liệu Chuẩn Hóa Triển Khai & Vận Hành Diễn Đàn Trên VPS (Hệ Thống Tự Động Hóa Dynamic VPS Migration)

Tài liệu này tổng hợp toàn bộ kiến trúc, quy trình vận hành và cơ chế **tự động hóa 100% (Zero-Touch VPS Migration)** của hệ thống diễn đàn **HTXSL Forum** (Spring Boot Backend + Vue.js Frontend + MySQL + OpenSearch + Docker Compose + Google Drive Backup).

---

## 1. Kiến Trúc Hệ Thống Tổng Thể

| Thành phần | Công nghệ / Nền tảng | Vai trò & Đặc điểm |
| :--- | :--- | :--- |
| **Frontend** | Vue.js SPA trên **Vercel** | CDN toàn cầu, chứng chỉ SSL tự động, domain chính `htxslvn.com`. |
| **Backend** | Spring Boot Java 17 trong **Docker** | Chạy độc lập trên VPS Ubuntu 24.04 (service `backend`), kết nối qua domain `api.htxslvn.com`. |
| **Database** | **Docker MySQL 8.0** | Lưu trữ dữ liệu nội bộ trong mạng ảo Docker, dữ liệu lưu bền vững qua Volume `mysql_data`. |
| **Search Engine** | **Docker OpenSearch 2.19.0** | Tìm kiếm full-text tiếng Việt tốc độ cao, lưu qua Volume `opensearch_data`. |
| **Reverse Proxy** | **Nginx + Let's Encrypt SSL** | Cổng vào duy nhất của VPS, định tuyến API `/` tới Spring Boot (8080) và cấp phát trực tiếp file tĩnh `/uploads/`. |
| **DNS & Anti-DDoS** | **Cloudflare** | Quản lý bản ghi DNS, ẩn IP gốc máy chủ (Proxied / Đám mây cam), lọc DDoS / WAF. |
| **Sao Lưu Tự Động** | **Cron Job + Rclone + Google Drive** | Nén Database + thư mục Uploads lúc 02:00 sáng hàng ngày và đẩy lên Google Drive. |
| **CI/CD Tự Động Hóa**| **GitHub Actions** | 2 Workflows: Deploy code hàng ngày (`deploy-vps.yml`) và Khởi tạo VPS tự động (`vps-bootstrap.yml`). |

---

## 2. Hệ Thống 8 GitHub Secrets Cần Thiết

Để toàn bộ quy trình tự động hóa hoạt động và không bị lộ bất kỳ thông tin nào trong mã nguồn, repository GitHub được cấu hình **8 Secrets** tại **Settings > Secrets and variables > Actions**:

| Secret Name | Mô tả | Định dạng / Ví dụ mẫu | Tần suất thay đổi |
| :--- | :--- | :--- | :--- |
| `VPS_HOST` | Địa chỉ IP của máy chủ VPS hiện tại | `${VPS_IP_ADDRESS}` | Đổi khi đổi VPS |
| `VPS_USERNAME` | Tên người dùng SSH | `${VPS_SSH_USER}` (mặc định: `root`) | Giữ nguyên |
| `VPS_PASSWORD` | Mật khẩu tài khoản root của VPS | `${VPS_SSH_PASSWORD}` | Đổi khi đổi VPS |
| `DOMAIN_NAME` | Subdomain API trỏ về VPS | `${APP_API_DOMAIN}` | Cố định (1 lần) |
| `GDRIVE_BACKUP_FOLDER` | Tên thư mục chứa backup trên Google Drive | `${GDRIVE_BACKUP_FOLDER}` | Cố định (1 lần) |
| `CERTBOT_EMAIL` | Email đăng ký chứng chỉ SSL Let's Encrypt | `${YOUR_EMAIL_FOR_SSL}` | Cố định (1 lần) |
| `VPS_ENV_FILE` | Toàn bộ nội dung file `.env` (chứa `JWT_SECRET`, `RESEND_API_KEY`...) đã mã hóa **base64** | Chuỗi base64 (xem mục 3.1) | Cố định (1 lần) |
| `RCLONE_CONF` | Toàn bộ nội dung file `~/.config/rclone/rclone.conf` kết nối Google Drive đã mã hóa **base64** | Chuỗi base64 (xem mục 3.2) | Cố định (1 lần) |

---

## 3. Hướng Dẫn Chuẩn Bị Secrets Một Lần Duy Nhất

### 3.1. Tạo giá trị cho `VPS_ENV_FILE`
Trên máy Windows PowerShell, chạy lệnh sau để lấy chuỗi base64 (điền các giá trị thực tế của con vào):

> ⚠️ **QUAN TRỌNG:** Phải giữ nguyên dòng trống trước `"@` để đảm bảo file `.env` có ký tự xuống dòng ở cuối. Nếu thiếu, các biến môi trường sẽ bị nối dính khi hệ thống tự động append thêm biến mới.

```powershell
$envText = @"
APP_JWT_SECRET=${YOUR_APP_JWT_SECRET}
RESEND_API_KEY=${YOUR_RESEND_API_KEY}
TURNSTILE_SECRET_KEY=${YOUR_TURNSTILE_SECRET_KEY}
APP_FRONTEND_URL=https://hoptacxavuive.com
APP_CORS_ALLOWED_ORIGINS=https://hoptacxavuive.com,https://www.hoptacxavuive.com
RESEND_FROM_EMAIL=admin@hoptacxavuive.com

"@

[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($envText))
```
👉 Copy toàn bộ output dán vào GitHub Secret `VPS_ENV_FILE`.

### 3.2. Cấu hình Rclone với Google Drive và tạo `RCLONE_CONF`
1. Trên Google Cloud Console, tạo OAuth Client ID loại **Desktop App** và thêm tài khoản Gmail vào mục **Test Users** (Audience).
2. Chạy `rclone config` kết nối Google Drive (chọn loại `drive`, nhập Client ID, Client Secret, Scope `1`).
3. Xác thực qua trình duyệt và lưu cấu hình dưới tên remote (ví dụ: `gdrive` hoặc `forum_gdrive_remote`).
4. Trên VPS, chạy lệnh lấy chuỗi base64:
   ```bash
   cat ~/.config/rclone/rclone.conf | base64 -w 0
   ```
👉 Copy output dán vào GitHub Secret `RCLONE_CONF`.

---

## 4. QUY TRÌNH DI TRÚ SANG VPS MỚI (CHỈ 3 BƯỚC THỦ CÔNG)

> **Lưu ý quan trọng:** Vì toàn bộ file cấu hình `.env` và `rclone.conf` (Google Drive) đã được lưu vĩnh viễn trong GitHub Secrets (`VPS_ENV_FILE` và `RCLONE_CONF`), nên từ lần đổi VPS này trở đi, con **KHÔNG CẦN cài đặt môi trường hay chạy `rclone config` thủ công nữa**. Hệ thống sẽ tự động giải mã và nạp vào VPS mới 100%!

Khi VPS cũ bị chết hoặc con đổi sang nhà cung cấp VPS mới, toàn bộ quy trình chỉ gồm **3 thao tác**:

```
Bước 1: Trỏ DNS trên Cloudflare
  └─ Vào Cloudflare > DNS > Sửa bản ghi A "api" -> IP VPS mới (Bật đám mây cam Proxied)

Bước 2: Cập nhật GitHub Secrets
  └─ Cập nhật VPS_HOST (IP mới) và VPS_PASSWORD (mật khẩu mới)

Bước 3: Kích hoạt Workflow Bootstrap
  └─ GitHub > Actions > Chọn "VPS Bootstrap - Khởi tạo VPS mới" > Nhấn "Run workflow"
```

### Hệ thống tự động thực hiện những gì sau khi bấm Run?
```
[Workflow: vps-bootstrap.yml]
  ├── 1. Job Bootstrap (Cài đặt môi trường)
  │     ├─ Cập nhật OS Ubuntu 24.04 (non-interactive)
  │     ├─ Cài Docker, Nginx, Certbot, Rclone
  │     ├─ Cấu hình vm.max_map_count=262144 (OpenSearch)
  │     ├─ Giải mã VPS_ENV_FILE thành /var/www/forum/.env
  │     ├─ Khởi động MySQL 8.0 & OpenSearch 2.19.0 qua Docker Compose
  │     ├─ Cấu hình Nginx Reverse Proxy (/api -> 8080, /uploads -> thư mục tĩnh)
  │     ├─ Cấp phát chứng chỉ SSL Let's Encrypt qua Certbot
  │     └─ Tự động đăng ký Cron Job backup hàng ngày (02:00 AM)
  │
  ├── 2. Job Restore (Khôi phục dữ liệu)
  │     ├─ Giải mã RCLONE_CONF kết nối Google Drive
  │     ├─ Tự động tìm file backup .tar.gz mới nhất trong thư mục Google Drive
  │     ├─ Tải về và giải nén (tự động nhận diện cấu trúc file .sql và folder uploads/)
  │     ├─ Nạp toàn bộ dữ liệu vào MySQL container forum-mysql
  │     ├─ Phục hồi toàn bộ ảnh vào thư mục /var/www/forum/uploads/
  │     └─ Dọn dẹp các file nén tạm thời
  │
  └── 3. Job Deploy (Khởi chạy ứng dụng)
        ├─ Biên dịch mã nguồn Spring Boot bằng Maven (JDK 17)
        ├─ Đẩy app.jar lên VPS và khởi động container forum-backend
        ├─ Kiểm tra Healthcheck (/api/settings/public)
        ├─ Tự động chạy DatabaseSchemaPatcher vá các bảng/cột còn thiếu
        └─ Kích hoạt tiến trình đồng bộ và tái lập chỉ mục (Reindex) OpenSearch
```

---

## 5. Quy Trình Phát Triển & Deploy Hàng Ngày (CI/CD)

Khi phát triển tính năng mới hoặc sửa lỗi trong code:
1. Con thực hiện lập trình và kiểm thử ở máy local.
2. Chạy lệnh commit và push lên nhánh `main`:
   ```bash
   git add .
   git commit -m "feat: mô tả tính năng mới"
   git push origin main
   ```
3. GitHub Actions sẽ tự động kích hoạt workflow **`Deploy Backend to VPS`** (`deploy-vps.yml`):
   - Tự động Maven build ra file JAR mới.
   - SCP đẩy file JAR lên `/var/www/forum/app.jar`.
   - Chạy lệnh `docker compose up -d backend` và restart backend trong **~15-30 giây** mà không làm gián đoạn MySQL và OpenSearch.
   - Tự động kiểm tra sức khỏe API và reindex OpenSearch.

---

## 6. Cơ Chế Tự Động Sao Lưu Dữ Liệu (Backup Hàng Ngày)

Script [`scripts/backup.sh`](file:///d:/CONGVIEC/FORUM_SPRING_VUEJS/scripts/backup.sh) được triển khai tại `/var/www/forum/scripts/backup.sh` trên VPS:

### 6.1. Nguyên lý hoạt động:
1. Chạy lệnh `mysqldump` xuất toàn bộ cơ sở dữ liệu `forum_db` ra file `db.sql`.
2. Sao chép toàn bộ các tệp tin hình ảnh tải lên từ thư mục `/var/www/forum/uploads/`.
3. Đóng gói và nén lại thành tệp tin duy nhất dạng `forum_backup_YYYYMMDD_HHMMSS.tar.gz`.
4. Sử dụng `rclone` đồng bộ file nén lên Google Drive (`forum_rclone_backups_update_01_09_2026`).
5. Tự động xóa các bản sao lưu cục bộ trên VPS có tuổi đời vượt quá **7 ngày** để giải phóng dung lượng đĩa cứng.

### 6.2. Tự Động Đăng Ký Cron Job Trên VPS Mới (Zero-Touch):
Script `bootstrap.sh` đã được thiết lập để **tự động đăng ký Cron Job** vào hệ thống VPS mới mà không cần thao tác thủ công. Dòng lệnh được thêm tự động vào `crontab`:

```bash
0 2 * * * /var/www/forum/scripts/backup.sh >> /var/www/forum/backup.log 2>&1
```

Để kiểm tra lại danh sách Cron Job trên VPS:
```bash
crontab -l
```

---

## 7. Tổng Hợp Các Lỗi Thường Gặp & Biện Pháp Khắc Phục (Troubleshooting)

### 7.1. Lỗi Google OAuth 403 `access_denied` khi cấu hình Rclone
- **Nguyên nhân**: Ứng dụng Google Cloud Console ở chế độ *Testing* nhưng chưa thêm email vào danh sách người dùng thử nghiệm.
- **Khắc phục**: Vào Google Cloud Console > **Google Auth Platform** > **Audience** > Kéo xuống mục **Test users** > Nhấn **`+ ADD USERS`** và nhập email Gmail của con.

### 7.2. Lỗi `Unknown column 'assigned_title_id' in field list` (Mã lỗi 500 sau khi restore)
- **Nguyên nhân**: File backup database cũ thiếu một số cột/bảng do code backend mới bổ sung sau này.
- **Khắc phục**: Class `DatabaseSchemaPatcher.java` được cấu hình chạy `ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...` và `CREATE TABLE IF NOT EXISTS ...` khi backend khởi động, tự động đồng bộ hóa cấu trúc DB cũ lên phiên bản mới nhất.

### 7.3. Lỗi `rclone exit status 3` trong quá trình Bootstrap
- **Nguyên nhân**: Script cài đặt `install.sh` của Rclone trả về status 3 khi phát hiện Rclone đã được cài đặt sẵn trên máy.
- **Khắc phục**: Script `bootstrap.sh` sử dụng `if ! command -v rclone` để kiểm tra trước, tránh gọi lại script cài đặt khi đã có Rclone.

### 7.4. Lỗi Race Condition khi Reindex OpenSearch
- **Nguyên nhân**: Khi backend khởi động, `SearchIndexInitializer` đã kích hoạt reindex ngầm, đồng thời CI/CD gọi thêm endpoint `/api/search/reindex` gây xung đột index.
- **Khắc phục**: Phương thức `reindexAll()` trong `SearchService.java` được bọc try-catch an toàn khi xóa/tạo index, và workflow được bổ sung vòng lặp retry 5 lần kèm độ trễ 5 giây.

### 7.5. Lỗi Backup Tự Động Dừng Đột Ngột Sau Vài Ngày (Google OAuth Token Hết Hạn 7 Ngày)
- **Nguyên nhân**: Khi tạo Google OAuth Client ID trên Google Cloud Console, nếu OAuth Consent Screen ở trạng thái **"Testing"**, Google áp dụng chính sách bảo mật: **Refresh Token sẽ tự động hết hạn và bị thu hồi sau đúng 7 ngày**. Khi đó lệnh `rclone copy` sẽ báo lỗi `Error 400: invalid_grant, Token has been expired or revoked`, làm script backup bị dừng tại Bước 4.
- **Khắc phục triệt để**:
  1. Vào [Google Cloud Console](https://console.cloud.google.com/) > **APIs & Services** > **OAuth consent screen** > Nhấn nút **PUBLISH APP** (Chuyển trạng thái từ *Testing* sang *In production*).
  2. SSH vào VPS và cấp lại token cho Rclone:
     ```bash
     rclone config reconnect $(rclone listremotes | head -1)
     ```
  3. Cập nhật lại Secret `RCLONE_CONF` trên GitHub:
     ```bash
     cat ~/.config/rclone/rclone.conf | base64 -w 0
     ```
     Copy chuỗi output dán đè vào GitHub Secret `RCLONE_CONF`.

### 7.6. Lỗi Backup Nhầm Thư Mục `forum_backups` Thay Vì Thư Mục Cấu Hình
- **Nguyên nhân**: Cron job trên Linux chạy độc lập và không tự động tải file `/var/www/forum/.env`. Do đó biến `GDRIVE_BACKUP_FOLDER` không tồn tại trong môi trường Cron, dẫn đến script tự động fallback về thư mục mặc định `forum_backups`.
- **Khắc phục**: Script `scripts/backup.sh` đã được cập nhật để tự động nạp cấu hình từ `/var/www/forum/.env`. Đồng thời script `bootstrap.sh` cũng tự động đồng bộ biến `GDRIVE_BACKUP_FOLDER` vào file `.env` khi khởi tạo VPS.

### 7.7. Lỗi Upload ảnh dung lượng lớn bị mã 413 hoặc CORS giả
- **Nguyên nhân**: Giới hạn mặc định `client_max_body_size` của Nginx là 1MB.
- **Khắc phục**: Nginx được cấu hình sẵn `client_max_body_size 100M;` trong `sites-available/forum` để đồng bộ với giới hạn 100MB của Spring Boot.

---

## 8. Các Lệnh Thao Tác Nhanh Với Backup Trên VPS

```bash
# 1. Xem nhật ký log chạy backup gần nhất:
cat /var/www/forum/backup.log | tail -n 50

# 2. Chạy thử nghiệm backup ngay lập tức bằng tay:
/bin/bash /var/www/forum/scripts/backup.sh

# 3. Kiểm tra danh sách Cron Job trên VPS:
crontab -l

# 4. Kiểm tra kết nối Google Drive qua Rclone:
rclone lsd $(rclone listremotes | head -1)
```

---

## 9. Danh Mục Các File Cốt Lõi Của Hệ Thống

- 📜 [`.github/workflows/vps-bootstrap.yml`](file:///d:/CONGVIEC/FORUM_SPRING_VUEJS/.github/workflows/vps-bootstrap.yml): Workflow GitHub Actions tự động hóa khởi tạo VPS mới và khôi phục dữ liệu.
- 📜 [`.github/workflows/deploy-vps.yml`](file:///d:/CONGVIEC/FORUM_SPRING_VUEJS/.github/workflows/deploy-vps.yml): Workflow GitHub Actions triển khai backend hàng ngày.
- 📜 [`scripts/bootstrap.sh`](file:///d:/CONGVIEC/FORUM_SPRING_VUEJS/scripts/bootstrap.sh): Script cài đặt OS, Docker, Nginx, SSL trên VPS mới.
- 📜 [`scripts/restore-data.sh`](file:///d:/CONGVIEC/FORUM_SPRING_VUEJS/scripts/restore-data.sh): Script tự động tải và khôi phục database + uploads từ Google Drive.
- 📜 [`scripts/backup.sh`](file:///d:/CONGVIEC/FORUM_SPRING_VUEJS/scripts/backup.sh): Script sao lưu dữ liệu tự động hàng ngày.
- 📜 [`docker-compose.yml`](file:///d:/CONGVIEC/FORUM_SPRING_VUEJS/docker-compose.yml): Cấu hình chạy MySQL 8.0, OpenSearch 2.19.0 và Backend Java 17.
- 📜 [`backend/src/main/java/com/forum/config/DatabaseSchemaPatcher.java`](file:///d:/CONGVIEC/FORUM_SPRING_VUEJS/backend/src/main/java/com/forum/config/DatabaseSchemaPatcher.java): Tự động vá và đồng bộ cấu trúc cơ sở dữ liệu khi khởi động.
