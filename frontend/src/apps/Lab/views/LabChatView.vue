<template>
  <div class="lab-chat-layout">
    <!-- Sidebar: Danh sách phiên trò chuyện (cho thành viên) -->
    <aside class="lab-sidebar" :class="{ 'sidebar-open': showSidebarMobile }">
      <div class="sidebar-header">
        <button class="btn btn-primary w-100 btn-new-chat" @click="startNewChat">
          <i class="bi bi-plus-lg me-1"></i> Cuộc trò chuyện mới
        </button>
      </div>

      <div class="sessions-list-wrapper">
        <div v-if="isLoggedIn" class="sessions-list">
          <div
            v-for="s in sessions"
            :key="s.sessionId"
            class="session-item"
            :class="{ active: currentSessionId === s.sessionId }"
            @click="switchSession(s.sessionId)"
          >
            <i class="bi bi-chat-left-text me-2"></i>
            <span class="session-title text-truncate">{{ s.title || 'Cuộc trò chuyện' }}</span>
            <button
              class="btn-delete-session"
              @click.stop="deleteSession(s.sessionId)"
              title="Xoá cuộc trò chuyện"
            >
              <i class="bi bi-trash3"></i>
            </button>
          </div>

          <div v-if="sessions.length === 0 && !loadingSessions" class="text-center text-muted py-4 small">
            Chưa có lịch sử trò chuyện
          </div>
        </div>

        <div v-else class="guest-sidebar-tip">
          <i class="bi bi-info-circle display-6 text-primary mb-2"></i>
          <p class="small text-muted mb-3">{{ LAB_TEXTS.GUEST_LOGIN_HINT }}</p>
          <router-link to="/login" class="btn btn-outline-primary btn-sm w-100">Đăng nhập</router-link>
        </div>
      </div>
    </aside>

    <!-- Khu vực Chat chính -->
    <main class="lab-chat-main">
      <!-- Chat Header -->
      <header class="chat-topbar">
        <button class="btn-toggle-sidebar d-md-none" @click="showSidebarMobile = !showSidebarMobile">
          <i class="bi bi-layout-sidebar"></i>
        </button>
        <div class="bot-status">
          <span class="bot-emoji">{{ LAB_BOT.AVATAR }}</span>
          <div>
            <div class="bot-name">{{ LAB_BOT.NAME }}</div>
            <div class="bot-sub">Trợ lý sáng tạo sản phẩm tương tác</div>
          </div>
        </div>
      </header>

      <!-- Khung hiển thị tin nhắn -->
      <div class="messages-container" ref="messagesContainer">
        <!-- Chào mừng & Gợi ý nhanh nếu chưa có tin nhắn -->
        <div v-if="messages.length === 0" class="welcome-box">
          <div class="welcome-bot-avatar">{{ LAB_BOT.AVATAR }}</div>
          <h2 class="welcome-title">Xin chào, ta có thể giúp gì cho bạn?</h2>
          <p class="welcome-sub">{{ LAB_BOT.GREETING }}</p>

          <div class="quick-prompts-grid">
            <button
              v-for="qp in LAB_QUICK_PROMPTS"
              :key="qp.label"
              class="quick-prompt-card"
              @click="sendQuickPrompt(qp.prompt)"
            >
              <span class="qp-icon">{{ qp.icon }}</span>
              <span class="qp-label">{{ qp.label }}</span>
              <span class="qp-desc">{{ qp.prompt }}</span>
            </button>
          </div>
        </div>

        <!-- Danh sách tin nhắn -->
        <div
          v-for="(msg, index) in messages"
          :key="msg.id || index"
          class="message-row"
          :class="msg.role === LAB_MESSAGE_ROLES.USER ? 'row-user' : 'row-assistant'"
        >
          <div class="message-avatar" v-if="msg.role === LAB_MESSAGE_ROLES.ASSISTANT">
            {{ LAB_BOT.AVATAR }}
          </div>

          <div class="message-bubble">
            <!-- Nội dung chữ an toàn đã qua renderSafeMarkdown -->
            <div class="message-text" v-html="formatMessage(msg.content)"></div>

            <!-- Thẻ sản phẩm nếu có -->
            <div v-if="msg.productCard" class="chat-product-card">
              <div class="card-icon">
                {{ getTemplateIcon(msg.productCard.templateType) }}
              </div>
              <div class="card-info">
                <div class="card-title">{{ msg.productCard.title }}</div>
                <div class="card-meta">Khuôn mẫu: {{ getTemplateName(msg.productCard.templateType) }}</div>
              </div>
              <div class="card-actions">
                <a
                  :href="getLabProductPath(msg.productCard.publicId)"
                  target="_blank"
                  class="btn btn-sm btn-primary"
                >
                  <i class="bi bi-box-arrow-up-right"></i> Mở sản phẩm
                </a>
              </div>
            </div>
          </div>
        </div>

        <!-- Trạng thái đang stream / gõ -->
        <div v-if="isStreaming" class="message-row row-assistant">
          <div class="message-avatar">{{ LAB_BOT.AVATAR }}</div>
          <div class="message-bubble">
            <div class="message-text" v-html="formatMessage(streamBuffer)"></div>
            <div class="typing-indicator" v-if="!streamBuffer">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- Ô nhập tin nhắn -->
      <footer class="chat-input-bar">
        <div class="input-wrapper">
          <textarea
            ref="inputArea"
            v-model="inputMessage"
            :placeholder="LAB_TEXTS.INPUT_PLACEHOLDER"
            class="chat-textarea"
            rows="1"
            :disabled="isStreaming"
            @keydown.enter.prevent="handleEnter"
            @input="adjustTextarea"
          ></textarea>

          <button
            v-if="!isStreaming"
            class="btn-send"
            :disabled="!inputMessage.trim()"
            @click="sendMessage"
          >
            <i class="bi bi-arrow-up"></i>
          </button>

          <button
            v-else
            class="btn-stop"
            @click="stopStream"
            title="Dừng sinh phản hồi"
          >
            <i class="bi bi-stop-fill"></i>
          </button>
        </div>
      </footer>
    </main>
  </div>
</template>

<script>
import {
  LAB_BOT,
  LAB_MESSAGE_ROLES,
  LAB_QUICK_PROMPTS,
  LAB_TEMPLATE_META,
  LAB_TEXTS
} from '@/shared/utils/constants'
import { renderSafeMarkdown, getLabProductPath } from '@/shared/utils/utils'
import { labService } from '../services/lab.service'
import { alertConfirm, alertError, alertSuccess } from '@/shared/utils/swal'

export default {
  name: 'LabChatView',
  data() {
    return {
      LAB_BOT,
      LAB_MESSAGE_ROLES,
      LAB_QUICK_PROMPTS,
      LAB_TEXTS,
      isLoggedIn: false,
      currentSessionId: null,
      sessions: [],
      messages: [],
      inputMessage: '',
      isStreaming: false,
      streamBuffer: '',
      abortStreamFn: null,
      loadingSessions: false,
      showSidebarMobile: false
    }
  },
  created() {
    this.checkLoginStatus()
  },
  async mounted() {
    if (this.isLoggedIn) {
      await this.fetchSessions()
    }
  },
  methods: {
    getLabProductPath,
    checkLoginStatus() {
      this.isLoggedIn = !!localStorage.getItem('token')
    },
    formatMessage(text) {
      return renderSafeMarkdown(text || '')
    },
    getTemplateIcon(type) {
      return LAB_TEMPLATE_META[type]?.icon || '📦'
    },
    getTemplateName(type) {
      return LAB_TEMPLATE_META[type]?.name || type
    },
    async fetchSessions() {
      this.loadingSessions = true
      try {
        const res = await labService.getSessions({ page: 0, size: 20 })
        this.sessions = res.data?.content || []
      } catch (err) {
        console.error('Lỗi tải danh sách phiên:', err)
      } finally {
        this.loadingSessions = false
      }
    },
    async switchSession(sessionId) {
      if (this.currentSessionId === sessionId) return
      this.currentSessionId = sessionId
      this.messages = []
      this.showSidebarMobile = false

      try {
        const res = await labService.getSessionMessages(sessionId)
        this.messages = res.data || []
        this.scrollToBottom()
      } catch (err) {
        alertError('Không thể tải nội dung cuộc trò chuyện')
      }
    },
    startNewChat() {
      if (this.isStreaming) this.stopStream()
      this.currentSessionId = null
      this.messages = []
      this.streamBuffer = ''
      this.showSidebarMobile = false
    },
    async deleteSession(sessionId) {
      const confirm = await alertConfirm('Xác nhận xoá', 'Cuộc trò chuyện này và sản phẩm liên quan sẽ bị xoá vĩnh viễn?')
      if (!confirm.isConfirmed) return

      try {
        await labService.deleteSession(sessionId)
        this.sessions = this.sessions.filter(s => s.sessionId !== sessionId)
        if (this.currentSessionId === sessionId) {
          this.startNewChat()
        }
        alertSuccess('Đã xoá cuộc trò chuyện thành công')
      } catch (err) {
        alertError(err.response?.data?.message || 'Không thể xoá cuộc trò chuyện')
      }
    },
    sendQuickPrompt(prompt) {
      this.inputMessage = prompt
      this.sendMessage()
    },
    handleEnter(e) {
      if (!e.shiftKey) {
        this.sendMessage()
      }
    },
    adjustTextarea() {
      const el = this.$refs.inputArea
      if (!el) return
      el.style.height = 'auto'
      el.style.height = Math.min(el.scrollHeight, 180) + 'px'
    },
    sendMessage() {
      const text = this.inputMessage.trim()
      if (!text || this.isStreaming) return

      // Thêm tin nhắn user vào danh sách
      this.messages.push({
        role: LAB_MESSAGE_ROLES.USER,
        content: text
      })

      this.inputMessage = ''
      this.adjustTextarea()
      this.scrollToBottom()

      this.isStreaming = true
      this.streamBuffer = ''
      let newProductCard = null

      this.abortStreamFn = labService.streamChat(
        {
          sessionId: this.currentSessionId,
          message: text
        },
        {
          onSession: (sessionId) => {
            if (!this.currentSessionId) {
              this.currentSessionId = sessionId
              if (this.isLoggedIn) this.fetchSessions()
            }
          },
          onMessage: (chunk) => {
            this.streamBuffer += chunk
            this.scrollToBottom()
          },
          onProduct: (productCard) => {
            newProductCard = productCard
          },
          onError: (errMsg) => {
            this.isStreaming = false
            this.messages.push({
              role: LAB_MESSAGE_ROLES.ASSISTANT,
              content: `⚠️ ${errMsg || LAB_TEXTS.STREAM_ERROR}`
            })
            this.streamBuffer = ''
            this.scrollToBottom()
          },
          onDone: () => {
            this.isStreaming = false
            if (this.streamBuffer) {
              this.messages.push({
                role: LAB_MESSAGE_ROLES.ASSISTANT,
                content: this.streamBuffer,
                productCard: newProductCard
              })
              this.streamBuffer = ''
            }
            this.scrollToBottom()
            if (this.isLoggedIn) this.fetchSessions()
          }
        }
      )
    },
    stopStream() {
      if (this.abortStreamFn) {
        this.abortStreamFn()
        this.abortStreamFn = null
      }
      this.isStreaming = false
      if (this.streamBuffer) {
        this.messages.push({
          role: LAB_MESSAGE_ROLES.ASSISTANT,
          content: this.streamBuffer + ' *(đã dừng)*'
        })
        this.streamBuffer = ''
      }
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const el = this.$refs.messagesContainer
        if (el) {
          el.scrollTop = el.scrollHeight
        }
      })
    }
  }
}
</script>

<style scoped>
.lab-chat-layout {
  display: flex;
  height: calc(100vh - 64px);
  background: #f8fafc;
  position: relative;
  overflow: hidden;
}

/* Sidebar */
.lab-sidebar {
  width: 280px;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease;
  z-index: 50;
}

@media (max-width: 768px) {
  .lab-sidebar {
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    transform: translateX(-100%);
    box-shadow: 4px 0 15px rgba(0, 0, 0, 0.1);
  }
  .lab-sidebar.sidebar-open {
    transform: translateX(0);
  }
}

.sidebar-header {
  padding: 1rem;
  border-bottom: 1px solid #f1f5f9;
}

.btn-new-chat {
  border-radius: 10px;
  font-weight: 600;
  padding: 10px 16px;
}

.sessions-list-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 4px;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s ease;
}

.session-item:hover {
  background: #f1f5f9;
}

.session-item.active {
  background: #e0f2fe;
  color: #0284c7;
  font-weight: 600;
}

.session-title {
  flex: 1;
  font-size: 0.9rem;
}

.btn-delete-session {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}

.session-item:hover .btn-delete-session {
  opacity: 1;
}

.btn-delete-session:hover {
  color: #ef4444;
}

.guest-sidebar-tip {
  padding: 2rem 1rem;
  text-align: center;
}

/* Chat Main */
.lab-chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  position: relative;
}

.chat-topbar {
  display: flex;
  align-items: center;
  padding: 0.75rem 1.5rem;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  gap: 12px;
}

.btn-toggle-sidebar {
  background: transparent;
  border: none;
  font-size: 1.25rem;
  color: #475569;
  cursor: pointer;
}

.bot-status {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bot-emoji {
  font-size: 1.8rem;
}

.bot-name {
  font-weight: 700;
  color: #0f172a;
}

.bot-sub {
  font-size: 0.75rem;
  color: #64748b;
}

/* Messages */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.welcome-box {
  max-width: 680px;
  margin: auto;
  text-align: center;
  padding: 2rem 1rem;
}

.welcome-bot-avatar {
  font-size: 3.5rem;
  margin-bottom: 1rem;
}

.welcome-title {
  font-size: 1.8rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 0.5rem;
}

.welcome-sub {
  font-size: 1rem;
  color: #64748b;
  margin-bottom: 2rem;
}

.quick-prompts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.quick-prompt-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 1.2rem;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quick-prompt-card:hover {
  border-color: #38bdf8;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(56, 189, 248, 0.15);
}

.qp-icon {
  font-size: 1.5rem;
}

.qp-label {
  font-weight: 700;
  color: #0f172a;
}

.qp-desc {
  font-size: 0.8rem;
  color: #64748b;
}

.message-row {
  display: flex;
  gap: 12px;
  max-width: 820px;
  width: 100%;
}

.row-user {
  align-self: flex-end;
  justify-content: flex-end;
}

.row-assistant {
  align-self: flex-start;
  justify-content: flex-start;
}

.message-avatar {
  font-size: 1.6rem;
  line-height: 1;
}

.message-bubble {
  border-radius: 18px;
  padding: 1rem 1.25rem;
  max-width: 85%;
  word-break: break-word;
  line-height: 1.6;
}

.row-user .message-bubble {
  background: #0284c7;
  color: #ffffff;
  border-bottom-right-radius: 4px;
}

.row-assistant .message-bubble {
  background: #ffffff;
  color: #1e293b;
  border: 1px solid #e2e8f0;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.chat-product-card {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 1rem;
  margin-top: 1rem;
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-icon {
  font-size: 2rem;
}

.card-info {
  flex: 1;
}

.card-title {
  font-weight: 700;
  color: #0f172a;
}

.card-meta {
  font-size: 0.8rem;
  color: #64748b;
}

/* Typing indicator */
.typing-indicator span {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: #94a3b8;
  border-radius: 50%;
  margin-right: 4px;
  animation: blink 1.4s infinite both;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0%, 80%, 100% { opacity: 0.2; }
  40% { opacity: 1; }
}

/* Chat Input */
.chat-input-bar {
  padding: 1rem 1.5rem;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
}

.input-wrapper {
  max-width: 820px;
  margin: auto;
  position: relative;
  display: flex;
  align-items: flex-end;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  border-radius: 16px;
  padding: 8px 12px;
}

.chat-textarea {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  resize: none;
  font-size: 0.95rem;
  line-height: 1.5;
  color: #0f172a;
  max-height: 180px;
}

.btn-send {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #0284c7;
  color: #ffffff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  transition: all 0.2s;
}

.btn-send:hover:not(:disabled) {
  background: #0369a1;
}

.btn-send:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-stop {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #ef4444;
  color: #ffffff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
}
</style>
