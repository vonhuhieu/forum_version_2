<template>
  <div>
    <main class="container" style="padding-top: 2rem;">
      <div v-if="loading" style="text-align: center; padding: 3rem;">Đang tải...</div>
      
      <div v-else>
        <!-- Block 1: Breadcrumb -->
        <Breadcrumb :items="breadcrumbItems" />

        <!-- Block 2: Danh sách bài viết mới nhất -->
        <div class="card" style="margin-bottom: 2rem;">
          <div class="card-header" style="display: flex; justify-content: space-between; align-items: center;">
            <span>Mới ra lò - Danh sách bài viết mới nhất</span>
            <button v-if="isLoggedIn && !isNonOfficial" class="btn-post-thread" @click="openPostModal">Đăng bài...</button>
          </div>

          <div class="pagination-wrapper" style="padding: 1rem; border-top: 1px solid #eee;">
            <ForumPagination 
              :current-page="currentPage" 
              :total-pages="totalPages" 
              @page-changed="currentPage = $event"
            />
          </div>
          
          <div class="thread-list">
            <div v-for="thread in paginatedThreads" :key="thread.id" class="thread-row thread-row-center">
              <div class="thread-avatar" :style="!isAvatarUrl(thread.author?.avatar) ? { backgroundColor: thread.author?.avatar || '#ccc', color: '#fff' } : {}">
                <img v-if="isAvatarUrl(thread.author?.avatar)" :src="thread.author.avatar" />
                <template v-else>
                  {{ thread.author ? (thread.author.displayName || thread.author.username).charAt(0).toUpperCase() : 'A' }}
                </template>
              </div>
              <div class="thread-main">
                <div class="thread-title">
                  <span v-if="thread.label" class="label-tag" :style="{ backgroundColor: thread.label.colorCode, color: thread.label.textColor, borderColor: thread.label.borderColor || 'transparent' }">
                    {{ thread.label.name }}
                  </span>
                  <router-link :to="{ name: 'ThreadDetail', params: { id: thread.id } }">{{ thread.title }}</router-link>
                  <span v-if="thread.pinned" title="Đã ghim" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 6px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="icon-pin" style="display: block; pointer-events: none;"><line x1="12" y1="17" x2="12" y2="22"></line><path d="M5 17h14v-1.76a2 2 0 0 0-.44-1.24l-2.78-3.5A2 2 0 0 1 15 9.26V5a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4.26a2 2 0 0 1-.78 1.24l-2.78 3.5a2 2 0 0 0-.44 1.24z"></path></svg>
                  </span>
                  <span v-if="thread.locked" title="Đã khóa" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 6px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-lock" style="display: block; pointer-events: none;"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                  </span>
                </div>
                <div class="thread-meta">
                  <span class="author-name white-space-nowrap">{{ thread.author ? (thread.author.displayName || thread.author.username) : 'Ẩn danh' }}</span>
                  <span class="dot-divider">•</span>
                  <router-link :to="{ name: 'ThreadDetail', params: { id: thread.id } }" class="meta-link">{{ formatDate(thread.createdAt) }}</router-link>
                  
                  <span class="quick-pages" v-if="getThreadPages(thread.replyCount).length > 0">
                    <router-link 
                      v-for="p in getThreadPages(thread.replyCount)" 
                      :key="p" 
                      :to="{ name: 'ThreadDetail', params: { id: thread.id }, query: { page: p } }"
                      class="page-badge"
                    >
                      {{ p }}
                    </router-link>
                  </span>
                </div>
                <div class="thread-author-mobile">
                  {{ thread.author ? (thread.author.displayName || thread.author.username) : 'Ẩn danh' }}
                </div>
                <div class="thread-stats-mobile">
                  Trả lời: {{ thread.replyCount || 0 }} <span class="dot-divider">•</span> {{ formatDate(thread.lastPostAt || thread.createdAt) }}
                </div>
              </div>
              <div class="thread-stats">
                <div class="stat-block">
                  <span class="stat-label">Trả lời:</span>
                  <span class="stat-value">{{ thread.replyCount || 0 }}</span>
                </div>
                <div class="stat-block">
                  <span class="stat-label">Xem:</span>
                  <span class="stat-value">{{ thread.viewCount || 0 }}</span>
                </div>
              </div>
              <div class="thread-last-post">
                <div class="last-post-info">
                  <router-link 
                    :to="thread.lastPostId ? { name: 'ThreadDetail', params: { id: thread.id }, query: { postId: thread.lastPostId } } : { name: 'ThreadDetail', params: { id: thread.id } }" 
                    class="last-post-time-link">
                    {{ formatDate(thread.lastPostAt || thread.createdAt) }}
                  </router-link>
                  <span class="last-post-author">{{ (thread.lastPostAuthor || thread.author)?.displayName || (thread.lastPostAuthor || thread.author)?.username || 'Ẩn danh' }}</span>
                </div>
                <div class="last-post-avatar" :style="!isAvatarUrl((thread.lastPostAuthor || thread.author)?.avatar) ? { backgroundColor: (thread.lastPostAuthor || thread.author)?.avatar || '#ccc', color: '#fff' } : {}">
                  <img v-if="isAvatarUrl((thread.lastPostAuthor || thread.author)?.avatar)" :src="(thread.lastPostAuthor || thread.author)?.avatar" />
                  <template v-else>
                    {{ ((thread.lastPostAuthor || thread.author)?.displayName || (thread.lastPostAuthor || thread.author)?.username || 'A').charAt(0).toUpperCase() }}
                  </template>
                </div>
              </div>
            </div>

            <div v-if="!threads || threads.length === 0"
              style="padding: 2rem; text-align: center; color: #999;">
              Chưa có bài viết nào.
            </div>
          </div>
          
          <!-- Phân trang -->
          <div class="pagination-wrapper" style="padding: 1rem; border-top: 1px solid #eee;">
            <ForumPagination 
              :current-page="currentPage" 
              :total-pages="totalPages" 
              @page-changed="currentPage = $event"
            />
          </div>
        </div>

        <Breadcrumb :items="breadcrumbItems" />
      </div>
    </main>

    <!-- Modal chọn chuyên mục -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal-card" style="width: 750px; max-width: 95vw; background: #f5f8fa; padding: 0; border-radius: 6px; overflow: hidden;">
        <div class="card-header" style="display: flex; justify-content: space-between; align-items: center; background: #e6f2fa; color: #1a507a; padding: 12px 20px; border-bottom: 1px solid #d0e3f0;">
          <span style="font-size: 1.15rem; font-weight: normal;">Đăng bài trong...</span>
          <button @click="showModal = false" style="background: none; border: none; color: #7cb3db; cursor: pointer; font-size: 1.5rem; line-height: 1;">&times;</button>
        </div>
        <div class="modal-body" style="max-height: 70vh; overflow-y: auto; padding: 0;">
          <div v-for="group in activeModalGroups" :key="group.id" class="modal-group" style="margin-bottom: 15px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
            <div class="modal-group-header">
              {{ group.name }}
            </div>
            <div class="modal-category-list">
              <template v-for="cat in group.categories.filter(c => !c.parentCategoryId)" :key="cat.id">
                <div class="modal-category-item" @click="selectCategory(cat.id)">
                  <div class="modal-cat-info">
                    <div class="modal-cat-name">{{ cat.name }}</div>
                    <div v-if="cat.description" class="modal-cat-desc">{{ cat.description }}</div>
                  </div>
                  <div class="modal-cat-stats">
                    <div class="modal-stat-label">Chủ đề</div>
                    <div class="modal-stat-value">{{ formatNumber(cat.threadCount || 0) }}</div>
                  </div>
                </div>
                <div v-for="sub in (cat.subCategories && cat.subCategories.length ? cat.subCategories : group.categories.filter(c => c.parentCategoryId === cat.id))" :key="'sub-' + sub.id" class="modal-category-item modal-sub-category" @click="selectCategory(sub.id)">
                  <div class="modal-cat-info" style="padding-left: 30px; position: relative;">
                    <div style="position: absolute; left: 10px; top: 12px; width: 15px; height: 15px; border-left: 2px solid #d0e3f0; border-bottom: 2px solid #d0e3f0; border-bottom-left-radius: 4px;"></div>
                    <div class="modal-cat-name" style="font-size: 0.95rem;">{{ sub.name }}</div>
                    <div v-if="sub.description" class="modal-cat-desc">{{ sub.description }}</div>
                  </div>
                  <div class="modal-cat-stats">
                    <div class="modal-stat-label">Chủ đề</div>
                    <div class="modal-stat-value">{{ formatNumber(sub.threadCount || 0) }}</div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import threadService from '@/apps/Forum/services/thread.service'
import categoryService from '@/apps/Forum/services/category.service'
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import { formatForumDate } from '@/shared/utils/date'
import { isNonOfficialUser, isAvatarUrl } from '@/shared/utils/utils'

export default {
  name: 'LatestThreadsView',
  components: {
    Breadcrumb,
    ForumPagination
  },
  data() {
    return {
      threads: [],
      loading: true,
      currentPage: 1,
      itemsPerPage: 10,
      isLoggedIn: false,
      showModal: false,
      categoryGroupsModal: [],
      totalPagesCount: 1,
      totalElements: 0
    }
  },
  watch: {
    currentPage: {
      async handler(newPage, oldPage) {
        if (newPage !== oldPage) {
          this.loading = true
          try {
            await this.fetchThreadsPaged()
          } finally {
            this.loading = false
          }
        }
      }
    }
  },
  computed: {
    isNonOfficial() {
      return isNonOfficialUser()
    },
    breadcrumbItems() {
      return [
        { title: 'Trang chủ', to: { name: 'Home' } },
        { title: 'Mới ra lò' }
      ]
    },
    totalPages() {
      return this.totalPagesCount
    },
    paginatedThreads() {
      return this.threads
    },
    activeModalGroups() {
      if (!this.categoryGroupsModal || !Array.isArray(this.categoryGroupsModal)) return []
      return this.categoryGroupsModal.filter(g => g.active && g.categories && g.categories.length > 0)
    }
  },
  async mounted() {
    this.checkAuth()
    await this.fetchData()
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
      this.threads = this.threads.map(t => {
        const updated = { ...t }
        if (t.author && t.author.username === username) {
          updated.author = { ...t.author, avatar }
        }
        if (t.lastPostAuthor && t.lastPostAuthor.username === username) {
          updated.lastPostAuthor = { ...t.lastPostAuthor, avatar }
        }
        return updated
      })
    },
    checkAuth() {
      const user = localStorage.getItem('user')
      this.isLoggedIn = !!user
    },
    async openPostModal() {
      try {
        const response = await categoryService.getGroups()
        this.categoryGroupsModal = response.data
        this.showModal = true
      } catch (error) {
        console.error('Lỗi khi tải nhóm chuyên mục:', error)
      }
    },
    selectCategory(catId) {
      this.showModal = false
      this.$router.push({ name: 'CreateThread', query: { catId } })
    },
    formatNumber(num) {
      if (!num) return 0
      return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",")
    },
    async fetchData() {
      this.loading = true
      try {
        await this.fetchThreadsPaged()
      } catch (error) {
        console.error('Lỗi khi tải dữ liệu bài viết mới nhất:', error)
      } finally {
        this.loading = false
      }
    },
    async fetchThreadsPaged() {
      const page = this.currentPage - 1
      const size = this.itemsPerPage
      const res = await threadService.getAll({ page, size })
      if (res.data && res.data.content) {
        this.threads = res.data.content
        this.totalPagesCount = res.data.totalPages || 1
        this.totalElements = res.data.totalElements || 0
      } else {
        this.threads = []
        this.totalPagesCount = 1
        this.totalElements = 0
      }
    },
    formatDate(dateStr) {
      return formatForumDate(dateStr)
    },
    getThreadPages(replyCount) {
      const itemsPerPage = 10;
      const totalItems = 1 + (replyCount || 0);
      const totalPages = Math.ceil(totalItems / itemsPerPage);
      
      if (totalPages <= 1) return [];
      if (totalPages === 2) return [2];
      if (totalPages === 3) return [2, 3];
      
      return [totalPages - 2, totalPages - 1, totalPages];
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
.label-tag {
  padding: 2px 6px;
  font-size: 0.75rem;
  border-radius: 3px;
  font-weight: 600;
  display: inline-block;
  border: 1px solid transparent;
  margin-right: 8px;
  white-space: nowrap;
  vertical-align: middle;
  line-height: 1;
}

.thread-title {
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  max-width: 100%;
}

.thread-title span {
  flex-shrink: 0;
}

.thread-title a {
  text-decoration: none;
  color: #1a507a;
  font-weight: 500;
  font-size: 1.05rem;
  line-height: 1.5;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
  flex: 1;
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

/* Modal Post Styles */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-group { margin-bottom: 5px; }
.modal-group-header { background: #f0f7fb; color: #3498db; padding: 8px 20px; font-weight: 600; font-size: 0.95rem; border-bottom: 1px solid #e1eef7; }
.modal-category-list { background: #ffffff; }
.modal-category-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 20px; border-bottom: 1px solid #f0f0f0; cursor: pointer; transition: background 0.2s; }
.modal-category-item:hover { background: #f9fbfc; }
.modal-category-item:last-child { border-bottom: none; }
.modal-cat-name { color: #3498db; font-size: 1.05rem; font-weight: 500; }
.modal-category-item:hover .modal-cat-name { text-decoration: underline; }
.modal-cat-desc { font-size: 0.8rem; color: #888; margin-top: 3px; }
.modal-cat-stats { text-align: right; color: #666; min-width: 60px; }
.modal-stat-label { font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.5px; color: #999; }
.modal-stat-value { font-size: 0.95rem; font-weight: 500; margin-top: 2px; }

@import "@/shared/assets/styles/custom.css";
</style>
