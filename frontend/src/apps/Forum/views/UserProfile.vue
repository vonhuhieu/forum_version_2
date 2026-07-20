<template>
  <div>
    <Loading :visible="loading" />

    <main class="container" style="padding-top: 2rem;">
      <!-- Breadcrumb -->
      <Breadcrumb :items="breadcrumbItems" />

      <div class="account-layout">
        <!-- Cột chính: Content chính (Full-width) -->
        <div class="account-content">
          <!-- Thành phần 1: Profile Header & Details -->
          <div class="profile-header-card card" :class="{ 'no-banner': !userStats.profileBanner }">
            <!-- Vùng ảnh bìa (banner) - Chỉ hiển thị nếu có ảnh bìa -->
            <div 
              v-if="userStats.profileBanner"
              class="profile-banner-area" 
              :style="{ backgroundImage: `url(${userStats.profileBanner})` }"
            >
              <div class="banner-overlay-gradient"></div>
            </div>
            
            <!-- Thông tin tài khoản phía trên (nằm ngoài banner) -->
            <div 
              class="profile-info-upper no-pt-mobile" 
              :class="{ 'text-white': !!userStats.profileBanner, 'positioned-absolute': !!userStats.profileBanner }"
            >
              <div class="profile-avatar-wrapper" :class="{ 'clickable': isCurrentUser }" @click="isCurrentUser && openUploadModal('avatar')">
                <img v-if="isAvatarUrl(userStats.avatar)" :src="userStats.avatar" class="profile-avatar-img" />
                <div v-else class="profile-avatar-placeholder" :style="{ backgroundColor: userStats.avatar || '#1a507a' }">
                  {{ userInitial }}
                </div>
                <div v-if="isCurrentUser" class="avatar-edit-overlay">
                  <span>Sửa</span>
                </div>
              </div>

              <div class="profile-meta-details">
                <h1 class="profile-displayname">{{ userStats.displayName || userStats.username }}</h1>
                <div class="profile-title-tag">Yếu sinh lý</div>
                <div class="profile-time-row">
                  <span class="meta-item">Tham gia: {{ formatDate(userStats.createdAt) }}</span>
                </div>
                <div class="profile-time-row">
                  <span class="meta-item text-dimmed">Thấy lần gần nhất: {{ formatDate(userStats.lastActiveAt) }}</span>
                </div>
                <!-- Nút hành động nằm dưới meta -->
                <div class="banner-actions" :class="{ 'banner-actions-other': !isCurrentUser }">
                  <button class="btn-banner-action fs-9" @click="triggerReport">Báo cáo</button>
                  <template v-if="!isCurrentUser">
                    <button class="btn-banner-action fs-9" @click="handleFollow">Theo dõi</button>
                    <button class="btn-banner-action fs-9" @click="handleBlock">Chặn</button>
                    <button class="btn-banner-action fs-9" @click="startConversation">Bắt đầu đối thoại</button>
                  </template>
                  <button v-else class="btn-banner-action btn-banner-edit fs-9" @click="openUploadModal('banner')">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                      <circle cx="12" cy="13" r="4"></circle>
                    </svg>
                    Đổi ảnh bìa
                  </button>
                </div>
              </div>
            </div>

            <!-- Vùng thống kê phía dưới banner -->
            <div class="profile-stats-bar">
              <div class="stat-box">
                <span class="stat-label">Bài viết</span>
                <span class="stat-val">{{ userStats.postCount || 0 }}</span>
              </div>
              <div class="stat-box">
                <span class="stat-label">Điểm tương tác</span>
                <span class="stat-val">{{ userStats.interactionPoints || 0 }}</span>
              </div>
              <div class="stat-box">
                <span class="stat-label">Điểm thành tích</span>
                <span class="stat-val">{{ userStats.trophyPoints || 0 }}</span>
              </div>
            </div>

            <!-- Khối nút hành động dàn ngang trên Mobile khi xem trang cá nhân người khác -->
            <div class="profile-mobile-actions" v-if="!isCurrentUser">
              <button class="btn-mobile-action" @click="triggerReport">Báo cáo</button>
              <button class="btn-mobile-action" @click="handleFollow">Theo dõi</button>
              <button class="btn-mobile-action" @click="handleBlock">Chặn</button>
              <button class="btn-mobile-action" @click="startConversation">Bắt đầu đối thoại</button>
            </div>

            <!-- Khối nút hành động dàn ngang trên Mobile khi xem trang cá nhân của chính mình -->
            <div class="profile-mobile-actions" v-else>
              <button class="btn-mobile-action" @click="triggerReport">Báo cáo</button>
              <button class="btn-mobile-action btn-banner-edit" @click="openUploadModal('banner')">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                  <circle cx="12" cy="13" r="4"></circle>
                </svg>
                Đổi ảnh bìa
              </button>
            </div>

            <!-- Thành phần 2: Tabs Selection -->
            <div class="profile-tabs-bar">
              <button 
                class="profile-tab-btn pl-and-pr-6" 
                :class="{ 'is-active': activeTab === 'posts' }"
                @click="switchTab('posts')"
              >
                Bài đăng ({{ userStats.threadCount || 0 }})
              </button>
              <button 
                class="profile-tab-btn pl-and-pr-6" 
                :class="{ 'is-active': activeTab === 'comments' }"
                @click="switchTab('comments')"
              >
                Bình luận/Phản hồi ({{ userStats.commentCount || 0 }})
              </button>
              <button 
                class="profile-tab-btn pl-and-pr-6" 
                :class="{ 'is-active': activeTab === 'about' }"
                @click="switchTab('about')"
              >
                Giới thiệu
              </button>
            </div>

            <!-- Thành phần 3: Danh sách & Phân trang -->
            <div class="profile-tab-content">
              <!-- Tab: Giới thiệu -->
              <div v-if="activeTab === 'about'" class="about-tab-panel">
                <p class="about-empty-text">Thành viên này chưa viết lời giới thiệu.</p>
              </div>

              <!-- Tab: Bài đăng & Bình luận/Phản hồi -->
              <div v-else class="list-tab-panel">
                <div v-if="listLoading" class="list-loading-state">
                  Đang tải dữ liệu...
                </div>
                <div v-else-if="items.length === 0" class="list-empty-state">
                  Không có nội dung nào được tìm thấy.
                </div>
                <template v-else>
                  <div class="profile-activity-list">
                    <div v-for="item in items" :key="item.id" class="activity-row-item">
                      <!-- Cột bên trái: Avatar -->
                      <div class="item-avatar-col">
                        <span class="item-avatar" :style="!isAvatarUrl(userStats.avatar) ? { backgroundColor: userStats.avatar || '#ccc' } : {}">
                          <img v-if="isAvatarUrl(userStats.avatar)" :src="userStats.avatar" />
                          <template v-else>
                            {{ userInitial }}
                          </template>
                        </span>
                      </div>

                      <!-- Cột bên phải: Nội dung -->
                      <div class="item-details-col">
                        <!-- Dòng 1: Tiêu đề + Label -->
                        <div class="item-title-row">
                          <span 
                            v-if="item.label || item.threadLabel" 
                            class="label-tag-mini" 
                            :style="getLabelStyle(item.label || item.threadLabel)"
                          >
                            {{ (item.label || item.threadLabel).name }}
                          </span>
                          <span class="item-title-link" @click="navigateToItem(item)">
                            {{ activeTab === 'posts' ? item.title : item.threadTitle }}
                          </span>
                        </div>

                        <!-- Dòng 2: Nội dung cắt tinh gọn -->
                        <div class="item-content-preview">
                          {{ stripHtml(item.content) }}
                        </div>

                        <!-- Dòng 3: Meta metadata -->
                        <div class="item-meta-row">
                          <span class="meta-author">{{ userStats.displayName || userStats.username }}</span>
                          <span class="meta-divider">&middot;</span>
                          <span class="meta-post-number">Post #{{ activeTab === 'posts' ? 1 : item.seqNumber }}</span>
                          <span class="meta-divider">&middot;</span>
                          <span class="meta-time">{{ formatDate(item.createdAt) }}</span>
                        </div>

                        <!-- Dòng 4: Chuyên mục -->
                        <div class="item-category-row">
                          Chuyên mục: 
                          <span class="category-link" @click="goToCategory(item.category)">
                            {{ item.category ? item.category.name : 'Không xác định' }}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Phân trang chung -->
                  <div class="pagination-wrapper" v-if="totalPages > 1" style="margin-top: 1.5rem;">
                    <ForumPagination 
                      :current-page="currentPage" 
                      :total-pages="totalPages" 
                      @page-changed="handlePageChange"
                    />
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Modal Upload ảnh đại diện / ảnh bìa -->
    <AvatarUploadModal 
      :show="showUploadModal" 
      :currentUser="userStats"
      :mode="uploadMode"
      @close="showUploadModal = false"
      @avatar-updated="onAvatarUpdated"
      @banner-updated="onBannerUpdated"
    />
  </div>
</template>

<script>
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import AvatarUploadModal from '@/shared/components/AvatarUploadModal.vue'
import Loading from '@/shared/components/Loading.vue'
import { formatForumDate } from '@/shared/utils/date'
import { isAvatarUrl } from '@/shared/utils/utils'
import api from '@/shared/services/api.service'
import userMixin from '@/shared/mixins/user.mixin.js'

export default {
  name: 'UserProfile',
  mixins: [userMixin],
  components: {
    Breadcrumb,
    ForumPagination,
    AvatarUploadModal,
    Loading
  },
  data() {
    return {
      loading: false,
      listLoading: false,
      userStats: {},
      activeTab: 'posts', // 'posts' | 'comments' | 'about'
      items: [],
      currentPage: 1,
      totalPages: 1,
      itemsPerPage: 10,
      showUploadModal: false,
      uploadMode: 'avatar', // 'avatar' | 'banner'
    }
  },
  computed: {
    isCurrentUser() {
      return this.checkIsCurrentUser(this.$route.query.username)
    },
    breadcrumbItems() {
      return [
        { title: 'Tài khoản của bạn', to: this.currentUserProfileLink },
        { title: 'Trang cá nhân', active: true }
      ]
    },
    userInitial() {
      const name = this.userStats.displayName || this.userStats.username || ''
      return name.charAt(0).toUpperCase()
    }
  },
  watch: {
    '$route.query.username': {
      handler(newVal) {
        const currentUserStr = localStorage.getItem('user')
        if (currentUserStr && !newVal) {
          const currentUser = JSON.parse(currentUserStr)
          this.$router.replace({
            name: 'UserProfile',
            query: { username: currentUser.username }
          })
          return
        }
        this.currentPage = 1
        this.activeTab = 'posts'
        this.loadProfileData()
      }
    }
  },
  created() {
    const currentUserStr = localStorage.getItem('user')
    if (currentUserStr) {
      const currentUser = JSON.parse(currentUserStr)
      if (!this.$route.query.username) {
        this.$router.replace({
          name: 'UserProfile',
          query: { username: currentUser.username }
        })
        return
      }
    }
    this.loadProfileData()
  },
  mounted() {
    window.addEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  beforeUnmount() {
    window.removeEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  methods: {
    handleAvatarUpdated(event) {
      const { username, avatar } = event.detail
      if (this.userStats && this.userStats.username === username) {
        this.userStats.avatar = avatar
      }
    },
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    formatDate(dateStr) {
      return formatForumDate(dateStr)
    },
    formatRoles(roles) {
      if (!roles) return 'Thành viên'
      const list = Array.from(roles)
      if (list.includes('ROLE_SUPER_ADMIN')) return 'Super Admin'
      if (list.includes('ROLE_ADMIN')) return 'Admin'
      return 'Thành viên chính thức'
    },
    getLabelStyle(label) {
      if (!label) return {}
      return {
        backgroundColor: label.colorCode,
        color: label.textColor,
        borderColor: label.borderColor || 'transparent'
      }
    },
    stripHtml(html) {
      if (!html) return ''
      let text = html.replace(/<[^>]*>/g, '')
      text = text.replace(/&nbsp;/g, ' ')
                 .replace(/&amp;/g, '&')
                 .replace(/&lt;/g, '<')
                 .replace(/&gt;/g, '>')
      if (text.length > 200) {
        return text.substring(0, 200) + '...'
      }
      return text
    },
    triggerReport() {
      alert('Chức năng báo cáo sẽ được cập nhật sau.')
    },
    openUploadModal(mode) {
      this.uploadMode = mode
      this.showUploadModal = true
    },
    async loadProfileData() {
      this.loading = true
      try {
        await Promise.all([
          this.fetchUserStats(),
          this.fetchTabData()
        ])
      } catch (e) {
        console.error('Lỗi load dữ liệu profile:', e)
      } finally {
        this.loading = false
      }
    },
    async fetchUserStats() {
      const currentUserStr = localStorage.getItem('user')
      if (!currentUserStr) return
      const currentUser = JSON.parse(currentUserStr)
      const queryUsername = this.$route.query.username || currentUser.username
      
      try {
        const res = await api.get('/users/by-name', { params: { name: queryUsername } })
        if (res.data) {
          this.userStats = res.data
          // Đồng bộ lại local storage chỉ khi là chính mình
          if (queryUsername === currentUser.username) {
            const updatedLocalUser = {
              ...currentUser,
              avatar: this.userStats.avatar,
              displayName: this.userStats.displayName,
              profileBanner: this.userStats.profileBanner,
              postCount: this.userStats.postCount,
              interactionPoints: this.userStats.interactionPoints,
              trophyPoints: this.userStats.trophyPoints
            }
            localStorage.setItem('user', JSON.stringify(updatedLocalUser))
          }
        }
      } catch (e) {
        console.error('Lỗi lấy thông tin người dùng:', e)
        if (queryUsername === currentUser.username) {
          this.userStats = currentUser
        }
      }
    },
    async fetchTabData() {
      if (this.activeTab === 'about') return
      
      this.listLoading = true
      try {
        const currentUserStr = localStorage.getItem('user')
        if (!currentUserStr) return
        const currentUser = JSON.parse(currentUserStr)
        const queryUsername = this.$route.query.username || currentUser.username
        
        let endpoint
        if (queryUsername === currentUser.username) {
          endpoint = this.activeTab === 'posts' ? '/threads/me' : '/posts/me'
        } else {
          endpoint = this.activeTab === 'posts' ? `/threads/user/${queryUsername}` : `/posts/user/${queryUsername}`
        }
        
        const res = await api.get(endpoint, {
          params: {
            page: this.currentPage - 1,
            size: this.itemsPerPage
          }
        })
        
        if (res.data) {
          this.items = res.data.content || []
          this.totalPages = res.data.totalPages || 1
        } else {
          this.items = []
          this.totalPages = 1
        }
      } catch (e) {
        console.error('Lỗi khi tải dữ liệu tab:', e)
        this.items = []
        this.totalPages = 1
      } finally {
        this.listLoading = false
      }
    },
    switchTab(tab) {
      this.activeTab = tab
      this.currentPage = 1
      this.fetchTabData()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchTabData()
    },
    async navigateToItem(item) {
      if (this.activeTab === 'posts') {
        this.$router.push({ name: 'ThreadDetail', params: { id: item.id } })
      } else {
        // Đối với bình luận, định tuyến trực tiếp đến trang chứa post và cuộn xuống
        try {
          this.loading = true
          const res = await api.get(`/posts/${item.id}/page-number`, { params: { size: 10 } })
          const pageNum = res.data || 1
          this.$router.push({
            name: 'ThreadDetail',
            params: { id: item.threadId },
            query: { page: pageNum, postId: item.id },
            hash: `#post-${item.id}`
          })
        } catch (e) {
          console.error(e)
          this.$router.push({ name: 'ThreadDetail', params: { id: item.threadId } })
        } finally {
          this.loading = false
        }
      }
    },
    goToCategory(category) {
      if (category && category.id) {
        this.$router.push({ name: 'CategoryDetail', params: { id: category.id } })
      }
    },
    onAvatarUpdated(newAvatar) {
      this.userStats.avatar = newAvatar
      // Phát sự kiện toàn cục để cập nhật avatar trên header
      window.dispatchEvent(new CustomEvent('user-avatar-updated', {
        detail: { username: this.userStats.username, avatar: newAvatar }
      }))
      this.fetchUserStats()
    },
    onBannerUpdated(newBanner) {
      this.userStats.profileBanner = newBanner
      this.fetchUserStats()
    },
    handleFollow() {
      alert('Tính năng Theo dõi sẽ được cập nhật sau.')
    },
    handleBlock() {
      alert('Tính năng Chặn sẽ được cập nhật sau.')
    },
    startConversation() {
      const nameParam = this.userStats.displayName || this.userStats.username
      this.$router.push({
        name: 'AddConversation',
        query: { to: nameParam }
      })
    }
  }
}
</script>

<style scoped>
.account-layout {
  display: flex;
  gap: 2rem;
  margin-bottom: 3rem;
}

@media (max-width: 992px) {
  .account-layout {
    flex-direction: column;
    gap: 0;
  }
}

.account-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Card base */
.card {
  background: #ffffff;
  border: 1px solid #d8dbe0;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  overflow: hidden;
}

/* Profile Header Card */
.profile-banner-area {
  height: 220px;
  background-color: #edf6fd;
  position: relative;
  background-size: cover;
  background-position: center;
  transition: all 0.3s ease;
  display: flex;
  align-items: flex-end;
  padding: 1.5rem;
}

@media (max-width: 767px) {
  .profile-banner-area {
    height: 160px;
    padding: 1rem;
  }
}

.banner-overlay-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, rgba(0,0,0,0.02) 0%, rgba(0,0,0,0.12) 100%);
  z-index: 1;
}

.banner-actions {
  position: relative;
  top: auto;
  left: auto;
  margin-top: 16px;
  display: flex;
  gap: 8px;
  z-index: 10;
}

.btn-banner-action {
  background: rgba(255, 255, 255, 0.9);
  color: #1a507a;
  border: 1px solid #c8d4e0;
  padding: 6px 12px;
  font-size: 0.82rem;
  border-radius: 3px;
  cursor: pointer;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn-banner-action:hover {
  background: #ffffff;
  color: #d13838;
}

.btn-banner-edit {
  background: rgba(26, 80, 122, 0.85);
  color: #ffffff;
  border-color: #123a59;
}
.btn-banner-edit:hover {
  background: #1a507a;
  color: #ffffff;
}

/* Info upper positioning inside banner */
.profile-info-upper {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  padding: 0 24px;
  gap: 20px;
  z-index: 10;
}

@media (max-width: 576px) {
  .profile-info-upper {
    gap: 1rem;
  }
}

.profile-avatar-wrapper {
  position: relative;
  left: auto;
  bottom: auto;
  margin-top: 20px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  border: 4px solid #ffffff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  overflow: hidden;
  cursor: default;
  z-index: 11;
  background-color: #fff;
  flex-shrink: 0;
}

.profile-avatar-wrapper.clickable {
  cursor: pointer;
}

.profile-avatar-wrapper:hover .avatar-edit-overlay {
  opacity: 1;
}

.avatar-edit-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 30px;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}
.avatar-edit-overlay span {
  color: #fff;
  font-size: 0.72rem;
  font-weight: bold;
}

.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
  font-size: 5.5rem;
}

.profile-meta-details {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  color: #1a507a;
  margin-left: 0;
  margin-top: 24px;
  flex: 1;
}

.profile-info-upper.text-white .profile-meta-details {
  color: #1a507a !important;
  background: none !important;
  padding: 0 !important;
  border: none !important;
  box-shadow: none !important;
}
.profile-info-upper.text-white .profile-displayname {
  color: #1a507a !important;
}
.profile-info-upper.text-white .profile-title-tag {
  color: #666 !important;
}
.profile-info-upper.text-white .text-dimmed {
  color: #7f8c8d !important;
  white-space: nowrap !important;
}

.profile-displayname {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
  line-height: 1.2;
}

.profile-title-tag {
  font-size: 0.9rem;
  font-weight: 500;
  margin-top: 2px;
}
.profile-info-upper.text-white .profile-title-tag {
  color: #e5edf5;
}

.profile-time-row {
  margin-top: 4px;
  font-size: 0.85rem;
}

.text-dimmed {
  opacity: 0.85;
}
.profile-info-upper.text-white .text-dimmed {
  color: #edf2f7;
}

.highlight-self {
  font-weight: 600;
  text-decoration: underline;
}

/* Stats Bar */
.profile-stats-bar {
  display: flex;
  background: #ffffff;
  border-top: 1px solid #d8dbe0;
  padding: 265px 24px 15px 268px;
  justify-content: space-between;
  align-items: center;
}

.stat-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 0.8rem;
  color: #7f8c8d;
}

.stat-val {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1a507a;
}

@media (max-width: 767px) {
  .profile-banner-area {
    height: 180px;
    padding: 0;
    flex-direction: column;
    justify-content: flex-end;
    position: relative;
  }
  
  .profile-info-upper {
    position: absolute;
    top: 100%;
    left: 16px;
    width: calc(100% - 32px);
    flex-direction: row;
    align-items: flex-start;
    text-align: left;
    gap: 12px;
    z-index: 10;
  }

  .profile-avatar-wrapper {
    position: relative;
    left: auto;
    bottom: auto;
    width: 90px;
    height: 90px;
    margin: 16px 0 0 0;
    flex-shrink: 0;
    border: 3px solid #ffffff;
    box-shadow: 0 2px 5px rgba(0,0,0,0.1);
  }

  .profile-avatar-placeholder {
    font-size: 2.2rem;
  }

  .profile-meta-details {
    margin-left: 0;
    align-items: flex-start;
    text-align: left;
    width: 100%;
    max-width: none;
    background: none !important;
    padding: 10px 0 0 0 !important;
    border: none !important;
    box-shadow: none !important;
    color: #1a507a !important;
  }

  .profile-displayname {
    font-size: 1.25rem;
    color: #1a507a !important;
  }
  
  .profile-title-tag {
    color: #666 !important;
    margin-top: 1px;
  }

  .profile-time-row,
  .profile-time-row .text-dimmed {
    color: #7f8c8d !important;
  }
  .profile-time-row {
    margin-top: 3px;
  }

  .banner-actions {
    position: relative;
    top: auto;
    left: auto;
    width: auto;
    margin-top: 20px;
    display: flex;
    justify-content: flex-start;
    gap: 8px;
    z-index: 10;
  }

  .profile-stats-bar {
    margin-top: 166px;
    padding: 12px 1rem;
    justify-content: space-between;
  }
}

@media (max-width: 576px) {
  .profile-banner-area {
    height: 140px;
  }
  
  .profile-avatar-wrapper {
    width: 76px;
    height: 76px;
    margin-top: 12px;
  }
  
  .profile-avatar-placeholder {
    font-size: 1.8rem;
  }
  
  .profile-meta-details {
    padding-top: 6px !important;
  }

  .profile-displayname {
    font-size: 1.15rem;
  }

  .banner-actions {
    margin-top: 4px;
  }

  .profile-stats-bar {
    margin-top: 166px;
    padding: 10px 0.75rem;
  }
}


/* Tabs styles */
.profile-tabs-bar {
  display: flex;
  background: #f8f9fa;
  border-top: 1px solid #d8dbe0;
  border-bottom: 1px solid #d8dbe0;
  overflow-x: auto;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}
.profile-tabs-bar::-webkit-scrollbar {
  display: none;
}

.profile-tab-btn {
  background: none;
  border: none;
  padding: 12px 20px;
  font-size: 0.92rem;
  color: #1a507a;
  font-weight: 500;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  transition: all 0.2s;
  outline: none;
  flex-shrink: 0;
  white-space: nowrap;
}

.profile-tab-btn:hover {
  background-color: #edf5fa;
  color: #d13838;
}

.profile-tab-btn.is-active {
  border-bottom-color: #1a507a;
  font-weight: 700;
  background-color: #ffffff;
}

.profile-tab-content {
  padding: 1.5rem;
}

/* Activity Items */
.profile-activity-list {
  display: flex;
  flex-direction: column;
}

.activity-row-item {
  display: flex;
  gap: 15px;
  padding: 15px 0;
  border-bottom: 1px solid #edf2f7;
}

.activity-row-item:first-child {
  padding-top: 0;
}

.activity-row-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.item-avatar-col {
  flex-shrink: 0;
}

.item-avatar {
  width: 44px;
  height: 44px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
  font-size: 1.1rem;
  overflow: hidden;
}
.item-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-details-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-title-row {
  line-height: 1.4;
  margin-bottom: 4px;
}

.label-tag-mini {
  display: inline-block;
  padding: 1px 6px;
  font-size: 0.72rem;
  font-weight: 600;
  border-radius: 3px;
  border: 1px solid transparent;
  margin-right: 6px;
  vertical-align: middle;
}

.item-title-link {
  font-size: 0.98rem;
  font-weight: 600;
  color: #1a507a;
  cursor: pointer;
  display: inline;
  vertical-align: middle;
}
.item-title-link:hover {
  text-decoration: underline;
}

.item-content-preview {
  font-size: 0.88rem;
  color: #555555;
  line-height: 1.4;
  word-break: break-word;
}

.item-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 0.8rem;
  color: #7f8c8d;
  margin-top: 2px;
}

.meta-author {
  font-weight: 600;
  color: #555;
}

.category-link {
  color: #1a507a;
  cursor: pointer;
  font-weight: 500;
}
.category-link:hover {
  text-decoration: underline;
}

.meta-divider {
  color: #cbd5e1;
}

.item-category-row {
  font-size: 0.8rem;
  color: #7f8c8d;
  margin-top: 2px;
}

/* Loading & Empty States */
.list-loading-state,
.list-empty-state {
  text-align: center;
  padding: 3rem 1.5rem;
  color: #7f8c8d;
  font-size: 0.95rem;
}

/* About tab specific styling */
.about-tab-panel {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.about-header {
  margin: 0 0 10px 0;
  font-size: 1.1rem;
  color: #1a507a;
  border-bottom: 2.5px solid #edf2f7;
  padding-bottom: 8px;
}

.about-empty-text {
  font-size: 0.9rem;
  color: #7f8c8d;
  margin: 0;
}

.about-stats-table {
  width: 100%;
  max-width: 450px;
  border-collapse: collapse;
}

.about-stats-table td {
  padding: 8px 0;
  font-size: 0.9rem;
  border-bottom: 1px solid #f1f3f5;
}

.about-stats-table td:first-child {
  color: #7f8c8d;
  width: 150px;
}

.about-stats-table td:last-child {
  font-weight: 600;
  color: #333333;
}

@import "@/shared/assets/styles/custom.css";

.profile-header-card {
  position: relative;
}

.profile-info-upper.positioned-absolute {
  position: absolute;
  top: 220px;
  left: 0;
  width: 100%;
  z-index: 10;
}

/* No Banner Layout Styles */
.profile-header-card.no-banner .profile-info-upper {
  position: static;
  padding: 24px 24px 0 24px;
  background: #ffffff;
  color: #1a507a;
}

.profile-header-card.no-banner .profile-avatar-wrapper {
  margin-top: 0;
  width: 150px;
  height: 150px;
}

.profile-header-card.no-banner .profile-avatar-placeholder {
  font-size: 3.5rem;
}

.profile-header-card.no-banner .profile-meta-details {
  margin-top: 0;
}

.profile-header-card.no-banner .profile-stats-bar {
  padding: 20px 24px;
  border-top: 1px solid #d8dbe0;
}

@media (max-width: 767px) {
  .profile-info-upper.positioned-absolute {
    top: 180px;
    left: 16px;
    width: calc(100% - 32px);
  }
  
  .profile-header-card.no-banner .profile-info-upper {
    padding: 16px 16px 0 16px;
  }
  
  .profile-header-card.no-banner .profile-avatar-wrapper {
    width: 100px;
    height: 100px;
  }
  
  .profile-header-card.no-banner .profile-avatar-placeholder {
    font-size: 2.5rem;
  }

  .profile-header-card.no-banner .profile-stats-bar {
    margin-top: 0;
    padding: 12px 1rem;
  }
}

@media (max-width: 576px) {
  .profile-info-upper.positioned-absolute {
    top: 140px;
  }
  
  .profile-header-card.no-banner .profile-stats-bar {
    margin-top: 0;
    padding: 10px 0.75rem;
  }
}

/* Mobile Actions Block */
.profile-mobile-actions {
  display: none;
  background: #ffffff;
  padding: 10px 15px;
  border-top: 1px solid #d8dbe0;
  border-bottom: 1px solid #d8dbe0;
  gap: 8px;
  justify-content: space-between;
}

.btn-mobile-action {
  flex: 1;
  background-color: #ffffff;
  border: 1px solid #c8d4e0;
  color: #1a507a;
  font-weight: 500;
  padding: 8px 4px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
  transition: all 0.2s;
  font-family: inherit;
  outline: none;
  text-align: center;
  white-space: nowrap;
}

.btn-mobile-action:hover {
  background-color: #1a507a;
  color: #ffffff;
  border-color: #1a507a;
}

@media (max-width: 767px) {
  /* Hiển thị khối nút mới trên mobile */
  .profile-mobile-actions {
    display: flex;
  }
  
  /* Ẩn hoàn toàn khối nút cũ trên header mobile cho cả mình và người khác */
  .banner-actions {
    display: none !important;
  }

  /* Cho phép text thời gian online gần nhất xuống dòng tự nhiên */
  .profile-info-upper.text-white .text-dimmed {
    white-space: normal !important;
    word-break: break-word;
  }
}
</style>
