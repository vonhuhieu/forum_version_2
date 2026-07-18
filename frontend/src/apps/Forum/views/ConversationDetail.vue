<template>
  <div v-if="!loading && conversation">
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
                  <user-profile-popup :user="msg.sender" v-if="msg.sender">
                    <div class="avatar-large" :style="!isAvatarUrl(msg.sender?.avatar) ? { backgroundColor: msg.sender?.avatar || '#ccc', color: '#fff' } : {}">
                      <img v-if="isAvatarUrl(msg.sender?.avatar)" :src="msg.sender.avatar" />
                      <template v-else>
                        {{ msg.sender ? (msg.sender.displayName || msg.sender.username).charAt(0).toUpperCase() : '?' }}
                      </template>
                    </div>
                  </user-profile-popup>
                  <div v-else class="avatar-large" style="background-color: #ccc; color: #fff;">?</div>
                  <div class="author-info-mobile-block">
                    <div class="author-name-large">{{ msg.sender ? (msg.sender.displayName || msg.sender.username) : 'Ẩn danh' }}</div>
                    <div class="author-title">{{ getUserRoleText(msg.sender?.roles) }}</div>
                  </div>
                  <span class="message-userArrow"></span>
                </div>

                <div class="post-main">
                  <div class="post-meta-top">
                    <span class="post-time-top">{{ formatDate(msg.createdAt) }}</span>
                    <div class="post-actions-top">
                      <span class="post-number">#{{ (currentPage - 1) * itemsPerPage + index + 1 }}</span>
                    </div>
                  </div>

                  <div v-if="editingMessageId === msg.id" class="inline-edit-box" style="padding: 10px; border: 1px solid #ddd; border-radius: 4px; background: #fff; margin-bottom: 10px;">
                    <CustomEditor ref="inlineEditEditor" v-model="editForm.content" minHeight="150px" :is-edit="true" />
                    <div class="edit-actions-footer" style="margin-top: 10px; display: flex; gap: 10px;">
                      <button class="btn-save" :disabled="submittingEdit" @click="submitEditMessage(msg)" style="padding: 6px 12px; background-color: #1a507a; color: white; border: none; border-radius: 4px; cursor: pointer;">
                        {{ submittingEdit ? 'Đang lưu...' : 'Lưu' }}
                      </button>
                      <button class="btn-cancel-edit" @click="cancelEditingMessage" style="padding: 6px 12px; background-color: #e0e0e0; color: #333; border: none; border-radius: 4px; cursor: pointer;">
                        Hủy
                      </button>
                    </div>
                  </div>
                  <div v-else class="content-body ql-editor" v-html="formatMessageContent(msg.content)"></div>

                  <div class="post-meta-bottom" v-if="editingMessageId !== msg.id">
                    <div class="left-actions">
                      <a href="#" class="action-link" @click.prevent>Báo cáo</a>
                      <a href="#" class="action-link" v-if="canEditMessage(msg, index)" @click.prevent="startEditingMessage(msg)">Sửa</a>
                    </div>
                    <div class="right-actions">
                      <ReactionButton 
                        v-if="canShowReactionForMessage(msg)"
                        :targetId="msg.id"
                        type="message"
                        :allIcons="reactionIconsList"
                        :userReaction="msg.currentUserReaction"
                        @reaction-updated="updateLocalMessageReaction(msg.id, $event)"
                        @reaction-failed="fetchConversation(true)"
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
          <div v-if="isCreator || !conversation.locked" ref="replyFormContainer" class="reply-box-wrapper card" style="margin-top: 2rem;">
            <div class="post-layout">
              <div class="post-sidebar" style="background: #f8f9fa; border-right: none;">
                  <div class="avatar-large" :style="!isAvatarUrl(currentUserAvatar) ? { backgroundColor: currentUserAvatar || '#ccc', color: '#fff' } : {}">
                     <img v-if="isAvatarUrl(currentUserAvatar)" :src="currentUserAvatar" />
                     <template v-else>
                        {{ currentUsername ? currentUsername.charAt(0).toUpperCase() : '?' }}
                     </template>
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
                <user-profile-popup :user="part" v-if="part">
                  <div class="avatar-mini" :style="!isAvatarUrl(part.avatar) ? { backgroundColor: part.avatar || '#ccc', color: '#fff' } : {}">
                    <img v-if="isAvatarUrl(part.avatar)" :src="part.avatar" />
                    <template v-else>
                      {{ (part.displayName || part.username).charAt(0).toUpperCase() }}
                    </template>
                  </div>
                </user-profile-popup>
                <div v-else class="avatar-mini" style="background-color: #ccc; color: #fff;">?</div>
                <div class="participant-info">
                  <div class="name">{{ part.displayName || part.username }}</div>
                  <div class="title">{{ getUserRoleText(part.roles) }}</div>
                </div>
              </div>
              
              <div v-if="isCreator || conversation.allowInvite" class="invite-more-wrapper">
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
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import CustomEditor from '@/shared/components/CustomEditor.vue'
import { formatForumDate } from '@/shared/utils/date'
import { alertSuccess, alertError, toastSuccess } from '@/shared/utils/swal'
import ReactionButton from '@/shared/components/ReactionButton.vue'
import ReactionSummary from '@/shared/components/ReactionSummary.vue'
import ReactionListPopup from '@/shared/components/ReactionListPopup.vue'
import reactionService from '@/apps/Forum/services/reaction.service'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import UserProfilePopup from '@/shared/components/UserProfilePopup.vue'
import { isAvatarUrl } from '@/shared/utils/utils'
import settingService from '@/shared/services/setting.service'
import { ROLES, SETTINGS } from '@/shared/utils/constants'

export default {
  name: 'ConversationDetail',
  components: {
    Breadcrumb,
    CustomEditor,
    ReactionButton,
    ReactionSummary,
    ReactionListPopup,
    ForumPagination,
    UserProfilePopup
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
      itemsPerPage: 10,
      conversationEditLimitMinutes: SETTINGS.DEFAULT_CONVERSATION_EDIT_LIMIT_MINUTES,
      conversationReplyEditLimitMinutes: SETTINGS.DEFAULT_CONVERSATION_REPLY_EDIT_LIMIT_MINUTES,
      editingMessageId: null,
      editForm: {
        content: ''
      },
      submittingEdit: false
    }
  },
  computed: {
    breadcrumbItems() {
      const items = [
        { title: 'Trang chủ', to: { name: 'Home' } },
        { title: 'Đối thoại', to: { name: 'ConversationList' } }
      ]
      if (this.conversation && this.conversation.title) {
        items.push({ title: this.conversation.title })
      }
      return items
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
    },
    isCreator() {
      if (!this.conversation || !this.conversation.creator || !this.currentUser) return false
      return this.conversation.creator.username === this.currentUser.username
    }
  },
  async mounted() {
    window.addEventListener('conversation-clicked', this.handleConversationClicked)
    await this.fetchSettings()
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
    window.addEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  beforeUnmount() {
    window.removeEventListener('conversation-clicked', this.handleConversationClicked)
    if (this.unsubscribeMessages) {
      this.unsubscribeMessages()
    }
    window.removeEventListener('user-avatar-updated', this.handleAvatarUpdated)
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
            const idx = this.conversation.messages.findIndex(m => m.id === newMsg.id)
            if (idx !== -1) {
              // Cập nhật tin nhắn đã tồn tại (Hot Update real-time qua WebSocket)
              this.conversation.messages[idx].content = newMsg.content
            } else {
              // Thêm tin nhắn mới
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
    updateLocalMessageReaction(messageId, newIcon) {
      if (!this.conversation || !this.conversation.messages) return;
      const msg = this.conversation.messages.find(m => String(m.id) === String(messageId));
      if (!msg) return;

      const oldReaction = msg.currentUserReaction;
      msg.currentUserReaction = newIcon;

      if (!msg.reactionSummary) {
        msg.reactionSummary = [];
      }

      // 1. Decrement old reaction
      if (oldReaction) {
        const prevIndex = msg.reactionSummary.findIndex(
          s => s.reactionIcon.id === oldReaction.id
        );
        if (prevIndex !== -1) {
          msg.reactionSummary[prevIndex].count--;
          if (msg.reactionSummary[prevIndex].count <= 0) {
            msg.reactionSummary.splice(prevIndex, 1);
          }
        }
      }

      // 2. Increment new reaction
      if (newIcon) {
        const newIndex = msg.reactionSummary.findIndex(
          s => s.reactionIcon.id === newIcon.id
        );
        if (newIndex !== -1) {
          msg.reactionSummary[newIndex].count++;
        } else {
          msg.reactionSummary.push({
            reactionIcon: newIcon,
            count: 1,
            latestTime: new Date().toISOString()
          });
        }
      }

      // Sort summary by count descending
      msg.reactionSummary.sort((a, b) => b.count - a.count);

      // 3. Handle recentReactors
      if (!msg.recentReactors) {
        msg.recentReactors = [];
      }
      msg.recentReactors = msg.recentReactors.filter(
        u => u.username !== this.currentUser?.username
      );
      if (newIcon) {
        const userDTO = {
          id: this.currentUser?.id,
          username: this.currentUser?.username,
          displayName: this.currentUser?.displayName,
          avatar: this.currentUser?.avatar
        };
        msg.recentReactors.unshift(userDTO);
        if (msg.recentReactors.length > 3) {
          msg.recentReactors = msg.recentReactors.slice(0, 3);
        }
      }
    },
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    handleAvatarUpdated(event) {
      const { username, avatar } = event.detail
      if (this.conversation) {
        const updatedConversation = { ...this.conversation }
        if (this.conversation.participants) {
          updatedConversation.participants = this.conversation.participants.map(p => {
            if (p.username === username) {
              return { ...p, avatar }
            }
            return p
          })
        }
        if (this.conversation.messages) {
          updatedConversation.messages = this.conversation.messages.map(m => {
            if (m.sender && m.sender.username === username) {
              return { ...m, sender: { ...m.sender, avatar } }
            }
            return m
          })
        }
        this.conversation = updatedConversation
      }
      if (this.currentUser && this.currentUser.username === username) {
        this.currentUserAvatar = avatar
      }
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
      const quoteHtml = `<blockquote data-source="${msgId}"><p><strong>${authorName} đã viết:</strong></p>${trimmedContent}</blockquote><p>&nbsp;</p>`
      
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
      let processed = content.replace(/đã nói:<\/strong>/g, 'đã viết:</strong>')

      // Đồng bộ quote động từ dữ liệu tin nhắn mới nhất trong cuộc đối thoại
      try {
        const parser = new DOMParser()
        const doc = parser.parseFromString(processed, 'text/html')
        const blockquotes = doc.querySelectorAll('blockquote[data-source]')
        
        let hasChanges = false
        
        blockquotes.forEach(bq => {
          const sourceId = bq.getAttribute('data-source')
          if (!sourceId) return
          
          const msgId = parseInt(sourceId, 10)
          if (!isNaN(msgId) && this.conversation && this.conversation.messages) {
            const quotedMsg = this.conversation.messages.find(m => m.id === msgId)
            if (quotedMsg) {
              const authorName = quotedMsg.sender ? (quotedMsg.sender.displayName || quotedMsg.sender.username) : 'Ẩn danh'
              const msgContentClean = this.stripBlockQuotes(quotedMsg.content || '')
              bq.innerHTML = `<p><strong>${authorName} đã viết:</strong></p>${msgContentClean}`
              hasChanges = true
            }
          }
        })
        
        if (hasChanges) {
          processed = doc.body.innerHTML
        }
      } catch (err) {
        console.error('Lỗi khi tự động cập nhật nội dung trích dẫn đối thoại:', err)
      }

      return processed
    },
    stripBlockQuotes(html) {
      if (!html) return ''
      try {
        const parser = new DOMParser()
        const doc = parser.parseFromString(html, 'text/html')
        const blockquotes = doc.querySelectorAll('blockquote')
        blockquotes.forEach(bq => bq.remove())
        return doc.body.innerHTML.trim()
      } catch (e) {
        return html
      }
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
    },
    async fetchSettings() {
      try {
        const response = await settingService.getPublicSettings();
        if (response && response.data) {
          if (response.data.conversation_edit_limit_minutes !== undefined) {
            this.conversationEditLimitMinutes = Number(response.data.conversation_edit_limit_minutes);
          }
          if (response.data.conversation_reply_edit_limit_minutes !== undefined) {
            this.conversationReplyEditLimitMinutes = Number(response.data.conversation_reply_edit_limit_minutes);
          }
        }
      } catch (e) {
        console.error('Không thể tải cấu hình thời gian chỉnh sửa đối thoại:', e);
      }
    },
    canEditMessage(msg, index) {
      if (!this.currentUser || !msg || !msg.sender) return false;

      // Kiểm tra quyền sở hữu
      const isOwner = this.currentUser.username === msg.sender.username;
      if (!isOwner) return false;

      // Quyền Admin/Super Admin luôn được phép chỉnh sửa
      const isServerAdmin = this.currentUser.roles?.includes(ROLES.ADMIN) || this.currentUser.roles?.includes(ROLES.SUPER_ADMIN);
      if (isServerAdmin) return true;

      // Xác định loại tin nhắn để lấy cấu hình phù hợp
      const isFirstMsg = index === 0 && this.currentPage === 1;
      const limitMinutes = isFirstMsg ? this.conversationEditLimitMinutes : this.conversationReplyEditLimitMinutes;

      // Kiểm tra mốc thời gian giới hạn chỉnh sửa đối với tin nhắn
      if (limitMinutes !== SETTINGS.NO_LIMIT_VALUE) {
        const createdAt = new Date(msg.createdAt);
        const now = new Date();
        const diffMinutes = (now - createdAt) / (1000 * 60);
        if (diffMinutes > limitMinutes) {
          return false;
        }
      }

      return true;
    },
    startEditingMessage(msg) {
      this.editingMessageId = msg.id;
      // Đồng bộ hóa nội dung quote động mới nhất trước khi đưa vào editor
      this.editForm.content = this.formatMessageContent(msg.content);
    },
    cancelEditingMessage() {
      this.editingMessageId = null;
      this.editForm.content = '';
    },
    async submitEditMessage(msg) {
      if (!this.editForm.content.trim()) {
        alertError('Nội dung không được để trống');
        return;
      }
      this.submittingEdit = true;
      try {
        await conversationService.updateMessage(msg.id, { content: this.editForm.content });
        msg.content = this.editForm.content;
        alertSuccess('Cập nhật tin nhắn thành công');
        this.cancelEditingMessage();
      } catch (error) {
        console.error('Lỗi khi cập nhật tin nhắn:', error);
        const errorMsg = error.response?.data?.message || 'Không thể cập nhật tin nhắn';
        alertError(errorMsg);
      } finally {
        this.submittingEdit = false;
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
  overflow-wrap: break-word;
  word-wrap: break-word;
  word-break: break-word;
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
  position: relative;
}

/* Speech bubble arrow indicator for message cells */
.message-userArrow {
  position: absolute;
  top: 20px;
  right: -1px;
  width: 0;
  height: 0;
  border: 10px solid transparent;
  border-left-width: 0;
  border-right-color: #e0e0e0;
  z-index: 2;
}

.message-userArrow::after {
  content: "";
  position: absolute;
  top: -10px;
  left: 1px;
  width: 0;
  height: 0;
  border: 10px solid transparent;
  border-left-width: 0;
  border-right-color: #ffffff;
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

.author-info-mobile-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
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
  overflow-wrap: break-word;
  word-wrap: break-word;
  word-break: break-word;
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

@media (min-width: 768px) and (max-width: 1024px) {
  .convo-layout {
    flex-direction: column;
    align-items: stretch;
    gap: 20px;
  }
  .convo-sidebar {
    width: 100%;
    order: -1;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
    margin-bottom: 20px;
  }
  .sidebar-card {
    margin-top: 0 !important;
  }
  .convo-main {
    width: 100%;
  }
  .reply-box-wrapper .post-layout {
    flex-direction: column;
  }
  .reply-box-wrapper .post-sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #e0e0e0;
    padding: 10px 15px;
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
  }
  .reply-box-wrapper .avatar-large {
    width: 40px;
    height: 40px;
    font-size: 1.2rem;
    margin-bottom: 0;
  }
}

@media (max-width: 767px) {
  /* Center top action pagination */
  .pagination-wrapper-left {
    width: 100% !important;
    display: flex !important;
    justify-content: center !important;
  }
  
  /* Center bottom pagination bar */
  .pagination-wrapper {
    display: flex !important;
    justify-content: center !important;
    width: 100% !important;
  }

  /* Make post layout column-stacked */
  .post-layout {
    flex-direction: column !important;
  }

  /* Style post-sidebar to be horizontal banner on top */
  .post-sidebar {
    width: 100% !important;
    flex-direction: row !important;
    align-items: center !important;
    justify-content: flex-start !important;
    padding: 12px 15px !important;
    border-right: none !important;
    border-bottom: 1px solid #e0e0e0 !important;
    background-color: #f5f5f5 !important;
  }

  /* Adjust avatar size on mobile */
  .avatar-large {
    width: 48px !important;
    height: 48px !important;
    font-size: 1.5rem !important;
    margin-bottom: 0 !important;
    margin-right: 12px !important;
  }

  .author-info-mobile-block {
    align-items: flex-start !important;
    text-align: left !important;
    width: auto !important;
    display: flex !important;
    flex-direction: column !important;
  }

  .author-name-large {
    text-align: left !important;
    font-size: 0.95rem !important;
    margin-bottom: 2px !important;
  }

  .author-title {
    text-align: left !important;
    font-size: 0.85rem !important;
  }

  /* Speech bubble arrow positioned pointing upwards on mobile */
  .message-userArrow {
    top: auto !important;
    right: auto !important;
    bottom: -1px !important;
    left: 24px !important;
    border: 10px solid transparent !important;
    border-top-width: 0 !important;
    border-bottom-color: #e0e0e0 !important;
  }

  .message-userArrow::after {
    border: 10px solid transparent !important;
    border-top-width: 0 !important;
    border-bottom-color: #ffffff !important;
    top: 1px !important;
    left: -10px !important;
  }

  /* Convo Layout on Mobile */
  .convo-layout {
    flex-direction: column;
    align-items: stretch;
    gap: 15px;
  }

  .convo-sidebar {
    width: 100%;
    order: -1;
    display: flex;
    flex-direction: column;
    gap: 15px;
    margin-bottom: 15px;
  }

  .sidebar-card {
    margin-top: 0 !important;
    width: 100%;
  }

  .convo-main {
    width: 100%;
  }
}
</style>
