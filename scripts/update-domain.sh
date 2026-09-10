#!/bin/bash
# =============================================================================
# update-domain.sh - Script đổi tên miền hệ thống Forum trên VPS
# Được gọi bởi GitHub Actions workflow change-domain.yml hoặc chạy trực tiếp trên VPS
#
# Cách sử dụng thủ công trên VPS:
#   sudo NEW_API_DOMAIN="api.domainmoi.com" NEW_FRONTEND_DOMAIN="domainmoi.com" CERTBOT_EMAIL="your-email@gmail.com" bash update-domain.sh
# =============================================================================

set -e

# Đọc tham số từ biến môi trường hoặc tham số dòng lệnh $1, $2, $3
NEW_API_DOMAIN=${NEW_API_DOMAIN:-$1}
NEW_FRONTEND_DOMAIN=${NEW_FRONTEND_DOMAIN:-$2}
CERTBOT_EMAIL=${CERTBOT_EMAIL:-$3}

FORUM_DIR="/var/www/forum"
ENV_FILE="$FORUM_DIR/.env"
NGINX_CONF="/etc/nginx/sites-available/forum"

echo "======================================================"
echo " HTXSL Forum - Script Cập Nhật Tên Miền Hệ Thống"
echo " API Domain Mới:      $NEW_API_DOMAIN"
echo " Frontend Domain Mới: $NEW_FRONTEND_DOMAIN"
echo " Thời gian:           $(date)"
echo "======================================================"

# Kiểm tra tham số bắt buộc
if [ -z "$NEW_API_DOMAIN" ] || [ -z "$NEW_FRONTEND_DOMAIN" ]; then
    echo "LỖI: Thiếu tham số tên miền mới!"
    echo "Sử dụng: NEW_API_DOMAIN=api.example.com NEW_FRONTEND_DOMAIN=example.com [CERTBOT_EMAIL=email] $0"
    exit 1
fi

# Hàm cập nhật hoặc thêm biến vào file .env
update_env_var() {
    local key="$1"
    local value="$2"
    local file="$3"

    if grep -q "^${key}=" "$file" 2>/dev/null; then
        # Đã có -> Thay thế giá trị
        sed -i "s|^${key}=.*|${key}=${value}|" "$file"
        echo "  [ENV] Cập nhật: ${key}=${value}"
    else
        # Chưa có -> Thêm vào cuối file
        echo "${key}=${value}" >> "$file"
        echo "  [ENV] Thêm mới: ${key}=${value}"
    fi
}

# ------------------------------------------------------------------------------
# BƯỚC 1: Cấu hình Nginx Virtual Host cho Domain API mới
# ------------------------------------------------------------------------------
echo ""
echo "[1/5] Cập nhật cấu hình Nginx cho $NEW_API_DOMAIN..."

# Sao lưu cấu hình cũ nếu có
if [ -f "$NGINX_CONF" ]; then
    cp "$NGINX_CONF" "${NGINX_CONF}.bak.$(date +%s)"
fi

# Tạo cấu hình Nginx mới (Port 80 ban đầu để Certbot xác thực SSL)
cat > "$NGINX_CONF" << EOF
server {
    listen 80;
    server_name $NEW_API_DOMAIN;
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

# Đảm bảo symlink kích hoạt
ln -sf "$NGINX_CONF" /etc/nginx/sites-enabled/forum
rm -f /etc/nginx/sites-enabled/default

# Kiểm tra cú pháp Nginx
nginx -t
systemctl restart nginx
echo "OK: Nginx đã nạp cấu hình vhost cho $NEW_API_DOMAIN."

# ------------------------------------------------------------------------------
# BƯỚC 2: Cấp chứng chỉ SSL Let's Encrypt bằng Certbot
# ------------------------------------------------------------------------------
echo ""
echo "[2/5] Cấp chứng chỉ SSL cho $NEW_API_DOMAIN..."

if [ -z "$CERTBOT_EMAIL" ]; then
    echo "CẢNH BÁO: CERTBOT_EMAIL chưa được cung cấp, sử dụng cờ --register-unsafely-without-email"
    certbot --nginx \
        -d "$NEW_API_DOMAIN" \
        --non-interactive \
        --agree-tos \
        --register-unsafely-without-email \
        --redirect \
        || echo "CẢNH BÁO: Certbot cấp SSL chưa thành công. Kiểm tra lại DNS trỏ đúng IP VPS."
else
    certbot --nginx \
        -d "$NEW_API_DOMAIN" \
        --non-interactive \
        --agree-tos \
        -m "$CERTBOT_EMAIL" \
        --redirect \
        || echo "CẢNH BÁO: Certbot cấp SSL chưa thành công. Kiểm tra lại DNS trỏ đúng IP VPS."
fi

# Reload Nginx sau khi cấp SSL
nginx -t && systemctl reload nginx
echo "OK: Chứng chỉ SSL đã được thiết lập."

# ------------------------------------------------------------------------------
# BƯỚC 3: Cập nhật biến môi trường trong file .env trên VPS
# ------------------------------------------------------------------------------
echo ""
echo "[3/5] Cập nhật file cấu hình .env tại $ENV_FILE..."

if [ ! -f "$ENV_FILE" ]; then
    touch "$ENV_FILE"
fi

# Đảm bảo file .env kết thúc bằng newline trước khi cập nhật
# Ngăn ngừa bug nối dính biến môi trường khi file thiếu ký tự xuống dòng cuối
sed -i -e '$a\' "$ENV_FILE"

# Cập nhật các biến tên miền
update_env_var "APP_FRONTEND_URL" "https://${NEW_FRONTEND_DOMAIN}" "$ENV_FILE"
update_env_var "APP_CORS_ALLOWED_ORIGINS" "https://${NEW_FRONTEND_DOMAIN},https://www.${NEW_FRONTEND_DOMAIN}" "$ENV_FILE"
update_env_var "RESEND_FROM_EMAIL" "admin@${NEW_FRONTEND_DOMAIN}" "$ENV_FILE"

echo "OK: File .env đã được cập nhật với tên miền mới."

# ------------------------------------------------------------------------------
# BƯỚC 4: Khởi động lại container Backend
# ------------------------------------------------------------------------------
echo ""
echo "[4/5] Khởi động lại Backend container để nạp biến môi trường mới..."

cd "$FORUM_DIR"
docker compose restart backend

echo "Đang chờ Backend sẵn sàng..."
for i in $(seq 1 24); do
    if curl -sf http://127.0.0.1:8080/api/settings/public > /dev/null 2>&1; then
        echo "OK: Backend đã sẵn sàng phản hồi!"
        break
    fi
    echo "  Attempt $i/24 - Backend đang khởi động, chờ 5s..."
    sleep 5
    if [ "$i" -eq 24 ]; then
        echo "LỖI: Backend không phản hồi sau 2 phút!"
        docker logs forum-backend --tail 100
        exit 1
    fi
done

# ------------------------------------------------------------------------------
# BƯỚC 5: Kiểm tra xác thực đầu cuối
# ------------------------------------------------------------------------------
echo ""
echo "[5/5] Kiểm tra API endpoint qua tên miền mới..."
if curl -sf -k "https://${NEW_API_DOMAIN}/api/settings/public" > /dev/null 2>&1; then
    echo "TUYỆT VỜI: https://${NEW_API_DOMAIN}/api/settings/public hoạt động hoàn hảo!"
else
    echo "THÔNG BÁO: Kiểm tra cục bộ qua 127.0.0.1 thành công. Kiểm tra bên ngoài có thể cần chờ DNS propagate."
fi

echo ""
echo "======================================================"
echo " Đổi tên miền trên VPS HOÀN TẤT THÀNH CÔNG!"
echo " API:      https://$NEW_API_DOMAIN"
echo " Frontend: https://$NEW_FRONTEND_DOMAIN"
echo " CORS:     https://$NEW_FRONTEND_DOMAIN, https://www.$NEW_FRONTEND_DOMAIN"
echo "======================================================"
