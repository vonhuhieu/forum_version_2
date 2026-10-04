<template>
  <div class="millionaire-container">
    <!-- Header: Tên game, đồng hồ, quyền trợ giúp -->
    <div class="millionaire-topbar">
      <div class="game-info">
        <h3 class="game-title">{{ product?.title || 'Đấu Trường Ai Là Triệu Phú' }}</h3>
        <span class="badge-level">CÂU SỐ {{ currentLevel }} / {{ totalLevels }}</span>
      </div>

      <!-- Đồng hồ đếm ngược -->
      <div class="timer-display" :class="{ 'timer-danger': timer <= 10 }">
        <span class="timer-number">{{ timer }}</span>
        <span class="timer-label">giây</span>
      </div>

      <!-- Các quyền trợ giúp -->
      <div class="lifelines-wrapper">
        <button
          class="btn-lifeline"
          :class="{ 'used': lifelines.fiftyFifty }"
          :disabled="lifelines.fiftyFifty || isAnswered || isGameOver"
          @click="useFiftyFifty"
          title="Trợ giúp 50:50"
        >
          50:50
        </button>

        <button
          class="btn-lifeline"
          :class="{ 'used': lifelines.audience }"
          :disabled="lifelines.audience || isAnswered || isGameOver"
          @click="useAudience"
          title="Hỏi ý kiến khán giả"
        >
          <i class="bi bi-people-fill"></i>
        </button>

        <button
          class="btn-lifeline"
          :class="{ 'used': lifelines.phone }"
          :disabled="lifelines.phone || isAnswered || isGameOver"
          @click="usePhone"
          title="Gọi điện cho người thân"
        >
          <i class="bi bi-telephone-fill"></i>
        </button>
      </div>
    </div>

    <!-- Khung chính -->
    <div class="millionaire-main-layout" v-if="!isGameOver && currentQuestion">
      <!-- Đấu trường câu hỏi -->
      <div class="arena-section">
        <!-- Khung câu hỏi -->
        <div class="question-box">
          <div class="question-text">{{ currentQuestion.question }}</div>
        </div>

        <!-- 4 Lựa chọn -->
        <div class="answers-grid">
          <button
            v-for="key in ['A', 'B', 'C', 'D']"
            :key="key"
            class="answer-btn"
            :class="getAnswerBtnClass(key)"
            :disabled="isAnswered || eliminatedOptions.includes(key)"
            @click="selectAnswer(key)"
          >
            <span class="opt-key">{{ key }}:</span>
            <span class="opt-val">{{ currentQuestion['option' + key] || '' }}</span>
          </button>
        </div>

        <!-- Trợ giúp khán giả popup -->
        <div v-if="audiencePoll" class="lifeline-popup">
          <div class="popup-title">📊 Khán giả trường quay bình chọn:</div>
          <div class="audience-bars">
            <div v-for="key in ['A', 'B', 'C', 'D']" :key="key" class="audience-bar-col">
              <div class="bar-fill-track">
                <div class="bar-fill" :style="{ height: audiencePoll[key] + '%' }"></div>
              </div>
              <span class="bar-percent">{{ audiencePoll[key] }}%</span>
              <span class="bar-key">{{ key }}</span>
            </div>
          </div>
          <button class="btn btn-sm btn-outline-light mt-2" @click="audiencePoll = null">Đóng</button>
        </div>

        <!-- Trợ giúp gọi điện popup -->
        <div v-if="phoneTip" class="lifeline-popup">
          <div class="popup-title">📞 Người thân tư vấn:</div>
          <p class="phone-tip-text">"{{ phoneTip }}"</p>
          <button class="btn btn-sm btn-outline-light mt-2" @click="phoneTip = null">Đóng</button>
        </div>
      </div>

      <!-- Thang tiền thưởng bên phải -->
      <div class="ladder-section d-none d-md-block">
        <div class="ladder-list">
          <div
            v-for="lvl in totalLevels"
            :key="lvl"
            class="ladder-step"
            :class="{
              'step-active': lvl === currentLevel,
              'step-passed': lvl < currentLevel,
              'step-milestone': isMilestone(lvl)
            }"
          >
            <span class="step-num">{{ lvl }}</span>
            <span class="step-amount">{{ formatPrize(lvl) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Trạng thái chưa có câu hỏi -->
    <div v-else-if="!isGameOver && (!questions || questions.length === 0)" class="text-center text-muted py-5">
      <p>Chưa có câu hỏi nào trong trò chơi này.</p>
    </div>

    <!-- Màn hình kết thúc / Chiến thắng -->
    <div v-if="isGameOver" class="gameover-card">
      <div class="gameover-icon">{{ isVictory ? '🏆' : '💥' }}</div>
      <h2 class="gameover-title">{{ isVictory ? 'CHÚC MỪNG BẠN LÀ TRIỆU PHÚ!' : 'RẤT TIẾC, CUỘC CHƠI DỪNG LẠI!' }}</h2>
      <p class="gameover-sub">Bạn đã xuất sắc vượt qua {{ currentLevel - 1 }} / {{ totalLevels }} câu hỏi.</p>
      <div class="gameover-prize">Phần thưởng: {{ formatPrize(Math.max(0, currentLevel - 1)) }}</div>
      <button class="btn btn-warning btn-lg fw-bold mt-4" @click="restartGame">
        <i class="bi bi-arrow-repeat"></i> Chơi Lại Từ Đầu
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MillionairePlayer',
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
      selectedAnswer: null,
      isAnswered: false,
      isGameOver: false,
      isVictory: false,
      timer: 30,
      timerInterval: null,
      eliminatedOptions: [],
      audiencePoll: null,
      phoneTip: null,
      lifelines: {
        fiftyFifty: false,
        audience: false,
        phone: false
      }
    }
  },
  computed: {
    questions() {
      return (this.rows || []).map((row, idx) => {
        const d = row.data || {}
        return {
          id: row.id || idx,
          level: Number(d.level) || idx + 1,
          question: d.question || `Câu hỏi số ${idx + 1}`,
          optionA: d.optionA || 'A',
          optionB: d.optionB || 'B',
          optionC: d.optionC || 'C',
          optionD: d.optionD || 'D',
          correct: (d.correct || 'A').toUpperCase().trim(),
          explanation: d.explanation || ''
        }
      })
    },
    totalLevels() {
      return this.questions.length || 15
    },
    currentLevel() {
      return this.currentIndex + 1
    },
    currentQuestion() {
      return this.questions[this.currentIndex] || null
    }
  },
  watch: {
    questions(newQuestions) {
      if (newQuestions.length > 0 && !this.timerInterval && !this.isGameOver) {
        this.startTimer()
      }
    }
  },
  mounted() {
    if (this.questions.length > 0) {
      this.startTimer()
    }
  },
  beforeUnmount() {
    this.stopTimer()
  },
  methods: {
    startTimer() {
      this.stopTimer()
      this.timer = 30
      this.timerInterval = setInterval(() => {
        if (this.timer > 0) {
          this.timer--
        } else {
          this.handleTimeout()
        }
      }, 1000)
    },
    stopTimer() {
      if (this.timerInterval) {
        clearInterval(this.timerInterval)
        this.timerInterval = null
      }
    },
    handleTimeout() {
      this.stopTimer()
      this.isGameOver = true
      this.isVictory = false
    },
    selectAnswer(key) {
      if (this.isAnswered || this.isGameOver) return
      this.isAnswered = true
      this.selectedAnswer = key
      this.stopTimer()

      const isCorrect = key === this.currentQuestion.correct

      setTimeout(() => {
        if (isCorrect) {
          if (this.currentIndex < this.questions.length - 1) {
            this.currentIndex++
            this.selectedAnswer = null
            this.isAnswered = false
            this.eliminatedOptions = []
            this.audiencePoll = null
            this.phoneTip = null
            this.startTimer()
          } else {
            this.isGameOver = true
            this.isVictory = true
          }
        } else {
          this.isGameOver = true
          this.isVictory = false
        }
      }, 2000)
    },
    getAnswerBtnClass(key) {
      if (!this.isAnswered) {
        return this.selectedAnswer === key ? 'state-selected' : ''
      }
      if (key === this.currentQuestion.correct) {
        return 'state-correct'
      }
      if (this.selectedAnswer === key) {
        return 'state-wrong'
      }
      return ''
    },
    useFiftyFifty() {
      if (this.lifelines.fiftyFifty || this.isAnswered) return
      this.lifelines.fiftyFifty = true
      const correct = this.currentQuestion.correct
      const wrongs = ['A', 'B', 'C', 'D'].filter(k => k !== correct)
      // Loại bỏ ngẫu nhiên 2 phương án sai
      wrongs.sort(() => Math.random() - 0.5)
      this.eliminatedOptions = wrongs.slice(0, 2)
    },
    useAudience() {
      if (this.lifelines.audience || this.isAnswered) return
      this.lifelines.audience = true
      const correct = this.currentQuestion.correct
      const poll = {}
      const correctPct = 60 + Math.floor(Math.random() * 25)
      let remain = 100 - correctPct

      const others = ['A', 'B', 'C', 'D'].filter(k => k !== correct)
      const p1 = Math.floor(Math.random() * remain)
      remain -= p1
      const p2 = Math.floor(Math.random() * remain)
      const p3 = remain - p2

      poll[correct] = correctPct
      poll[others[0]] = p1
      poll[others[1]] = p2
      poll[others[2]] = p3

      this.audiencePoll = poll
    },
    usePhone() {
      if (this.lifelines.phone || this.isAnswered) return
      this.lifelines.phone = true
      const correct = this.currentQuestion.correct
      this.phoneTip = `Tôi khá chắc chắn đáp án đúng là phương án ${correct}! Chúc bạn may mắn.`
    },
    isMilestone(lvl) {
      return lvl === 5 || lvl === 10 || lvl === 15
    },
    formatPrize(lvl) {
      const prizes = [
        '0 đ', '200.000 đ', '400.000 đ', '600.000 đ', '1.000.000 đ',
        '2.000.000 đ 🌟', '3.000.000 đ', '6.000.000 đ', '10.000.000 đ', '14.000.000 đ',
        '22.000.000 đ 🌟', '30.000.000 đ', '40.000.000 đ', '60.000.000 đ', '85.000.000 đ',
        '150.000.000 đ 🏆'
      ]
      return prizes[lvl] || `${(lvl * 1000000).toLocaleString('vi-VN')} đ`
    },
    restartGame() {
      this.currentIndex = 0
      this.selectedAnswer = null
      this.isAnswered = false
      this.isGameOver = false
      this.isVictory = false
      this.eliminatedOptions = []
      this.audiencePoll = null
      this.phoneTip = null
      this.lifelines = {
        fiftyFifty: false,
        audience: false,
        phone: false
      }
      this.startTimer()
    }
  }
}
</script>

<style scoped>
.millionaire-container {
  min-height: 85vh;
  display: flex;
  flex-direction: column;
  background: radial-gradient(circle at center, #0a1128 0%, #000411 100%);
  color: #ffffff;
  padding: 1.5rem;
  position: relative;
}

.millionaire-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 15px;
}

.game-title {
  font-size: 1.4rem;
  font-weight: 800;
  margin: 0 0 4px 0;
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.badge-level {
  background: rgba(56, 189, 248, 0.2);
  color: #38bdf8;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba(56, 189, 248, 0.4);
}

.timer-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 68px;
  height: 68px;
  border-radius: 50%;
  border: 3px solid #38bdf8;
  background: rgba(15, 23, 42, 0.8);
  box-shadow: 0 0 20px rgba(56, 189, 248, 0.3);
}

.timer-danger {
  border-color: #ef4444;
  color: #ef4444;
  animation: pulse 1s infinite;
}

.timer-number {
  font-size: 1.5rem;
  font-weight: 900;
  line-height: 1;
}

.timer-label {
  font-size: 0.65rem;
  text-transform: uppercase;
  color: #94a3b8;
}

.lifelines-wrapper {
  display: flex;
  gap: 10px;
}

.btn-lifeline {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1e293b, #0f172a);
  border: 2px solid #f59e0b;
  color: #fbbf24;
  font-weight: 800;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-lifeline:hover:not(:disabled) {
  transform: scale(1.1);
  box-shadow: 0 0 15px rgba(245, 158, 11, 0.5);
}

.btn-lifeline.used,
.btn-lifeline:disabled {
  opacity: 0.3;
  cursor: not-allowed;
  filter: grayscale(1);
}

.millionaire-main-layout {
  display: grid;
  grid-template-columns: 1fr 260px;
  gap: 2rem;
  flex: 1;
}

@media (max-width: 768px) {
  .millionaire-main-layout {
    grid-template-columns: 1fr;
  }
}

.arena-section {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1.5rem;
  position: relative;
}

.question-box {
  background: linear-gradient(135deg, #1e293b, #0f172a);
  border: 2px solid #38bdf8;
  border-radius: 16px;
  padding: 2rem;
  text-align: center;
  min-height: 110px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.6);
}

.question-text {
  font-size: 1.3rem;
  font-weight: 700;
  line-height: 1.5;
}

.answers-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

@media (max-width: 600px) {
  .answers-grid {
    grid-template-columns: 1fr;
  }
}

.answer-btn {
  background: linear-gradient(135deg, #1e293b, #0f172a);
  border: 2px solid #475569;
  border-radius: 12px;
  padding: 1.1rem 1.25rem;
  color: #ffffff;
  font-size: 1.05rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.answer-btn:hover:not(:disabled) {
  border-color: #f59e0b;
  background: #334155;
  transform: translateY(-2px);
}

.answer-btn.state-selected {
  border-color: #f59e0b;
  background: #d97706;
  color: #ffffff;
}

.answer-btn.state-correct {
  border-color: #10b981;
  background: #059669;
  color: #ffffff;
  animation: pulse 0.5s infinite;
}

.answer-btn.state-wrong {
  border-color: #ef4444;
  background: #dc2626;
  color: #ffffff;
}

.opt-key {
  color: #f59e0b;
  font-weight: 800;
}

.ladder-section {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 1rem;
}

.ladder-list {
  display: flex;
  flex-direction: column-reverse;
  gap: 6px;
}

.ladder-step {
  display: flex;
  justify-content: space-between;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #94a3b8;
}

.step-active {
  background: #f59e0b;
  color: #0f172a;
  font-weight: 800;
}

.step-passed {
  color: #10b981;
}

.step-milestone {
  color: #fbbf24;
}

.lifeline-popup {
  background: #1e293b;
  border: 2px solid #38bdf8;
  border-radius: 12px;
  padding: 1.25rem;
  margin-top: 1rem;
  text-align: center;
}

.audience-bars {
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  height: 120px;
  margin-top: 1rem;
}

.audience-bar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.bar-fill-track {
  width: 28px;
  height: 80px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  display: flex;
  align-items: flex-end;
}

.bar-fill {
  width: 100%;
  background: #38bdf8;
  border-radius: 4px;
  transition: height 0.5s ease;
}

.bar-percent {
  font-size: 0.75rem;
  font-weight: 700;
}

.bar-key {
  font-weight: 800;
  color: #f59e0b;
}

.gameover-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  text-align: center;
  max-width: 540px;
  margin: auto;
  background: #1e293b;
  border: 2px solid #f59e0b;
  border-radius: 20px;
}

.gameover-icon {
  font-size: 3.5rem;
  margin-bottom: 1rem;
}

.gameover-title {
  font-size: 1.8rem;
  font-weight: 900;
  margin-bottom: 0.5rem;
}

.gameover-prize {
  font-size: 1.5rem;
  font-weight: 800;
  color: #fbbf24;
  margin-top: 1rem;
}
</style>
