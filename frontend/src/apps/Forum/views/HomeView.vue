<template>
  <div>
    <Loading :visible="isLoading" />

    <main class="container">
      <div class="home-action-bar container">
        <div class="smart-greeting-wrapper">
          <span class="greeting-icon">{{ greetingData.icon }}</span>
          <span class="greeting-text"><span class="greeting-period">{{ greetingData.periodTitle }}</span>&nbsp;<span :class="greetingData.isGuest ? 'greeting-guest' : 'greeting-name'" :title="greetingData.name">{{ greetingData.name }}</span>🥰</span>
        </div>
        <div v-if="canShowPostButton" class="user-actions">
          <button @click="openPostModal" class="btn-post-thread">Đăng bài...</button>
        </div>
      </div>

      <div class="main-wrapper">
        <div class="content-left">
          <ForumHome :stats="stats" :latestThreads="latestThreads" :pinnedThreads="pinnedThreads" @loaded="onForumHomeLoaded" />
        </div>
        
        <aside class="content-right">
          <!-- Block "Chú ý" (Pinned Threads) -->
          <div class="card" style="margin-bottom: 1.5rem;">
            <div class="card-header section-header background-f8f9fa pl-and-pr-16">
              <a @click="$router.push({ name: 'PinnedThreads' })" class="header-link text-transform-uppercase color-1a507a">Chú ý</a>
            </div>
            <div class="card-body" style="padding: 0; position: relative;">
              <Loading :visible="loadingPinned" text="Đang tải..." />
              <div class="latest-threads-list">
                <div v-for="thread in pinnedThreads" :key="thread.id" class="latest-thread-item" @click="goToThread($event, thread, false)">
                  <user-profile-popup :user="thread.author" v-if="thread.author">
                    <div class="lt-avatar" :style="!isAvatarUrl(thread.author?.avatar) ? { backgroundColor: thread.author?.avatar || '#e0e0e0', color: '#fff' } : {}">
                      <img v-if="isAvatarUrl(thread.author?.avatar)" :src="formatAvatarUrl(thread.author?.avatar)" />
                      <template v-else>
                        {{ ((thread.author?.displayName || thread.author?.username || 'A')).charAt(0).toUpperCase() }}
                      </template>
                    </div>
                  </user-profile-popup>
                  <div v-else class="lt-avatar" style="background-color: #ccc; color: #fff;">A</div>
                  <div class="lt-content">
                    <div class="lt-title">
                      <router-link :to="{ name: 'ThreadDetail', params: { id: thread.id } }" :title="thread.title">
                        <span v-if="thread.label" class="label-tag-mini" :style="{ backgroundColor: thread.label.colorCode, color: thread.label.textColor, borderColor: thread.label.borderColor || 'transparent' }">
                          {{ thread.label.name }}
                        </span>
                        <span class="lt-title-text">{{ thread.title }}</span>
                        <span v-if="thread.isFollowed" title="Chủ đề đang theo dõi" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 4px;">
                          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#777" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-bell-watched" style="display: block; pointer-events: none;"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
                        </span>
                        <span v-if="thread.pinned" title="Đã ghim" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 4px;">
                          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="icon-pin" style="display: block; pointer-events: none;"><line x1="12" y1="17" x2="12" y2="22"></line><path d="M5 17h14v-1.76a2 2 0 0 0-.44-1.24l-2.78-3.5A2 2 0 0 1 15 9.26V5a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4.26a2 2 0 0 1-.78 1.24l-2.78 3.5a2 2 0 0 0-.44 1.24z"></path></svg>
                        </span>
                        <span v-if="thread.locked" title="Đã khóa" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 4px;">
                          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: block; pointer-events: none;"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                        </span>
                      </router-link>
                    </div>
                    <div class="lt-meta d-flex align-items-center">
                      <user-profile-popup :user="thread.author" v-if="thread.author">
                        <span class="cursor-pointer font-weight-bold me-1">
                          {{ thread.author?.displayName || thread.author?.username }} <VerifiedBadge :user="thread.author" size="14px" />
                        </span>
                      </user-profile-popup>
                      <span v-else>Ẩn danh</span>
                      &nbsp;&middot;&nbsp;{{ formatDate(thread.createdAt) }}
                    </div>
                    <div class="lt-meta" style="margin-top: 2px; color: #666; font-size: 0.8rem;">
                      Trả lời: {{ thread.replyCount || 0 }}
                    </div>
                  </div>
                </div>
                <div v-if="pinnedThreads.length === 0" style="padding: 1rem; text-align: center; color: #999; font-size: 0.9rem;">
                  Chưa có bài viết chú ý nào.
                </div>
                <div v-if="pinnedTotalPages > 1" class="p-2 border-top d-flex justify-content-center">
                  <ForumPagination
                    :current-page="pinnedCurrentPage"
                    :total-pages="pinnedTotalPages"
                    @page-changed="onPinnedPageChanged"
                  />
                </div>
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-header section-header background-f8f9fa pl-and-pr-16">
              <a @click="$router.push({ name: 'LatestThreads' })" class="header-link text-transform-uppercase color-1a507a">Bài viết mới nhất</a>
            </div>
            <div class="card-body" style="padding: 0;">
              <div v-if="loadingLatest" style="padding: 1rem; text-align: center; color: #666; font-size: 0.9rem;">
                Đang tải...
              </div>
              <div v-else class="latest-threads-list">
                <div v-for="thread in latestThreads" :key="thread.id" class="latest-thread-item" @click="goToThread($event, thread, true)">
                  <user-profile-popup :user="thread.lastPostAuthor || thread.author" v-if="thread.lastPostAuthor || thread.author">
                    <div class="lt-avatar" :style="!isAvatarUrl((thread.lastPostAuthor || thread.author)?.avatar) ? { backgroundColor: (thread.lastPostAuthor || thread.author)?.avatar || '#e0e0e0', color: '#fff' } : {}">
                      <img v-if="isAvatarUrl((thread.lastPostAuthor || thread.author)?.avatar)" :src="formatAvatarUrl((thread.lastPostAuthor || thread.author)?.avatar)" />
                      <template v-else>
                        {{ ((thread.lastPostAuthor || thread.author)?.displayName || (thread.lastPostAuthor || thread.author)?.username || 'A').charAt(0).toUpperCase() }}
                      </template>
                    </div>
                  </user-profile-popup>
                  <div v-else class="lt-avatar" style="background-color: #ccc; color: #fff;">A</div>
                  <div class="lt-content">
                    <div class="lt-title">
                      <router-link :to="thread.lastPostId ? { name: 'ThreadDetail', params: { id: thread.id }, query: { postId: thread.lastPostId } } : { name: 'ThreadDetail', params: { id: thread.id } }" :title="thread.title">
                        <span v-if="thread.label" class="label-tag-mini" :style="{ backgroundColor: thread.label.colorCode, color: thread.label.textColor, borderColor: thread.label.borderColor || 'transparent' }">
                          {{ thread.label.name }}
                        </span>
                        <span class="lt-title-text">{{ thread.title }}</span>
                        <span v-if="thread.isFollowed" title="Chủ đề đang theo dõi" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 4px;">
                          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#777" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-bell-watched" style="display: block; pointer-events: none;"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
                        </span>
                        <span v-if="thread.pinned" title="Đã ghim" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 4px;">
                          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="icon-pin" style="display: block; pointer-events: none;"><line x1="12" y1="17" x2="12" y2="22"></line><path d="M5 17h14v-1.76a2 2 0 0 0-.44-1.24l-2.78-3.5A2 2 0 0 1 15 9.26V5a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4.26a2 2 0 0 1-.78 1.24l-2.78 3.5a2 2 0 0 0-.44 1.24z"></path></svg>
                        </span>
                        <span v-if="thread.locked" title="Đã khóa" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 4px;">
                          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: block; pointer-events: none;"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                        </span>
                      </router-link>
                    </div>
                    <div class="lt-meta d-flex align-items-center">
                      Mới nhất:&nbsp;
                      <user-profile-popup :user="thread.lastPostAuthor || thread.author" v-if="thread.lastPostAuthor || thread.author">
                        <span class="cursor-pointer font-weight-bold me-1">
                          {{ (thread.lastPostAuthor || thread.author)?.displayName || (thread.lastPostAuthor || thread.author)?.username }} <VerifiedBadge :user="thread.lastPostAuthor || thread.author" size="14px" />
                        </span>
                      </user-profile-popup>
                      <span v-else>Ẩn danh</span>
                      &nbsp;&middot;&nbsp;{{ formatDate(thread.lastPostAt || thread.createdAt) }}
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
          <div class="card display-none">
            <div class="card-header background-f8f9fa text-transform-uppercase color-1a507a pl-and-pr-16 display-none">Vô công rỗi nghề</div>
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
              <template v-for="cat in getVisibleCategories(group.categories.filter(c => !c.parentCategoryId))" :key="cat.id">
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
                  v-for="sub in getVisibleCategories(cat.subCategories && cat.subCategories.length ? cat.subCategories : group.categories.filter(c => c.parentCategoryId === cat.id))" 
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
import UserProfilePopup from '@/shared/components/UserProfilePopup.vue'
import VerifiedBadge from '@/shared/components/VerifiedBadge.vue'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import threadService from '@/apps/Forum/services/thread.service'
import categoryService from '@/apps/Forum/services/category.service'
import statisticsService from '@/apps/Forum/services/statistics.service'
import { formatForumDate } from '@/shared/utils/date'
import { isNonOfficialUser, isAvatarUrl, formatAvatarUrl, isAdminOrSuperAdmin, canShowPostButtonOnScreen, loadPublicSettings } from '@/shared/utils/utils'

export default {
  name: 'HomeView',
  components: {
    ForumHome,
    Loading,
    UserProfilePopup,
    VerifiedBadge,
    ForumPagination
  },
  data() {
    return {
      isLoggedIn: false,
      currentUser: null,
      categoryGroupsModal: [],
      showModal: false,
      pinnedThreads: [],
      loadingPinned: false,
      pinnedCurrentPage: 1,
      pinnedPageSize: 5,
      pinnedTotalPages: 1,
      pinnedTotalElements: 0,
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
    isAdmin() {
      return isAdminOrSuperAdmin()
    },
    canShowPostButton() {
      return canShowPostButtonOnScreen('home')
    },
    isLoading() {
      return !this.apiDataLoaded || !this.forumHomeLoaded || this.loadingCategories
    },
    activeModalGroups() {
      if (!this.categoryGroupsModal || !Array.isArray(this.categoryGroupsModal)) return []
      return this.categoryGroupsModal.filter(g => g.active && g.categories && g.categories.length > 0)
    },
    greetingData() {
      const hour = new Date().getHours()
      let periodTitle = ''
      let icon = ''

      if (hour >= 5 && hour < 11) {
        periodTitle = 'Chào buổi sáng'
        icon = '🌅'
      } else if (hour >= 11 && hour < 14) {
        periodTitle = 'Chào buổi trưa'
        icon = '☀️'
      } else if (hour >= 14 && hour < 18) {
        periodTitle = 'Chào buổi chiều'
        icon = '🌤️'
      } else if (hour >= 18 && hour < 23) {
        periodTitle = 'Chào buổi tối'
        icon = '🌙'
      } else {
        periodTitle = 'Đã khuya rồi'
        icon = '🦉'
      }

      const rawName = this.currentUser?.displayName || this.currentUser?.username || ''
      const isGuest = !rawName
      const name = isGuest ? 'bạn nhé' : `bạn ${rawName} nhé`

      return {
        icon,
        periodTitle,
        name,
        isGuest
      }
    }
  },
  mounted() {
    this.checkAuth()
    loadPublicSettings()
    this.loadAllData()
    window.addEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  beforeUnmount() {
    window.removeEventListener('user-avatar-updated', this.handleAvatarUpdated)
  },
  methods: {
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    formatAvatarUrl(avatar) {
      return formatAvatarUrl(avatar)
    },
    getVisibleCategories(cats) {
      if (!cats) return []
      if (this.isAdmin) return cats
      return cats.filter(c => !c.onlyAdminCanPost)
    },
    handleAvatarUpdated(event) {
      const { username, avatar } = event.detail
      this.latestThreads = this.latestThreads.map(t => {
        const updated = { ...t }
        if (t.author && t.author.username === username) {
          updated.author = { ...t.author, avatar }
        }
        if (t.lastPostAuthor && t.lastPostAuthor.username === username) {
          updated.lastPostAuthor = { ...t.lastPostAuthor, avatar }
        }
        return updated
      })
      this.pinnedThreads = this.pinnedThreads.map(t => {
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
          this.fetchLatestThreads(),
          this.fetchPinnedThreads()
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
    async fetchPinnedThreads() {
      this.loadingPinned = true
      try {
        const page = this.pinnedCurrentPage - 1
        const size = this.pinnedPageSize
        const res = await threadService.getAll({ pinned: true, page, size, sortBy: 'createdAt', sortOrder: 'desc' })
        if (res.data && res.data.content) {
          this.pinnedThreads = res.data.content
          this.pinnedTotalPages = res.data.totalPages || 1
          this.pinnedTotalElements = res.data.totalElements || 0
        } else {
          this.pinnedThreads = []
          this.pinnedTotalPages = 1
          this.pinnedTotalElements = 0
        }
      } catch (error) {
        console.error('Lỗi khi tải bài viết chú ý:', error)
      } finally {
        this.loadingPinned = false
      }
    },
    async onPinnedPageChanged(newPage) {
      this.pinnedCurrentPage = newPage
      await this.fetchPinnedThreads()
    },
    async fetchLatestThreads() {
      this.loadingLatest = true
      try {
        const response = await threadService.getLatest()
        if (response.data && Array.isArray(response.data)) {
          this.latestThreads = response.data.slice(0, 10)
        } else {
          this.latestThreads = []
        }
      } catch (error) {
        console.error('Lỗi khi tải bài viết mới nhất:', error)
        this.latestThreads = []
      } finally {
        this.loadingLatest = false
      }
    },
    formatDate(dateStr) {
      return formatForumDate(dateStr)
    },
    goToThread(event, thread, targetLast = false) {
      if (event.target.closest('a, button, .lt-avatar, [role="button"]')) {
        return
      }
      const route = {
        name: 'ThreadDetail',
        params: { id: thread.id }
      }
      if (targetLast && thread.lastPostId) {
        route.query = { postId: thread.lastPostId }
      }
      this.$router.push(route)
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
  cursor: pointer;
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
  display: inline;
  color: #2c3e50;
  text-decoration: none;
}

.lt-title a:hover .lt-title-text {
  color: #1a507a;
  text-decoration: underline;
}

.lt-title-text {
  display: inline;
  white-space: normal;
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

/* Lời Chào Thông Minh Theo Buổi (Smart Greeting) - Tối Giản */
.home-action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0;
  margin-bottom: 0.85rem;
  gap: 12px;
}

.smart-greeting-wrapper {
  font-size: 18px;
  font-weight: 700;
  color: #1a507a;
  display: flex;
  align-items: center;
  line-height: 1.4;
  animation: greetingFadeIn 0.35s ease-out;
  flex: 1;
  min-width: 0;
  white-space: nowrap;
}

@keyframes greetingFadeIn {
  from {
    opacity: 0;
    transform: translateY(2px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.greeting-icon {
  font-size: 1.25rem;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  transform: translateY(-2px); /* Căn chỉnh tâm thị giác nâng Emoji lên thẳng hàng hoàn hảo với chữ */
  user-select: none;
  flex-shrink: 0;
}

.greeting-text {
  display: inline-flex;
  align-items: center;
  line-height: 1.4;
  color: #1a507a;
  min-width: 0;
  white-space: nowrap;
}

.greeting-period {
  flex-shrink: 0;
  white-space: nowrap;
}

.greeting-guest {
  color: #1a507a;
  font-weight: 700;
  white-space: nowrap;
}

.greeting-name {
  color: #0284c7;
  font-weight: 700;
  display: inline-block;
  max-width: 250px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.user-actions {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .home-action-bar {
    margin-bottom: 0.75rem;
    gap: 8px;
  }

  .smart-greeting-wrapper {
    font-size: 15px;
  }

  .greeting-icon {
    font-size: 1.15rem;
    margin-right: 5px;
  }

  .greeting-name {
    max-width: 110px;
  }
}

@media (max-width: 380px) {
  .smart-greeting-wrapper {
    font-size: 14px;
  }

  .greeting-name {
    max-width: 80px;
  }

  .btn-post-thread {
    padding: 7px 14px;
    font-size: 0.88rem;
  }
}

@import "@/shared/assets/styles/custom.css";
</style>
