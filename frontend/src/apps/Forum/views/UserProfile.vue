<template>
  <div>
    <Loading :visible="loading || isUploadLoading" />

    <main class="container" style="padding-top: 2rem;">
      <!-- Breadcrumb -->
      <Breadcrumb :items="breadcrumbItems" />

      <div class="account-layout">
        <!-- Cột chính: Content chính (Full-width) -->
        <div class="account-content">
          <!-- Thành phần 1: Profile Header & Details -->
          <div class="profile-header-card card" :class="{ 'no-banner': !userStats.profileBanner }">
            <!-- Vùng ảnh bìa (banner) - Chỉ hiển thị nếu có ảnh bìa -->
            <div 
              v-if="userStats.profileBanner"
              class="profile-banner-area" 
              :style="{ backgroundImage: `url(${formatAvatarUrl(userStats.profileBanner)})` }"
            >
              <div class="banner-overlay-gradient"></div>
            </div>
            
            <!-- Thông tin tài khoản phía trên (nằm ngoài banner) -->
            <div 
              class="profile-info-upper no-pt-mobile" 
              :class="{ 'text-white': !!userStats.profileBanner, 'positioned-absolute': !!userStats.profileBanner }"
            >
              <div class="profile-avatar-wrapper" :class="{ 'clickable': isCurrentUser }" @click="isCurrentUser && openUploadModal('avatar')">
                <img v-if="isAvatarUrl(userStats.avatar)" :src="formatAvatarUrl(userStats.avatar)" class="profile-avatar-img" />
                <div v-else class="profile-avatar-placeholder" :style="{ backgroundColor: userStats.avatar || '#1a507a' }">
                  {{ userInitial }}
                </div>
                <div v-if="isCurrentUser" class="avatar-edit-overlay">
                  <span>Sửa</span>
                </div>
              </div>

              <div class="profile-meta-details">
                <h1 class="profile-displayname">
                  {{ userStats.displayName || userStats.username }} <VerifiedBadge :user="userStats" size="20px" />
                </h1>
                <div class="profile-title-tag">{{ userStats.displayTitle || 'Thành viên' }}</div>
                <div class="profile-time-row">
                  <span class="meta-item">Tham gia: {{ formatDate(userStats.createdAt) }}</span>
                </div>
                <div class="profile-time-row">
                  <span class="meta-item text-dimmed">Thấy lần gần nhất: {{ formatDate(userStats.lastActiveAt) }}</span>
                </div>
                <!-- Nút hành động nằm dưới meta -->
                <div class="banner-actions" :class="{ 'banner-actions-other': !isCurrentUser }">
                  <button class="btn-banner-action fs-9" @click="triggerReport">Báo cáo</button>
                  <template v-if="!isCurrentUser">
                    <button class="btn-banner-action fs-9" @click="handleFollow" :disabled="loadingFollow">{{ isFollowing ? 'Bỏ theo dõi' : 'Theo dõi' }}</button>
                    <button class="btn-banner-action fs-9" @click="handleBlock">Chặn</button>
                    <button class="btn-banner-action fs-9" @click="startConversation">Bắt đầu đối thoại</button>
                  </template>
                  <button v-else class="btn-banner-action btn-banner-edit fs-9" @click="openUploadModal('banner')">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                      <circle cx="12" cy="13" r="4"></circle>
                    </svg>
                    Đổi ảnh bìa
                  </button>
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

            <!-- Khối nút hành động dàn ngang trên Mobile khi xem trang cá nhân người khác -->
            <div class="profile-mobile-actions" v-if="!isCurrentUser">
              <button class="btn-mobile-action" @click="triggerReport">Báo cáo</button>
              <button class="btn-mobile-action" @click="handleFollow" :disabled="loadingFollow">{{ isFollowing ? 'Bỏ theo dõi' : 'Theo dõi' }}</button>
              <button class="btn-mobile-action" @click="handleBlock">Chặn</button>
              <button class="btn-mobile-action" @click="startConversation">Bắt đầu đối thoại</button>
            </div>

            <!-- Khối nút hành động dàn ngang trên Mobile khi xem trang cá nhân của chính mình -->
            <div class="profile-mobile-actions" v-else>
              <button class="btn-mobile-action" @click="triggerReport">Báo cáo</button>
              <button class="btn-mobile-action btn-banner-edit" @click="openUploadModal('banner')">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                  <circle cx="12" cy="13" r="4"></circle>
                </svg>
                Đổi ảnh bìa
              </button>
            </div>

            <!-- Thành phần 2: Tabs Selection -->
            <div class="profile-tabs-bar">
              <button 
                class="profile-tab-btn pl-and-pr-6" 
                :class="{ 'is-active': activeTab === 'profile_posts' }"
                @click="switchTab('profile_posts')"
              >
                Lưu bút
              </button>
              <button 
                class="profile-tab-btn pl-and-pr-6" 
                :class="{ 'is-active': activeTab === 'posts' }"
                @click="switchTab('posts')"
              >
                Chủ đề ({{ userStats.threadCount || 0 }})
              </button>
              <button 
                class="profile-tab-btn pl-and-pr-6" 
                :class="{ 'is-active': activeTab === 'comments' }"
                @click="switchTab('comments')"
              >
                Bình luận/Phản hồi ({{ userStats.commentCount || 0 }})
              </button>
              <button 
                class="profile-tab-btn pl-and-pr-6" 
                :class="{ 'is-active': activeTab === 'about' }"
                @click="switchTab('about')"
              >
                Giới thiệu
              </button>
            </div>
          </div>

          <!-- Thành phần 3: Danh sách & Phân trang -->
          <div class="profile-tab-content-wrapper">
            <!-- Tab 1: Lưu bút (Profile posts) -->
            <div v-if="activeTab === 'profile_posts'" class="profile-posts-panel">
              <!-- Form nhập liệu trạng thái -->
              <div class="profile-status-input-card">
                <div class="status-input-row" :class="{ 'is-expanded': showStatusEditor }">
                  <!-- Cột trái: Avatar người đang đăng nhập -->
                  <div class="status-avatar-col">
                    <div 
                      class="status-avatar" 
                      :style="!isAvatarUrl(currentUserAvatar) ? { backgroundColor: currentUserAvatar || '#1a507a' } : {}"
                    >
                      <img v-if="isAvatarUrl(currentUserAvatar)" :src="formatAvatarUrl(currentUserAvatar)" alt="avatar" />
                      <span v-else>{{ myInitial }}</span>
                    </div>
                  </div>

                  <!-- Cột phải: Input giả lập / Trình soạn thảo CustomEditor -->
                  <div class="status-form-col no-padding-mobile">
                    <!-- Trạng thái thu gọn mặc định -->
                    <div 
                      v-if="!showStatusEditor" 
                      class="status-fake-input" 
                      @click="openStatusEditor"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="pen-icon">
                        <path d="M12 20h9"></path>
                        <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
                      </svg>
                      <span>Bạn đang nghĩ gì?</span>
                    </div>

                    <!-- Trạng thái mở rộng trình soạn thảo -->
                    <div v-else class="status-editor-expanded">
                      <CustomEditor
                        ref="statusEditorRef"
                        v-model="newPostContent"
                        minHeight="140px"
                        :autoFocus="true"
                        @image-uploaded="handleNewPostImageUploaded"
                        @upload-loading-start="handleUploadLoadingStart"
                        @upload-loading-end="handleUploadLoadingEnd"
                      />

                      <!-- Khối xem trước đính kèm chân bài đăng lưu bút -->
                      <div v-if="newPostAttachedImages && newPostAttachedImages.length > 0" class="attachment-block" style="margin: 1rem 0; border-top: 1px dashed #ddd; padding-top: 1rem;">
                        <div class="attachment-label" style="font-weight: bold; color: #1a507a; margin-bottom: 0.5rem; font-size: 0.9rem;">Đính kèm</div>
                        <div class="attachment-list" style="display: flex; flex-wrap: wrap; gap: 10px;" @click="handleContentImageClick">
                          <img v-for="(img, idx) in newPostAttachedImages" :key="idx" :src="img.url" :alt="img.name" style="width: 150px; height: 150px; object-fit: cover; border: 1px solid #ddd; border-radius: 4px; cursor: zoom-in;" />
                        </div>
                      </div>

                      <ImageUploaderPanel 
                        ref="newPostUploaderPanel" 
                        v-model:images="newPostAttachedImages" 
                        @insert-images="handleNewPostInsertImages" 
                        @upload-loading-start="handleUploadLoadingStart"
                        @upload-loading-end="handleUploadLoadingEnd"
                      />

                      <div class="status-editor-actions">
                        <button 
                          class="btn-submit-status" 
                          :disabled="isSubmittingPost || !newPostContent.trim()" 
                          @click="submitProfilePost"
                        >
                          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="22" y1="2" x2="11" y2="13"></line>
                            <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                          </svg>
                          {{ isSubmittingPost ? 'Đang đăng...' : 'Đăng' }}
                        </button>
                        <button 
                          class="btn-cancel-status" 
                          @click="cancelStatusEditor"
                        >
                          Hủy
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Danh sách bài đăng Lưu bút -->
              <div v-if="profilePostsLoading" class="list-loading-state">
                Đang tải bài viết lưu bút...
              </div>
              <div v-else-if="profilePosts.length === 0" class="list-empty-state">
                Chưa có bài viết lưu bút nào trên hồ sơ này. Hãy là người đầu tiên để lại lời nhắn!
              </div>
              <div v-else class="profile-posts-list">
                <ProfilePostItem
                  v-for="post in profilePosts"
                  :key="post.id"
                  :post="post"
                  :currentUser="currentUser"
                  :reactionIconsList="reactionIconsList"
                  :isLoggedIn="isLoggedIn"
                  @deleted="handleProfilePostDeleted"
                  @updated="handleProfilePostUpdated"
                />
              </div>

              <!-- Phân trang Lưu bút -->
              <div class="pagination-wrapper p-1_5rem-on-pc" v-if="profilePostsTotalPages > 1">
                <ForumPagination 
                  :current-page="profilePostsCurrentPage" 
                  :total-pages="profilePostsTotalPages" 
                  @page-changed="handleProfilePostsPageChange"
                />
              </div>
            </div>

            <!-- Các Tab khác: Bọc trong card riêng biệt để giữ nguyên viền đẹp -->
            <div v-else class="card profile-other-tab-card">
              <!-- Tab: Giới thiệu -->
              <div v-if="activeTab === 'about'" class="about-tab-panel">
                <p class="about-empty-text">Thành viên này chưa viết lời giới thiệu.</p>
              </div>

              <!-- Tab: Chủ đề & Bình luận/Phản hồi -->
              <div v-else class="list-tab-panel">
                <div v-if="listLoading" class="list-loading-state">
                  Đang tải dữ liệu...
                </div>
                <div v-else-if="items.length === 0" class="list-empty-state">
                  Không có nội dung nào được tìm thấy.
                </div>
                <template v-else>
                  <div class="profile-activity-list">
                    <div v-for="item in items" :key="item.id" class="activity-row-item" @click="handleItemClick($event, item)">
                      <!-- Cột bên trái: Avatar -->
                      <div class="item-avatar-col">
                        <user-profile-popup :user="item.author || userStats">
                          <span class="item-avatar" :style="!isAvatarUrl((item.author || userStats).avatar) ? { backgroundColor: (item.author || userStats).avatar || '#ccc' } : {}">
                            <img v-if="isAvatarUrl((item.author || userStats).avatar)" :src="formatAvatarUrl((item.author || userStats).avatar)" />
                            <span v-else>{{ ((item.author || userStats).displayName || (item.author || userStats).username || '?').charAt(0).toUpperCase() }}</span>
                          </span>
                        </user-profile-popup>
                      </div>

                      <!-- Cột bên phải: Nội dung chi tiết -->
                      <div class="item-details-col">
                        <!-- Dòng 1: Tiêu đề thread -->
                        <div class="item-title-row">
                          <span 
                            v-if="item.label" 
                            class="label-tag-mini" 
                            :style="getLabelStyle(item.label)"
                          >
                            {{ item.label.title }}
                          </span>
                          <router-link 
                            :to="getItemRoute(item)" 
                            class="item-title-link" 
                            @click.prevent="navigateToItem(item)"
                          >
                            {{ activeTab === 'posts' ? item.title : item.threadTitle }}
                          </router-link>
                        </div>

                        <!-- Dòng 2: Nội dung xem trước -->
                        <template v-if="activeTab === 'comments'">
                          <template v-for="(q, qi) in parseCommentPreview(item.content).quotes" :key="qi">
                            <div class="mini-quote-box">
                              <span class="mini-quote-author" v-if="q.quoteAuthor" v-html="getQuoteAuthorHtml(q.quoteAuthor, item)">
                              </span>
                              <span class="mini-quote-text">
                                {{ q.quoteText }}
                              </span>
                            </div>
                          </template>
                          <div class="item-content-preview">
                            {{ parseCommentPreview(item.content).replyText || '(Nội dung đính kèm)' }}
                          </div>
                        </template>
                        <template v-else>
                          <div class="item-content-preview">
                            {{ stripHtml(item.content) }}
                          </div>
                        </template>

                        <!-- Dòng 3: Meta metadata -->
                        <div class="item-meta-row">
                          <span class="meta-author">
                            {{ (item.author || userStats).displayName || (item.author || userStats).username }}
                            <VerifiedBadge :user="item.author || userStats" size="14px" />
                          </span>
                          <span class="meta-divider">&middot;</span>
                          <span class="meta-post-number">Post #{{ activeTab === 'posts' ? 1 : item.seqNumber }}</span>
                          <span class="meta-divider">&middot;</span>
                          <span class="meta-time">{{ formatDate(item.createdAt) }}</span>
                        </div>

                        <!-- Dòng 4: Chuyên mục -->
                        <div class="item-category-row">
                          Chuyên mục: 
                          <span class="category-link" @click="goToCategory(item.category)">
                            {{ item.category ? item.category.name : 'Không xác định' }}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Phân trang chung -->
                  <div class="pagination-wrapper p-1_5rem-on-pc" v-if="totalPages > 1">
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

    <!-- Modal Xem ảnh phóng to toàn màn hình -->
    <ImageLightboxModal 
      :visible="showLightbox" 
      :src="lightboxImageUrl" 
      @close="closeLightbox" 
    />
  </div>
</template>

<script>
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import AvatarUploadModal from '@/shared/components/AvatarUploadModal.vue'
import Loading from '@/shared/components/Loading.vue'
import UserProfilePopup from '@/shared/components/UserProfilePopup.vue'
import VerifiedBadge from '@/shared/components/VerifiedBadge.vue'
import ProfilePostItem from '@/shared/components/ProfilePostItem.vue'
import CustomEditor from '@/shared/components/CustomEditor.vue'
import ImageUploaderPanel from '@/shared/components/ImageUploaderPanel.vue'
import ImageLightboxModal from '@/shared/components/ImageLightboxModal.vue'
import { formatForumDate } from '@/shared/utils/date'
import { isAvatarUrl, formatAvatarUrl, getVerifiedBadgeSvgHtml } from '@/shared/utils/utils'
import { alertConfirm, toastSuccess, toastError } from '@/shared/utils/swal'
import userFollowService from '@/apps/Forum/services/user-follow.service'
import profilePostService from '@/apps/Forum/services/profile-post.service'
import reactionService from '@/apps/Forum/services/reaction.service'
import api from '@/shared/services/api.service'
import userMixin from '@/shared/mixins/user.mixin.js'
import editorAttachmentMixin from '@/shared/mixins/editorAttachment.mixin.js'
import imageLightboxMixin from '@/shared/mixins/imageLightbox.mixin.js'

export default {
  name: 'UserProfile',
  mixins: [userMixin, editorAttachmentMixin, imageLightboxMixin],
  components: {
    Breadcrumb,
    ForumPagination,
    AvatarUploadModal,
    Loading,
    UserProfilePopup,
    VerifiedBadge,
    ProfilePostItem,
    CustomEditor,
    ImageUploaderPanel,
    ImageLightboxModal
  },
  data() {
    return {
      loading: false,
      listLoading: false,
      userStats: {},
      activeTab: 'profile_posts', // 'profile_posts' | 'posts' | 'comments' | 'about'
      items: [],
      currentPage: 1,
      totalPages: 1,
      itemsPerPage: 10,
      showUploadModal: false,
      uploadMode: 'avatar', // 'avatar' | 'banner'
      isFollowing: false,
      loadingFollow: false,

      // Profile Posts (Lưu bút) state
      profilePosts: [],
      profilePostsCurrentPage: 1,
      profilePostsTotalPages: 1,
      profilePostsLoading: false,
      showStatusEditor: false,
      newPostContent: '',
      newPostAttachedImages: [],
      isSubmittingPost: false,
      reactionIconsList: []
    }
  },
  computed: {
    currentUser() {
      const currentUserStr = localStorage.getItem('user')
      return currentUserStr ? JSON.parse(currentUserStr) : null
    },
    currentUserAvatar() {
      return this.currentUser?.avatar || ''
    },
    isLoggedIn() {
      return !!localStorage.getItem('token')
    },
    myInitial() {
      const name = this.currentUser?.displayName || this.currentUser?.username || '?'
      return name.charAt(0).toUpperCase()
    },
    targetUsername() {
      const queryUsername = this.$route.query.username
      return queryUsername || this.currentUser?.username || ''
    },
    isCurrentUser() {
      return this.checkIsCurrentUser(this.$route.query.username)
    },
    breadcrumbItems() {
      const displayName = this.userStats.displayName || this.userStats.username || 'Trang cá nhân'
      return [
        { title: 'Thành viên', to: '/thanh-vien' },
        { title: displayName }
      ]
    },
    userInitial() {
      const name = this.userStats.displayName || this.userStats.username || ''
      return name.charAt(0).toUpperCase()
    }
  },
  watch: {
    '$route.query.username': {
      handler(newVal) {
        const currentUserStr = localStorage.getItem('user')
        if (currentUserStr && !newVal) {
          const currentUser = JSON.parse(currentUserStr)
          this.$router.replace({
            name: 'UserProfile',
            query: { username: currentUser.username }
          })
          return
        }
        this.currentPage = 1
        this.profilePostsCurrentPage = 1
        this.activeTab = 'profile_posts'
        this.loadProfileData()
      }
    }
  },
  created() {
    const currentUserStr = localStorage.getItem('user')
    if (currentUserStr) {
      const currentUser = JSON.parse(currentUserStr)
      if (!this.$route.query.username) {
        this.$router.replace({
          name: 'UserProfile',
          query: { username: currentUser.username }
        })
        return
      }
    }
    this.loadProfileData()
    this.loadReactionIcons()
  },
  mounted() {
    window.addEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  beforeUnmount() {
    window.removeEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  methods: {
    formatAvatarUrl(avatar) {
      return formatAvatarUrl(avatar)
    },
    handleAvatarUpdated(event) {
      const { username, avatar } = event.detail
      if (this.userStats && this.userStats.username === username) {
        this.userStats.avatar = avatar
      }
    },
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
    getItemRoute(item) {
      if (this.activeTab === 'posts') {
        return { name: 'ThreadDetail', params: { id: item.id } }
      }
      return {
        name: 'ThreadDetail',
        params: { id: item.threadId },
        query: { postId: item.id },
        hash: `#post-${item.id}`
      }
    },
    handleItemClick(event, item) {
      if (event.target.closest('a, button, .item-avatar, .category-link, [role="button"], .user-profile-popup-wrapper')) {
        return
      }
      this.navigateToItem(item)
    },
    getQuoteAuthorHtml(quoteAuthorStr, item) {
      if (!quoteAuthorStr) return ''
      const match = quoteAuthorStr.match(/(.*?)\s*đã\s*(?:viết|nói):/i)
      if (!match || !match[1]) return quoteAuthorStr

      const authorName = match[1].trim()
      let targetUser = null
      if (this.userStats && ((this.userStats.displayName && this.userStats.displayName.trim() === authorName) || (this.userStats.username && this.userStats.username.trim() === authorName))) {
        targetUser = this.userStats
      } else if (item && item.author && ((item.author.displayName && item.author.displayName.trim() === authorName) || (item.author.username && item.author.username.trim() === authorName))) {
        targetUser = item.author
      }

      const badgeHtml = getVerifiedBadgeSvgHtml(targetUser || { isVerifiedBadge: true })
      return `${authorName}${badgeHtml} đã viết:`
    },
    parseCommentPreview(content) {
      if (!content) return { hasQuote: false, quotes: [], replyText: '' }

      const tempDiv = document.createElement('div')
      tempDiv.innerHTML = content

      // Lấy TẤT CẢ blockquote để build mảng quotes
      const allBqs = Array.from(tempDiv.querySelectorAll('blockquote'))
      const quotes = allBqs.map(bq => {
        let quoteAuthor = ''
        const firstP = bq.querySelector('p:first-child')
        if (firstP) {
          // Lấy text từ toàn bộ p đầu tiên để bao gồm cả "đã viết:" dù nằm trong hay ngoài strong
          // Lọc bỏ text từ span.verified-badge-wrapper (SVG badge không có text content nhưng cẩn thận)
          let rawText = ''
          firstP.childNodes.forEach(node => {
            if (node.nodeType === Node.TEXT_NODE) {
              rawText += node.textContent
            } else if (node.nodeName === 'STRONG') {
              // Lấy text từ strong (bỏ qua span badge bên trong)
              node.childNodes.forEach(child => {
                if (child.nodeType === Node.TEXT_NODE) rawText += child.textContent
                else if (!(child.nodeName === 'SPAN' && child.classList && child.classList.contains('verified-badge-wrapper'))) {
                  rawText += child.textContent
                }
              })
            } else if (!(node.nodeName === 'SPAN' && node.classList && node.classList.contains('verified-badge-wrapper'))) {
              rawText += node.textContent
            }
          })
          quoteAuthor = rawText.trim()
        }

        // Lấy nội dung quote (không bao gồm header strong)
        const bqClone = bq.cloneNode(true)
        const strongInClone = bqClone.querySelector('p strong, strong')
        if (strongInClone) {
          if (strongInClone.parentNode && strongInClone.parentNode.tagName === 'P') {
            strongInClone.parentNode.remove()
          } else {
            strongInClone.remove()
          }
        }
        let quoteText = bqClone.textContent.replace(/\s+/g, ' ').trim()
        if (quoteText.length > 100) quoteText = quoteText.substring(0, 100) + '...'

        return { quoteAuthor, quoteText }
      })

      // Xóa TẤT CẢ blockquote trước khi trích xuất replyText
      allBqs.forEach(bq => bq.remove())

      const attach = tempDiv.querySelector('.attachment-block')
      if (attach) attach.remove()

      let replyText = tempDiv.textContent.replace(/\s+/g, ' ').trim()
      if (replyText.length > 200) {
        replyText = replyText.substring(0, 200) + '...'
      }
      return {
        hasQuote: quotes.length > 0,
        quotes,
        replyText
      }
    },
    triggerReport() {
      alert('Chức năng báo cáo sẽ được cập nhật sau.')
    },
    openUploadModal(mode) {
      this.uploadMode = mode
      this.showUploadModal = true
    },
    async loadProfileData() {
      this.loading = true
      try {
        await Promise.all([
          this.fetchUserStats(),
          this.fetchTabData(),
          this.loadReactionIcons()
        ])
      } catch (e) {
        console.error('Lỗi load dữ liệu profile:', e)
      } finally {
        this.loading = false
      }
    },
    async fetchUserStats() {
      const currentUserStr = localStorage.getItem('user')
      if (!currentUserStr) return
      const currentUser = JSON.parse(currentUserStr)
      const queryUsername = this.$route.query.username || currentUser.username
      
      try {
        const res = await api.get('/users/by-name', { params: { name: queryUsername } })
        if (res.data) {
          this.userStats = res.data
          // Đồng bộ lại local storage chỉ khi là chính mình
          if (queryUsername === currentUser.username) {
            const updatedLocalUser = {
              ...currentUser,
              avatar: this.userStats.avatar,
              displayName: this.userStats.displayName,
              profileBanner: this.userStats.profileBanner,
              postCount: this.userStats.postCount,
              interactionPoints: this.userStats.interactionPoints,
              trophyPoints: this.userStats.trophyPoints
            }
            localStorage.setItem('user', JSON.stringify(updatedLocalUser))
          }
        }
      } catch (e) {
        console.error('Lỗi lấy thông tin người dùng:', e)
        if (queryUsername === currentUser.username) {
          this.userStats = currentUser
        }
      } finally {
        await this.checkFollowStatus()
      }
    },
    async checkFollowStatus() {
      const currentUserStr = localStorage.getItem('user')
      if (!currentUserStr) return
      const currentUser = JSON.parse(currentUserStr)
      const queryUsername = this.$route.query.username || currentUser.username
      if (queryUsername === currentUser.username) return

      try {
        const res = await userFollowService.getFollowStatus(queryUsername)
        if (res.data !== undefined) {
          this.isFollowing = res.data
        }
      } catch (e) {
        console.error('Error checking follow status:', e)
      }
    },
    async handleFollow() {
      const currentUserStr = localStorage.getItem('user')
      if (!currentUserStr) {
        toastError('Vui lòng đăng nhập để thực hiện chức năng này.')
        return
      }
      const currentUser = JSON.parse(currentUserStr)
      const queryUsername = this.$route.query.username
      if (!queryUsername || queryUsername === currentUser.username) return

      const targetState = !this.isFollowing
      const actionTitle = 'Xác nhận'
      const actionText = targetState
        ? 'Bạn chắc chắn muốn theo dõi người dùng này?'
        : 'Bạn chắc chắn muốn bỏ theo dõi người dùng này?'

      const confirmRes = await alertConfirm(actionTitle, actionText)
      if (!confirmRes.isConfirmed) return

      this.loadingFollow = true
      try {
        await userFollowService.toggleFollow(queryUsername, targetState)
        this.isFollowing = targetState
        toastSuccess(targetState ? 'Đã theo dõi người dùng thành công' : 'Đã bỏ theo dõi người dùng')
      } catch (e) {
        console.error('Lỗi khi thao tác theo dõi:', e)
        toastError(e.response?.data?.message || 'Có lỗi xảy ra, vui lòng thử lại sau.')
      } finally {
        this.loadingFollow = false
      }
    },
    async fetchTabData() {
      if (this.activeTab === 'about') return

      if (this.activeTab === 'profile_posts') {
        await this.fetchProfilePosts()
        return
      }
      
      this.listLoading = true
      try {
        const currentUserStr = localStorage.getItem('user')
        if (!currentUserStr) return
        const currentUser = JSON.parse(currentUserStr)
        const queryUsername = this.$route.query.username || currentUser.username
        
        let endpoint
        if (queryUsername === currentUser.username) {
          endpoint = this.activeTab === 'posts' ? '/threads/me' : '/posts/me'
        } else {
          endpoint = this.activeTab === 'posts' ? `/threads/user/${queryUsername}` : `/posts/user/${queryUsername}`
        }
        
        const res = await api.get(endpoint, {
          params: {
            page: this.currentPage - 1,
            size: this.itemsPerPage
          }
        })
        
        if (res.data) {
          this.items = res.data.content || []
          this.totalPages = res.data.totalPages || 1
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
      if (tab === 'profile_posts') {
        this.profilePostsCurrentPage = 1
        this.fetchProfilePosts()
      } else if (tab === 'posts' || tab === 'comments') {
        this.currentPage = 1
        this.fetchTabData()
      }
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchTabData()
    },
    async loadReactionIcons() {
      try {
        const res = await reactionService.getIcons()
        this.reactionIconsList = res.data || []
      } catch (e) {
        console.error('Lỗi tải reaction icons:', e)
      }
    },
    async fetchProfilePosts() {
      this.profilePostsLoading = true
      try {
        const username = this.targetUsername
        if (!username) return
        const res = await profilePostService.getProfilePosts(username, this.profilePostsCurrentPage - 1, 10)
        if (res.data) {
          this.profilePosts = res.data.content || []
          this.profilePostsTotalPages = res.data.totalPages || 1
        } else {
          this.profilePosts = []
          this.profilePostsTotalPages = 1
        }
      } catch (e) {
        console.error('Lỗi tải bài viết lưu bút:', e)
        this.profilePosts = []
        this.profilePostsTotalPages = 1
      } finally {
        this.profilePostsLoading = false
      }
    },
    handleProfilePostsPageChange(page) {
      this.profilePostsCurrentPage = page
      this.fetchProfilePosts()
    },
    openStatusEditor() {
      if (!this.isLoggedIn) {
        toastError('Vui lòng đăng nhập để đăng bài viết lên hồ sơ')
        return
      }
      this.showStatusEditor = true
      this.$nextTick(() => {
        setTimeout(() => {
          this.$refs.statusEditorRef?.focus()
        }, 60)
      })
    },
    cancelStatusEditor() {
      this.showStatusEditor = false
      this.newPostContent = ''
      this.newPostAttachedImages = []
    },
    async submitProfilePost() {
      if (!this.newPostContent.trim()) {
        toastError('Vui lòng nhập nội dung bài viết')
        return
      }
      if (!this.isLoggedIn) {
        toastError('Vui lòng đăng nhập để thực hiện')
        return
      }

      this.isSubmittingPost = true
      try {
        const finalContent = this.buildAttachmentHtml(this.newPostAttachedImages, this.newPostContent.trim())

        const payload = {
          profileUsername: this.targetUsername,
          content: finalContent
        }
        await profilePostService.createProfilePost(payload)
        toastSuccess('Đăng bài viết lên hồ sơ thành công')
        
        // Đưa form về giao diện mặc định và reset nội dung
        this.showStatusEditor = false
        this.newPostContent = ''
        this.newPostAttachedImages = []

        // Đồng bộ lại phân trang máy chủ: reset về trang 1 và tải lại danh sách 10 bài mới nhất
        this.profilePostsCurrentPage = 1
        await this.fetchProfilePosts()
      } catch (e) {
        console.error('Lỗi khi đăng bài viết hồ sơ:', e)
        toastError(e.response?.data?.message || 'Có lỗi xảy ra khi đăng bài')
      } finally {
        this.isSubmittingPost = false
      }
    },
    handleNewPostInsertImages(urls, type) {
      if (this.$refs.statusEditorRef && this.$refs.statusEditorRef.insertImages) {
        this.$refs.statusEditorRef.insertImages(urls, type)
      }
    },
    handleNewPostImageUploaded(image) {
      this.newPostAttachedImages.push(image)
    },
    async handleProfilePostDeleted() {
      // Tải lại dữ liệu từ server để nạp bù bài viết và cập nhật tổng số trang chuẩn xác
      await this.fetchProfilePosts()
      if (this.profilePostsCurrentPage > this.profilePostsTotalPages) {
        this.profilePostsCurrentPage = Math.max(1, this.profilePostsTotalPages)
        await this.fetchProfilePosts()
      }
    },
    handleProfilePostUpdated(updatedPost) {
      const index = this.profilePosts.findIndex(p => p.id === updatedPost.id)
      if (index !== -1) {
        this.profilePosts.splice(index, 1, updatedPost)
      }
    },
    async navigateToItem(item) {
      if (this.activeTab === 'posts') {
        this.$router.push({ name: 'ThreadDetail', params: { id: item.id } })
      } else {
        // Đối với bình luận, định tuyến trực tiếp đến trang chứa post và cuộn xuống
        try {
          this.loading = true
          const res = await api.get(`/posts/${item.id}/page-number`, { params: { size: 10 } })
          const pageNum = res.data || 1
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
    },
    handleBlock() {
      alert('Tính năng Chặn sẽ được cập nhật sau.')
    },
    startConversation() {
      const nameParam = this.userStats.displayName || this.userStats.username
      this.$router.push({
        name: 'AddConversation',
        query: { to: nameParam }
      })
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
    gap: 0;
  }
}

.account-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
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
  background: linear-gradient(to bottom, rgba(0,0,0,0.02) 0%, rgba(0,0,0,0.12) 100%);
  z-index: 1;
}

.banner-actions {
  position: relative;
  top: auto;
  left: auto;
  margin-top: 16px;
  display: flex;
  gap: 8px;
  z-index: 10;
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
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  padding: 0 24px;
  gap: 20px;
  z-index: 10;
}

@media (max-width: 576px) {
  .profile-info-upper {
    gap: 1rem;
  }
}

.profile-avatar-wrapper {
  position: relative;
  left: auto;
  bottom: auto;
  margin-top: 20px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  border: 4px solid #ffffff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  overflow: hidden;
  cursor: default;
  z-index: 11;
  background-color: #fff;
  flex-shrink: 0;
}

.profile-avatar-wrapper.clickable {
  cursor: pointer;
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
  font-size: 5.5rem;
}

.profile-meta-details {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  color: #1a507a;
  margin-left: 0;
  margin-top: 24px;
  flex: 1;
}

.profile-info-upper.text-white .profile-meta-details {
  color: #1a507a !important;
  background: none !important;
  padding: 0 !important;
  border: none !important;
  box-shadow: none !important;
}
.profile-info-upper.text-white .profile-displayname {
  color: #1a507a !important;
}
.profile-info-upper.text-white .profile-title-tag {
  color: #666 !important;
}
.profile-info-upper.text-white .text-dimmed {
  color: #7f8c8d !important;
  white-space: nowrap !important;
}

.profile-displayname {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
  line-height: 1.2;
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
  background: #ffffff;
  border-top: 1px solid #d8dbe0;
  padding: 265px 24px 15px 268px;
  justify-content: space-between;
  align-items: center;
}

.stat-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
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

@media (max-width: 767px) {
  .profile-banner-area {
    height: 180px;
    padding: 0;
    flex-direction: column;
    justify-content: flex-end;
    position: relative;
  }
  
  .profile-info-upper {
    position: absolute;
    top: 100%;
    left: 16px;
    width: calc(100% - 32px);
    flex-direction: row;
    align-items: flex-start;
    text-align: left;
    gap: 12px;
    z-index: 10;
  }

  .profile-avatar-wrapper {
    position: relative;
    left: auto;
    bottom: auto;
    width: 90px;
    height: 90px;
    margin: 16px 0 0 0;
    flex-shrink: 0;
    border: 3px solid #ffffff;
    box-shadow: 0 2px 5px rgba(0,0,0,0.1);
  }

  .profile-avatar-placeholder {
    font-size: 2.2rem;
  }

  .profile-meta-details {
    margin-left: 0;
    align-items: flex-start;
    text-align: left;
    width: 100%;
    max-width: none;
    background: none !important;
    padding: 10px 0 0 0 !important;
    border: none !important;
    box-shadow: none !important;
    color: #1a507a !important;
  }

  .profile-displayname {
    font-size: 1.25rem;
    color: #1a507a !important;
  }
  
  .profile-title-tag {
    color: #666 !important;
    margin-top: 1px;
  }

  .profile-time-row,
  .profile-time-row .text-dimmed {
    color: #7f8c8d !important;
  }
  .profile-time-row {
    margin-top: 3px;
  }

  .banner-actions {
    position: relative;
    top: auto;
    left: auto;
    width: auto;
    margin-top: 20px;
    display: flex;
    justify-content: flex-start;
    gap: 8px;
    z-index: 10;
  }

  .profile-stats-bar {
    margin-top: 166px;
    padding: 12px 1rem;
    justify-content: space-between;
  }
}

@media (max-width: 576px) {
  .profile-banner-area {
    height: 140px;
  }
  
  .profile-avatar-wrapper {
    width: 76px;
    height: 76px;
    margin-top: 12px;
  }
  
  .profile-avatar-placeholder {
    font-size: 1.8rem;
  }
  
  .profile-meta-details {
    padding-top: 6px !important;
  }

  .profile-displayname {
    font-size: 1.15rem;
  }

  .banner-actions {
    margin-top: 4px;
  }

  .profile-stats-bar {
    margin-top: 166px;
    padding: 10px 0.75rem;
  }
}


/* Tabs styles */
.profile-tabs-bar {
  display: flex;
  background: #f8f9fa;
  border-top: 1px solid #d8dbe0;
  border-bottom: 1px solid #d8dbe0;
  overflow-x: auto;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}
.profile-tabs-bar::-webkit-scrollbar {
  display: none;
}

@media (max-width: 767px) {
  .profile-tabs-bar {
    scrollbar-width: thin;
    scrollbar-color: #1a507a #edf0f2;
  }
  .profile-tabs-bar::-webkit-scrollbar {
    display: block !important;
    height: 3px;
  }
  .profile-tabs-bar::-webkit-scrollbar-track {
    background: #edf0f2;
  }
  .profile-tabs-bar::-webkit-scrollbar-thumb {
    background: #1a507a;
    border-radius: 3px;
  }

  /* Form nhập liệu trên mobile: Ẩn cột avatar, thẻ input chiếm full width chuẩn voz */
  .status-avatar-col {
    display: none !important;
  }
  .status-form-col::before,
  .status-form-col::after {
    display: none !important;
  }
  .status-form-col {
    padding: 10px 12px;
    width: 100%;
  }
  .status-fake-input {
    padding: 10px 14px;
    font-size: 0.88rem;
  }
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
  flex-shrink: 0;
  white-space: nowrap;
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
  padding: 12px 10px;
  border-bottom: 1px solid #edf2f7;
  border-radius: 4px;
  transition: background-color 0.2s;
  cursor: pointer;
}

.activity-row-item:hover {
  background-color: #f8f9fa;
}

.mini-quote-box {
  background-color: #f8f9fa;
  border-left: 3px solid #e67e22;
  padding: 4px 10px;
  margin-bottom: 6px;
  border-radius: 3px;
  font-size: 0.82rem;
  color: #555;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.mini-quote-author {
  font-weight: 600;
  color: #e67e22;
  margin-right: 6px;
}

.mini-quote-text {
  color: #666;
  font-style: italic;
}

.activity-row-item:last-child {
  border-bottom: none;
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
  line-height: 1.4;
  margin-bottom: 4px;
}

.label-tag-mini {
  display: inline-block;
  padding: 1px 6px;
  font-size: 0.72rem;
  font-weight: 600;
  border-radius: 3px;
  border: 1px solid transparent;
  margin-right: 6px;
  vertical-align: middle;
}

.item-title-link {
  font-size: 0.98rem;
  font-weight: 600;
  color: #1a507a;
  cursor: pointer;
  display: inline;
  vertical-align: middle;
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

.item-category-row {
  font-size: 0.8rem;
  color: #7f8c8d;
  margin-top: 2px;
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

@import "@/shared/assets/styles/custom.css";

.profile-header-card {
  position: relative;
  margin-bottom: 8px !important;
}

.profile-info-upper.positioned-absolute {
  position: absolute;
  top: 220px;
  left: 0;
  width: 100%;
  z-index: 10;
}

/* No Banner Layout Styles */
.profile-header-card.no-banner .profile-info-upper {
  position: static;
  padding: 24px 24px 0 24px;
  background: #ffffff;
  color: #1a507a;
}

.profile-header-card.no-banner .profile-avatar-wrapper {
  margin-top: 0;
  width: 150px;
  height: 150px;
}

.profile-header-card.no-banner .profile-avatar-placeholder {
  font-size: 3.5rem;
}

.profile-header-card.no-banner .profile-meta-details {
  margin-top: 0;
}

.profile-header-card.no-banner .profile-stats-bar {
  padding: 20px 24px;
  border-top: 1px solid #d8dbe0;
}

@media (max-width: 767px) {
  .profile-info-upper.positioned-absolute {
    top: 180px;
    left: 16px;
    width: calc(100% - 32px);
  }
  
  .profile-header-card.no-banner .profile-info-upper {
    padding: 16px 16px 0 16px;
  }
  
  .profile-header-card.no-banner .profile-avatar-wrapper {
    width: 100px;
    height: 100px;
  }
  
  .profile-header-card.no-banner .profile-avatar-placeholder {
    font-size: 2.5rem;
  }

  .profile-header-card.no-banner .profile-stats-bar {
    margin-top: 0;
    padding: 12px 1rem;
  }
}

@media (max-width: 576px) {
  .profile-info-upper.positioned-absolute {
    top: 140px;
  }
  
  .profile-header-card.no-banner .profile-stats-bar {
    margin-top: 0;
    padding: 10px 0.75rem;
  }
}

/* Mobile Actions Block */
.profile-mobile-actions {
  display: none;
  background: #ffffff;
  padding: 10px 15px;
  border-top: 1px solid #d8dbe0;
  border-bottom: 1px solid #d8dbe0;
  gap: 8px;
  justify-content: space-between;
}

.btn-mobile-action {
  flex: 1;
  background-color: #ffffff;
  border: 1px solid #c8d4e0;
  color: #1a507a;
  font-weight: 500;
  padding: 8px 4px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
  transition: all 0.2s;
  font-family: inherit;
  outline: none;
  text-align: center;
  white-space: nowrap;
}

.btn-mobile-action:hover {
  background-color: #1a507a;
  color: #ffffff;
  border-color: #1a507a;
}

@media (max-width: 767px) {
  /* Hiển thị khối nút mới trên mobile */
  .profile-mobile-actions {
    display: flex;
  }
  
  /* Ẩn hoàn toàn khối nút cũ trên header mobile cho cả mình và người khác */
  .banner-actions {
    display: none !important;
  }

  /* Cho phép text thời gian online gần nhất xuống dòng tự nhiên */
  .profile-info-upper.text-white .text-dimmed {
    white-space: normal !important;
    word-break: break-word;
  }
}

/* Profile Tab Content Wrapper & Status Input Card */
.profile-tab-content-wrapper {
  margin-top: 0 !important;
}

.profile-other-tab-card {
  padding: 1.5rem;
}

.profile-status-input-card {
  background: #ffffff;
  border: 1px solid #d8dbe0;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  margin-bottom: 12px;
}

.status-input-row {
  display: flex;
  align-items: stretch;
  width: 100%;
}

.status-avatar-col {
  width: 64px;
  flex-shrink: 0;
  background-color: #f5f6f8;
  border-right: 1px solid #d8dbe0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 0;
  transition: all 0.2s ease;
}

.status-input-row.is-expanded .status-avatar-col {
  align-items: flex-start;
  padding-top: 14px;
}

.status-avatar {
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
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}
.status-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-form-col {
  flex: 1;
  min-width: 0;
  background-color: #ffffff;
  padding: 10px 14px;
  position: relative;
  display: flex;
  align-items: center;
}

.status-input-row.is-expanded .status-form-col {
  display: flex;
  flex-direction: column;
  align-items: stretch;
}

/* Mũi tên tin nhắn tại vách ngăn giữa 2 cột trỏ về phía Avatar */
.status-form-col::before,
.status-form-col::after {
  content: '';
  position: absolute;
  border-style: solid;
  display: block;
  width: 0;
  height: 0;
  pointer-events: none;
  z-index: 2;
}

.status-input-row:not(.is-expanded) .status-form-col::before,
.status-input-row:not(.is-expanded) .status-form-col::after {
  top: 50%;
  transform: translateY(-50%);
}

.status-input-row.is-expanded .status-form-col::before {
  top: 24px;
}
.status-input-row.is-expanded .status-form-col::after {
  top: 25px;
}

.status-form-col::before {
  right: 100%;
  border-width: 8px 8px 8px 0;
  border-color: transparent #d8dbe0 transparent transparent;
}

.status-form-col::after {
  right: calc(100% - 1px);
  border-width: 7px 7px 7px 0;
  border-color: transparent #ffffff transparent transparent;
}

/* Thẻ input nằm trọn bên trong box cột phải */
.status-fake-input {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: #f0f6fa;
  border: 1px solid #cde0ed;
  border-radius: 4px;
  padding: 14px;
  color: #6a8296;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.status-fake-input:hover {
  border-color: #1a507a;
  background-color: #e5f1f9;
  color: #1a507a;
}
.status-fake-input:hover .pen-icon {
  color: #1a507a;
}

.pen-icon {
  color: #7d96a8;
  transition: color 0.2s;
}

.status-editor-expanded {
  width: 100%;
}

.status-editor-actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.btn-submit-status {
  background: #1a507a;
  color: #ffffff;
  border: none;
  padding: 8px 20px;
  font-size: 0.88rem;
  font-weight: 600;
  border-radius: 4px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: background-color 0.2s;
}
.btn-submit-status:hover:not(:disabled) {
  background: #133a59;
}
.btn-submit-status:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-cancel-status {
  background: #e2e8f0;
  color: #4a5568;
  border: none;
  padding: 8px 16px;
  font-size: 0.88rem;
  font-weight: 500;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.btn-cancel-status:hover {
  background: #cbd5e0;
}

.profile-posts-list {
  display: flex;
  flex-direction: column;
}

@import "@/shared/assets/styles/custom.css";
</style>
