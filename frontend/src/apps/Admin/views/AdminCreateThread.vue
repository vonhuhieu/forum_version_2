<template>
  <div class="admin-create-thread">
    <Loading :visible="isSubmitting || isPageLoading" />
    <div class="card">
      <div class="card-header">{{ pageTitle }}</div>
      <div class="admin-form" style="padding: 2rem;">
        <div class="form-group" style="display: flex; gap: 1rem; align-items: center; margin-bottom: 1.5rem;">
          <div class="custom-select" style="position: relative; width: 250px;">
            <div 
              class="select-selected admin-input" 
              @click="!isViewMode && (labelDropdownOpen = !labelDropdownOpen)"
              :style="selectedLabel ? { backgroundColor: selectedLabel.colorCode, color: selectedLabel.textColor, borderColor: selectedLabel.borderColor || 'transparent' } : {}"
              :class="{ 'disabled': isViewMode }"
            >
              {{ selectedLabel ? selectedLabel.name : '(Chọn nhãn nếu có)' }}
            </div>
            <div class="select-items" v-if="labelDropdownOpen">
              <div class="select-item" @click="selectLabel(null)">
                (Không chọn nhãn)
              </div>
              <div 
                v-for="label in labels" 
                :key="label.id" 
                class="select-item"
                :style="{ backgroundColor: label.colorCode, color: label.textColor, borderColor: label.borderColor || 'transparent' }"
                @click="selectLabel(label)"
              >
                {{ label.name }}
              </div>
            </div>
          </div>

          <div style="flex: 1;">
            <input 
              v-model="form.title" 
              class="admin-input-large" 
              placeholder="Nhập tiêu đề..." 
              required
              :disabled="isViewMode"
              style="margin-bottom: 0;"
            >
          </div>
        </div>

        <div class="form-row" style="display: flex; gap: 2rem; margin-bottom: 1.5rem;">
          <div class="form-group" style="flex: 1;">
            <label>Nhóm chuyên mục:</label>
            <select 
              v-model="selectedGroupId" 
              @change="form.categoryId = ''"
              class="admin-input" 
              required
              :disabled="isViewMode"
            >
              <option value="">-- Chọn nhóm chuyên mục --</option>
              <option v-for="group in categoryGroups" :key="group.id" :value="group.id">{{ group.name }}</option>
            </select>
          </div>
          <div class="form-group" style="flex: 1;">
            <label>Chuyên mục:</label>
            <select 
              v-model="form.categoryId" 
              class="admin-input" 
              required
              :disabled="isViewMode || !selectedGroupId"
            >
              <option value="">-- Chọn chuyên mục --</option>
              <option v-for="cat in filteredCategories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>
          <div class="form-group" style="padding-top: 2rem;">
            <label style="display: flex; align-items: center; gap: 10px; cursor: pointer;">
              <input 
                type="checkbox" 
                v-model="form.pinned"
                :disabled="isViewMode"
              > Ghim bài viết lên đầu
            </label>
          </div>
        </div>

        <div class="form-group">
          <label>Nội dung bài viết:</label>
          <div class="editor-wrapper" :class="{ 'disabled-editor': isViewMode }">
            <div class="post-type-tabs" :class="{ 'disabled-tabs': isViewMode }">
              <button
                :class="['tab-btn', postType === 'discussion' ? 'active' : '']"
                @click="!isViewMode && (postType = 'discussion')"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
                Thảo luận
              </button>
              <button
                :class="['tab-btn', postType === 'poll' ? 'active' : '']"
                @click="!isViewMode && (postType = 'poll')"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
                Bình chọn
              </button>
            </div>

            <div class="editor-inner">
              <CustomEditor 
                v-if="!isViewMode"
                ref="editor"
                v-model="form.content" 
                @image-uploaded="handleImageUploaded"
              />
              
              <!-- Khối xem trước đính kèm chân bài viết (chỉ hiển thị ở chế độ Tạo mới/Chỉnh sửa) -->
              <div v-if="!isViewMode && attachedImages && attachedImages.length > 0" class="attachment-block" style="margin: 1.5rem 0; border-top: 1px dashed #ddd; padding-top: 1.5rem;">
                <div class="attachment-label" style="font-weight: bold; color: #1a507a; margin-bottom: 1rem; font-size: 0.95rem;">Đính kèm</div>
                <div class="attachment-list" style="display: flex; flex-wrap: wrap; gap: 15px;">
                  <img v-for="(img, idx) in attachedImages" :key="idx" :src="img.url" :alt="img.name" style="width: 200px; height: 200px; object-fit: cover; border: 1px solid #ddd; border-radius: 4px; cursor: zoom-in;" />
                </div>
              </div>

              <div v-if="isViewMode" class="ck-content readonly-content" style="margin-top: 1rem;">
                <div v-html="form.content"></div>
              </div>

              <!-- Field Up Ảnh luôn hiển thị nhưng disabled ở chế độ Xem -->
              <ImageUploaderPanel 
                ref="uploaderPanel" 
                v-model:images="attachedImages"
                @insert-images="handleInsertImages" 
                :disabled="isViewMode" 
              />
            </div>

            <!-- Poll Form -->
            <div v-if="!isViewMode && postType === 'poll'" style="margin-top: 0;">
              <PollForm v-model="form.poll" />
            </div>
            <div v-if="isViewMode && form.poll" style="margin-top: 1.5rem;">
              <PollForm :modelValue="form.poll" disabled />
            </div>
          </div>
        </div>

        <div class="form-actions" style="margin-top: 2rem; display: flex; gap: 1rem; justify-content: flex-end;">
          <button @click="$router.push({ name: 'AdminThreads' })" class="btn-cancel">
            {{ isViewMode ? 'Quay lại' : 'Hủy bỏ' }}
          </button>
          <button v-if="!isViewMode" @click="handlePost" class="btn-save">
            {{ isEditMode ? 'Cập nhật bài viết' : 'Đăng bài ngay' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import AdminService from '@/apps/Admin/services/admin.service'
import { alertSuccess, alertError } from '@/shared/utils/swal'
import labelService from '@/apps/Forum/services/label.service'
import threadService from '@/apps/Forum/services/thread.service'
import CustomEditor from '@/shared/components/CustomEditor.vue'
import ImageUploaderPanel from '@/shared/components/ImageUploaderPanel.vue'
import PollForm from '@/shared/components/PollForm.vue'
import Loading from '@/shared/components/Loading.vue'

export default {
  name: 'AdminCreateThread',
  components: {
    CustomEditor,
    ImageUploaderPanel,
    PollForm,
    Loading
  },
  data() {
    return {
      categories: [],
      categoryGroups: [],
      labels: [],
      selectedLabel: null,
      labelDropdownOpen: false,
      selectedGroupId: '',
      postType: 'discussion',
      attachedImages: [],
      isSubmitting: false,
      isPageLoading: false,
      form: { title: '', content: '', categoryId: '', pinned: false, poll: null, labelId: null, attachedImages: '' }
    }
  },

  computed: {
    isEditMode() {
      return this.$route.path.includes('/threads/edit/')
    },
    isViewMode() {
      return this.$route.path.includes('/threads/view/')
    },
    threadId() {
      return this.$route.params.id
    },
    pageTitle() {
      if (this.isViewMode) return 'XEM CHI TIẾT BÀI VIẾT'
      if (this.isEditMode) return 'CHỈNH SỬA BÀI VIẾT'
      return 'TẠO BÀI VIẾT MỚI'
    },
    filteredCategories() {
      if (!this.selectedGroupId) return [];
      const rawCategories = this.categories.filter(c => c.categoryGroupId == this.selectedGroupId);
      return this.formatCategoriesHierarchy(rawCategories);
    }
  },
  async mounted() {
    this.isPageLoading = true
    try {
      await Promise.all([
        this.fetchCategoryGroups(),
        this.fetchCategories(),
        this.fetchLabels()
      ])
      if (this.threadId) {
        await this.fetchThread()
      }
    } catch (error) {
      console.error('Error loading admin page details:', error)
    } finally {
      this.isPageLoading = false
    }
    document.addEventListener('click', this.handleClickOutside)
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleClickOutside)
  },
  methods: {
    handleClickOutside(e) {
      if (!this.$el.querySelector('.custom-select')?.contains(e.target)) {
        this.labelDropdownOpen = false
      }
    },
    async fetchLabels() {
      try {
        const response = await labelService.getAll()
        this.labels = response.data
      } catch (error) {
        console.error('Error fetching labels:', error)
      }
    },
    selectLabel(label) {
      this.selectedLabel = label
      this.form.labelId = label ? label.id : null
      this.labelDropdownOpen = false
    },
    async fetchCategoryGroups() {
      try {
        const response = await AdminService.getCategoryGroups()
        this.categoryGroups = response.data
      } catch (error) {
        console.error('Error fetching groups:', error)
      }
    },
    async fetchCategories() {
      try {
        const response = await AdminService.getCategories()
        this.categories = response.data
      } catch (error) {
        console.error('Error fetching categories:', error)
      }
    },
    formatCategoriesHierarchy(flatCategories) {
      if (!flatCategories || flatCategories.length === 0) return [];
      
      const categoryMap = {};
      const roots = [];
      
      flatCategories.forEach(cat => {
        categoryMap[cat.id] = { ...cat, children: [] };
      });
      
      flatCategories.forEach(cat => {
        if (cat.parentCategoryId && categoryMap[cat.parentCategoryId]) {
          categoryMap[cat.parentCategoryId].children.push(categoryMap[cat.id]);
        } else {
          roots.push(categoryMap[cat.id]);
        }
      });
      
      const result = [];
      const flatten = (nodes, prefix = '') => {
        nodes.forEach(node => {
          result.push({
            ...node,
            name: prefix + node.name
          });
          if (node.children && node.children.length > 0) {
            flatten(node.children, prefix + '\u00A0\u00A0\u00A0\u00A0└─ ');
          }
        });
      };
      
      flatten(roots);
      return result;
    },
    async fetchThread() {
      try {
        const response = await threadService.getById(this.threadId)
        const thread = response.data
        let content = thread.content || ''
        if (!this.isViewMode) {
          content = this.stripAttachments(content)
        }
        
        if (this.isViewMode) {
          content = content.replace(/<oembed\s+url="([^"]+)"><\/oembed>/gi, (match, url) => {
            const youtubeRegex = /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?\/\s]{11})/i
            const ytMatch = url.match(youtubeRegex)
            if (ytMatch && ytMatch[1]) {
              return `<iframe width="100%" height="450" src="https://www.youtube.com/embed/${ytMatch[1]}" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>`
            }
            
            if (url.match(/\.(mp4|webm|ogg|avi|mov)(\?.*)?$/i)) {
               let fixedUrl = url;
               const uploadsIndex = fixedUrl.indexOf('/uploads/');
               if (uploadsIndex !== -1) {
                   fixedUrl = fixedUrl.substring(uploadsIndex);
               }
               return `<figure class="media"><video controls style="width: 100%; max-height: 500px; object-fit: contain; background: #000;" src="${fixedUrl}"></video></figure>`
            }

            return `<a href="${url}" target="_blank">${url}</a>`
          })
        }

        this.form = {
          title: thread.title,
          content: content,
          categoryId: thread.category ? thread.category.id : '',
          pinned: thread.pinned || false,
          poll: thread.poll || null,
          labelId: thread.label ? thread.label.id : null,
          attachedImages: thread.attachedImages || ''
        }
        
        if (thread.attachedImages) {
          try {
            const imgs = JSON.parse(thread.attachedImages)
            this.attachedImages = imgs
          } catch (e) {
            console.error('Error parsing attached images:', e)
          }
        }
        
        if (thread.label) {
          this.selectedLabel = thread.label
        }
        
        if (thread.poll) {
          this.postType = 'poll'
        } else {
          this.postType = 'discussion'
        }
        
        if (thread.category && thread.category.categoryGroupId) {
          this.selectedGroupId = thread.category.categoryGroupId
        }
      } catch (error) {
        alertError('Không thể tải thông tin bài viết')
      }
    },
    async handlePost() {
      if (!this.form.title || !this.form.content || !this.form.categoryId) {
        alertError('Vui lòng điền đầy đủ thông tin')
        return
      }
      try {
        this.isSubmitting = true
        // Loại bỏ các khối đính kèm chuẩn hóa từ editor trước khi nối khối HTML chuẩn cho cơ sở dữ liệu
        let cleanContent = this.form.content || ''
        const markers = [
          /<div[^>]*class="attachment-block"[^>]*>/i,
          /<p><strong>Đính kèm<\/strong><\/p>/i
        ]
        let matchIndex = -1
        for (const marker of markers) {
          const match = cleanContent.match(marker)
          if (match) {
            matchIndex = match.index
            break
          }
        }
        if (matchIndex !== -1) {
          cleanContent = cleanContent.substring(0, matchIndex).trim()
        }

        let finalContent = cleanContent
        const attachedImages = this.attachedImages

        // 3. Nếu có ảnh đính kèm, tạo HTML block và nối vào cuối content
        if (attachedImages && attachedImages.length > 0) {
          let attachedHtml = `<div id="attachment-section" class="attachment-block" style="margin-top: 2rem; border-top: 1px dashed #ddd; padding-top: 1.5rem;">`
          attachedHtml += `<div class="attachment-label" style="font-weight: bold; color: #1a507a; margin-bottom: 1rem; font-size: 0.95rem;">Đính kèm</div>`
          attachedHtml += `<div class="attachment-list" style="display: flex; flex-wrap: wrap; gap: 15px;">`
          
          attachedImages.forEach(img => {
            attachedHtml += `<img src="${img.url}" alt="${img.name}" style="width: 200px; height: 200px; object-fit: cover; border: 1px solid #ddd; border-radius: 4px; cursor: pointer; display: inline-block; margin: 5px;" />`
          })
          
          attachedHtml += `</div><!-- END ATTACHMENT SECTION --></div>`
          finalContent = finalContent.trim() + '\n' + attachedHtml
        }

        const payload = {
          title: this.form.title,
          content: finalContent,
          category: { id: this.form.categoryId },
          label: this.form.labelId ? { id: this.form.labelId } : null,
          pinned: this.form.pinned,
          attachedImages: JSON.stringify(attachedImages)
        }
        
        if (this.postType === 'poll' && this.form.poll) {
          payload.poll = this.form.poll
        }
        
        if (this.isEditMode) {
          await threadService.update(this.threadId, payload)
          await alertSuccess('Cập nhật bài viết thành công')
        } else {
          await threadService.create(payload)
          await alertSuccess('Đăng bài viết thành công')
        }
        
        this.$router.push({ name: 'AdminThreads' })
      } catch (error) {
        alertError(this.isEditMode ? 'Lỗi khi cập nhật bài viết' : 'Lỗi khi đăng bài')
      } finally {
        this.isSubmitting = false
      }
    },
    handleInsertImages(urls, type) {
      if (this.$refs.editor && this.$refs.editor.insertImages) {
        this.$refs.editor.insertImages(urls, type)
      }
    },
    handleImageUploaded(image) {
      this.attachedImages.push(image)
    },
    stripAttachments(content) {
      if (!content) return ''
      let cleanContent = content
      const markers = [
        /<div[^>]*class="attachment-block"[^>]*>/i,
        /<p><strong>Đính kèm<\/strong><\/p>/i
      ]
      let matchIndex = -1
      for (const marker of markers) {
        const match = cleanContent.match(marker)
        if (match) {
          matchIndex = match.index
          break
        }
      }
      if (matchIndex !== -1) {
        cleanContent = cleanContent.substring(0, matchIndex).trim()
      }
      return cleanContent
    },
    openImage(url) {
      window.open(url, '_blank')
    }
  }
}
</script>

<style scoped>
.admin-input-large { width: 100%; padding: 1rem; font-size: 1.2rem; border: 1px solid #ddd; border-radius: 4px; }
.admin-input { width: 100%; padding: 0.75rem; border: 1px solid #ddd; border-radius: 4px; }
.btn-save { background-color: #27ae60; color: white; border: none; padding: 12px 30px; border-radius: 4px; font-weight: bold; cursor: pointer; }
.btn-cancel { background-color: #95a5a6; color: white; border: none; padding: 12px 20px; border-radius: 4px; cursor: pointer; }
.form-group label { font-weight: bold; display: block; margin-bottom: 0.5rem; }

.post-type-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid #ddd;
  margin-bottom: -1px;
  position: relative;
  z-index: 1;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border: 1px solid #ddd;
  border-bottom: none;
  background: #f5f5f5;
  color: #666;
  cursor: pointer;
  font-size: 13px;
  border-radius: 4px 4px 0 0;
  transition: all 0.2s;
  margin-right: 4px;
}

.tab-btn.active {
  background: white;
  color: #3498db;
  border-color: #ddd;
  font-weight: 600;
  border-bottom-color: white;
}

.tab-btn:hover:not(.active) {
  background: #ebebeb;
}

.editor-wrapper {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: white;
}

.disabled-tabs {
  pointer-events: none;
  opacity: 0.8;
}
.disabled-tabs .tab-btn:not(.active) {
  opacity: 0.5;
}

.editor-inner {
  border-top: 1px solid #ddd;
}

:deep(.ck-editor__editable) {
  min-height: 400px;
  font-size: 16px;
  line-height: 1.6;
}

.readonly-content {
  border: 1px solid #ccc;
  padding: 1rem;
  border-radius: 4px;
  background: #f9f9f9;
  min-height: 400px;
}

:deep(.ck-content table) {
  width: 100%;
  border-collapse: collapse;
}

:deep(.ck-content td), :deep(.ck-content th) {
  border: 1px solid #bfbfbf;
  padding: 0.4em;
}

:deep(.ck-content .image-inline) {
  margin: 5px !important;
  display: inline-block !important;
}

:deep(.ck-content .image-inline img) {
  max-width: 100%;
  height: auto;
  object-fit: contain;
}

.custom-select {
  font-size: 14px;
}
.select-selected {
  background-color: #f9f9f9;
  border: 1px solid #ddd;
  padding: 10px 15px;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  font-weight: bold;
  color: #555;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 42px;
  border: 1px solid transparent;
}
.select-selected.disabled {
  cursor: not-allowed;
  opacity: 0.7;
}
.select-selected:after {
  content: "▼";
  position: absolute;
  right: 10px;
  top: 15px;
  font-size: 10px;
}
.select-items {
  position: absolute;
  background-color: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 99;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  max-height: 200px;
  overflow-y: auto;
  margin-top: 5px;
  padding: 5px;
}
.select-item {
  padding: 8px 10px;
  cursor: pointer;
  border-radius: 3px;
  margin-bottom: 2px;
  font-weight: 500;
  color: #333;
  border: 1px solid transparent;
}
.select-item:hover {
  filter: brightness(0.9);
}

.attached-section {
  margin-top: 0;
  border-top: 1px dashed #ddd;
  padding: 1.5rem;
}

.attached-label {
  font-weight: bold;
  color: #1a507a;
  margin-bottom: 1rem;
  font-size: 0.9rem;
}

.attached-list {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.attached-item img {
  width: 200px;
  height: 200px;
  object-fit: cover;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: zoom-in;
  transition: transform 0.2s;
}

.attached-item img:hover {
  transform: scale(1.02);
}

.edit-mode-preview {
  margin-top: 1rem;
  padding: 1rem;
  background: #fdfdfd;
  border: 1px solid #eee;
  border-radius: 4px;
}
</style>
