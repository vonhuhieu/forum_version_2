<template>
  <teleport to="body">
    <div class="avatar-modal-overlay" v-if="show" @click.self="close">
      <div class="avatar-modal">
        <!-- Header -->
        <div class="avatar-modal-header">
          <h3>{{ mode === 'banner' ? 'Cập nhật ảnh bìa' : 'Cập nhật ảnh đại diện' }}</h3>
          <button class="btn-close-modal" @click="close">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>

        <!-- Body -->
        <div class="avatar-modal-body">
          <!-- Vùng hiển thị avatar/banner hiện tại hoặc preview mới -->
          <div class="avatar-preview-header">
            <div :class="mode === 'banner' ? 'banner-preview-rect' : 'avatar-preview-circle-large'" :style="mode === 'banner' ? { borderRadius: '6px', width: '240px', height: '60px', overflow: 'hidden', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: '#f8f9fa', border: '1px solid #dee2e6' } : {}">
              <img v-if="previewDataUrl" :src="previewDataUrl" :style="mode === 'banner' ? { width: '240px', height: '60px', objectFit: 'cover' } : {}" class="avatar-img-large" />
              <img v-else-if="mode !== 'banner' && isAvatarUrl(currentAvatar)" :src="currentAvatar" class="avatar-img-large" />
              <img v-else-if="mode === 'banner' && currentUser && currentUser.profileBanner" :src="currentUser.profileBanner" :style="{ width: '240px', height: '60px', objectFit: 'cover' }" />
              <div v-else-if="mode === 'banner'" class="avatar-color-large" :style="{ backgroundColor: '#edf6fd', color: '#1a507a', width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1rem', fontWeight: 'bold' }">
                Chưa có banner
              </div>
              <div v-else class="avatar-color-large" :style="{ backgroundColor: currentAvatar || '#fff', color: currentAvatar ? '#fff' : '#1a507a' }">
                {{ userInitial }}
              </div>
            </div>
            <div class="avatar-preview-info">
              <span class="avatar-preview-label">
                {{ previewDataUrl ? 'Ảnh mới (Xem trước)' : (mode === 'banner' ? 'Ảnh bìa hiện tại' : 'Ảnh đại diện hiện tại') }}
              </span>
              <span class="avatar-preview-sub" v-if="currentUser">
                {{ currentUser.displayName || currentUser.username }}
              </span>
            </div>
          </div>

          <!-- Step 1: Drop Zone -->
          <div v-if="!imageSrc"
            class="drop-zone"
            :class="{ 'drag-over': isDragging }"
            @dragover.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
            @drop.prevent="onDrop"
            @click="triggerFileInput"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none"
              stroke="#adb5bd" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
              <circle cx="8.5" cy="8.5" r="1.5"></circle>
              <polyline points="21 15 16 10 5 21"></polyline>
            </svg>
            <p class="drop-title">Kéo thả ảnh vào đây</p>
            <p class="drop-sub">hoặc <span class="drop-link">click để chọn file</span></p>
            <p class="drop-hint">PNG, JPG, WEBP — tối đa 5MB</p>
          </div>

          <!-- Step 2: Crop Area -->
          <div v-else class="crop-section">
            <div class="crop-container" :style="{ width: canvasWidth + 'px', height: canvasHeight + 'px' }">
              <canvas
                ref="cropCanvas"
                class="crop-canvas"
                :style="{ width: canvasWidth + 'px', height: canvasHeight + 'px' }"
                @mousedown="onMouseDown"
                @mousemove="onMouseMove"
                @mouseup="onMouseUp"
                @mouseleave="onMouseUp"
                @wheel.prevent="onWheel"
                @touchstart.prevent="onTouchStart"
                @touchmove.prevent="onTouchMove"
                @touchend.prevent="onTouchEnd"
              ></canvas>
              <div class="crop-hint-overlay">
                <span>Kéo để di chuyển · Cuộn để zoom</span>
              </div>
            </div>

            <!-- Zoom slider -->
            <div class="zoom-controls">
              <button class="btn-zoom" @click="zoomOut" title="Thu nhỏ">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none"
                  stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                  <line x1="8" y1="11" x2="14" y2="11"></line>
                </svg>
              </button>
              <input type="range" class="zoom-slider" min="0.3" max="3" step="0.01" v-model.number="scale" @input="drawCanvas" />
              <button class="btn-zoom" @click="zoomIn" title="Phóng to">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none"
                  stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                  <line x1="8" y1="11" x2="14" y2="11"></line>
                  <line x1="11" y1="8" x2="11" y2="14"></line>
                </svg>
              </button>
            </div>

            <!-- Preview + actions -->
            <div class="preview-row" style="justify-content: center;">
              <div class="crop-actions">
                <button class="btn-reselect" @click="resetImage">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none"
                    stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="1 4 1 10 7 10"></polyline>
                    <path d="M3.51 15a9 9 0 1 0 .49-3.51"></path>
                  </svg>
                  Chọn lại
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="avatar-modal-footer">
          <button class="btn-save" @click="saveAvatar" :disabled="!imageSrc || isUploading">
            {{ mode === 'banner' ? 'Lưu ảnh bìa' : 'Lưu ảnh đại diện' }}
          </button>
          <button class="btn-cancel" @click="close" :disabled="isUploading">Hủy</button>
        </div>

        <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFileSelected" />
      </div>
      <Loading :visible="isUploading" />
    </div>
  </teleport>
</template>

<script>
import api from '@/shared/services/api.service'
import Loading from '@/shared/components/Loading.vue'

export default {
  name: 'AvatarUploadModal',
  components: {
    Loading
  },
  props: {
    show: { type: Boolean, default: false },
    currentUser: { type: Object, default: null },
    mode: { type: String, default: 'avatar' }
  },
  emits: ['close', 'avatar-updated', 'banner-updated'],
  data() {
    return {
      imageSrc: null,
      image: null,
      scale: 1,
      offsetX: 0,
      offsetY: 0,
      isDragging: false,
      isDraggingCanvas: false,
      lastMouseX: 0,
      lastMouseY: 0,
      isUploading: false,
      previewDataUrl: '',
      lastTouchDist: null,
    }
  },
  computed: {
    canvasWidth() {
      return this.mode === 'banner' ? 440 : 320
    },
    canvasHeight() {
      return this.mode === 'banner' ? 110 : 320
    },
    outputWidth() {
      return this.mode === 'banner' ? 960 : 400
    },
    outputHeight() {
      return this.mode === 'banner' ? 240 : 400
    },
    previewStyle() {
      return {
        backgroundImage: this.previewDataUrl ? `url(${this.previewDataUrl})` : 'none',
        backgroundSize: 'cover',
        backgroundPosition: 'center',
      }
    },
    currentAvatar() {
      return this.currentUser ? this.currentUser.avatar : ''
    },
    userInitial() {
      if (!this.currentUser) return '?'
      const name = this.currentUser.displayName || this.currentUser.username || ''
      return name.charAt(0).toUpperCase()
    }
  },
  watch: {
    show(val) {
      if (!val) this.resetAll()
    }
  },
  methods: {
    close() { this.$emit('close') },
    isAvatarUrl(avatar) {
      if (!avatar) return false
      return avatar.startsWith('http://') || avatar.startsWith('https://') || avatar.startsWith('/')
    },
    triggerFileInput() { this.$refs.fileInput.click() },

    onFileSelected(e) {
      const file = e.target.files[0]
      if (file) this.loadImage(file)
      e.target.value = ''
    },
    onDrop(e) {
      this.isDragging = false
      const file = e.dataTransfer.files[0]
      if (file && file.type.startsWith('image/')) this.loadImage(file)
    },

    loadImage(file) {
      if (file.size > 5 * 1024 * 1024) {
        alert('Ảnh vượt quá 5MB, vui lòng chọn ảnh nhỏ hơn.')
        return
      }
      const reader = new FileReader()
      reader.onload = (e) => {
        this.imageSrc = e.target.result
        const img = new Image()
        img.onload = () => {
          this.image = img
          // Fit image to cover canvas
          const fitScale = Math.max(this.canvasWidth / img.width, this.canvasHeight / img.height)
          this.scale = fitScale
          this.offsetX = (this.canvasWidth - img.width * fitScale) / 2
          this.offsetY = (this.canvasHeight - img.height * fitScale) / 2
          this.$nextTick(() => this.drawCanvas())
        }
        img.src = e.target.result
      }
      reader.readAsDataURL(file)
    },

    drawCanvas() {
      const canvas = this.$refs.cropCanvas
      if (!canvas || !this.image) return
      canvas.width = this.canvasWidth
      canvas.height = this.canvasHeight
      const ctx = canvas.getContext('2d')
      ctx.clearRect(0, 0, this.canvasWidth, this.canvasHeight)

      // Draw image with current scale & offset
      const w = this.image.width * this.scale
      const h = this.image.height * this.scale
      ctx.drawImage(this.image, this.offsetX, this.offsetY, w, h)

      if (this.mode === 'avatar') {
        // Lớp tối CHỈ bên ngoài vòng tròn (dùng even-odd fill rule)
        ctx.save()
        ctx.beginPath()
        ctx.rect(0, 0, this.canvasWidth, this.canvasHeight)
        ctx.arc(this.canvasWidth / 2, this.canvasHeight / 2, this.canvasWidth / 2 - 4, 0, Math.PI * 2, true)
        ctx.fillStyle = 'rgba(0,0,0,0.45)'
        ctx.fill('evenodd')
        ctx.restore()

        // Circle border
        ctx.strokeStyle = '#ffffff'
        ctx.lineWidth = 2.5
        ctx.beginPath()
        ctx.arc(this.canvasWidth / 2, this.canvasHeight / 2, this.canvasWidth / 2 - 4, 0, Math.PI * 2)
        ctx.stroke()
      } else {
        // Banner mode: rectangular border highlight
        ctx.strokeStyle = '#ffffff'
        ctx.lineWidth = 2
        ctx.strokeRect(1, 1, this.canvasWidth - 2, this.canvasHeight - 2)
      }

      this.updatePreview()
    },

    updatePreview() {
      const offscreen = document.createElement('canvas')
      offscreen.width = this.outputWidth
      offscreen.height = this.outputHeight
      const ctx = offscreen.getContext('2d')
      const sf = this.outputWidth / this.canvasWidth
      if (this.mode === 'avatar') {
        ctx.beginPath()
        ctx.arc(this.outputWidth / 2, this.outputHeight / 2, this.outputWidth / 2, 0, Math.PI * 2)
        ctx.clip()
      }
      ctx.drawImage(this.image, this.offsetX * sf, this.offsetY * sf,
        this.image.width * this.scale * sf, this.image.height * this.scale * sf)
      this.previewDataUrl = offscreen.toDataURL('image/jpeg', 0.95)
    },

    getCroppedBlob() {
      return new Promise((resolve) => {
        const offscreen = document.createElement('canvas')
        offscreen.width = this.outputWidth
        offscreen.height = this.outputHeight
        const ctx = offscreen.getContext('2d')
        const sf = this.outputWidth / this.canvasWidth
        if (this.mode === 'avatar') {
          ctx.beginPath()
          ctx.arc(this.outputWidth / 2, this.outputHeight / 2, this.outputWidth / 2, 0, Math.PI * 2)
          ctx.clip()
        }
        ctx.drawImage(this.image, this.offsetX * sf, this.offsetY * sf,
          this.image.width * this.scale * sf, this.image.height * this.scale * sf)
        offscreen.toBlob(resolve, 'image/jpeg', 0.92)
      })
    },

    // --- Mouse drag ---
    onMouseDown(e) {
      this.isDraggingCanvas = true
      this.lastMouseX = e.clientX
      this.lastMouseY = e.clientY
    },
    onMouseMove(e) {
      if (!this.isDraggingCanvas) return
      this.offsetX += e.clientX - this.lastMouseX
      this.offsetY += e.clientY - this.lastMouseY
      this.lastMouseX = e.clientX
      this.lastMouseY = e.clientY
      this.drawCanvas()
    },
    onMouseUp() { this.isDraggingCanvas = false },

    // --- Wheel zoom ---
    onWheel(e) {
      const delta = e.deltaY < 0 ? 0.1 : -0.1
      const newScale = Math.min(3, Math.max(0.3, this.scale + delta))
      const cx = this.canvasWidth / 2
      const cy = this.canvasHeight / 2
      const ratio = newScale / this.scale
      this.offsetX = cx + (this.offsetX - cx) * ratio
      this.offsetY = cy + (this.offsetY - cy) * ratio
      this.scale = newScale
      this.drawCanvas()
    },

    // --- Touch events ---
    onTouchStart(e) {
      if (e.touches.length === 1) {
        this.isDraggingCanvas = true
        this.lastMouseX = e.touches[0].clientX
        this.lastMouseY = e.touches[0].clientY
        this.lastTouchDist = null
      } else if (e.touches.length === 2) {
        this.lastTouchDist = Math.hypot(
          e.touches[0].clientX - e.touches[1].clientX,
          e.touches[0].clientY - e.touches[1].clientY
        )
      }
    },
    onTouchMove(e) {
      if (e.touches.length === 1 && this.isDraggingCanvas) {
        this.offsetX += e.touches[0].clientX - this.lastMouseX
        this.offsetY += e.touches[0].clientY - this.lastMouseY
        this.lastMouseX = e.touches[0].clientX
        this.lastMouseY = e.touches[0].clientY
        this.drawCanvas()
      } else if (e.touches.length === 2 && this.lastTouchDist) {
        const dist = Math.hypot(
          e.touches[0].clientX - e.touches[1].clientX,
          e.touches[0].clientY - e.touches[1].clientY
        )
        this.scale = Math.min(3, Math.max(0.3, this.scale + (dist - this.lastTouchDist) * 0.01))
        this.drawCanvas()
        this.lastTouchDist = dist
      }
    },
    onTouchEnd() { this.isDraggingCanvas = false; this.lastTouchDist = null },

    zoomIn() {
      this.scale = Math.min(3, this.scale + 0.15)
      this.drawCanvas()
    },
    zoomOut() {
      this.scale = Math.max(0.3, this.scale - 0.15)
      this.drawCanvas()
    },

    resetImage() {
      this.imageSrc = null
      this.image = null
      this.scale = 1
      this.offsetX = 0
      this.offsetY = 0
      this.previewDataUrl = ''
    },
    resetAll() { this.resetImage(); this.isUploading = false },

    async saveAvatar() {
      if (!this.image) return
      this.isUploading = true
      try {
        // 1. Crop canvas to blob
        const blob = await this.getCroppedBlob()
        const filename = this.mode === 'banner' ? 'banner.jpg' : 'avatar.jpg'
        const formData = new FormData()
        formData.append('file', blob, filename)

        // 2. Upload file
        const uploadRes = await api.post('/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        const fileUrl = uploadRes.data?.url
        if (!fileUrl) throw new Error('Upload failed')

        // 3. Update user avatar or banner
        if (this.mode === 'banner') {
          await api.put('/users/me/banner', { banner: fileUrl })
          this.$emit('banner-updated', fileUrl)
        } else {
          await api.put('/users/me/avatar', { avatar: fileUrl })
          this.$emit('avatar-updated', fileUrl)
        }

        this.close()
      } catch (err) {
        console.error('File upload error:', err)
        alert(this.mode === 'banner' ? 'Có lỗi khi lưu ảnh bìa. Vui lòng thử lại.' : 'Có lỗi khi lưu ảnh đại diện. Vui lòng thử lại.')
      } finally {
        this.isUploading = false
      }
    }
  }
}
</script>

<style scoped>
.avatar-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: avatarFadeIn 0.15s ease;
}
@keyframes avatarFadeIn { from { opacity: 0 } to { opacity: 1 } }

.avatar-modal {
  background: #fff;
  border-radius: 12px;
  width: 480px;
  max-width: 95vw;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  animation: avatarSlideUp 0.2s ease;
}
@keyframes avatarSlideUp {
  from { transform: translateY(20px); opacity: 0 }
  to { transform: translateY(0); opacity: 1 }
}

.avatar-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e9ecef;
}
.avatar-modal-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #212529;
}

.avatar-preview-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
  gap: 8px;
}
.avatar-preview-circle-large {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 3px solid #fff;
  outline: 1px solid #dee2e6;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8f9fa;
}
.avatar-img-large {
  width: 96px;
  height: 96px;
  object-fit: cover;
  display: block;
}
.avatar-color-large {
  width: 96px;
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  font-weight: bold;
}
.avatar-preview-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.avatar-preview-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #495057;
}
.avatar-preview-sub {
  font-size: 0.75rem;
  color: #8c8c8c;
}

.btn-close-modal {
  background: none;
  border: none;
  cursor: pointer;
  color: #6c757d;
  padding: 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  transition: background 0.15s;
}
.btn-close-modal:hover { background: #f1f3f5; color: #212529; }

.avatar-modal-body {
  padding: 20px;
  flex: 1;
  overflow-y: auto;
}

/* Drop zone */
.drop-zone {
  border: 2px dashed #ced4da;
  border-radius: 12px;
  padding: 48px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.drop-zone:hover,
.drop-zone.drag-over {
  border-color: #1a507a;
  background: #f0f7ff;
}
.drop-zone.drag-over { transform: scale(1.02); }
.drop-title { margin: 0; font-weight: 600; color: #343a40; font-size: 1rem; }
.drop-sub { margin: 0; color: #6c757d; font-size: 0.9rem; }
.drop-link { color: #1a507a; text-decoration: underline; }
.drop-hint { margin: 0; color: #adb5bd; font-size: 0.8rem; }

/* Crop section */
.crop-section { display: flex; flex-direction: column; gap: 16px; }

.crop-container {
  position: relative;
  width: 320px;
  height: 320px;
  margin: 0 auto;
  border-radius: 12px;
  overflow: hidden;
  background: #1a1a2e;
  cursor: grab;
}
.crop-container:active { cursor: grabbing; }

.crop-canvas {
  width: 320px;
  height: 320px;
  display: block;
  user-select: none;
  touch-action: none;
}

.crop-hint-overlay {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0,0,0,0.55);
  color: #fff;
  font-size: 0.72rem;
  padding: 3px 10px;
  border-radius: 20px;
  white-space: nowrap;
  pointer-events: none;
}

/* Zoom slider */
.zoom-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 4px;
}
.zoom-slider {
  flex: 1;
  accent-color: #1a507a;
  cursor: pointer;
}
.btn-zoom {
  background: none;
  border: none;
  cursor: pointer;
  color: #6c757d;
  padding: 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  transition: all 0.15s;
}
.btn-zoom:hover {
  background: #e9ecef;
  color: #1a507a;
}

/* Preview */
.preview-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.preview-block { display: flex; align-items: center; gap: 12px; }
.preview-label {
  margin: 0;
  font-size: 0.8rem;
  color: #6c757d;
  writing-mode: vertical-lr;
  transform: rotate(180deg);
}
.preview-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 2px solid #dee2e6;
  background-color: #f1f3f5;
}
.preview-square {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  border: 2px solid #dee2e6;
  background-color: #f1f3f5;
}

.crop-actions { display: flex; flex-direction: column; gap: 8px; }
.btn-reselect {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  background: #f8f9fa;
  border: 1px solid #ced4da;
  border-radius: 8px;
  font-size: 0.85rem;
  color: #495057;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-reselect:hover { background: #e9ecef; border-color: #adb5bd; }

/* Footer */
.avatar-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  align-items: center;
  padding: 14px 20px;
  border-top: 1px solid #e9ecef;
  background: #f8f9fa;
}

.btn-cancel {
  padding: 8px 20px;
  border: 1px solid #ced4da;
  border-radius: 8px;
  background: #fff;
  color: #495057;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.15s;
}
.btn-cancel:hover:not(:disabled) { background: #f1f3f5; }

.btn-save {
  padding: 8px 24px;
  border: none;
  border-radius: 8px;
  background: #1a507a;
  color: #fff;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.15s;
}
.btn-save:hover:not(:disabled) { background: #14406a; }
.btn-save:disabled,
.btn-cancel:disabled { opacity: 0.6; cursor: not-allowed; }

@keyframes spin { to { transform: rotate(360deg) } }
.spin-icon { animation: spin 0.8s linear infinite; }
</style>
