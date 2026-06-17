<template>
  <div class="convo-detail-page app-wrapper" v-if="!loading && conversation">
    <ForumHeader />

    <main class="container" style="padding-top: 2rem;">
      <Breadcrumb :items="breadcrumbItems" />

      <div class="convo-header card">
        <div class="convo-title-full">
          <h1>{{ conversation.title }}</h1>
        </div>
        <div class="convo-meta-bar">
          <div class="author-info">
            <svg class="meta-icon" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
            <span class="author-name">{{ participantListString }}</span>
            <span class="meta-dot">·</span>
            <svg class="meta-icon" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <polyline points="12 6 12 12 16 14"></polyline>
            </svg>
            <span class="post-time">{{ formatDate(conversation.createdAt) }}</span>
          </div>
        </div>
      </div>

      <!-- Action Buttons Row (Mockups) -->
      <div class="convo-actions-row">
        <button class="btn-action-placeholder" disabled>
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 1 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
          Sửa
        </button>
        <button class="btn-action-placeholder" disabled>
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
          Đánh dấu sao
        </button>
        <button class="btn-action-placeholder" disabled>
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline></svg>
          Đánh dấu là chưa đọc
        </button>
        <button class="btn-action-placeholder" disabled>
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
          Leave
        </button>
      </div>

      <!-- Main Layout: Left Messages, Right Sidebar -->
      <div class="convo-layout">
        
        <!-- Left Block: Messages & Reply -->
        <div class="convo-main">
          <!-- TOP PAGINATION -->
          <div class="pagination-wrapper-left" style="margin-bottom: 1rem;">
            <ForumPagination 
              v-if="totalPages > 1"
              :current-page="currentPage" 
              :total-pages="totalPages" 
              @page-changed="changePage"
            />
          </div>

          <div class="message-list">
            <div 
              v-for="(msg, index) in paginatedMessages" 
              :key="msg.id" 
              class="message-card card"
              :class="{ 'highlight-jump': String(msg.id) === String(highlightedMessageId) }"
              :id="'msg-' + msg.id"
            >
              <div class="post-layout">
                <div class="post-sidebar">
                  <div class="avatar-large" :style="{ backgroundColor: msg.sender?.avatar || '#ccc', color: '#fff' }">
                    {{ msg.sender ? (msg.sender.displayName || msg.sender.username).charAt(0).toUpperCase() : '?' }}
                  </div>
                  <div class="author-name-large">{{ msg.sender ? (msg.sender.displayName || msg.sender.username) : 'Ẩn danh' }}</div>
                  <div class="author-title">{{ getUserRoleText(msg.sender?.roles) }}</div>
                </div>

                <div class="post-main">
                  <div class="post-meta-top">
                    <span class="post-time-top">{{ formatDate(msg.createdAt) }}</span>
                    <div class="post-actions-top">
                      <span class="post-number">#{{ (currentPage - 1) * itemsPerPage + index + 1 }}</span>
                    </div>
                  </div>

                  <div class="content-body ql-editor" v-html="formatMessageContent(msg.content)"></div>

                  <div class="post-meta-bottom">
                    <div class="left-actions">
                      <a href="#" class="action-link" @click.prevent>Báo cáo</a>
                    </div>
                    <div class="right-actions">
                      <ReactionButton 
                        v-if="canShowReactionForMessage(msg)"
                        :targetId="msg.id"
                        type="message"
                        :allIcons="reactionIconsList"
                        :userReaction="msg.currentUserReaction"
                        @reaction-changed="fetchConversation(true)"
                      />
                      <a href="#" class="action-link reply-link" @click.prevent="quoteMessage(msg.sender ? (msg.sender.displayName || msg.sender.username) : 'Ẩn danh', msg.content, msg.id)">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 17 4 12 9 7"></polyline><path d="M20 18v-2a4 4 0 0 0-4-4H4"></path></svg>
                        Trả lời
                      </a>
                    </div>
                  </div>

                  <!-- Reaction Summary for Message (XenForo Style) -->
                  <div class="reactions-bar-container" v-if="msg.reactionSummary && msg.reactionSummary.length > 0">
                    <ReactionSummary 
                      :summary="msg.reactionSummary" 
                      :recentReactors="msg.recentReactors" 
                      @open-popup="openReactionPopup('#' + ((currentPage - 1) * itemsPerPage + index + 1), msg.id, msg.reactionSummary)"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- BOTTOM PAGINATION -->
          <div class="pagination-wrapper" v-if="totalPages > 1" style="margin-top: 1rem; margin-bottom: 1rem;">
            <ForumPagination 
              :current-page="currentPage" 
              :total-pages="totalPages" 
              @page-changed="changePage"
            />
          </div>

          <!-- Reply Editor Container -->
          <div ref="replyFormContainer" class="reply-box-wrapper card" style="margin-top: 2rem;">
            <div class="post-layout">
              <div class="post-sidebar" style="background: #f8f9fa; border-right: none;">
                 <div class="avatar-large" :style="{ backgroundColor: currentUserAvatar || '#ccc', color: '#fff' }">
                    {{ currentUsername ? currentUsername.charAt(0).toUpperCase() : '?' }}
                 </div>
              </div>
              <div class="post-main" style="padding: 0; border: 1px solid #e0e0e0;">
                 <CustomEditor ref="replyEditor" v-model="replyForm.content" minHeight="150px" :allowedUsers="conversationParticipantsForTag" />
                 
                 <div class="editor-footer" style="padding: 15px; display: flex; justify-content: flex-end; background: #f8f9fa; border-top: 1px solid #eee;">
                   <button class="btn-post" :disabled="submittingReply" @click="submitReply">
                     <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 5px; vertical-align: middle;"><polyline points="9 17 4 12 9 7"></polyline><path d="M20 18v-2a4 4 0 0 0-4-4H4"></path></svg>
                     {{ submittingReply ? 'Đang gửi...' : 'Trả lời' }}
                   </button>
                 </div>
              </div>
            </div>
          </div>

          <Breadcrumb :items="breadcrumbItems" style="margin-top: 2rem;" />
        </div>

        <!-- Right Block: Sidebar info and participants -->
        <div class="convo-sidebar">
          
          <!-- Card 1: Thông tin đối thoại -->
          <div class="card sidebar-card">
            <div class="card-header">Thông tin đối thoại</div>
            <div class="card-body">
              <div class="sidebar-row">
                <span class="label">Những người tham gia:</span>
                <span class="val">{{ conversation.participantCount }}</span>
              </div>
              <div class="sidebar-row">
                <span class="label">Trả lời:</span>
                <span class="val">{{ conversation.replyCount }}</span>
              </div>
              <div class="sidebar-row">
                <span class="label">Lần trả lời cuối:</span>
                <span class="val time">{{ formatDate(conversation.lastReplyAt || conversation.createdAt) }}</span>
              </div>
              <div class="sidebar-row" v-if="conversation.lastReplyAuthor">
                <span class="label">Trả lời lần cuối từ:</span>
                <span class="val author">{{ conversation.lastReplyAuthor.displayName || conversation.lastReplyAuthor.username }}</span>
              </div>
            </div>
          </div>

          <!-- Card 2: Những người tham gia đối thoại -->
          <div class="card sidebar-card" style="margin-top: 20px;">
            <div class="card-header">Những người tham gia đối thoại</div>
            <div class="card-body participant-list">
              <div v-for="part in conversation.participants" :key="part.id" class="participant-row">
                <div class="avatar-mini" :style="{ backgroundColor: part.avatar || '#ccc', color: '#fff' }">
                  {{ (part.displayName || part.username).charAt(0).toUpperCase() }}
                </div>
                <div class="participant-info">
                  <div class="name">{{ part.displayName || part.username }}</div>
                  <div class="title">{{ getUserRoleText(part.roles) }}</div>
                </div>
              </div>
              
              <div class="invite-more-wrapper">
                <a href="#" class="btn-invite-more" @click.prevent>Mời thêm</a>
              </div>
            </div>
          </div>

        </div>

      </div>
    </main>

    <ReactionListPopup 
      :show="showReactionPopup" 
      @update:show="showReactionPopup = $event" 
      :orderNumber="reactionPopupData.orderNumber" 
      :targetId="reactionPopupData.targetId" 
      type="messages"
      :summary="reactionPopupData.summary" 
    />
  </div>

  <div v-else-if="loading" class="container" style="padding: 3rem; text-align: center;">
    Đang tải cuộc đối thoại...
  </div>
</template>

<script>
import webSocketService from '@/shared/services/websocket.service'
import conversationService from '@/apps/Forum/services/conversation.service'
import ForumHeader from '@/shared/components/ForumHeader.vue'
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import CustomEditor from '@/shared/components/CustomEditor.vue'
import { formatForumDate } from '@/shared/utils/date'
import { alertSuccess, alertError, toastSuccess } from '@/shared/utils/swal'
import ReactionButton from '@/shared/components/ReactionButton.vue'
import ReactionSummary from '@/shared/components/ReactionSummary.vue'
import ReactionListPopup from '@/shared/components/ReactionListPopup.vue'
import reactionService from '@/apps/Forum/services/reaction.service'
import ForumPagination from '@/shared/components/ForumPagination.vue'

export default {
  name: 'ConversationDetail',
  components: {
    ForumHeader,
    Breadcrumb,
    CustomEditor,
    ReactionButton,
    ReactionSummary,
    ReactionListPopup,
    ForumPagination
  },
  data() {
    const userStr = localStorage.getItem('user')
    let parsedUser = null
    try {
      if (userStr) parsedUser = JSON.parse(userStr)
    } catch (e) {
      console.error('Error parsing stored user')
    }

    return {
      conversation: null,
      loading: true,
      submittingReply: false,
      replyForm: {
        content: '',
        quotedMessageId: null
      },
      currentUsername: parsedUser ? (parsedUser.displayName || parsedUser.username) : 'Me',
      currentUserAvatar: parsedUser ? parsedUser.avatar : '#3498db',
      currentUser: parsedUser,
      unsubscribeMessages: null,
      highlightedMessageId: null,
      reactionIconsList: [],
      showReactionPopup: false,
      reactionPopupData: {
        orderNumber: '#1',
        targetId: null,
        summary: []
      },
      justClickedConvo: false,
      currentPage: 1,
      itemsPerPage: 10
    }
  },
  computed: {
    breadcrumbItems() {
      return [
        { title: 'Trang chủ', to: { name: 'Home' } },
        { title: 'Đối thoại' }
      ]
    },
    participantListString() {
      if (!this.conversation || !this.conversation.participants) return ''
      return this.conversation.participants
        .map(p => p.displayName || p.username)
        .join(', ')
    },
    totalPages() {
      if (!this.conversation || !this.conversation.messages) return 1;
      return Math.ceil(this.conversation.messages.length / this.itemsPerPage) || 1;
    },
    paginatedMessages() {
      if (!this.conversation || !this.conversation.messages) return [];
      const start = (this.currentPage - 1) * this.itemsPerPage;
      const end = start + this.itemsPerPage;
      return this.conversation.messages.slice(start, end);
    },
    // Danh sách participants để giới hạn tag @ (loại trừ chính mình)
    conversationParticipantsForTag() {
      if (!this.conversation?.participants) return []
      return this.conversation.participants.filter(p => String(p.id) !== String(this.currentUser?.id))
    }
  },
  async mounted() {
    window.addEventListener('conversation-clicked', this.handleConversationClicked)
    await this.fetchReactionIcons()
    await this.fetchConversation()
    this.subscribeToMessages()
    if (this.$route.query.page) {
      this.currentPage = parseInt(this.$route.query.page) || 1
    }
    if (this.$route.query.messageId) {
      await this.jumpToTargetMessage()
    } else {
      this.scrollToBottom()
    }
  },
  beforeUnmount() {
    window.removeEventListener('conversation-clicked', this.handleConversationClicked)
    if (this.unsubscribeMessages) {
      this.unsubscribeMessages()
    }
  },
  watch: {
    '$route.params.id': {
      async handler(newVal, oldVal) {
        if (newVal && newVal !== oldVal) {
          if (this.unsubscribeMessages) {
            this.unsubscribeMessages()
          }
          if (this.$route.query.page) {
            this.currentPage = parseInt(this.$route.query.page) || 1
          } else {
            this.currentPage = 1
          }
          await this.fetchConversation()
          this.subscribeToMessages()
          if (this.$route.query.messageId) {
            await this.jumpToTargetMessage()
          } else {
            this.scrollToBottom()
          }
        }
      }
    },
    '$route.query.page': {
      handler(newVal) {
        this.currentPage = parseInt(newVal) || 1
      }
    },
    '$route.query.messageId': {
      handler(newVal) {
        if (newVal) {
          if (this.justClickedConvo) {
            this.justClickedConvo = false
            return
          }
          this.jumpToTargetMessage()
        }
      }
    }
  },
  methods: {
    async fetchConversation(silent = false) {
      if (!silent) this.loading = true
      try {
        const res = await conversationService.getById(this.$route.params.id)
        this.conversation = res.data
      } catch (e) {
        console.error('Lỗi khi tải chi tiết đối thoại:', e)
        if (!silent) {
          alertError('Không thể tải cuộc đối thoại này.')
          this.$router.push({ name: 'Home' })
        }
      } finally {
        if (!silent) this.loading = false
      }
    },
    subscribeToMessages() {
      if (!this.conversation) return
      
      this.unsubscribeMessages = webSocketService.subscribe(
        `/topic/conversations/${this.conversation.id}/messages`,
        (newMsg) => {
          if (this.conversation && this.conversation.messages) {
            // Tránh chèn trùng tin nhắn
            if (!this.conversation.messages.some(m => m.id === newMsg.id)) {
              this.conversation.messages.push(newMsg)
              this.conversation.replyCount = this.conversation.messages.length
              this.conversation.lastReplyAt = newMsg.createdAt
              this.conversation.lastReplyAuthor = newMsg.sender
              
              this.currentPage = this.totalPages
              this.$nextTick(() => {
                this.scrollToBottom()
              })
            }
          }
        }
      )
    },
    async fetchReactionIcons() {
      try {
        const res = await reactionService.getIcons();
        this.reactionIconsList = res.data || [];
      } catch (e) {
        console.error('Lỗi khi tải Icons Reaction:', e);
      }
    },
    canShowReactionForMessage(msg) {
      if (!this.currentUser || !msg || !msg.sender) return false;
      return String(msg.sender.id) !== String(this.currentUser.id);
    },
    openReactionPopup(orderNumber, targetId, summary) {
      this.reactionPopupData = {
        orderNumber,
        targetId,
        summary
      }
      this.showReactionPopup = true
    },
    async submitReply() {
      if (!this.replyForm.content.trim()) {
        alertError('Vui lòng nhập nội dung phản hồi')
        return
      }

      this.submittingReply = true
      try {
        const hasQuote = this.replyForm.content.includes('<blockquote')
        const payload = {
          content: this.replyForm.content
        }
        if (hasQuote && this.replyForm.quotedMessageId) {
          payload.quotedMessageId = this.replyForm.quotedMessageId
        }

        await conversationService.addMessage(this.conversation.id, payload)
        
        this.replyForm.content = ''
        this.replyForm.quotedMessageId = null
        toastSuccess('Gửi tin nhắn thành công')
        this.scrollToBottom()
      } catch (e) {
        console.error(e)
        alertError('Có lỗi xảy ra khi gửi tin nhắn')
      } finally {
        this.submittingReply = false
      }
    },
    quoteMessage(authorName, rawContent, msgId) {
      const tempDiv = document.createElement('div')
      tempDiv.innerHTML = rawContent
      
      // Loại bỏ blockquote con nếu có
      const innerQuotes = tempDiv.querySelectorAll('blockquote')
      innerQuotes.forEach(q => q.remove())

      const trimmedContent = tempDiv.innerHTML.trim()
      const quoteHtml = `<blockquote><p><strong>${authorName} đã viết:</strong></p>${trimmedContent}</blockquote><p>&nbsp;</p>`
      
      this.replyForm.content = this.replyForm.content + quoteHtml
      this.replyForm.quotedMessageId = msgId
      
      this.$nextTick(() => {
        const element = this.$refs.replyFormContainer
        if (element) {
          element.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }
      })
    },
    getUserRoleText(roles) {
      if (!roles) return 'Thành viên'
      if (roles.includes('ROLE_SUPER_ADMIN')) return 'Super Admin'
      if (roles.includes('ROLE_ADMIN')) return 'Admin'
      if (roles.includes('ROLE_NON_OFFICIAL_USER')) return 'Chưa chính thức'
      return 'Thành viên chính thức'
    },
    formatMessageContent(content) {
      if (!content) return ''
      // Thay đổi "đã nói" thành "đã viết" nếu có quote cũ
      return content.replace(/đã nói:<\/strong>/g, 'đã viết:</strong>')
    },
    formatDate(dateStr) {
      return formatForumDate(dateStr)
    },
    scrollToBottom() {
      this.$nextTick(() => {
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' })
      })
    },
    changePage(page) {
      this.currentPage = page;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    async jumpToTargetMessage() {
      const msgId = this.$route.query.messageId
      if (!msgId) return

      if (this.conversation && this.conversation.messages) {
        const idx = this.conversation.messages.findIndex(m => String(m.id) === String(msgId));
        if (idx !== -1) {
          this.currentPage = Math.ceil((idx + 1) / this.itemsPerPage);
        }
      }

      this.highlightedMessageId = msgId
      await this.$nextTick()
      setTimeout(() => {
        const element = document.getElementById(`msg-${msgId}`)
        if (element) {
          element.scrollIntoView({ behavior: 'auto', block: 'center' })
          setTimeout(() => {
            this.highlightedMessageId = null
          }, 4000)
        }
      }, 400)
    },
    async handleConversationClicked(event) {
      if (String(event.detail.conversationId) === String(this.conversation?.id)) {
        this.justClickedConvo = true
        await this.fetchConversation(true)
        if (event.detail.messageId) {
          if (this.conversation && this.conversation.messages) {
            const idx = this.conversation.messages.findIndex(m => String(m.id) === String(event.detail.messageId));
            if (idx !== -1) {
              this.currentPage = Math.ceil((idx + 1) / this.itemsPerPage);
            }
          }
          this.highlightedMessageId = event.detail.messageId
          await this.$nextTick()
          const element = document.getElementById(`msg-${event.detail.messageId}`)
          if (element) {
            element.scrollIntoView({ behavior: 'smooth', block: 'center' })
            setTimeout(() => {
              this.highlightedMessageId = null
            }, 4000)
          }
        }
      }
    }
  }
}
</script>

<style scoped>
.convo-title-full {
  padding: 1.2rem 1.5rem;
  margin: 0;
  border-bottom: 1px solid #eee;
}

.convo-title-full h1 {
  margin: 0;
  font-size: 1.4rem;
  color: #333;
  font-weight: 500;
  line-height: 1.4;
}

.convo-meta-bar {
  padding: 8px 1.5rem;
  display: flex;
  background-color: #f8f9fa;
  font-size: 0.85rem;
  color: #666;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.avatar-mini {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: bold;
}

.author-name {
  /* font-weight: bold; */
}

.meta-icon {
  color: #999;
}

.highlight-participants {
  color: #16a085;
}

.meta-dot {
  margin: 0 4px;
}

/* Action row styling matching XenForo mockup */
.convo-actions-row {
  margin: 15px 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-action-placeholder {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background-color: #fcfcfc;
  border: 1px solid #d5dbdb;
  border-radius: 4px;
  padding: 6px 12px;
  font-size: 0.85rem;
  color: #7f8c8d;
  cursor: not-allowed;
  opacity: 0.7;
}

/* Convo Layout */
.convo-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  margin-top: 15px;
}

.convo-main {
  flex: 1;
  min-width: 0;
}

.convo-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.message-card {
  margin-bottom: 15px;
  min-height: 180px;
}

.post-layout {
  display: flex;
}

.post-sidebar {
  width: 150px;
  background: #f5f5f5;
  padding: 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-right: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.avatar-large {
  width: 80px;
  height: 80px;
  background-color: #73c6b6;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  margin-bottom: 10px;
}

.author-name-large {
  font-weight: bold;
  color: #2980b9;
  font-size: 1rem;
  text-align: center;
  margin-bottom: 5px;
}

.author-title {
  font-size: 0.8rem;
  color: #7f8c8d;
  text-align: center;
}

.post-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 180px;
  min-width: 0;
}

.post-meta-top {
  padding: 10px 15px;
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
  color: #999;
  font-size: 0.85rem;
}

.post-number {
  font-weight: bold;
}

.content-body {
  padding: 15px;
  flex: 1;
  font-size: 1rem;
  line-height: 1.6;
  color: #333;
}

.post-meta-bottom {
  padding: 10px 15px;
  display: flex;
  justify-content: space-between;
  border-top: 1px solid #eee;
  background: #fcfcfc;
}

.action-link {
  color: #3498db;
  text-decoration: none;
  font-size: 0.85rem;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.action-link:hover {
  text-decoration: underline;
}

.left-actions {
  display: flex;
  gap: 15px;
}

.reply-link {
  font-weight: 600;
}

.reply-box-wrapper {
  margin-bottom: 15px;
}

.btn-post {
  background: #1a507a;
  color: white;
  border: none;
  padding: 8px 18px;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: background 0.2s;
}

.btn-post:hover {
  background: #154267;
}

/* Sidebar Styles */
.sidebar-card {
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  border-radius: 4px;
}

.sidebar-card .card-header {
  background: #ebf2f7;
  color: #1a507a;
  font-weight: bold;
  padding: 10px 15px;
  font-size: 0.95rem;
  border-bottom: 1px solid #d4e6f1;
}

.sidebar-card .card-body {
  padding: 15px;
}

.sidebar-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 0.88rem;
  line-height: 1.4;
}

.sidebar-row:last-child {
  margin-bottom: 0;
}

.sidebar-row .label {
  color: #666;
}

.sidebar-row .val {
  font-weight: bold;
  color: #333;
}

.sidebar-row .val.time {
  font-weight: normal;
  color: #888;
}

.sidebar-row .val.author {
  color: #2980b9;
}

.participant-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.participant-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.participant-row .avatar-mini {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.95rem;
  font-weight: bold;
  flex-shrink: 0;
}

.participant-info {
  display: flex;
  flex-direction: column;
}

.participant-info .name {
  font-weight: bold;
  font-size: 0.9rem;
  color: #2980b9;
}

.participant-info .title {
  font-size: 0.75rem;
  color: #7f8c8d;
}

.invite-more-wrapper {
  margin-top: 5px;
  border-top: 1px dashed #eee;
  padding-top: 10px;
  text-align: right;
}

.btn-invite-more {
  color: #3498db;
  text-decoration: none;
  font-size: 0.85rem;
}

.btn-invite-more:hover {
  text-decoration: underline;
}

:deep(.ql-editor img), :deep(.ql-editor video) { max-width: 100%; height: auto; }
:deep(blockquote) {
  background: #fcfbf7;
  border-left: 3px solid #e67e22;
  padding: 12px 16px;
  margin: 10px 0;
  font-style: normal;
  color: #657786;
  border-radius: 4px;
}

/* Flash highlighting animation for jumped posts */
.highlight-jump {
  animation: flash-glow 3s ease-in-out;
  position: relative;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.3);
}

@keyframes flash-glow {
  0% { background-color: #fff2c2; box-shadow: 0 0 15px #f39c12; }
  30% { background-color: #fff2c2; box-shadow: 0 0 10px #f39c12; }
  100% { background-color: #ffffff; box-shadow: none; }
}

/* Flexbox Patch for Aligning Actions Inline */
.left-actions, .right-actions {
  display: flex !important;
  align-items: center !important;
  gap: 15px !important;
  position: relative;
}

/* XenForo-style statistics bubble container */
.reactions-bar-container {
  padding: 0 15px 10px 15px;
  margin-top: -5px;
  display: flex;
}
</style>
