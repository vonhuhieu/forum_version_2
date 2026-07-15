# Tài Liệu Hướng Dẫn Cấu Hình, Triển Khai & Vận Hành Diễn Đàn Trên VPS (Sử Dụng Docker Compose)

Tài liệu này tổng hợp toàn bộ quy trình, các câu lệnh, file cấu hình và giải thích chi tiết quá trình triển khai hệ thống diễn đàn (Spring Boot Backend + Vue.js Frontend) lên máy chủ VPS mới chạy Ubuntu 24.04, tích hợp CI/CD tự động bằng Docker Compose và cơ chế sao lưu tự động lên Google Drive.

---

## 1. Kiến Trúc Hệ Thống (Trước & Sau Di Trú)

| Thành phần | Trước khi di trú | Sau khi di trú (Hiện tại) | Lý do thay đổi |
| :--- | :--- | :--- | :--- |
| **Frontend** | Vercel (Miễn phí) | Vercel (Miễn phí, Custom Domain) | Giữ nguyên vì Vercel chạy SPA Vue.js cực nhanh, tối ưu CDN toàn cầu. |
| **Backend** | HuggingFace Spaces | **Docker Container (eclipse-temurin:17-jre) trên VPS** | Khắc phục triệt để lỗi ngủ đông (cold start), đóng gói gọn gàng dễ dàng vận hành bằng Docker Compose. |
| **Database** | Aiven MySQL Cloud | **Docker MySQL 8.0 trên VPS** | Lưu trữ cục bộ không giới hạn kết nối, truy vấn nội bộ tốc độ cao và miễn phí. |
| **Tìm kiếm** | Bonsai OpenSearch | **Docker OpenSearch 2.19.0 trên VPS** | Khắc phục giới hạn dung lượng lưu trữ của gói Bonsai Free. |
| **Upload File** | Cloudinary | **Local Storage gắn qua Docker Volume** | Lưu trữ ảnh vĩnh viễn trực tiếp trên ổ cứng VPS, không bị khóa giới hạn băng thông. |

---

## 2. Nhật Ký Chi Tiết 6 Giai Đoạn Triển Khai

### Giai Đoạn 1: Điều Chỉnh Mã Nguồn Backend

Mục tiêu giai đoạn này là nâng cấp Backend hỗ trợ lưu file trực tiếp lên đĩa cứng của VPS và hỗ trợ đọc cấu hình động.

#### Bước 1.1: Cập nhật cấu hình động chọn Provider
- **File thực hiện**: [application.properties](file:///d:/CONG_VIEC/FORUM_SPRING_VUEJS_VERSION_2/forum_version_2/backend/src/main/resources/application.properties)
- **Thao tác**: Thêm 2 thuộc tính cấu hình động:
  ```properties
  app.upload.provider=${APP_UPLOAD_PROVIDER:local}
  app.upload.local-dir=${APP_UPLOAD_LOCAL_DIR:uploads}
  ```
- **Giải thích**: `APP_UPLOAD_PROVIDER` quyết định lưu file ở đâu (`local` hoặc `cloudinary`). Nếu không có biến môi trường truyền vào, mặc định sẽ là `local`.

#### Bước 1.2: Nâng cấp lớp xử lý tải file
- **File thực hiện**: `FileUploadService.java`
- **Thao tác**: Nhúng `@Value` đọc cấu hình và triển khai nhánh code lưu file vật lý:
  - Tự động kiểm tra và tạo thư mục `/uploads` nếu chưa tồn tại.
  - Sử dụng `UUID` để tạo tên file ngẫu nhiên độc nhất tránh trùng lặp.
  - Trả về đường dẫn tương đối dạng `/uploads/filename.ext` để frontend định tuyến mượt mà qua Nginx.

#### Bước 1.3: Tạo cấu hình chạy thực tế trên VPS (Production)
- **File thực hiện**: `application-prod.properties`
- **Thao tác**: Thiết lập các thông số kết nối nội bộ trong Docker:
  - MySQL: kết nối thông qua service name của docker-compose: `jdbc:mysql://mysql:3306/forum_db`
  - OpenSearch: kết nối thông qua service name: `http://opensearch:9200`
  - CORS: Mở quyền truy cập cho tên miền chính thức `https://htxslvn.com` và `https://www.htxslvn.com`.

#### Bước 1.4: Tự động khởi tạo index và phòng chống crash tìm kiếm
- **File thực hiện**: `SearchIndexInitializer.java` và `SearchService.java`
- **Thao tác**:
  - Tạo `SearchIndexInitializer` thực thi `CommandLineRunner` để kiểm tra và khởi tạo index `forum_search` cùng tác vụ chạy ngầm reindex nếu index chưa tồn tại.
  - Sửa `SearchService` dùng `try-catch` bắt `NoSuchIndexException` trả về danh sách rỗng để tránh lỗi 500 sập API tìm kiếm khi thiếu index.

---

### Giai Đoạn 2: Thiết Lập Môi Trường Trên VPS Ubuntu 24.04

Thực hiện trực tiếp trên Terminal của VPS.

#### Bước 2.1: Cập nhật hệ thống
- **Câu lệnh**:
  ```bash
  apt update && apt upgrade -y
  ```
- **Giải thích**: Nâng cấp hệ điều hành và các bản vá lỗi bảo mật lên phiên bản mới nhất.

#### Bước 2.2: Cài đặt Docker & Docker Compose
- **Câu lệnh**:
  ```bash
  apt install -y docker.io docker-compose-v2 nginx certbot python3-certbot-nginx rclone
  systemctl enable --now docker
  ```
- **Giải thích**: Cài đặt nền tảng ảo hóa Docker để chạy cô lập các dịch vụ MySQL, OpenSearch và Backend Spring Boot mà không cần cài đặt JDK 17 trực tiếp trên OS máy chủ.

#### Bước 2.3: Cấu hình bộ nhớ ảo cho OpenSearch
- **Câu lệnh**:
  ```bash
  sysctl -w vm.max_map_count=262144
  echo "vm.max_map_count=262144" | tee -a /etc/sysctl.conf
  ```
- **Giải thích**: OpenSearch yêu cầu tối thiểu `262144` vùng nhớ để khởi chạy không bị crash tự thoát.

#### Bước 2.4: Tạo thư mục dự án và khởi chạy Docker Compose
- **Câu lệnh**:
  ```bash
  mkdir -p /var/www/forum/uploads
  mkdir -p /var/www/forum/backups
  ```
- **Tạo cấu hình biến môi trường**: Ghi file bí mật `/var/www/forum/.env` trên VPS chứa thông tin kết nối thực tế. Định dạng bắt buộc là `KEY=VALUE` (không dùng từ khóa `Environment=` hay ký hiệu `${}`):
  ```env
  APP_JWT_SECRET=chuoi_bi_mat_cua_con
  SPRING_MAIL_USERNAME=email_gui_thu@gmail.com
  SPRING_MAIL_PASSWORD=mat_khau_ung_dung_email
  RESEND_API_KEY=khoa_api_resend_cua_con
  ```
- **Tạo file cấu hình**: [docker-compose.yml](file:///d:/CONG_VIEC/FORUM_SPRING_VUEJS_VERSION_2/forum_version_2/docker-compose.yml) đặt tại `/var/www/forum/docker-compose.yml`:
  ```yaml
  version: '3.8'

  services:
    mysql:
      image: mysql:8.0
      container_name: forum-mysql
      environment:
        MYSQL_ROOT_PASSWORD: root_password
        MYSQL_DATABASE: forum_db
      ports:
        - "127.0.0.1:3306:3306"
      volumes:
        - mysql_data:/var/lib/mysql
      restart: always

    opensearch:
      image: opensearchproject/opensearch:2.19.0
      container_name: forum-opensearch
      environment:
        - cluster.name=opensearch-cluster
        - node.name=opensearch-node
        - discovery.type=single-node
        - bootstrap.memory_lock=true
        - "OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m"
        - DISABLE_INSTALL_DEMO_CONFIG=true
        - DISABLE_SECURITY_PLUGIN=true
        - network.publish_host=127.0.0.1
      ulimits:
        memlock:
          soft: -1
          hard: -1
        nofile:
          soft: 65536
          hard: 65536
      ports:
        - "127.0.0.1:9200:9200"
      volumes:
        - opensearch_data:/usr/share/opensearch/data
      restart: always

    backend:
      image: eclipse-temurin:17-jre-alpine
      container_name: forum-backend
      working_dir: /app
      volumes:
        - ./app.jar:/app/app.jar
        - ./uploads:/app/uploads
        - .env:/app/.env
      env_file:
        - .env
      environment:
        - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/forum_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
        - OPENSEARCH_URIS=http://opensearch:9200
      command: java -Dspring.profiles.active=prod -Duser.timezone=Asia/Ho_Chi_Minh -jar app.jar
      ports:
        - "127.0.0.1:8080:8080"
      depends_on:
        - mysql
        - opensearch
      restart: always

  volumes:
    mysql_data:
    opensearch_data:
  ```
- **Khởi chạy ban đầu (Database & Search):**
  ```bash
  cd /var/www/forum && docker compose up -d mysql opensearch
  ```

---

### Giai Đoạn 3: Cấu Hình Tên Miền Cloudflare & Vercel

Thực hiện trên các bảng điều khiển quản trị DNS/Web:

#### Bước 3.1: Cấu hình DNS trên Cloudflare
- Thêm bản ghi **`CNAME`** với tên `@` và `www` trỏ về `cname.vercel-dns.com` (Bật đám mây cam Proxy).
- Thêm bản ghi **`A`** với tên `api` trỏ về IP mới của VPS `159.223.44.52` (Bật đám mây cam Proxy).

#### Bước 3.2: Gắn tên miền và biến môi trường trên Vercel
- Thêm tên miền `htxslvn.com` vào phần cấu hình Domains của dự án Frontend trên Vercel.
- Thiết lập các biến môi trường trên Vercel Dashboard:
  - `VUE_APP_API_BASE_URL` $\rightarrow$ `https://api.htxslvn.com/api`
  - `VUE_APP_BACKEND_URL` $\rightarrow$ `https://api.htxslvn.com`
  - `VUE_APP_WS_URL` $\rightarrow$ `wss://api.htxslvn.com/ws`

---

### Giai Đoạn 4: Cấu Hình Nginx & CI/CD Tự Động

#### Bước 4.1: Cấu hình Nginx làm Reverse Proxy và SSL HTTPS
- **Tạo file cấu hình định tuyến**: Tạo file `/etc/nginx/sites-available/forum`:
  ```nginx
  server {
      listen 80;
      server_name api.htxslvn.com;
      client_max_body_size 100M;

      location /uploads/ {
          alias /var/www/forum/uploads/;
          expires 30d;
          add_header Cache-Control "public, no-transform";
      }

      location / {
          proxy_pass http://127.0.0.1:8080;
          proxy_set_header Host $host;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
          proxy_set_header X-Forwarded-Proto $scheme;

          # Hỗ trợ kết nối Websocket
          proxy_http_version 1.1;
          proxy_set_header Upgrade $http_upgrade;
          proxy_set_header Connection "upgrade";
      }
  }
  ```
- **Kích hoạt & cài đặt SSL**:
  ```bash
  ln -s /etc/nginx/sites-available/forum /etc/nginx/sites-enabled/
  rm -f /etc/nginx/sites-enabled/default
  nginx -t && systemctl restart nginx
  certbot --nginx -d api.htxslvn.com
  ```

#### Bước 4.2: Thiết lập Pipeline CI/CD tự động
- **File thực hiện**: `.github/workflows/deploy-vps.yml`
- **Cách thức vận hành**:
  1. Build file JAR Spring Boot bằng JDK 17 trên GitHub Actions Runner.
  2. Đẩy file JAR lên `/var/www/forum/` thông qua SCP.
  3. Sử dụng SSH để đổi tên file thành `app.jar`, chạy lệnh `docker compose up -d backend` và `docker compose restart backend` để khởi chạy phiên bản backend mới.
  4. Script thực hiện kiểm tra sức khỏe tại endpoint `/api/settings/public` (chờ tối đa 2 phút). Nếu khởi chạy lỗi, script sẽ phát hiện và dừng pipeline lập tức.
  5. Sau khi backend hoạt động, CI/CD tự động trigger tiến trình đồng bộ OpenSearch thông qua lệnh `docker exec forum-backend wget -qO- --post-data="" http://localhost:8080/api/search/reindex` chạy từ bên trong container (giúp giữ IP nguồn là `127.0.0.1`, vượt qua bộ lọc 401 Unauthorized do NAT của Docker).

---

### Giai Đoạn 5: Di Trú Dữ Liệu & Re-Index OpenSearch

#### Bước 5.1: Đồng bộ dữ liệu MySQL từ máy cũ hoặc máy cá nhân lên VPS mới
- **Chạy trên máy cá nhân**: Đẩy file SQL database lên VPS mới:
  ```powershell
  scp "C:\Users\WIN112025\db_backup.sql" root@159.223.44.52:/var/www/forum/backups/
  ```
- **Chạy trên VPS mới**: Nạp dữ liệu SQL vào MySQL Container:
  ```bash
  docker exec -i forum-mysql mysql -u root -proot_password forum_db < /var/www/forum/backups/db_backup.sql
  rm -f /var/www/forum/backups/db_backup.sql
  ```

#### Bước 5.2: Tái lập chỉ mục tìm kiếm (Reindex)
Vì việc nạp SQL database không đi qua lớp xử lý của Spring Boot, OpenSearch sẽ không tự cập nhật bài viết cũ.
- **Thực hiện trên VPS mới**:
  ```bash
  docker exec forum-backend wget -qO- --post-data="" http://localhost:8080/api/search/reindex
  ```
- **Kết quả**: Spring Boot tự động đồng bộ hóa toàn bộ các bài viết cũ từ MySQL sang OpenSearch.

---

### Giai Đoạn 6: Thiết Lập Tự Động Sao Lưu Dữ Liệu (Backup)

Đảm bảo an toàn dữ liệu tự động hàng ngày lên Google Drive.

#### Bước 6.1: Viết script nén cả Database và tệp tin Uploads
- **File thực hiện**: `/var/www/forum/backup.sh` (cấp quyền thực thi `chmod +x`).
- **Nội dung chính**:
  1. Dump database từ container MySQL thành file `db.sql`.
  2. Gom file `db.sql` và thư mục ảnh tải lên `/var/www/forum/uploads` nén thành file `.tar.gz` chứa ngày giờ.
  3. Dọn dẹp tệp tin tạm và đồng bộ lên Google Drive qua Rclone.
  4. Tự động xóa các file sao lưu cũ hơn 7 ngày.

#### Bước 6.2: Cấu hình Rclone liên kết Google Drive
- Cài đặt Rclone trên VPS và cấu hình liên kết OAuth Google Drive qua lệnh `rclone config`.

#### Bước 6.3: Cấu hình Cron Job chạy tự động lúc 02h00 sáng
```text
0 2 * * * /var/www/forum/backup.sh >> /var/www/forum/backup.log 2>&1
```

---

## 3. Hướng Dẫn Vận Ngày Hàng Ngày & Di Trú Sang VPS Mới

### Quy trình phát triển hàng ngày
1. Môn đồ thực hiện code tính năng mới ở máy local (sử dụng Java 21 chạy local nếu muốn đồng nhất với công ty, nhưng hãy lưu ý **không commit/push file `backend/pom.xml`** để giữ nguyên cấu hình JDK 17 cho CI/CD).
2. Thực hiện Commit & Push các thay đổi lên nhánh chính `main`.
3. GitHub Actions sẽ tự động biên dịch, đẩy lên VPS mới và khởi chạy lại container backend.
4. Kiểm tra logs hoạt động của container trên VPS:
   ```bash
   docker logs -f forum-backend
   ```

### Quy trình di trú sang VPS mới (Khi VPS cũ hết hạn)
Khi cần chuyển đổi sang một VPS hoàn toàn mới khác, con thực hiện 5 bước sau (khoảng 10-15 phút):

1. **Cài đặt môi trường VPS mới:**
   ```bash
   apt update && apt upgrade -y
   apt install -y docker.io docker-compose nginx certbot python3-certbot-nginx rclone
   systemctl enable --now docker
   sysctl -w vm.max_map_count=262144
   echo "vm.max_map_count=262144" | tee -a /etc/sysctl.conf
   ```
2. **Khởi tạo thư mục và dịch vụ trên VPS mới:**
   * Tạo thư mục `/var/www/forum/uploads` và `/var/www/forum/backups`.
   * Tạo lại file `/var/www/forum/.env` chứa các giá trị thực tế.
   * Copy nội dung file `docker-compose.yml` đặt vào `/var/www/forum/docker-compose.yml`. Chạy lệnh: `cd /var/www/forum && docker compose up -d mysql opensearch`.
   * Thiết lập file Nginx `/etc/nginx/sites-available/forum` và liên kết sang `sites-enabled`.
3. **Cập nhật DNS & Cấp lại SSL:**
   * Thay đổi địa chỉ IP bản ghi `api.htxslvn.com` trỏ sang IP của VPS mới trên Cloudflare.
   * Chạy lệnh cấp SSL: `certbot --nginx -d api.htxslvn.com`.
4. **Kết nối CI/CD:**
   * Cập nhật `VPS_HOST` và `VPS_PASSWORD` trong mục Settings > Secrets của repository GitHub.
   * Đẩy code hoặc trigger chạy lại Actions để tải Backend lên VPS mới.
5. **Khôi phục dữ liệu:**
   * Tải bản backup `.tar.gz` mới nhất từ Google Drive về đặt vào `/var/www/forum/backups/` trên VPS mới.
   * Giải nén và khôi phục:
     ```bash
     tar -xzf /var/www/forum/backups/tên_file_backup.tar.gz -C /var/www/forum/backups/
     cp -r /var/www/forum/backups/uploads/* /var/www/forum/uploads/
     docker exec -i forum-mysql mysql -u root -proot_password forum_db < /var/www/forum/backups/db.sql
     rm -rf /var/www/forum/backups/uploads /var/www/forum/backups/db.sql
     # Đồng bộ chỉ mục OpenSearch:
     docker exec forum-backend wget -qO- --post-data="" http://localhost:8080/api/search/reindex
     ```

---

## 4. Hướng Dẫn Giám Sát Tài Nguyên Hệ Thống (CPU, RAM, Đĩa Cứng)

Để đảm bảo VPS luôn hoạt động tốt, con có thể sử dụng các lệnh giám sát sau:
* **Kiểm tra RAM:** `free -h` (theo dõi lượng `available` còn lại).
* **Giám sát trực quan CPU & RAM:** `htop` (nhấn `q` để thoát).
* **Xem tài nguyên tiêu tốn bởi các container:** `docker stats` (nhấn `Ctrl + C` để thoát).
* **Kiểm tra đĩa cứng còn trống:** `df -h /` (đảm bảo `Use%` dưới 90%).

---

## 5. Hướng Dẫn Cấu HÌnh Gửi Email Bất Đồng Bộ qua Resend HTTP API
* **Gửi email bất đồng bộ (@Async):** Logic gửi email chạy ngầm giúp API phản hồi ngay lập tức cho client (< 100ms), tránh timeout Gateway 504.
* **Xác thực tên miền:** Phải cấu hình các bản ghi DNS (DKIM, SPF, DMARC) của `htxslvn.com` sang Resend.com.
* **API Key:** Khai báo khóa `RESEND_API_KEY` trong file `/var/www/forum/.env` của VPS mới.

---

## 6. Khắc Phục Lỗi Index OpenSearch Bị Xóa (Lỗ hổng bảo mật & Cơ chế Tự Động Khởi Tạo)
* **Lỗ hổng cũ:** Map cổng `"9200:9200"` ra internet công cộng khiến tin tặc quét IP và xóa index.
* **Biện pháp bảo mật:** docker-compose cấu hình chỉ lắng nghe local loopback `"127.0.0.1:9200:9200"` hoặc giao tiếp thuần nội bộ trong mạng ảo Docker.
* **Khởi tạo tự động:** `SearchIndexInitializer` kiểm tra index khi khởi động, nếu trống sẽ chạy tác vụ ngầm tạo lại index và reindex tự động để chống crash trang chủ.

---

## 7. Hướng Dẫn Kết Nối Database Production Bằng DBeaver (Qua SSH Tunnel)
Vì MySQL Docker chỉ lắng nghe tại loopback `127.0.0.1:3306`, con kết nối thông qua DBeaver bằng tính năng **SSH Tunnel**:
1. **Tab SSH**: Tích chọn *Use SSH Tunnel*, điền IP VPS `159.223.44.52`, port `22`, user `root`, và mật khẩu SSH.
2. **Tab Main**: Điền Host `127.0.0.1`, Port `3306`, Database `forum_db`, User `root`, và mật khẩu MySQL (`root_password`).

---

## 8. Khắc Phục Các Sự Cố Liên Quan Đến Upload Ảnh Lên VPS

### Sự cố 1: Upload ảnh thành công (mã 200) nhưng ảnh không hiển thị (lỗi 404)
* **Triệu chứng**:
  * API `/api/upload/multiple` trả về HTTP 200 kèm đường dẫn `/uploads/uuid.png`.
  * Trên giao diện hiển thị icon ảnh bị vỡ. Request tải ảnh trả về lỗi HTTP 404.
* **Nguyên nhân**: 
  * Backend Spring Boot chạy trong Docker với `working_dir: /app` nên mặc định ghi ảnh vào thư mục `/app/uploads`.
  * Tuy nhiên, cấu hình volume mount của container backend bị lệch: `- ./uploads:/var/www/forum/uploads` khiến ảnh lưu trong container không được ghi ra thư mục `./uploads` trên host VPS để Nginx truy xuất.
* **Giải pháp**:
  * Sửa lại cấu hình volumes của service `backend` trong `docker-compose.yml` thành:
    ```yaml
    volumes:
      - ./uploads:/app/uploads
    ```
  * Chạy lệnh tái tạo container trên VPS để áp dụng cấu hình mới:
    ```bash
    docker compose up -d backend
    ```

### Sự cố 2: Upload nhiều ảnh hoặc ảnh dung lượng lớn bị lỗi 413 (hoặc báo lỗi CORS giả)
* **Triệu chứng**:
  * Khi upload 1 ảnh dung lượng nhỏ thì thành công.
  * Khi upload nhiều ảnh cùng lúc hoặc ảnh lớn, API trả về lỗi **HTTP 413 Request Entity Too Large**. Trong một số trường hợp, console trình duyệt lại báo lỗi **CORS** do Nginx chặn request từ trước khi chạm tới Spring Boot và trả về trang lỗi 413 mặc định không có header CORS.
* **Nguyên nhân**:
  * Máy chủ Nginx trên VPS có giới hạn dung lượng request gửi lên mặc định là **1MB** (`client_max_body_size 1M`).
* **Giải pháp**:
  * Cấu hình tăng giới hạn `client_max_body_size` của Nginx lên **100M** (để khớp với giới hạn tối đa `100MB` cấu hình trong file `application-prod.properties` của Spring Boot).
  * Chỉnh sửa cấu hình Nginx `/etc/nginx/sites-available/forum` trên VPS, thêm vào trong block `server`:
    ```nginx
    server {
        ...
        client_max_body_size 100M;
        ...
    }
    ```
  * Reload lại Nginx để áp dụng cấu hình mới:
    ```bash
    sudo nginx -t && sudo systemctl reload nginx
    ```

