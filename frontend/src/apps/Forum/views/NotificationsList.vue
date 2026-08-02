<template>
  <div>
    <main class="container" style="padding-top: 2rem;">
      <!-- Breadcrumb -->
      <Breadcrumb :items="breadcrumbItems" />

      <div class="account-layout">
        <!-- Cột trái: Sidebar -->
        <AccountSidebar activeMenu="notifications" />

        <!-- Cột phải: Content chính -->
        <div class="account-content">
          <div class="notifications-header-row">
            <h1 class="page-title"></h1>
            <button class="btn-mark-all-read" @click="markAllAsRead" :disabled="loading">
              Đánh dấu đã xem
            </button>
          </div>

          <div class="notifications-list-card card">
            <div v-if="loading" class="notifications-loading">Đang tải...</div>
            <div v-else-if="notifications.length === 0" class="notifications-empty">Không có thông báo nào.</div>
            <template v-else>
              <div 
                v-for="notif in notifications" 
                :key="notif.id" 
                class="notif-item" 
                :class="{ 'unread': !notif.isRead }"
                @click="handleNotifClick(notif)"
              >
                <!-- Cột bên trái: avatar người gửi -->
                <div class="notif-avatar-wrapper">
                  <user-profile-popup :user="getNotifUser(notif)" v-if="getNotifUser(notif)">
                    <div class="notif-avatar" :style="!isAvatarUrl(notif.actorAvatar) ? { backgroundColor: notif.actorAvatar || '#3498db' } : {}">
                      <img v-if="isAvatarUrl(notif.actorAvatar)" :src="notif.actorAvatar" />
                      <template v-else>
                        {{ (notif.actorDisplayName || notif.actorUsername || '?').charAt(0).toUpperCase() }}
                      </template>
                    </div>
                  </user-profile-popup>
                  <div v-else class="notif-avatar" style="background-color: #ccc; color: #fff;">?</div>
                </div>

                <!-- Cột bên phải: nội dung thông báo -->
                <div class="notif-body">
                  <div class="notif-text">
                    <strong>{{ notif.actorDisplayName || notif.actorUsername }}</strong>
                    <VerifiedBadge :user="getNotifUser(notif)" size="16px" />
                    <template v-if="notif.type === 'REACTION'">
                      đã tương tác <ReactionIcon :code="notif.reactionIcon" :color="notif.reactionColor" size="18px" style="display:inline-flex;vertical-align:middle;" /> 
                      <strong :style="{ color: notif.reactionColor || '#2c3e50' }">{{ notif.reactionName }}</strong>
                      với bài viết của bạn trong chủ đề
                    </template>
                    <template v-else-if="notif.type === 'QUOTE'">
                      đã trích bài viết của bạn trong chủ đề
                    </template>
                    <template v-else-if="notif.type === 'MENTION'">
                      đã tag bạn trong chủ đề
                    </template>
                    <template v-else-if="notif.type === 'FOLLOWED_USER_THREAD'">
                      đã đăng một chủ đề mới là
                    </template>
                    <template v-else-if="notif.type === 'FOLLOWED_USER_POST'">
                      đã trả lời vào chủ đề
                    </template>
                    <template v-else>
                      đã trả lời vào chủ đề
                    </template>
                    <span class="notif-link-block" @click.stop="handleNotifClick(notif)">
                      <span v-if="notif.threadLabelName" class="notif-label-tag" :style="{ backgroundColor: notif.type === 'MENTION' ? '#2577b1' : (notif.threadLabelColor || '#95a5a6'), color: notif.type === 'MENTION' ? '#fff' : (notif.threadLabelTextColor || '#fff'), borderColor: notif.type === 'MENTION' ? 'transparent' : (notif.threadLabelBorderColor || 'transparent') }">{{ notif.threadLabelName }}</span>
                      <span class="highlight-thread">{{ notif.threadTitle }}</span>
                      <span v-if="!notif.isRead" class="unread-dot"></span>
                    </span>.
                    <span v-if="notif.type !== 'QUOTE' && notif.type !== 'REACTION' && notif.type !== 'MENTION' && notif.type !== 'FOLLOWED_USER_THREAD' && notif.type !== 'FOLLOWED_USER_POST'" class="notif-extra">Có thể có bài viết thêm trong chủ đề</span>
                  </div>
                  <div class="notif-time">{{ formatDate(notif.createdAt) }}</div>
                </div>
              </div>
            </template>
          </div>

          <!-- Phân trang -->
          <div class="pagination-container" v-if="totalPages > 1 && !loading">
            <ForumPagination 
              :current-page="currentPage" 
              :total-pages="totalPages" 
              @page-changed="handlePageChange"
            />
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import AccountSidebar from '@/shared/components/AccountSidebar.vue'
import ReactionIcon from '@/shared/components/ReactionIcon.vue'
import UserProfilePopup from '@/shared/components/UserProfilePopup.vue'
import VerifiedBadge from '@/shared/components/VerifiedBadge.vue'
import notificationService from '@/apps/Forum/services/notification.service'
import { formatForumDate } from '@/shared/utils/date'
import { isAvatarUrl } from '@/shared/utils/utils'
import userMixin from '@/shared/mixins/user.mixin.js'
import { alertSuccess } from '@/shared/utils/swal'

export default {
  name: 'NotificationsList',
  mixins: [userMixin],
  components: {
    Breadcrumb,
    ForumPagination,
    AccountSidebar,
    ReactionIcon,
    UserProfilePopup,
    VerifiedBadge
  },
  data() {
    return {
      notifications: [],
      loading: true,
      currentPage: 1,
      totalPages: 1,
      itemsPerPage: 10
    }
  },
  computed: {
    breadcrumbItems() {
      return [
        { title: 'Tài khoản của bạn', to: this.currentUserProfileLink },
        { title: 'Thông báo', active: true }
      ]
    }
  },
  mounted() {
    this.fetchNotifications()
    window.addEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  beforeUnmount() {
    window.removeEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  methods: {
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    getNotifUser(notif) {
      if (!notif) return null
      return {
        username: notif.actorUsername,
        displayName: notif.actorDisplayName,
        avatar: notif.actorAvatar,
        isVerifiedBadge: notif.actorIsVerifiedBadge || notif.isVerifiedBadge
      }
    },
    handleAvatarUpdated(event) {
      const { username, avatar } = event.detail
      this.notifications = this.notifications.map(item => {
        if (item.actorUsername === username) {
          return { ...item, actorAvatar: avatar }
        }
        return item
      })
    },
    async fetchNotifications() {
      this.loading = true
      try {
        const res = await notificationService.getPage({
          page: this.currentPage - 1,
          size: this.itemsPerPage
        })
        if (res.data) {
          this.notifications = res.data.content || []
          this.totalPages = res.data.totalPages || 1
        }
      } catch (e) {
        console.error('Lỗi khi tải danh sách thông báo:', e)
        this.notifications = []
        this.totalPages = 1
      } finally {
        this.loading = false
      }
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchNotifications()
    },
    async markAllAsRead() {
      if (this.notifications.length === 0) return
      try {
        await notificationService.markAllRead()
        this.notifications.forEach(n => n.isRead = true)
        window.dispatchEvent(new CustomEvent('notifications-updated'))
        alertSuccess('Đã đánh dấu toàn bộ thông báo là đã xem.')
      } catch (e) {
        console.error('Lỗi khi đánh dấu đã xem toàn bộ:', e)
      }
    },
    async handleNotifClick(notif) {
      if (!notif.isRead) {
        notif.isRead = true
        try {
          await notificationService.markAsRead(notif.id)
          window.dispatchEvent(new CustomEvent('notifications-updated'))
        } catch (e) {
          console.error(e)
        }
      }

      window.dispatchEvent(new CustomEvent('notification-clicked', {
        detail: {
          threadId: notif.threadId,
          postId: notif.postId
        }
      }))

      const routeTarget = {
         name: 'ThreadDetail',
         params: { id: notif.threadId }
      }
      
      if (notif.postId) {
         routeTarget.query = { postId: notif.postId }
      } else {
         routeTarget.query = { postId: 'main_thread_entry' }
      }
      
      this.$router.push(routeTarget)
    },
    formatDate(dateStr) {
      return formatForumDate(dateStr)
    }
  }
}
</script>

<style scoped>
.notifications-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.page-title {
  font-size: 1.75rem;
  color: #1a507a;
  margin: 0;
  font-weight: 500;
}

.btn-mark-all-read {
  background-color: #ffffff;
  border: 1px solid #d8dbe0;
  border-radius: 4px;
  padding: 6px 12px;
  font-size: 0.85rem;
  color: #1a507a;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.btn-mark-all-read:hover:not(:disabled) {
  background-color: #f0f4f8;
  border-color: #1a507a;
  color: #d13838;
}

.btn-mark-all-read:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.notifications-list-card {
  background: #ffffff;
  border: 1px solid #d8dbe0;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.notifications-loading,
.notifications-empty {
  padding: 30px;
  text-align: center;
  color: #7f8c8d;
  font-size: 0.95rem;
}

.notif-item {
  display: flex;
  gap: 15px;
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
  align-items: flex-start;
}

.notif-item:last-child {
  border-bottom: none;
}

.notif-item:hover {
  background: #f5f8fa;
}

.notif-item.unread {
  background: #f0f7fb;
}

.notif-avatar-wrapper {
  flex-shrink: 0;
}

.notif-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
  font-size: 1rem;
  overflow: hidden;
}

.notif-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.notif-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 0.95rem;
  color: #4f5d73;
  line-height: 1.45;
  padding-right: 25px;
}

.notif-text strong {
  color: #2c3e50;
  font-weight: 600;
}

.highlight-thread {
  color: #2577b1;
  font-weight: 500;
  cursor: pointer;
}

.highlight-thread:hover {
  text-decoration: underline;
}

.notif-link-block {
  display: inline;
  cursor: pointer;
}

.notif-link-block:hover .highlight-thread {
  text-decoration: underline !important;
}

.notif-label-tag {
  display: inline-block;
  padding: 1px 6px;
  font-size: 0.75rem;
  border-radius: 3px;
  color: #fff;
  margin: 0 4px;
  vertical-align: middle;
  line-height: 1.4;
  border: 1px solid transparent;
}

.notif-extra {
  display: block;
  font-size: 0.82rem;
  color: #7f8c8d;
  margin-top: 3px;
}

.notif-time {
  font-size: 0.8rem;
  color: #8c98a5;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background-color: #2577b1;
  border-radius: 50%;
  display: inline-block;
  margin-left: 8px;
  flex-shrink: 0;
  vertical-align: middle;
}

.pagination-container {
  margin-top: 1.5rem;
  display: flex;
  justify-content: center;
}

.account-layout {
  display: flex;
  gap: 2rem;
  margin-bottom: 3rem;
}

.account-content {
  flex: 1;
  min-width: 0;
}

@media (max-width: 992px) {
  .account-layout {
    flex-direction: column;
    gap: 0;
  }
}
</style>
