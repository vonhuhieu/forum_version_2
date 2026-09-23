/**
 * Mixin quản lý trạng thái mở và xem ảnh phóng to toàn màn hình qua ImageLightboxModal
 * Dùng chung cho toàn bộ các màn hình hiển thị bài viết, bình luận, lưu bút, đối thoại...
 */
export default {
  data() {
    return {
      showLightbox: false,
      lightboxImageUrl: ''
    }
  },
  methods: {
    openLightbox(url) {
      if (!url) return
      this.lightboxImageUrl = url
      this.showLightbox = true
    },
    closeLightbox() {
      this.showLightbox = false
      this.lightboxImageUrl = ''
    },
    /**
     * Bắt sự kiện click vào bất kỳ ảnh nào trong nội dung bài viết / bình luận / đính kèm
     * @param {Event} e Sự kiện click DOM
     */
    handleContentImageClick(e) {
      if (!e || !e.target) return

      // Bỏ qua nếu click vào avatar hoặc icon reaction nhỏ
      if (
        e.target.closest('.user-avatar') ||
        e.target.closest('.item-avatar') ||
        e.target.closest('.reaction-icon') ||
        e.target.classList.contains('avatar-img')
      ) {
        return
      }

      if (e.target.tagName === 'IMG') {
        const src = e.target.currentSrc || e.target.src || e.target.getAttribute('src')
        if (src) {
          e.preventDefault()
          e.stopPropagation()
          this.openLightbox(src)
        }
      }
    }
  }
}
