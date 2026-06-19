<template>
  <div>
    <Loading :visible="isUploadLoading || isSubmitting || isPageLoading" />
    
    <main class="container" style="padding-top: 2rem;">
      <Breadcrumb :items="breadcrumbItems" />

      <div class="card">
        <div class="card-header">ĐĂNG BÀI</div>
        <div class="post-form">
          <div class="title-row">
            <div class="custom-select label-select">
              <div 
                class="select-selected" 
                @click="labelDropdownOpen = !labelDropdownOpen"
                :style="selectedLabel ? { backgroundColor: selectedLabel.colorCode, color: selectedLabel.textColor, borderColor: selectedLabel.borderColor || 'transparent' } : {}"
              >
                {{ selectedLabel ? selectedLabel.name : '(Chọn nhãn nếu có)' }}
              </div>
              <div class="select-items" v-if="labelDropdownOpen">
                <div 
                  class="select-item" 
                  @click="selectLabel(null)"
                >
                  (Không chọn nhãn)
                </div>
                <div 
                  v-for="label in filteredLabels" 
                  :key="label.id" 
                  class="select-item"
                  :style="{ backgroundColor: label.colorCode, color: label.textColor, borderColor: label.borderColor || 'transparent' }"
                  @click="selectLabel(label)"
                >
                  {{ label.name }}
                </div>
              </div>
            </div>
            
            <input v-model="form.title" class="title-input title-field" placeholder="Nhập tiêu đề bài viết..." required>
          </div>

          <div class="form-group" style="margin-bottom: 1.5rem;">
            <label style="display: block; margin-bottom: 0.5rem; font-weight: bold; color: #1a507a;">Chuyên mục:</label>
            <input 
              :value="category ? category.name : 'Đang tải...'" 
              class="title-input" 
              style="background-color: #f5f5f5; color: #888; font-size: 1rem; cursor: not-allowed;" 
              disabled
            >
          </div>

          <div v-if="isAdmin" style="display: flex; gap: 3rem; margin-bottom: 1.5rem; flex-wrap: wrap;">
            <div class="form-group" style="margin-bottom: 0;">
              <label style="display: block; margin-bottom: 0.5rem; font-weight: bold; color: #1a507a;">Phạm vi bài đăng:</label>
              <div style="display: flex; gap: 1.5rem; align-items: center; padding: 0.5rem 0;">
                <label style="display: flex; align-items: center; gap: 8px; cursor: pointer; font-weight: normal; color: #333;">
                  <input 
                    type="radio" 
                    v-model="form.scope" 
                    :value="THREAD_SCOPES.PUBLIC" 
                    style="cursor: pointer; width: 18px; height: 18px;"
                  >
                  Công khai
                </label>
                <label style="display: flex; align-items: center; gap: 8px; cursor: pointer; font-weight: normal; color: #333;">
                  <input 
                    type="radio" 
                    v-model="form.scope" 
                    :value="THREAD_SCOPES.INTERNAL" 
                    style="cursor: pointer; width: 18px; height: 18px;"
                  >
                  Nội bộ
                </label>
              </div>
            </div>

            <div class="form-group" style="margin-bottom: 0;">
              <label style="display: block; margin-bottom: 0.5rem; font-weight: bold; color: #1a507a;">Khóa bình luận:</label>
              <div style="display: flex; gap: 1.5rem; align-items: center; padding: 0.5rem 0;">
                <label style="display: flex; align-items: center; gap: 8px; cursor: pointer; font-weight: normal; color: #333;">
                  <input 
                    type="radio" 
                    v-model="form.locked" 
                    :value="true" 
                    style="cursor: pointer; width: 18px; height: 18px;"
                  >
                  Có
                </label>
                <label style="display: flex; align-items: center; gap: 8px; cursor: pointer; font-weight: normal; color: #333;">
                  <input 
                    type="radio" 
                    v-model="form.locked" 
                    :value="false" 
                    style="cursor: pointer; width: 18px; height: 18px;"
                  >
                  Không
                </label>
              </div>
            </div>
          </div>

          <!-- Tabs Thảo luận / Bình chọn -->
          <div class="editor-block">
            <div class="post-type-tabs">
              <button
                :class="['tab-btn', postType === 'discussion' ? 'active' : '']"
                @click="postType = 'discussion'"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
                Thảo luận
              </button>
              <button
                :class="['tab-btn', postType === 'poll' ? 'active' : '']"
                @click="postType = 'poll'"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
                Bình chọn
              </button>
            </div>

            <div class="editor-container">
              <CustomEditor
                ref="editor"
                v-model="form.content"
                @image-uploaded="handleImageUploaded"
                @upload-loading-start="isUploadLoading = true"
                @upload-loading-end="isUploadLoading = false"
              />
              
              <!-- Khối xem trước đính kèm chân bài viết -->
              <div v-if="attachedImages && attachedImages.length > 0" class="attachment-block" style="margin: 1.5rem 0; border-top: 1px dashed #ddd; padding-top: 1.5rem;">
                <div class="attachment-label" style="font-weight: bold; color: #1a507a; margin-bottom: 1rem; font-size: 0.95rem;">Đính kèm</div>
                <div class="attachment-list" style="display: flex; flex-wrap: wrap; gap: 15px;">
                  <img v-for="(img, idx) in attachedImages" :key="idx" :src="img.url" :alt="img.name" style="width: 200px; height: 200px; object-fit: cover; border: 1px solid #ddd; border-radius: 4px; cursor: zoom-in;" />
                </div>
              </div>

              <ImageUploaderPanel ref="uploaderPanel" v-model:images="attachedImages" @insert-images="handleInsertImages" />
            </div>

            <!-- Poll Form -->
            <div v-if="postType === 'poll'">
              <PollForm v-model="form.poll" />
            </div>
          </div>

          <div class="form-actions">
            <button @click="handlePost" class="btn-post">Đăng bài</button>
            <button @click="$router.push({ name: 'Home' })" class="btn-cancel">Hủy bỏ</button>
          </div>
        </div>
      </div>

      <Breadcrumb :items="breadcrumbItems" />
    </main>
  </div>
</template>

<script>
import threadService from '@/apps/Forum/services/thread.service'
import categoryService from '@/apps/Forum/services/category.service'
import labelService from '@/apps/Forum/services/label.service'
import { alertSuccess, alertError } from '@/shared/utils/swal'
import { isAdminOrSuperAdmin, THREAD_SCOPES } from '@/shared/utils/utils'
import CustomEditor from '@/shared/components/CustomEditor.vue'
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import ImageUploaderPanel from '@/shared/components/ImageUploaderPanel.vue'
import PollForm from '@/shared/components/PollForm.vue'
import Loading from '@/shared/components/Loading.vue'

export default {
  name: 'CreateThread',
  components: {
    CustomEditor,
    Breadcrumb,
    ImageUploaderPanel,
    PollForm,
    Loading
  },
  data() {
    return {
      THREAD_SCOPES,
      catId: this.$route.query.catId,
      category: null,
      categoryGroup: null,
      allCategories: [],
      labels: [],
      selectedLabel: null,
      labelDropdownOpen: false,
      postType: 'discussion',
      attachedImages: [],
      isUploadLoading: false,
      isSubmitting: false,
      isPageLoading: false,
      form: { title: '', content: '', categoryId: '', poll: null, labelId: null, scope: THREAD_SCOPES.PUBLIC, locked: false }
    }
  },
  computed: {
    breadcrumbItems() {
      const items = [{ title: 'Trang chủ', to: { name: 'Home' } }]
      
      if (this.categoryGroup) {
        items.push({ 
          title: this.categoryGroup.name, 
          to: { name: 'Home', hash: `#group-${this.categoryGroup.id}` } 
        })
      }

      if (this.category && this.allCategories && this.allCategories.length > 0) {
        let parents = [];
        let currentParentId = this.category.parentCategoryId;
        while (currentParentId) {
          const parent = this.allCategories.find(c => c.id === currentParentId);
          if (parent) {
            parents.unshift(parent);
            currentParentId = parent.parentCategoryId;
          } else {
            break;
          }
        }
        parents.forEach(p => {
          items.push({
            title: p.name,
            to: { name: 'CategoryDetail', params: { id: p.id } }
          })
        });

        items.push({ 
          title: this.category.name, 
          to: { name: 'CategoryDetail', params: { id: this.category.id } } 
        })
      }
      
      items.push({ title: 'Đăng bài mới' })
      return items
    },
    filteredLabels() {
      return this.labels.filter(l => !l.adminOnly)
    },
    isAdmin() {
      return isAdminOrSuperAdmin()
    }
  },

  async mounted() {
    this.isPageLoading = true
    try {
      await Promise.all([
        this.fetchCategory(),
        this.fetchLabels()
      ])
    } catch (error) {
      console.error('Error loading page details:', error)
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
    async fetchCategory() {
      if (!this.catId) return
      try {
        const [catRes, groupRes] = await Promise.all([
          categoryService.getAll(),
          categoryService.getGroups()
        ])
        
        const categories = catRes.data
        this.allCategories = categories
        this.category = categories.find(c => c.id == this.catId)

        if (this.category && this.category.categoryGroupId) {
          this.categoryGroup = groupRes.data.find(g => g.id === this.category.categoryGroupId)
        }
      } catch (error) {
        console.error('Error fetching category:', error)
      }
    },
    async handlePost() {
      if (!this.form.title || !this.form.content) {
        alertError('Vui lòng nhập đầy đủ tiêu đề và nội dung')
        return
      }
      try {
        this.isSubmitting = true
        let cleanContent = this.form.content || ''
        const markers = [
          /<!--/i, // Safe fallback or marker if comments exist
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
          category: { id: this.catId },
          label: this.form.labelId ? { id: this.form.labelId } : null,
          attachedImages: JSON.stringify(attachedImages),
          scope: this.form.scope,
          locked: this.form.locked
        }
        if (this.postType === 'poll' && this.form.poll) {
          payload.poll = this.form.poll
        }
        
        const res = await threadService.create(payload)
        const newThread = res.data
        
        alertSuccess('Đăng bài viết thành công')
        this.$router.push({ name: 'ThreadDetail', params: { id: newThread.id } })
      } catch (error) {
        alertError('Lỗi khi đăng bài')
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

  }
}
</script>

<style scoped>
.create-thread-page { padding-bottom: 5rem; }
.post-form {
  padding: 2rem;
}
.title-row {
  margin-bottom: 1.5rem;
  display: flex;
  gap: 1rem;
  align-items: center;
}
.label-select {
  position: relative;
  width: 200px;
  flex-shrink: 0;
}
.title-input {
  width: 100%;
  box-sizing: border-box;
  padding: 1rem;
  font-size: 1.2rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-weight: bold;
}
.title-field {
  flex: 1;
}
.form-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}
.btn-post { background-color: #3498db; color: white; border: none; padding: 12px 35px; border-radius: 4px; font-weight: bold; cursor: pointer; transition: background 0.3s; }
.btn-post:hover { background-color: #2980b9; }
.btn-cancel { background-color: #ecf0f1; color: #333; border: none; padding: 12px 25px; border-radius: 4px; cursor: pointer; }

@media (max-width: 768px) {
  .post-form {
    padding: 1rem;
  }
  .title-row {
    flex-direction: column;
    align-items: stretch;
  }
  .label-select {
    width: 100%;
  }
}

/* Custom Select for Labels */
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
  border: 1px solid transparent;
}
.select-selected:after {
  content: "▼";
  position: absolute;
  right: 10px;
  top: 12px;
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

.post-type-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 0;
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

.editor-block {
  margin-bottom: 1.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
}

.editor-container {
  border-top: 1px solid #ddd;
}

:deep(.ck-editor__editable) {
  min-height: 400px;
  font-size: 16px;
}
</style>
