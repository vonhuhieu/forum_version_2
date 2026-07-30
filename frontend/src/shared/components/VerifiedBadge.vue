<template>
  <span 
    v-if="isVisible" 
    class="verified-badge-wrapper" 
    :title="titleText"
  >
    <svg 
      class="verified-badge-icon" 
      :style="{ width: size, height: size }" 
      viewBox="0 0 24 24" 
      fill="none" 
      xmlns="http://www.w3.org/2000/svg"
    >
      <circle cx="12" cy="12" r="10" fill="#1877F2"/>
      <path d="M8.5 12.5L10.5 14.5L15.5 9.5" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
  </span>
</template>

<script>
export default {
  name: 'VerifiedBadge',
  props: {
    user: {
      type: Object,
      default: () => null
    },
    show: {
      type: Boolean,
      default: false
    },
    size: {
      type: String,
      default: '24px'
    },
    titleText: {
      type: String,
      default: 'Tài khoản Uy tín / Quản trị viên'
    }
  },
  computed: {
    isVisible() {
      if (this.show) return true
      if (!this.user) return false
      if (this.user.isVerifiedBadge) return true
      if (Array.isArray(this.user.roles)) {
        if (this.user.roles.includes('ROLE_SUPER_ADMIN') || this.user.roles.includes('ROLE_ADMIN')) {
          return true
        }
      }
      return false
    }
  }
}
</script>

<style scoped>
.verified-badge-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
  line-height: 1;
  margin-left: 4px;
  pointer-events: none; /* Coi displayName và tích xanh như một block đồng bộ 100% khi hover/click */
  user-select: none;
}

.verified-badge-icon {
  display: inline-block;
  vertical-align: middle;
  flex-shrink: 0;
  pointer-events: none;
  width: 24px !important;
  height: 24px !important;
}
</style>
