<template>
  <div class="user-popup-wrapper clickable" @click="handleWrapperClick" @mouseenter="handleMouseEnter" @mouseleave="handleMouseLeave">
    <!-- Slot for trigger (e.g., avatar) -->
    <slot></slot>

    <teleport to="body" v-if="visible">
      <!-- Backdrop overlay to absorb background clicks on mobile/touch screens -->
      <div v-if="isTouch" class="popup-backdrop" @click.stop="hidePopup"></div>

      <!-- Popup container -->
      <div
        class="user-profile-popup"
        :class="{ 'display-below': displayBelow }"
        :style="popupStyle"
        @click.stop
        @mouseenter="clearTimer"
        @mouseleave="handleMouseLeave"
      >
      <div class="popup-loading" v-if="loading">Đang tải...</div>
      <div class="popup-content" v-else-if="userData">
        <!-- Real HTML elements for arrows to support dynamic positioning -->
        <div class="popup-arrow-border" :style="arrowStyle"></div>
        <div class="popup-arrow-fill" :style="arrowStyle"></div>

        <!-- Vùng ảnh bìa chứa thông tin đè lên -->
        <div 
          class="popup-banner-header"
          :style="userData.profileBanner ? { backgroundImage: `url(${userData.profileBanner})` } : { backgroundColor: '#edf6fd' }"
        >
          <div class="popup-header-content">
            <!-- Cột trái: avatar -->
            <div class="popup-avatar-col" @click="goToProfile">
              <div class="popup-avatar-large" :style="!isAvatarUrl(userData.avatar) ? { backgroundColor: userData.avatar || '#1a507a', color: '#fff' } : {}">
                <img v-if="isAvatarUrl(userData.avatar)" :src="userData.avatar" />
                <template v-else>
                  {{ (userData.displayName || userData.username || 'A').charAt(0).toUpperCase() }}
                </template>
              </div>
            </div>

            <!-- Cột phải: thông tin đè lên banner -->
            <div class="popup-meta-col">
              <div class="popup-displayname" @click="goToProfile">{{ userData.displayName || userData.username }}</div>
              <div class="popup-meta-box">
                <div class="popup-title-tag">Yếu sinh lý</div>
                <div class="popup-meta-item">Tham gia: {{ formatJoinDate(userData.createdAt) }}</div>
                <div class="popup-meta-item text-dimmed">{{ formatLastActive(userData.lastActiveAt || userData.createdAt) }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Bảng thống kê số liệu ngang -->
        <div class="popup-stats-bar">
          <div class="stat-box">
            <span class="stat-label">Bài viết</span>
            <span class="stat-val">{{ userData.postCount || 0 }}</span>
          </div>
          <div class="stat-box">
            <span class="stat-label">Điểm tương tác</span>
            <span class="stat-val">{{ userData.interactionPoints || 0 }}</span>
          </div>
          <div class="stat-box">
            <span class="stat-label">Điểm thành tích</span>
            <span class="stat-val">{{ userData.trophyPoints || 0 }}</span>
          </div>
        </div>

        <!-- Thanh nút hành động (ẩn khi là chính mình hoặc chưa đăng nhập) -->
        <div class="popup-actions" v-if="isLoggedIn && !isCurrentUser">
          <button class="btn-popup-action" @click="handleFollow">Theo dõi</button>
          <button class="btn-popup-action" @click="handleBlock">Chặn</button>
          <button class="btn-popup-action" @click="startConversation">Bắt đầu đối thoại</button>
        </div>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script>
import userService from '@/apps/Forum/services/user.service'
import { isAvatarUrl } from '@/shared/utils/utils'
import { formatForumDate } from '@/shared/utils/date'
import userMixin from '@/shared/mixins/user.mixin.js'

export default {
  name: 'UserProfilePopup',
  mixins: [userMixin],
  props: {
    user: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      visible: false,
      loading: false,
      userData: null,
      timer: null,
      displayBelow: false,
      popupStyle: {},
      arrowStyle: {},
      isTouch: false
    }
  },
  mounted() {
    this.isTouch = 'ontouchstart' in window || navigator.maxTouchPoints > 0
    window.addEventListener('scroll', this.handleScroll, { passive: true })
  },
  computed: {
    isCurrentUser() {
      return this.checkIsCurrentUser(this.user)
    },
    isLoggedIn() {
      try {
        return !!localStorage.getItem('user')
      } catch (e) {
        return false
      }
    }
  },
  beforeUnmount() {
    this.clearTimer()
    window.removeEventListener('scroll', this.handleScroll)
  },
  methods: {
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    handleWrapperClick(e) {
      if (!this.isTouch) return
      
      e.stopPropagation()
      if (this.visible) {
        this.hidePopup()
      } else {
        this.showPopup()
      }
    },
    handleMouseEnter() {
      if (this.isTouch) return
      
      this.clearTimer()
      this.timer = setTimeout(async () => {
        await this.showPopup()
      }, 350) // hover delay for professional user experience
    },
    handleMouseLeave() {
      if (this.isTouch) return
      
      this.clearTimer()
      this.timer = setTimeout(() => {
        this.hidePopup()
      }, 300) // leave delay allowing transition to popup
    },
    clearTimer() {
      if (this.timer) {
        clearTimeout(this.timer)
        this.timer = null
      }
    },
    handleScroll() {
      if (this.visible) {
        this.hidePopup()
      }
    },
    async showPopup() {
      if (this.$el) {
        const rect = this.$el.getBoundingClientRect()
        this.displayBelow = rect.top < 240
        
        const viewportWidth = window.innerWidth
        const viewportHeight = window.innerHeight
        const popupWidth = 320
        
        const avatarCenterX = rect.left + rect.width / 2
        let popupLeft = avatarCenterX - popupWidth / 2
        
        // boundary check for left/right screen edge
        if (popupLeft < 10) {
          popupLeft = 10
        } else if (popupLeft + popupWidth > viewportWidth - 10) {
          popupLeft = viewportWidth - 10 - popupWidth
        }
        
        const styles = {
          position: 'fixed',
          left: `${popupLeft}px`,
          transform: 'none',
          zIndex: 99999
        }
        
        if (this.displayBelow) {
          styles.top = `${rect.bottom + 10}px`
          styles.bottom = 'auto'
        } else {
          styles.bottom = `${viewportHeight - rect.top + 10}px`
          styles.top = 'auto'
        }
        
        this.popupStyle = styles
        this.arrowStyle = {
          left: `${avatarCenterX - popupLeft}px`
        }
      }
      
      this.visible = true
      if (!this.userData) {
        await this.fetchUserProfile()
      }
    },
    hidePopup() {
      this.clearTimer()
      this.visible = false
    },
    async fetchUserProfile() {
      this.loading = true
      try {
        const usernameParam = this.user.username
        if (!usernameParam) {
          this.userData = this.user
          return
        }
        const res = await userService.getPublicByName(usernameParam)
        if (res.data) {
          this.userData = res.data
        } else {
          this.userData = this.user
        }
      } catch (e) {
        console.error('Error fetching user profile:', e)
        this.userData = this.user
      } finally {
        this.loading = false
      }
    },
    formatJoinDate(dateStr) {
      if (!dateStr) return '29/8/2025'
      try {
        const d = new Date(dateStr)
        if (isNaN(d.getTime())) return '29/8/2025'
        const day = d.getDate()
        const month = d.getMonth() + 1
        const year = d.getFullYear()
        return `${day}/${month}/${year}`
      } catch (e) {
        return '29/8/2025'
      }
    },
    formatLastActive(dateStr) {
      if (!dateStr) return 'Thấy lần gần nhất: Không rõ'
      return `Thấy lần gần nhất: ${formatForumDate(dateStr)}`
    },
    startConversation() {
      this.visible = false
      document.removeEventListener('click', this.handleOutsideClick)
      const nameParam = this.userData.displayName || this.userData.username
      this.$router.push({
        name: 'AddConversation',
        query: { to: nameParam }
      })
    },
    goToProfile() {
      this.visible = false
      const usernameParam = this.userData?.username
      if (!usernameParam) return
      this.$router.push({
        name: 'UserProfile',
        query: { username: usernameParam }
      })
    },
    handleFollow() {
      alert('Tính năng Theo dõi sẽ được cập nhật sau.')
    },
    handleBlock() {
      alert('Tính năng Chặn sẽ được cập nhật sau.')
    }
  }
}
</script>

<style scoped>
.user-popup-wrapper {
  position: relative;
  display: inline-block;
  flex-shrink: 0;
}

.user-popup-wrapper.clickable {
  cursor: pointer;
}

.popup-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: transparent;
  z-index: 999;
}

.user-profile-popup {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 50%;
  transform: translateX(-50%);
  width: 320px;
  background-color: #ffffff;
  border: 1px solid #b3d1e6;
  border-radius: 4px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.15);
  z-index: 1000;
  padding: 0;
  text-align: left;
  font-family: Arial, sans-serif;
  overflow: hidden;
}

/* Arrow stylings using real HTML to support offsets */
.popup-arrow-border {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border-width: 7px;
  border-style: solid;
  border-color: #b3d1e6 transparent transparent transparent;
  margin-top: 1px;
  z-index: 1001;
}

.popup-arrow-fill {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border-width: 6px;
  border-style: solid;
  border-color: #ffffff transparent transparent transparent;
  z-index: 1002;
}

.popup-loading {
  font-size: 0.9rem;
  color: #666;
  text-align: center;
  padding: 20px 0;
}

/* Banner area with info overlay */
.popup-banner-header {
  height: 140px;
  background-size: cover;
  background-position: center;
  position: relative;
  border-top-left-radius: 4px;
  border-top-right-radius: 4px;
}

.popup-header-content {
  position: absolute;
  inset: 0;
  display: flex;
  padding: 12px;
  gap: 12px;
  background: rgba(0, 0, 0, 0.15); /* Soft dark overlay on banner */
  align-items: center;
}

.popup-avatar-col {
  flex-shrink: 0;
}

.popup-avatar-large {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 2.5px solid #ffffff;
  box-shadow: 0 2px 5px rgba(0,0,0,0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.8rem;
  font-weight: bold;
  overflow: hidden;
  background-color: #1a507a;
  color: #fff;
  cursor: pointer;
}

.popup-avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.popup-meta-col {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  min-width: 0;
}

.popup-displayname {
  font-size: 1.25rem;
  font-weight: 700;
  line-height: 1.2;
  color: #ffffff;
  margin-bottom: 4px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.8);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.popup-displayname:hover {
  text-decoration: underline;
}

/* Dark semi-transparent box for text info */
.popup-meta-box {
  background: rgba(0, 0, 0, 0.5);
  border-radius: 4px;
  padding: 6px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.popup-title-tag {
  font-size: 0.8rem;
  font-weight: 500;
  color: #e5edf5;
}

.popup-meta-item {
  font-size: 0.75rem;
  color: #dcdde1;
}

.popup-meta-item.text-dimmed {
  white-space: normal;
  word-break: break-word;
}

/* Stats Bar */
.popup-stats-bar {
  display: flex;
  background: #ffffff;
  border-top: 1px solid #d8dbe0;
  padding: 3px 15px;
  justify-content: space-between;
  align-items: center;
}

.stat-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  line-height: 1.1;
}

.stat-label {
  font-size: 0.7rem;
  color: #7f8c8d;
  margin-bottom: 2px;
}

.stat-val {
  font-size: 0.95rem;
  font-weight: 600;
  color: #1a507a;
}

/* Actions Row */
.popup-actions {
  display: flex;
  gap: 8px;
  padding: 6px 12px;
  background-color: #ffffff;
  border-top: 1px solid #d8dbe0;
}

.btn-popup-action {
  flex: 1;
  background-color: #ffffff;
  border: 1px solid #c8d4e0;
  color: #1a507a;
  font-weight: 500;
  padding: 4px 4px;
  border-radius: 3px;
  cursor: pointer;
  font-size: 0.75rem;
  transition: all 0.2s;
  font-family: inherit;
  outline: none;
  text-align: center;
  white-space: nowrap;
}

.btn-popup-action:hover {
  background-color: #1a507a;
  color: #ffffff;
  border-color: #1a507a;
}

/* Below placement adjustments */
.user-profile-popup.display-below {
  bottom: auto;
  top: calc(100% + 10px);
}

.user-profile-popup.display-below .popup-arrow-border {
  top: auto;
  bottom: 100%;
  border-color: transparent transparent #b3d1e6 transparent;
  margin-top: -1px;
}

.user-profile-popup.display-below .popup-arrow-fill {
  top: auto;
  bottom: 100%;
  border-color: transparent transparent #edf6fd transparent;
}
</style>
