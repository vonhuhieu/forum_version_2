<template>
  <div>
    <main class="container" style="padding-top: 2rem;">
      <div v-if="loading" style="text-align: center; padding: 3rem;">Đang tải...</div>
      
      <div v-else>
        <!-- Block 1: Breadcrumb -->
        <Breadcrumb :items="breadcrumbItems" />

        <!-- Block 2: Danh sách đối thoại -->
        <div class="card" style="margin-bottom: 2rem;">
          <div class="card-header" style="display: flex; justify-content: space-between; align-items: center;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <span>Đối thoại</span>
            </div>
            <button class="btn-post-thread" @click="goToCreateConversation">Bắt đầu đối thoại</button>
          </div>

          <!-- Phân trang trên -->
          <div class="pagination-wrapper" style="padding: 1rem; border-top: 1px solid #eee;">
            <ForumPagination 
              :current-page="currentPage" 
              :total-pages="totalPages" 
              @page-changed="currentPage = $event"
            />
          </div>

          <!-- Danh sách item đối thoại -->
          <div class="thread-list">
            <div v-for="convo in paginatedConversations" :key="convo.id" class="thread-row thread-row-center min-height-100-on-pc" @click="goToConversation($event, convo)">
              
              <!-- Block trái (Avatar) -->
              <user-profile-popup :user="getConvoCreator(convo)" v-if="getConvoCreator(convo)">
                <div class="thread-avatar" :style="!isAvatarUrl(convo.creatorAvatar) ? { backgroundColor: convo.creatorAvatar || '#ccc', color: '#fff' } : {}">
                  <img v-if="isAvatarUrl(convo.creatorAvatar)" :src="convo.creatorAvatar" />
                  <template v-else>
                    {{ (convo.creatorDisplayName || convo.creatorUsername || 'C').charAt(0).toUpperCase() }}
                  </template>
                </div>
              </user-profile-popup>
              <div v-else class="thread-avatar" style="background-color: #ccc; color: #fff;">C</div>

              <!-- Block chính (Main) -->
              <div class="thread-main">
                <div class="thread-title">
                  <router-link 
                    :to="convo.lastMessageId ? { name: 'ConversationDetail', params: { id: convo.id }, query: { messageId: convo.lastMessageId } } : { name: 'ConversationDetail', params: { id: convo.id } }"
                    :style="{ fontWeight: !convo.isRead ? 'bold' : 'normal' }"
                  >
                    {{ convo.title }}
                    <span v-if="!convo.isRead" class="unread-dot"></span>
                  </router-link>
                </div>
                
                <div class="thread-meta">
                  <span class="author-name white-space-nowrap">{{ convo.creatorDisplayName || convo.creatorUsername || 'Ẩn danh' }}</span>
                  <span class="dot-divider">•</span>
                  <router-link :to="{ name: 'ConversationDetail', params: { id: convo.id } }" class="meta-link">
                    {{ formatDate(convo.createdAt) }}
                  </router-link>
                  
                  <!-- Thành phần phân trang nhanh cho từng item đối thoại -->
                  <span class="quick-pages" v-if="getConvoPages(convo.replyCount).length > 0">
                    <router-link 
                      v-for="p in getConvoPages(convo.replyCount)" 
                      :key="p" 
                      :to="{ name: 'ConversationDetail', params: { id: convo.id }, query: { page: p } }"
                      class="page-badge"
                    >
                      {{ p }}
                    </router-link>
                  </span>
                </div>
              </div>

              <!-- Block giữa (Thống kê) -->
              <div class="thread-stats">
                <div class="stat-block">
                  <span class="stat-label">Trả lời:</span>
                  <span class="stat-value">{{ convo.replyCount || 0 }}</span>
                </div>
                <div class="stat-block">
                  <span class="stat-label">Người tham gia:</span>
                  <span class="stat-value">{{ convo.participantCount || 0 }}</span>
                </div>
              </div>

              <!-- Block phải (Tin nhắn cuối) -->
              <div class="thread-last-post">
                <div class="last-post-info">
                  <router-link 
                    :to="convo.lastMessageId ? { name: 'ConversationDetail', params: { id: convo.id }, query: { messageId: convo.lastMessageId } } : { name: 'ConversationDetail', params: { id: convo.id } }" 
                    class="last-post-time-link"
                  >
                    {{ formatDate(convo.updatedAt) }}
                  </router-link>
                  <span class="last-post-author">
                    {{ convo.lastMessageSenderDisplayName || convo.lastMessageSenderUsername || 'Ẩn danh' }}
                  </span>
                </div>
                <user-profile-popup :user="getConvoLastSender(convo)" v-if="getConvoLastSender(convo)">
                  <div class="last-post-avatar" :style="!isAvatarUrl(convo.lastMessageSenderAvatar) ? { backgroundColor: convo.lastMessageSenderAvatar || '#ccc', color: '#fff' } : {}">
                    <img v-if="isAvatarUrl(convo.lastMessageSenderAvatar)" :src="convo.lastMessageSenderAvatar" />
                    <template v-else>
                      {{ (convo.lastMessageSenderDisplayName || convo.lastMessageSenderUsername || 'A').charAt(0).toUpperCase() }}
                    </template>
                  </div>
                </user-profile-popup>
                <div v-else class="last-post-avatar" style="background-color: #ccc; color: #fff;">A</div>
              </div>

            </div>

            <!-- Nếu không có cuộc đối thoại nào -->
            <div v-if="!conversations || conversations.length === 0"
              style="padding: 2rem; text-align: center; color: #999;">
              Chưa có cuộc đối thoại nào.
            </div>
          </div>
          
          <!-- Phân trang dưới -->
          <div class="pagination-wrapper" style="padding: 1rem; border-top: 1px solid #eee;">
            <ForumPagination 
              :current-page="currentPage" 
              :total-pages="totalPages" 
              @page-changed="currentPage = $event"
            />
          </div>

        </div>

        <!-- Block 3: Breadcrumb cuối -->
        <Breadcrumb :items="breadcrumbItems" />
      </div>
    </main>
  </div>
</template>

<script>
import conversationService from '@/apps/Forum/services/conversation.service'
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import UserProfilePopup from '@/shared/components/UserProfilePopup.vue'
import { formatForumDate } from '@/shared/utils/date'
import { isAvatarUrl } from '@/shared/utils/utils'

export default {
  name: 'ConversationList',
  components: {
    Breadcrumb,
    ForumPagination,
    UserProfilePopup
  },
  data() {
    return {
      conversations: [],
      loading: true,
      currentPage: 1,
      itemsPerPage: 10,
      totalPagesCount: 1,
      totalElements: 0
    }
  },
  watch: {
    currentPage: {
      async handler(newPage, oldPage) {
        if (newPage !== oldPage) {
          await this.fetchConversationsPaged()
        }
      }
    }
  },
  computed: {
    breadcrumbItems() {
      return [
        { title: 'Trang chủ', to: { name: 'Home' } },
        { title: 'Đối thoại' }
      ]
    },
    totalPages() {
      return this.totalPagesCount
    },
    paginatedConversations() {
      return this.conversations
    }
  },
  async mounted() {
    await this.fetchConversationsPaged()
    window.addEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  beforeUnmount() {
    window.removeEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  methods: {
    goToCreateConversation() {
      this.$router.push({ name: 'AddConversation' })
    },
    async fetchConversationsPaged() {
      this.loading = true
      const page = this.currentPage - 1
      const size = this.itemsPerPage
      try {
        const res = await conversationService.getPaged({ page, size })
        if (res.data && res.data.content) {
          this.conversations = res.data.content
          this.totalPagesCount = res.data.totalPages || 1
          this.totalElements = res.data.totalElements || 0
        } else {
          this.conversations = []
          this.totalPagesCount = 1
          this.totalElements = 0
        }
      } catch (error) {
        console.error('Lỗi khi tải danh sách đối thoại:', error)
      } finally {
        this.loading = false
      }
    },
    getConvoCreator(convo) {
      if (!convo) return null
      return {
        username: convo.creatorUsername,
        displayName: convo.creatorDisplayName,
        avatar: convo.creatorAvatar
      }
    },
    getConvoLastSender(convo) {
      if (!convo) return null
      return {
        username: convo.lastMessageSenderUsername,
        displayName: convo.lastMessageSenderDisplayName,
        avatar: convo.lastMessageSenderAvatar
      }
    },
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    handleAvatarUpdated(event) {
      const { username, avatar } = event.detail
      this.conversations = this.conversations.map(c => {
        const updated = { ...c }
        if (c.creatorUsername === username) {
          updated.creatorAvatar = avatar
        }
        if (c.lastMessageSenderUsername === username) {
          updated.lastMessageSenderAvatar = avatar
        }
        return updated
      })
    },
    formatDate(dateStr) {
      return formatForumDate(dateStr)
    },
    getConvoPages(replyCount) {
      const itemsPerPage = 10;
      const totalPages = Math.ceil((replyCount || 0) / itemsPerPage);
      
      if (totalPages <= 1) return [];
      if (totalPages === 2) return [2];
      if (totalPages === 3) return [2, 3];
      
      return [totalPages - 2, totalPages - 1, totalPages];
    },
    goToConversation(event, convo) {
      if (event.target.closest('a, button, .thread-avatar, .last-post-avatar, [role="button"]')) {
        return
      }
      const route = {
        name: 'ConversationDetail',
        params: { id: convo.id }
      }
      if (convo.lastMessageId) {
        route.query = { messageId: convo.lastMessageId }
      }
      this.$router.push(route)
    }
  }
}
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-start;
  align-items: center;
}

.thread-title {
  margin-bottom: 4px;
  display: block;
}

.thread-title a {
  text-decoration: none;
  color: #1a507a;
  font-weight: 500;
  font-size: 1.05rem;
  line-height: 1.5;
  display: inline-flex;
  align-items: center;
}

.thread-title a:hover {
  text-decoration: underline;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #3498db;
  display: inline-block;
  margin-left: 8px;
  flex-shrink: 0;
}

.thread-meta {
  font-size: 0.85rem;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  gap: 5px;
}

.dot-divider {
  font-size: 0.85rem;
  color: #bbb;
}

.meta-link {
  color: #8c8c8c;
  text-decoration: none;
  cursor: pointer;
}

.meta-link:hover {
  text-decoration: underline;
}

.quick-pages {
  display: inline-flex;
  gap: 4px;
  margin-left: 8px;
}

.page-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  border: 1px solid #e0e0e0;
  background-color: #f8f9fa;
  color: #666;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 3px;
  text-decoration: none;
  font-weight: normal;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-badge:hover {
  background-color: #1a507a;
  border-color: #1a507a;
  color: white;
  font-weight: bold;
}

.thread-last-post {
  width: 180px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 10px;
  text-align: right;
}

.last-post-info {
  display: flex;
  flex-direction: column;
}

.last-post-time-link {
  font-size: 0.85rem;
  color: #2980b9;
  text-decoration: none;
  cursor: pointer;
}

.last-post-time-link:hover {
  text-decoration: underline;
}

.last-post-author {
  font-size: 0.8rem;
  color: #444;
}

.last-post-avatar {
  width: 32px;
  height: 32px;
  background-color: #5c6bc0;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.85rem;
  flex-shrink: 0;
  border: 1px solid #dee2e6;
  margin-top: 3px;
}

@import "@/shared/assets/styles/custom.css";
</style>
