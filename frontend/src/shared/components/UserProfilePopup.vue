<template>
  <div class="user-popup-wrapper" :class="{ 'clickable': !isCurrentUser }" @mouseenter="handleMouseEnter" @mouseleave="handleMouseLeave">
    <!-- Slot for trigger (e.g., avatar) -->
    <slot></slot>

    <!-- Popup container -->
    <div
      v-if="visible"
      class="user-profile-popup"
      @mouseenter="clearTimer"
      @mouseleave="handleMouseLeave"
    >
      <div class="popup-loading" v-if="loading">Đang tải...</div>
      <div class="popup-content" v-else-if="userData">
        <div class="popup-top">
          <!-- Large circular avatar -->
          <div class="popup-avatar" :style="!isAvatarUrl(userData.avatar) ? { backgroundColor: userData.avatar || '#3f51b5', color: '#fff' } : {}">
            <img v-if="isAvatarUrl(userData.avatar)" :src="userData.avatar" />
            <template v-else>
              {{ (userData.displayName || userData.username || 'A').charAt(0).toUpperCase() }}
            </template>
          </div>
          <!-- Info Details -->
          <div class="popup-info">
            <div class="popup-name">{{ userData.displayName || userData.username }}</div>
            <div class="popup-status">Thành viên</div>
            <div class="popup-joined">Tham gia: {{ formatJoinDate(userData.createdAt) }}</div>
          </div>
        </div>

        <div class="popup-actions">
          <button class="btn-start-convo" @click="startConversation">Bắt đầu đối thoại</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import userService from '@/apps/Forum/services/user.service'
import { isAvatarUrl } from '@/shared/utils/utils'

export default {
  name: 'UserProfilePopup',
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
      timer: null
    }
  },
  computed: {
    isCurrentUser() {
      try {
        const userStr = localStorage.getItem('user')
        if (userStr && this.user) {
          const u = JSON.parse(userStr)
          return u.id === this.user.id || u.username === this.user.username
        }
      } catch (e) {}
      return false
    }
  },
  beforeUnmount() {
    this.clearTimer()
  },
  methods: {
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    handleMouseEnter() {
      if (this.isCurrentUser) return
      this.clearTimer()
      this.timer = setTimeout(async () => {
        this.visible = true
        if (!this.userData) {
          await this.fetchUserProfile()
        }
      }, 350) // hover delay for professional user experience
    },
    handleMouseLeave() {
      this.clearTimer()
      this.timer = setTimeout(() => {
        this.visible = false
      }, 300) // leave delay allowing transition to popup
    },
    clearTimer() {
      if (this.timer) {
        clearTimeout(this.timer)
        this.timer = null
      }
    },
    async fetchUserProfile() {
      this.loading = true
      try {
        const usernameParam = this.user.username
        if (!usernameParam) {
          this.userData = this.user
          return
        }
        const res = await userService.getByName(usernameParam)
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
    startConversation() {
      this.visible = false
      const nameParam = this.userData.displayName || this.userData.username
      this.$router.push({
        name: 'AddConversation',
        query: { to: nameParam }
      })
    }
  }
}
</script>

<style scoped>
.user-popup-wrapper {
  position: relative;
  display: inline-block;
}

.user-popup-wrapper.clickable {
  cursor: pointer;
}

.user-profile-popup {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 50%;
  transform: translateX(-50%);
  width: 280px;
  background-color: #ebf4f9;
  border: 1px solid #b3d1e6;
  border-radius: 4px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.15);
  z-index: 1000;
  padding: 15px;
  text-align: left;
  font-family: Arial, sans-serif;
}

.user-profile-popup::after {
  content: "";
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border-width: 6px;
  border-style: solid;
  border-color: #ebf4f9 transparent transparent transparent;
}

.user-profile-popup::before {
  content: "";
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border-width: 7px;
  border-style: solid;
  border-color: #b3d1e6 transparent transparent transparent;
  margin-top: 1px;
}

.popup-loading {
  font-size: 0.9rem;
  color: #666;
  text-align: center;
  padding: 10px 0;
}

.popup-top {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.popup-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.8rem;
  font-weight: bold;
  flex-shrink: 0;
  border: 1px solid #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.popup-info {
  margin-left: 15px;
  flex-grow: 1;
}

.popup-name {
  color: #2577b1;
  font-size: 1.1rem;
  font-weight: bold;
  line-height: 1.2;
  margin-bottom: 2px;
}

.popup-status {
  color: #666;
  font-size: 0.85rem;
  margin-bottom: 2px;
}

.popup-joined {
  color: #888;
  font-size: 0.8rem;
}

.popup-actions {
  border-top: 1px solid #c8d7e1;
  padding-top: 10px;
  text-align: right;
}

.btn-start-convo {
  background-color: #fff;
  border: 1px solid #c8d7e1;
  color: #2577b1;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 3px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.2s ease;
  font-family: inherit;
  outline: none;
}

.btn-start-convo:hover {
  background-color: #2577b1;
  color: #fff;
  border-color: #2577b1;
}
</style>
