<template>
  <div 
    class="post-share-wrapper" 
    ref="wrapperRef"
    @mouseenter="handleMouseEnter" 
    @mouseleave="handleMouseLeave"
  >
    <!-- Share Button Trigger -->
    <button 
      type="button"
      class="post-action-btn share-trigger-btn" 
      :class="{ 'active': isOpen }"
      @click.stop="handleButtonClick"
      title="Chia sẻ"
      aria-label="Chia sẻ bài viết"
    >
      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="18" cy="5" r="3"></circle>
        <circle cx="6" cy="12" r="3"></circle>
        <circle cx="18" cy="19" r="3"></circle>
        <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line>
        <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line>
      </svg>
    </button>

    <!-- Share Popover Box (Được bọc hoàn toàn trong post-share-wrapper) -->
    <transition name="share-fade">
      <div 
        v-if="isOpen" 
        class="share-popover" 
        @click.stop
      >
        <div class="share-popover-inner">
          <!-- Top Arrow pointer pointing to the share button -->
          <div class="popover-arrow"></div>

          <!-- Title Header -->
          <div class="share-popover-title">Share this post</div>

          <!-- Social Icons Row (XenForo Style: Icon Facebook nguyên bản) -->
          <div class="share-social-list">
            <!-- Facebook Icon Nguyên Bản -->
            <button 
              type="button" 
              class="social-btn facebook" 
              title="Chia sẻ lên Facebook" 
              @click.stop="shareToFacebook"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
              </svg>
            </button>
          </div>

          <!-- Copy URL Input Box -->
          <div class="share-copy-box" @click="copyLink">
            <button 
              type="button" 
              class="share-copy-btn" 
              title="Sao chép liên kết"
              @click.stop="copyLink"
            >
              <!-- Copy Icon (XenForo Style: overlapping sheets) -->
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="copy-icon-svg">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
              </svg>
            </button>
            <input 
              type="text" 
              ref="inputRef" 
              class="share-url-input" 
              :value="shareUrl" 
              readonly 
              @click.stop="copyLink"
              title="Nhấn để sao chép liên kết"
            />
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { toastSuccess } from '@/shared/utils/swal'

export default {
  name: 'PostSharePopup',
  props: {
    threadId: {
      type: [Number, String],
      required: true
    },
    postId: {
      type: [Number, String],
      required: true
    },
    isMain: {
      type: Boolean,
      default: false
    },
    postNumber: {
      type: [Number, String],
      default: 1
    }
  },
  data() {
    return {
      isOpen: false,
      isPinned: false, // Được ghim mở khi click trực tiếp
      closeTimer: null
    }
  },
  computed: {
    shareUrl() {
      // Loại bỏ tiền tố www. để URL luôn chuẩn mực và chuyên nghiệp
      const origin = window.location.origin.replace(/^(https?:\/\/)www\./i, '$1')
      if (this.isMain || String(this.postId) === 'main_thread_entry') {
        return `${origin}/thread/${this.threadId}#post-main_thread_entry`
      }
      return `${origin}/thread/${this.threadId}?postId=${this.postId}#post-${this.postId}`
    }
  },
  mounted() {
    document.addEventListener('click', this.handleOutsideClick)
    window.addEventListener('close-all-share-popups', this.handleCloseOtherPopups)
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleOutsideClick)
    window.removeEventListener('close-all-share-popups', this.handleCloseOtherPopups)
    this.clearCloseTimer()
  },
  methods: {
    clearCloseTimer() {
      if (this.closeTimer) {
        clearTimeout(this.closeTimer)
        this.closeTimer = null
      }
    },
    // Chuột đi vào vùng wrapper (nút hoặc popup)
    handleMouseEnter() {
      this.clearCloseTimer()
      this.openPopup()
    },
    // Chuột rời khỏi hoàn toàn cả nút và popup
    handleMouseLeave() {
      if (this.isPinned) return
      this.clearCloseTimer()
      this.closeTimer = setTimeout(() => {
        this.closePopup()
      }, 300)
    },
    // Click vào nút trigger
    handleButtonClick() {
      this.clearCloseTimer()
      if (this.isOpen && this.isPinned) {
        // Đã mở do click trước đó -> click lại để đóng
        this.closePopup()
      } else {
        // Chưa mở hoặc đang mở do hover -> click để mở và ghim cố định
        this.openPopup()
        this.isPinned = true
      }
    },
    openPopup() {
      this.clearCloseTimer()
      // Thông báo đóng các popup share khác trên trang
      window.dispatchEvent(new CustomEvent('close-all-share-popups', {
        detail: { activePostId: String(this.postId) }
      }))
      this.isOpen = true
    },
    closePopup() {
      this.clearCloseTimer()
      this.isOpen = false
      this.isPinned = false
    },
    handleCloseOtherPopups(event) {
      if (String(event?.detail?.activePostId) !== String(this.postId)) {
        this.closePopup()
      }
    },
    handleOutsideClick(event) {
      if (this.$refs.wrapperRef && !this.$refs.wrapperRef.contains(event.target)) {
        this.closePopup()
      }
    },
    async copyLink() {
      const url = this.shareUrl
      try {
        if (navigator.clipboard && window.isSecureContext) {
          await navigator.clipboard.writeText(url)
        } else {
          const tempInput = document.createElement('textarea')
          tempInput.value = url
          tempInput.style.position = 'fixed'
          tempInput.style.left = '-999999px'
          tempInput.style.top = '-999999px'
          document.body.appendChild(tempInput)
          tempInput.focus()
          tempInput.select()
          document.execCommand('copy')
          document.body.removeChild(tempInput)
        }
        toastSuccess('Đã sao chép liên kết vào bộ nhớ tạm!')
        if (this.$refs.inputRef) {
          this.$refs.inputRef.select()
        }
      } catch (err) {
        console.error('Không thể sao chép liên kết:', err)
        if (this.$refs.inputRef) {
          this.$refs.inputRef.select()
        }
      }
    },
    shareToFacebook() {
      const url = encodeURIComponent(this.shareUrl)
      const shareTarget = `https://www.facebook.com/sharer/sharer.php?u=${url}`
      const width = 640
      const height = 580
      const left = Math.max(0, Math.floor((window.innerWidth - width) / 2 + (window.screenX || 0)))
      const top = Math.max(0, Math.floor((window.innerHeight - height) / 2 + (window.screenY || 0)))

      window.open(
        shareTarget,
        'facebook-share-dialog',
        `width=${width},height=${height},top=${top},left=${left},toolbar=0,menubar=0,location=0,status=0,scrollbars=1,resizable=1`
      )
    }
  }
}
</script>

<style scoped>
.post-share-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
}

/* Nút trigger Share icon: hoàn toàn không background, chỉ đổi màu icon */
.share-trigger-btn {
  background: transparent;
  border: none;
  padding: 0;
  color: #718096;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s ease;
  line-height: 1;
}

.share-trigger-btn:hover,
.share-trigger-btn.active {
  color: #1a507a;
  background: transparent;
}

/* Khối Popover bao ngoài: tiếp giáp ngay tại top: 100% của button, padding-top: 8px tạo khoảng cách thẩm mỹ mà chuột không bao giờ bị rớt ra ngoài wrapper */
.share-popover {
  position: absolute;
  top: 100%;
  right: -8px;
  padding-top: 8px;
  width: 290px;
  z-index: 1050;
  text-align: left;
}

/* Khối nội dung trắng bên trong popover */
.share-popover-inner {
  position: relative;
  background-color: #ffffff;
  border: 1px solid #d2d6dc;
  border-radius: 6px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  padding: 14px 16px 16px;
}

/* Mũi tên chỉ lên nút share */
.popover-arrow {
  position: absolute;
  top: -6px;
  right: 14px;
  width: 10px;
  height: 10px;
  background-color: #ffffff;
  border-left: 1px solid #d2d6dc;
  border-top: 1px solid #d2d6dc;
  transform: rotate(45deg);
  z-index: 1051;
}

/* Header Tiêu Đề */
.share-popover-title {
  color: #2b82d9;
  font-size: 1.05rem;
  font-weight: 500;
  margin-bottom: 12px;
  letter-spacing: -0.01em;
}

/* Hàng Icon Mạng Xã Hội */
.share-social-list {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  padding: 0 2px;
}

/* Icon mạng xã hội nguyên bản chuẩn XenForo */
.social-btn {
  background: transparent;
  border: none;
  padding: 4px;
  color: #616161;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: transform 0.15s ease, color 0.15s ease;
}

.social-btn:hover {
  transform: translateY(-2px);
}

.social-btn.facebook:hover {
  color: #1877f2;
}

/* Hộp Sao Chép Liên Kết */
.share-copy-box {
  display: flex;
  align-items: center;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  background-color: #ffffff;
  overflow: hidden;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  cursor: pointer;
}

.share-copy-box:hover,
.share-copy-box:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.share-copy-btn {
  background: #f8fafc;
  border: none;
  border-right: 1px solid #cbd5e1;
  padding: 6px 10px;
  color: #1d70b8;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.share-copy-btn:hover {
  background-color: #e2e8f0;
  color: #155b96;
}

.copy-icon-svg {
  display: block;
}

.share-url-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 6px 10px;
  font-size: 0.85rem;
  color: #334155;
  background: transparent;
  width: 100%;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Hiệu ứng Fade & Slide cho Popover */
.share-fade-enter-active,
.share-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.share-fade-leave-active {
  pointer-events: none;
}

.share-fade-enter-from,
.share-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
