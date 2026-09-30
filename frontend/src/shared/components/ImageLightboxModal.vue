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
        @wheel.prevent="handleWheel"
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
        <!-- Nút thu nhỏ -->
        <button class="toolbar-btn" @click="zoomOut" :disabled="scale <= LIGHTBOX_ZOOM.MIN" title="Thu nhỏ (-15%)">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            <line x1="8" y1="11" x2="14" y2="11"></line>
          </svg>
        </button>

        <!-- Thanh trượt điều chỉnh kích thước tùy ý -->
        <input 
          type="range" 
          class="toolbar-zoom-slider" 
          :min="LIGHTBOX_ZOOM.MIN" 
          :max="LIGHTBOX_ZOOM.MAX" 
          step="0.01" 
          v-model.number="scale" 
          title="Kéo để điều chỉnh kích thước tùy ý" 
        />

        <!-- Nút phóng to -->
        <button class="toolbar-btn" @click="zoomIn" :disabled="scale >= LIGHTBOX_ZOOM.MAX" title="Phóng to (+15%)">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            <line x1="11" y1="8" x2="11" y2="14"></line>
            <line x1="8" y1="11" x2="14" y2="11"></line>
          </svg>
        </button>

        <!-- Khối hiển thị % & mở menu chọn mốc nhanh -->
        <div class="zoom-presets-container" ref="presetsMenuRef">
          <button 
            class="zoom-level-badge" 
            @click="togglePresetsMenu" 
            title="Bấm để chọn mốc kích thước nhanh hoặc nhập số %"
            :class="{ 'is-active': showPresetsMenu }"
          >
            <span>{{ Math.round(scale * 100) }}%</span>
            <svg class="chevron-icon" :class="{ 'rotate': showPresetsMenu }" xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="18 15 12 9 6 15"></polyline>
            </svg>
          </button>

          <!-- Popover chọn mốc nhanh -->
          <transition name="popover-fade">
            <div v-if="showPresetsMenu" class="zoom-presets-popover" @click.stop>
              <div class="popover-title">Chọn kích thước</div>
              <div class="presets-grid">
                <button 
                  v-for="preset in LIGHTBOX_ZOOM.PRESETS" 
                  :key="preset.value" 
                  class="preset-btn" 
                  :class="{ 'is-selected': Math.round(scale * 100) === Math.round(preset.value * 100) }"
                  @click="setZoomPreset(preset.value)"
                >
                  {{ preset.label }}
                </button>
              </div>

              <!-- Nhập số % tùy ý -->
              <div class="custom-percent-row">
                <input 
                  type="number" 
                  class="custom-percent-input" 
                  :min="LIGHTBOX_ZOOM.MIN * 100" 
                  :max="LIGHTBOX_ZOOM.MAX * 100" 
                  v-model.number="customPercentInput"
                  @keydown.enter="applyCustomPercent" 
                  placeholder="Nhập %"
                />
                <button class="btn-apply-percent" @click="applyCustomPercent">Áp dụng</button>
              </div>
            </div>
          </transition>
        </div>

        <!-- Nút đặt lại kích thước chuẩn 100% -->
        <button class="toolbar-btn" @click="resetZoom" :disabled="scale === LIGHTBOX_ZOOM.DEFAULT && translateX === 0 && translateY === 0" title="Đặt lại kích thước chuẩn (100%)">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path>
            <path d="M3 3v5h5"></path>
          </svg>
        </button>
      </div>

      <!-- Hướng dẫn thao tác nhanh cho người dùng trên mobile -->
      <div class="lightbox-hint">
        {{ scale > 1 ? 'Chạm đúp để thu nhỏ · Kéo để di chuyển · Cuộn để zoom' : 'Chạm đúp vào ảnh để phóng to toàn màn hình' }}
      </div>
    </div>
  </Teleport>
</template>

<script>
import { LIGHTBOX_ZOOM } from '@/shared/utils/constants'

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
      scale: LIGHTBOX_ZOOM.DEFAULT,
      translateX: 0,
      translateY: 0,
      isDragging: false,
      startX: 0,
      startY: 0,
      initialTranslateX: 0,
      initialTranslateY: 0,
      lastTapTime: 0,
      lastTouchDist: null,
      showPresetsMenu: false,
      customPercentInput: '',
      LIGHTBOX_ZOOM
    }
  },
  computed: {
    imageTransformStyle() {
      return {
        transform: `translate3d(${this.translateX}px, ${this.translateY}px, 0) scale(${this.scale})`,
        transition: this.isDragging ? 'none' : 'transform 0.2s cubic-bezier(0.2, 0, 0.2, 1)'
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
        this.showPresetsMenu = false
      }
    },
    scale(newVal) {
      if (newVal <= LIGHTBOX_ZOOM.DEFAULT) {
        this.translateX = 0
        this.translateY = 0
      }
    }
  },
  mounted() {
    document.addEventListener('click', this.handleDocumentClick)
  },
  beforeUnmount() {
    document.body.style.overflow = ''
    document.removeEventListener('click', this.handleDocumentClick)
  },
  methods: {
    close() {
      this.showPresetsMenu = false
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
      if (this.scale > LIGHTBOX_ZOOM.DEFAULT) {
        this.resetZoom()
      } else {
        this.scale = 2.0
        this.translateX = 0
        this.translateY = 0
      }
    },
    zoomIn() {
      this.scale = Math.min(LIGHTBOX_ZOOM.MAX, +(this.scale + LIGHTBOX_ZOOM.STEP).toFixed(2))
    },
    zoomOut() {
      this.scale = Math.max(LIGHTBOX_ZOOM.MIN, +(this.scale - LIGHTBOX_ZOOM.STEP).toFixed(2))
    },
    resetZoom() {
      this.scale = LIGHTBOX_ZOOM.DEFAULT
      this.translateX = 0
      this.translateY = 0
      this.showPresetsMenu = false
    },
    handleWheel(e) {
      const delta = e.deltaY < 0 ? LIGHTBOX_ZOOM.STEP : -LIGHTBOX_ZOOM.STEP
      this.scale = Math.min(LIGHTBOX_ZOOM.MAX, Math.max(LIGHTBOX_ZOOM.MIN, +(this.scale + delta).toFixed(2)))
    },
    togglePresetsMenu() {
      this.showPresetsMenu = !this.showPresetsMenu
      if (this.showPresetsMenu) {
        this.customPercentInput = Math.round(this.scale * 100)
      }
    },
    setZoomPreset(val) {
      this.scale = val
      this.showPresetsMenu = false
    },
    applyCustomPercent() {
      const parsed = parseFloat(this.customPercentInput)
      if (!isNaN(parsed)) {
        const clamped = Math.min(LIGHTBOX_ZOOM.MAX * 100, Math.max(LIGHTBOX_ZOOM.MIN * 100, parsed))
        this.setZoomPreset(+(clamped / 100).toFixed(2))
      }
    },
    handleDocumentClick(e) {
      if (this.showPresetsMenu && this.$refs.presetsMenuRef && !this.$refs.presetsMenuRef.contains(e.target)) {
        this.showPresetsMenu = false
      }
    },
    // --- Mouse Drag Handling (PC) ---
    startDrag(e) {
      if (this.scale <= LIGHTBOX_ZOOM.DEFAULT) return
      this.isDragging = true
      this.startX = e.clientX
      this.startY = e.clientY
      this.initialTranslateX = this.translateX
      this.initialTranslateY = this.translateY
      e.preventDefault()
    },
    onDrag(e) {
      if (!this.isDragging || this.scale <= LIGHTBOX_ZOOM.DEFAULT) return
      const deltaX = e.clientX - this.startX
      const deltaY = e.clientY - this.startY
      this.translateX = this.initialTranslateX + deltaX
      this.translateY = this.initialTranslateY + deltaY
    },
    endDrag() {
      this.isDragging = false
    },
    // --- Touch Drag & Pinch Handling (Mobile) ---
    onTouchStart(e) {
      if (e.touches.length === 1 && this.scale > LIGHTBOX_ZOOM.DEFAULT) {
        this.isDragging = true
        this.startX = e.touches[0].clientX
        this.startY = e.touches[0].clientY
        this.initialTranslateX = this.translateX
        this.initialTranslateY = this.translateY
      } else if (e.touches.length === 2) {
        this.isDragging = false
        this.lastTouchDist = Math.hypot(
          e.touches[0].clientX - e.touches[1].clientX,
          e.touches[0].clientY - e.touches[1].clientY
        )
      }
    },
    onTouchMove(e) {
      if (this.isDragging && e.touches.length === 1 && this.scale > LIGHTBOX_ZOOM.DEFAULT) {
        const deltaX = e.touches[0].clientX - this.startX
        const deltaY = e.touches[0].clientY - this.startY
        this.translateX = this.initialTranslateX + deltaX
        this.translateY = this.initialTranslateY + deltaY
        e.preventDefault()
      } else if (e.touches.length === 2 && this.lastTouchDist) {
        const dist = Math.hypot(
          e.touches[0].clientX - e.touches[1].clientX,
          e.touches[0].clientY - e.touches[1].clientY
        )
        const diff = (dist - this.lastTouchDist) * 0.008
        this.scale = Math.min(LIGHTBOX_ZOOM.MAX, Math.max(LIGHTBOX_ZOOM.MIN, +(this.scale + diff).toFixed(2)))
        this.lastTouchDist = dist
        e.preventDefault()
      }
    },
    onTouchEnd(e) {
      if (e.touches.length < 2) {
        this.lastTouchDist = null
      }
      if (e.touches.length === 0) {
        this.isDragging = false
      }
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
  background: rgba(20, 24, 30, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(14px);
  padding: 6px 14px;
  border-radius: 30px;
  z-index: 100000;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.45);
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
  flex-shrink: 0;
}

.toolbar-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.toolbar-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* Slider điều chỉnh kích thước */
.toolbar-zoom-slider {
  width: 85px;
  height: 4px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 4px;
  outline: none;
  -webkit-appearance: none;
  appearance: none;
  cursor: pointer;
  margin: 0 4px;
}

.toolbar-zoom-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #38bdf8;
  cursor: pointer;
  box-shadow: 0 0 6px rgba(56, 189, 248, 0.6);
  transition: transform 0.15s ease, background 0.15s ease;
}

.toolbar-zoom-slider::-webkit-slider-thumb:hover {
  transform: scale(1.2);
  background: #0ea5e9;
}

.toolbar-zoom-slider::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #38bdf8;
  cursor: pointer;
  border: none;
}

/* Badge hiển thị % và Container Presets */
.zoom-presets-container {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.zoom-level-badge {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #e2e8f0;
  font-size: 13px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s ease;
}

.zoom-level-badge:hover,
.zoom-level-badge.is-active {
  background: rgba(56, 189, 248, 0.2);
  border-color: #38bdf8;
  color: #38bdf8;
}

.chevron-icon {
  transition: transform 0.2s ease;
}
.chevron-icon.rotate {
  transform: rotate(180deg);
}

/* Popover chọn mốc zoom */
.zoom-presets-popover {
  position: absolute;
  bottom: calc(100% + 14px);
  left: 50%;
  transform: translateX(-50%);
  background: rgba(20, 24, 30, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(16px);
  border-radius: 12px;
  padding: 12px;
  width: 220px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.5);
  z-index: 100005;
}

.zoom-presets-popover::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%) rotate(45deg);
  width: 10px;
  height: 10px;
  background: rgba(20, 24, 30, 0.95);
  border-right: 1px solid rgba(255, 255, 255, 0.18);
  border-bottom: 1px solid rgba(255, 255, 255, 0.18);
}

.popover-title {
  color: #94a3b8;
  font-size: 11px;
  text-transform: uppercase;
  font-weight: 600;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
  text-align: center;
}

.presets-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
  margin-bottom: 10px;
}

.preset-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #f1f5f9;
  font-size: 12px;
  font-weight: 500;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: center;
}

.preset-btn:hover {
  background: rgba(56, 189, 248, 0.25);
  border-color: #38bdf8;
  color: #ffffff;
}

.preset-btn.is-selected {
  background: #38bdf8;
  border-color: #38bdf8;
  color: #0f172a;
  font-weight: 700;
}

.custom-percent-row {
  display: flex;
  gap: 6px;
  align-items: center;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: 8px;
}

.custom-percent-input {
  flex: 1;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  color: #ffffff;
  padding: 5px 8px;
  font-size: 12px;
  outline: none;
  width: 100%;
}

.custom-percent-input:focus {
  border-color: #38bdf8;
  background: rgba(255, 255, 255, 0.15);
}

.btn-apply-percent {
  background: #38bdf8;
  color: #0f172a;
  border: none;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  padding: 6px 10px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.btn-apply-percent:hover {
  background: #7dd3fc;
}

.popover-fade-enter-active,
.popover-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.popover-fade-enter-from,
.popover-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(6px);
}

/* Dòng gợi ý thao tác */
.lightbox-hint {
  position: absolute;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255, 255, 255, 0.75);
  font-size: 12px;
  pointer-events: none;
  background: rgba(0, 0, 0, 0.45);
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
    padding: 6px 10px;
    gap: 6px;
  }
  .toolbar-zoom-slider {
    width: 60px;
  }
  .zoom-presets-popover {
    width: 200px;
  }
  .lightbox-hint {
    display: none;
  }
}
</style>
