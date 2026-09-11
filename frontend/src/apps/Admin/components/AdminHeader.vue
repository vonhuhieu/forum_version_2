<template>
  <header class="admin-header">
    <div class="header-left">
      <div class="logo" @click="$router.push({ name: 'Home' })">HTXSL</div>
    </div>
    <div class="header-right">
      <div class="user-info" v-if="currentUser">
        <span class="user-avatar" :style="!isAvatarUrl(currentUser.avatar) ? { backgroundColor: currentUser.avatar || '#fff', color: currentUser.avatar ? '#fff' : '#1a507a' } : {}">
          <img v-if="isAvatarUrl(currentUser.avatar)" :src="formatAvatarUrl(currentUser.avatar)" style="width: 100%; height: 100%; object-fit: cover; border-radius: 50%;" />
          <template v-else>
            {{ (currentUser.displayName || currentUser.username).charAt(0).toUpperCase() }}
          </template>
        </span>
        <span class="user-name">Chào, {{ currentUser.displayName || currentUser.username }}</span>
      </div>
    </div>
  </header>
</template>

<script>
import { isAvatarUrl, formatAvatarUrl } from '@/shared/utils/utils'

export default {
  name: 'AdminHeader',
  data() {
    return {
      currentUser: null
    }
  },
  mounted() {
    const user = localStorage.getItem('user')
    if (user) {
      this.currentUser = JSON.parse(user)
    }
  },
  methods: {
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    formatAvatarUrl(avatar) {
      return formatAvatarUrl(avatar)
    }
  }
}
</script>

<style scoped>
.admin-header {
  height: 60px;
  background-color: #1a507a;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
  color: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.logo {
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: 1px;
  cursor: pointer;
  color: white;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  background: white;
  color: #1a507a;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: bold;
}

.user-name {
  font-size: 0.9rem;
  color: white;
  font-weight: 500;
}
</style>
