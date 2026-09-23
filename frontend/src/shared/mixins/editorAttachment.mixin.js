/**
 * Mixin quản lý trạng thái tải tệp (Loading overlay) và định dạng khối đính kèm
 * Dùng chung cho toàn bộ các màn hình có sử dụng trình soạn thảo CustomEditor hoặc ImageUploaderPanel
 */
export default {
  data() {
    return {
      isUploadLoading: false
    }
  },
  methods: {
    handleUploadLoadingStart() {
      this.isUploadLoading = true
    },
    handleUploadLoadingEnd() {
      this.isUploadLoading = false
    },
    /**
     * Tự động ghép nối các hình ảnh đính kèm chưa được chèn inline vào cuối nội dung bài viết
     * @param {Array} attachedImages Danh sách ảnh đính kèm [{ url, name }]
     * @param {String} content Nội dung hiện tại của bài viết
     * @param {Object} options Tùy chỉnh (kích thước ảnh, margin...)
     * @returns {String} Nội dung đã ghép khối đính kèm
     */
    buildAttachmentHtml(attachedImages, content = '', options = {}) {
      let finalContent = (content || '').trim()
      if (!attachedImages || attachedImages.length === 0) {
        return finalContent
      }

      // Lọc ra các ảnh chưa từng xuất hiện trong thẻ <img> của content
      const uninsertedImages = attachedImages.filter(img => img && img.url && !finalContent.includes(img.url))
      if (uninsertedImages.length === 0) {
        return finalContent
      }

      const imgWidth = options.imageWidth || '160px'
      const imgHeight = options.imageHeight || '160px'
      const marginTop = options.marginTop || '1.5rem'

      let attachedHtml = `<div id="attachment-section" class="attachment-block" style="margin-top: ${marginTop}; border-top: 1px dashed #ddd; padding-top: 1.2rem;">`
      attachedHtml += `<div class="attachment-label" style="font-weight: bold; color: #1a507a; margin-bottom: 0.8rem; font-size: 0.95rem;">Đính kèm</div>`
      attachedHtml += `<div class="attachment-list" style="display: flex; flex-wrap: wrap; gap: 12px;">`

      uninsertedImages.forEach(img => {
        attachedHtml += `<img src="${img.url}" alt="${img.name || 'image'}" style="width: ${imgWidth}; height: ${imgHeight}; object-fit: cover; border: 1px solid #ddd; border-radius: 4px; cursor: pointer; display: inline-block; margin: 4px;" />`
      })

      attachedHtml += `</div></div>`
      return finalContent + '\n' + attachedHtml
    }
  }
}
