<template>
  <Transition name="loading-fade">
    <div v-if="visible" class="loading-overlay" aria-live="polite" aria-label="Đang tải dữ liệu...">
      <div class="loading-backdrop"></div>
      <div class="loading-spinner-wrapper">
        <div class="loading-ring">
          <div></div>
          <div></div>
          <div></div>
          <div></div>
        </div>
        <p class="loading-text">{{ text || 'Đang tải...' }}</p>
      </div>
    </div>
  </Transition>
</template>

<script>
export default {
  name: 'Loading',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    text: {
      type: String,
      default: 'Đang tải...'
    }
  }
}
</script>

<style scoped>
.loading-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Backdrop mờ */
.loading-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
}

/* Wrapper spinner nằm trên backdrop */
.loading-spinner-wrapper {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

/* Ring spinner */
.loading-ring {
  display: inline-block;
  position: relative;
  width: 64px;
  height: 64px;
}

.loading-ring div {
  box-sizing: border-box;
  display: block;
  position: absolute;
  width: 52px;
  height: 52px;
  margin: 6px;
  border: 5px solid transparent;
  border-radius: 50%;
  animation: loading-ring-spin 1.1s cubic-bezier(0.5, 0, 0.5, 1) infinite;
}

.loading-ring div:nth-child(1) {
  border-top-color: #1a507a;
  animation-delay: -0.3s;
}

.loading-ring div:nth-child(2) {
  border-top-color: #3498db;
  animation-delay: -0.2s;
}

.loading-ring div:nth-child(3) {
  border-top-color: #7cb3db;
  animation-delay: -0.1s;
}

.loading-ring div:nth-child(4) {
  border-top-color: #b8d9f0;
  animation-delay: 0s;
}

@keyframes loading-ring-spin {
  0%   { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Text */
.loading-text {
  font-size: 0.9rem;
  color: #1a507a;
  font-weight: 500;
  letter-spacing: 0.5px;
  margin: 0;
  animation: loading-text-pulse 1.4s ease-in-out infinite;
}

@keyframes loading-text-pulse {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.4; }
}

/* Transition fade in/out */
.loading-fade-enter-active,
.loading-fade-leave-active {
  transition: opacity 0.25s ease;
}

.loading-fade-enter-from,
.loading-fade-leave-to {
  opacity: 0;
}
</style>
