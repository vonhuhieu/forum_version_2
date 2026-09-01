#!/bin/bash
# =============================================================================
# backup.sh - Script sao lưu tự động Database + Uploads lên Google Drive
# Chạy hàng ngày lúc 02:00 sáng qua Cron Job
#
# Cài đặt Cron Job:
#   crontab -e
#   0 2 * * * /var/www/forum/backup.sh >> /var/www/forum/backup.log 2>&1
#
# Yêu cầu: Rclone đã được cấu hình kết nối Google Drive
# =============================================================================

set -e

FORUM_DIR="/var/www/forum"
BACKUP_DIR="$FORUM_DIR/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILENAME="forum_backup_$TIMESTAMP.tar.gz"
TMP_DIR="$BACKUP_DIR/tmp_$TIMESTAMP"

# Cấu hình Google Drive (tự động phát hiện tên remote)
GDRIVE_FOLDER=${GDRIVE_BACKUP_FOLDER:-"forum_backups"}
KEEP_DAYS=7  # Số ngày giữ backup cục bộ trên VPS

echo "======================================================"
echo " HTXSL Forum - Auto Backup Script"
echo " Thời gian bắt đầu: $(date)"
echo "======================================================"

# ------------------------------------------------------------------------------
# BƯỚC 1: Kiểm tra môi trường
# ------------------------------------------------------------------------------
echo ""
echo "[1/5] Kiểm tra môi trường..."
mkdir -p "$BACKUP_DIR"
mkdir -p "$TMP_DIR"

# Kiểm tra rclone đã cấu hình chưa
REMOTE_NAME=$(rclone listremotes 2>/dev/null | head -1 | sed 's/://')
if [ -z "$REMOTE_NAME" ]; then
    echo "LỖI: Rclone chưa được cấu hình!"
    echo "Chạy 'rclone config' để cấu hình kết nối Google Drive."
    exit 1
fi
echo "OK: Rclone remote đã cấu hình: '$REMOTE_NAME'"

# ------------------------------------------------------------------------------
# BƯỚC 2: Dump database MySQL
# ------------------------------------------------------------------------------
echo ""
echo "[2/5] Dump database forum_db..."
docker exec forum-mysql mysqldump \
    -u root -proot_password \
    --single-transaction \
    --routines \
    --triggers \
    forum_db > "$TMP_DIR/db.sql"

if [ ! -s "$TMP_DIR/db.sql" ]; then
    echo "LỖI: mysqldump thất bại hoặc file rỗng!"
    exit 1
fi

DB_SIZE=$(du -sh "$TMP_DIR/db.sql" | cut -f1)
echo "OK: Database đã dump thành công (size: $DB_SIZE)"

# ------------------------------------------------------------------------------
# BƯỚC 3: Copy thư mục uploads
# ------------------------------------------------------------------------------
echo ""
echo "[3/5] Sao chép thư mục uploads..."
if [ -d "$FORUM_DIR/uploads" ] && [ "$(ls -A "$FORUM_DIR/uploads" 2>/dev/null)" ]; then
    cp -r "$FORUM_DIR/uploads" "$TMP_DIR/uploads"
    UPLOADS_SIZE=$(du -sh "$TMP_DIR/uploads" | cut -f1)
    echo "OK: Uploads đã được sao chép (size: $UPLOADS_SIZE)"
else
    echo "THÔNG BÁO: Thư mục uploads rỗng, bỏ qua."
fi

# ------------------------------------------------------------------------------
# BƯỚC 4: Nén tất cả thành file .tar.gz và đồng bộ lên Google Drive
# ------------------------------------------------------------------------------
echo ""
echo "[4/5] Nén và tải lên Google Drive..."
cd "$BACKUP_DIR"
tar -czf "$BACKUP_FILENAME" -C "$TMP_DIR" .

ARCHIVE_SIZE=$(du -sh "$BACKUP_FILENAME" | cut -f1)
echo "OK: Đã tạo file backup $BACKUP_FILENAME (size: $ARCHIVE_SIZE)"

# Upload lên Google Drive
rclone copy "$BACKUP_DIR/$BACKUP_FILENAME" "$REMOTE_NAME:$GDRIVE_FOLDER/" --progress
echo "OK: Đã upload lên Google Drive tại $REMOTE_NAME:$GDRIVE_FOLDER/"

# ------------------------------------------------------------------------------
# BƯỚC 5: Dọn dẹp
# ------------------------------------------------------------------------------
echo ""
echo "[5/5] Dọn dẹp tệp tin tạm và backup cũ..."

# Xóa thư mục tạm
rm -rf "$TMP_DIR"
echo "OK: Đã xóa thư mục tạm."

# Xóa các file backup cục bộ trên VPS cũ hơn KEEP_DAYS ngày
find "$BACKUP_DIR" -name "forum_backup_*.tar.gz" -mtime +"$KEEP_DAYS" -delete
echo "OK: Đã xóa các backup cũ hơn $KEEP_DAYS ngày trên VPS."

# Thống kê backup hiện có
BACKUP_COUNT=$(find "$BACKUP_DIR" -name "forum_backup_*.tar.gz" | wc -l)
BACKUP_TOTAL_SIZE=$(du -sh "$BACKUP_DIR" 2>/dev/null | cut -f1 || echo "0")

echo ""
echo "======================================================"
echo " Backup hoàn tất!"
echo " File mới: $BACKUP_FILENAME ($ARCHIVE_SIZE)"
echo " Số backup trên VPS: $BACKUP_COUNT file"
echo " Tổng dung lượng backup cục bộ: $BACKUP_TOTAL_SIZE"
echo " Kết thúc: $(date)"
echo "======================================================"
