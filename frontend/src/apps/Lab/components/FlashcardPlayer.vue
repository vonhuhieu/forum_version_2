<template>
  <div class="flashcard-player-container">
    <div class="flashcard-header">
      <h2 class="flashcard-title">{{ product?.title || 'Bộ Thẻ Ghi Nhớ' }}</h2>
      <div class="flashcard-progress-wrapper" v-if="cards.length > 0">
        <div class="progress-bar-track">
          <div class="progress-bar-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <span class="progress-text">Thẻ {{ currentIndex + 1 }} / {{ cards.length }}</span>
      </div>
    </div>

    <div class="flashcard-stage" v-if="currentCard">
      <!-- Thẻ 3D -->
      <div
        class="card-3d-wrapper"
        :class="{ 'is-flipped': isFlipped }"
        @click="toggleFlip"
      >
        <!-- Mặt trước -->
        <div class="card-face card-front">
          <div class="card-tag">CÂU HỎI / KHÁI NIỆM</div>
          <div class="card-content-text">{{ currentCard.front }}</div>
          <div class="card-hint-text" v-if="currentCard.hint && showHint">
            💡 Gợi ý: {{ currentCard.hint }}
          </div>
          <div class="card-footer-tip">Chạm để lật mặt đáp án ↺</div>
        </div>

        <!-- Mặt sau -->
        <div class="card-face card-back">
          <div class="card-tag tag-success">ĐÁP ÁN / Ý NGHĨA</div>
          <div class="card-content-text">{{ currentCard.back }}</div>
          <div class="card-footer-tip">Chạm để quay lại mặt trước ↻</div>
        </div>
      </div>

      <!-- Thanh điều khiển -->
      <div class="flashcard-controls">
        <button
          class="btn-control"
          :disabled="currentIndex === 0"
          @click="prevCard"
          title="Thẻ trước"
        >
          <i class="bi bi-chevron-left"></i> Trước
        </button>

        <button
          class="btn-control btn-hint"
          v-if="currentCard.hint"
          @click.stop="showHint = !showHint"
        >
          <i class="bi bi-lightbulb"></i> {{ showHint ? 'Ẩn gợi ý' : 'Gợi ý' }}
        </button>

        <button
          class="btn-control"
          :disabled="currentIndex === cards.length - 1"
          @click="nextCard"
          title="Thẻ tiếp theo"
        >
          Tiếp <i class="bi bi-chevron-right"></i>
        </button>

        <button
          class="btn-control btn-shuffle"
          @click="shuffleCards"
          title="Xáo trộn thẻ"
        >
          <i class="bi bi-shuffle"></i> Xáo trộn
        </button>
      </div>
    </div>

    <div v-else class="text-center text-muted py-5">
      <p>Chưa có dữ liệu thẻ bài nào trong sản phẩm này.</p>
    </div>
  </div>
</template>

<script>
import { shuffleArray } from '@/shared/utils/utils'

export default {
  name: 'FlashcardPlayer',
  props: {
    product: {
      type: Object,
      required: true
    },
    rows: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      currentIndex: 0,
      isFlipped: false,
      showHint: false,
      deck: []
    }
  },
  computed: {
    cards() {
      return this.deck
    },
    currentCard() {
      if (this.cards.length === 0) return null
      return this.cards[this.currentIndex] || null
    },
    progressPercent() {
      if (this.cards.length === 0) return 0
      return Math.round(((this.currentIndex + 1) / this.cards.length) * 100)
    }
  },
  watch: {
    rows: {
      immediate: true,
      handler(newRows) {
        this.deck = (newRows || []).map((row, idx) => {
          const d = row.data || {}
          return {
            id: row.id || idx,
            front: d.front || d.question || `Thẻ số ${idx + 1}`,
            back: d.back || d.answer || 'Chưa có nội dung mặt sau',
            hint: d.hint || ''
          }
        })
        this.currentIndex = 0
        this.isFlipped = false
        this.showHint = false
      }
    }
  },
  mounted() {
    window.addEventListener('keydown', this.handleKeydown)
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.handleKeydown)
  },
  methods: {
    toggleFlip() {
      this.isFlipped = !this.isFlipped
    },
    nextCard() {
      if (this.currentIndex < this.cards.length - 1) {
        this.isFlipped = false
        this.showHint = false
        this.currentIndex++
      }
    },
    prevCard() {
      if (this.currentIndex > 0) {
        this.isFlipped = false
        this.showHint = false
        this.currentIndex--
      }
    },
    shuffleCards() {
      this.deck = shuffleArray(this.deck)
      this.currentIndex = 0
      this.isFlipped = false
      this.showHint = false
    },
    handleKeydown(e) {
      if (e.key === ' ' || e.key === 'Enter') {
        e.preventDefault()
        this.toggleFlip()
      } else if (e.key === 'ArrowRight') {
        this.nextCard()
      } else if (e.key === 'ArrowLeft') {
        this.prevCard()
      }
    }
  }
}
</script>

<style scoped>
.flashcard-player-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  padding: 2rem 1rem;
  background: radial-gradient(circle at center, #1e293b 0%, #0f172a 100%);
  color: #ffffff;
}

.flashcard-header {
  text-align: center;
  margin-bottom: 2rem;
  width: 100%;
  max-width: 560px;
}

.flashcard-title {
  font-size: 2rem;
  font-weight: 800;
  background: linear-gradient(135deg, #38bdf8 0%, #818cf8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 1rem;
}

.flashcard-progress-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-bar-track {
  flex: 1;
  height: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #38bdf8, #818cf8);
  border-radius: 999px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.85rem;
  color: #94a3b8;
  white-space: nowrap;
}

.flashcard-stage {
  width: 100%;
  max-width: 560px;
  perspective: 1200px;
}

.card-3d-wrapper {
  width: 100%;
  min-height: 320px;
  position: relative;
  transform-style: preserve-3d;
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.card-3d-wrapper.is-flipped {
  transform: rotateY(180deg);
}

.card-face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  border-radius: 20px;
  padding: 2.5rem 2rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 20px 40px -15px rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.card-front {
  background: linear-gradient(145deg, #1e293b 0%, #0f172a 100%);
  border-top: 3px solid #38bdf8;
}

.card-back {
  background: linear-gradient(145deg, #064e3b 0%, #022c22 100%);
  border-top: 3px solid #10b981;
  transform: rotateY(180deg);
}

.card-tag {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 1px;
  color: #38bdf8;
}

.tag-success {
  color: #34d399;
}

.card-content-text {
  font-size: 1.35rem;
  font-weight: 600;
  line-height: 1.6;
  margin: 1.5rem 0;
  word-break: break-word;
}

.card-hint-text {
  background: rgba(245, 158, 11, 0.15);
  border-left: 3px solid #f59e0b;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 0.9rem;
  color: #fde68a;
  margin-top: 0.5rem;
}

.card-footer-tip {
  font-size: 0.8rem;
  color: #64748b;
  text-align: center;
}

.flashcard-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 2rem;
  flex-wrap: wrap;
}

.btn-control {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #ffffff;
  padding: 10px 20px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-control:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.18);
  transform: translateY(-2px);
}

.btn-control:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.btn-hint {
  border-color: rgba(245, 158, 11, 0.4);
  color: #fde68a;
}

.btn-shuffle {
  border-color: rgba(129, 140, 248, 0.4);
  color: #c7d2fe;
}
</style>
