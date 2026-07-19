<template>
  <div>
    <Loading :visible="loadingUsers || submitting" />
    
    <main class="container" style="padding-top: 2rem;">
      <Breadcrumb :items="breadcrumbItems" />

      <div class="card">
        <div class="card-header">Bắt đầu đối thoại</div>
        
        <div class="post-form" style="padding: 16px;">
          <!-- Người nhận -->
          <div class="form-group" style="margin-bottom: 1.5rem; position: relative;">
            <label style="display: block; margin-bottom: 0.5rem; font-weight: bold; color: #1a507a;">Người nhận:</label>
            <div class="recipient-input-container" @click="focusInput">
              <div v-for="user in selectedRecipients" :key="user.username" class="recipient-tag">
                <span v-if="!isReadOnly" class="remove-tag" @click.stop="removeRecipient(user)">&times;</span>
                <span>{{ user.displayName || user.username }}</span>
              </div>
              <div v-if="!isReadOnly" class="search-input-wrapper">
                <span 
                  v-if="selectedRecipients.length === 0 && !searchQuery" 
                  class="recipient-placeholder"
                >
                  Nhập tên đăng nhập hoặc tên hiển thị để tìm...
                </span>
                <input 
                  ref="searchInput"
                  type="text" 
                  class="recipient-search-input" 
                  :value="searchQuery" 
                  @focus="showDropdown = true"
                  @input="handleSearchInput"
                  @keydown.delete="handleBackspace"
                />
              </div>
            </div>
            
            <!-- Dropdown autocomplete search results -->
            <div v-if="!isReadOnly && showDropdown && searchResults.length > 0" class="autocomplete-dropdown">
              <div 
                v-for="user in searchResults" 
                :key="user.id" 
                class="autocomplete-item" 
                @click="selectRecipient(user)"
              >
                <span class="user-avatar-circle" :style="{ backgroundColor: getAvatarColor(user) }">
                  {{ (user.displayName || user.username || '?').charAt(0).toUpperCase() }}
                </span>
                <span class="user-name-text">
                  {{ user.displayName || user.username }} ({{ user.username }})
                </span>
              </div>
            </div>
            <!-- <small style="color: #888; font-size: 0.85rem; margin-top: 4px; display: block;">Dãn cách tên bằng dấu phẩy(,).</small> -->
          </div>

          <!-- Tiêu đề -->
          <div class="form-group" style="margin-bottom: 1.5rem;">
            <input 
              v-model="form.title" 
              class="title-input" 
              style="width: 100%;" 
              placeholder="Tiêu đề..." 
              required
            />
          </div>

          <!-- Nội dung đối thoại -->
          <div class="editor-block" style="margin-bottom: 1.5rem; border: 1px solid #ddd; border-radius: 4px; overflow: hidden;">
            <div class="editor-container">
              <CustomEditor ref="editor" v-model="form.content" :allowedUsers="selectedRecipients" />
            </div>
          </div>

          <!-- Tùy chọn đối thoại -->
          <div class="options-row">
            <div class="options-label"></div>
            <div class="options-inputs">
              <label>
                <input 
                  type="checkbox" 
                  v-model="form.allowInvite" 
                />
                <span>Cho phép mọi người trong đối thoại mời người khác</span>
              </label>
              <label>
                <input 
                  type="checkbox" 
                  v-model="form.locked" 
                />
                <span>Khóa đối thoại (không cho phép trả lời)</span>
              </label>
            </div>
          </div>

          <!-- Actions -->
          <div class="form-actions" style="display: flex; justify-content: center; gap: 10px; border-top: 1px solid #eee; padding-top: 1.5rem;">
            <button @click="handleSubmit" class="btn-post" :disabled="submitting">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 5px; vertical-align: middle;"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
              Bắt đầu đối thoại
            </button>

            <button @click="$router.push({ name: 'Home' })" class="btn-cancel">Hủy bỏ</button>
          </div>
        </div>
      </div>

      <Breadcrumb :items="breadcrumbItems" />
    </main>
  </div>
</template>

<script>
import userService from '@/apps/Forum/services/user.service'
import conversationService from '@/apps/Forum/services/conversation.service'
import { alertSuccess, alertError } from '@/shared/utils/swal'
import { getImeValue } from '@/shared/utils/utils'
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import CustomEditor from '@/shared/components/CustomEditor.vue'
import Loading from '@/shared/components/Loading.vue'

export default {
  name: 'AddConversation',
  components: {
    Breadcrumb,
    CustomEditor,
    Loading
  },
  data() {
    const toParam = this.$route.query.to || ''
    return {
      recipientName: toParam,
      selectedRecipients: toParam ? [{ username: toParam, displayName: toParam }] : [],
      isReadOnly: !!toParam,
      searchQuery: '',
      searchResults: [],
      showDropdown: false,
      searchTimeout: null,
      submitting: false,
      loadingUsers: false,
      form: {
        title: '',
        content: '',
        allowInvite: false,
        locked: false
      }
    }
  },
  computed: {
    breadcrumbItems() {
      return [
        { title: 'Trang chủ', to: { name: 'Home' } },
        { title: 'Đối thoại' }
      ]
    }
  },
  async mounted() {
    document.addEventListener('click', this.handleClickOutside)
    if (this.recipientName) {
      try {
        const response = await userService.getByName(this.recipientName)
        if (response.data) {
          const user = response.data
          this.selectedRecipients = [{
            id: user.id,
            username: user.username,
            displayName: user.displayName || user.username,
            avatar: user.avatar
          }]
        }
      } catch (error) {
        console.error('Lỗi khi lấy thông tin người nhận:', error)
      }
    }
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleClickOutside)
  },
  methods: {
    handleSearchInput(e) {
      this.searchQuery = getImeValue(e)
      if (!this.searchQuery || !this.searchQuery.trim()) {
        this.searchResults = []
        return
      }
      this.showDropdown = true
      clearTimeout(this.searchTimeout)
      this.searchTimeout = setTimeout(() => {
        this.fetchUsers()
      }, 300)
    },
    async fetchUsers() {
      this.loadingUsers = true
      try {
        const response = await userService.search({
          keyword: this.searchQuery,
          page: 0,
          size: 10
        })
        if (response.data) {
          const content = response.data.content || []
          this.searchResults = content.filter(user => 
            !this.selectedRecipients.some(selected => selected.username === user.username)
          )
        }
      } catch (error) {
        console.error('Lỗi khi tìm kiếm người nhận:', error)
      } finally {
        this.loadingUsers = false
      }
    },
    selectRecipient(user) {
      if (!this.selectedRecipients.some(r => r.username === user.username)) {
        this.selectedRecipients.push({
          id: user.id,
          username: user.username,
          displayName: user.displayName || user.username,
          avatar: user.avatar
        })
      }
      this.searchQuery = ''
      this.searchResults = []
      this.showDropdown = false
      this.$nextTick(() => {
        this.focusInput()
      })
    },
    removeRecipient(user) {
      this.selectedRecipients = this.selectedRecipients.filter(r => r.username !== user.username)
    },
    handleBackspace() {
      if (!this.searchQuery && this.selectedRecipients.length > 0) {
        this.selectedRecipients.pop()
      }
    },
    focusInput() {
      if (this.$refs.searchInput) {
        this.$refs.searchInput.focus()
      }
    },
    handleClickOutside(e) {
      const container = this.$el.querySelector('.form-group')
      if (container && !container.contains(e.target)) {
        this.showDropdown = false
      }
    },
    getAvatarColor(user) {
      if (user.avatar && user.avatar.startsWith('#')) {
        return user.avatar
      }
      if (user.avatar && user.avatar.startsWith('hsl')) {
        return user.avatar
      }
      const name = user.displayName || user.username || '?'
      let hash = 0
      for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash)
      }
      const h = Math.abs(hash % 360)
      return `hsl(${h}, 60%, 50%)`
    },
    async handleSubmit() {
      if (this.selectedRecipients.length === 0) {
        alertError('Vui lòng chọn ít nhất một người nhận cuộc đối thoại')
        return
      }
      if (!this.form.title || !this.form.title.trim()) {
        alertError('Vui lòng nhập tiêu đề cuộc đối thoại')
        return
      }
      if (!this.form.content || !this.form.content.trim()) {
        alertError('Vui lòng nhập nội dung đối thoại')
        return
      }

      this.submitting = true
      try {
        const recipientDisplayNames = this.selectedRecipients.map(r => r.username)
        const payload = {
          recipientDisplayNames: recipientDisplayNames,
          title: this.form.title,
          content: this.form.content,
          allowInvite: this.form.allowInvite,
          locked: this.form.locked
        }

        const res = await conversationService.create(payload)
        this.submitting = false
        if (res.data) {
          await alertSuccess('Bắt đầu cuộc đối thoại thành công')
          const convo = res.data
          this.$router.push({ 
            name: 'ConversationDetail', 
            params: { id: convo.id },
            query: convo.firstMessageId ? { messageId: convo.firstMessageId } : {}
          })
        } else {
          alertError('Có lỗi xảy ra khi bắt đầu đối thoại')
        }
      } catch (error) {
        console.error(error)
        alertError(error.response?.data?.message || 'Lỗi hệ thống khi bắt đầu đối thoại')
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.add-conversation-page {
  padding-bottom: 5rem;
}

.recipient-input-container {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 6px 12px;
  background-color: #fff;
  min-height: 48px;
  cursor: text;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.recipient-input-container:focus-within {
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
}

.recipient-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  background-color: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  padding: 2px 8px;
  color: #495057;
  font-size: 0.9rem;
  font-weight: 500;
  user-select: none;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.remove-tag {
  cursor: pointer;
  color: #adb5bd;
  font-weight: bold;
  font-size: 1.1rem;
  line-height: 1;
  transition: color 0.15s;
}

.remove-tag:hover {
  color: #dc3545;
}

.search-input-wrapper {
  display: grid;
  flex: 1;
  min-width: 150px;
  position: relative;
}

.recipient-placeholder {
  grid-area: 1 / 1;
  color: #888;
  font-size: 0.95rem;
  pointer-events: none;
  align-self: center;
  line-height: 1.3;
  white-space: normal;
  word-break: break-word;
}

.recipient-search-input {
  grid-area: 1 / 1;
  border: none;
  background: transparent;
  width: 100%;
  outline: none;
  font-size: 0.95rem;
  color: #333;
  z-index: 2;
}

.autocomplete-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  z-index: 1000;
  max-height: 250px;
  overflow-y: auto;
  margin-top: 5px;
}

.autocomplete-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 15px;
  cursor: pointer;
  transition: background 0.2s;
}

.autocomplete-item:hover {
  background-color: #f5f7fa;
}

.user-avatar-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: white;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}

.user-name-text {
  font-size: 0.95rem;
  color: #333;
}

.title-input {
  padding: 12px 15px;
  font-size: 1.1rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  outline: none;
  font-weight: bold;
}

.title-input:focus {
  border-color: #3498db;
}

.btn-post {
  background-color: #3498db;
  color: white;
  border: none;
  padding: 12px 35px;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
  transition: background 0.3s;
  font-size: 0.95rem;
  display: inline-flex;
  align-items: center;
}

.btn-post:hover:not(:disabled) {
  background-color: #2980b9;
}

.btn-post:disabled {
  background-color: #bdc3c7;
  cursor: not-allowed;
}

.btn-cancel {
  background-color: #ecf0f1;
  color: #333;
  border: none;
  padding: 12px 25px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
  font-size: 0.95rem;
}

.btn-cancel:hover {
  background-color: #bdc3c7;
}

:deep(.ck-editor__editable) {
  min-height: 350px;
  font-size: 16px;
}

.options-row {
  display: flex;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 1.5rem;
  overflow: hidden;
}

.options-label {
  width: 200px;
  background-color: #f8f9fa;
  border-right: 1px solid #ddd;
  padding: 1.5rem;
}

.options-inputs {
  flex: 1;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: #fff;
}

.options-inputs label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-weight: normal;
  margin: 0;
  font-size: 0.95rem;
  color: #333;
}

.options-inputs input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
  margin: 0;
  flex-shrink: 0;
}

@media (max-width: 767px) {
  .btn-post, .btn-cancel {
    padding: 12px 10px !important;
  }
  .options-row {
    flex-direction: column;
  }
  .options-label {
    display: none;
  }
  .options-inputs {
    padding: 1rem;
  }
  .options-inputs label {
    align-items: flex-start;
    line-height: 1.4;
  }
  .options-inputs input[type="checkbox"] {
    margin-top: 3px;
  }
}
</style>
