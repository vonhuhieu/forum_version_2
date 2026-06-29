<template>
  <div class="reaction-selector-tray" @click.stop>
    <div 
      v-for="(icon, idx) in icons" 
      :key="icon.id" 
      class="reaction-item"
      :style="{ animationDelay: `${idx * 40}ms` }"
      @click.stop="$emit('select', icon)"
    >
      <!-- Hover Tooltip -->
      <div class="reaction-tooltip" :style="{ backgroundColor: '#333', color: '#fff' }">
        {{ icon.tooltip }}
      </div>
      
      <!-- Icon Vector -->
      <div class="reaction-img-container">
        <ReactionIcon :code="icon.icon" :color="icon.color" size="38px" />
      </div>
    </div>
  </div>
</template>

<script>
import ReactionIcon from './ReactionIcon.vue'

export default {
  name: 'ReactionSelector',
  components: {
    ReactionIcon
  },
  props: {
    icons: {
      type: Array,
      required: true,
      default: () => []
    }
  }
}
</script>

<style scoped>
.reaction-selector-tray {
  display: flex;
  align-items: center;
  background: white;
  border-radius: 30px;
  padding: 6px 10px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
  border: 1px solid #e4e6eb;
  position: absolute;
  bottom: 100%;
  left: 0;
  margin-bottom: 8px;
  z-index: 1000;
  gap: 6px;
  /* Slide in animation */
  animation: slideInUp 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
  pointer-events: auto;
}

.reaction-item {
  position: relative;
  cursor: pointer;
  padding: 4px;
  transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  opacity: 0;
  animation: popItem 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
}

.reaction-img-container {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.reaction-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

/* Hover Scaling Effects - Target internal container to bypass keyframe forwards blocking */
.reaction-item:hover {
  z-index: 10;
}

.reaction-item:hover .reaction-img-container {
  transform: scale(1.4) translateY(-8px);
}

.reaction-item:hover .reaction-tooltip {
  visibility: visible;
  opacity: 1;
  transform: translate(-50%, -18px); /* Raised up further to float clear above scaled icon */
}

/* Tooltip Styling */
.reaction-tooltip {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translate(-50%, 0);
  padding: 4px 8px;
  font-size: 12px;
  font-weight: bold;
  border-radius: 12px;
  white-space: nowrap;
  pointer-events: none;
  visibility: hidden;
  opacity: 0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
  transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  z-index: 20; /* Ensure tooltip stays on very top level */
}

/* Keyframe Animations */
@keyframes slideInUp {
  0% {
    opacity: 0;
    transform: translateY(15px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes popItem {
  0% {
    opacity: 0;
    transform: scale(0.5);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

@media (max-width: 767px) {
  .reaction-selector-tray {
    left: auto !important;
    right: -81px !important;
  }
}
</style>
