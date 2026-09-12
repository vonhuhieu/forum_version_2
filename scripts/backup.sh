#!/bin/bash
# =============================================================================
# backup.sh - Script sao lưu tự động Database + Uploads lên Google Drive
# Chạy hàng ngày lúc 02:00 sáng qua Cron Job
#
# Cài đặt Cron Job:
#   0 2 * * * /bin/bash /var/www/forum/scripts/backup.sh >> /var/www/forum/backup.log 2>&1
#
# Yêu cầu: Rclone đã được cấu hình kết nối Google Drive
# =============================================================================

set -e

# Đảm bảo PATH đầy đủ cho cron job (bao gồm docker, rclone)
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/snap/bin:$PATH

FORUM_DIR="/var/www/forum"
BACKUP_DIR="$FORUM_DIR/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILENAME="forum_backup_$TIMESTAMP.tar.gz"
TMP_DIR="$BACKUP_DIR/tmp_$TIMESTAMP"

# Tự động nạp biến môi trường từ file .env nếu có (quan trọng cho cron job)
if [ -f "$FORUM_DIR/.env" ]; then
    echo "Nạp cấu hình môi trường từ $FORUM_DIR/.env..."
    set -a
    # shellcheck source=/dev/null
    . "$FORUM_DIR/.env"
    set +a
fi

# Cấu hình Google Drive (ưu tiên lấy từ biến GDRIVE_BACKUP_FOLDER, fallback về forum_backups)
GDRIVE_FOLDER=${GDRIVE_BACKUP_FOLDER:-"forum_backups"}
KEEP_DAYS=7  # Số ngày giữ backup cục bộ trên VPS

# Đảm bảo luôn dọn dẹp thư mục tạm khi script thoát (kể cả khi gặp lỗi)
cleanup() {
    local exit_code=$?
    if [ -d "$TMP_DIR" ]; then
        echo "Dọn dẹp thư mục tạm: $TMP_DIR"
        rm -rf "$TMP_DIR"
    fi
    if [ $exit_code -ne 0 ]; then
        echo ""
        echo "======================================================"
        echo "❌ LỖI: Sao lưu thất bại vào lúc $(date) với mã lỗi: $exit_code"
        echo "Vui lòng kiểm tra kỹ các thông báo lỗi phía trên."
        echo "======================================================"
    fi
}
trap cleanup EXIT

echo "======================================================"
echo " HTXSL Forum - Auto Backup Script"
echo " Thời gian bắt đầu: $(date)"
echo " Thư mục Google Drive đích: $GDRIVE_FOLDER"
echo "======================================================"

# ------------------------------------------------------------------------------
# BƯỚC 1: Kiểm tra môi trường & Rclone
# ------------------------------------------------------------------------------
echo ""
echo "[1/5] Kiểm tra môi trường..."
mkdir -p "$BACKUP_DIR"
mkdir -p "$TMP_DIR"

# Kiểm tra rclone đã cài đặt và cấu hình chưa
if ! command -v rclone >/dev/null 2>&1; then
    echo "❌ LỖI: rclone chưa được cài đặt trên hệ thống hoặc không nằm trong PATH!"
    exit 1
fi

REMOTE_NAME=$(rclone listremotes 2>/dev/null | head -1 | sed 's/://')
if [ -z "$REMOTE_NAME" ]; then
    echo "❌ LỖI: Rclone chưa được cấu hình remote nào!"
    echo "Chạy 'rclone config' để cấu hình kết nối Google Drive."
    exit 1
fi
echo "OK: Rclone remote đã phát hiện: '$REMOTE_NAME'"

# Kiểm tra kết nối thực tế tới Google Drive (bắt lỗi token hết hạn sớm trước khi dump DB)
echo "Đang kiểm tra kết nối tới Google Drive ($REMOTE_NAME)..."
if ! rclone lsd "$REMOTE_NAME:" --timeout=15s >/dev/null 2>&1; then
    echo "❌ LỖI: Không thể kết nối tới Google Drive remote '$REMOTE_NAME'!"
    echo "Nguyên nhân khả dĩ nhất:"
    echo "  1. Token Google OAuth đã hết hạn (lưu ý: Google hạn chế Refresh Token 7 ngày nếu app ở trạng thái 'Testing')."
    echo "  2. Mất kết nối Internet hoặc rclone.conf bị hỏng/xóa."
    echo "Để kiểm tra chi tiết, hãy chạy thủ công lệnh trên VPS: rclone lsd $REMOTE_NAME:"
    exit 1
fi
echo "OK: Kết nối Google Drive remote '$REMOTE_NAME' sẵn sàng."

# Tìm kiếm container MySQL linh hoạt (chấp nhận forum-mysql, mysql, hoặc container chứa image mysql)
MYSQL_CONTAINER=""
if docker ps --format '{{.Names}}' | grep -q "^forum-mysql$"; then
    MYSQL_CONTAINER="forum-mysql"
elif docker ps --format '{{.Names}}' | grep -q "mysql"; then
    MYSQL_CONTAINER=$(docker ps --format '{{.Names}}' | grep "mysql" | head -1)
elif docker ps -a --format '{{.Names}}' | grep -q "^forum-mysql$"; then
    echo "⚠️ Phát hiện container forum-mysql đang bị tắt (Exited), đang khởi động lại..."
    docker start forum-mysql || true
    sleep 3
    if docker ps --format '{{.Names}}' | grep -q "^forum-mysql$"; then
        MYSQL_CONTAINER="forum-mysql"
    fi
fi

# Nếu vẫn chưa tìm thấy container MySQL, dùng docker compose để bật lại service mysql
if [ -z "$MYSQL_CONTAINER" ] && [ -f "$FORUM_DIR/docker-compose.yml" ]; then
    echo "Đang khởi động lại MySQL service qua Docker Compose..."
    cd "$FORUM_DIR"
    docker compose up -d mysql || true
    sleep 5
    MYSQL_CONTAINER=$(docker ps --format '{{.Names}}' | grep "mysql" | head -1)
fi

if [ -z "$MYSQL_CONTAINER" ]; then
    echo "❌ LỖI: Không tìm thấy bất kỳ container MySQL nào đang chạy trên hệ thống!"
    echo "Danh sách các container hiện có trên VPS:"
    docker ps -a --format "table {{.ID}}\t{{.Names}}\t{{.Status}}\t{{.Image}}"
    exit 1
fi
echo "OK: Phát hiện MySQL container đang chạy: '$MYSQL_CONTAINER'"

# ------------------------------------------------------------------------------
# BƯỚC 2: Dump database MySQL
# ------------------------------------------------------------------------------
echo ""
echo "[2/5] Dump database forum_db từ container '$MYSQL_CONTAINER'..."
docker exec "$MYSQL_CONTAINER" mysqldump \
    -u root -proot_password \
    --single-transaction \
    --routines \
    --triggers \
    forum_db > "$TMP_DIR/db.sql"

if [ ! -s "$TMP_DIR/db.sql" ]; then
    echo "❌ LỖI: mysqldump thất bại hoặc file rỗng!"
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
# BƯỚC 4: Nén và đồng bộ lên Google Drive
# ------------------------------------------------------------------------------
echo ""
echo "[4/5] Nén và tải lên Google Drive..."
cd "$BACKUP_DIR"
tar -czf "$BACKUP_FILENAME" -C "$TMP_DIR" .

ARCHIVE_SIZE=$(du -sh "$BACKUP_FILENAME" | cut -f1)
echo "OK: Đã tạo file backup $BACKUP_FILENAME (size: $ARCHIVE_SIZE)"

# Upload lên Google Drive
echo "Đang tải $BACKUP_FILENAME lên Google Drive tại $REMOTE_NAME:$GDRIVE_FOLDER/..."
rclone copy "$BACKUP_DIR/$BACKUP_FILENAME" "$REMOTE_NAME:$GDRIVE_FOLDER/" --stats-one-line

# Xác minh file tồn tại trên Google Drive
if rclone ls "$REMOTE_NAME:$GDRIVE_FOLDER/$BACKUP_FILENAME" >/dev/null 2>&1; then
    echo "✅ ĐÃ XÁC MINH: File $BACKUP_FILENAME đã được tải lên Google Drive thành công!"
else
    echo "⚠️ CẢNH BÁO: Không thể xác minh file trên Google Drive. Vui lòng kiểm tra lại."
fi

# ------------------------------------------------------------------------------
# BƯỚC 5: Dọn dẹp backup cục bộ cũ
# ------------------------------------------------------------------------------
echo ""
echo "[5/5] Dọn dẹp backup cũ cục bộ trên VPS..."

# Xóa các file backup cục bộ trên VPS cũ hơn KEEP_DAYS ngày
find "$BACKUP_DIR" -name "forum_backup_*.tar.gz" -mtime +"$KEEP_DAYS" -delete
echo "OK: Đã xóa các backup cũ hơn $KEEP_DAYS ngày trên VPS."

# Thống kê backup hiện có trên VPS
BACKUP_COUNT=$(find "$BACKUP_DIR" -name "forum_backup_*.tar.gz" | wc -l)
BACKUP_TOTAL_SIZE=$(du -sh "$BACKUP_DIR" 2>/dev/null | cut -f1 || echo "0")

echo ""
echo "======================================================"
echo "🎉 Backup hoàn tất thành công!"
echo " File mới: $BACKUP_FILENAME ($ARCHIVE_SIZE)"
echo " Thư mục Google Drive: $REMOTE_NAME:$GDRIVE_FOLDER/"
echo " Số backup trên VPS: $BACKUP_COUNT file ($BACKUP_TOTAL_SIZE)"
echo " Kết thúc: $(date)"
echo "======================================================"
