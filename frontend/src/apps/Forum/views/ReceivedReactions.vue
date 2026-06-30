<template>
  <div>
    <main class="container" style="padding-top: 2rem;">
      <!-- Breadcrumb -->
      <Breadcrumb :items="breadcrumbItems" />

      <div class="account-layout">
        <!-- Cột trái: Sidebar -->
        <AccountSidebar activeMenu="reactions-received" />

        <!-- Cột phải: Content chính -->
        <div class="account-content">
          <!-- Card thống kê reaction -->
          <div class="reactions-stats-card card">
            <!-- Tab bar -->
            <div class="reaction-tabs">
              <div 
                class="reaction-tab" 
                :class="{ 'is-active': activeTab === null }"
                @click="changeTab(null)"
                style="--tab-color: #1a507a;"
              >
                Tất cả ({{ totalCount }})
              </div>
              <div 
                v-for="item in sortedSummaries" 
                :key="item.reactionIcon.id"
                class="reaction-tab"
                :class="{ 'is-active': activeTab === item.reactionIcon.id }"
                @click="changeTab(item.reactionIcon.id)"
                :style="`--tab-color: ${item.reactionIcon.color || '#1a507a'};`"
              >
                <ReactionIcon 
                  :code="item.reactionIcon.icon"
                  :color="item.reactionIcon.color"
                  size="20px"
                  style="margin-right: 6px;"
                />
                {{ item.reactionIcon.tooltip }} ({{ item.count }})
              </div>
            </div>

            <!-- List chi tiết -->
            <div class="received-reactions-list">
              <div v-if="loading" class="reactions-loading">Đang tải...</div>
              <div v-else-if="reactions.length === 0" class="reactions-empty">Không có lượt tương tác nào.</div>
              <template v-else>
                <div v-for="item in reactions" :key="item.id" class="reaction-row-item">
                  <!-- Cột bên trái: avatar người tương tác -->
                  <div class="reactor-avatar-col">
                    <span class="reactor-avatar" :style="!isAvatarUrl(item.actor.avatar) ? { backgroundColor: item.actor.avatar || '#ccc' } : {}">
                      <img v-if="isAvatarUrl(item.actor.avatar)" :src="item.actor.avatar" />
                      <template v-else>
                        {{ (item.actor.displayName || item.actor.username).charAt(0).toUpperCase() }}
                      </template>
                    </span>
                  </div>
                  
                  <!-- Cột bên phải: chi tiết lượt tương tác -->
                  <div class="reaction-detail-col">
                    <!-- Dòng 1 -->
                    <div class="reaction-msg-line">
                      <span class="reactor-name">{{ item.actor.displayName || item.actor.username }}</span>
                      đã tương tác với bạn trong chủ đề
                      <span v-if="item.threadLabel" class="label-tag-mini" :style="{ backgroundColor: item.threadLabel.colorCode, color: item.threadLabel.textColor, borderColor: item.threadLabel.borderColor || 'transparent' }">
                        {{ item.threadLabel.name }}
                      </span>
                      <router-link :to="getPostDetailLink(item)" class="thread-title-link">
                        {{ item.threadTitle }}
                      </router-link>
                      với biểu cảm
                      <ReactionIcon 
                        :code="item.reactionIcon.icon"
                        :color="item.reactionIcon.color"
                        size="18px"
                        style="margin: 0 4px; display: inline-flex; vertical-align: middle;"
                      />
                      <span :style="{ color: item.reactionIcon.color || '#1a507a', fontWeight: 'bold' }">{{ item.reactionIcon.tooltip }}</span>.
                    </div>
                    
                    <!-- Dòng 2: Nội dung của bài viết/bình luận được thả reaction -->
                    <div class="reacted-content-line">
                      {{ stripHtml(item.content) }}
                    </div>
                    
                    <!-- Dòng 3: Thời gian của lượt tương tác/reaction -->
                    <div class="reaction-time-line">
                      {{ formatDate(item.interactedAt) }}
                    </div>
                  </div>
                </div>
              </template>
            </div>

            <!-- Phân trang -->
            <div class="pagination-container" v-if="totalPages > 1">
              <ForumPagination 
                :current-page="currentPage" 
                :total-pages="totalPages" 
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
import ReactionIcon from '@/shared/components/ReactionIcon.vue'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import AccountSidebar from '@/shared/components/AccountSidebar.vue'
import reactionService from '@/apps/Forum/services/reaction.service'
import { formatForumDate } from '@/shared/utils/date'
import { isAvatarUrl } from '@/shared/utils/utils'

export default {
  name: 'ReceivedReactions',
  components: {
    Breadcrumb,
    ReactionIcon,
    ForumPagination,
    AccountSidebar
  },
  data() {
    return {
      activeTab: null,
      reactionSummaries: [],
      reactions: [],
      loading: true,
      currentPage: 1,
      totalPages: 1,
      itemsPerPage: 10
    }
  },
  computed: {
    breadcrumbItems() {
      return [
        { title: 'Tài khoản của bạn' },
        { title: 'Điểm tương tác nhận được' }
      ]
    },
    totalCount() {
      return this.reactionSummaries.reduce((acc, curr) => acc + curr.count, 0)
    },
    sortedSummaries() {
      // Sắp xếp các tab theo thứ tự lượt tương tác từ lớn đến bé
      return [...this.reactionSummaries].sort((a, b) => b.count - a.count)
    }
  },
  mounted() {
    this.fetchData()
    window.addEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  beforeUnmount() {
    window.removeEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  methods: {
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    handleAvatarUpdated(event) {
      const { username, avatar } = event.detail
      this.reactions = this.reactions.map(item => {
        if (item.actor && item.actor.username === username) {
          return { ...item, actor: { ...item.actor, avatar } }
        }
        return item
      })
    },
    async fetchData() {
      this.loading = true
      try {
        const [summaryRes, reactionsRes] = await Promise.all([
          reactionService.getReceivedReactionsSummary(),
          reactionService.getReceivedReactions({
            iconId: this.activeTab || '',
            page: this.currentPage - 1,
            size: this.itemsPerPage
          })
        ])
        
        this.reactionSummaries = summaryRes.data || []
        if (reactionsRes.data) {
          this.reactions = reactionsRes.data.content || []
          this.totalPages = reactionsRes.data.totalPages || 1
        }
      } catch (e) {
        console.error('Lỗi khi tải dữ liệu tương tác:', e)
        this.reactions = []
        this.totalPages = 1
      } finally {
        this.loading = false
      }
    },
    async fetchReactionsOnly() {
      this.loading = true
      try {
        const res = await reactionService.getReceivedReactions({
          iconId: this.activeTab || '',
          page: this.currentPage - 1,
          size: this.itemsPerPage
        })
        if (res.data) {
          this.reactions = res.data.content || []
          this.totalPages = res.data.totalPages || 1
        }
      } catch (e) {
        console.error('Lỗi khi tải trang danh sách reaction:', e)
        this.reactions = []
        this.totalPages = 1
      } finally {
        this.loading = false
      }
    },
    changeTab(tabId) {
      this.activeTab = tabId
      this.currentPage = 1
      this.fetchReactionsOnly()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchReactionsOnly()
    },
    formatDate(dateStr) {
      return formatForumDate(dateStr)
    },
    stripHtml(html) {
      if (!html) return ''
      // Loại bỏ thẻ HTML để tránh vỡ giao diện tóm tắt
      let text = html.replace(/<[^>]*>/g, '')
      // Giải mã các ký tự thực thể cơ bản
      text = text.replace(/&nbsp;/g, ' ')
                 .replace(/&amp;/g, '&')
                 .replace(/&lt;/g, '<')
                 .replace(/&gt;/g, '>')
      if (text.length > 200) {
        return text.substring(0, 200) + '...'
      }
      return text
    },
    getPostDetailLink(item) {
      if (item.postId) {
        return {
          name: 'ThreadDetail',
          params: { id: item.threadId },
          query: { postId: item.postId }
        }
      } else {
        return {
          name: 'ThreadDetail',
          params: { id: item.threadId },
          query: { postId: 'main_thread_entry' }
        }
      }
    }
  }
}
</script>

<style scoped>
.account-layout {
  display: flex;
  gap: 1.5rem;
  margin-top: 16px;
  align-items: flex-start;
}

.account-content {
  flex: 1;
  min-width: 0;
}

.reaction-tabs {
  display: flex;
  border-bottom: 1px solid #ddd;
  padding: 0 15px;
  background: #fdfdfd;
  overflow-x: auto;
  border-top-left-radius: 4px;
  border-top-right-radius: 4px;
}

.reaction-tab {
  padding: 12px 15px 9px;
  cursor: pointer;
  font-weight: 500;
  color: #1877f2;
  border-bottom: 3px solid transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  transition: all 0.2s ease;
  user-select: none;
}

.reaction-tab:hover {
  background-color: #f0f2f5;
}

.reaction-tab.is-active {
  border-bottom-color: var(--tab-color, #1877f2);
  color: var(--tab-color, #1877f2);
  font-weight: bold;
}

.received-reactions-list {
  display: flex;
  flex-direction: column;
}

.reaction-row-item {
  display: flex;
  padding: 15px;
  border-bottom: 1px solid #eceef1;
}

.reaction-row-item:last-child {
  border-bottom: none;
}

.reactor-avatar-col {
  margin-right: 15px;
  flex-shrink: 0;
}

.reactor-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
  font-size: 1.25rem;
}

.reaction-detail-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 0.95rem;
}

.reaction-msg-line {
  color: #141414;
  line-height: 1.4;
}

.reactor-name {
  font-weight: bold;
  color: #1a507a;
}

.thread-title-link {
  color: #1a507a;
  text-decoration: none;
  font-weight: 500;
}

.thread-title-link:hover {
  color: #d13838;
  text-decoration: underline;
}

.label-tag-mini {
  display: inline-block;
  padding: 1px 6px;
  font-size: 0.75rem;
  font-weight: bold;
  border-radius: 2px;
  margin: 0 4px;
  border: 1px solid transparent;
}

.reacted-content-line {
  color: #141414;
  font-size: 0.85rem;
  white-space: pre-line;
  word-break: break-word;
}

.reaction-time-line {
  color: #8c8c8c;
  font-size: 0.8rem;
}

.reactions-loading, .reactions-empty {
  padding: 30px;
  text-align: center;
  color: #8c8c8c;
}

.pagination-container {
  padding: 15px;
  border-top: 1px solid #eceef1;
  background: #fdfdfd;
  border-bottom-left-radius: 4px;
  border-bottom-right-radius: 4px;
}

@media (max-width: 992px) {
  .account-layout {
    flex-direction: column;
    gap: 0;
  }

  .account-content {
    width: 100%;
  }
}
</style>
