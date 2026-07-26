<template>
  <div>
    <Loading :visible="isLoading" />

    <main class="container" style="padding-bottom: 3rem;">
      <!-- Breadcrumb (hiển thị khi ở màn hình danh sách) -->
      <div v-if="isListView" class="members-breadcrumb">
        <router-link to="/" class="bc-home-link" title="Trang chủ">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
            <polyline points="9 22 9 12 15 12 15 22"></polyline>
          </svg>
        </router-link>
        <span class="bc-sep">›</span>
        <router-link to="/thanh-vien" class="bc-link">Thành viên</router-link>
        <span class="bc-sep">›</span>
        <span class="bc-current">{{ currentBlockTitle }}</span>
      </div>

      <!-- Action bar / Title -->
      <div class="members-title-bar" style="margin-top: 1rem; margin-bottom: 1rem;">
        <h1 v-if="isListView" class="members-page-title">{{ currentBlockTitle }}</h1>
        <div v-else class="forum-slogan" style="font-weight: bold; color: #1a507a; font-size: 1.1rem; text-transform: uppercase;">
          Thành viên đáng chú ý
        </div>
      </div>

      <!-- Nút Toggle Sidebar trên Mobile/Tablet (centered) -->
      <div class="toggle-button-container">
        <button class="btn-members-toggle" @click="isSidebarOpen = true">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-toggle">
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
          Thành viên
        </button>
      </div>

      <!-- Main Layout: 2 Columns -->
      <div class="members-layout">
        <!-- Sidebar Drawer Wrapper -->
        <div class="members-sidebar-wrapper" :class="{ 'open': isSidebarOpen }">
          <div class="sidebar-backdrop" @click="isSidebarOpen = false"></div>

          <div class="members-sidebar-container">
            <!-- Sidebar Column (Left) -->
            <aside class="sidebar-col">
              <!-- Block 1: Sidebar Menu -->
              <div class="sidebar-box card">
                <div class="sidebar-header">
                  <span>Thành viên</span>
                  <button class="btn-close-sidebar" @click="isSidebarOpen = false" aria-label="Đóng menu">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <line x1="18" y1="6" x2="6" y2="18"></line>
                      <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                  </button>
                </div>
                <div class="sidebar-menu">
                  <router-link 
                    :to="{ name: 'MembersView' }"
                    class="menu-item" 
                    :class="{ 'is-active': !currentKey || currentKey === 'overview' }"
                    @click="isSidebarOpen = false"
                  >
                    Tổng quan
                  </router-link>
                  <router-link 
                    :to="{ name: 'MembersView', query: { key: MEMBER_KEYS.MOST_MESSAGES } }"
                    class="menu-item" 
                    :class="{ 'is-active': currentKey === MEMBER_KEYS.MOST_MESSAGES }"
                    @click="isSidebarOpen = false"
                  >
                    Nhiều bài nhất
                  </router-link>
                  <router-link 
                    :to="{ name: 'MembersView', query: { key: MEMBER_KEYS.MOST_REACTIONS } }"
                    class="menu-item" 
                    :class="{ 'is-active': currentKey === MEMBER_KEYS.MOST_REACTIONS }"
                    @click="isSidebarOpen = false"
                  >
                    Nhiều điểm tương tác nhất
                  </router-link>
                  <router-link 
                    :to="{ name: 'MembersView', query: { key: MEMBER_KEYS.MOST_POINTS } }"
                    class="menu-item" 
                    :class="{ 'is-active': currentKey === MEMBER_KEYS.MOST_POINTS }"
                    @click="isSidebarOpen = false"
                  >
                    Nhiều điểm nhất
                  </router-link>
                </div>
              </div>

              <!-- Block 2: Tìm thành viên -->
              <div class="sidebar-box card search-member-box">
                <div class="sidebar-header-sub">Tìm thành viên</div>
                <div class="search-box-padding">
                  <UserSearchInput 
                    placeholder="Tên..."
                    @select="goToProfile"
                  />
                </div>
              </div>

              <!-- Block 3: Thành viên mới nhất -->
              <div class="sidebar-box card newest-members-box">
                <div class="sidebar-header-sub">Thành viên mới nhất</div>
                <div class="newest-members-padding">
                  <div class="newest-members-grid">
                    <div v-for="u in newestMembers" :key="u.id || u.username" class="newest-member-item">
                      <UserProfilePopup :user="u">
                        <div 
                          class="member-avatar-circle" 
                          :style="!isAvatarUrl(u.avatar) ? { backgroundColor: getAvatarColor(u) } : {}"
                        >
                          <img v-if="isAvatarUrl(u.avatar)" :src="u.avatar" alt="avatar" />
                          <span v-else>{{ (u.displayName || u.username || '?').charAt(0).toUpperCase() }}</span>
                        </div>
                      </UserProfilePopup>
                    </div>
                  </div>
                </div>
              </div>
            </aside>
          </div>
        </div>

        <!-- Main Content Column (Right) -->
        <div class="content-col">
          <!-- Overview Mode -->
          <template v-if="!isListView">
            <div class="top-stats-row">
              <!-- Block 1: Nhiều bài nhất -->
              <div id="block-top-posters" class="card stats-block">
                <div class="card-header section-header">Nhiều bài nhất</div>
                <div class="card-body padding-0">
                  <div v-for="item in topPosters" :key="item.id || item.username" class="stat-user-row">
                    <UserProfilePopup :user="item" style="display: flex; align-items: center;">
                      <div class="user-left-col">
                        <div 
                          class="stat-avatar-circle" 
                          :style="!isAvatarUrl(item.avatar) ? { backgroundColor: getAvatarColor(item) } : {}" 
                        >
                          <img v-if="isAvatarUrl(item.avatar)" :src="item.avatar" alt="avatar" />
                          <span v-else>{{ (item.displayName || item.username || '?').charAt(0).toUpperCase() }}</span>
                        </div>
                        <span class="user-name-link">
                          {{ item.displayName || item.username }}
                        </span>
                      </div>
                    </UserProfilePopup>

                    <div class="user-right-val">
                      {{ formatNumber(item.postCount) }}
                    </div>
                  </div>

                  <div class="block-footer">
                    <button class="btn-see-more" @click="navigateToKey(MEMBER_KEYS.MOST_MESSAGES)">Xem thêm...</button>
                  </div>
                </div>
              </div>

              <!-- Block 2: Nhiều điểm tương tác nhất -->
              <div id="block-top-interactions" class="card stats-block">
                <div class="card-header section-header">Nhiều điểm tương tác nhất</div>
                <div class="card-body padding-0">
                  <div v-for="item in topInteractions" :key="item.id || item.username" class="stat-user-row">
                    <UserProfilePopup :user="item" style="display: flex; align-items: center;">
                      <div class="user-left-col">
                        <div 
                          class="stat-avatar-circle" 
                          :style="!isAvatarUrl(item.avatar) ? { backgroundColor: getAvatarColor(item) } : {}" 
                        >
                          <img v-if="isAvatarUrl(item.avatar)" :src="item.avatar" alt="avatar" />
                          <span v-else>{{ (item.displayName || item.username || '?').charAt(0).toUpperCase() }}</span>
                        </div>
                        <span class="user-name-link">
                          {{ item.displayName || item.username }}
                        </span>
                      </div>
                    </UserProfilePopup>

                    <div class="user-right-val">
                      {{ formatNumber(item.interactionPoints) }}
                    </div>
                  </div>

                  <div class="block-footer">
                    <button class="btn-see-more" @click="navigateToKey(MEMBER_KEYS.MOST_REACTIONS)">Xem thêm...</button>
                  </div>
                </div>
              </div>

              <!-- Block 3: Nhiều điểm nhất -->
              <div id="block-top-trophy-points" class="card stats-block">
                <div class="card-header section-header">Nhiều điểm nhất</div>
                <div class="card-body padding-0">
                  <div v-for="item in topTrophyPoints" :key="item.id || item.username" class="stat-user-row">
                    <UserProfilePopup :user="item" style="display: flex; align-items: center;">
                      <div class="user-left-col">
                        <div 
                          class="stat-avatar-circle" 
                          :style="!isAvatarUrl(item.avatar) ? { backgroundColor: getAvatarColor(item) } : {}" 
                        >
                          <img v-if="isAvatarUrl(item.avatar)" :src="item.avatar" alt="avatar" />
                          <span v-else>{{ (item.displayName || item.username || '?').charAt(0).toUpperCase() }}</span>
                        </div>
                        <span class="user-name-link">
                          {{ item.displayName || item.username }}
                        </span>
                      </div>
                    </UserProfilePopup>

                    <div class="user-right-val">
                      {{ formatNumber(item.trophyPoints) }}
                    </div>
                  </div>

                  <div class="block-footer">
                    <button class="btn-see-more" @click="navigateToKey(MEMBER_KEYS.MOST_POINTS)">Xem thêm...</button>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- List View Mode (Phân trang) -->
          <template v-else>
            <div class="card member-list-card">
              <div class="member-list-body">
                <div v-if="memberList.length === 0 && !isLoading" class="no-members-msg">
                  Không có thành viên nào.
                </div>
                <div 
                  v-for="userItem in memberList" 
                  :key="userItem.id || userItem.username" 
                  class="member-list-row"
                >
                  <!-- Cột bên trái: Avatar & Info 3 hàng -->
                  <div class="member-left-main-col">
                    <!-- Cột con 1: Avatar -->
                    <UserProfilePopup :user="userItem">
                      <div 
                        class="list-avatar-circle" 
                        :style="!isAvatarUrl(userItem.avatar) ? { backgroundColor: getAvatarColor(userItem) } : {}"
                      >
                        <img v-if="isAvatarUrl(userItem.avatar)" :src="userItem.avatar" alt="avatar" />
                        <span v-else>{{ (userItem.displayName || userItem.username || '?').charAt(0).toUpperCase() }}</span>
                      </div>
                    </UserProfilePopup>

                    <!-- Cột con 2: 3 Hàng thông tin -->
                    <div class="member-info-col">
                      <!-- Hàng 1: DisplayName -->
                      <div class="member-name-row">
                        <UserProfilePopup :user="userItem">
                          <span class="member-display-name">
                            {{ userItem.displayName || userItem.username }}
                          </span>
                        </UserProfilePopup>
                      </div>

                      <!-- Hàng 2: Cấp bậc -->
                      <div class="member-rank-row">
                        Thành viên
                      </div>

                      <!-- Hàng 3: Thống kê 3 chỉ số -->
                      <div class="member-stats-row">
                        Bài viết: {{ formatNumber(userItem.postCount) }} · Điểm tương tác: {{ formatNumber(userItem.interactionPoints) }} · Điểm: {{ formatNumber(userItem.trophyPoints) }}
                      </div>
                    </div>
                  </div>

                  <!-- Cột bên phải: Chỉ số tiêu chí chính -->
                  <div class="member-right-main-col">
                    <span class="main-stat-value">
                      {{ formatNumber(getMetricValue(userItem)) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Phân trang dùng chung -->
            <div class="member-pagination-wrapper">
              <ForumPagination
                :currentPage="currentPage"
                :totalPages="totalPages"
                @page-changed="handlePageChange"
              />
            </div>
          </template>

        </div>
      </div>
    </main>
  </div>
</template>

<script>
import userService from '@/apps/Forum/services/user.service'
import Loading from '@/shared/components/Loading.vue'
import UserProfilePopup from '@/shared/components/UserProfilePopup.vue'
import UserSearchInput from '@/shared/components/UserSearchInput.vue'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import { isAvatarUrl } from '@/shared/utils/utils'
import { MEMBER_KEYS } from '@/shared/utils/constants'

export default {
  name: 'MembersView',
  components: {
    Loading,
    UserProfilePopup,
    UserSearchInput,
    ForumPagination
  },
  data() {
    return {
      MEMBER_KEYS,
      isLoading: true,
      isSidebarOpen: false,
      newestMembers: [],
      // Overview data
      topPosters: [],
      topInteractions: [],
      topTrophyPoints: [],
      // List data
      memberList: [],
      currentPage: 1,
      totalPages: 1,
      pageSize: 10
    }
  },
  computed: {
    currentKey() {
      return this.$route.query.key || ''
    },
    isListView() {
      return [MEMBER_KEYS.MOST_MESSAGES, MEMBER_KEYS.MOST_REACTIONS, MEMBER_KEYS.MOST_POINTS].includes(this.currentKey)
    },
    currentBlockTitle() {
      if (this.currentKey === MEMBER_KEYS.MOST_MESSAGES) return 'Nhiều bài nhất'
      if (this.currentKey === MEMBER_KEYS.MOST_REACTIONS) return 'Nhiều điểm tương tác nhất'
      if (this.currentKey === MEMBER_KEYS.MOST_POINTS) return 'Nhiều điểm nhất'
      return 'Tổng quan'
    }
  },
  watch: {
    '$route.query': {
      immediate: true,
      async handler(newQuery) {
        await this.loadPageData(newQuery)
      }
    }
  },
  methods: {
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    navigateToKey(key) {
      if (!key) {
        this.$router.push({ name: 'MembersView' })
      } else {
        this.$router.push({ name: 'MembersView', query: { key } })
      }
    },
    async loadPageData(query) {
      this.isLoading = true
      try {
        if (!this.newestMembers || this.newestMembers.length === 0) {
          const resNewest = await userService.getNewestMembers(12)
          if (resNewest.data) this.newestMembers = resNewest.data
        }

        const key = query.key
        if ([MEMBER_KEYS.MOST_MESSAGES, MEMBER_KEYS.MOST_REACTIONS, MEMBER_KEYS.MOST_POINTS].includes(key)) {
          const page = parseInt(query.page) || 1
          this.currentPage = page
          const resPaged = await userService.getMembersPaged(key, page - 1, this.pageSize)
          console.log("check", resPaged);
          if (resPaged.data) {
            const pageData = resPaged.data
            this.memberList = pageData.content || []
            this.totalPages = pageData.totalPages || 1
            this.currentPage = (pageData.number || 0) + 1
          }
        } else {
          // Overview mode
          const [resPosters, resInteractions, resPoints] = await Promise.all([
            userService.getTopPosters(5),
            userService.getTopInteractions(5),
            userService.getTopTrophyPoints(5)
          ])
          if (resPosters.data) this.topPosters = resPosters.data
          if (resInteractions.data) this.topInteractions = resInteractions.data
          if (resPoints.data) this.topTrophyPoints = resPoints.data
        }
      } catch (e) {
        console.error('Lỗi khi nạp dữ liệu màn hình Thành viên:', e)
      } finally {
        this.isLoading = false
      }
    },
    handlePageChange(newPage) {
      this.$router.push({
        name: 'MembersView',
        query: {
          ...this.$route.query,
          page: newPage
        }
      })
    },
    goToProfile(user) {
      this.isSidebarOpen = false
      if (!user) return
      const username = user.username
      if (username) {
        this.$router.push({
          name: 'UserProfile',
          query: { username }
        })
      }
    },
    formatNumber(num) {
      if (num === null || num === undefined) return '0'
      return Number(num).toLocaleString('en-US')
    },
    getMetricValue(user) {
      if (!user) return 0
      if (this.currentKey === MEMBER_KEYS.MOST_MESSAGES) return user.postCount
      if (this.currentKey === MEMBER_KEYS.MOST_REACTIONS) return user.interactionPoints
      if (this.currentKey === MEMBER_KEYS.MOST_POINTS) return user.trophyPoints
      return 0
    },
    getAvatarColor(user) {
      if (!user) return '#1a507a'
      if (user.avatar && (user.avatar.startsWith('#') || user.avatar.startsWith('hsl'))) {
        return user.avatar
      }
      const name = user.displayName || user.username || '?'
      let hash = 0
      for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash)
      }
      const h = Math.abs(hash % 360)
      return `hsl(${h}, 60%, 50%)`
    }
  }
}
</script>

<style scoped>
.members-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.sidebar-col {
  width: 250px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.content-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.top-stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
}

/* Default state for drawer & toggle button on Desktop */
.btn-close-sidebar {
  display: none;
}

.toggle-button-container {
  display: none;
}

.members-sidebar-wrapper {
  display: contents;
}

.sidebar-backdrop {
  display: none;
}

.members-sidebar-container {
  display: contents;
}

/* Sidebar Box matching AccountSidebar.vue */
.sidebar-box {
  background: #ffffff;
  border: 1px solid #d8dbe0;
  border-radius: 4px;
}

.sidebar-header {
  background: #f8f9fa;
  padding: 12px 15px;
  font-weight: bold;
  font-size: 1rem;
  color: #1a507a;
  border-bottom: 1px solid #d8dbe0;
  border-top-left-radius: 4px;
  border-top-right-radius: 4px;
}

.sidebar-header-sub {
  font-size: 1rem;
  font-weight: bold;
  color: #1a507a;
  padding: 12px 15px 6px 15px;
}

.sidebar-menu {
  display: flex;
  flex-direction: column;
}

.menu-item {
  padding: 10px 15px;
  color: #1a507a;
  text-decoration: none;
  font-size: 0.95rem;
  transition: all 0.2s;
  border-left: 3px solid transparent;
  display: block;
}

.menu-item:hover {
  background-color: #f8f9fa;
  color: #d13838;
}

.menu-item.is-active {
  background-color: #f0f4f8;
  color: #1a507a;
  font-weight: bold;
  border-left-color: #1a507a;
}

.search-box-padding {
  padding: 6px 15px 15px 15px;
}

.newest-members-padding {
  padding: 6px 15px 15px 15px;
}

.newest-members-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.member-avatar-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  color: #ffffff;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  cursor: pointer;
  overflow: hidden;
  background-color: #1a507a;
  transition: transform 0.15s ease;
}

.member-avatar-circle:hover {
  transform: scale(1.05);
}

.member-avatar-circle img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Stats Blocks (Right Column) */
.stats-block {
  background: #ffffff;
  border: 1px solid #d8dbe0;
  border-radius: 4px;
}

.section-header {
  font-weight: bold;
  color: #1a507a;
  font-size: 1.05rem;
  background-color: #f8f9fa;
  padding: 12px 16px;
  border-bottom: 1px solid #d8dbe0;
}

.padding-0 {
  padding: 0 !important;
}

.stat-user-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #edf0f5;
  transition: background-color 0.15s;
}

.stat-user-row:hover {
  background-color: #fafbfc;
}

.stat-user-row:last-child {
  border-bottom: none;
}

.user-left-col {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-avatar-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: #ffffff;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.95rem;
  cursor: pointer;
  overflow: hidden;
  background-color: #1a507a;
  flex-shrink: 0;
}

.stat-avatar-circle img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-name-link {
  color: #1a507a;
  font-weight: 500;
  font-size: 0.95rem;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.15s;
}

.user-name-link:hover {
  text-decoration: underline;
  color: #1a507a;
  cursor: pointer;
}

.user-right-val {
  font-weight: bold;
  color: #495057;
  font-size: 0.95rem;
}

.block-footer {
  padding: 10px 16px;
  background-color: #fcfcfc;
  text-align: center;
  border-top: 1px solid #edf0f5;
}

.btn-see-more {
  background-color: #f8f9fa;
  border: 1px solid #d8dbe0;
  color: #1a507a;
  padding: 6px 20px;
  border-radius: 4px;
  font-size: 0.88rem;
  cursor: pointer;
  width: 100%;
  transition: all 0.2s;
  font-weight: 500;
}

.btn-see-more:hover {
  background-color: #eef4f8;
  border-color: #b3c6d6;
}

/* Breadcrumb */
.members-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  color: #666;
  margin-top: 0.75rem;
  margin-bottom: 0.5rem;
}

.bc-home-link {
  display: inline-flex;
  align-items: center;
  color: #1a507a;
  text-decoration: none;
}

.bc-home-link:hover {
  color: #d13838;
}

.bc-link {
  color: #1a507a;
  text-decoration: none;
  font-weight: 500;
}

.bc-link:hover {
  text-decoration: underline;
}

.bc-sep {
  color: #888;
  font-size: 1rem;
}

.bc-current {
  color: #444;
  font-weight: 500;
}

.members-page-title {
  font-size: 1.5rem;
  font-weight: bold;
  color: #1a507a;
  margin: 0;
}

/* Member List Card */
.member-list-card {
  background: #ffffff;
  border: 1px solid #d8dbe0;
  border-radius: 4px;
  overflow: hidden;
}

.member-list-body {
  display: flex;
  flex-direction: column;
}

.member-list-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid #edf0f5;
  transition: background-color 0.15s ease;
}

.member-list-row:last-child {
  border-bottom: none;
}

.member-list-row:hover {
  background-color: #fafbfc;
}

.member-left-main-col {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.list-avatar-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  color: #ffffff;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  cursor: pointer;
  overflow: hidden;
  background-color: #1a507a;
  flex-shrink: 0;
}

.list-avatar-circle img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.member-info-col {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.member-name-row {
  display: flex;
  align-items: center;
}

.member-display-name {
  font-weight: 600;
  font-size: 1.05rem;
  color: #1a507a;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.15s;
}

.member-display-name:hover {
  text-decoration: underline;
}

.member-rank-row {
  font-size: 0.85rem;
  color: #666;
}

.member-stats-row {
  font-size: 0.82rem;
  color: #777;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.member-right-main-col {
  flex-shrink: 0;
  margin-left: 16px;
  text-align: right;
}

.main-stat-value {
  font-size: 1.25rem;
  font-weight: bold;
  color: #666;
}

.no-members-msg {
  padding: 2rem;
  text-align: center;
  color: #777;
}

.member-pagination-wrapper {
  display: flex;
  justify-content: flex-start;
}

@media (max-width: 1024px) {
  .toggle-button-container {
    display: flex;
    justify-content: center;
    width: 100%;
    margin-bottom: 1.25rem;
  }

  .btn-members-toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    background-color: #ffffff;
    border: 1px solid #d8dbe0;
    border-radius: 4px;
    padding: 8px 16px;
    color: #1a507a;
    font-weight: 500;
    font-size: 0.95rem;
    cursor: pointer;
    transition: all 0.2s ease;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  }

  .btn-members-toggle:hover {
    background-color: #f8f9fa;
    color: #d13838;
    border-color: #c0c4cc;
  }

  .btn-close-sidebar {
    display: flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    border: none;
    color: white;
    cursor: pointer;
    padding: 5px;
    border-radius: 4px;
    transition: background-color 0.2s;
  }

  .btn-close-sidebar:hover {
    background-color: rgba(255, 255, 255, 0.15);
  }

  .sidebar-header {
    background: #1a507a !important;
    color: #ffffff !important;
    border-bottom: 1px solid #123a59 !important;
    border-radius: 0 !important;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 15px !important;
  }

  .sidebar-box {
    border: none !important;
    border-radius: 0 !important;
    box-shadow: none !important;
    background: transparent !important;
  }

  .members-sidebar-wrapper {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 10000;
    visibility: hidden;
    transition: visibility 0.3s;
  }

  .members-sidebar-wrapper.open {
    visibility: visible;
  }

  .sidebar-backdrop {
    display: block;
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.6);
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  .members-sidebar-wrapper.open .sidebar-backdrop {
    opacity: 1;
  }

  .members-sidebar-container {
    display: block;
    position: absolute;
    top: 0;
    left: 0;
    width: 280px;
    height: 100%;
    background-color: #ffffff;
    box-shadow: 2px 0 12px rgba(0, 0, 0, 0.15);
    display: flex;
    flex-direction: column;
    transform: translateX(-100%);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    z-index: 1;
    overflow-y: auto;
  }

  .members-sidebar-wrapper.open .members-sidebar-container {
    transform: translateX(0);
  }

  .sidebar-col {
    width: 100% !important;
    gap: 0 !important;
  }
}

@media (min-width: 768px) and (max-width: 1024px) {
  .top-stats-row {
    grid-template-columns: repeat(2, 1fr);
    gap: 15px;
  }
}

@media (max-width: 767.98px) {
  .top-stats-row {
    grid-template-columns: 1fr;
    gap: 15px;
  }

  .member-stats-row {
    white-space: normal;
  }
}
</style>
