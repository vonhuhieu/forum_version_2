<template>
  <div class="forum-home">
    <!-- Section 1: Mới ra lò -->
    <section id="moi-ra-lo" class="forum-section card">
      <div class="card-header section-header">
        <a @click="$router.push({ name: 'LatestThreads' })" class="header-link">Mới ra lò</a>
      </div>
      
      <div class="thread-list">
        <div v-for="thread in latestThreads" :key="thread.id" class="thread-row home-thread-row thread-row-center pt-and-pb-10-and-pl-and-pr-8">
          <user-profile-popup :user="thread.author" v-if="thread.author">
            <div class="thread-avatar" :style="!isAvatarUrl(thread.author.avatar) ? { backgroundColor: thread.author.avatar || '#ccc', color: '#fff' } : {}">
              <img v-if="isAvatarUrl(thread.author.avatar)" :src="thread.author.avatar" />
              <template v-else>
                {{ (thread.author.displayName || thread.author.username).charAt(0).toUpperCase() }}
              </template>
            </div>
          </user-profile-popup>
          <div v-else class="thread-avatar" style="background-color: #ccc; color: #fff;">A</div>
          <div class="thread-main">
            <div class="thread-title-wrapper">
              <span v-if="thread.label" class="label-tag" :style="{ backgroundColor: thread.label.colorCode, color: thread.label.textColor, borderColor: thread.label.borderColor || 'transparent' }">
                {{ thread.label.name }}
              </span>
              <router-link :to="{ name: 'ThreadDetail', params: { id: thread.id } }" class="thread-title">
                {{ thread.title }}
              </router-link>
              <span v-if="thread.pinned" title="Đã ghim" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 6px;">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="icon-pin" style="display: block; pointer-events: none;"><line x1="12" y1="17" x2="12" y2="22"></line><path d="M5 17h14v-1.76a2 2 0 0 0-.44-1.24l-2.78-3.5A2 2 0 0 1 15 9.26V5a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4.26a2 2 0 0 1-.78 1.24l-2.78 3.5a2 2 0 0 0-.44 1.24z"></path></svg>
              </span>
              <span v-if="thread.locked" title="Đã khóa" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 6px;">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-lock" style="display: block; pointer-events: none;"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
              </span>
            </div>
            <div class="thread-meta desktop-only">
              <span class="author-name white-space-nowrap">{{ thread.author ? (thread.author.displayName || thread.author.username) : 'Ẩn danh' }}</span>
              <span class="dot-divider">•</span>
              <router-link :to="{ name: 'ThreadDetail', params: { id: thread.id } }" class="meta-link white-space-nowrap">{{ formatDate(thread.createdAt) }}</router-link>
              <span v-if="thread.category" class="dot-divider home-category-dot">•</span>
              <router-link v-if="thread.category" :to="{ name: 'CategoryDetail', params: { id: thread.category.id } }" class="meta-link meta-category home-category-link white-space-nowrap">
                {{ thread.category.name }}
              </router-link>
            </div>
            
            <div class="home-quick-pages-wrapper desktop-only" v-if="getThreadPages(thread.replyCount).length > 0">
              <span class="home-quick-pages">
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
            <div class="thread-meta-mobile mobile-only">
              <div class="thread-meta-row-2">
                <span class="author-name">{{ thread.author ? (thread.author.displayName || thread.author.username) : 'Ẩn danh' }}</span>
                <span v-if="thread.category" class="dot-divider">·</span>
                <router-link v-if="thread.category" :to="{ name: 'CategoryDetail', params: { id: thread.category.id } }" class="meta-link meta-category">
                  {{ thread.category.name }}
                </router-link>
              </div>
              <div class="thread-meta-row-3">
                <span>Trả lời: {{ formatNumber(thread.replyCount) }}</span>
                <span class="dot-divider">·</span>
                <span>{{ formatDate(thread.lastPostAt || thread.createdAt) }}</span>
              </div>
            </div>
          </div>
          <div class="thread-stats">
            <div class="stat-block">
              <span class="stat-label">Trả lời:</span>
              <span class="stat-value">{{ formatNumber(thread.replyCount) }}</span>
            </div>
            <div class="stat-block">
              <span class="stat-label">Xem:</span>
              <span class="stat-value">{{ formatNumber(thread.viewCount) }}</span>
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
            <user-profile-popup :user="thread.lastPostAuthor || thread.author" v-if="thread.lastPostAuthor || thread.author">
              <div class="last-post-avatar" :style="!isAvatarUrl((thread.lastPostAuthor || thread.author)?.avatar) ? { backgroundColor: (thread.lastPostAuthor || thread.author)?.avatar || '#ccc', color: '#fff' } : {}">
                <img v-if="isAvatarUrl((thread.lastPostAuthor || thread.author)?.avatar)" :src="(thread.lastPostAuthor || thread.author)?.avatar" />
                <template v-else>
                  {{ ((thread.lastPostAuthor || thread.author)?.displayName || (thread.lastPostAuthor || thread.author)?.username || 'A').charAt(0).toUpperCase() }}
                </template>
              </div>
            </user-profile-popup>
            <div v-else class="last-post-avatar" style="background-color: #ccc; color: #fff;">A</div>
          </div>
        </div>
      </div>
      
      <div class="card-footer">
        <button class="btn-view-more" @click="$router.push({ name: 'LatestThreads' })">Xem thêm...</button>
      </div>
    </section>

    <!-- Grouped Sections -->
    <section v-for="group in activeGroups" :key="group.id" :id="'group-' + group.id" class="forum-section card">
      <div class="card-header section-header group-header background-f8f9fa">
        <a :href="'#group-' + group.id" class="header-link">{{ group.name }}</a>
      </div>
      
      <div class="category-list">
        <div v-for="cat in group.categories.filter(c => !c.parentCategoryId)" :key="cat.id" class="category-row home-category-row">
          <div class="category-icon home-category-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-msg"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
          </div>
          <div class="category-info home-category-info">
            <div class="cat-name-row">
              <router-link :to="{ name: 'CategoryDetail', params: { id: cat.id } }" class="category-name">
                {{ cat.name }}
              </router-link>
            </div>
            
            <div v-if="cat.subCategories && cat.subCategories.length > 0" class="sub-categories-trigger">
              <span class="sub-trigger-text">Chuyên mục con</span>
              <span class="arrow">▼</span>
              
              <div class="sub-categories-dropdown">
                <div class="dropdown-arrow-up"></div>
                <div class="dropdown-header">Chuyên mục con</div>
                <div class="dropdown-body">
                  <router-link v-for="sub in cat.subCategories" :key="sub.id" :to="{ name: 'CategoryDetail', params: { id: sub.id } }" class="sub-item">
                    <span class="sub-icon">💬</span>
                    {{ sub.name }}
                  </router-link>
                </div>
              </div>
            </div>
          </div>
          <div class="category-stats home-category-stats">
            <div class="stat-item">
              <span class="stat-label">Chủ đề</span>
              <span class="stat-value">{{ formatNumber(cat.threadCount || 0) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">Bài viết</span>
              <span class="stat-value">{{ formatNumber(cat.postCount || 0) }}</span>
            </div>
          </div>
          <div class="category-last-thread home-category-last-thread">
            <div v-if="lastThreadByCat[cat.id]" class="last-thread-box home-last-thread-box">
              <div class="last-thread-avatar" :style="!isAvatarUrl((lastThreadByCat[cat.id].lastPostAuthor || lastThreadByCat[cat.id].author)?.avatar) ? { backgroundColor: (lastThreadByCat[cat.id].lastPostAuthor || lastThreadByCat[cat.id].author)?.avatar || '#ccc', color: '#fff' } : {}">
                <img v-if="isAvatarUrl((lastThreadByCat[cat.id].lastPostAuthor || lastThreadByCat[cat.id].author)?.avatar)" :src="(lastThreadByCat[cat.id].lastPostAuthor || lastThreadByCat[cat.id].author)?.avatar" />
                <template v-else>
                  {{ ((lastThreadByCat[cat.id].lastPostAuthor || lastThreadByCat[cat.id].author)?.displayName || (lastThreadByCat[cat.id].lastPostAuthor || lastThreadByCat[cat.id].author)?.username || 'A').charAt(0).toUpperCase() }}
                </template>
              </div>
              <div class="last-thread-info home-last-thread-info">
                <router-link :to="{ name: 'ThreadDetail', params: { id: lastThreadByCat[cat.id].id } }" class="last-thread-title home-last-thread-title">
                  <span v-if="lastThreadByCat[cat.id].label" class="label-tag-mini" :style="{ backgroundColor: lastThreadByCat[cat.id].label.colorCode, color: lastThreadByCat[cat.id].label.textColor, borderColor: lastThreadByCat[cat.id].label.borderColor || 'transparent' }">
                    {{ lastThreadByCat[cat.id].label.name }}
                  </span>
                  <span class="title-txt home-last-thread-title-txt">{{ lastThreadByCat[cat.id].title }}</span>
                  <span v-if="lastThreadByCat[cat.id].pinned" title="Đã ghim" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 4px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="icon-pin" style="display: block; pointer-events: none;"><line x1="12" y1="17" x2="12" y2="22"></line><path d="M5 17h14v-1.76a2 2 0 0 0-.44-1.24l-2.78-3.5A2 2 0 0 1 15 9.26V5a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4.26a2 2 0 0 1-.78 1.24l-2.78 3.5a2 2 0 0 0-.44 1.24z"></path></svg>
                  </span>
                  <span v-if="lastThreadByCat[cat.id].locked" title="Đã khóa" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 4px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: block; pointer-events: none;"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                  </span>
                </router-link>
                <div class="last-thread-meta home-last-thread-meta display-flex-on-tablet-and-pc">
                  <span class="last-post-time">{{ formatDate(lastThreadByCat[cat.id].lastPostAt || lastThreadByCat[cat.id].createdAt) }}</span>
                  <span class="dot home-last-post-dot">•</span>
                  <span class="author home-last-post-author">{{ (lastThreadByCat[cat.id].lastPostAuthor || lastThreadByCat[cat.id].author)?.displayName || (lastThreadByCat[cat.id].lastPostAuthor || lastThreadByCat[cat.id].author)?.username || 'Ẩn danh' }}</span>
                </div>
              </div>
            </div>
            <div v-else class="no-thread home-no-thread">Chưa có bài viết</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Block "Con sò mới" cho Mobile (width < 768px) -->
    <div class="card mobile-con-so-block">
      <div class="card-header section-header">
        <a @click="$router.push({ name: 'LatestThreads' })" class="header-link">Con sò mới</a>
      </div>
      <div class="card-body" style="padding: 0;">
        <div v-if="loading" style="padding: 1rem; text-align: center; color: #666; font-size: 0.9rem;">
          Đang tải...
        </div>
        <div v-else class="latest-threads-list">
          <div v-for="thread in latestThreads.slice(0, 15)" :key="thread.id" class="latest-thread-item">
            <div class="lt-avatar" :style="!isAvatarUrl((thread.lastPostAuthor || thread.author)?.avatar) ? { backgroundColor: (thread.lastPostAuthor || thread.author)?.avatar || '#e0e0e0', color: '#fff' } : {}">
              <img v-if="isAvatarUrl((thread.lastPostAuthor || thread.author)?.avatar)" :src="(thread.lastPostAuthor || thread.author)?.avatar" />
              <template v-else>
                {{ ((thread.lastPostAuthor || thread.author)?.displayName || (thread.lastPostAuthor || thread.author)?.username || 'A').charAt(0).toUpperCase() }}
              </template>
            </div>
            <div class="lt-content">
              <div class="lt-title">
                <router-link :to="{ name: 'ThreadDetail', params: { id: thread.id } }" :title="thread.title">
                  <span v-if="thread.label" class="label-tag-mini" :style="{ backgroundColor: thread.label.colorCode, color: thread.label.textColor, borderColor: thread.label.borderColor || 'transparent' }">
                    {{ thread.label.name }}
                  </span>
                  <span class="lt-title-text">{{ thread.title }}</span>
                  <span v-if="thread.pinned" title="Đã ghim" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 4px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="icon-pin" style="display: block; pointer-events: none;"><line x1="12" y1="17" x2="12" y2="22"></line><path d="M5 17h14v-1.76a2 2 0 0 0-.44-1.24l-2.78-3.5A2 2 0 0 1 15 9.26V5a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4.26a2 2 0 0 1-.78 1.24l-2.78 3.5a2 2 0 0 0-.44 1.24z"></path></svg>
                  </span>
                  <span v-if="thread.locked" title="Đã khóa" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 4px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: block; pointer-events: none;"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                  </span>
                </router-link>
              </div>
              <div class="lt-meta">
                Mới nhất: {{ (thread.lastPostAuthor || thread.author)?.displayName || (thread.lastPostAuthor || thread.author)?.username || 'Ẩn danh' }} &middot; {{ formatDate(thread.lastPostAt || thread.createdAt) }}
              </div>
              <div class="lt-category">
                <router-link :to="{ name: 'CategoryDetail', params: { id: thread.category?.id } }">{{ thread.category?.name || 'Không rõ' }}</router-link>
              </div>
            </div>
          </div>
          <div v-if="latestThreads.length === 0" style="padding: 1rem; text-align: center; color: #999; font-size: 0.9rem;">
            Chưa có bài viết nào.
          </div>
        </div>
      </div>
    </div>

    <!-- Responsive blocks for mobile and tablet -->
    <div class="responsive-stats-blocks">
      <!-- Vô công rỗi nghề -->
      <div class="card responsive-vo-cong-block">
        <div class="card-header background-f8f9fa text-transform-uppercase color-1a507a pl-and-pr-16">Vô công rỗi nghề</div>
        <div class="card-body" style="padding: 1rem;">
          <div class="stats-row">
            <span>Người có học:</span>
            <strong>{{ formatCommaNumber(stats.officialMembers) }} thằng</strong>
          </div>
          <div class="stats-row">
            <span>Bọn ất ơ:</span>
            <strong>{{ formatCommaNumber(stats.unofficialMembers) }} thằng</strong>
          </div>
          <div class="stats-row">
            <span>Tổng cộng:</span>
            <strong>{{ formatCommaNumber(stats.totalOfficialAndUnofficial) }} thằng</strong>
          </div>
        </div>
      </div>

      <!-- Thống kê diễn đàn -->
      <div class="card responsive-thong-ke-block">
        <div class="card-header background-f8f9fa text-transform-uppercase color-1a507a pl-and-pr-16">Thống kê diễn đàn</div>
        <div class="card-body" style="padding: 1rem;">
          <div class="stats-row">
            <span>Chuyên mục:</span>
            <strong>{{ formatCommaNumber(stats.totalCategories) }}</strong>
          </div>
          <div class="stats-row">
            <span>Bài viết:</span>
            <strong>{{ formatCommaNumber(stats.totalPosts) }}</strong>
          </div>
          <div class="stats-row">
            <span>Thành viên:</span>
            <strong>{{ formatCommaNumber(stats.totalMembers) }}</strong>
          </div>
          <div class="stats-row">
            <span>Thành viên mới nhất:</span>
            <strong style="color: #1a507a;">{{ stats.latestMember }}</strong>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import threadService from '@/apps/Forum/services/thread.service'
import categoryService from '@/apps/Forum/services/category.service'
import { formatForumDate } from '@/shared/utils/date'
import { isAvatarUrl } from '@/shared/utils/utils'
import UserProfilePopup from '@/shared/components/UserProfilePopup.vue'

export default {
  name: 'ForumHome',
  emits: ['loaded'],
  props: {
    stats: {
      type: Object,
      default: () => ({
        totalCategories: 0,
        totalThreads: 0,
        totalPosts: 0,
        totalMembers: 0,
        latestMember: '',
        officialMembers: 0,
        unofficialMembers: 0,
        totalOfficialAndUnofficial: 0
      })
    },
    latestThreads: {
      type: Array,
      default: () => []
    }
  },
  components: {
    UserProfilePopup
  },
  data() {
    return {
      categoryGroups: [],
      lastThreadByCat: {},
      loading: true
    }
  },
  computed: {
    activeGroups() {
      if (!this.categoryGroups || !Array.isArray(this.categoryGroups)) return []
      return this.categoryGroups.filter(g => g.active && g.categories && g.categories.length > 0)
    }
  },
  watch: {
    '$route.hash': {
      handler(newHash) {
        if (newHash) {
          this.scrollToHash(newHash)
        }
      }
    }
  },
  async mounted() {
    await this.fetchData()
    if (this.$route.hash) {
      this.scrollToHash(this.$route.hash)
    }
    window.addEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  beforeUnmount() {
    window.removeEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        // Fetch Groups with nested categories
        const groupRes = await categoryService.getGroups()
        this.categoryGroups = groupRes.data || []

        // Fetch last thread for each category (this could be optimized in backend)
        for (const group of this.categoryGroups) {
          for (const cat of group.categories) {
            this.fetchLastThread(cat.id)
          }
        }
      } catch (error) {
        console.error('Lỗi khi tải dữ liệu trang chủ:', error)
      } finally {
        this.loading = false
        this.$emit('loaded')
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
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    handleAvatarUpdated(event) {
      const { username, avatar } = event.detail
      const updatedLastThreadByCat = { ...this.lastThreadByCat }
      Object.keys(updatedLastThreadByCat).forEach(catId => {
        const thread = updatedLastThreadByCat[catId]
        if (thread) {
          const updatedThread = { ...thread }
          let changed = false
          if (thread.author && thread.author.username === username) {
            updatedThread.author = { ...thread.author, avatar }
            changed = true
          }
          if (thread.lastPostAuthor && thread.lastPostAuthor.username === username) {
            updatedThread.lastPostAuthor = { ...thread.lastPostAuthor, avatar }
            changed = true
          }
          if (changed) {
            updatedLastThreadByCat[catId] = updatedThread
          }
        }
      })
      this.lastThreadByCat = updatedLastThreadByCat
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
    formatCommaNumber(num) {
      if (!num) return 0
      return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",")
    },
    scrollToHash(hash) {
      this.$nextTick(() => {
        const id = hash.replace('#', '')
        const element = document.getElementById(id)
        if (element) {
          element.scrollIntoView({ behavior: 'auto' })
        }
      })
    },
    getThreadPages(replyCount) {
      const itemsPerPage = 10; // As defined in ThreadDetail.vue
      const totalItems = 1 + (replyCount || 0);
      const totalPages = Math.ceil(totalItems / itemsPerPage);
      
      if (totalPages <= 1) return [];
      if (totalPages === 2) return [2];
      if (totalPages === 3) return [2, 3];
      
      // Last 3 pages
      return [totalPages - 2, totalPages - 1, totalPages];
    }
  }
}
</script>

<style scoped>
@media (min-width: 1025px) {
  .responsive-stats-blocks {
    display: none;
  }
}

.stats-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-direction: row;
  margin-bottom: 0.5rem;
}

.stats-row:last-child {
  margin-bottom: 0;
}

.header-link {
  font-size: 1.1rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  text-decoration: none;
  color: #1a507a;
  cursor: pointer;
  display: inline-block;
}

.header-link:hover {
  text-decoration: underline;
}

.section-header {
  background: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
  color: #1a507a;
  padding: 10px 15px;
}

.group-header {
  background: #ebf2f7;
}

.card-footer {
  padding: 10px;
  text-align: center;
  border-top: 1px solid #eee;
}

.btn-view-more {
  background: #1a507a;
  color: white;
  border: none;
  padding: 6px 15px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
}

/* Category Row Styles */
.category-row {
  display: flex;
  padding: 12px 15px;
  border-bottom: 1px solid #f0f2f5;
  align-items: center;
}

.category-icon {
  width: 40px;
  color: #f39c12;
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

.sub-categories-trigger {
  font-size: 0.85rem;
  color: #999;
  cursor: pointer;
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
  padding: 2px 4px;
  transition: all 0.2s;
}

.sub-trigger-text {
  font-weight: 400;
}

.sub-categories-trigger:hover {
  color: #1a507a;
}

.sub-categories-trigger:hover .sub-categories-dropdown {
  display: block;
}

.sub-categories-dropdown {
  display: none;
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  background: white;
  border: 1px solid #3498db;
  box-shadow: 0 5px 15px rgba(0,0,0,0.15);
  min-width: 220px;
  z-index: 100;
  border-radius: 4px;
  animation: fadeIn 0.2s ease;
}

/* Cầu nối để không bị mất hover khi di chuột xuống */
.sub-categories-dropdown::before {
  content: '';
  position: absolute;
  top: -15px;
  left: 0;
  right: 0;
  height: 15px;
  background: transparent;
}

.dropdown-arrow-up {
  position: absolute;
  top: -8px;
  left: 20px;
  width: 0;
  height: 0;
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-bottom: 8px solid #3498db;
}

.dropdown-header {
  background: #ebf5fb;
  padding: 8px 12px;
  color: #2980b9;
  font-size: 0.9rem;
  font-weight: 600;
  border-bottom: 1px solid #d4e6f1;
  border-radius: 4px 4px 0 0;
}

.dropdown-body {
  padding: 5px 0;
}

.sub-item {
  display: flex;
  align-items: center;
  padding: 8px 15px;
  color: #444;
  text-decoration: none;
  font-size: 0.9rem;
  transition: background 0.2s;
  gap: 8px;
}

.sub-item:hover {
  background: #f8f9fa;
  color: #1a507a;
}

.sub-icon {
  font-size: 0.8rem;
  color: #f39c12;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

.category-name {
  font-weight: 700;
  color: #1a507a;
  text-decoration: none;
  font-size: 1.1rem;
  display: block;
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

.thread-title-wrapper {
  margin-bottom: 4px;
  display: block;
}

.thread-title {
  font-weight: 500;
  font-size: 1.05rem;
  color: #1a507a;
  text-decoration: none;
  margin-bottom: 0;
  line-height: 1.5;
  display: inline;
}

.thread-title:hover {
  text-decoration: underline;
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

.meta-category {
  color: #666;
}

.home-thread-row {
  align-items: flex-start;
}

.home-quick-pages-wrapper {
  margin-top: 4px;
}

.home-quick-pages {
  display: inline-flex;
  gap: 4px;
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
  align-items: center;
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
}

@media (min-width: 768px) {
  .display-flex-on-tablet-and-pc {
    display: flex !important;
  }
}

@import "@/shared/assets/styles/custom.css";
@import "@/shared/assets/styles/responsive/mobile/forum_home/block_moi_ra_lo.css";
@import "@/shared/assets/styles/responsive/mobile/forum_home/block_nhom_chuyen_muc.css";
@import "@/shared/assets/styles/responsive/mobile/forum_home/block_con_so_moi.css";
@import "@/shared/assets/styles/responsive/tablet/forum_home/block_con_so_moi.css";
@import "@/shared/assets/styles/responsive/mobile/forum_home/block_vo_cong_roi_nghe.css";
@import "@/shared/assets/styles/responsive/mobile/forum_home/block_thong_ke_dien_dan.css";
@import "@/shared/assets/styles/responsive/tablet/forum_home/block_vo_cong_roi_nghe.css";
@import "@/shared/assets/styles/responsive/tablet/forum_home/block_thong_ke_dien_dan.css";
</style>
