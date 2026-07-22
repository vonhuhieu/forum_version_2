<template>
  <div>
    <Loading :visible="isLoading" />

    <main class="container" style="padding-bottom: 3rem;">
      <!-- Banner -->
      <div class="banner-box" style="margin-top: 1rem;">
        <img src="/675456323_122106804740812631_4737388993277477397_n.jpg" alt="Members Banner">
      </div>

      <!-- Action bar / Title -->
      <div class="members-action-bar container" style="display: flex; justify-content: space-between; align-items: center; padding: 0; margin-top: 1rem; margin-bottom: 1.5rem;">
        <div class="forum-slogan" style="font-weight: bold; color: #1a507a; font-size: 1.1rem; text-transform: uppercase;">
          Thành viên
        </div>
      </div>

      <!-- Main Layout: 2 Columns -->
      <div class="members-layout">
        <!-- Sidebar Column (Left) -->
        <aside class="sidebar-col">
          <!-- Block 1: Sidebar Menu (Styled matching AccountSidebar.vue) -->
          <div class="sidebar-box card">
            <div class="sidebar-header">Thành viên</div>
            <div class="sidebar-menu">
              <a 
                href="#" 
                class="menu-item" 
                :class="{ 'is-active': activeTab === 'overview' }"
                @click.prevent="scrollToTab('overview')"
              >
                Tổng quan
              </a>
              <a 
                href="#" 
                class="menu-item" 
                :class="{ 'is-active': activeTab === 'top-posters' }"
                @click.prevent="scrollToTab('top-posters')"
              >
                Nhiều bài nhất
              </a>
              <a 
                href="#" 
                class="menu-item" 
                :class="{ 'is-active': activeTab === 'top-interactions' }"
                @click.prevent="scrollToTab('top-interactions')"
              >
                Nhiều điểm tương tác nhất
              </a>
              <a 
                href="#" 
                class="menu-item" 
                :class="{ 'is-active': activeTab === 'top-trophy-points' }"
                @click.prevent="scrollToTab('top-trophy-points')"
              >
                Nhiều điểm nhất
              </a>
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

          <!-- Block 3: Thành viên mới nhất (Grid 3x4 = 12 items) -->
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

        <!-- Main Content Column (Right) -->
        <div class="content-col">
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
                  <button class="btn-see-more" @click="handleSeeMore">Xem thêm...</button>
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
                  <button class="btn-see-more" @click="handleSeeMore">Xem thêm...</button>
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
                  <button class="btn-see-more" @click="handleSeeMore">Xem thêm...</button>
                </div>
              </div>
            </div>
          </div>

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
import { isAvatarUrl } from '@/shared/utils/utils'

export default {
  name: 'MembersView',
  components: {
    Loading,
    UserProfilePopup,
    UserSearchInput
  },
  data() {
    return {
      isLoading: true,
      activeTab: 'overview',
      newestMembers: [],
      topPosters: [],
      topInteractions: [],
      topTrophyPoints: []
    }
  },
  async mounted() {
    await this.fetchData()
  },
  methods: {
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    async fetchData() {
      this.isLoading = true
      try {
        const [resNewest, resPosters, resInteractions, resPoints] = await Promise.all([
          userService.getNewestMembers(12),
          userService.getTopPosters(5),
          userService.getTopInteractions(5),
          userService.getTopTrophyPoints(5)
        ])

        if (resNewest.data) this.newestMembers = resNewest.data
        if (resPosters.data) this.topPosters = resPosters.data
        if (resInteractions.data) this.topInteractions = resInteractions.data
        if (resPoints.data) this.topTrophyPoints = resPoints.data
      } catch (e) {
        console.error('Lỗi khi nạp dữ liệu màn hình Thành viên:', e)
      } finally {
        this.isLoading = false
      }
    },
    scrollToTab(tabName) {
      this.activeTab = tabName
      if (tabName === 'overview') {
        window.scrollTo({ top: 0, behavior: 'smooth' })
        return
      }
      const elementId = `block-${tabName}`
      const el = document.getElementById(elementId)
      if (el) {
        const offset = 80
        const bodyRect = document.body.getBoundingClientRect().top
        const elementRect = el.getBoundingClientRect().top
        const elementPosition = elementRect - bodyRect
        const offsetPosition = elementPosition - offset

        window.scrollTo({
          top: offsetPosition,
          behavior: 'smooth'
        })
      }
    },
    goToProfile(user) {
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
    getAvatarColor(user) {
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
    },
    handleSeeMore() {
      // Placeholder UI
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
  gap: 20px;
  min-width: 0;
}

.top-stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
}

@media (max-width: 992px) {
  .top-stats-row {
    grid-template-columns: 1fr;
  }
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

@media (max-width: 768px) {
  .members-layout {
    flex-direction: column;
  }
  .sidebar-col {
    width: 100%;
  }
}
</style>
