<template>
  <div class="image-uploader-panel" :class="{ 'disabled-panel': disabled }">
    <!-- Component Loading mờ toàn màn hình dùng chung hệ thống -->
    <Loading :visible="isUploading" />

    <div class="uploader-actions">
      <button class="btn-upload" @click="triggerFileInput" :disabled="isUploading || disabled">
        <svg v-if="!isUploading" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-paperclip">
          <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"></path>
        </svg>
        <span v-else class="spinner"></span>
        {{ isUploading ? 'Đang tải...' : 'Up Ảnh' }}
      </button>
      <input 
        type="file" 
        ref="fileInput" 
        multiple 
        accept="image/jpeg,image/png,image/webp,image/gif" 
        style="display: none" 
        @change="onFileChange"
      />
    </div>

    <div v-if="uploadedImages.length > 0" class="uploaded-images-container">
      <div v-for="(img, index) in uploadedImages" :key="index" class="image-thumbnail-wrapper" @mouseleave="activeInsertIndex = null">
        <img :src="img.url" :alt="img.name" class="image-thumbnail" @click="handleImageClick(img.url, index)" :title="isMultipleSelectionMode ? 'Click để chọn' : 'Click để xem ảnh lớn'" :class="{ 'selected-img': selectedImages.includes(index) }" />
        
        <div v-if="isMultipleSelectionMode" class="checkbox-container" @click="toggleSelectImage(index)">
          <input type="checkbox" :checked="selectedImages.includes(index)" @click.stop="toggleSelectImage(index)" />
        </div>

        <div class="thumbnail-overlay" v-if="!isMultipleSelectionMode">
          <div class="insert-dropdown">
            <button class="btn-insert-over" @click.stop="toggleInsertMenu(index)">Chèn...</button>
            <div class="insert-menu" v-if="activeInsertIndex === index">
              <button class="btn-menu-item" @click.stop="insertImage(img.url, 'thumbnail')">Hình thu nhỏ</button>
              <button class="btn-menu-item" @click.stop="insertImage(img.url, 'full')">Hình đầy đủ</button>
            </div>
          </div>
          <button class="btn-delete-over" @click="removeImage(index)" title="Xóa ảnh">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
          </button>
        </div>
        <div class="thumbnail-name" :title="img.name">{{ img.name }}</div>
      </div>
    </div>

    <div v-if="uploadedImages.length > 1 && !isMultipleSelectionMode" class="multiple-actions">
      <button class="btn-insert-multiple" @click="enableMultipleMode">Insert multiple...</button>
    </div>

    <div v-if="isMultipleSelectionMode" class="multiple-action-bar">
      <div class="bar-left">
        <label class="select-all-label">
          <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" /> Chọn tất cả
        </label>
        <div class="insert-group">
          <span>Chèn:</span>
          <button class="btn-action-insert" @click="insertSelected('thumbnail')" :disabled="selectedImages.length === 0">Hình thu nhỏ</button>
          <button class="btn-action-insert" @click="insertSelected('full')" :disabled="selectedImages.length === 0">Hình đầy đủ</button>
        </div>
      </div>
      <div class="bar-right">
        <button class="btn-action-icon" @click="deleteSelected" :disabled="selectedImages.length === 0" title="Xóa ảnh đã chọn">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
        </button>
        <button class="btn-action-cancel" @click="cancelMultipleMode">Hủy</button>
      </div>
    </div>
  </div>
</template>

<script>
import uploadService from '@/apps/Forum/services/upload.service'
import { processFilesForUpload } from '@/shared/utils/heicUtils'
import Loading from '@/shared/components/Loading.vue'

export default {
  name: 'ImageUploaderPanel',
  components: {
    Loading
  },
  props: {
    images: {
      type: Array,
      default: () => []
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      isUploading: false,
      uploadedImages: [...this.images],
      activeInsertIndex: null,
      isMultipleSelectionMode: false,
      selectedImages: []
    }
  },
  watch: {
    images: {
      handler(newVal) {
        // Chỉ cập nhật nếu giá trị thực sự khác nhau để tránh vòng lặp
        if (JSON.stringify(newVal) !== JSON.stringify(this.uploadedImages)) {
          this.uploadedImages = [...newVal];
        }
      },
      deep: true
    },
    uploadedImages: {
      handler(newVal) {
        // Chỉ emit nếu giá trị thực sự khác với prop
        if (JSON.stringify(newVal) !== JSON.stringify(this.images)) {
          this.$emit('update:images', newVal);
        }
      },
      deep: true
    }
  },
  computed: {
    isAllSelected() {
      return this.uploadedImages.length > 0 && this.selectedImages.length === this.uploadedImages.length;
    }
  },
  methods: {
    handleImageClick(url, index) {
      if (this.isMultipleSelectionMode) {
        this.toggleSelectImage(index);
      } else {
        this.enlargeImage(url);
      }
    },
    toggleSelectImage(index) {
      const idx = this.selectedImages.indexOf(index);
      if (idx > -1) {
        this.selectedImages.splice(idx, 1);
      } else {
        this.selectedImages.push(index);
      }
    },
    toggleSelectAll() {
      if (this.isAllSelected) {
        this.selectedImages = [];
      } else {
        this.selectedImages = this.uploadedImages.map((_, i) => i);
      }
    },
    insertSelected(type) {
      if (this.selectedImages.length === 0) return;
      const urls = this.selectedImages.map(i => this.uploadedImages[i].url);
      this.$emit('insert-images', urls, type);
      this.cancelMultipleMode();
    },
    deleteSelected() {
      if (this.selectedImages.length === 0) return;
      const sortedIndexes = [...this.selectedImages].sort((a, b) => b - a);
      sortedIndexes.forEach(idx => {
        this.uploadedImages.splice(idx, 1);
      });
      this.cancelMultipleMode();
      if (this.uploadedImages.length <= 1) {
         this.isMultipleSelectionMode = false;
      }
    },
    cancelMultipleMode() {
      this.isMultipleSelectionMode = false;
      this.selectedImages = [];
    },
    enableMultipleMode() {
      this.isMultipleSelectionMode = true;
      this.selectedImages = [];
    },
    toggleInsertMenu(index) {
      this.activeInsertIndex = this.activeInsertIndex === index ? null : index;
    },
    triggerFileInput() {
      this.$refs.fileInput.click()
    },
    async onFileChange(event) {
      const files = Array.from(event.target.files)
      if (files.length === 0) return

      this.isUploading = true
      try {
        const formData = new FormData()
        files.forEach(file => formData.append('files', file))

        const res = await uploadService.uploadMultiple(formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        
        const validResults = res.data || []
        const newImages = validResults.filter(result => result.url)
        
        this.uploadedImages.push(...newImages)
        
        // Reset input để có thể chọn lại file vừa tải
        this.$refs.fileInput.value = ''
      } catch (err) {
        console.error('Lỗi khi tải ảnh:', err)
        alert('Không thể tải ảnh. Vui lòng thử lại sau.')
      } finally {
        this.isUploading = false
      }
    },
    addImage(image) {
      this.uploadedImages.push(image);
    },
    insertImage(url, type = 'full') {
      this.$emit('insert-images', [url], type)
      this.activeInsertIndex = null;
    },
    removeImage(index) {
      this.uploadedImages.splice(index, 1)
    },
    enlargeImage(url) {
      window.open(url, '_blank')
    }
  }
}
</script>

<style scoped>
.image-uploader-panel {
  margin-top: 10px;
  border: 1px solid #ddd;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 4px;
}

.disabled-panel {
  opacity: 0.6;
  pointer-events: none;
}

.btn-upload {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: white;
  border: 1px solid #ccc;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  color: #3498db;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-upload:hover:not(:disabled) {
  background: #f0f7fb;
  border-color: #3498db;
}

.btn-upload:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  color: #888;
}

.icon-paperclip {
  color: #3498db;
}

.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(0, 0, 0, 0.1);
  border-top-color: #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.uploaded-images-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 15px;
}

.image-thumbnail-wrapper {
  position: relative;
  width: 200px;
  height: 200px;
  background: #eee;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
}

.image-thumbnail {
  width: 100%;
  height: calc(100% - 24px); /* Chừa chỗ cho tên file */
  object-fit: cover;
  cursor: zoom-in;
  display: block;
}

.thumbnail-overlay {
  position: absolute;
  top: 5px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 0 5px;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-thumbnail-wrapper:hover .thumbnail-overlay {
  opacity: 1;
}

.insert-dropdown {
  position: relative;
}

.insert-menu {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 4px;
  background: rgba(0, 0, 0, 0.85);
  border-radius: 3px;
  display: flex;
  flex-direction: column;
  padding: 4px;
  gap: 2px;
  z-index: 10;
  min-width: 90px;
}

.btn-menu-item {
  background: transparent;
  color: white;
  border: none;
  padding: 4px 6px;
  font-size: 11px;
  cursor: pointer;
  text-align: left;
  white-space: nowrap;
  border-radius: 2px;
  transition: background 0.2s;
}

.btn-menu-item:hover {
  background: rgba(52, 152, 219, 0.9);
}

.btn-insert-over {
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  border-radius: 3px;
  padding: 4px 8px;
  font-size: 11px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-insert-over:hover {
  background: rgba(52, 152, 219, 0.9);
}

.btn-delete-over {
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  border-radius: 3px;
  padding: 4px 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.btn-delete-over:hover {
  background: rgba(231, 76, 60, 0.9);
}

.thumbnail-name {
  height: 24px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 11px;
  line-height: 24px;
  padding: 0 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  position: absolute;
  bottom: 0;
  width: 100%;
  box-sizing: border-box;
}

.multiple-actions {
  margin-top: 15px;
}

.btn-insert-multiple {
  background: white;
  border: 1px solid #ccc;
  padding: 6px 15px;
  border-radius: 4px;
  cursor: pointer;
  color: #3498db;
  transition: all 0.2s;
  font-size: 0.9rem;
}

.btn-insert-multiple:hover {
  background: #f0f7fb;
  border-color: #3498db;
}

.selected-img {
  border: 2px solid #3498db;
  box-sizing: border-box;
}

.checkbox-container {
  position: absolute;
  top: 5px;
  left: 5px;
  background: rgba(255, 255, 255, 0.8);
  padding: 3px;
  border-radius: 3px;
  cursor: pointer;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.checkbox-container input {
  margin: 0;
  cursor: pointer;
}

.multiple-action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding: 8px 10px;
  background: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.bar-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.select-all-label {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  color: #3498db;
  font-size: 13px;
  background: white;
  border: 1px solid #ddd;
  padding: 4px 8px;
  border-radius: 3px;
}

.insert-group {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
}

.btn-action-insert {
  background: white;
  border: 1px solid #ddd;
  color: #3498db;
  padding: 4px 10px;
  border-radius: 3px;
  cursor: pointer;
}

.btn-action-insert:disabled {
  color: #aaa;
  cursor: not-allowed;
  border-color: #eee;
}

.btn-action-insert:not(:disabled):hover {
  background: #f0f7fb;
  border-color: #3498db;
}

.bar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-action-icon {
  background: white;
  border: 1px solid #ddd;
  color: #3498db;
  padding: 4px 8px;
  border-radius: 3px;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.btn-action-icon:disabled {
  color: #aaa;
  cursor: not-allowed;
  border-color: #eee;
}

.btn-action-icon:not(:disabled):hover {
  background: #f0f7fb;
  border-color: #3498db;
}

.btn-action-cancel {
  background: white;
  border: 1px solid #ddd;
  color: #3498db;
  padding: 4px 12px;
  border-radius: 3px;
  cursor: pointer;
}

.btn-action-cancel:hover {
  background: #f0f7fb;
  border-color: #3498db;
}
</style>
