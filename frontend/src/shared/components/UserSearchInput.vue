<template>
  <div class="user-search-input-wrapper" ref="wrapperRef">
    <input 
      type="text" 
      class="user-search-input"
      :class="[inputClass, { 'is-borderless': borderless }]"
      :style="inputStyle"
      :placeholder="placeholder" 
      v-model="keyword"
      @focus="onFocus"
      @input="handleInput"
      @keydown.down.prevent="navigateDown"
      @keydown.up.prevent="navigateUp"
      @keydown.enter.prevent="selectHighlighted"
      @keydown.esc="closeDropdown"
    />

    <div v-if="showDropdown && searchResults.length > 0" class="user-autocomplete-dropdown" ref="dropdownRef">
      <div 
        v-for="(user, index) in searchResults" 
        :key="user.id || user.username || index" 
        class="user-autocomplete-item"
        :class="{ 'is-highlighted': index === selectedIndex }"
        @mouseenter="selectedIndex = index"
        @click="selectUser(user)"
      >
        <div class="user-avatar-circle" :style="!isAvatarUrl(user.avatar) ? { backgroundColor: getAvatarColor(user) } : {}">
          <img v-if="isAvatarUrl(user.avatar)" :src="user.avatar" alt="avatar" />
          <span v-else>{{ (user.displayName || user.username || '?').charAt(0).toUpperCase() }}</span>
        </div>
        <div class="user-info-text">
          <span class="user-display-name">{{ user.displayName || user.username }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import userService from '@/apps/Forum/services/user.service'
import { isAvatarUrl } from '@/shared/utils/utils'

export default {
  name: 'UserSearchInput',
  props: {
    placeholder: {
      type: String,
      default: 'Tên...'
    },
    isPublic: {
      type: Boolean,
      default: true
    },
    clearOnSelect: {
      type: Boolean,
      default: false
    },
    borderless: {
      type: Boolean,
      default: false
    },
    excludeUsernames: {
      type: Array,
      default: () => []
    },
    inputClass: {
      type: String,
      default: ''
    },
    inputStyle: {
      type: [Object, String],
      default: null
    },
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['select', 'update:modelValue'],
  data() {
    return {
      keyword: this.modelValue || '',
      searchResults: [],
      showDropdown: false,
      selectedIndex: -1,
      loading: false,
      searchTimeout: null,
      requestId: 0
    }
  },
  watch: {
    modelValue(val) {
      if (val !== this.keyword) {
        this.keyword = val
      }
    },
    selectedIndex() {
      this.scrollToHighlighted()
    }
  },
  mounted() {
    document.addEventListener('click', this.handleClickOutside)
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleClickOutside)
  },
  methods: {
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    onFocus() {
      if (this.keyword && this.keyword.trim() && this.searchResults.length > 0) {
        this.showDropdown = true
      }
    },
    handleInput() {
      this.$emit('update:modelValue', this.keyword)
      if (!this.keyword || !this.keyword.trim()) {
        this.searchResults = []
        this.showDropdown = false
        this.selectedIndex = -1
        return
      }
      this.showDropdown = true
      clearTimeout(this.searchTimeout)
      this.searchTimeout = setTimeout(() => {
        this.fetchUsers()
      }, 300)
    },
    async fetchUsers() {
      if (!this.keyword || !this.keyword.trim()) {
        this.searchResults = []
        this.showDropdown = false
        return
      }
      const currentReqId = ++this.requestId
      this.loading = true
      try {
        const searchFn = this.isPublic
          ? userService.searchPublic({ keyword: this.keyword, page: 0, size: 10 })
          : userService.search({ keyword: this.keyword, page: 0, size: 10 })
        const res = await searchFn
        if (currentReqId === this.requestId && res.data) {
          const content = res.data.content || []
          this.searchResults = content.filter(u => !this.excludeUsernames.includes(u.username))
          this.selectedIndex = this.searchResults.length > 0 ? 0 : -1
          this.showDropdown = this.searchResults.length > 0
        }
      } catch (e) {
        console.error('Lỗi khi tìm kiếm người dùng:', e)
      } finally {
        if (currentReqId === this.requestId) {
          this.loading = false
        }
      }
    },
    navigateDown() {
      if (!this.showDropdown || this.searchResults.length === 0) return
      if (this.selectedIndex < this.searchResults.length - 1) {
        this.selectedIndex++
      } else {
        this.selectedIndex = 0
      }
    },
    navigateUp() {
      if (!this.showDropdown || this.searchResults.length === 0) return
      if (this.selectedIndex > 0) {
        this.selectedIndex--
      } else {
        this.selectedIndex = this.searchResults.length - 1
      }
    },
    scrollToHighlighted() {
      this.$nextTick(() => {
        if (!this.$refs.dropdownRef) return
        const highlightedEl = this.$refs.dropdownRef.querySelector('.user-autocomplete-item.is-highlighted')
        if (highlightedEl) {
          highlightedEl.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
        }
      })
    },
    selectHighlighted() {
      if (!this.showDropdown || this.searchResults.length === 0) return
      const targetIdx = this.selectedIndex >= 0 && this.selectedIndex < this.searchResults.length 
        ? this.selectedIndex 
        : 0
      const user = this.searchResults[targetIdx]
      if (user) {
        this.selectUser(user)
      }
    },
    selectUser(user) {
      this.$emit('select', user)
      if (this.clearOnSelect) {
        this.keyword = ''
        this.$emit('update:modelValue', '')
      } else {
        this.keyword = user.displayName || user.username
        this.$emit('update:modelValue', this.keyword)
      }
      this.showDropdown = false
      this.searchResults = []
      this.selectedIndex = -1
    },
    closeDropdown() {
      this.showDropdown = false
      this.selectedIndex = -1
    },
    handleClickOutside(e) {
      if (this.$refs.wrapperRef && !this.$refs.wrapperRef.contains(e.target)) {
        this.closeDropdown()
      }
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
    }
  }
}
</script>

<style scoped>
.user-search-input-wrapper {
  position: relative;
  width: 100%;
}

.user-search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d8dbe0;
  border-radius: 4px;
  font-size: 0.95rem;
  outline: none;
  background-color: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.user-search-input:focus {
  border-color: #1a507a;
  box-shadow: 0 0 0 2px rgba(26, 80, 122, 0.15);
}

.user-search-input.is-borderless {
  border: none !important;
  box-shadow: none !important;
  padding: 4px 6px;
  background: transparent;
}

.user-search-input.is-borderless:focus {
  border: none !important;
  box-shadow: none !important;
}

.user-autocomplete-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: #ffffff;
  border: 1px solid #c8d4e0;
  border-radius: 4px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  max-height: 280px;
  overflow-y: auto;
}

.user-autocomplete-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.user-autocomplete-item:hover,
.user-autocomplete-item.is-highlighted {
  background-color: #eef6fc;
}

.user-avatar-circle {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  color: #ffffff;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
  overflow: hidden;
  background-color: #1a507a;
}

.user-avatar-circle img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-info-text {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  white-space: nowrap;
}

.user-display-name {
  font-size: 0.92rem;
  font-weight: 500;
  color: #1a507a;
  text-overflow: ellipsis;
  overflow: hidden;
}

.user-username-tag {
  font-size: 0.82rem;
  color: #7f8c8d;
}
</style>
