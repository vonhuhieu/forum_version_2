<template>
  <div>
    <Loading :visible="loading || crawling" :text="crawling ? 'Đang quét và cập nhật bản tin mới...' : 'Đang tải bản tin...'" />
    <main class="container" style="padding-top: 2rem; padding-bottom: 3rem;">
      <!-- Breadcrumb -->
      <Breadcrumb :items="breadcrumbItems" />

      <!-- News Hub Header Banner -->
      <div class="news-banner card" style="margin-bottom: 1.5rem; overflow: hidden; border: none; box-shadow: 0 4px 15px rgba(26, 115, 232, 0.08);">
        <div class="news-banner-content">
          <div class="news-badge-wrapper">
            <span class="news-chip">BẢN TIN CHÍNH THỐNG</span>
            <span v-if="stats.todayCrawled !== undefined" class="news-stats-pill">
              Hôm nay: <strong>{{ stats.todayCrawled }}</strong> bản tin
            </span>
          </div>
          <h1 class="news-title">Điểm Tin Thời Sự & Đàm Đạo Tri Thức</h1>
          <p class="news-description">
            Tổng hợp tin tức nóng hổi từ các cơ quan báo chí chính thống uy tín. Cùng đàm đạo, phân tích và chia sẻ góc nhìn văn minh, xây dựng.
          </p>
          <div class="news-disclaimer">
            <span class="disclaimer-icon">ℹ️</span>
            <span>Nội dung được tổng hợp tự động phục vụ mục đích học hỏi và trao đổi học thuật. Bản quyền toàn văn thuộc về đơn vị báo chí phát hành.</span>
          </div>

          <!-- Admin Quick Action -->
          <div v-if="isAdmin" class="news-admin-bar">
            <button 
              class="btn-crawl-now" 
              :disabled="crawling" 
              @click="handleManualCrawl"
            >
              <span v-if="crawling">⏳ Đang quét tin mới...</span>
              <span v-else>⚡ Quét & Cập nhật tin ngay (Bot Crawler)</span>
            </button>
            <span v-if="crawlMessage" class="crawl-msg">{{ crawlMessage }}</span>
          </div>
        </div>
      </div>

      <!-- Quick Filter Tabs -->
      <div class="news-tabs-bar">
        <button 
          v-for="t in tabs" 
          :key="t.key"
          class="news-tab-btn"
          :class="{ active: currentTab === t.key }"
          @click="selectTab(t.key)"
        >
          <span class="tab-icon">{{ t.icon }}</span>
          <span>
            {{ t.label }}
            <span v-if="getTabCount(t.key) !== null" class="tab-count-badge">({{ getTabCount(t.key) }})</span>
          </span>
        </button>
      </div>

      <!-- Thread List Card -->
      <div class="card" style="margin-bottom: 2rem;">
        <div class="card-header" style="display: flex; justify-content: space-between; align-items: center;">
          <span>{{ currentTabLabel }} ({{ totalElements }} bài viết)</span>
        </div>

        <div class="pagination-wrapper" v-if="totalPages > 1" style="padding: 1rem; border-top: 1px solid #eee;">
          <ForumPagination 
            :current-page="currentPage" 
            :total-pages="totalPages" 
            @page-changed="handlePageChange"
          />
        </div>

        <div v-if="threads.length === 0 && !loading" style="padding: 3rem; text-align: center; color: #888;">
          <p style="font-size: 1.1rem; margin-bottom: 0.5rem;">Chưa có bản tin nào trong danh mục này.</p>
          <p style="font-size: 0.9rem;">Hệ thống Bot sẽ tự động cập nhật bản tin mới vào các khung giờ 7h, 12h và 18h hàng ngày.</p>
        </div>

        <div v-else class="thread-list">
          <div 
            v-for="thread in threads" 
            :key="thread.id" 
            class="thread-row thread-row-center min-height-100-on-pc"
            @click="goToThread($event, thread)"
          >
            <!-- Author Avatar -->
            <user-profile-popup :user="thread.author" v-if="thread.author">
              <div class="thread-avatar" :style="!isAvatarUrl(thread.author?.avatar) ? { backgroundColor: thread.author?.avatar || '#1a73e8', color: '#fff' } : {}">
                <img v-if="isAvatarUrl(thread.author?.avatar)" :src="formatAvatarUrl(thread.author?.avatar)" alt="Avatar" />
                <template v-else>🤖</template>
              </div>
            </user-profile-popup>
            <div v-else class="thread-avatar" style="background-color: #1a73e8; color: #fff;">🤖</div>

            <!-- Thread Main -->
            <div class="thread-main">
              <div class="thread-title">
                <span 
                  v-if="thread.label" 
                  class="label-tag" 
                  :style="{ backgroundColor: thread.label.colorCode, color: thread.label.textColor, borderColor: thread.label.borderColor || 'transparent' }"
                >
                  {{ thread.label.name }}
                </span>
                <router-link :to="{ name: 'ThreadDetail', params: { id: thread.id } }">
                  {{ thread.title }}
                </router-link>
              </div>

              <div class="thread-meta">
                <span class="author-name white-space-nowrap">
                  <user-profile-popup :user="thread.author" v-if="thread.author">
                    <span class="cursor-pointer font-weight-bold" style="color: #1a73e8;">
                      {{ thread.author.displayName || thread.author.username }} 
                      <VerifiedBadge :user="thread.author" size="14px" />
                    </span>
                  </user-profile-popup>
                  <span v-else>Điểm Tin Bot 🤖</span>
                </span>
                <span class="dot-divider">•</span>
                <span v-if="thread.category" class="news-cat-badge">
                  {{ thread.category.name }}
                </span>
                <span v-if="thread.category" class="dot-divider">•</span>
                <span class="meta-link">{{ formatDate(thread.createdAt) }}</span>
              </div>

              <!-- Mobile Stats -->
              <div class="thread-stats-mobile">
                Trả lời: {{ thread.replyCount || 0 }} <span class="dot-divider">•</span> Xem: {{ thread.viewCount || 0 }}
              </div>
            </div>

            <!-- Desktop Stats -->
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

            <!-- Last Post Info -->
            <div class="thread-last-post">
              <div class="last-post-info">
                <span class="last-post-date">{{ formatDate(thread.lastPostAt || thread.createdAt) }}</span>
                <div class="last-post-author">
                  <span v-if="thread.lastPostAuthor">{{ thread.lastPostAuthor.displayName || thread.lastPostAuthor.username }}</span>
                  <span v-else>{{ thread.author?.displayName || 'Điểm Tin Bot' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="pagination-wrapper" v-if="totalPages > 1" style="padding: 1rem; border-top: 1px solid #eee;">
          <ForumPagination 
            :current-page="currentPage" 
            :total-pages="totalPages" 
            @page-changed="handlePageChange"
          />
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import UserProfilePopup from '@/shared/components/UserProfilePopup.vue'
import VerifiedBadge from '@/shared/components/VerifiedBadge.vue'
import Loading from '@/shared/components/Loading.vue'
import newsService from '@/apps/Forum/services/news.service'
import { formatForumDate } from '@/shared/utils/date'
import authService from '@/apps/Auth/services/auth.service'
import { ROLES, NEWS_TABS } from '@/shared/utils/constants'

export default {
  name: 'NewsFeedView',
  components: {
    Breadcrumb,
    ForumPagination,
    UserProfilePopup,
    VerifiedBadge,
    Loading
  },
  data() {
    return {
      threads: [],
      currentPage: 1,
      totalPages: 1,
      totalElements: 0,
      pageSize: 15,
      loading: false,
      crawling: false,
      crawlMessage: '',
      currentTab: NEWS_TABS.ALL,
      stats: {
        todayCrawled: 0,
        totalCrawled: 0,
        enabled: true
      },
      tabs: [
        { key: NEWS_TABS.ALL, label: 'Tất cả bản tin', icon: '🌐' },
        { key: NEWS_TABS.TECH, label: 'Công nghệ & AI', icon: '💻' },
        { key: NEWS_TABS.FINANCE, label: 'Kinh tế & Đầu tư', icon: '📈' },
        { key: NEWS_TABS.SOCIETY, label: 'Đời sống & Xã hội', icon: '☕' }
      ]
    }
  },
  computed: {
    breadcrumbItems() {
      return [
        { title: 'Điểm tin' }
      ]
    },
    currentTabLabel() {
      const found = this.tabs.find(t => t.key === this.currentTab)
      return found ? found.label : 'Bản tin'
    },
    isAdmin() {
      const user = authService.getCurrentUser()
      if (!user || !user.roles) return false
      return user.roles.includes(ROLES.ADMIN) || user.roles.includes(ROLES.SUPER_ADMIN)
    }
  },
  watch: {
    '$route.query.tab'(newTab) {
      if (newTab && newTab !== this.currentTab) {
        this.currentTab = newTab
        this.currentPage = 1
        this.fetchNews()
      }
    }
  },
  created() {
    if (this.$route.query.tab) {
      this.currentTab = this.$route.query.tab
    }
    this.fetchNews()
    this.fetchStats()
  },
  methods: {
    formatDate: formatForumDate,
    async fetchNews() {
      this.loading = true
      try {
        const response = await newsService.getNewsThreads({
          tab: this.currentTab,
          page: this.currentPage - 1,
          size: this.pageSize
        })
        const pageData = response.data?.data !== undefined ? response.data.data : response.data
        if (pageData) {
          this.threads = pageData.content || []
          this.totalPages = pageData.totalPages || 1
          this.totalElements = pageData.totalElements || 0
        }
      } catch (err) {
        console.error('Lỗi khi tải bản tin:', err)
      } finally {
        this.loading = false
      }
    },
    async fetchStats() {
      try {
        const response = await newsService.getNewsStats()
        const statsData = response.data?.data !== undefined ? response.data.data : response.data
        if (statsData) {
          this.stats = statsData
        }
      } catch (err) {
        // ignore
      }
    },
    getTabCount(key) {
      if (this.stats && this.stats.tabCounts && this.stats.tabCounts[key] !== undefined) {
        return this.stats.tabCounts[key]
      }
      return null
    },
    selectTab(key) {
      if (this.currentTab === key) return
      this.currentTab = key
      this.currentPage = 1
      this.$router.replace({ query: { ...this.$route.query, tab: key } })
      this.fetchNews()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchNews()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    goToThread(event, thread) {
      if (event.target.closest('a') || event.target.closest('.user-profile-popup')) {
        return
      }
      this.$router.push({ name: 'ThreadDetail', params: { id: thread.id } })
    },
    isAvatarUrl(avatar) {
      return avatar && (avatar.startsWith('http') || avatar.startsWith('/uploads'))
    },
    formatAvatarUrl(avatar) {
      if (!avatar) return ''
      if (avatar.startsWith('http')) return avatar
      return avatar
    },
    async handleManualCrawl() {
      if (this.crawling) return
      this.crawling = true
      this.crawlMessage = ''
      try {
        const res = await newsService.triggerManualCrawl()
        const crawlData = res.data?.data !== undefined ? res.data.data : res.data
        if (crawlData) {
          this.crawlMessage = crawlData.message || 'Cào tin thành công!'
          await this.fetchNews()
          await this.fetchStats()
        }
      } catch (err) {
        this.crawlMessage = 'Lỗi khi kích hoạt cào tin.'
      } finally {
        this.crawling = false
      }
    }
  }
}
</script>

<style scoped>
.news-banner {
  background: linear-gradient(135deg, #1e3a8a 0%, #1a73e8 60%, #38bdf8 100%);
  color: #ffffff;
  padding: 2rem 2.5rem;
  border-radius: 12px;
}

.news-badge-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.news-chip {
  background-color: rgba(255, 255, 255, 0.22);
  color: #ffffff;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.news-stats-pill {
  background-color: rgba(0, 0, 0, 0.2);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.8rem;
}

.news-title {
  font-size: 1.85rem;
  font-weight: 800;
  margin: 0 0 10px 0;
  letter-spacing: -0.5px;
}

.news-description {
  font-size: 0.98rem;
  line-height: 1.6;
  opacity: 0.92;
  margin: 0 0 16px 0;
  max-width: 820px;
}

.news-disclaimer {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: rgba(255, 255, 255, 0.12);
  border-radius: 6px;
  padding: 8px 14px;
  font-size: 0.82rem;
  opacity: 0.9;
}

.news-admin-bar {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.btn-crawl-now {
  background-color: #ffffff;
  color: #1a73e8;
  border: none;
  font-weight: 700;
  font-size: 0.88rem;
  padding: 8px 18px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.btn-crawl-now:hover:not(:disabled) {
  background-color: #f1f5f9;
  transform: translateY(-1px);
}

.btn-crawl-now:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.crawl-msg {
  font-size: 0.88rem;
  background: rgba(0, 0, 0, 0.25);
  padding: 4px 10px;
  border-radius: 4px;
}

.news-tabs-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 1.25rem;
  overflow-x: auto;
  padding-bottom: 4px;
}

.news-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.92rem;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.news-tab-btn:hover {
  background: #f8fafc;
  color: #1a73e8;
  border-color: #cbd5e1;
}

.news-tab-btn.active {
  background: #1a73e8;
  color: #ffffff;
  border-color: #1a73e8;
  box-shadow: 0 3px 10px rgba(26, 115, 232, 0.25);
}

.tab-count-badge {
  font-size: 0.82rem;
  opacity: 0.85;
  font-weight: 500;
  margin-left: 3px;
}

.news-tab-btn.active .tab-count-badge {
  opacity: 0.95;
  font-weight: 700;
}

.news-cat-badge {
  color: #64748b;
  font-size: 0.82rem;
  background-color: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .news-banner {
    padding: 1.5rem;
  }
  .news-title {
    font-size: 1.4rem;
  }
  .news-description {
    font-size: 0.9rem;
  }
}
</style>
