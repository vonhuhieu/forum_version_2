# Hướng Dẫn: Dynamic VPS Migration — Chuyển VPS Chỉ Với 3 Bước

## Tổng Quan

Khi VPS cũ chết, con chỉ cần làm **3 thao tác thủ công**, toàn bộ còn lại hệ thống tự vận hành:

| Thao tác | Thực hiện ở đâu | Thời gian |
|:---|:---|:---|
| 1. Đổi bản ghi `A` của API subdomain → IP VPS mới | Cloudflare Dashboard | ~1 phút |
| 2. Cập nhật `VPS_HOST` và `VPS_PASSWORD` | GitHub Secrets | ~2 phút |
| 3. Nhấn "Run workflow" | GitHub Actions → `vps-bootstrap.yml` | ~2 phút |

**Sau đó chờ ~10-15 phút, hệ thống tự vận hành hoàn toàn!** 🚀

---

## Cài Đặt Một Lần (Setup Secrets lần đầu tiên)

### Các Secrets cần thiết trên GitHub Actions

Truy cập: **GitHub → Settings → Secrets and variables → Actions → New repository secret**

| Secret Name | Mô tả | Giá trị mẫu | Tần suất cập nhật |
|:---|:---|:---|:---|
| `VPS_HOST` | IP của VPS hiện tại | `${VPS_IP_ADDRESS}` | Đổi khi có VPS mới |
| `VPS_USERNAME` | Username SSH | `root` | Giữ nguyên |
| `VPS_PASSWORD` | Mật khẩu SSH của VPS | `${VPS_SSH_PASSWORD}` | Đổi khi có VPS mới |
| `DOMAIN_NAME` | Subdomain API trỏ về VPS | `api.htxslvn.com` | 1 lần duy nhất |
| `GDRIVE_BACKUP_FOLDER` | Thư mục backup trên Google Drive | `forum_rclone_backups_update_01_09_2026` | 1 lần duy nhất |
| `CERTBOT_EMAIL` | Email đăng ký SSL Let's Encrypt | `${YOUR_EMAIL_FOR_SSL}` | 1 lần duy nhất |
| `VPS_ENV_FILE` | Nội dung file `.env` (base64) | Chuỗi base64 | 1 lần duy nhất |
| `RCLONE_CONF` | Nội dung `rclone.conf` (base64) | Chuỗi base64 | 1 lần duy nhất |

#### 1. `VPS_ENV_FILE` — Nội dung file `.env` của VPS

File `.env` chứa tất cả key bí mật của hệ thống. Cách tạo giá trị cho Secret này:

**Trên máy tính cá nhân (Windows PowerShell):**
```powershell
# Tạo file .env tạm với nội dung thực tế
$envContent = @"
APP_JWT_SECRET=${YOUR_APP_JWT_SECRET}
SPRING_MAIL_USERNAME=${YOUR_MAIL_USERNAME}
SPRING_MAIL_PASSWORD=${YOUR_MAIL_APP_PASSWORD}
RESEND_API_KEY=${YOUR_RESEND_API_KEY}
"@
# Encode sang base64
[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($envContent))
```

**Hoặc trên Linux/Mac:**
```bash
cat .env | base64 -w 0
```

Copy toàn bộ output (chuỗi base64) → dán vào giá trị Secret `VPS_ENV_FILE`.

---

#### 2. `CERTBOT_EMAIL` — Email đăng ký SSL

Nhập thẳng địa chỉ email của con (không cần encode). Ví dụ: `${YOUR_EMAIL_FOR_SSL}`

---

#### 3. `RCLONE_CONF` — Cấu hình kết nối Google Drive

> ⚠️ **Lần đầu tiên** cần thực hiện thủ công một lần trên VPS mới để lấy token OAuth Google Drive.

**Các bước thực hiện lần đầu:**

```bash
# 1. SSH vào VPS mới
ssh root@<IP_VPS_MỚI>

# 2. Cài rclone (nếu chưa cài)
apt install rclone -y

# 3. Chạy cấu hình rclone (trình wizard tương tác)
rclone config
```

Trong wizard rclone, con sẽ:
- Chọn `n` (New remote)
- Nhập tên remote, ví dụ: `gdrive`
- Chọn loại storage: `drive` (Google Drive)
- Để trống Client ID và Client Secret (nhấn Enter)
- Chọn scope: `1` (Full access)
- Chọn `n` ở "Edit advanced config"
- Chọn `n` ở "Use auto config" (vì đây là headless server)
- Copy URL xuất hiện → mở trên trình duyệt máy tính cá nhân → đăng nhập Google → copy verification code → dán vào terminal VPS
- Chọn `n` ở "Configure this as a Shared Drive"
- Xác nhận `y`

```bash
# 4. Sau khi config xong, lấy nội dung file rclone.conf encode base64
cat ~/.config/rclone/rclone.conf | base64 -w 0
```

Copy toàn bộ output → dán vào giá trị Secret `RCLONE_CONF` trên GitHub.

**Từ lần sau, workflow sẽ tự động restore rclone.conf — không cần làm thủ công nữa!**

---

## Quy Trình Chuyển VPS (Sau Khi Đã Setup Secrets)

```
Bước 1: Cloudflare DNS
  - Vào cloudflare.com → htxslvn.com → DNS
  - Sửa bản ghi A: api → <IP_VPS_MỚI> (giữ nguyên Proxy enabled)

Bước 2: Cập nhật GitHub Secrets
  - VPS_HOST    → <IP_VPS_MỚI>
  - VPS_PASSWORD → <Mật_khẩu_VPS_mới>

Bước 3: Trigger workflow
  - GitHub → Actions → "VPS Bootstrap - Khởi tạo VPS mới"
  - Nhấn "Run workflow" → "Run workflow"
  - Chờ ~10-15 phút
```

---

## Cấu Trúc Scripts

```
scripts/
├── bootstrap.sh    — Cài đặt môi trường VPS mới (Docker, Nginx, SSL...)
├── restore-data.sh — Khôi phục dữ liệu từ Google Drive
└── backup.sh       — Sao lưu hàng ngày lên Google Drive (chạy bởi Cron)
```

---

## Cài Đặt Backup Tự Động Trên VPS Mới

Sau khi bootstrap xong, cần cài Cron Job để backup tự động hàng ngày:

```bash
# SSH vào VPS mới
ssh root@<IP_VPS_MỚI>

# Mở crontab
crontab -e

# Thêm dòng sau (backup lúc 02:00 sáng hàng ngày)
0 2 * * * /var/www/forum/scripts/backup.sh >> /var/www/forum/backup.log 2>&1
```

> **Lưu ý:** Script `bootstrap.sh` sẽ copy file `scripts/backup.sh` lên VPS nhưng **không tự setup Cron Job** (để tránh chạy backup khi VPS mới chưa có dữ liệu đầy đủ). Con setup Cron Job thủ công sau khi đã verify hệ thống chạy ổn định.

---

## Kiểm Tra Sau Khi Chuyển VPS

Sau khi workflow `vps-bootstrap.yml` chạy xong, kiểm tra theo thứ tự:

- [ ] `https://api.htxslvn.com` → Trình duyệt hiển thị HTTPS, không bị cảnh báo SSL
- [ ] `https://api.htxslvn.com/api/settings/public` → Trả về JSON, không báo lỗi
- [ ] `https://htxslvn.com` → Trang chủ forum load bình thường
- [ ] Đăng nhập tài khoản cũ → Thành công
- [ ] Kiểm tra bài viết cũ còn đầy đủ
- [ ] Upload 1 ảnh thử → Ảnh hiển thị bình thường
- [ ] Tìm kiếm 1 từ khóa → Kết quả trả về đúng
- [ ] SSH vào VPS: `docker compose -f /var/www/forum/docker-compose.yml ps` → Cả 3 container đang `Up`
