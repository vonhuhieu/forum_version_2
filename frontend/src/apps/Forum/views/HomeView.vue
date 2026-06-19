<template>
  <div>
    <Loading :visible="isLoading" />

    <main class="container">
      <div class="banner-box" style="margin-top: 1rem;">
        <img src="/675456323_122106804740812631_4737388993277477397_n.jpg" alt="HTXHS Banner">
      </div>

      <div class="home-action-bar container" style="display: flex; justify-content: space-between; align-items: center; padding: 0;">
        <div class="forum-slogan" style="font-weight: bold; color: #1a507a; font-size: 1.1rem;">
          HỢP TÁC XÃ SINH LÝ
        </div>
        <div v-if="isLoggedIn && !isNonOfficial" class="user-actions">
          <button @click="openPostModal" class="btn-post-thread">Đăng bài...</button>
        </div>
      </div>

      <div class="main-wrapper">
        <div class="content-left">
          <ForumHome :stats="stats" :latestThreads="latestThreads" @loaded="onForumHomeLoaded" />
        </div>
        
        <aside class="content-right">
          <div class="card">
            <div class="card-header section-header background-f8f9fa pl-and-pr-16">
              <a @click="$router.push({ name: 'LatestThreads' })" class="header-link text-transform-uppercase color-1a507a">Con sò mới</a>
            </div>
            <div class="card-body" style="padding: 0;">
              <div v-if="loadingLatest" style="padding: 1rem; text-align: center; color: #666; font-size: 0.9rem;">
                Đang tải...
              </div>
              <div v-else class="latest-threads-list">
                <div v-for="thread in latestThreads" :key="thread.id" class="latest-thread-item">
                  <div class="lt-avatar" :style="{ backgroundColor: (thread.lastPostAuthor || thread.author)?.avatar || '#e0e0e0', color: '#fff' }">
                    {{ ((thread.lastPostAuthor || thread.author)?.displayName || (thread.lastPostAuthor || thread.author)?.username || 'A').charAt(0).toUpperCase() }}
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
          <div class="card">
            <div class="card-header background-f8f9fa text-transform-uppercase color-1a507a pl-and-pr-16">Vô công rỗi nghề</div>
            <div class="card-body" style="padding: 1rem;">
              <div class="stat-item" style="display: flex; justify-content: space-between; margin-bottom: 0.5rem;">
                <span>Người có học:</span>
                <strong>{{ formatNumber(stats.officialMembers) }} thằng</strong>
              </div>
              <div class="stat-item" style="display: flex; justify-content: space-between; margin-bottom: 0.5rem;">
                <span>Bọn ất ơ:</span>
                <strong>{{ formatNumber(stats.unofficialMembers) }} thằng</strong>
              </div>
              <div class="stat-item" style="display: flex; justify-content: space-between;">
                <span>Tổng cộng:</span>
                <strong>{{ formatNumber(stats.totalOfficialAndUnofficial) }} thằng</strong>
              </div>
            </div>
          </div>
          <div class="card">
            <div class="card-header background-f8f9fa text-transform-uppercase color-1a507a pl-and-pr-16">Thống kê diễn đàn</div>
            <div class="card-body" style="padding: 1rem;">
              <div class="stat-item" style="display: flex; justify-content: space-between; margin-bottom: 0.5rem;">
                <span>Chuyên mục:</span>
                <strong>{{ formatNumber(stats.totalCategories) }}</strong>
              </div>
              <div class="stat-item" style="display: flex; justify-content: space-between; margin-bottom: 0.5rem;">
                <span>Bài viết:</span>
                <strong>{{ formatNumber(stats.totalPosts) }}</strong>
              </div>
              <div class="stat-item" style="display: flex; justify-content: space-between; margin-bottom: 0.5rem;">
                <span>Thành viên:</span>
                <strong>{{ formatNumber(stats.totalMembers) }}</strong>
              </div>
              <div class="stat-item" style="display: flex; justify-content: space-between;">
                <span>Thành viên mới nhất:</span>
                <strong style="color: #1a507a;">{{ stats.latestMember }}</strong>
              </div>
            </div>
          </div>
          <!-- <div class="banner-right" style="margin-top: 1rem;">
            <img src="/banner_block_phai.jpg" alt="Banner" style="width: 100%; border-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);" />
          </div>
          <div class="banner-right" style="margin-top: 1rem;">
            <img src="/photo_2026-05-09_10-54-26.jpg" alt="Banner" style="width: 100%; border-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);" />
          </div>
          <div class="banner-right" style="margin-top: 1rem;">
            <img src="/gop-y-phan-anh.png" alt="Banner" style="width: 100%; border-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);" />
          </div> -->
        </aside>
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
                <!-- Parent -->
                <div 
                  class="modal-category-item"
                  @click="selectCategory(cat.id)"
                >
                  <div class="modal-cat-info">
                    <div class="modal-cat-name">{{ cat.name }}</div>
                    <div v-if="cat.description" class="modal-cat-desc">{{ cat.description }}</div>
                  </div>
                  <div class="modal-cat-stats">
                    <div class="modal-stat-label">Chủ đề</div>
                    <div class="modal-stat-value">{{ formatNumber(cat.threadCount || 0) }}</div>
                  </div>
                </div>

                <!-- Sub-categories -->
                <div 
                  v-for="sub in (cat.subCategories && cat.subCategories.length ? cat.subCategories : group.categories.filter(c => c.parentCategoryId === cat.id))" 
                  :key="'sub-' + sub.id" 
                  class="modal-category-item modal-sub-category"
                  @click="selectCategory(sub.id)"
                >
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
import ForumHome from '@/shared/components/ForumHome.vue'
import Loading from '@/shared/components/Loading.vue'
import threadService from '@/apps/Forum/services/thread.service'
import categoryService from '@/apps/Forum/services/category.service'
import statisticsService from '@/apps/Forum/services/statistics.service'
import { formatForumDate } from '@/shared/utils/date'
import { isNonOfficialUser } from '@/shared/utils/utils'

export default {
  name: 'HomeView',
  components: {
    ForumHome,
    Loading
  },
  data() {
    return {
      isLoggedIn: false,
      currentUser: null,
      categoryGroupsModal: [],
      showModal: false,
      latestThreads: [],
      loadingLatest: false,
      apiDataLoaded: false,
      forumHomeLoaded: false,
      loadingCategories: false,
      stats: {
        totalCategories: 0,
        totalThreads: 0,
        totalPosts: 0,
        totalMembers: 0,
        latestMember: '',
        officialMembers: 0,
        unofficialMembers: 0,
        totalOfficialAndUnofficial: 0
      }
    }
  },
  computed: {
    isNonOfficial() {
      return isNonOfficialUser()
    },
    isLoading() {
      return !this.apiDataLoaded || !this.forumHomeLoaded || this.loadingCategories
    },
    activeModalGroups() {
      if (!this.categoryGroupsModal || !Array.isArray(this.categoryGroupsModal)) return []
      return this.categoryGroupsModal.filter(g => g.active && g.categories && g.categories.length > 0)
    }
  },
  mounted() {
    this.checkAuth()
    this.loadAllData()
  },
  methods: {
    checkAuth() {
      const user = localStorage.getItem('user')
      if (user) {
        this.isLoggedIn = true
        this.currentUser = JSON.parse(user)
      } else {
        this.isLoggedIn = false
        this.currentUser = null
      }
    },
    async openPostModal() {
      this.loadingCategories = true
      try {
        const response = await categoryService.getGroups()
        this.categoryGroupsModal = response.data
        this.showModal = true
      } catch (error) {
        console.error('Lỗi khi tải nhóm chuyên mục:', error)
      } finally {
        this.loadingCategories = false
      }
    },
    selectCategory(catId) {
      this.showModal = false
      this.$router.push({ name: 'CreateThread', query: { catId } })
    },
    async loadAllData() {
      this.apiDataLoaded = false
      try {
        await Promise.all([
          this.fetchStatistics(),
          this.fetchLatestThreads()
        ])
      } finally {
        this.apiDataLoaded = true
      }
    },
    onForumHomeLoaded() {
      this.forumHomeLoaded = true
    },
    async fetchStatistics() {
      try {
        const response = await statisticsService.get()
        if (response.data) {
          this.stats = response.data
        }
      } catch (error) {
        console.error('Lỗi khi tải thống kê:', error)
      }
    },
    formatNumber(num) {
      if (!num) return 0
      return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",")
    },
    async fetchLatestThreads() {
      this.loadingLatest = true
      try {
        const response = await threadService.getLatest()
        if (response.data) {
          this.latestThreads = response.data.slice(0, 10)
        }
      } catch (error) {
        console.error('Lỗi khi tải bài viết mới nhất:', error)
      } finally {
        this.loadingLatest = false
      }
    },
    formatDate(dateStr) {
      return formatForumDate(dateStr)
    }
  }
}
</script>

<style scoped>
.header-link {
  cursor: pointer;
  text-decoration: none;
  color: inherit;
  transition: all 0.2s;
}
.header-link:hover {
  text-decoration: underline;
}

.latest-threads-list {
  display: flex;
  flex-direction: column;
}

.latest-thread-item {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.latest-thread-item:last-child {
  border-bottom: none;
}

.latest-thread-item:hover {
  background-color: #f9f9f9;
}

.lt-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: #555;
  margin-right: 12px;
  flex-shrink: 0;
  overflow: hidden;
}

.lt-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.lt-content {
  flex: 1;
  min-width: 0;
}

.lt-title {
  font-weight: 500;
  margin-bottom: 4px;
  font-size: 0.95rem;
  word-break: break-word;
  line-height: 1.5;
}

.lt-title a {
  display: block;
  color: #2c3e50;
  text-decoration: none;
}

.lt-title a:hover .lt-title-text {
  color: #1a507a;
  text-decoration: underline;
}

.lt-title-text {
  vertical-align: middle;
}

.label-tag-mini {
  padding: 1px 5px;
  font-size: 0.7rem;
  border-radius: 3px;
  font-weight: 600;
  display: inline-block;
  vertical-align: middle;
  border: 1px solid transparent;
  white-space: nowrap;
  line-height: 1;
  margin-right: 6px;
  flex-shrink: 0;
  position: relative;
  top: -1px;
}

.lt-meta {
  font-size: 0.8rem;
  color: #666;
  margin-bottom: 3px;
  word-break: break-word;
}

.lt-category {
  font-size: 0.8rem;
}

.lt-category a {
  color: #999;
  text-decoration: none;
}

.lt-category a:hover {
  color: #1a507a;
  text-decoration: underline;
}

/* Modal Post Styles */
.modal-group {
  margin-bottom: 5px;
}

.modal-group-header {
  background: #f0f7fb;
  color: #3498db;
  padding: 8px 20px;
  font-weight: 600;
  font-size: 0.95rem;
  border-bottom: 1px solid #e1eef7;
}

.modal-category-list {
  background: #ffffff;
}

.modal-category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.modal-category-item:hover {
  background: #f9fbfc;
}

.modal-category-item:last-child {
  border-bottom: none;
}

.modal-cat-name {
  color: #3498db;
  font-size: 1.05rem;
  font-weight: 500;
}

.modal-category-item:hover .modal-cat-name {
  text-decoration: underline;
}

.modal-cat-desc {
  font-size: 0.8rem;
  color: #888;
  margin-top: 3px;
}

.modal-cat-stats {
  text-align: right;
  color: #666;
  min-width: 60px;
}

.modal-stat-label {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #999;
}

.modal-stat-value {
  font-size: 0.95rem;
  font-weight: 500;
  margin-top: 2px;
}

@import "@/shared/assets/styles/custom.css";
</style>
