<template>
  <div>
    <main class="container" style="padding-top: 2rem;">
      <div v-if="loading" style="text-align: center; padding: 3rem;">Đang tải...</div>
      
      <div v-else>
        <!-- Block 1: Breadcrumb -->
        <Breadcrumb :items="breadcrumbItems" />

        <!-- Block 2: Danh sách bài viết -->
        <div class="card" style="margin-bottom: 2rem;">
          <div class="card-header" style="display: flex; justify-content: space-between; align-items: center;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <span>{{ category ? category.name : 'Chuyên mục' }}</span>
              <span v-if="category" style="font-size: 0.8rem; font-weight: normal; opacity: 0.8;">{{ category.description }}</span>
            </div>
            <button v-if="isLoggedIn && !isNonOfficial" class="btn-post-thread" @click="goToCreateThread">Đăng bài...</button>
          </div>

          <div class="pagination-wrapper" style="padding: 1rem; border-top: 1px solid #eee;">
            <ForumPagination 
              :current-page="currentPage" 
              :total-pages="totalPages" 
              @page-changed="currentPage = $event"
            />
          </div>

          <!-- Sub-categories block (New) -->
          <div v-if="category && category.subCategories && category.subCategories.length > 0" class="sub-categories-block">
            <div class="sub-categories-list">
              <div v-for="sub in category.subCategories" :key="sub.id" class="category-row">
                <div class="category-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#f39c12" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
                </div>
                <div class="category-info">
                  <div class="cat-name-row">
                    <router-link :to="{ name: 'CategoryDetail', params: { id: sub.id } }" class="category-name">
                      {{ sub.name }}
                    </router-link>
                  </div>
                  <div v-if="sub.description" class="cat-desc">{{ sub.description }}</div>
                </div>
                <div class="category-stats">
                  <div class="stat-item">
                    <span class="stat-label">Chủ đề</span>
                    <span class="stat-value">{{ formatNumber(sub.threadCount || 0) }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">Bài viết</span>
                    <span class="stat-value">{{ formatNumber(sub.postCount || 0) }}</span>
                  </div>
                </div>
                <div class="category-last-thread">
                  <div v-if="lastThreadByCat[sub.id]" class="last-thread-box">
                    <div class="last-thread-avatar" :style="!isAvatarUrl((lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.avatar) ? { backgroundColor: (lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.avatar || '#ccc', color: '#fff' } : {}">
                      <img v-if="isAvatarUrl((lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.avatar)" :src="(lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.avatar" />
                      <template v-else>
                        {{ ((lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.displayName || (lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.username || 'A').charAt(0).toUpperCase() }}
                      </template>
                    </div>
                    <div class="last-thread-info">
                      <router-link :to="{ name: 'ThreadDetail', params: { id: lastThreadByCat[sub.id].id } }" class="last-thread-title" :title="lastThreadByCat[sub.id].title">
                        <span v-if="lastThreadByCat[sub.id].label" class="label-tag-mini" :style="{ backgroundColor: lastThreadByCat[sub.id].label.colorCode, color: lastThreadByCat[sub.id].label.textColor, borderColor: lastThreadByCat[sub.id].label.borderColor || 'transparent' }">
                          {{ lastThreadByCat[sub.id].label.name }}
                        </span>
                        <span class="title-txt">{{ lastThreadByCat[sub.id].title }}</span>
                        <span v-if="lastThreadByCat[sub.id].pinned" title="Đã ghim" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 4px;">
                          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="icon-pin" style="display: block; pointer-events: none;"><line x1="12" y1="17" x2="12" y2="22"></line><path d="M5 17h14v-1.76a2 2 0 0 0-.44-1.24l-2.78-3.5A2 2 0 0 1 15 9.26V5a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4.26a2 2 0 0 1-.78 1.24l-2.78 3.5a2 2 0 0 0-.44 1.24z"></path></svg>
                        </span>
                        <span v-if="lastThreadByCat[sub.id].locked" title="Đã khóa" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 4px;">
                          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: block; pointer-events: none;"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                        </span>
                      </router-link>
                      <div class="last-thread-meta">
                        <span>{{ formatDate(lastThreadByCat[sub.id].lastPostAt || lastThreadByCat[sub.id].createdAt) }}</span>
                        <span class="dot">•</span>
                        <span class="author">{{ (lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.displayName || (lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.username || 'Ẩn danh' }}</span>
                      </div>
                    </div>
                  </div>
                  <div v-else class="no-thread" style="color: #999; font-size: 0.85rem;">Chưa có bài viết</div>
                </div>
              </div>
            </div>
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
              Chưa có bài viết nào trong mục này.
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
  name: 'CategoryView',
  components: {
    Breadcrumb,
    ForumPagination
  },
  data() {
    return {
      category: null,
      categoryGroup: null,
      allCategories: [],
      lastThreadByCat: {},
      threads: [],
      loading: true,
      currentPage: 1,
      itemsPerPage: 10,
      isLoggedIn: false,
      totalPagesCount: 1,
      totalElements: 0,
      isChangingCategory: false
    }
  },
  watch: {
    '$route.params.id': {
      async handler(newId, oldId) {
        if (newId && newId !== oldId) {
          this.isChangingCategory = true
          this.currentPage = 1
          await this.fetchData()
          this.isChangingCategory = false
        }
      }
    },
    currentPage: {
      async handler(newPage, oldPage) {
        if (this.isChangingCategory) return
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
      const items = [{ title: 'Trang chủ', to: { name: 'Home' } }]
      
      if (this.categoryGroup) {
        items.push({ 
          title: this.categoryGroup.name, 
          to: { name: 'Home', hash: `#group-${this.categoryGroup.id}` } 
        })
      }

      if (this.category && this.allCategories && this.allCategories.length > 0) {
         let parents = [];
         let currentParentId = this.category.parentCategoryId;
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
      }
      
      items.push({ title: this.category ? this.category.name : 'Chuyên mục' })
      return items
    },
    totalPages() {
      return this.totalPagesCount
    },
    paginatedThreads() {
      return this.threads
    }
  },
  async mounted() {
    this.checkAuth()
    await this.fetchData()
  },
  methods: {
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    checkAuth() {
      const user = localStorage.getItem('user')
      this.isLoggedIn = !!user
    },
    goToCreateThread() {
      if (this.category) {
        this.$router.push({ name: 'CreateThread', query: { catId: this.category.id } })
      }
    },
    async fetchData() {
      this.loading = true
      const categoryId = this.$route.params.id
      try {
        // Fetch tất cả chuyên mục để tìm tên chuyên mục hiện tại
        const [catRes, groupRes] = await Promise.all([
          categoryService.getAll(),
          categoryService.getGroups()
        ])
        
        const categories = catRes.data
        this.allCategories = categories
        this.category = categories.find(c => c.id == categoryId)

        if (this.category && this.category.categoryGroupId) {
          this.categoryGroup = groupRes.data.find(g => g.id === this.category.categoryGroupId)
        }

        // Fetch danh sách bài viết trang hiện tại
        await this.fetchThreadsPaged()

        // Fetch last thread for subcategories
        if (this.category && this.category.subCategories) {
          for (const sub of this.category.subCategories) {
            this.fetchLastThread(sub.id)
          }
        }
      } catch (error) {
        console.error('Lỗi khi tải dữ liệu chuyên mục:', error)
      } finally {
        this.loading = false
      }
    },
    async fetchThreadsPaged() {
      const categoryId = this.$route.params.id
      const page = this.currentPage - 1
      const size = this.itemsPerPage
      
      const threadRes = await threadService.getAll({ categoryId, page, size })
      if (threadRes.data && threadRes.data.content) {
        this.threads = threadRes.data.content
        this.totalPagesCount = threadRes.data.totalPages || 1
        this.totalElements = threadRes.data.totalElements || 0
      } else {
        this.threads = []
        this.totalPagesCount = 1
        this.totalElements = 0
      }
    },
    async fetchLastThread(catId) {
      try {
        const res = await threadService.getAll({ categoryId: catId, limit: 1 })
        if (res.data && res.data.length > 0) {
          this.lastThreadByCat = { ...this.lastThreadByCat, [catId]: res.data[0] }
        }
      } catch (e) {
        console.error(e)
      }
    },

    formatDate(dateStr) {
      return formatForumDate(dateStr)
    },
    formatNumber(num) {
      if (!num) return 0
      if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
      if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
      return num
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

.sub-categories-block {
  background-color: #f8f9fa;
  border-bottom: 1px solid #eee;
}

.sub-categories-list {
  display: flex;
  flex-direction: column;
}

.category-row {
  display: flex;
  padding: 12px 15px;
  border-bottom: 1px solid #f0f2f5;
  align-items: center;
  background: white;
  transition: background-color 0.2s;
}

.category-row:last-child {
  border-bottom: none;
}

.category-row:hover {
  background: #f9fbfc;
}

.category-icon {
  width: 40px;
  display: flex;
  align-items: center;
}

.category-info {
  flex: 1;
  min-width: 0;
}

.category-name {
  font-weight: 600;
  color: #1a507a;
  text-decoration: none;
  font-size: 1.05rem;
}

.category-name:hover {
  text-decoration: underline;
}

.cat-desc {
  font-size: 0.8rem;
  color: #888;
  margin-top: 3px;
}

.category-stats {
  display: flex;
  width: 150px;
  text-align: center;
  gap: 15px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 0.7rem;
  text-transform: uppercase;
  color: #999;
}

.stat-value {
  font-size: 0.95rem;
  font-weight: 500;
  margin-top: 2px;
}

.category-last-thread {
  width: 320px;
  padding-left: 15px;
  border-left: 1px solid #eee;
}

.last-thread-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.last-thread-avatar {
  width: 36px;
  height: 36px;
  background: #5c6bc0;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 1rem;
  flex-shrink: 0;
}

.last-thread-info {
  flex: 1;
  min-width: 0;
}

.last-thread-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.95rem;
  font-weight: 500;
  color: #1a507a;
  text-decoration: none;
  margin-bottom: 2px;
  min-width: 0;
}

.last-thread-title .title-txt {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 0 1 auto;
}

.last-thread-title:hover .title-txt {
  text-decoration: underline;
}

.label-tag-mini {
  padding: 1px 5px;
  font-size: 0.7rem;
  border-radius: 3px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  white-space: nowrap;
  line-height: 1.2;
  flex-shrink: 0;
}

.last-thread-meta {
  font-size: 0.8rem;
  color: #888;
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
  display: block;
}

.thread-title a {
  text-decoration: none;
  color: #1a507a;
  font-weight: 500;
  font-size: 1.05rem;
  line-height: 1.5;
  display: inline;
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
