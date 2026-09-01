#!/bin/bash
# =============================================================================
# bootstrap.sh - Script khởi tạo môi trường VPS mới cho Forum HTXSL
# Chạy bởi GitHub Actions workflow vps-bootstrap.yml
# Biến môi trường cần có:
#   VPS_ENV_FILE   - Nội dung file .env (base64 encoded)
#   CERTBOT_EMAIL  - Email đăng ký SSL Let's Encrypt
#   DOMAIN_NAME    - Tên miền API (mặc định: api.htxslvn.com)
# =============================================================================

set -e  # Dừng ngay nếu có lệnh nào thất bại

DOMAIN=${DOMAIN_NAME:-"api.htxslvn.com"}
FORUM_DIR="/var/www/forum"

echo "======================================================"
echo " HTXSL Forum - VPS Bootstrap Script"
echo " Domain: $DOMAIN"
echo " Thời gian: $(date)"
echo "======================================================"

# ------------------------------------------------------------------------------
# BƯỚC 1: Cập nhật hệ điều hành
# ------------------------------------------------------------------------------
echo ""
echo "[1/9] Cập nhật hệ điều hành..."
apt-get update -y
apt-get upgrade -y
echo "OK: Hệ điều hành đã được cập nhật."

# ------------------------------------------------------------------------------
# BƯỚC 2: Cài đặt các phần mềm cần thiết
# ------------------------------------------------------------------------------
echo ""
echo "[2/9] Cài đặt Docker, Nginx, Certbot, Rclone..."
apt-get install -y \
    docker.io \
    docker-compose-v2 \
    nginx \
    certbot \
    python3-certbot-nginx \
    curl \
    wget

systemctl enable --now docker

# Cài rclone bằng script chính thức (tránh lỗi 404 từ Ubuntu mirror)
echo "Cài đặt rclone từ rclone.org..."
curl https://rclone.org/install.sh | bash
echo "OK: Tất cả phần mềm đã được cài đặt."

# ------------------------------------------------------------------------------
# BƯỚC 3: Cấu hình tham số hệ thống cho OpenSearch (bắt buộc)
# ------------------------------------------------------------------------------
echo ""
echo "[3/9] Cấu hình vm.max_map_count cho OpenSearch..."
sysctl -w vm.max_map_count=262144
# Lưu vĩnh viễn vào /etc/sysctl.conf (tránh mất khi reboot)
if ! grep -q "vm.max_map_count" /etc/sysctl.conf; then
    echo "vm.max_map_count=262144" | tee -a /etc/sysctl.conf
fi
echo "OK: vm.max_map_count=262144 đã được áp dụng."

# ------------------------------------------------------------------------------
# BƯỚC 4: Tạo cấu trúc thư mục dự án
# ------------------------------------------------------------------------------
echo ""
echo "[4/9] Tạo thư mục dự án..."
mkdir -p "$FORUM_DIR/uploads"
mkdir -p "$FORUM_DIR/backups"
echo "OK: Thư mục $FORUM_DIR đã được tạo."

# ------------------------------------------------------------------------------
# BƯỚC 5: Tạo file .env từ GitHub Secret
# ------------------------------------------------------------------------------
echo ""
echo "[5/9] Tạo file .env từ secret..."
if [ -z "$VPS_ENV_FILE" ]; then
    echo "LỖI: Biến môi trường VPS_ENV_FILE chưa được cung cấp!"
    echo "Hướng dẫn: Thêm GitHub Secret 'VPS_ENV_FILE' chứa nội dung file .env đã encode base64."
    exit 1
fi
echo "$VPS_ENV_FILE" | base64 -d > "$FORUM_DIR/.env"
echo "OK: File .env đã được tạo tại $FORUM_DIR/.env"

# Kiểm tra file .env có nội dung không
if [ ! -s "$FORUM_DIR/.env" ]; then
    echo "LỖI: File .env rỗng! Kiểm tra lại giá trị của GitHub Secret 'VPS_ENV_FILE'."
    exit 1
fi

# ------------------------------------------------------------------------------
# BƯỚC 6: Copy docker-compose.yml và khởi động MySQL + OpenSearch
# ------------------------------------------------------------------------------
echo ""
echo "[6/9] Copy docker-compose.yml và khởi động MySQL + OpenSearch..."
# File docker-compose.yml được GitHub Actions copy vào trước khi chạy script này
if [ ! -f "$FORUM_DIR/docker-compose.yml" ]; then
    echo "LỖI: Không tìm thấy $FORUM_DIR/docker-compose.yml!"
    echo "Hướng dẫn: Đảm bảo workflow đã copy file này trước khi chạy bootstrap."
    exit 1
fi

cd "$FORUM_DIR"
docker compose up -d mysql opensearch
echo "OK: MySQL và OpenSearch đã được khởi động."

# Chờ MySQL sẵn sàng (tối đa 60 giây)
echo "Đang chờ MySQL khởi động..."
for i in $(seq 1 12); do
    if docker exec forum-mysql mysqladmin ping -u root -proot_password --silent 2>/dev/null; then
        echo "OK: MySQL đã sẵn sàng."
        break
    fi
    echo "  Attempt $i/12 - MySQL chưa sẵn sàng, chờ 5s..."
    sleep 5
    if [ "$i" -eq 12 ]; then
        echo "LỖI: MySQL không khởi động được sau 60 giây!"
        docker logs forum-mysql --tail 50
        exit 1
    fi
done

# ------------------------------------------------------------------------------
# BƯỚC 7: Cấu hình Nginx làm Reverse Proxy
# ------------------------------------------------------------------------------
echo ""
echo "[7/9] Cấu hình Nginx..."
cat > /etc/nginx/sites-available/forum << EOF
server {
    listen 80;
    server_name $DOMAIN;
    client_max_body_size 100M;

    location /uploads/ {
        alias $FORUM_DIR/uploads/;
        expires 30d;
        add_header Cache-Control "public, no-transform";
    }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        # Hỗ trợ WebSocket
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
EOF

# Kích hoạt cấu hình và tắt default
ln -sf /etc/nginx/sites-available/forum /etc/nginx/sites-enabled/forum
rm -f /etc/nginx/sites-enabled/default

# Kiểm tra cấu hình Nginx trước khi reload
nginx -t
systemctl restart nginx
echo "OK: Nginx đã được cấu hình và khởi động."

# ------------------------------------------------------------------------------
# BƯỚC 8: Cấp SSL bằng Certbot
# ------------------------------------------------------------------------------
echo ""
echo "[8/9] Cấp chứng chỉ SSL cho $DOMAIN..."
if [ -z "$CERTBOT_EMAIL" ]; then
    echo "CẢNH BÁO: Biến môi trường CERTBOT_EMAIL chưa được cung cấp!"
    echo "Bỏ qua bước cấp SSL. Con cần chạy thủ công: certbot --nginx -d $DOMAIN"
else
    # Thử cấp SSL, nếu Cloudflare proxy đang bật có thể cần chờ DNS propagate
    certbot --nginx \
        -d "$DOMAIN" \
        --non-interactive \
        --agree-tos \
        -m "$CERTBOT_EMAIL" \
        --redirect \
        || echo "CẢNH BÁO: Certbot chưa thành công. Có thể DNS chưa propagate. Thử lại sau vài phút."
fi

# ------------------------------------------------------------------------------
# BƯỚC 9: Tổng kết
# ------------------------------------------------------------------------------
echo ""
echo "======================================================"
echo " Bootstrap hoàn tất!"
echo " Trạng thái các container:"
docker compose ps
echo ""
echo " Bước tiếp theo:"
echo " 1. Kiểm tra workflow restore-data để khôi phục dữ liệu"
echo " 2. Sau khi có dữ liệu, trigger deploy-vps.yml để đẩy app.jar"
echo "======================================================"
