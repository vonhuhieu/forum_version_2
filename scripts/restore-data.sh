#!/bin/bash
# =============================================================================
# restore-data.sh - Script khôi phục dữ liệu từ Google Drive lên VPS mới
# Chạy bởi GitHub Actions workflow vps-bootstrap.yml (job restore-data)
#
# Biến môi trường cần có:
#   RCLONE_CONF  - Nội dung file rclone.conf (base64 encoded)
#                  Nếu không có → script sẽ hướng dẫn setup thủ công
#
# Lưu ý: Script tự động phát hiện tên remote rclone bằng 'rclone listremotes'
# =============================================================================

set -e

FORUM_DIR="/var/www/forum"
GDRIVE_FOLDER=${GDRIVE_BACKUP_FOLDER:-"forum_rclone_backups_update_01_09_2026"}
BACKUP_DIR="$FORUM_DIR/backups"

echo "======================================================"
echo " HTXSL Forum - Restore Data Script"
echo " Thời gian: $(date)"
echo "======================================================"

# ------------------------------------------------------------------------------
# BƯỚC 1: Kiểm tra / Setup Rclone config
# ------------------------------------------------------------------------------
echo ""
echo "[1/6] Kiểm tra cấu hình Rclone..."

mkdir -p "$HOME/.config/rclone"

if [ -n "$RCLONE_CONF" ]; then
    echo "Tìm thấy GitHub Secret RCLONE_CONF, đang giải mã..."
    echo "$RCLONE_CONF" | base64 -d > "$HOME/.config/rclone/rclone.conf"
    echo "OK: rclone.conf đã được tạo."
else
    echo "======================================================"
    echo " CẢNH BÁO: Chưa có GitHub Secret 'RCLONE_CONF'!"
    echo ""
    echo " Đây là lần đầu tiên setup Rclone trên VPS mới."
    echo " Con cần thực hiện thủ công CÁC BƯỚC SAU trên VPS:"
    echo ""
    echo "   1. SSH vào VPS mới: ssh root@<IP_VPS_MỚI>"
    echo "   2. Chạy: rclone config"
    echo "   3. Làm theo hướng dẫn kết nối Google Drive"
    echo "   4. Sau khi xong, lấy nội dung file config:"
    echo "      cat ~/.config/rclone/rclone.conf | base64 -w 0"
    echo "   5. Copy toàn bộ output, vào GitHub → Settings → Secrets"
    echo "      Tạo Secret tên: RCLONE_CONF, dán nội dung vào"
    echo "   6. Trigger lại workflow vps-bootstrap.yml"
    echo "======================================================"
    exit 1
fi

# ------------------------------------------------------------------------------
# BƯỚC 2: Tự động phát hiện tên remote rclone
# ------------------------------------------------------------------------------
echo ""
echo "[2/6] Phát hiện tên remote rclone..."

REMOTE_NAME=$(rclone listremotes | head -1 | sed 's/://')

if [ -z "$REMOTE_NAME" ]; then
    echo "LỖI: Không tìm thấy remote nào trong rclone.conf!"
    echo "Kiểm tra lại nội dung GitHub Secret 'RCLONE_CONF'."
    exit 1
fi

echo "OK: Tìm thấy remote: '$REMOTE_NAME'"
GDRIVE_PATH="$REMOTE_NAME:$GDRIVE_FOLDER"
echo "Sẽ tải backup từ: $GDRIVE_PATH"

# ------------------------------------------------------------------------------
# BƯỚC 3: Tìm file backup mới nhất trên Google Drive
# ------------------------------------------------------------------------------
echo ""
echo "[3/6] Tìm file backup mới nhất trên Google Drive..."

# Liệt kê các file .tar.gz, sắp xếp theo thời gian mới nhất (mtime), lấy file đầu tiên
LATEST_BACKUP=$(rclone lsf "$GDRIVE_PATH/" --format="p" | grep "\.tar\.gz" | sort -r | head -1 | tr -d '\r\n')

if [ -z "$LATEST_BACKUP" ]; then
    echo "LỖI: Không tìm thấy file backup nào trong $GDRIVE_PATH/"
    echo "Kiểm tra:"
    echo "  - Tên folder trên Google Drive có đúng là '$GDRIVE_FOLDER' không?"
    echo "  - Folder có chứa file .tar.gz không?"
    echo ""
    echo "Danh sách folder hiện có trên Google Drive:"
    rclone lsd "$REMOTE_NAME:" || true
    exit 1
fi

echo "OK: File backup mới nhất: $LATEST_BACKUP"

# ------------------------------------------------------------------------------
# BƯỚC 4: Tải backup về VPS
# ------------------------------------------------------------------------------
echo ""
echo "[4/6] Tải file backup về VPS..."
mkdir -p "$BACKUP_DIR"

rclone copy "$GDRIVE_PATH/$LATEST_BACKUP" "$BACKUP_DIR/" --progress

if [ ! -f "$BACKUP_DIR/$LATEST_BACKUP" ]; then
    echo "LỖI: Tải file thất bại!"
    exit 1
fi

echo "OK: Đã tải $LATEST_BACKUP về $BACKUP_DIR/"

# ------------------------------------------------------------------------------
# BƯỚC 5: Giải nén backup
# ------------------------------------------------------------------------------
echo ""
echo "[5/6] Giải nén backup..."
cd "$BACKUP_DIR"
tar -xzf "$LATEST_BACKUP"

# Kiểm tra file db.sql có tồn tại sau khi giải nén không
if [ ! -f "$BACKUP_DIR/db.sql" ]; then
    echo "LỖI: Không tìm thấy db.sql sau khi giải nén!"
    echo "Nội dung thư mục backups:"
    ls -la "$BACKUP_DIR/"
    exit 1
fi
echo "OK: Giải nén thành công."

# ------------------------------------------------------------------------------
# BƯỚC 6: Restore dữ liệu vào MySQL và uploads/
# ------------------------------------------------------------------------------
echo ""
echo "[6/6] Restore dữ liệu..."

# 6a. Chờ MySQL sẵn sàng (tối đa 2 phút)
echo "Đang chờ MySQL sẵn sàng..."
for i in $(seq 1 24); do
    if docker exec forum-mysql mysqladmin ping -u root -proot_password --silent 2>/dev/null; then
        echo "OK: MySQL đã sẵn sàng."
        break
    fi
    echo "  Attempt $i/24 - chờ 5s..."
    sleep 5
    if [ "$i" -eq 24 ]; then
        echo "LỖI: MySQL không phản hồi sau 2 phút!"
        docker logs forum-mysql --tail 50
        exit 1
    fi
done

# 6b. Restore database
echo "Đang restore database forum_db..."
docker exec -i forum-mysql mysql -u root -proot_password forum_db < "$BACKUP_DIR/db.sql"
echo "OK: Database đã được restore thành công."

# 6c. Restore thư mục uploads (nếu có trong backup)
if [ -d "$BACKUP_DIR/uploads" ]; then
    echo "Đang restore thư mục uploads..."
    cp -r "$BACKUP_DIR/uploads/." "$FORUM_DIR/uploads/"
    echo "OK: Uploads đã được restore thành công."
else
    echo "THÔNG BÁO: Không tìm thấy thư mục uploads/ trong backup (bỏ qua)."
fi

# 6d. Dọn dẹp tệp tin tạm
echo "Dọn dẹp file tạm..."
rm -f "$BACKUP_DIR/$LATEST_BACKUP"
rm -f "$BACKUP_DIR/db.sql"
[ -d "$BACKUP_DIR/uploads" ] && rm -rf "$BACKUP_DIR/uploads"
echo "OK: Dọn dẹp hoàn tất."

# ------------------------------------------------------------------------------
# Tổng kết
# ------------------------------------------------------------------------------
echo ""
echo "======================================================"
echo " Restore hoàn tất!"
echo " - Database: forum_db đã được restore"
echo " - Uploads: đã được restore (nếu có trong backup)"
echo ""
echo " Bước tiếp theo:"
echo " Trigger deploy-vps.yml để đẩy app.jar và reindex OpenSearch"
echo "======================================================"
