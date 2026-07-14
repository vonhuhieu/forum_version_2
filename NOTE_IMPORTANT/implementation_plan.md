# KẾ HOẠCH CHI TIẾT TRIỂN KHAI DIỄN ĐÀN LÊN VPS SINGAPORE (UBUNTU 24.04)

Bản kế hoạch này mô tả từng bước thực hiện từ khâu lập trình, cấu hình hệ thống trên VPS cho đến cấu hình DNS tên miền `htxslvn.com` trên Cloudflare, deploy Frontend trên Vercel và thiết lập CI/CD tự động.

---

## 📋 GIAI ĐOẠN 1: ĐIỀU CHỈNH MÃ NGUỒN BACKEND (LẬP TRÌNH)

### Bước 1: Thêm thuộc tính cấu hình động cho File Upload
Chúng ta sẽ thêm cấu hình chọn nhà cung cấp upload (`app.upload.provider`) vào [application.properties](file:///d:/CONGVIEC/FORUM_SPRING_VUEJS/backend/src/main/resources/application.properties).
*   Mặc định khi chạy local hoặc production sẽ dùng: `local` (lưu đĩa cứng).
*   Có thể cấu hình thành: `cloudinary` nếu muốn đổi ngược lại.
*   Cấu hình đường dẫn thư mục lưu trữ file: `app.upload.local-dir=uploads`.

### Bước 2: Nâng cấp FileUploadService.java
Sửa đổi [FileUploadService.java](file:///d:/CONGVIEC/FORUM_SPRING_VUEJS/backend/src/main/java/com/forum/service/FileUploadService.java):
*   Sử dụng Annotation `@Value("${app.upload.provider}")` và `@Value("${app.upload.local-dir}")` để nhận cấu hình.
*   Trong phương thức `uploadFile(MultipartFile file)`:
    *   Nếu provider là `local`:
        *   Tạo thư mục lưu trữ (nếu chưa tồn tại).
        *   Tạo tên file ngẫu nhiên và an toàn bằng `UUID` để tránh trùng lặp.
        *   Lưu dữ liệu nhị phân của file vào đĩa cứng VPS.
        *   Trả về đường dẫn tương đối dạng `/uploads/tên_file_đã_lưu.đuôi` (phù hợp với quy tắc rewrite của Frontend).
    *   Nếu provider là `cloudinary`:
        *   Giữ nguyên logic gọi API của Cloudinary để tương thích ngược.

### Bước 3: Tạo cấu hình môi trường Production
Tạo file cấu hình mới [application-prod.properties](file:///d:/CONGVIEC/FORUM_SPRING_VUEJS/backend/src/main/resources/application-prod.properties):
*   Kích hoạt profile sản xuất bằng cách trỏ:
    *   `spring.datasource.url=jdbc:mysql://localhost:3306/forum_db?useSSL=false&serverTimezone=UTC` (MySQL nội bộ).
    *   `opensearch.uris=http://localhost:9200` (OpenSearch nội bộ).
    *   `app.upload.provider=local` (Lưu file lên VPS).
    *   Cấu hình CORS để cho phép Vercel kết nối: `app.cors.allowed-origins=https://htxslvn.com`.

---

## 🖥️ GIAI ĐOẠN 2: THIẾT LẬP MÔI TRƯỜNG TRÊN VPS (UBUNTU 24.04)

*(Các bước này sẽ được thực hiện thông qua terminal SSH kết nối vào VPS của bạn)*

### Bước 1: Cài đặt Docker & Docker Compose
Chạy các lệnh cài đặt trên VPS Ubuntu:
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y docker.io docker-compose
sudo systemctl enable --now docker
```

### Bước 2: Chuẩn bị file cấu hình triển khai (docker-compose)
Chuẩn bị thư mục `/var/www/forum/` trên VPS chứa:
*   [docker-compose.yml](file:///d:/CONGVIEC/FORUM_SPRING_VUEJS/docker-compose.yml) (được chỉnh sửa nhẹ để mount volume lưu trữ dữ liệu ra ngoài máy chủ thật giúp tránh mất data MySQL và OpenSearch).
*   Thư mục `uploads/` để chứa tệp tin người dùng tải lên.

### Bước 3: Cấu hình tham số hệ thống cho OpenSearch
Chạy lệnh cấu hình bộ nhớ ảo cho OpenSearch trên VPS (bắt buộc):
```bash
sudo sysctl -w vm.max_map_count=262144
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
```

### Bước 4: Khởi động MySQL & OpenSearch cục bộ
Chạy lệnh khởi động các container cơ sở dữ liệu và tìm kiếm trên VPS:
```bash
docker-compose up -d
```

---

## 🔀 GIAI ĐOẠN 3: CẤU HÌNH CLOUDFLARE & VERCEL FRONTEND

### Bước 1: Cấu hình bản ghi DNS trên Cloudflare
Trong tài khoản Cloudflare quản lý tên miền `htxslvn.com`:
1.  **Trỏ Frontend:** Tạo bản ghi `CNAME`:
    *   `@` (tên miền chính) -> `cname.vercel-dns.com` (Đám mây màu cam: Proxied).
    *   `www` -> `cname.vercel-dns.com` (Đám mây màu cam: Proxied).
2.  **Trỏ Backend:** Tạo bản ghi `A`:
    *   `api` (subdomain của backend) -> `IP_CỦA_VPS_CỦA_BẠN` (Đám mây màu cam: Proxied).

### Bước 2: Gắn tên miền vào Vercel
*   Truy cập Dashboard dự án trên Vercel.
*   Vào mục **Settings > Domains** và thêm tên miền `htxslvn.com` và `www.htxslvn.com`.
*   Vercel sẽ tự động cấp phát chứng chỉ SSL Let's Encrypt cho tên miền của bạn.

---

## 🚀 GIAI ĐOẠN 4: DEPLOY BACKEND, CẤU HÌNH NGINX & THIẾT LẬP CI/CD TỰ ĐỘNG

### Bước 1: Cài đặt và cấu hình Nginx làm Proxy
1.  Cài đặt Nginx trên VPS:
    ```bash
    sudo apt install nginx -y
    ```
2.  Tạo tệp cấu hình `/etc/nginx/sites-available/forum` để định tuyến:
    *   Chuyển tiếp request có tiền tố `/api/` vào Spring Boot chạy tại cổng `8080` (hoặc `7860`).
    *   Định tuyến trực tiếp đường dẫn `/uploads/` đến thư mục tĩnh `/var/www/forum/uploads/` trên VPS để tối ưu hiệu năng tải ảnh mà không cần thông qua Spring Boot.
3.  Kích hoạt cấu hình và restart Nginx:
    ```bash
    sudo ln -s /etc/nginx/sites-available/forum /etc/nginx/sites-enabled/
    sudo nginx -t && sudo systemctl restart nginx
    ```

### Bước 2: Tạo service chạy ngầm (Systemd Service) cho Backend
Tạo file service `/etc/systemd/system/forum-backend.service` để quản lý tiến trình Spring Boot chạy nền, tự động khởi động lại khi VPS restart hoặc có phiên bản code mới:
```ini
[Unit]
Description=Forum Spring Boot Backend
After=syslog.target

[Service]
User=root
LimitNOFILE=65536
WorkingDirectory=/var/www/forum
ExecStart=/usr/bin/java -Dspring.profiles.active=prod -jar /var/www/forum/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```
Chạy lệnh kích hoạt service:
```bash
sudo systemctl daemon-reload
sudo systemctl enable forum-backend
```

### Bước 3: Cấu hình tự động hóa CI/CD qua GitHub Actions
Tạo một workflow GitHub Actions mới `.github/workflows/deploy-vps.yml` để tự động hóa hoàn toàn việc deploy Backend lên VPS mỗi khi có push lên nhánh `main`:
1.  **Mô tả luồng hoạt động:**
    *   **Trigger:** Push code lên nhánh `main`.
    *   **Build:** Maven build ra file `.jar` bằng lệnh `mvn clean package -DskipTests`.
    *   **Transfer (SCP):** Upload file `.jar` sang thư mục `/var/www/forum/app.jar` trên VPS.
    *   **Restart (SSH):** Chạy lệnh `sudo systemctl restart forum-backend` để restart backend.
2.  **Thiết lập các tham số bảo mật (GitHub Secrets):**
    Thêm thông tin bảo mật vào phần **Settings > Secrets and variables > Actions** trên GitHub:
    *   `VPS_HOST`: Địa chỉ IP của VPS Singapore.
    *   `VPS_USERNAME`: Tên tài khoản SSH (thường là `root`).
    *   `VPS_SSH_KEY`: Khóa SSH Private Key.

### Bước 4: Kích hoạt lần deploy đầu tiên qua CI/CD
*   Commit code lên GitHub để chạy luồng CI/CD đầu tiên, kiểm tra xem backend có tự động chạy trên VPS thành công không.

---

## 🗄️ GIAI ĐOẠN 5: DI TRÚ DỮ LIỆU & RE-INDEX OPENSEARCH

### Bước 1: Import dữ liệu MySQL từ Aiven vào VPS
Sử dụng file backup `forum_db_aiven.sql` có sẵn trong máy:
```bash
docker exec -i forum-mysql mysql -u root -proot_password forum_db < forum_db_aiven.sql
```

### Bước 2: Chạy Seeder tái lập chỉ mục tìm kiếm trên OpenSearch
Chạy script seeder để nạp dữ liệu từ MySQL mới vào OpenSearch cục bộ vừa dựng:
*   Script này sẽ quét toàn bộ dữ liệu trong bảng threads, posts và đẩy sang chỉ mục của OpenSearch.

---

## 💾 GIAI ĐOẠN 6: CẤU HÌNH TỰ ĐỘNG SAO LƯU DỮ LIỆU (DATABASE BACKUP) (MỚI)

> [!WARNING]
> Đối với VPS giá rẻ hoặc VPS dùng thử (Trial), rủi ro lỗi phần cứng hoặc bị nhà cung cấp thu hồi/khóa tài khoản đột ngột là có thể xảy ra. Do đó, việc tự động backup dữ liệu hàng ngày ra ngoài đám mây (như Google Drive) là **BẮT BUỘC** để bảo vệ dữ liệu thành viên diễn đàn.

### Bước 1: Tạo Script sao lưu MySQL
Chúng ta sẽ chuẩn bị một shell script `backup.sh` tại `/var/www/forum/scripts/backup.sh` trên VPS để thực hiện:
1.  Chạy lệnh `mysqldump` để sao lưu toàn bộ cơ sở dữ liệu `forum_db` ra file SQL.
2.  Nén file SQL lại thành định dạng `.tar.gz` chứa ngày tháng (Ví dụ: `forum_db_backup_20260627.tar.gz`).
3.  Lưu trữ tạm thời vào một thư mục cục bộ bảo mật trên VPS.

### Bước 2: Tích hợp Rclone để đẩy file lên Google Drive
1.  Cài đặt `rclone` (công cụ đồng bộ đám mây nguồn mở phổ biến) trên VPS:
    ```bash
    sudo apt install rclone -y
    ```
2.  Cấu hình liên kết Rclone với tài khoản Google Drive của bạn (lệnh `rclone config`).
3.  Cập nhật script `backup.sh` để sử dụng rclone upload tự động bản sao lưu lên một thư mục trên Google Drive của bạn:
    ```bash
    rclone copy /var/www/forum/backups/forum_db_backup_xxxx.tar.gz gdrive:HTXSL_Backups/
    ```
4.  Tự động xóa các bản sao lưu cũ trên VPS (giữ lại 7 ngày gần nhất) để tránh đầy dung lượng đĩa cứng VPS.

### Bước 3: Thiết lập Cron Job hàng ngày
Thiết lập tiến trình định kỳ trên VPS chạy script backup vào lúc **2:00 sáng** hàng ngày (thời điểm lượng truy cập thấp nhất):
1.  Mở crontab: `sudo crontab -e`
2.  Thêm dòng lệnh sau:
    ```cron
    0 2 * * * /bin/bash /var/www/forum/scripts/backup.sh >> /var/www/forum/backups/backup.log 2>&1
    ```

---

## 🔍 KẾ HOẠCH XÁC MINH (VERIFICATION PLAN)

1.  **Kiểm tra tính năng đăng bài và tìm kiếm:** Đăng một bài viết mới, thử tìm kiếm xem OpenSearch cục bộ hoạt động chính xác không.
2.  **Kiểm tra tính năng Upload hình ảnh:** Upload avatar và đính kèm ảnh vào bài viết, xác nhận ảnh được lưu vào thư mục `/var/www/forum/uploads/` trên VPS và hiển thị tốt trên tên miền chính `htxslvn.com`.
3.  **Kiểm tra luồng CI/CD:** Thực hiện chỉnh sửa nhỏ trong code, push lên GitHub và quan sát tab *Actions* xem quy trình tự động build và deploy lên VPS có thành công hoàn toàn không.
4.  **Đo đạc hiệu năng API:** Truy cập trang chủ qua `htxslvn.com`, bật tab Network kiểm tra xem API `/api/statistics` có phản hồi tức thì (<100ms) không.
5.  **Xác minh tính năng tự động Backup:** Chạy thử thủ công script `/var/www/forum/scripts/backup.sh` trên VPS và kiểm tra xem thư mục Google Drive của bạn đã nhận được file backup dạng `.tar.gz` chưa.
