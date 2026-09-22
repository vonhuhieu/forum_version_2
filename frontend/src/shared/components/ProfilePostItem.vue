<template>
  <div class="profile-post-card" :id="`profile-post-${post.id}`">
    <div class="profile-post-row">
      <!-- Cột bên trái: Avatar người đăng bài -->
      <div class="post-left-col">
        <user-profile-popup :user="post.author">
          <div 
            class="post-author-avatar" 
            :style="!isAvatarUrl(post.author?.avatar) ? { backgroundColor: post.author?.avatar || '#1a507a' } : {}"
          >
            <img v-if="isAvatarUrl(post.author?.avatar)" :src="formatAvatarUrl(post.author?.avatar)" alt="avatar" />
            <span v-else>{{ (post.author?.displayName || post.author?.username || '?').charAt(0).toUpperCase() }}</span>
          </div>
        </user-profile-popup>
      </div>

      <!-- Cột bên phải: Toàn bộ nội dung và tương tác -->
      <div class="post-right-col">
        <!-- Chế độ chỉnh sửa bài đăng (Thay thế cả cột bên phải bằng CustomEditor + Lưu/Hủy) -->
        <div v-if="isEditing" class="post-inline-editor-box">
          <CustomEditor
            v-model="editContent"
            minHeight="140px"
            :is-edit="true"
          />
          <div class="inline-editor-actions">
            <button 
              class="btn-save-edit" 
              :disabled="isSavingEdit || !editContent.trim()" 
              @click="saveEditPost"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                <polyline points="17 21 17 13 7 13 7 21"></polyline>
                <polyline points="7 3 7 8 15 8"></polyline>
              </svg>
              {{ isSavingEdit ? 'Đang lưu...' : 'Lưu' }}
            </button>
            <button class="btn-cancel-edit" @click="cancelEditPost">Hủy</button>
          </div>
        </div>

        <!-- Chế độ hiển thị bình thường -->
        <template v-else>
          <!-- Khối nội dung bài đăng (nền trắng) -->
          <div class="post-main-content-block">
            <!-- Dòng 1: Tên hiển thị + Thời gian đăng -->
            <div class="post-header-line">
              <!-- Mobile inline avatar (chỉ hiện trên mobile khi cột avatar trái bị ẩn) -->
              <div class="post-mobile-avatar">
                <user-profile-popup :user="post.author">
                  <div 
                    class="post-author-avatar-mobile" 
                    :style="!isAvatarUrl(post.author?.avatar) ? { backgroundColor: post.author?.avatar || '#1a507a' } : {}"
                  >
                    <img v-if="isAvatarUrl(post.author?.avatar)" :src="formatAvatarUrl(post.author?.avatar)" alt="avatar" />
                    <span v-else>{{ authorInitial }}</span>
                  </div>
                </user-profile-popup>
              </div>

              <user-profile-popup :user="post.author">
                <span class="post-author-name">
                  {{ post.author?.displayName || post.author?.username }}
                </span>
              </user-profile-popup>
              <VerifiedBadge :user="post.author" size="15px" />
              <span class="meta-dot">·</span>
              <span class="post-time" :title="post.createdAt">{{ formatDate(post.createdAt) }}</span>
            </div>

            <!-- Dòng 2: Nội dung bài đăng (cắt bớt có "..." nếu quá dài) -->
            <div class="post-content-line">
              <div 
                class="post-html-content ql-editor"
                :class="{ 'is-clamped': isContentClamped && !isExpanded }"
                v-html="post.content"
              ></div>
              <button 
                v-if="isContentClamped" 
                class="btn-toggle-expand" 
                @click="isExpanded = !isExpanded"
              >
                {{ isExpanded ? 'Thu gọn' : '... Xem thêm' }}
              </button>
            </div>

            <!-- Dòng 3: Action Buttons & Reaction -->
            <div class="post-action-bar">
              <div class="action-left-group">
                <!-- Nếu là bài của chính tài khoản đang đăng nhập: Sửa và Xóa -->
                <template v-if="isMyPost">
                  <button class="btn-post-action" @click="startEditPost">Chỉnh sửa</button>
                  <button class="btn-post-action btn-danger-text desktop-action-btn" @click="confirmDeletePost">Xóa</button>

                  <!-- Mobile options dropdown -->
                  <div class="mobile-action-dropdown-wrapper">
                    <button 
                      class="btn-options-toggle" 
                      :class="{ 'is-active': isPostOptionsOpen }"
                      @click.stop="togglePostOptions"
                      title="Tùy chọn khác"
                    >
                      <span class="dots-text">•••</span>
                      <svg class="arrow-down-icon" width="8" height="6" viewBox="0 0 8 6" fill="currentColor">
                        <path d="M0 1l4 4 4-4z"/>
                      </svg>
                    </button>
                    <div class="options-dropdown-menu" v-if="isPostOptionsOpen" @click.stop>
                      <div class="options-menu-header">Tùy chọn khác</div>
                      <button class="options-menu-item btn-danger-text" @click="confirmDeletePost(); isPostOptionsOpen = false">
                        Xóa
                      </button>
                    </div>
                  </div>
                </template>

                <!-- Nếu là bài của người khác: Hiển thị Report, Reaction, và Xóa nếu là chủ tường/admin -->
                <template v-else>
                  <button 
                    v-if="isLoggedIn" 
                    class="btn-post-action" 
                    @click="triggerReport('PROFILE_POST', post.id)"
                  >
                    Report
                  </button>

                  <!-- Quyền xóa bảo vệ chủ nhà / Admin: trên desktop hiện trực tiếp, trên mobile đưa vào options -->
                  <button 
                    v-if="post.canDelete" 
                    class="btn-post-action btn-danger-text desktop-action-btn" 
                    @click="confirmDeletePost" 
                    title="Xóa bài viết trên hồ sơ của bạn"
                  >
                    Xóa
                  </button>

                  <div class="mobile-action-dropdown-wrapper" v-if="post.canDelete">
                    <button 
                      class="btn-options-toggle" 
                      :class="{ 'is-active': isPostOptionsOpen }"
                      @click.stop="togglePostOptions"
                      title="Tùy chọn khác"
                    >
                      <span class="dots-text">•••</span>
                      <svg class="arrow-down-icon" width="8" height="6" viewBox="0 0 8 6" fill="currentColor">
                        <path d="M0 1l4 4 4-4z"/>
                      </svg>
                    </button>
                    <div class="options-dropdown-menu" v-if="isPostOptionsOpen" @click.stop>
                      <div class="options-menu-header">Tùy chọn khác</div>
                      <button class="options-menu-item btn-danger-text" @click="confirmDeletePost(); isPostOptionsOpen = false">
                        Xóa
                      </button>
                    </div>
                  </div>
                </template>
              </div>

              <!-- Reaction Button nằm bên phải (XenForo / voz style) -->
              <div class="action-right-group">
                <ReactionButton
                  v-if="canShowReactionForPost"
                  :targetId="post.id"
                  type="profile-post"
                  :allIcons="reactionIconsList"
                  :userReaction="localCurrentUserReaction"
                  @reaction-updated="handlePostReactionUpdated"
                  @reaction-failed="handleReactionFailed"
                />
              </div>
            </div>

            <!-- Thanh tổng hợp Reaction (Reaction Summary) nếu có -->
            <div class="post-reactions-summary-wrapper" v-if="localReactionSummary && localReactionSummary.length > 0">
              <ReactionSummary
                :summary="localReactionSummary"
                :recentReactors="post.recentReactors"
              />
            </div>
          </div>

          <!-- Dòng 4: Khung Bình luận (Comments Section - Box riêng biệt chuẩn voz) -->
          <div class="post-comments-container" v-if="comments.length > 0 || isLoggedIn">
            <!-- Nút "Xem các bình luận trước" hoặc "Đóng các bình luận cũ" -->
            <div class="comments-pagination-toggle" v-if="hasPreviousComments">
              <button 
                class="btn-toggle-previous-comments" 
                :disabled="loadingComments" 
                @click="togglePreviousComments"
              >
                <span v-if="loadingComments">Đang tải bình luận...</span>
                <span v-else-if="allCommentsLoaded">Đóng các bình luận cũ</span>
                <span v-else>Xem các bình luận trước</span>
              </button>
            </div>

            <!-- Danh sách các bình luận con -->
            <div class="comments-list" v-if="comments.length > 0">
              <div 
                v-for="comment in comments" 
                :key="comment.id" 
                class="comment-row-item"
                :id="`profile-post-comment-${comment.id}`"
              >
                <!-- Cột trái bình luận: Avatar nhỏ -->
                <div class="comment-avatar-col">
                  <user-profile-popup :user="comment.author">
                    <div 
                      class="comment-avatar" 
                      :style="!isAvatarUrl(comment.author?.avatar) ? { backgroundColor: comment.author?.avatar || '#1a507a' } : {}"
                    >
                      <img v-if="isAvatarUrl(comment.author?.avatar)" :src="formatAvatarUrl(comment.author?.avatar)" alt="avatar" />
                      <span v-else>{{ (comment.author?.displayName || comment.author?.username || '?').charAt(0).toUpperCase() }}</span>
                    </div>
                  </user-profile-popup>
                </div>

                <!-- Cột phải bình luận -->
                <div class="comment-body-col">
                  <!-- Chế độ sửa bình luận -->
                  <div v-if="editingCommentId === comment.id" class="comment-inline-edit-box">
                    <CustomEditor
                      v-model="editCommentContent"
                      minHeight="100px"
                      :is-edit="true"
                    />
                    <div class="inline-editor-actions mt-2">
                      <button 
                        class="btn-save-edit btn-sm" 
                        :disabled="isSavingEditComment || !editCommentContent.trim()" 
                        @click="saveEditComment(comment)"
                      >
                        {{ isSavingEditComment ? 'Đang lưu...' : 'Lưu' }}
                      </button>
                      <button class="btn-cancel-edit btn-sm" @click="cancelEditComment">Hủy</button>
                    </div>
                  </div>

                  <!-- Chế độ xem bình luận -->
                  <template v-else>
                    <div class="comment-author-and-content">
                      <div class="comment-author-header">
                        <user-profile-popup :user="comment.author">
                          <span class="comment-author-name">
                            {{ comment.author?.displayName || comment.author?.username }}
                          </span>
                        </user-profile-popup>
                        <VerifiedBadge :user="comment.author" size="13px" />
                      </div>
                      <div class="comment-html-text ql-editor" v-html="comment.content"></div>
                    </div>

                    <!-- Meta row: Thời gian & Action buttons chung 1 hàng (như voz hình 5) -->
                    <div class="comment-meta-row">
                      <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
                      
                      <!-- Action buttons -->
                      <template v-if="isLoggedIn">
                        <!-- Nếu là bài của mình -->
                        <template v-if="isMyComment(comment)">
                          <button class="btn-comment-action" @click="startEditComment(comment)">Chỉnh sửa</button>
                          <button class="btn-comment-action btn-danger-text desktop-comment-action" @click="confirmDeleteComment(comment)">Xóa</button>

                          <!-- Mobile options dropdown -->
                          <div class="mobile-action-dropdown-wrapper">
                            <button 
                              class="btn-options-toggle btn-sm-options" 
                              :class="{ 'is-active': openCommentOptionsId === comment.id }"
                              @click.stop="toggleCommentOptions(comment.id)"
                              title="Tùy chọn khác"
                            >
                              <span class="dots-text">•••</span>
                              <svg class="arrow-down-icon" width="8" height="6" viewBox="0 0 8 6" fill="currentColor">
                                <path d="M0 1l4 4 4-4z"/>
                              </svg>
                            </button>
                            <div class="options-dropdown-menu" v-if="openCommentOptionsId === comment.id" @click.stop>
                              <div class="options-menu-header">Tùy chọn khác</div>
                              <button class="options-menu-item btn-danger-text" @click="confirmDeleteComment(comment); openCommentOptionsId = null">
                                Xóa
                              </button>
                            </div>
                          </div>
                        </template>

                        <!-- Nếu không phải bài của mình -->
                        <template v-else>
                          <button 
                            class="btn-comment-action" 
                            @click="triggerReport('PROFILE_POST_COMMENT', comment.id)"
                          >
                            Report
                          </button>
                          <button 
                            v-if="comment.canDelete" 
                            class="btn-comment-action btn-danger-text desktop-comment-action" 
                            @click="confirmDeleteComment(comment)" 
                            title="Xóa bình luận trên hồ sơ của bạn"
                          >
                            Xóa
                          </button>

                          <!-- Mobile options nếu có quyền xóa bình luận của người khác -->
                          <div class="mobile-action-dropdown-wrapper" v-if="comment.canDelete">
                            <button 
                              class="btn-options-toggle btn-sm-options" 
                              :class="{ 'is-active': openCommentOptionsId === comment.id }"
                              @click.stop="toggleCommentOptions(comment.id)"
                              title="Tùy chọn khác"
                            >
                              <span class="dots-text">•••</span>
                              <svg class="arrow-down-icon" width="8" height="6" viewBox="0 0 8 6" fill="currentColor">
                                <path d="M0 1l4 4 4-4z"/>
                              </svg>
                            </button>
                            <div class="options-dropdown-menu" v-if="openCommentOptionsId === comment.id" @click.stop>
                              <div class="options-menu-header">Tùy chọn khác</div>
                              <button class="options-menu-item btn-danger-text" @click="confirmDeleteComment(comment); openCommentOptionsId = null">
                                Xóa
                              </button>
                            </div>
                          </div>

                          <div class="comment-reaction-btn-wrapper">
                            <ReactionButton
                              :targetId="comment.id"
                              type="profile-post-comment"
                              :allIcons="reactionIconsList"
                              :userReaction="comment.currentUserReaction"
                              @reaction-updated="handleCommentReactionUpdated(comment, $event)"
                              @reaction-failed="handleReactionFailed"
                            />
                          </div>
                        </template>
                      </template>

                      <!-- Reaction Summary cho bình luận nếu có -->
                      <div class="comment-reactions-summary" v-if="comment.reactionSummary && comment.reactionSummary.length > 0">
                        <ReactionSummary
                          :summary="comment.reactionSummary"
                        />
                      </div>
                    </div>
                  </template>
                </div>
              </div>
            </div>

            <!-- Form Nhập Bình luận mới (Dưới danh sách bình luận) -->
            <div class="comment-input-wrapper">
              <div class="comment-input-avatar-col">
                <div 
                  class="comment-avatar my-avatar" 
                  :style="!isAvatarUrl(currentUserAvatar) ? { backgroundColor: currentUserAvatar || '#1a507a' } : {}"
                >
                  <img v-if="isAvatarUrl(currentUserAvatar)" :src="formatAvatarUrl(currentUserAvatar)" alt="my-avatar" />
                  <span v-else>{{ currentUserInitial }}</span>
                </div>
              </div>

              <div class="comment-input-box-col">
                <!-- Khung giả lập (placeholder + cây bút) -->
                <div 
                  v-if="!showCommentInput" 
                  class="fake-input-placeholder" 
                  @click="openCommentInput"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="pen-icon">
                    <path d="M12 20h9"></path>
                    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
                  </svg>
                  <span>Viết một bình luận...</span>
                </div>

                <!-- Khi click vào: Nở ra trình soạn thảo CustomEditor -->
                <div v-else class="comment-editor-expanded">
                  <CustomEditor
                    v-model="commentContent"
                    minHeight="100px"
                    ref="commentEditorRef"
                    :autoFocus="true"
                  />
                  <div class="comment-editor-actions">
                    <button 
                      class="btn-submit-comment" 
                      :disabled="isSubmittingComment || !commentContent.trim()" 
                      @click="submitComment"
                    >
                      {{ isSubmittingComment ? 'Đang đăng...' : 'Đăng bình luận' }}
                    </button>
                    <button 
                      class="btn-cancel-comment" 
                      @click="cancelCommentInput"
                    >
                      Hủy
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- Modal Báo cáo -->
    <ReportModal
      v-model:show="showReportModal"
      :submitting="submittingReport"
      @submit="handleReportSubmit"
    />
  </div>
</template>

<script>
import CustomEditor from '@/shared/components/CustomEditor.vue'
import ReactionButton from '@/shared/components/ReactionButton.vue'
import ReactionSummary from '@/shared/components/ReactionSummary.vue'
import ReportModal from '@/shared/components/ReportModal.vue'
import UserProfilePopup from '@/shared/components/UserProfilePopup.vue'
import VerifiedBadge from '@/shared/components/VerifiedBadge.vue'
import { formatForumDate } from '@/shared/utils/date'
import { isAvatarUrl, formatAvatarUrl } from '@/shared/utils/utils'
import { alertConfirm, toastSuccess, toastError } from '@/shared/utils/swal'
import profilePostService from '@/apps/Forum/services/profile-post.service'
import reportService from '@/apps/Forum/services/report.service'

export default {
  name: 'ProfilePostItem',
  components: {
    CustomEditor,
    ReactionButton,
    ReactionSummary,
    ReportModal,
    UserProfilePopup,
    VerifiedBadge
  },
  props: {
    post: {
      type: Object,
      required: true
    },
    currentUser: {
      type: Object,
      default: () => ({})
    },
    reactionIconsList: {
      type: Array,
      default: () => []
    },
    isLoggedIn: {
      type: Boolean,
      default: false
    }
  },
  emits: ['deleted', 'updated'],
  data() {
    return {
      isEditing: false,
      editContent: '',
      isSavingEdit: false,
      isExpanded: false,
      isContentClamped: false,

      // Comments state
      comments: [],
      initialLatestComments: [],
      commentsPage: 0,
      totalCommentsCount: 0,
      loadingComments: false,
      allCommentsLoaded: false,

      // New Comment input
      showCommentInput: false,
      commentContent: '',
      isSubmittingComment: false,

      // Edit Comment
      editingCommentId: null,
      editCommentContent: '',
      isSavingEditComment: false,

      // Report
      showReportModal: false,
      reportTargetType: 'PROFILE_POST',
      reportTargetId: null,
      submittingReport: false,

      // Local Reaction State to avoid mutating post prop
      localCurrentUserReaction: this.post.currentUserReaction || null,
      localReactionSummary: this.post.reactionSummary || [],

      // Mobile Options Dropdown State
      isPostOptionsOpen: false,
      openCommentOptionsId: null
    }
  },
  computed: {
    authorInitial() {
      const name = this.post.author?.displayName || this.post.author?.username || '?'
      return name.charAt(0).toUpperCase()
    },
    isMyPost() {
      if (!this.currentUser || !this.currentUser.username) return false
      return this.post.author && this.post.author.username === this.currentUser.username
    },
    canShowReactionForPost() {
      if (!this.isLoggedIn) return false
      return !this.isMyPost
    },
    hasPreviousComments() {
      return this.totalCommentsCount > 3
    },
    currentUserAvatar() {
      return this.currentUser?.avatar || ''
    },
    currentUserInitial() {
      const name = this.currentUser?.displayName || this.currentUser?.username || '?'
      return name.charAt(0).toUpperCase()
    }
  },
  watch: {
    'post.latestComments': {
      immediate: true,
      handler(newVal) {
        this.comments = newVal ? [...newVal] : []
        this.initialLatestComments = newVal ? [...newVal] : []
        this.totalCommentsCount = this.post.totalComments || this.post.commentsCount || 0
      }
    },
    'post.currentUserReaction': {
      immediate: true,
      handler(newVal) {
        this.localCurrentUserReaction = newVal || null
      }
    },
    'post.reactionSummary': {
      immediate: true,
      handler(newVal) {
        this.localReactionSummary = newVal ? [...newVal] : []
      }
    }
  },
  mounted() {
    this.checkContentLength()
    document.addEventListener('click', this.closeAllOptions)
  },
  beforeUnmount() {
    document.removeEventListener('click', this.closeAllOptions)
  },
  methods: {
    togglePostOptions() {
      this.isPostOptionsOpen = !this.isPostOptionsOpen
      this.openCommentOptionsId = null
    },
    toggleCommentOptions(commentId) {
      this.openCommentOptionsId = this.openCommentOptionsId === commentId ? null : commentId
      this.isPostOptionsOpen = false
    },
    closeAllOptions() {
      this.isPostOptionsOpen = false
      this.openCommentOptionsId = null
    },
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    formatAvatarUrl(avatar) {
      return formatAvatarUrl(avatar)
    },
    formatDate(dateStr) {
      return formatForumDate(dateStr)
    },
    checkContentLength() {
      if (!this.post.content) return
      const text = this.post.content.replace(/<[^>]*>/g, '').trim()
      if (text.length > 300) {
        this.isContentClamped = true
      }
    },
    isMyComment(comment) {
      if (!this.currentUser || !this.currentUser.username) return false
      return comment.author && comment.author.username === this.currentUser.username
    },

    // --- Post Editing ---
    startEditPost() {
      this.editContent = this.post.content
      this.isEditing = true
    },
    cancelEditPost() {
      this.isEditing = false
      this.editContent = ''
    },
    async saveEditPost() {
      if (!this.editContent.trim()) {
        toastError('Nội dung không được để trống')
        return
      }
      this.isSavingEdit = true
      try {
        const res = await profilePostService.updateProfilePost(this.post.id, this.editContent)
        toastSuccess('Cập nhật bài viết thành công')
        this.isEditing = false
        if (res.data) {
          this.$emit('updated', res.data)
        }
      } catch (e) {
        console.error('Lỗi khi sửa bài viết:', e)
        toastError(e.response?.data?.message || 'Có lỗi xảy ra khi cập nhật')
      } finally {
        this.isSavingEdit = false
      }
    },

    // --- Post Deleting ---
    async confirmDeletePost() {
      const confirmRes = await alertConfirm(
        'Xác nhận xóa',
        'Bạn có chắc chắn muốn xóa bài viết này không? Toàn bộ bình luận liên quan cũng sẽ bị xóa.'
      )
      if (!confirmRes.isConfirmed) return

      try {
        await profilePostService.deleteProfilePost(this.post.id)
        toastSuccess('Đã xóa bài viết thành công')
        this.$emit('deleted', this.post.id)
      } catch (e) {
        console.error('Lỗi khi xóa bài viết:', e)
        toastError(e.response?.data?.message || 'Có lỗi xảy ra khi xóa bài viết')
      }
    },

    // --- Reaction for Post ---
    handlePostReactionUpdated(icon) {
      this.localCurrentUserReaction = icon
    },
    handleCommentReactionUpdated(comment, icon) {
      comment.currentUserReaction = icon
    },
    handleReactionFailed() {
      // no-op or refresh
    },

    // --- Comments Pagination & Collapse ---
    async togglePreviousComments() {
      if (this.allCommentsLoaded) {
        // Thu gọn về 3 bình luận mới nhất
        try {
          const res = await profilePostService.getComments(this.post.id, 0, 3)
          if (res.data && res.data.content) {
            this.comments = res.data.content
            this.initialLatestComments = [...this.comments]
          } else {
            this.comments = this.comments.slice(-3)
          }
        } catch (e) {
          this.comments = this.comments.slice(-3)
        }
        this.allCommentsLoaded = false
        this.commentsPage = 0
        return
      }

      this.loadingComments = true
      try {
        const nextPage = this.commentsPage + 1
        const res = await profilePostService.getComments(this.post.id, nextPage, 3)
        if (res.data && res.data.content) {
          const olderBatch = res.data.content // đã được sắp xếp tăng dần theo thời gian
          
          // Prepend vào danh sách comments hiện tại, lọc trùng id
          const existingIds = new Set(this.comments.map(c => c.id))
          const newComments = olderBatch.filter(c => !existingIds.has(c.id))
          this.comments = [...newComments, ...this.comments]
          this.commentsPage = nextPage

          // Kiểm tra xem đã nạp hết chưa
          if (this.comments.length >= this.totalCommentsCount || nextPage >= (res.data.totalPages - 1)) {
            this.allCommentsLoaded = true
          }
        } else {
          this.allCommentsLoaded = true
        }
      } catch (e) {
        console.error('Lỗi khi tải bình luận cũ:', e)
        toastError('Không thể tải thêm bình luận')
      } finally {
        this.loadingComments = false
      }
    },

    // --- Comment Input Form ---
    openCommentInput() {
      if (!this.isLoggedIn) {
        toastError('Vui lòng đăng nhập để bình luận')
        return
      }
      this.showCommentInput = true
      this.$nextTick(() => {
        setTimeout(() => {
          this.$refs.commentEditorRef?.focus()
        }, 60)
      })
    },
    cancelCommentInput() {
      this.showCommentInput = false
      this.commentContent = ''
    },
    async submitComment() {
      if (!this.commentContent.trim()) {
        toastError('Vui lòng nhập nội dung bình luận')
        return
      }

      this.isSubmittingComment = true
      try {
        const res = await profilePostService.createComment(this.post.id, this.commentContent)
        toastSuccess('Đăng bình luận thành công')
        
        if (res.data) {
          this.totalCommentsCount++
          if (this.allCommentsLoaded) {
            this.comments.push(res.data)
          } else {
            this.comments.push(res.data)
            // Nếu vượt quá 3 bình luận và chưa mở rộng xem toàn bộ, chỉ hiển thị 3 bình luận mới nhất
            if (this.comments.length > 3) {
              this.comments = this.comments.slice(-3)
            }
          }
          this.initialLatestComments = [...this.comments]
        }

        // Reset form về mặc định
        this.showCommentInput = false
        this.commentContent = ''
      } catch (e) {
        console.error('Lỗi khi đăng bình luận:', e)
        toastError(e.response?.data?.message || 'Có lỗi xảy ra khi đăng bình luận')
      } finally {
        this.isSubmittingComment = false
      }
    },

    // --- Comment Edit & Delete ---
    startEditComment(comment) {
      this.editingCommentId = comment.id
      this.editCommentContent = comment.content
    },
    cancelEditComment() {
      this.editingCommentId = null
      this.editCommentContent = ''
    },
    async saveEditComment(comment) {
      if (!this.editCommentContent.trim()) {
        toastError('Nội dung bình luận không được để trống')
        return
      }
      this.isSavingEditComment = true
      try {
        const res = await profilePostService.updateComment(comment.id, this.editCommentContent)
        toastSuccess('Cập nhật bình luận thành công')
        if (res.data) {
          comment.content = res.data.content
          comment.updatedAt = res.data.updatedAt
        }
        this.editingCommentId = null
        this.editCommentContent = ''
      } catch (e) {
        console.error('Lỗi khi sửa bình luận:', e)
        toastError(e.response?.data?.message || 'Có lỗi xảy ra khi cập nhật bình luận')
      } finally {
        this.isSavingEditComment = false
      }
    },
    async confirmDeleteComment(comment) {
      const confirmRes = await alertConfirm('Xác nhận xóa', 'Bạn có chắc chắn muốn xóa bình luận này?')
      if (!confirmRes.isConfirmed) return

      try {
        await profilePostService.deleteComment(comment.id)
        toastSuccess('Đã xóa bình luận')
        this.totalCommentsCount = Math.max(0, this.totalCommentsCount - 1)

        if (this.allCommentsLoaded || this.totalCommentsCount <= 3) {
          this.comments = this.comments.filter(c => c.id !== comment.id)
          this.initialLatestComments = this.initialLatestComments.filter(c => c.id !== comment.id)
          if (this.totalCommentsCount <= 3) {
            this.allCommentsLoaded = false
          }
        } else {
          // Đang ở chế độ xem 3 bình luận mới nhất nhưng tổng số bình luận còn lại > 3:
          // Nạp lại 3 bình luận mới nhất từ máy chủ để bù bình luận cũ vào cho đủ 3 bình luận
          try {
            const res = await profilePostService.getComments(this.post.id, 0, 3)
            if (res.data && res.data.content) {
              this.comments = res.data.content
              this.initialLatestComments = [...this.comments]
            } else {
              this.comments = this.comments.filter(c => c.id !== comment.id)
            }
          } catch (fetchErr) {
            this.comments = this.comments.filter(c => c.id !== comment.id)
          }
        }
      } catch (e) {
        console.error('Lỗi khi xóa bình luận:', e)
        toastError(e.response?.data?.message || 'Có lỗi xảy ra khi xóa bình luận')
      }
    },

    // --- Report ---
    triggerReport(targetType, targetId) {
      if (!this.isLoggedIn) {
        toastError('Vui lòng đăng nhập để báo cáo')
        return
      }
      this.reportTargetType = targetType
      this.reportTargetId = targetId
      this.showReportModal = true
    },
    async handleReportSubmit(reason) {
      this.submittingReport = true
      try {
        await reportService.create({
          targetType: this.reportTargetType,
          targetId: this.reportTargetId,
          reason
        })
        toastSuccess('Báo cáo vi phạm đã được gửi thành công')
        this.showReportModal = false
      } catch (e) {
        console.error('Lỗi gửi báo cáo:', e)
        toastError(e.response?.data?.message || 'Có lỗi xảy ra khi gửi báo cáo')
      } finally {
        this.submittingReport = false
      }
    }
  }
}
</script>

<style scoped>
.profile-post-card {
  background: #ffffff;
  border: 1px solid #d8dbe0;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  margin-bottom: 12px;
  transition: all 0.2s ease;
}

.profile-post-row {
  display: flex;
  align-items: stretch;
  width: 100%;
}

.post-left-col {
  width: 64px;
  flex-shrink: 0;
  background-color: #f5f6f8;
  border-right: 1px solid #d8dbe0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 14px;
}

.post-author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 1.05rem;
  background-color: #1a507a;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}
.post-author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.post-right-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  position: relative;
  padding: 14px 16px 16px;
}

.post-right-col::before,
.post-right-col::after {
  content: '';
  position: absolute;
  border-style: solid;
  display: block;
  width: 0;
  height: 0;
  pointer-events: none;
  z-index: 2;
}

.post-right-col::before {
  top: 24px;
  right: 100%;
  border-width: 8px 8px 8px 0;
  border-color: transparent #d8dbe0 transparent transparent;
}

.post-right-col::after {
  top: 25px;
  right: calc(100% - 1px);
  border-width: 7px 7px 7px 0;
  border-color: transparent #ffffff transparent transparent;
}

.post-main-content-block {
  background-color: transparent;
}

/* Dòng 1: Header Line */
.post-header-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}

.post-author-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: #1a507a;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.15s;
}
.post-author-name:hover {
  text-decoration: underline;
  color: #d13838;
}

.meta-dot {
  color: #8c9ba5;
  font-size: 0.9rem;
}

.post-time {
  font-size: 0.82rem;
  color: #7f8c8d;
}

/* Dòng 2: Content */
.post-content-line {
  margin-bottom: 10px;
}

.post-html-content {
  font-size: 0.92rem;
  color: #2c3e50;
  line-height: 1.5;
  word-break: break-word;
}
.post-html-content.is-clamped {
  max-height: 110px;
  overflow: hidden;
  position: relative;
}

.btn-toggle-expand {
  background: none;
  border: none;
  color: #1a507a;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  padding: 2px 0;
  margin-top: 4px;
}
.btn-toggle-expand:hover {
  text-decoration: underline;
}

/* Dòng 3: Action bar */
.post-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
  border: none;
  margin-bottom: 8px;
}

.action-left-group {
  display: flex;
  align-items: center;
  gap: 14px;
}

.btn-post-action {
  background: none;
  border: none;
  color: #1a507a;
  font-size: 0.84rem;
  font-weight: 500;
  cursor: pointer;
  padding: 2px 4px;
  transition: color 0.15s;
}
.btn-post-action:hover {
  color: #d13838;
  text-decoration: underline;
}
.btn-post-action.btn-danger-text:hover {
  color: #e74c3c;
}

.action-right-group {
  display: flex;
  align-items: center;
}

.post-reactions-summary-wrapper {
  margin-bottom: 10px;
}

/* Dòng 4: Comments */
.post-comments-container {
  background-color: #f5f6f8;
  border: 1px solid #e1e7ec;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.comments-pagination-toggle {
  text-align: left;
  padding: 8px 14px;
  border-bottom: 2px solid #dce2e8;
}

.btn-toggle-previous-comments {
  background: none;
  border: none;
  color: #1a507a;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  padding: 2px 4px;
}
.btn-toggle-previous-comments:hover {
  text-decoration: underline;
}

.comments-list {
  display: flex;
  flex-direction: column;
}

.comment-row-item {
  display: flex;
  gap: 10px;
  padding: 10px 14px;
  border-bottom: 1.5px solid #dce2e8;
  align-items: flex-start;
}

.comments-list:last-child .comment-row-item:last-child {
  border-bottom: none;
}

.comment-avatar-col {
  flex-shrink: 0;
  padding-top: 0;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 0.85rem;
  background-color: #1a507a;
}
.comment-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comment-body-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.comment-author-and-content {
  background: transparent;
  border: none;
  padding: 0;
  box-shadow: none;
}

.comment-author-header {
  display: flex;
  align-items: center;
  gap: 4px;
  line-height: 1;
  margin-top: 0;
}

.comment-author-name {
  font-size: 0.88rem;
  font-weight: 700;
  color: #1a507a;
  text-decoration: none;
  cursor: pointer;
  margin-right: 4px;
  line-height: 1.1;
  transition: color 0.15s;
}
.comment-author-name:hover {
  text-decoration: underline;
  color: #d13838;
}

.comment-html-text {
  font-size: 0.88rem;
  color: #2c3e50;
  margin: 3px 0 5px;
  word-break: break-word;
  line-height: 1.45;
}

.comment-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 2px;
  padding-left: 0;
}

.comment-time {
  font-size: 0.78rem;
  color: #8c9ba5;
}

.btn-comment-action {
  background: none;
  border: none;
  color: #1a507a;
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  padding: 0 2px;
  transition: color 0.15s;
}
.btn-comment-action:hover {
  color: #d13838;
  text-decoration: underline;
}
.btn-comment-action.btn-danger-text:hover {
  color: #e74c3c;
}

.comment-reaction-btn-wrapper {
  display: inline-flex;
}

/* Form Nhập Bình luận */
.comment-input-wrapper {
  display: flex;
  gap: 10px;
  padding: 10px 14px;
  align-items: flex-start;
}

.comment-input-avatar-col {
  flex-shrink: 0;
  padding-top: 0;
}

.comment-input-box-col {
  flex: 1;
  min-width: 0;
  position: relative;
}

.fake-input-placeholder {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: #ffffff;
  border: 1px solid #ccd5dc;
  border-radius: 4px;
  padding: 8px 14px;
  color: #6a8296;
  font-size: 0.88rem;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.fake-input-placeholder:hover {
  border-color: #1a507a;
  background-color: #ffffff;
  box-shadow: 0 1px 4px rgba(26, 80, 122, 0.12);
  color: #1a507a;
}
.fake-input-placeholder:hover .pen-icon {
  color: #1a507a;
}

.pen-icon {
  color: #7d96a8;
  transition: color 0.2s;
}

.comment-editor-expanded {
  background: #ffffff;
  border: 1px solid #ccd5dc;
  border-radius: 4px;
  padding: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.comment-editor-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.btn-submit-comment {
  background: #1a507a;
  color: #ffffff;
  border: none;
  padding: 6px 14px;
  font-size: 0.82rem;
  font-weight: 600;
  border-radius: 3px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.btn-submit-comment:hover:not(:disabled) {
  background: #133a59;
}
.btn-submit-comment:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-cancel-comment {
  background: #e2e8f0;
  color: #4a5568;
  border: none;
  padding: 6px 12px;
  font-size: 0.82rem;
  font-weight: 500;
  border-radius: 3px;
  cursor: pointer;
}
.btn-cancel-comment:hover {
  background: #cbd5e0;
}

/* Inline Editor Styling */
.post-inline-editor-box {
  background: #ffffff;
  border: 1px solid #ccd1d9;
  border-radius: 4px;
  padding: 8px;
}

.inline-editor-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.btn-save-edit {
  background: #1a507a;
  color: #ffffff;
  border: none;
  padding: 6px 16px;
  font-size: 0.84rem;
  font-weight: 600;
  border-radius: 3px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.btn-save-edit:hover:not(:disabled) {
  background: #133a59;
}
.btn-save-edit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-cancel-edit {
  background: #e2e8f0;
  color: #4a5568;
  border: none;
  padding: 6px 14px;
  font-size: 0.84rem;
  font-weight: 500;
  border-radius: 3px;
  cursor: pointer;
}
.btn-cancel-edit:hover {
  background: #cbd5e0;
}

.btn-sm {
  padding: 4px 10px;
  font-size: 0.78rem;
}

/* Mobile Author Avatar in Post Header */
.post-mobile-avatar {
  display: none;
}

.post-author-avatar-mobile {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 0.8rem;
  background-color: #1a507a;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  cursor: pointer;
}
.post-author-avatar-mobile img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Mobile Action Dropdown Options */
.mobile-action-dropdown-wrapper {
  display: none;
  position: relative;
}

.btn-options-toggle {
  background: none;
  border: 1px solid transparent;
  border-radius: 3px;
  color: #1a507a;
  cursor: pointer;
  padding: 1px 6px;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 0.8rem;
  line-height: 1;
  transition: all 0.15s ease;
}

.btn-options-toggle:hover {
  background-color: #edf2f7;
  color: #d13838;
}

.btn-options-toggle.is-active {
  background-color: #1a507a;
  color: #ffffff;
  border-color: #1a507a;
}

.dots-text {
  font-weight: bold;
  letter-spacing: -1px;
  font-size: 0.85rem;
  line-height: 1;
}

.arrow-down-icon {
  width: 7px;
  height: 5px;
  transition: transform 0.2s ease;
}

.btn-options-toggle.is-active .arrow-down-icon {
  transform: rotate(180deg);
}

.btn-sm-options {
  padding: 1px 4px;
  font-size: 0.76rem;
}

.options-dropdown-menu {
  position: absolute;
  top: calc(100% + 7px);
  left: 0;
  min-width: 130px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  padding: 4px 0;
  animation: fadeInDown 0.15s ease;
}

.options-dropdown-menu::before,
.options-dropdown-menu::after {
  content: '';
  position: absolute;
  bottom: 100%;
  left: 14px;
  border-style: solid;
  pointer-events: none;
}

.options-dropdown-menu::before {
  border-width: 0 6px 6px 6px;
  border-color: transparent transparent #cbd5e1 transparent;
}

.options-dropdown-menu::after {
  border-width: 0 5px 5px 5px;
  border-color: transparent transparent #ffffff transparent;
  bottom: calc(100% - 1px);
  left: 15px;
}

.options-menu-header {
  padding: 5px 12px;
  font-size: 0.78rem;
  font-weight: 600;
  color: #1a507a;
  border-bottom: 1px solid #edf2f7;
  background: #f8fafc;
}

.options-menu-item {
  background: none;
  border: none;
  padding: 7px 12px;
  text-align: left;
  font-size: 0.82rem;
  color: #2c3e50;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  transition: background-color 0.15s;
}

.options-menu-item:hover {
  background-color: #edf2f7;
  color: #1a507a;
}

.options-menu-item.btn-danger-text {
  color: #e74c3c;
}
.options-menu-item.btn-danger-text:hover {
  background-color: #fdf2f2;
  color: #c0392b;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Mobile Responsive Breakpoint */
@media (max-width: 767px) {
  .post-left-col {
    display: none !important;
  }
  .post-right-col::before,
  .post-right-col::after {
    display: none !important;
  }
  .post-right-col {
    padding: 12px 14px;
  }
  .post-mobile-avatar {
    display: inline-flex;
    margin-right: 4px;
    vertical-align: middle;
  }
  .desktop-action-btn,
  .desktop-comment-action {
    display: none !important;
  }
  .mobile-action-dropdown-wrapper {
    display: inline-flex;
    align-items: center;
  }
}
</style>
