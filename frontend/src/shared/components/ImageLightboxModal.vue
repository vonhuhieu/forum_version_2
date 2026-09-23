<template>
  <Teleport to="body">
    <div 
      v-if="visible" 
      class="lightbox-overlay" 
      @click="handleOverlayClick"
      @keydown.esc="close"
      tabindex="-1"
      ref="overlayRef"
    >
      <!-- Nút đóng góc trên phải -->
      <button class="lightbox-btn-close" @click="close" title="Đóng (Esc)">
        <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>

      <!-- Khung hiển thị ảnh và xử lý cử chỉ -->
      <div 
        class="lightbox-viewport" 
        ref="viewportRef"
        @mousedown="startDrag"
        @mousemove="onDrag"
        @mouseup="endDrag"
        @mouseleave="endDrag"
        @touchstart="onTouchStart"
        @touchmove="onTouchMove"
        @touchend="onTouchEnd"
      >
        <img 
          :src="src" 
          alt="Lightbox preview"
          class="lightbox-image"
          :class="{ 'is-dragging': isDragging, 'is-zoomed': scale > 1 }"
          :style="imageTransformStyle"
          @click.stop="handleImageClick"
          @dblclick.stop="handleDoubleClick"
          draggable="false"
        />
      </div>

      <!-- Thanh công cụ điều khiển Zoom nổi phía dưới -->
      <div class="lightbox-toolbar" @click.stop>
        <button class="toolbar-btn" @click="zoomOut" :disabled="scale <= 1" title="Thu nhỏ">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            <line x1="8" y1="11" x2="14" y2="11"></line>
          </svg>
        </button>

        <span class="zoom-level-text" @click="resetZoom" title="Bấm để đưa về 100%">
          {{ Math.round(scale * 100) }}%
        </span>

        <button class="toolbar-btn" @click="zoomIn" :disabled="scale >= 4" title="Phóng to">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            <line x1="11" y1="8" x2="11" y2="14"></line>
            <line x1="8" y1="11" x2="14" y2="11"></line>
          </svg>
        </button>

        <button class="toolbar-btn" @click="resetZoom" :disabled="scale === 1 && translateX === 0 && translateY === 0" title="Đặt lại kích thước chuẩn">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path>
            <path d="M3 3v5h5"></path>
          </svg>
        </button>
      </div>

      <!-- Hướng dẫn thao tác nhanh cho người dùng trên mobile -->
      <div class="lightbox-hint">
        {{ scale > 1 ? 'Chạm đúp để thu nhỏ · Kéo để di chuyển' : 'Chạm đúp vào ảnh để phóng to toàn màn hình' }}
      </div>
    </div>
  </Teleport>
</template>

<script>
export default {
  name: 'ImageLightboxModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    src: {
      type: String,
      default: ''
    }
  },
  emits: ['close', 'update:visible'],
  data() {
    return {
      scale: 1,
      translateX: 0,
      translateY: 0,
      isDragging: false,
      startX: 0,
      startY: 0,
      initialTranslateX: 0,
      initialTranslateY: 0,
      lastTapTime: 0
    }
  },
  computed: {
    imageTransformStyle() {
      return {
        transform: `translate3d(${this.translateX}px, ${this.translateY}px, 0) scale(${this.scale})`,
        transition: this.isDragging ? 'none' : 'transform 0.25s cubic-bezier(0.2, 0, 0.2, 1)'
      }
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.resetZoom()
        document.body.style.overflow = 'hidden'
        this.$nextTick(() => {
          this.$refs.overlayRef?.focus()
        })
      } else {
        document.body.style.overflow = ''
      }
    }
  },
  beforeUnmount() {
    document.body.style.overflow = ''
  },
  methods: {
    close() {
      this.$emit('close')
      this.$emit('update:visible', false)
    },
    handleOverlayClick(e) {
      if (e.target === e.currentTarget || e.target.classList.contains('lightbox-viewport')) {
        this.close()
      }
    },
    handleImageClick() {
      const now = Date.now()
      if (now - this.lastTapTime < 300) {
        this.toggleZoom()
        this.lastTapTime = 0
      } else {
        this.lastTapTime = now
      }
    },
    handleDoubleClick() {
      this.toggleZoom()
    },
    toggleZoom() {
      if (this.scale > 1) {
        this.resetZoom()
      } else {
        this.scale = 2.5
        this.translateX = 0
        this.translateY = 0
      }
    },
    zoomIn() {
      this.scale = Math.min(4, +(this.scale + 0.5).toFixed(1))
    },
    zoomOut() {
      if (this.scale <= 1.5) {
        this.resetZoom()
      } else {
        this.scale = Math.max(1, +(this.scale - 0.5).toFixed(1))
      }
    },
    resetZoom() {
      this.scale = 1
      this.translateX = 0
      this.translateY = 0
    },
    // --- Mouse Drag Handling (PC) ---
    startDrag(e) {
      if (this.scale <= 1) return
      this.isDragging = true
      this.startX = e.clientX
      this.startY = e.clientY
      this.initialTranslateX = this.translateX
      this.initialTranslateY = this.translateY
      e.preventDefault()
    },
    onDrag(e) {
      if (!this.isDragging || this.scale <= 1) return
      const deltaX = e.clientX - this.startX
      const deltaY = e.clientY - this.startY
      this.translateX = this.initialTranslateX + deltaX
      this.translateY = this.initialTranslateY + deltaY
    },
    endDrag() {
      this.isDragging = false
    },
    // --- Touch Drag Handling (Mobile) ---
    onTouchStart(e) {
      if (e.touches.length === 1 && this.scale > 1) {
        this.isDragging = true
        this.startX = e.touches[0].clientX
        this.startY = e.touches[0].clientY
        this.initialTranslateX = this.translateX
        this.initialTranslateY = this.translateY
      }
    },
    onTouchMove(e) {
      if (this.isDragging && e.touches.length === 1 && this.scale > 1) {
        const deltaX = e.touches[0].clientX - this.startX
        const deltaY = e.touches[0].clientY - this.startY
        this.translateX = this.initialTranslateX + deltaX
        this.translateY = this.initialTranslateY + deltaY
        e.preventDefault()
      }
    },
    onTouchEnd() {
      this.isDragging = false
    }
  }
}
</script>

<style scoped>
.lightbox-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.94);
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  outline: none;
  touch-action: none;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.lightbox-btn-close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.15);
  border: none;
  color: #ffffff;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100001;
  transition: all 0.2s ease;
  backdrop-filter: blur(8px);
}

.lightbox-btn-close:hover {
  background: rgba(231, 76, 60, 0.85);
  transform: scale(1.08);
}

.lightbox-viewport {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

.lightbox-image {
  max-width: 92vw;
  max-height: 86vh;
  object-fit: contain;
  cursor: zoom-in;
  transform-origin: center center;
  will-change: transform;
  border-radius: 2px;
}

.lightbox-image.is-zoomed {
  cursor: grab;
}

.lightbox-image.is-dragging {
  cursor: grabbing !important;
}

/* Thanh công cụ Zoom nổi */
.lightbox-toolbar {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(20, 24, 30, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(12px);
  padding: 6px 14px;
  border-radius: 30px;
  z-index: 100000;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
}

.toolbar-btn {
  background: transparent;
  border: none;
  color: #f1f5f9;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.toolbar-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.toolbar-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.zoom-level-text {
  color: #e2e8f0;
  font-size: 13px;
  font-weight: 600;
  min-width: 44px;
  text-align: center;
  cursor: pointer;
}

.zoom-level-text:hover {
  color: #38bdf8;
}

/* Dòng gợi ý thao tác */
.lightbox-hint {
  position: absolute;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  pointer-events: none;
  background: rgba(0, 0, 0, 0.4);
  padding: 4px 12px;
  border-radius: 12px;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .lightbox-btn-close {
    top: 12px;
    right: 12px;
    width: 38px;
    height: 38px;
  }
  .lightbox-toolbar {
    bottom: 20px;
    padding: 4px 10px;
  }
  .lightbox-hint {
    display: none;
  }
}
</style>
