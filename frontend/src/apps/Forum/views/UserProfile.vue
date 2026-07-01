<template>
  <div>
    <Loading :visible="loading" />

    <main class="container" style="padding-top: 2rem;">
      <!-- Breadcrumb -->
      <Breadcrumb :items="breadcrumbItems" />

      <div class="account-layout">
        <!-- Cột trái: Sidebar -->
        <AccountSidebar activeMenu="profile" />

        <!-- Cột phải: Content chính -->
        <div class="account-content">
          <!-- Thành phần 1: Profile Header & Details -->
          <div class="profile-header-card card">
            <!-- Vùng ảnh bìa (banner) -->
            <div 
              class="profile-banner-area" 
              :class="{ 'has-banner': !!userStats.profileBanner }"
              :style="userStats.profileBanner ? { backgroundImage: `url(${userStats.profileBanner})` } : {}"
            >
              <div class="banner-overlay-gradient" v-if="userStats.profileBanner"></div>
              
              <!-- Nút hành động trên banner -->
              <div class="banner-actions">
                <button class="btn-banner-action" @click="triggerReport">Báo cáo</button>
                <button class="btn-banner-action btn-banner-edit" @click="openUploadModal('banner')">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                    <circle cx="12" cy="13" r="4"></circle>
                  </svg>
                  Edit profile banner
                </button>
              </div>

              <!-- Thông tin tài khoản phía trên (nằm trong phạm vi banner) -->
              <div class="profile-info-upper" :class="{ 'text-white': !!userStats.profileBanner }">
                <div class="profile-avatar-wrapper" @click="openUploadModal('avatar')">
                  <img v-if="isAvatarUrl(userStats.avatar)" :src="userStats.avatar" class="profile-avatar-img" />
                  <div v-else class="profile-avatar-placeholder" :style="{ backgroundColor: userStats.avatar || '#1a507a' }">
                    {{ userInitial }}
                  </div>
                  <div class="avatar-edit-overlay">
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
                    <span class="meta-item text-dimmed">Thấy lần gần nhất: 3 phút trước · Đang xem hồ sơ thành viên <span class="highlight-self">{{ userStats.displayName || userStats.username }}</span></span>
                  </div>
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
          </div>

          <!-- Thành phần 2: Tabs Selection -->
          <div class="profile-tabs-card card" style="margin-top: 1.5rem;">
            <div class="profile-tabs-bar">
              <button 
                class="profile-tab-btn" 
                :class="{ 'is-active': activeTab === 'posts' }"
                @click="switchTab('posts')"
              >
                Bài đăng
              </button>
              <button 
                class="profile-tab-btn" 
                :class="{ 'is-active': activeTab === 'comments' }"
                @click="switchTab('comments')"
              >
                Bình luận/Phản hồi
              </button>
              <button 
                class="profile-tab-btn" 
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
                <div class="about-section">
                  <h3 class="about-header">Giới thiệu bản thân</h3>
                  <p class="about-empty-text">Thành viên này chưa viết lời giới thiệu.</p>
                </div>
                <div class="about-section" style="margin-top: 2rem;">
                  <h3 class="about-header">Thông tin tài khoản</h3>
                  <table class="about-stats-table">
                    <tr>
                      <td>Ngày tham gia:</td>
                      <td>{{ formatDate(userStats.createdAt) }}</td>
                    </tr>
                    <tr>
                      <td>Tên tài khoản:</td>
                      <td>{{ userStats.username }}</td>
                    </tr>
                    <tr>
                      <td>Vai trò nhóm:</td>
                      <td>{{ formatRoles(userStats.roles) }}</td>
                    </tr>
                  </table>
                </div>
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
                          <span class="meta-divider">&middot;</span>
                          <span class="meta-category">
                            Chuyên mục: 
                            <span class="category-link" @click="goToCategory(item.category || (item.category || {}))">
                              {{ item.category ? item.category.name : 'Không xác định' }}
                            </span>
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
import AccountSidebar from '@/shared/components/AccountSidebar.vue'
import AvatarUploadModal from '@/shared/components/AvatarUploadModal.vue'
import Loading from '@/shared/components/Loading.vue'
import { formatForumDate } from '@/shared/utils/date'
import { isAvatarUrl } from '@/shared/utils/utils'
import api from '@/shared/services/api.service'

export default {
  name: 'UserProfile',
  components: {
    Breadcrumb,
    ForumPagination,
    AccountSidebar,
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
    breadcrumbItems() {
      return [
        { title: 'Tài khoản của bạn', to: '#' },
        { title: 'Trang cá nhân', active: true }
      ]
    },
    userInitial() {
      const name = this.userStats.displayName || this.userStats.username || ''
      return name.charAt(0).toUpperCase()
    }
  },
  created() {
    this.fetchUserStats()
    this.fetchTabData()
  },
  methods: {
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
    async fetchUserStats() {
      const userStr = localStorage.getItem('user')
      if (!userStr) return
      const localUser = JSON.parse(userStr)
      try {
        const res = await api.get('/users/by-name', { params: { name: localUser.username } })
        if (res.data && res.data.data) {
          this.userStats = res.data.data
          // Đồng bộ lại local storage nếu có thay đổi ảnh/tên
          const updatedLocalUser = {
            ...localUser,
            avatar: this.userStats.avatar,
            displayName: this.userStats.displayName,
            profileBanner: this.userStats.profileBanner,
            postCount: this.userStats.postCount,
            interactionPoints: this.userStats.interactionPoints,
            trophyPoints: this.userStats.trophyPoints
          }
          localStorage.setItem('user', JSON.stringify(updatedLocalUser))
        }
      } catch (e) {
        console.error('Lỗi lấy thông tin người dùng:', e)
        this.userStats = localUser
      }
    },
    async fetchTabData() {
      if (this.activeTab === 'about') return
      
      this.listLoading = true
      try {
        const endpoint = this.activeTab === 'posts' ? '/threads/me' : '/posts/me'
        const res = await api.get(endpoint, {
          params: {
            page: this.currentPage - 1,
            size: this.itemsPerPage
          }
        })
        
        if (res.data && res.data.data) {
          this.items = res.data.data.content || []
          this.totalPages = res.data.data.totalPages || 1
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
          const pageNum = res.data?.data || 1
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
    gap: 1rem;
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
  background: linear-gradient(to bottom, rgba(0,0,0,0.1) 0%, rgba(0,0,0,0.5) 100%);
  z-index: 1;
}

.banner-actions {
  position: absolute;
  top: 15px;
  right: 15px;
  display: flex;
  gap: 8px;
  z-index: 5;
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
  display: flex;
  align-items: center;
  gap: 1.5rem;
  z-index: 3;
  width: 100%;
}

@media (max-width: 576px) {
  .profile-info-upper {
    gap: 1rem;
  }
}

.profile-avatar-wrapper {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  border: 4px solid #ffffff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  overflow: hidden;
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
  background-color: #fff;
}

@media (max-width: 576px) {
  .profile-avatar-wrapper {
    width: 72px;
    height: 72px;
  }
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
  font-size: 2.2rem;
}

.profile-meta-details {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  color: #1a507a;
}
.profile-info-upper.text-white .profile-meta-details {
  color: #ffffff;
}

.profile-displayname {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
  line-height: 1.2;
}

@media (max-width: 576px) {
  .profile-displayname {
    font-size: 1.25rem;
  }
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
  background: #f8f9fa;
  border-top: 1px solid #d8dbe0;
  padding: 12px 1.5rem;
  gap: 2.5rem;
}

@media (max-width: 576px) {
  .profile-stats-bar {
    gap: 1.2rem;
    padding: 10px 1rem;
  }
}

.stat-box {
  display: flex;
  flex-direction: column;
  gap: 2px;
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

/* Tabs styles */
.profile-tabs-bar {
  display: flex;
  background: #f8f9fa;
  border-bottom: 1px solid #d8dbe0;
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
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.label-tag-mini {
  display: inline-block;
  padding: 1px 6px;
  font-size: 0.72rem;
  font-weight: 600;
  border-radius: 3px;
  border: 1px solid transparent;
}

.item-title-link {
  font-size: 0.98rem;
  font-weight: 600;
  color: #1a507a;
  cursor: pointer;
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
</style>
