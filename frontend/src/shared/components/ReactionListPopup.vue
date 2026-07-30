<template>
  <BaseModal
    :show="show"
    @update:show="$emit('update:show', $event)"
    @close="$emit('close')"
    :title="`Thành viên đã phản ứng với tin nhắn ${orderNumber}`"
    cardClass="reaction-popup-card"
    :showCloseButton="true"
    :closeOnOverlay="true"
  >
    <div class="reaction-popup-body">
      <!-- Tabs -->
      <div class="reaction-tabs">
        <div 
          class="reaction-tab" 
          :class="{ 'is-active': activeTab === null }"
          @click="changeTab(null)"
          style="--tab-color: #47a7eb;"
        >
          Tất cả ({{ totalCount }})
        </div>
        <div 
          v-for="item in topTabs" 
          :key="item.reactionIcon.id"
          class="reaction-tab"
          :class="{ 'is-active': activeTab === item.reactionIcon.id }"
          @click="changeTab(item.reactionIcon.id)"
          :style="`--tab-color: ${item.reactionIcon.color || '#47a7eb'};`"
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

      <!-- List -->
      <div class="reaction-list-content">
        <div v-if="loading" class="reaction-loading">Đang tải...</div>
        <div v-else-if="participants.length === 0" class="reaction-empty">Không có dữ liệu.</div>
        <template v-else>
          <div v-for="participant in participants" :key="participant.user.id" class="reaction-item">
            <div class="reactor-info">
              <user-profile-popup :user="participant.user" v-if="participant.user">
                <div class="avatar-circle" :style="!isAvatarUrl(participant.user.avatar) ? { backgroundColor: participant.user.avatar || '#ccc' } : {}">
                  <img v-if="isAvatarUrl(participant.user.avatar)" :src="participant.user.avatar" />
                  <template v-else>
                    {{ (participant.user.displayName || participant.user.username).charAt(0).toUpperCase() }}
                  </template>
                </div>
              </user-profile-popup>
              <div v-else class="avatar-circle" style="background-color: #ccc; color: #fff;">?</div>
              <div class="reactor-details">
                <user-profile-popup :user="participant.user" v-if="participant.user">
                  <div class="reactor-name font-weight-bold cursor-pointer">
                    {{ participant.user.displayName || participant.user.username }} <VerifiedBadge :user="participant.user" size="14px" />
                  </div>
                </user-profile-popup>
                <div v-else class="reactor-name">Ẩn danh</div>
                <div class="reactor-title">{{ participant.user?.displayTitle || 'Thành viên' }}</div>
                <div class="reactor-stats" v-if="participant.user">
                  Bài viết: {{ formatNumber(participant.user.postCount) }} · Điểm tương tác: {{ formatNumber(participant.user.interactionPoints) }} · Điểm: {{ formatNumber(participant.user.trophyPoints) }}
                </div>
              </div>
            </div>
            <div class="reaction-meta">
              <ReactionIcon 
                :code="participant.reactionIcon.icon"
                :color="participant.reactionIcon.color"
                size="20px"
                class="reaction-icon-right"
              />
              <div class="reaction-time">{{ formatDate(participant.interactedAt) }}</div>
            </div>
          </div>
        </template>
      </div>

      <!-- Pagination -->
      <div class="reaction-pagination-wrapper" v-if="totalPages > 1">
        <ForumPagination 
          :current-page="currentPage" 
          :total-pages="totalPages" 
          @page-changed="handlePageChange"
        />
      </div>
    </div>
  </BaseModal>
</template>

<script>
import BaseModal from './BaseModal.vue'
import ReactionIcon from './ReactionIcon.vue'
import ForumPagination from './ForumPagination.vue'
import UserProfilePopup from './UserProfilePopup.vue'
import VerifiedBadge from './VerifiedBadge.vue'
import reactionService from '@/apps/Forum/services/reaction.service'
import { formatForumDate } from '@/shared/utils/date'
import { isAvatarUrl } from '@/shared/utils/utils'

export default {
  name: 'ReactionListPopup',
  components: {
    BaseModal,
    ReactionIcon,
    ForumPagination,
    UserProfilePopup,
    VerifiedBadge
  },
  props: {
    show: {
      type: Boolean,
      default: false
    },
    orderNumber: {
      type: String,
      default: '#1'
    },
    targetId: {
      default: null
    },
    isMainPost: {
      type: Boolean,
      default: false
    },
    summary: {
      type: Array,
      default: () => []
    },
    type: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      activeTab: null,
      participants: [],
      loading: false,
      currentPage: 1,
      totalPages: 1,
      itemsPerPage: 10
    }
  },
  computed: {
    totalCount() {
      if (!this.summary) return 0
      return this.summary.reduce((acc, curr) => acc + curr.count, 0)
    },
    topTabs() {
      if (!this.summary) return []
      return [...this.summary]
        .sort((a, b) => b.count - a.count)
    }
  },
  watch: {
    show(newVal) {
      if (newVal) {
        this.activeTab = null
        this.currentPage = 1
        this.fetchParticipants()
      }
    }
  },
  methods: {
    changeTab(tabId) {
      this.activeTab = tabId
      this.currentPage = 1
      this.fetchParticipants()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchParticipants()
    },
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    formatDate(dateStr) {
      return formatForumDate(dateStr)
    },
    formatNumber(val) {
      if (val === null || val === undefined) return '0'
      return val.toLocaleString('vi-VN')
    },
    async fetchParticipants() {
      this.loading = true
      try {
        let type = this.type
        if (!type) {
          type = this.isMainPost ? 'threads' : 'posts'
        }
        const params = {
          page: this.currentPage - 1,
          size: this.itemsPerPage
        }
        if (this.activeTab !== null) {
          params.iconId = this.activeTab
        }
        
        const res = await reactionService.getParticipants(type, this.targetId, params)
        if (res.data) {
          this.participants = res.data.content || []
          this.totalPages = res.data.totalPages || 1
        }
      } catch (e) {
        console.error('Error fetching reaction participants:', e)
        this.participants = []
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
:deep(.reaction-popup-card) {
  width: 600px;
  max-width: calc(100% - 30px);
}

.reaction-popup-body {
  display: flex;
  flex-direction: column;
  background: #fff;
}

.reaction-tabs {
  display: flex;
  border-bottom: 1px solid #ddd;
  padding: 0 15px;
  background: #fdfdfd;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.reaction-tab {
  padding: 6px 10px 3px;
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
  flex-shrink: 0;
}

.reaction-tab:hover {
  background-color: #f0f2f5;
}

.reaction-tab.is-active {
  border-bottom-color: var(--tab-color);
  color: var(--tab-color);
}

.reaction-list-content {
  max-height: 400px;
  overflow-y: auto;
  padding: 10px 0;
}

.reaction-loading, .reaction-empty {
  padding: 20px;
  text-align: center;
  color: #65676b;
}

.reaction-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #f0f2f5;
}

.reaction-item:last-child {
  border-bottom: none;
}

.reactor-info {
  display: flex;
  align-items: center;
}

.avatar-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
  font-size: 1.2rem;
  margin-right: 12px;
}

.reactor-name {
  font-weight: bold;
  color: #1a507a;
  font-size: 0.95rem;
}

.reactor-title {
  font-size: 0.8rem;
  color: #65676b;
  margin-top: 2px;
}

.reactor-stats {
  font-size: 0.78rem;
  color: #7f8c8d;
  margin-top: 2px;
}

.reaction-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.reaction-icon-right {
  margin-bottom: 4px;
}

.reaction-time {
  font-size: 0.8rem;
  color: #65676b;
}

.reaction-pagination-wrapper {
  padding: 15px;
  border-top: 1px solid #eee;
  background: #fdfdfd;
}

/* Custom header colors to match standard forum */
:deep(.card-header) {
  background: #e9f0f5;
  color: #1a507a;
  border-bottom: 1px solid #cfdce8;
  font-weight: 400;
  text-align: left;
  font-size: 1.1rem;
}

:deep(.close-btn) {
  color: #1a507a;
}
:deep(.close-btn:hover) {
  color: #133a59;
}
</style>
