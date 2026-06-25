<template>
  <div 
    class="reaction-btn-container"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <!-- The animated Tray -->
    <ReactionSelector 
      v-if="showTray && hasAvailableIcons"
      :icons="allIcons"
      @select="handleSelectReaction"
    />

    <!-- The Trigger Button -->
    <button 
      class="reaction-action-btn"
      :class="{ 'is-reacted': isReacted }"
      @click="handleClickButton"
    >
      <ReactionIcon 
        :code="displayIconCode"
        :color="activeColor"
        :grayscale="!isReacted && !isHovered"
        size="20px"
        class="btn-reaction-icon"
      />
      <span 
        class="btn-reaction-text"
        :style="textStyle"
      >
        {{ displayText }}
      </span>
    </button>
  </div>
</template>

<script>
import ReactionSelector from './ReactionSelector.vue'
import ReactionIcon from './ReactionIcon.vue'
import reactionService from '@/apps/Forum/services/reaction.service'
import { toastError } from '@/shared/utils/swal'

export default {
  name: 'ReactionButton',
  components: {
    ReactionSelector,
    ReactionIcon
  },
  props: {
    targetId: {
      default: null
    },
    type: {
      default: ''
    },
    allIcons: {
      default: () => []
    },
    userReaction: {
      default: null
    }
  },
  emits: ['reaction-changed'],
  data() {
    return {
      showTray: false,
      isHovered: false,
      hoverTimeout: null,
      leaveTimeout: null
    }
  },
  computed: {
    hasAvailableIcons() {
      return this.allIcons && this.allIcons.length > 0
    },
    defaultReaction() {
      // The very first reaction from the sorted allIcons array
      return this.hasAvailableIcons ? this.allIcons[0] : null
    },
    isReacted() {
      return !!this.userReaction
    },
    // Resolves what icon to render in the button
    displayIconCode() {
      if (this.isReacted) {
        return this.userReaction.icon
      }
      return this.defaultReaction ? this.defaultReaction.icon : 'like'
    },
    // Resolves what text label to render
    displayText() {
      if (this.isReacted) {
        return this.userReaction.tooltip
      }
      return this.defaultReaction ? this.defaultReaction.tooltip : 'Thích'
    },
    textStyle() {
      // When hovered or reacted, apply config colors
      if (this.isReacted) {
        return {
          color: this.activeColor,
          fontWeight: 'bold'
        }
      }
      if (this.isHovered) {
        return {
          color: this.activeColor
        }
      }
      return {}
    },
    activeColor() {
      if (this.isReacted) {
        return this.userReaction.color || '#1877f2'
      }
      return this.defaultReaction ? this.defaultReaction.color : '#1877f2'
    }
  },
  methods: {
    getReactionUrl(code) {
      if (!code) return ''
      try {
        return require(`@/assets/reactions/${code}.svg`)
      } catch (e) {
        // Return simple Like icon as fallback if assets not found
        return ''
      }
    },
    handleMouseEnter() {
      this.isHovered = true
      clearTimeout(this.leaveTimeout)
      this.hoverTimeout = setTimeout(() => {
        this.showTray = true
      }, 400) // 400ms facebook-style hover delay trigger
    },
    handleMouseLeave() {
      this.isHovered = false
      clearTimeout(this.hoverTimeout)
      this.leaveTimeout = setTimeout(() => {
        this.showTray = false
      }, 300)
    },
    async handleClickButton() {
      // Ensure auth token exists
      if (!localStorage.getItem('token')) {
        toastError('Bạn cần đăng nhập để thực hiện hành động này!')
        return
      }

      if (this.isReacted) {
        // Already reacted -> Toggle OFF (Remove reaction)
        this.$emit('reaction-updated', null)
        await this.removeReaction()
      } else {
        // Not reacted -> Toggle ON (Assign the first default reaction)
        if (this.defaultReaction) {
          this.$emit('reaction-updated', this.defaultReaction)
          await this.submitReaction(this.defaultReaction.id)
        }
      }
    },
    async handleSelectReaction(icon) {
      this.showTray = false
      clearTimeout(this.hoverTimeout)
      clearTimeout(this.leaveTimeout)
      
      this.$emit('reaction-updated', icon)
      await this.submitReaction(icon.id)
    },
    async submitReaction(iconId) {
      try {
        await reactionService.addReaction(this.type, this.targetId, iconId)
      } catch (error) {
        console.error('Lỗi khi gửi reaction:', error)
        const msg = error.response?.data?.message || 'Lỗi khi cập nhật cảm xúc'
        toastError(msg)
        this.$emit('reaction-failed')
      }
    },
    async removeReaction() {
      try {
        await reactionService.removeReaction(this.type, this.targetId)
      } catch (error) {
        console.error('Lỗi khi xóa reaction:', error)
        toastError('Không thể xóa cảm xúc')
        this.$emit('reaction-failed')
      }
    }
  }
}
</script>

<style scoped>
.reaction-btn-container {
  position: relative;
  display: inline-block;
  user-select: none;
}

.reaction-action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  color: #65676B; /* standard Facebook neutral gray */
  font-size: 14px;
  font-weight: 600;
  transition: background-color 0.2s, color 0.2s;
  outline: none;
}

.reaction-action-btn:hover {
  background-color: #f0f2f5;
}

/* CSS State: Hover cursor change & text underline */
.reaction-action-btn:hover .btn-reaction-text {
  text-decoration: underline;
}

/* Transition adjustments */
.btn-reaction-icon {
  transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

/* Hover scaling of the small icon */
.reaction-action-btn:hover .btn-reaction-icon {
  transform: scale(1.2);
}
</style>
