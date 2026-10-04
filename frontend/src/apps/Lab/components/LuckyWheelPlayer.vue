<template>
  <div class="wheel-player-container">
    <div class="wheel-header">
      <h2 class="wheel-title">{{ product?.title || 'Vòng Quay May Mắn' }}</h2>
      <p class="wheel-subtitle" v-if="items.length > 0">Có {{ items.length }} lựa chọn sẵn sàng</p>
      <p class="wheel-subtitle text-warning" v-else>Chưa có mục nào trong vòng quay</p>
    </div>

    <div class="wheel-stage" v-if="items.length > 0">
      <!-- Mũi tên chỉ vị trí thưởng ở đỉnh vòng quay -->
      <div class="wheel-pointer">
        <div class="pointer-arrow"></div>
      </div>

      <!-- Canvas vẽ vòng quay -->
      <canvas
        ref="wheelCanvas"
        class="wheel-canvas"
        :width="canvasSize"
        :height="canvasSize"
      ></canvas>

      <!-- Nút quay ở tâm -->
      <button
        class="spin-center-button"
        :disabled="isSpinning"
        @click="startSpin"
      >
        <span>{{ isSpinning ? 'ĐANG QUAY' : 'QUAY NGAY' }}</span>
      </button>
    </div>

    <!-- Kết quả trúng thưởng -->
    <div v-if="winner" class="winner-modal-backdrop" @click="closeWinner">
      <div class="winner-modal-card" @click.stop>
        <div class="winner-confetti">🎉 🏆 🎉</div>
        <h3 class="winner-heading">Chúc Mừng!</h3>
        <div class="winner-prize" :style="{ color: winner.color || '#f59e0b' }">
          {{ winner.label }}
        </div>
        <div class="winner-actions">
          <button class="btn btn-primary btn-lg" @click="closeWinner">Quay Tiếp</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { LAB_WHEEL_DEFAULT_COLORS } from '@/shared/utils/constants'
import { pickWeightedIndex } from '@/shared/utils/utils'

export default {
  name: 'LuckyWheelPlayer',
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
      canvasSize: 500,
      currentAngle: 0,
      isSpinning: false,
      winner: null,
      animationFrameId: null
    }
  },
  computed: {
    items() {
      if (!this.rows || this.rows.length === 0) return []
      return this.rows.map((row, index) => {
        const d = row.data || {}
        return {
          id: row.id || index,
          label: d.label || d.name || `Mục ${index + 1}`,
          color: d.color || LAB_WHEEL_DEFAULT_COLORS[index % LAB_WHEEL_DEFAULT_COLORS.length],
          weight: Number(d.weight) > 0 ? Number(d.weight) : 1
        }
      })
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.drawWheel()
    })
  },
  beforeUnmount() {
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId)
    }
  },
  watch: {
    items() {
      this.drawWheel()
    }
  },
  methods: {
    drawWheel() {
      const canvas = this.$refs.wheelCanvas
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      const size = this.canvasSize
      const center = size / 2
      const radius = center - 15

      ctx.clearRect(0, 0, size, size)

      if (this.items.length === 0) return

      const arc = (2 * Math.PI) / this.items.length

      ctx.save()
      ctx.translate(center, center)
      ctx.rotate(this.currentAngle)

      // Vẽ các lát cắt
      this.items.forEach((item, index) => {
        const startAngle = index * arc
        const endAngle = startAngle + arc

        ctx.beginPath()
        ctx.fillStyle = item.color
        ctx.moveTo(0, 0)
        ctx.arc(0, 0, radius, startAngle, endAngle)
        ctx.lineTo(0, 0)
        ctx.fill()

        ctx.strokeStyle = '#ffffff'
        ctx.lineWidth = 3
        ctx.stroke()

        // Vẽ chữ
        ctx.save()
        ctx.rotate(startAngle + arc / 2)
        ctx.textAlign = 'right'
        ctx.fillStyle = '#ffffff'
        ctx.font = 'bold 16px Inter, sans-serif'
        ctx.shadowColor = 'rgba(0, 0, 0, 0.45)'
        ctx.shadowBlur = 4

        const text = item.label.length > 18 ? item.label.substring(0, 18) + '...' : item.label
        ctx.fillText(text, radius - 25, 6)
        ctx.restore()
      })

      // Vòng tròn viền ngoài
      ctx.beginPath()
      ctx.arc(0, 0, radius, 0, 2 * Math.PI)
      ctx.strokeStyle = '#ffffff'
      ctx.lineWidth = 6
      ctx.stroke()

      ctx.restore()
    },
    startSpin() {
      if (this.isSpinning || this.items.length === 0) return
      this.isSpinning = true
      this.winner = null

      const weights = this.items.map(item => item.weight)
      const targetIndex = pickWeightedIndex(weights)
      const arc = (2 * Math.PI) / this.items.length

      // Mũi tên ở đỉnh (góc 3*PI/2 hay -PI/2)
      // Góc cần quay đến
      const targetAngleWithin = targetIndex * arc + arc / 2
      const totalRounds = 6 + Math.floor(Math.random() * 3) // 6-8 vòng quay
      const targetStopAngle = (1.5 * Math.PI) - targetAngleWithin
      const deltaAngle = (2 * Math.PI * totalRounds) + (targetStopAngle - (this.currentAngle % (2 * Math.PI))) + (2 * Math.PI)

      const duration = 5000 // 5 giây
      const startAngle = this.currentAngle
      const startTime = performance.now()

      const animate = (currentTime) => {
        const elapsed = currentTime - startTime
        const progress = Math.min(elapsed / duration, 1)

        // Easing cubic out
        const easeOut = 1 - Math.pow(1 - progress, 3)
        this.currentAngle = startAngle + deltaAngle * easeOut
        this.drawWheel()

        if (progress < 1) {
          this.animationFrameId = requestAnimationFrame(animate)
        } else {
          this.isSpinning = false
          this.winner = this.items[targetIndex]
        }
      }

      this.animationFrameId = requestAnimationFrame(animate)
    },
    closeWinner() {
      this.winner = null
    }
  }
}
</script>

<style scoped>
.wheel-player-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  padding: 2rem 1rem;
  background: radial-gradient(circle at center, #1e293b 0%, #0f172a 100%);
  color: #ffffff;
  position: relative;
}

.wheel-header {
  text-align: center;
  margin-bottom: 2rem;
}

.wheel-title {
  font-size: 2.2rem;
  font-weight: 800;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 50%, #d97706 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 0.5rem;
}

.wheel-subtitle {
  font-size: 1rem;
  color: #94a3b8;
}

.wheel-stage {
  position: relative;
  width: 500px;
  height: 500px;
  max-width: 92vw;
  max-height: 92vw;
  display: flex;
  align-items: center;
  justify-content: center;
}

.wheel-canvas {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 15px 30px rgba(0, 0, 0, 0.5));
}

.wheel-pointer {
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
}

.pointer-arrow {
  width: 0;
  height: 0;
  border-left: 20px solid transparent;
  border-right: 20px solid transparent;
  border-top: 40px solid #ef4444;
  filter: drop-shadow(0 4px 6px rgba(0, 0, 0, 0.4));
}

.spin-center-button {
  position: absolute;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffffff 0%, #e2e8f0 100%);
  color: #0f172a;
  font-weight: 800;
  font-size: 0.85rem;
  border: 5px solid #38bdf8;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.4), inset 0 2px 4px rgba(255, 255, 255, 0.8);
  cursor: pointer;
  z-index: 5;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 8px;
}

.spin-center-button:hover:not(:disabled) {
  transform: scale(1.08);
  box-shadow: 0 10px 30px rgba(56, 189, 248, 0.5);
}

.spin-center-button:disabled {
  opacity: 0.8;
  cursor: not-allowed;
}

/* Winner Modal */
.winner-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

.winner-modal-card {
  background: #1e293b;
  border: 2px solid #f59e0b;
  border-radius: 20px;
  padding: 3rem 2.5rem;
  text-align: center;
  max-width: 480px;
  width: 90%;
  box-shadow: 0 25px 50px -12px rgba(245, 158, 11, 0.25);
  animation: scaleUp 0.35s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.winner-confetti {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.winner-heading {
  font-size: 1.5rem;
  font-weight: 700;
  color: #94a3b8;
  margin-bottom: 0.75rem;
}

.winner-prize {
  font-size: 2.2rem;
  font-weight: 900;
  margin-bottom: 2rem;
  word-break: break-word;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleUp {
  from { transform: scale(0.8); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
</style>
