<template>
  <div class="thread-detail-page app-wrapper" v-if="!loading && thread">
    <ForumHeader />

    <main class="container" style="padding-top: 2rem;">
      <Breadcrumb :items="breadcrumbItems" />

      <div class="thread-header card">
        <div class="thread-title-full">
          <h1>
            <span v-if="thread.label" class="label-tag" :style="{ backgroundColor: thread.label.colorCode, color: thread.label.textColor, borderColor: thread.label.borderColor || 'transparent' }">{{ thread.label.name }}</span>
            {{ thread.title }}
          </h1>
        </div>
        <div class="thread-meta-bar">
          <div class="author-info">
            <svg class="meta-icon" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
            <span class="author-name">{{ thread.author ? (thread.author.displayName || thread.author.username) : 'Ẩn danh' }}</span>
            <span class="meta-dot">·</span>
            <svg class="meta-icon" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
            <span class="post-time">{{ formatDate(thread.createdAt) }}</span>
          </div>
        </div>
      </div>

      <!-- Poll Box -->
      <div v-if="thread.poll" class="poll-wrapper">
        <PollDisplay :pollData="thread.poll" @vote-updated="handleVoteUpdated" />
      </div>

      <!-- Thread Actions (Pagination & Follow Button) -->
      <div class="thread-action-bar" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; flex-wrap: wrap; gap: 10px;">
        <div class="pagination-wrapper-left">
          <ForumPagination 
            v-if="totalPages > 1"
            :current-page="currentPage" 
            :total-pages="totalPages" 
            @page-changed="changePage"
          />
        </div>
        <div class="follow-btn-wrapper-right" v-if="isLoggedIn">
          <button class="btn-follow-thread" @click="handleFollowToggle">
            {{ isFollowing ? 'Bỏ theo dõi' : 'Theo dõi' }}
          </button>
        </div>
      </div>

      <!-- Unified Content Loop (Main Post & Replies) -->
      <template v-for="item in paginatedItems" :key="item.id">
        
        <!-- MAIN POST (#1) -->
        <div v-if="item.isMain" class="thread-content-card card" :id="'post-' + item.id" :class="{ 'highlight-jump': String(item.id) === String(highlightedPostId) }">
          <div class="post-layout">
            <div class="post-sidebar">
              <div class="avatar-large" :style="{ backgroundColor: thread.author && thread.author.avatar ? thread.author.avatar : '#ccc', color: '#fff' }">
                {{ thread.author ? (thread.author.displayName || thread.author.username).charAt(0).toUpperCase() : 'A' }}
              </div>
              <div class="author-info-mobile-block">
                <div class="author-name-large">{{ thread.author ? (thread.author.displayName || thread.author.username) : 'Ẩn danh' }}</div>
                <div class="author-title">Yếu sinh lý</div>
              </div>
              <span class="message-userArrow"></span>
            </div>
            
            <div class="post-main">
              <div class="post-meta-top">
                <span class="post-time-top">{{ formatDate(thread.createdAt) }}</span>
                <div class="post-actions-top">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"></circle><circle cx="6" cy="12" r="3"></circle><circle cx="18" cy="19" r="3"></circle><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line></svg>
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path></svg>
                  <span class="post-number">#1</span>
                </div>
              </div>
              
              <div v-if="editingItemId === item.id" class="inline-edit-box">
                <CustomEditor ref="inlineEditEditor" v-model="editForm.content" minHeight="150px" @image-uploaded="handleEditImageUploaded" />
                
                <!-- Khối xem trước đính kèm khi sửa nhanh bài viết gốc -->
                <div v-if="editAttachedImages && editAttachedImages.length > 0" class="attachment-block" style="margin: 1rem 1.5rem; border-top: 1px dashed #ddd; padding-top: 1.5rem;">
                  <div class="attachment-label" style="font-weight: bold; color: #1a507a; margin-bottom: 1rem; font-size: 0.95rem;">Đính kèm</div>
                  <div class="attachment-list" style="display: flex; flex-wrap: wrap; gap: 15px;">
                    <img v-for="(img, idx) in editAttachedImages" :key="idx" :src="img.url" :alt="img.name" style="width: 200px; height: 200px; object-fit: cover; border: 1px solid #ddd; border-radius: 4px; cursor: zoom-in;" />
                  </div>
                </div>

                <ImageUploaderPanel ref="inlineEditUploader" v-model:images="editAttachedImages" @insert-images="handleEditInsertImages" style="padding: 10px; background: #fdfdfd; border-top: 1px solid #eee;" />
                <div class="edit-actions-footer">
                  <button class="btn-save" :disabled="submittingEdit" @click="submitEdit(item)">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="btn-icon"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path><polyline points="17 21 17 13 7 13 7 21"></polyline><polyline points="7 3 7 8 15 8"></polyline></svg>
                    Lưu
                  </button>
                  <button class="btn-cancel-edit" @click="cancelEditing">Hủy</button>
                </div>
              </div>
              <div v-else class="content-body ql-editor" v-html="thread.content" @click="handleContentClick"></div>
              

              <div class="post-meta-bottom" v-if="editingItemId !== item.id">
                <div class="left-actions">
                  <a href="#" class="action-link" @click.prevent v-if="isLoggedIn && !isNonOfficial">Báo cáo</a>
                  <a href="#" class="action-link" v-if="canEdit(item)" @click.prevent="startEditing(item)">Sửa</a>
                </div>
                <div class="right-actions">
                  <!-- Reactions for Main Post positioned on the right -->
                  <ReactionButton 
                    v-if="canShowReactionForMainPost"
                    :targetId="thread.id"
                    type="thread"
                    :allIcons="reactionIconsList"
                    :userReaction="thread.currentUserReaction"
                    @reaction-changed="fetchThread"
                  />
                  <a href="#" class="action-link reply-link" @click.prevent="quotePost(thread.author ? (thread.author.displayName || thread.author.username) : 'Ẩn danh', thread.content, 'main_thread_entry')" v-if="isLoggedIn && !isNonOfficial">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 17 4 12 9 7"></polyline><path d="M20 18v-2a4 4 0 0 0-4-4H4"></path></svg>
                    Trả lời
                  </a>
                </div>
              </div>

              <!-- Reaction Summary for Main Post (XenForo Style) -->
              <div class="reactions-bar-container" v-if="thread.reactionSummary && thread.reactionSummary.length > 0">
                <ReactionSummary 
                  :summary="thread.reactionSummary" 
                  :recentReactors="thread.recentReactors" 
                  @open-popup="openReactionPopup('#1', thread.id, true, thread.reactionSummary)"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- REPLY POSTS (#2+) -->
        <div v-else class="thread-content-card card reply-card" :id="'post-' + item.id" :class="{ 'highlight-jump': String(item.id) === String(highlightedPostId) }">
          <div class="post-layout">
            <div class="post-sidebar">
              <div class="avatar-large" :style="{ backgroundColor: item.author && item.author.avatar ? item.author.avatar : '#ccc', color: '#fff' }">
                {{ item.author ? (item.author.displayName || item.author.username).charAt(0).toUpperCase() : '?' }}
              </div>
              <div class="author-info-mobile-block">
                <div class="author-name-large">{{ item.author ? (item.author.displayName || item.author.username) : 'Ẩn danh' }}</div>
                <div class="author-title">Thành viên</div>
              </div>
              <span class="message-userArrow"></span>
            </div>
            
            <div class="post-main">
              <div class="post-meta-top">
                <span class="post-time-top">{{ formatDate(item.createdAt) }}</span>
                <div class="post-actions-top">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"></circle><circle cx="6" cy="12" r="3"></circle><circle cx="18" cy="19" r="3"></circle><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line></svg>
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path></svg>
                  <span class="post-number">#{{ item.seqNumber }}</span>
                </div>
              </div>
              
              <div v-if="editingItemId === item.id" class="inline-edit-box">
                <CustomEditor ref="inlineEditEditor" v-model="editForm.content" minHeight="150px" @image-uploaded="handleEditImageUploaded" />
                
                <!-- Khối xem trước đính kèm khi sửa nhanh bài viết -->
                <div v-if="editAttachedImages && editAttachedImages.length > 0" class="attachment-block" style="margin: 1rem 1.5rem; border-top: 1px dashed #ddd; padding-top: 1.5rem;">
                  <div class="attachment-label" style="font-weight: bold; color: #1a507a; margin-bottom: 1rem; font-size: 0.95rem;">Đính kèm</div>
                  <div class="attachment-list" style="display: flex; flex-wrap: wrap; gap: 15px;">
                    <img v-for="(img, idx) in editAttachedImages" :key="idx" :src="img.url" :alt="img.name" style="width: 200px; height: 200px; object-fit: cover; border: 1px solid #ddd; border-radius: 4px; cursor: zoom-in;" />
                  </div>
                </div>

                <ImageUploaderPanel ref="inlineEditUploader" v-model:images="editAttachedImages" @insert-images="handleEditInsertImages" style="padding: 10px; background: #fdfdfd; border-top: 1px solid #eee;" />
                <div class="edit-actions-footer">
                  <button class="btn-save" :disabled="submittingEdit" @click="submitEdit(item)">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="btn-icon"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path><polyline points="17 21 17 13 7 13 7 21"></polyline><polyline points="7 3 7 8 15 8"></polyline></svg>
                    Lưu
                  </button>
                  <button class="btn-cancel-edit" @click="cancelEditing">Hủy</button>
                </div>
              </div>
              <div v-else class="content-body ql-editor" v-html="formatPostContent(item.content)" @click="handleContentClick"></div>
              

              <div class="post-meta-bottom" v-if="editingItemId !== item.id">
                <div class="left-actions">
                  <a href="#" class="action-link" @click.prevent v-if="isLoggedIn && !isNonOfficial">Báo cáo</a>
                  <a href="#" class="action-link" v-if="canEdit(item)" @click.prevent="startEditing(item)">Sửa</a>
                </div>
                <div class="right-actions">
                  <!-- Reactions for Reply Item positioned on the right -->
                  <ReactionButton 
                    v-if="canShowReactionForReply(item)"
                    :targetId="item.id"
                    type="post"
                    :allIcons="reactionIconsList"
                    :userReaction="item.currentUserReaction"
                    @reaction-changed="reloadPostsOnly"
                  />
                  <a href="#" class="action-link reply-link" @click.prevent="quotePost(item.author ? (item.author.displayName || item.author.username) : 'Ẩn danh', item.content, item.id)" v-if="isLoggedIn && !isNonOfficial">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 17 4 12 9 7"></polyline><path d="M20 18v-2a4 4 0 0 0-4-4H4"></path></svg>
                    Trả lời
                  </a>
                </div>
              </div>

              <!-- Reaction Summary for Reply Item (XenForo Style) -->
              <div class="reactions-bar-container" v-if="item.reactionSummary && item.reactionSummary.length > 0">
                <ReactionSummary 
                  :summary="item.reactionSummary" 
                  :recentReactors="item.recentReactors" 
                  @open-popup="openReactionPopup('#' + item.seqNumber, item.id, false, item.reactionSummary)"
                />
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- Bottom Pagination Bar -->
      <div class="pagination-wrapper" v-if="totalPages > 1" style="margin-top: 1rem; margin-bottom: 1rem;">
        <ForumPagination 
          :current-page="currentPage" 
          :total-pages="totalPages" 
          @page-changed="changePage"
        />
      </div>

      <!-- Reply Editor Container -->
      <div ref="replyFormContainer" class="reply-box-wrapper card" style="margin-top: 2rem;" v-if="isLoggedIn && !isNonOfficial">
        <div class="post-layout">
          <div class="post-sidebar" style="background: #f8f9fa; border-right: none;">
             <div class="avatar-large" :style="{ backgroundColor: currentUserAvatar || '#ccc', color: '#fff' }">
                {{ currentUsername ? currentUsername.charAt(0).toUpperCase() : '?' }}
             </div>
          </div>
          <div class="post-main" style="padding: 0; border: 1px solid #e0e0e0;">
             <CustomEditor ref="replyEditor" v-model="replyForm.content" minHeight="150px" @image-uploaded="handleImageUploaded" />
             
             <!-- Khối xem trước đính kèm chân bình luận -->
             <div v-if="replyAttachedImages && replyAttachedImages.length > 0" class="attachment-block" style="margin: 1rem 1.5rem; border-top: 1px dashed #ddd; padding-top: 1.5rem;">
               <div class="attachment-label" style="font-weight: bold; color: #1a507a; margin-bottom: 1rem; font-size: 0.95rem;">Đính kèm</div>
               <div class="attachment-list" style="display: flex; flex-wrap: wrap; gap: 15px;">
                 <img v-for="(img, idx) in replyAttachedImages" :key="idx" :src="img.url" :alt="img.name" style="width: 200px; height: 200px; object-fit: cover; border: 1px solid #ddd; border-radius: 4px; cursor: zoom-in;" />
               </div>
             </div>

             <ImageUploaderPanel ref="uploaderPanel" v-model:images="replyAttachedImages" @insert-images="handleInsertImages" style="padding: 10px; background: #fdfdfd; border-top: 1px solid #eee;" />
             
             <div class="editor-footer" style="padding: 15px; display: flex; justify-content: flex-end; background: #f8f9fa; border-top: 1px solid #eee;">
               <button class="btn-post" :disabled="submittingPost" @click="submitReply">
                 {{ submittingPost ? 'Đang gửi...' : 'Gửi trả lời' }}
               </button>
             </div>
          </div>
        </div>
      </div>

      <div v-else-if="!isLoggedIn" class="card" style="margin-top: 2rem; padding: 2rem; text-align: center; background: #f8f9fa; border: 1px dashed #bbb;">
        Bạn phải <router-link to="/login" style="color: #3498db; font-weight: bold;">đăng nhập</router-link> để có thể trả lời bài viết này.
      </div>

      <Breadcrumb :items="breadcrumbItems" />
      
      <ReactionListPopup 
        :show="showReactionPopup" 
        @update:show="showReactionPopup = $event" 
        :orderNumber="reactionPopupData.orderNumber" 
        :targetId="reactionPopupData.targetId" 
        :isMainPost="reactionPopupData.isMainPost" 
        :summary="reactionPopupData.summary" 
      />
    </main>
  </div>
  <Loading :visible="loading" />

  <!-- Lightbox Modal -->
  <div v-if="showLightbox" class="lightbox-modal" @click="closeLightbox">
    <div class="lightbox-content">
      <button class="btn-close-lightbox" @click="closeLightbox">&times;</button>
      <div class="lightbox-main">
        <div class="lightbox-image-wrapper">
          <img :src="activeImageUrl" class="main-lightbox-img" @click.stop />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import threadService from '@/apps/Forum/services/thread.service'
import postService from '@/apps/Forum/services/post.service'
import reactionService from '@/apps/Forum/services/reaction.service'
import categoryService from '@/apps/Forum/services/category.service'
import ForumHeader from '@/shared/components/ForumHeader.vue'
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import PollDisplay from '@/shared/components/PollDisplay.vue'
import CustomEditor from '@/shared/components/CustomEditor.vue'
import ImageUploaderPanel from '@/shared/components/ImageUploaderPanel.vue'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import { alertSuccess, alertError, alertConfirm, toastSuccess, toastError } from '@/shared/utils/swal'
import { formatForumDate } from '@/shared/utils/date'
import ReactionButton from '@/shared/components/ReactionButton.vue'
import ReactionSummary from '@/shared/components/ReactionSummary.vue'
import ReactionListPopup from '@/shared/components/ReactionListPopup.vue'
import Loading from '@/shared/components/Loading.vue'
import { downloadFileAsBlob, extractAttachmentFilename } from '@/shared/utils/downloadUtils'
import { isNonOfficialUser } from '@/shared/utils/utils'

export default {
  name: 'ThreadDetail',
  components: {
    ForumHeader,
    Breadcrumb,
    PollDisplay,
    CustomEditor,
    ImageUploaderPanel,
    ForumPagination,
    ReactionButton,
    ReactionSummary,
    ReactionListPopup,
    Loading
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
      thread: null,
      posts: [],
      totalPosts: 0,
      categoryGroup: null,
      allCategories: [],
      loading: true,
      showLightbox: false,
      activeImageUrl: '',
      replyForm: {
        content: ''
      },
      replyAttachedImages: [],
      submittingPost: false,
      isLoggedIn: !!localStorage.getItem('token'),
      isFollowing: false,
      currentUsername: parsedUser ? (parsedUser.displayName || parsedUser.username) : 'Me',
      currentUserAvatar: parsedUser ? parsedUser.avatar : '#3498db',
      currentPage: Number(this.$route.query.page) || 1,
      itemsPerPage: 10,
      highlightedPostId: null,
      currentUser: parsedUser,
      editingItemId: null,
      editForm: {
        content: ''
      },
      editAttachedImages: [],
      submittingEdit: false,
      reactionIconsList: [],
      showReactionPopup: false,
      reactionPopupData: {
        orderNumber: '#1',
        targetId: null,
        isMainPost: true,
        summary: []
      }
    }
  },
  computed: {
    breadcrumbItems() {
      const items = [{ title: 'Trang chủ', to: { name: 'Home' } }]
      
      if (this.thread && this.thread.category && this.thread.category.categoryGroupId) {
        if (this.categoryGroup) {
           items.push({ 
             title: this.categoryGroup.name, 
             to: { name: 'Home', hash: `#group-${this.categoryGroup.id}` } 
           })
        }
      }

      if (this.thread && this.thread.category && this.allCategories && this.allCategories.length > 0) {
         let parents = [];
         let currentParentId = this.thread.category.parentCategoryId;
         while (currentParentId) {
             const parent = this.allCategories.find(c => c.id === currentParentId);
             if (parent) {
                 parents.unshift(parent);
                 currentParentId = parent.parentCategoryId;
             } else {
                 break;
             }
         }
         parents.forEach(p => {
             items.push({
                 title: p.name,
                 to: { name: 'CategoryDetail', params: { id: p.id } }
             })
         });
         
         items.push({ 
           title: this.thread.category.name, 
           to: { name: 'CategoryDetail', params: { id: this.thread.category.id } } 
         })
      } else if (this.thread && this.thread.category) {
        items.push({ 
          title: this.thread.category.name, 
          to: { name: 'CategoryDetail', params: { id: this.thread.category.id } } 
        })
      }

      if (this.thread) {
        items.push({ title: this.thread.title })
      }
      return items
    },
    attachedImages() {
      if (this.thread && this.thread.attachedImages) {
        try {
          return JSON.parse(this.thread.attachedImages)
        } catch (e) {
          console.error('Error parsing attached images:', e)
          return []
        }
      }
      return []
    },
    totalPages() {
      if (!this.thread) return 1;
      const totalCount = 1 + this.totalPosts;
      return Math.ceil(totalCount / this.itemsPerPage) || 1;
    },
    paginatedItems() {
      if (!this.thread) return [];
      if (this.currentPage === 1) {
        return [
          { ...this.thread, id: 'main_thread_entry', isMain: true, seqNumber: 1 },
          ...this.posts.map((p, idx) => ({ ...p, isMain: false, seqNumber: idx + 2 }))
        ];
      }
      const startSeq = (this.currentPage - 1) * this.itemsPerPage + 1;
      return this.posts.map((p, idx) => ({ ...p, isMain: false, seqNumber: startSeq + idx }));
    },
    isNonOfficial() {
      return isNonOfficialUser();
    },
    canShowReactionForMainPost() {
      if (!this.isLoggedIn || !this.thread || !this.thread.author || !this.currentUser || this.isNonOfficial) return false;
      return String(this.thread.author.id) !== String(this.currentUser.id);
    }
  },
  async mounted() {
    this.loading = true;
    try {
      await Promise.all([
        this.fetchReactionIcons(),
        this.fetchThread(),
        this.fetchPosts(),
        this.fetchFollowStatus()
      ]);
    } catch (e) {
      console.error('Lỗi khi tải dữ liệu trang:', e);
    } finally {
      this.loading = false;
    }
    
    // Tự động nhảy tới bình luận nếu URL có chỉ định postId
    this.jumpToTargetPost()

    this.initQuoteCollapsing()

    window.addEventListener('notification-clicked', this.onNotificationClicked);
  },
  updated() {
    this.initQuoteCollapsing()
  },
  beforeUnmount() {
    window.removeEventListener('notification-clicked', this.onNotificationClicked);
  },
  watch: {
    // Lắng nghe khi tham số query thay đổi (trong trường hợp click thông báo khi đang ở sẵn trong trang này)
    '$route'(to, from) {
      if (to.hash && to.hash !== from.hash && to.hash.startsWith('#post-')) {
        this.jumpToTargetPost();
      }
    },
    '$route.query.postId': {
      handler(newVal) {
        if (newVal) {
          this.jumpToTargetPost()
        }
      }
    },
    '$route.query.page': {
      async handler(newVal) {
        const page = Number(newVal) || 1;
        if (page !== this.currentPage) {
          this.currentPage = page;
          this.loading = true;
          try {
            await this.fetchPosts();
          } catch (e) {
            console.error('Error fetching page posts:', e);
          } finally {
            this.loading = false;
          }
          if (this.highlightedPostId) {
            this.$nextTick(() => {
              setTimeout(() => {
                const element = document.getElementById(`post-${this.highlightedPostId}`);
                if (element) {
                  element.scrollIntoView({ behavior: 'smooth', block: 'center' });
                  setTimeout(() => {
                     this.highlightedPostId = null;
                  }, 4000);
                }
              }, 400);
            });
          }
        }
      }
    },
    '$route.params.id': {
      async handler(newVal, oldVal) {
        if (newVal && newVal !== oldVal) {
          this.loading = true;
          try {
            await Promise.all([
              this.fetchReactionIcons(),
              this.fetchThread(),
              this.fetchPosts(),
              this.fetchFollowStatus()
            ]);
          } catch (e) {
            console.error('Lỗi khi tải lại dữ liệu trang:', e);
          } finally {
            this.loading = false;
          }
          this.jumpToTargetPost();
        }
      }
    },
    currentPage() {
      this.initQuoteCollapsing()
    },
    posts: {
      deep: true,
      handler() {
        this.initQuoteCollapsing()
      }
    },
    thread: {
      deep: true,
      handler() {
        this.initQuoteCollapsing()
      }
    }
  },
  methods: {
    async onNotificationClicked(event) {
      const { threadId } = event.detail;
      if (this.thread && String(this.thread.id) === String(threadId)) {
        await this.fetchThread();
        await this.fetchPosts();
        await this.fetchFollowStatus();
        this.jumpToTargetPost();
      }
    },
    openReactionPopup(orderNumber, targetId, isMainPost, summary) {
      this.reactionPopupData = {
        orderNumber,
        targetId,
        isMainPost,
        summary
      }
      this.showReactionPopup = true
    },
    async fetchReactionIcons() {
      try {
        const res = await reactionService.getIcons();
        this.reactionIconsList = res.data || [];
      } catch (e) {
        console.error('Lỗi khi tải Icons Reaction:', e);
      }
    },
    canShowReactionForReply(item) {
      if (!this.isLoggedIn || !item || !item.author || !this.currentUser || this.isNonOfficial) return false;
      return String(item.author.id) !== String(this.currentUser.id);
    },
    async reloadPostsOnly() {
      await this.fetchPosts();
    },
    async fetchFollowStatus() {
      if (!this.isLoggedIn) return;
      try {
        const res = await threadService.getFollowStatus(this.$route.params.id);
        this.isFollowing = res.data === true;
      } catch (e) {
        console.error('Lỗi khi tải trạng thái theo dõi:', e);
      }
    },
    async handleFollowToggle() {
      if (!this.isLoggedIn) return;
      const originalState = this.isFollowing;
      const actionText = originalState ? 'bỏ theo dõi' : 'theo dõi';
      const confirmRes = await alertConfirm(
        'Xác nhận',
        `Bạn có muốn ${actionText} bài đăng này không?`
      );
      if (confirmRes.isConfirmed) {
        try {
          await threadService.follow(this.$route.params.id, !originalState);
          this.isFollowing = !originalState;
          alertSuccess(`Đã ${originalState ? 'hủy theo dõi' : 'theo dõi'} bài viết thành công!`);
        } catch (e) {
          console.error('Lỗi khi cập nhật trạng thái theo dõi:', e);
          alertError('Có lỗi xảy ra, vui lòng thử lại sau.');
        }
      }
    },
    async fetchThread() {
      try {
        const response = await threadService.getById(this.$route.params.id)
        this.thread = response.data
        
        let content = this.thread.content || ''
        content = this.processMediaTags(content)

        this.thread = { ...response.data, content }

        // Fetch Group Name
        if (this.thread.category && this.thread.category.categoryGroupId) {
          try {
            const [catRes, groupRes] = await Promise.all([
               categoryService.getAll(),
               categoryService.getGroups()
            ])
            this.allCategories = catRes.data
            this.categoryGroup = groupRes.data.find(g => g.id === this.thread.category.categoryGroupId)
          } catch (e) {
            console.error('Lỗi khi tải nhóm chuyên mục:', e)
          }
        }
      } catch (error) {
        console.error('Lỗi khi tải chi tiết bài viết:', error)
      }
    },
    async fetchPosts() {
      try {
        const page = this.currentPage - 1;
        const size = this.itemsPerPage;
        const response = await postService.getByThreadId(this.$route.params.id, page, size)
        this.posts = response.data.content || []
        this.totalPosts = response.data.totalElements || 0
      } catch (error) {
        console.error('Lỗi khi tải bài bình luận:', error)
      }
    },
    changePage(page) {
      if (page !== this.currentPage) {
        this.$router.push({ query: { ...this.$route.query, page } });
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    },
    async jumpToTargetPost() {
      let pId = this.$route.query.postId;
      if (!pId && window.location.hash && window.location.hash.startsWith('#post-')) {
        pId = window.location.hash.replace('#post-', '');
      }
      if (!pId) return;
      
      let targetPage = 1;
      
      // 1. Check if Target is main post
      if (String(pId) === 'main_thread_entry') {
         targetPage = 1;
      } else {
         try {
           const res = await postService.getPageNumber(pId, this.itemsPerPage);
           targetPage = res.data || 1;
         } catch (e) {
           console.error('Error fetching page number for post:', e);
         }
      }

      // 3. Switch Page and navigate if different
      if (targetPage !== this.currentPage) {
         this.highlightedPostId = pId;
         this.$router.push({ query: { ...this.$route.query, page: targetPage } });
      } else {
         // Same page, just scroll to it
         this.highlightedPostId = pId;
         await this.$nextTick();
         setTimeout(() => {
           const element = document.getElementById(`post-${pId}`);
           if (element) {
             element.scrollIntoView({ behavior: 'auto', block: 'center' });
             setTimeout(() => {
                this.highlightedPostId = null;
             }, 4000);
           }
         }, 400);
      }
    },
    processMediaTags(content) {
      if (!content) return ''
      let fixed = content.replace(/<oembed\s+url="([^"]+)"><\/oembed>/gi, (match, url) => {
        const youtubeRegex = /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?\/\s]{11})/i
        const ytMatch = url.match(youtubeRegex)
        if (ytMatch && ytMatch[1]) {
          return `<iframe width="100%" height="450" src="https://www.youtube.com/embed/${ytMatch[1]}" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>`
        }
        
        if (url.match(/\.(mp4|webm|ogg|avi|mov)(\?.*)?$/i)) {
           let fixedUrl = url;
           const uploadsIndex = fixedUrl.indexOf('/uploads/');
           if (uploadsIndex !== -1) {
               fixedUrl = fixedUrl.substring(uploadsIndex);
           }
           return `<figure class="media"><video controls style="width: 100%; max-height: 500px; object-fit: contain; background: #000;" src="${fixedUrl}"></video></figure>`
        }
        return `<a href="${url}" target="_blank">${url}</a>`
      })
      
      if (fixed) {
        // Apply global wording update from "đã nói" to "đã viết" for render consistency
        fixed = fixed.replace(/đã nói:<\/strong>/g, 'đã viết:</strong>')
      }
      
      return fixed
    },
    formatPostContent(content) {
      return this.processMediaTags(content)
    },
    async submitReply() {
      if (!this.replyForm.content.trim()) {
        alertError('Vui lòng nhập nội dung trả lời')
        return
      }

      try {
        this.submittingPost = true
        
        // Clean up old attachment HTML if exists
        let cleanContent = this.replyForm.content || ''
        const markers = [
          /<div[^>]*class="attachment-block"[^>]*>/i,
          /<p><strong>Đính kèm<\/strong><\/p>/i
        ]
        let matchIndex = -1
        for (const marker of markers) {
          const match = cleanContent.match(marker)
          if (match) {
            matchIndex = match.index
            break
          }
        }
        if (matchIndex !== -1) {
          cleanContent = cleanContent.substring(0, matchIndex).trim()
        }

        let finalContent = cleanContent
        const attachedImages = this.replyAttachedImages

        if (attachedImages && attachedImages.length > 0) {
          let attachedHtml = `<div id="attachment-section" class="attachment-block" style="margin-top: 2rem; border-top: 1px dashed #ddd; padding-top: 1.5rem;">`
          attachedHtml += `<div class="attachment-label" style="font-weight: bold; color: #1a507a; margin-bottom: 1rem; font-size: 0.95rem;">Đính kèm</div>`
          attachedHtml += `<div class="attachment-list" style="display: flex; flex-wrap: wrap; gap: 15px;">`
          attachedImages.forEach(img => {
            attachedHtml += `<img src="${img.url}" alt="${img.name}" style="width: 200px; height: 200px; object-fit: cover; border: 1px solid #ddd; border-radius: 4px; cursor: pointer; display: inline-block; margin: 5px;" />`
          })
          attachedHtml += `</div></div>`
          finalContent = finalContent.trim() + '\n' + attachedHtml
        }

        const payload = {
          content: finalContent,
          threadId: this.thread.id,
          attachedImages: JSON.stringify(attachedImages)
        }

        const response = await postService.create(payload)
        
        await alertSuccess('Gửi trả lời thành công')
        
        if (response.data && response.data.autoFollowed) {
          this.isFollowing = true;
          const displayUser = this.currentUser ? this.currentUser.username : 'Ẩn danh';
          const displayAuthor = this.thread.author ? (this.thread.author.displayName || this.thread.author.username) : 'Tác giả';
          await alertSuccess('Từ bây giờ trở đi tài khoản ' + displayUser + ' đã theo dõi bài đăng ' + window.location.href + ' này của tác giả ' + displayAuthor);
        }

        this.replyForm.content = ''
        this.replyAttachedImages = []
        
        this.totalPosts++
        const lastPage = this.totalPages
        if (this.currentPage !== lastPage) {
          this.$router.push({ query: { ...this.$route.query, page: lastPage } })
        } else {
          await this.fetchPosts()
        }
        
        this.$nextTick(() => {
          setTimeout(() => {
            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
          }, 400);
        })
      } catch (error) {
        console.error(error)
        alertError('Không thể gửi câu trả lời')
      } finally {
        this.submittingPost = false
      }
    },
    quotePost(authorName, rawContent, sourceId) {
      if (!this.isLoggedIn) {
        this.$router.push({ name: 'Login' })
        return
      }
      
      const tempDiv = document.createElement('div')
      tempDiv.innerHTML = rawContent
      
      const innerQuotes = tempDiv.querySelectorAll('blockquote')
      innerQuotes.forEach(q => q.remove())

      const attachments = tempDiv.querySelector('.attachment-block')
      if (attachments) attachments.remove()

      const trimmedContent = tempDiv.innerHTML.trim() // safety trim (increased to avoid truncation)
      
      // Use data-source attribute. Preserved cleanly by modified CKEditor plugin without live links inside composer.
      const sourceAttr = sourceId ? ` data-source="${sourceId}"` : '';
      const quoteHtml = `<blockquote${sourceAttr}><p><strong>${authorName} đã viết:</strong></p>${trimmedContent}</blockquote><p>&nbsp;</p>`
      
      this.replyForm.content = this.replyForm.content + quoteHtml
      
      this.$nextTick(() => {
        const element = this.$refs.replyFormContainer
        if (element) {
          element.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }
      })
    },
    handleInsertImages(urls, type) {
      if (this.$refs.replyEditor && this.$refs.replyEditor.insertImages) {
        this.$refs.replyEditor.insertImages(urls, type)
      }
    },
    handleImageUploaded(image) {
      this.replyAttachedImages.push(image)
    },

    formatDate(dateStr) {
      return formatForumDate(dateStr)
    },
    handleVoteUpdated(updatedPoll) {
      if (this.thread) {
        this.thread.poll = updatedPoll
      }
    },
    handleContentClick(e) {
      // --- Xử lý click vào link tài liệu đính kèm (📎 filename) để download ---
      const clickedLink = e.target.closest('a');
      if (clickedLink) {
        const filename = extractAttachmentFilename(clickedLink);
        if (filename) {
          e.preventDefault();
          e.stopPropagation();
          downloadFileAsBlob(clickedLink.href, filename);
          return;
        }
      }

      if (e.target.tagName === 'IMG') {
        this.openLightbox(e.target.src)
        return
      }

      // Intercept clicks on quote headers (both future-proof and legacy support)
      const strongElem = e.target.closest('blockquote p:first-child strong');
      if (!strongElem) return;

      const blockquote = strongElem.closest('blockquote');
      let targetId = null;

      // 1. First try loading from dedicated data attribute
      if (blockquote && blockquote.hasAttribute('data-source')) {
        targetId = blockquote.getAttribute('data-source');
      } 
      // 2. Fallback: Smart Heuristic lookup for legacy quotes that lacked ID references
      else if (strongElem.textContent) {
        const text = strongElem.textContent;
        const match = text.match(/(.*?)\s*đã\s*(?:viết|nói):/i);
        if (match && match[1]) {
          const quotedName = match[1].trim();
          
          const container = strongElem.closest('.thread-content-card');
          if (container) {
            const containerId = container.id.replace('post-', '');
            
            const list = [
              { id: 'main_thread_entry', author: this.thread?.author?.displayName || this.thread?.author?.username },
              ...this.posts.map(p => ({ id: String(p.id), author: p.author?.displayName || p.author?.username }))
            ];
            
            let currentIdx = list.findIndex(i => i.id === containerId);
            if (currentIdx === -1) currentIdx = list.length; 

            for (let i = currentIdx - 1; i >= 0; i--) {
               if (list[i].author === quotedName) {
                 targetId = list[i].id;
                 break;
               }
            }
          }
        }
      }

      if (!targetId) return;

      // 3. Calculate target page number
      let targetPage = 1;
      if (targetId === 'main_thread_entry') {
          targetPage = 1;
      } else if (this.posts && this.posts.length > 0) {
          const idx = this.posts.findIndex(p => String(p.id) === String(targetId));
          if (idx !== -1) {
             const seqNum = idx + 2;
             targetPage = Math.ceil(seqNum / this.itemsPerPage);
          }
      }

      // 4. Execute branch logic based on page locality
      if (targetPage === this.currentPage) {
          // SCENARIO 1: SAME PAGE - Local scroll with highlight effect
          this.highlightedPostId = targetId;
          const element = document.getElementById(`post-${targetId}`);
          if (element) {
             element.scrollIntoView({ behavior: 'smooth', block: 'center' });
             setTimeout(() => { this.highlightedPostId = null; }, 4000);
          }
      } else {
          // SCENARIO 2: DIFFERENT PAGE - Open in new tab directly focused on target
          const route = this.$router.resolve({
             path: this.$route.path,
             query: { ...this.$route.query, postId: targetId }
          });
          window.open(route.href, '_blank');
      }
    },
    openLightbox(url) {
      this.activeImageUrl = url
      this.showLightbox = true
      document.body.style.overflow = 'hidden'
    },
    closeLightbox() {
      this.showLightbox = false
      this.activeImageUrl = ''
      document.body.style.overflow = ''
    },
    stripAttachments(content) {
      if (!content) return ''
      let cleanContent = content
      const markers = [
        /<div[^>]*class="attachment-block"[^>]*>/i,
        /<p><strong>Đính kèm<\/strong><\/p>/i
      ]
      let matchIndex = -1
      for (const marker of markers) {
        const match = cleanContent.match(marker)
        if (match) {
          matchIndex = match.index
          break
        }
      }
      if (matchIndex !== -1) {
        cleanContent = cleanContent.substring(0, matchIndex).trim()
      }
      return cleanContent
    },
    canEdit(item) {
      if (!this.isLoggedIn || !this.currentUser) return false;
      const author = item.isMain ? this.thread.author : item.author;
      if (!author) return false;
      return this.currentUser.username === author.username;
    },
    startEditing(item) {
      this.editingItemId = item.id;
      const rawContent = item.isMain ? this.thread.content : item.content;
      this.editForm.content = this.stripAttachments(rawContent);
      
      try {
        const imgsStr = item.isMain ? this.thread.attachedImages : item.attachedImages;
        this.editAttachedImages = imgsStr ? JSON.parse(imgsStr) : [];
      } catch (e) {
        this.editAttachedImages = [];
      }
    },
    cancelEditing() {
      this.editingItemId = null;
      this.editForm.content = '';
      this.editAttachedImages = [];
    },
    async submitEdit(item) {
      let cleanContent = this.stripAttachments(this.editForm.content);
      if (!cleanContent.trim()) {
        alertError('Nội dung không được để trống');
        return;
      }

      try {
        this.submittingEdit = true;
        let finalContent = cleanContent;
        const attachedImages = this.editAttachedImages;

        if (attachedImages && attachedImages.length > 0) {
          let attachedHtml = `<div id="attachment-section" class="attachment-block" style="margin-top: 2rem; border-top: 1px dashed #ddd; padding-top: 1.5rem;">`
          attachedHtml += `<div class="attachment-label" style="font-weight: bold; color: #1a507a; margin-bottom: 1rem; font-size: 0.95rem;">Đính kèm</div>`
          attachedHtml += `<div class="attachment-list" style="display: flex; flex-wrap: wrap; gap: 15px;">`
          attachedImages.forEach(img => {
            attachedHtml += `<img src="${img.url}" alt="${img.name}" style="width: 200px; height: 200px; object-fit: cover; border: 1px solid #ddd; border-radius: 4px; cursor: pointer; display: inline-block; margin: 5px;" />`
          })
          attachedHtml += `</div></div>`
          finalContent = finalContent.trim() + '\n' + attachedHtml
        }

        if (item.isMain) {
          const payload = {
            title: this.thread.title,
            content: finalContent,
            attachedImages: JSON.stringify(attachedImages),
            category: this.thread.category,
            label: this.thread.label
          }
          await threadService.update(this.thread.id, payload);
          await this.fetchThread();
        } else {
          const payload = {
            content: finalContent,
            attachedImages: JSON.stringify(attachedImages)
          }
          await postService.update(item.id, payload);
          await this.fetchPosts();
        }

        alertSuccess('Cập nhật nội dung thành công');
        this.cancelEditing();
      } catch (error) {
        console.error('Lỗi cập nhật:', error);
        alertError('Không thể cập nhật nội dung');
      } finally {
        this.submittingEdit = false;
      }
    },
    handleEditInsertImages(urls, type) {
      if (this.$refs.inlineEditEditor) {
        const editor = Array.isArray(this.$refs.inlineEditEditor) ? this.$refs.inlineEditEditor[0] : this.$refs.inlineEditEditor;
        if (editor && editor.insertImages) {
          editor.insertImages(urls, type);
        }
      }
    },
    handleEditImageUploaded(image) {
      this.editAttachedImages.push(image);
    },
    initQuoteCollapsing() {
      setTimeout(() => {
        const blockquotes = document.querySelectorAll('.content-body blockquote');
        blockquotes.forEach(bq => {
          if (bq.dataset.quoteInitialized) return;
          
          const height = bq.scrollHeight;
          if (height > 160) {
            bq.classList.add('collapsible');
            bq.classList.add('quote-collapsed');
            bq.dataset.quoteInitialized = 'true';
            
            // Tránh chèn trùng nút bấm nếu đã tồn tại
            if (bq.nextSibling && bq.nextSibling.className === 'quote-expand-bar') {
              return;
            }
            
            const btnBar = document.createElement('div');
            btnBar.className = 'quote-expand-bar';
            btnBar.textContent = 'Nhấn để mở rộng...';
            
            bq.parentNode.insertBefore(btnBar, bq.nextSibling);
            
            btnBar.addEventListener('click', () => {
              if (bq.classList.contains('quote-collapsed')) {
                bq.classList.remove('quote-collapsed');
                btnBar.textContent = 'Nhấn để thu hẹp';
              } else {
                bq.classList.add('quote-collapsed');
                btnBar.textContent = 'Nhấn để mở rộng...';
                bq.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
              }
            });
          } else if (height > 0) {
            bq.dataset.quoteInitialized = 'true';
          }
        });
      }, 150);
    }
  }
}
</script>

<style scoped>
.thread-title-full {
  padding: 1.2rem 1.5rem;
  margin: 0;
  border-bottom: 1px solid #eee;
}

.thread-title-full h1 {
  margin: 0;
  font-size: 1.4rem;
  color: #333;
  font-weight: 500;
  line-height: 1.4;
}

.label-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  padding: 2px 8px;
  font-size: 0.85rem;
  border-radius: 4px;
  font-weight: 600;
  border: 1px solid transparent;
  margin-right: 8px;
  vertical-align: middle;
  position: relative;
  top: -2px;
}

.thread-meta-bar {
  padding: 8px 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f8f9fa;
  font-size: 0.85rem;
  color: #666;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.meta-icon {
  color: #999;
}

.meta-dot {
  margin: 0 4px;
}

.poll-wrapper {
  margin-top: 15px;
}

.thread-content-card {
  margin-top: 15px;
  min-height: 200px;
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
  position: relative;
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
  min-height: 200px;
}

.post-meta-top {
  padding: 10px 15px;
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
  color: #999;
  font-size: 0.85rem;
}

.post-actions-top {
  display: flex;
  align-items: center;
  gap: 10px;
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

:deep(.ql-editor img), :deep(.ql-editor video) { max-width: 100%; height: auto; }
:deep(.ql-editor .image-inline img) {
  width: 200px !important;
  height: 200px !important;
  object-fit: cover !important;
}
:deep(.ql-editor table) { border-collapse: collapse; width: 100%; margin: 1rem 0; }
:deep(.ql-editor td) { border: 1px solid #ccc; padding: 8px; }

/* Blockquote styles updated to match requested reference style (Orange accent) */
:deep(.ql-editor blockquote), :deep(.ck-content blockquote) {
  background: #fcfbf7;
  border-left: 3px solid #e67e22;
  padding: 12px 16px;
  margin: 10px 0;
  font-style: normal;
  color: #657786;
  border-radius: 4px;
  position: relative;
}

:deep(.ql-editor blockquote.collapsible), :deep(.ck-content blockquote.collapsible) {
  margin-bottom: 0 !important;
  border-bottom-left-radius: 0 !important;
  border-bottom-right-radius: 0 !important;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

:deep(.ql-editor blockquote.quote-collapsed), :deep(.ck-content blockquote.quote-collapsed) {
  max-height: 200px;
}

/* Gradient fade at bottom when collapsed */
:deep(.ql-editor blockquote.quote-collapsed::after), :deep(.ck-content blockquote.quote-collapsed::after) {
  content: "";
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 45px;
  background: linear-gradient(to bottom, rgba(252, 251, 247, 0), #fcfbf7);
  pointer-events: none;
}

/* Expand/collapse bar */
:deep(.quote-expand-bar) {
  background: #fcfbf7;
  border-left: 3px solid #e67e22;
  border-bottom-left-radius: 4px;
  border-bottom-right-radius: 4px;
  text-align: center;
  padding: 8px 0;
  cursor: pointer;
  user-select: none;
  font-size: 0.85rem;
  font-weight: bold;
  color: #e67e22;
  margin-top: -1px;
  margin-bottom: 10px;
  border-top: 1px dashed rgba(230, 126, 34, 0.2);
  transition: background-color 0.2s, color 0.2s;
}

:deep(.quote-expand-bar:hover) {
  background-color: #faf6eb;
  color: #d35400;
}

:deep(.ql-editor blockquote p:first-child strong), :deep(.ck-content blockquote p:first-child strong) {
  color: #e67e22;
  font-size: 0.9rem;
}

/* EXCLUSIVE TO READER (HIDDEN IN EDITOR) */
.content-body :deep(blockquote p:first-child strong) {
  cursor: pointer !important;
  display: inline-flex !important;
  align-items: center !important;
  gap: 6px;
  transition: text-decoration 0.2s;
}

.content-body :deep(blockquote p:first-child strong:hover) {
  text-decoration: underline !important;
}

/* Circular Arrow Icon - RENDERER ONLY */
.content-body :deep(blockquote p:first-child strong::after) {
  content: '\2191' !important; 
  display: inline-flex !important;
  justify-content: center !important;
  align-items: center !important;
  width: 16px !important;
  height: 16px !important;
  background: #f39c12 !important;
  color: white !important;
  border-radius: 50% !important;
  font-size: 10px !important;
  line-height: 1 !important;
  font-weight: bold !important;
  text-decoration: none !important;
  position: relative;
  top: -1px;
}

.reply-card {
  margin-top: 10px !important;
}

.btn-post {
  background-color: #3498db;
  color: white;
  border: none;
  padding: 10px 25px;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s;
}

.btn-post:hover:not(:disabled) {
  background-color: #2980b9;
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

.btn-post:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

:deep(.ck-editor__editable) {
  min-height: 150px;
}


.attached-section {
  margin-top: 1.5rem;
  border-top: 1px dashed #ddd;
  padding-top: 1.5rem;
  padding-left: 15px; /* Cân lề ngang với phần text content */
}

.attached-label {
  font-weight: bold;
  color: #1a507a;
  margin-bottom: 1rem;
  font-size: 0.95rem;
}

.attached-list {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.attached-item img {
  max-width: 200px;
  max-height: 200px;
  object-fit: cover;
  border: 1px solid #eee;
  border-radius: 4px;
  cursor: zoom-in;
  transition: transform 0.2s;
}

.attached-item img:hover {
  transform: scale(1.02);
}

/* Lightbox Styles */
.lightbox-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.9);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.lightbox-content {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.btn-close-lightbox {
  position: absolute;
  top: 20px;
  right: 20px;
  background: transparent;
  border: none;
  color: white;
  font-size: 40px;
  cursor: pointer;
  z-index: 10001;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lightbox-main {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.lightbox-image-wrapper {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.main-lightbox-img {
  max-width: 100%;
  max-height: calc(100% - 60px);
  object-fit: contain;
  transition: transform 0.3s;
}

/* Inline Editing Styles */
.inline-edit-box {
  border: 1px solid #dcdcdc;
  border-radius: 4px;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #fff;
  margin: 15px;
  overflow: hidden;
}

.inline-edit-box:focus-within {
  border-color: #3498db;
  box-shadow: 0 0 8px rgba(52, 152, 219, 0.25);
}

.edit-actions-footer {
  padding: 12px 15px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  background: #f8f9fa;
  border-top: 1px solid #eee;
}

.btn-save {
  background-color: #3498db;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: 0.2s;
}

.btn-save:hover:not(:disabled) {
  background-color: #2980b9;
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-cancel-edit {
  background-color: #7f8c8d;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s;
}

.btn-cancel-edit:hover {
  background-color: #95a5a6;
}

.btn-icon {
  display: inline-block;
  vertical-align: middle;
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

/* Styling for Thread Follow Button */
.btn-follow-thread {
  background-color: white;
  border: 1px solid #2577b1;
  color: #2577b1;
  padding: 6px 14px;
  border-radius: 4px;
  font-size: 0.9rem;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s, color 0.2s;
  outline: none;
}

.btn-follow-thread:hover {
  background-color: #edf6fd;
}

/* User information block and speech bubble layout updates */
.author-info-mobile-block {
  display: flex;
  flex-direction: column;
  align-items: center;
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

/* Mobile responsive adjustments */
@media (max-width: 767px) {
  /* Center top action pagination */
  .thread-action-bar {
    flex-direction: column !important;
    justify-content: center !important;
    align-items: center !important;
    gap: 12px !important;
  }
  
  .pagination-wrapper-left,
  .follow-btn-wrapper-right {
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

  /* Align user text left on mobile */
  .author-info-mobile-block {
    align-items: flex-start !important;
    text-align: left !important;
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
}
</style>
