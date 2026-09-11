<template>
  <div class="following-page-wrapper">
    <main class="container" style="padding-top: 2rem; padding-bottom: 2.5rem;">
      <!-- Breadcrumb điều hướng -->
      <Breadcrumb :items="breadcrumbItems" />

      <div class="account-layout">
        <!-- Cột trái: Sidebar điều hướng tài khoản -->
        <AccountSidebar activeMenu="following" />

        <!-- Cột phải: Khung nội dung chính -->
        <div class="account-content">
          <div class="following-card card">

            <!-- Trạng thái đang tải -->
            <div v-if="isLoading" class="state-container loading-state">
              <div class="spinner"></div>
              <span>Đang tải danh sách...</span>
            </div>

            <!-- Trạng thái trống -->
            <div v-else-if="users.length === 0" class="state-container empty-state">
              <div class="empty-icon">
                <i class="fa fa-users-slash"></i>
              </div>
              <div class="empty-text">Bạn chưa theo dõi thành viên nào.</div>
              <router-link to="/thanh-vien" class="btn-discover">
                Khám phá thành viên
              </router-link>
            </div>

            <!-- Danh sách thành viên đang theo dõi -->
            <div v-else class="following-list">
              <div 
                v-for="user in users" 
                :key="user.id" 
                class="following-item-row"
              >
                <!-- Khối bên trái: Avatar & Thông tin 3 dòng -->
                <div class="following-left-col">
                  <!-- Avatar có popup -->
                  <UserProfilePopup :user="user">
                    <div 
                      class="following-avatar-circle" 
                      :style="!isAvatarUrl(user.avatar) ? { backgroundColor: getAvatarColor(user) } : {}"
                    >
                      <img v-if="isAvatarUrl(user.avatar)" :src="formatAvatarUrl(user.avatar)" alt="avatar" />
                      <span v-else>{{ (user.displayName || user.username || '?').charAt(0).toUpperCase() }}</span>
                    </div>
                  </UserProfilePopup>

                  <!-- Thông tin thành viên -->
                  <div class="following-info-col">
                    <!-- Dòng 1: Tên hiển thị + Tích xanh -->
                    <div class="following-name-row">
                      <UserProfilePopup :user="user">
                        <span class="following-display-name">
                          {{ user.displayName || user.username }}
                          <VerifiedBadge :user="user" size="15px" />
                        </span>
                      </UserProfilePopup>
                    </div>

                    <!-- Dòng 2: Cấp bậc / Danh hiệu -->
                    <div class="following-rank-row">
                      {{ user.displayTitle || 'Thành viên' }}
                    </div>

                    <!-- Dòng 3: 3 chỉ số thống kê -->
                    <div class="following-stats-row">
                      Bài viết: {{ formatNumber(user.postCount) }} · Điểm tương tác: {{ formatNumber(user.interactionPoints) }} · Điểm: {{ formatNumber(user.trophyPoints) }}
                    </div>
                  </div>
                </div>

                <!-- Khối bên phải: Nút Bỏ theo dõi -->
                <div class="following-right-col">
                  <button 
                    class="btn-unfollow"
                    :disabled="unfollowingIds.includes(user.id)"
                    @click="handleUnfollow(user)"
                    title="Bỏ theo dõi"
                  >
                    <i v-if="unfollowingIds.includes(user.id)" class="fa fa-spinner fa-spin"></i>
                    <i v-else class="fa fa-user-minus"></i>
                    <span>Bỏ theo dõi</span>
                  </button>
                </div>
              </div>
            </div>

            <!-- Phân trang -->
            <div v-if="totalPages > 1" class="following-pagination-wrapper">
              <ForumPagination
                :currentPage="currentPage"
                :totalPages="totalPages"
                @page-changed="handlePageChange"
              />
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import AccountSidebar from '@/shared/components/AccountSidebar.vue'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import UserProfilePopup from '@/shared/components/UserProfilePopup.vue'
import VerifiedBadge from '@/shared/components/VerifiedBadge.vue'
import userFollowService from '@/apps/Forum/services/user-follow.service'
import { isAvatarUrl, formatAvatarUrl } from '@/shared/utils/utils'
import { alertConfirm, toastSuccess, toastError } from '@/shared/utils/swal'

export default {
  name: 'FollowingList',
  components: {
    Breadcrumb,
    AccountSidebar,
    ForumPagination,
    UserProfilePopup,
    VerifiedBadge
  },
  computed: {
    breadcrumbItems() {
      return [
        { title: 'Tài khoản của bạn', to: '/account/profile' },
        { title: 'Đang theo dõi', active: true }
      ]
    }
  },
  data() {
    return {
      users: [],
      currentPage: 1,
      pageSize: 15,
      totalPages: 1,
      totalElements: 0,
      isLoading: false,
      unfollowingIds: []
    }
  },
  created() {
    const pageFromQuery = parseInt(this.$route.query.page, 10)
    if (!isNaN(pageFromQuery) && pageFromQuery > 0) {
      this.currentPage = pageFromQuery
    }
    this.fetchFollowingUsers()
  },
  watch: {
    '$route.query.page'(newPage) {
      const p = parseInt(newPage, 10) || 1
      if (p !== this.currentPage) {
        this.currentPage = p
        this.fetchFollowingUsers()
      }
    }
  },
  methods: {
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    formatAvatarUrl(avatar) {
      return formatAvatarUrl(avatar)
    },
    formatNumber(num) {
      if (num === null || num === undefined) return '0'
      return Number(num).toLocaleString('en-US')
    },
    getAvatarColor(user) {
      if (!user) return '#1a507a'
      if (user.avatar && (user.avatar.startsWith('#') || user.avatar.startsWith('hsl'))) {
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
    async fetchFollowingUsers() {
      this.isLoading = true
      try {
        const res = await userFollowService.getFollowingUsers({
          page: this.currentPage - 1,
          size: this.pageSize
        })
        const pageData = res?.data?.data || res?.data
        if (pageData && Array.isArray(pageData.content)) {
          this.users = pageData.content
          this.totalPages = pageData.totalPages || 1
          this.totalElements = pageData.totalElements !== undefined ? pageData.totalElements : pageData.content.length
        } else {
          this.users = []
          this.totalPages = 1
          this.totalElements = 0
        }
      } catch (err) {
        console.error('Lỗi khi nạp danh sách đang theo dõi:', err)
        toastError('Không thể tải danh sách đang theo dõi.')
      } finally {
        this.isLoading = false
      }
    },
    handlePageChange(newPage) {
      if (newPage === this.currentPage) return
      this.$router.push({
        name: 'FollowingList',
        query: {
          ...this.$route.query,
          page: newPage
        }
      })
    },
    async handleUnfollow(user) {
      if (!user || this.unfollowingIds.includes(user.id)) return

      const displayName = user.displayName || user.username
      const confirmRes = await alertConfirm(
        'Bỏ theo dõi',
        `Bạn có chắc chắn muốn bỏ theo dõi "${displayName}"?`
      )

      if (!confirmRes.isConfirmed) return

      this.unfollowingIds.push(user.id)
      try {
        await userFollowService.toggleFollow(user.username, false)
        toastSuccess(`Đã bỏ theo dõi ${displayName}`)

        // Xóa ngay người dùng này khỏi danh sách hiện tại
        this.users = this.users.filter(u => u.id !== user.id)
        if (this.totalElements > 0) {
          this.totalElements--
        }

        // Nếu danh sách trang hiện tại trống và còn trang trước, quay lại trang trước
        if (this.users.length === 0 && this.currentPage > 1) {
          this.handlePageChange(this.currentPage - 1)
        } else if (this.users.length === 0 && this.totalElements > 0) {
          this.fetchFollowingUsers()
        }
      } catch (err) {
        console.error('Lỗi khi bỏ theo dõi:', err)
        toastError(err.response?.data?.message || 'Có lỗi xảy ra khi bỏ theo dõi.')
      } finally {
        this.unfollowingIds = this.unfollowingIds.filter(id => id !== user.id)
      }
    }
  }
}
</script>

<style scoped>
.following-page-wrapper {
  min-height: calc(100vh - 120px);
}

.account-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  margin-top: 15px;
  width: 100%;
}

.account-content {
  flex: 1;
  min-width: 0;
  width: 100%;
}

.card {
  background: #fff;
  border: 1px solid #d8dbe0;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.following-card {
  background: #ffffff;
  overflow: hidden;
  width: 100%;
}

.following-card-header {
  background: #f8f9fa;
  padding: 14px 18px;
  border-bottom: 1px solid #e2e5e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.following-card-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: #1a507a;
  margin: 0;
}

.following-count-badge {
  background: #eef4f8;
  color: #1a507a;
  font-size: 0.82rem;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: 12px;
  border: 1px solid #d0deea;
}

/* State containers */
.state-container {
  padding: 3.5rem 1.5rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #6c757d;
  width: 100%;
}

.loading-state {
  gap: 12px;
  font-size: 0.95rem;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e9ecef;
  border-top: 3px solid #1a507a;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  gap: 10px;
}

.empty-icon {
  font-size: 2.5rem;
  color: #adb5bd;
  margin-bottom: 4px;
}

.empty-text {
  font-size: 0.98rem;
  color: #666;
}

.btn-discover {
  margin-top: 10px;
  display: inline-block;
  background-color: #1a507a;
  color: #ffffff;
  padding: 7px 18px;
  border-radius: 4px;
  text-decoration: none;
  font-size: 0.88rem;
  font-weight: 500;
  transition: background-color 0.2s;
}

.btn-discover:hover {
  background-color: #133c5c;
  color: #fff;
}

/* Danh sách dòng thành viên */
.following-list {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.following-item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid #edf0f5;
  transition: background-color 0.15s ease;
  width: 100%;
  box-sizing: border-box;
}

.following-item-row:last-child {
  border-bottom: none;
}

.following-item-row:hover {
  background-color: #fafbfc;
}

.following-left-col {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.following-avatar-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  color: #ffffff;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  cursor: pointer;
  overflow: hidden;
  background-color: #1a507a;
  flex-shrink: 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.following-avatar-circle img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.following-info-col {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  flex: 1;
}

.following-name-row {
  display: flex;
  align-items: center;
}

.following-display-name {
  font-weight: 600;
  font-size: 1.05rem;
  color: #1a507a;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.15s;
}

.following-display-name:hover {
  text-decoration: underline;
  color: #0f3554;
}

.following-rank-row {
  font-size: 0.85rem;
  color: #666;
}

.following-stats-row {
  font-size: 0.82rem;
  color: #777;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Nút Bỏ theo dõi */
.following-right-col {
  flex-shrink: 0;
  margin-left: 16px;
}

.btn-unfollow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background-color: #ffffff;
  border: 1px solid #d8dbe0;
  border-radius: 4px;
  color: #495057;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-unfollow:hover:not(:disabled) {
  background-color: #fff5f5;
  border-color: #f5c2c7;
  color: #dc3545;
}

.btn-unfollow:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-unfollow i {
  font-size: 0.85rem;
}

/* Phân trang */
.following-pagination-wrapper {
  padding: 14px 18px;
  background: #fdfdfd;
  border-top: 1px solid #edf0f5;
  display: flex;
  justify-content: center;
  width: 100%;
  box-sizing: border-box;
}

/* Responsive Tablet & Mobile */
@media (max-width: 992px) {
  .account-layout {
    flex-direction: column;
    align-items: stretch;
    gap: 0;
    width: 100%;
  }

  .account-content {
    width: 100%;
    min-width: 100%;
  }

  .following-card {
    width: 100%;
  }
}

@media (max-width: 576px) {
  .following-item-row {
    padding: 12px 14px;
    gap: 10px;
  }

  .following-left-col {
    gap: 10px;
  }

  .following-avatar-circle {
    width: 42px;
    height: 42px;
    font-size: 1rem;
  }

  .following-display-name {
    font-size: 0.98rem;
  }

  .following-rank-row {
    font-size: 0.8rem;
  }

  .following-stats-row {
    white-space: normal;
    font-size: 0.78rem;
    line-height: 1.35;
  }

  .following-right-col {
    margin-left: 8px;
  }

  .btn-unfollow {
    padding: 5px 10px;
    font-size: 0.8rem;
    gap: 4px;
  }
}
</style>
