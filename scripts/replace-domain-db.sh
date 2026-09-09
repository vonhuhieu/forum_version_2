#!/bin/bash
# =============================================================================
# replace-domain-db.sh - Thay thế liên kết tên miền cũ trong Database MySQL
# Chạy bên trong máy chủ VPS (tác động lên container forum-mysql)
# =============================================================================

set -e

OLD_DOMAIN=${1:-"htxslvn.com"}
NEW_DOMAIN=${2}

if [ -z "$NEW_DOMAIN" ]; then
    echo "LỖI: Chưa cung cấp tên miền mới!"
    echo "Sử dụng: $0 <OLD_DOMAIN> <NEW_DOMAIN>"
    echo "Ví dụ:    $0 htxslvn.com hoptacxavuive.com"
    exit 1
fi

FORUM_DIR="/var/www/forum"
ENV_FILE="$FORUM_DIR/.env"

# Lấy mật khẩu root MySQL từ file .env nếu có
MYSQL_PASS="root_password"
if [ -f "$ENV_FILE" ]; then
    ENV_PASS=$(grep -E "^SPRING_DATASOURCE_PASSWORD=" "$ENV_FILE" | cut -d '=' -f2- || true)
    if [ -n "$ENV_PASS" ]; then
        MYSQL_PASS="$ENV_PASS"
    fi
fi

echo "======================================================"
echo " HTXSL Forum - Thay Thế Tên Miền Trong Database"
echo " Domain Cũ: $OLD_DOMAIN"
echo " Domain Mới: $NEW_DOMAIN"
echo " Thời gian:  $(date)"
echo "======================================================"

# Kiểm tra container MySQL có đang chạy không
if ! docker ps --format '{{.Names}}' | grep -q "^forum-mysql$"; then
    echo "LỖI: Container forum-mysql không đang chạy!"
    exit 1
fi

echo "Đang thực thi lệnh thay thế chuỗi trong MySQL..."

docker exec -i forum-mysql mysql -u root -p"$MYSQL_PASS" forum_db << EOF
-- Tắt kiểm tra an toàn tạm thời để update theo pattern
SET SQL_SAFE_UPDATES = 0;

-- 1. Cập nhật nội dung bài viết
UPDATE posts 
SET content = REPLACE(content, '$OLD_DOMAIN', '$NEW_DOMAIN') 
WHERE content LIKE '%$OLD_DOMAIN%';
SELECT ROW_COUNT() AS 'Posts_Updated';

-- 2. Cập nhật thumbnail chủ đề
UPDATE threads 
SET thumbnail = REPLACE(thumbnail, '$OLD_DOMAIN', '$NEW_DOMAIN') 
WHERE thumbnail LIKE '%$OLD_DOMAIN%';
SELECT ROW_COUNT() AS 'Threads_Thumbnail_Updated';

-- 3. Cập nhật cấu hình cài đặt hệ thống
UPDATE settings 
SET setting_value = REPLACE(setting_value, '$OLD_DOMAIN', '$NEW_DOMAIN') 
WHERE setting_value LIKE '%$OLD_DOMAIN%';
SELECT ROW_COUNT() AS 'Settings_Updated';

-- 4. Cập nhật avatar người dùng nếu có link tuyệt đối
UPDATE users 
SET avatar = REPLACE(avatar, '$OLD_DOMAIN', '$NEW_DOMAIN') 
WHERE avatar LIKE '%$OLD_DOMAIN%';
SELECT ROW_COUNT() AS 'Users_Avatar_Updated';

SET SQL_SAFE_UPDATES = 1;
EOF

echo "OK: Hoàn tất cập nhật các liên kết trong database."
