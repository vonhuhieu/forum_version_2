<template>
  <div class="category-icon-wrapper" :class="customClass">
    <!-- 1. Render SVG động trực tiếp từ Database / File SQL (Không hardcode bất kỳ icon nào) -->
    <span v-if="isSvg" class="category-svg-icon" v-html="sanitizedSvg"></span>

    <!-- 2. Render ảnh nếu icon là đường dẫn URL / File ảnh -->
    <img v-else-if="isImageUrl" :src="icon" alt="icon" class="category-img-icon" />

    <!-- 3. Render Emoji hoặc Ký tự nếu icon là emoji / text ngắn -->
    <span v-else-if="isTextOrEmoji" class="category-emoji">{{ icon }}</span>

    <!-- 4. Fallback mặc định an toàn: Icon chat tiêu chuẩn (chỉ hiện khi icon null hoặc để trống) -->
    <svg v-else xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#f39c12" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-msg">
      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
    </svg>
  </div>
</template>

<script>
export default {
  name: 'CategoryIcon',
  props: {
    icon: {
      type: String,
      default: ''
    },
    customClass: {
      type: String,
      default: ''
    }
  },
  computed: {
    isSvg() {
      if (!this.icon) return false
      const trimmed = this.icon.trim().toLowerCase()
      return trimmed.startsWith('<svg') || trimmed.includes('<svg')
    },
    sanitizedSvg() {
      if (!this.icon) return ''
      return this.icon.trim()
    },
    isImageUrl() {
      if (!this.icon) return false
      const trimmed = this.icon.trim().toLowerCase()
      return trimmed.startsWith('http://') || 
             trimmed.startsWith('https://') || 
             trimmed.startsWith('/') || 
             trimmed.endsWith('.png') || 
             trimmed.endsWith('.svg') || 
             trimmed.endsWith('.jpg') || 
             trimmed.endsWith('.webp')
    },
    isTextOrEmoji() {
      return Boolean(this.icon && this.icon.trim())
    }
  }
}
</script>

<style scoped>
.category-icon-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.category-svg-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 0;
}

.category-svg-icon :deep(svg) {
  display: block;
  width: 22px;
  height: 22px;
  transition: transform 0.2s ease;
}

.category-img-icon {
  width: 22px;
  height: 22px;
  object-fit: contain;
}

.category-emoji {
  font-size: 20px;
  line-height: 1;
  display: inline-block;
  user-select: none;
}

.category-row:hover .category-icon-wrapper {
  transform: scale(1.12);
}
</style>
