<template>
  <div class="poll-display card" v-if="poll">
    <div class="poll-header">
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
      <h2>{{ poll.question }}</h2>
    </div>

    <div class="poll-body">
      <!-- State 1: Hiện kết quả (khi đã vote, hoặc poll đóng, hoặc chọn xem kết quả, hoặc chưa đăng nhập và poll cho phép) -->
      <div v-if="showResults" class="poll-results">
        <div 
          v-for="option in poll.options" 
          :key="option.id" 
          class="result-item"
        >
          <div class="option-name">
            <!-- Dấu check nếu user đã vote cho option này -->
            <svg v-if="hasVotedFor(option.id)" class="voted-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
            <span :class="{ 'voted-text': hasVotedFor(option.id) }">{{ option.optionText }}</span>
          </div>
          <div class="result-details">
            <div class="vote-stats">
              <span class="vote-count">Số phiếu: {{ option.voteCount }}</span>
              <span class="vote-percent">{{ option.percentage.toFixed(1) }}%</span>
            </div>
            <div class="progress-col">
              <div v-if="option.percentage > 0" class="progress-bar-fill" :style="{ width: option.percentage + '%' }"></div>
            </div>
          </div>
        </div>

        <div v-if="poll.publicVoting" class="public-voting-link">
          <a href="#" @click.prevent="showVotersModal = true">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
            Xem danh sách thành viên tham gia vote
          </a>
        </div>
      </div>

      <!-- State 2: Form bầu chọn -->
      <div v-else class="poll-vote-form">
        <label 
          v-for="option in poll.options" 
          :key="option.id" 
          class="vote-option-label"
          :class="{ 'disabled-label': !isLoggedIn }"
        >
          <input 
            :type="inputType" 
            :value="option.id" 
            v-model="selectedOptions"
            :disabled="isSubmitting || !isLoggedIn"
          />
          <span>{{ option.optionText }}</span>
        </label>
        <div v-if="poll.maxChoices > 1" class="max-choice-hint">
          Bạn có thể chọn tối đa {{ poll.maxChoices }} đáp án.
        </div>
        <div v-if="!isLoggedIn" class="login-prompt">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path><polyline points="10 17 15 12 10 7"></polyline><line x1="15" y1="12" x2="3" y2="12"></line></svg>
          Vui lòng <router-link to="/login">đăng nhập</router-link> để tham gia bầu chọn.
        </div>
      </div>
    </div>

    <div class="poll-footer">
      <div class="poll-meta">
        <span v-if="poll.totalVotes !== undefined">Số lượng người bầu chọn: {{ poll.totalVotes }}</span>
        <span v-if="poll.closedAt" class="meta-dot">·</span>
        <span v-if="poll.closedAt" :class="{'poll-closed': isClosed}">
          {{ isClosed ? 'Đã đóng lúc:' : 'Sẽ đóng lúc:' }} {{ formatDate(poll.closedAt) }}
        </span>
      </div>

      <div class="poll-actions" v-if="canVote || canChangeVote || (poll.showResultWithoutVote && !hasVoted)">
        <!-- Các nút khi đang ở màn hình Bầu chọn -->
        <template v-if="!showResults">
          <button 
            class="btn-vote" 
            @click="submitVote" 
            :disabled="!hasSelectedOption || isSubmitting || isClosed"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
            Bầu chọn
          </button>
          <button 
            v-if="poll.showResultWithoutVote" 
            class="btn-view-results" 
            @click="viewResultsOnly = true"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
            Hiện kết quả
          </button>
        </template>

        <!-- Các nút khi đang ở màn hình Kết quả -->
        <template v-else>
          <button 
            v-if="canChangeVote && !isClosed" 
            class="btn-change-vote" 
            @click="changeVote"
          >
            Thay đổi bầu chọn
          </button>
          <button 
            v-if="viewResultsOnly && !hasVoted && !isClosed" 
            class="btn-back-vote" 
            @click="viewResultsOnly = false"
          >
            Quay lại bầu chọn
          </button>
        </template>
      </div>
    </div>

    <TableModal 
      v-model:show="showVotersModal" 
      title="DANH SÁCH THÀNH VIÊN ĐÃ BÌNH CHỌN"
      cardClass="voters-modal-card"
      placeholder="Tìm kiếm thành viên..."
      :headers="voterHeaders"
      :items="voters"
      :totalItems="voterTotal"
      v-model:pageSize="voterPageSize"
      v-model:currentPage="voterCurrentPage"
      :loading="voterLoading"
      @search="handleVoterSearch"
      :showAddButton="false"
      :showAction="false"
      :showSTT="true"
    >
      <template #extra-filters>
        <div class="filter-group-wrapper">
          <select v-model="voterSelectedOptionId" class="form-select-sm" @change="handleVoterFilterChange">
            <option :value="null">-- Tất cả lựa chọn --</option>
            <option v-for="opt in poll.options" :key="opt.id" :value="opt.id">
              {{ opt.optionText }}
            </option>
          </select>
        </div>
      </template>
      
      <template #item-username="{ item }">
        <strong>{{ item.displayName || item.username }}</strong>
      </template>
    </TableModal>
  </div>
</template>

<script>
import pollService from '@/apps/Forum/services/poll.service'
import { formatForumDate } from '@/shared/utils/date'
import { alertSuccess, alertError } from '@/shared/utils/swal'
import TableModal from './TableModal.vue'

export default {
  name: 'PollDisplay',
  components: { TableModal },
  props: {
    pollData: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      poll: null,
      selectedOptions: [],
      viewResultsOnly: false,
      isSubmitting: false,
      showVotersModal: false,
      
      // Voter List State
      voters: [],
      voterTotal: 0,
      voterKeyword: '',
      voterSelectedOptionId: null,
      voterPageSize: 10,
      voterCurrentPage: 1,
      voterLoading: false,
      voterHeaders: [
        { text: 'Tên thành viên', value: 'username', sortable: false },
        { text: 'Lựa chọn đã vote', value: 'optionText', sortable: false }
      ]
    }
  },
  created() {
    this.initPoll()
  },
  watch: {
    pollData: {
      handler() {
        this.initPoll()
      },
      deep: true
    },
    selectedOptions(newVal) {
      if (this.poll && this.poll.maxChoices > 1 && Array.isArray(newVal)) {
        if (newVal.length > this.poll.maxChoices) {
          // Bỏ chọn cái cũ nhất
          this.selectedOptions = newVal.slice(newVal.length - this.poll.maxChoices)
        }
      }
    },
    showVotersModal(newVal) {
      if (newVal) {
        this.resetVoterFilters()
        this.fetchVoters()
      }
    },
    voterCurrentPage() {
      if (this.showVotersModal) {
        this.fetchVoters()
      }
    },
    voterPageSize() {
      if (this.showVotersModal) {
        this.voterCurrentPage = 1
        this.fetchVoters()
      }
    }
  },
  computed: {
    isLoggedIn() {
      return !!localStorage.getItem('token')
    },
    hasVoted() {
      return this.poll && this.poll.userVotedOptionIds && this.poll.userVotedOptionIds.length > 0
    },
    isClosed() {
      if (!this.poll || !this.poll.closedAt) return false
      return new Date(this.poll.closedAt) < new Date()
    },
    showResults() {
      if (this.isClosed) return true
      if (this.hasVoted) return true
      if (this.viewResultsOnly) return true
      if (!this.isLoggedIn && this.poll.showResultWithoutVote) return true
      return false
    },
    canVote() {
      return this.isLoggedIn && !this.hasVoted && !this.isClosed
    },
    canChangeVote() {
      return this.isLoggedIn && this.hasVoted && this.poll.allowChangeVote && !this.isClosed
    },
    inputType() {
      if (!this.poll) return 'radio'
      return (this.poll.maxChoices === 1) ? 'radio' : 'checkbox'
    },
    hasSelectedOption() {
      if (this.inputType === 'radio') {
        return this.selectedOptions !== null && this.selectedOptions !== undefined
      }
      return Array.isArray(this.selectedOptions) && this.selectedOptions.length > 0
    }
  },
  methods: {
    initPoll() {
      this.poll = JSON.parse(JSON.stringify(this.pollData)) // clone
      if (this.hasVoted) {
        if (this.inputType === 'radio') {
          this.selectedOptions = this.poll.userVotedOptionIds[0]
        } else {
          this.selectedOptions = [...this.poll.userVotedOptionIds]
        }
      } else {
        this.selectedOptions = this.inputType === 'radio' ? null : []
      }
    },
    formatDate(dateStr) {
      return formatForumDate(dateStr)
    },
    hasVotedFor(optionId) {
      if (!this.poll.userVotedOptionIds) return false
      return this.poll.userVotedOptionIds.includes(optionId)
    },
    changeVote() {
      this.viewResultsOnly = false
      // Tạm xóa userVotedOptionIds để hiển thị form
      this.poll.userVotedOptionIds = [] 
      if (this.inputType === 'radio') {
        this.selectedOptions = null
      } else {
        this.selectedOptions = []
      }
    },
    async submitVote() {
      try {
        this.isSubmitting = true
        let optionIds = []
        if (this.inputType === 'radio') {
          optionIds = [this.selectedOptions]
        } else {
          optionIds = [...this.selectedOptions]
        }

        const response = await pollService.vote(this.poll.id, optionIds)
        if (response.status === 200) {
          this.poll = response.data
          this.viewResultsOnly = false
          this.$emit('vote-updated', response.data)
        } else {
          alertError('Lỗi', 'Không thể gửi bình chọn')
        }
      } catch (error) {
        alertError('Lỗi', error.response?.data?.message || 'Có lỗi xảy ra khi bình chọn')
      } finally {
        this.isSubmitting = false
      }
    },
    
    // --- DANH SÁCH NGƯỜI VOTE LOGIC ---
    resetVoterFilters() {
      this.voterKeyword = ''
      this.voterSelectedOptionId = null
      this.voterCurrentPage = 1
    },
    handleVoterSearch(k) {
      this.voterKeyword = k
      this.voterCurrentPage = 1
      this.fetchVoters()
    },
    handleVoterFilterChange() {
      this.voterCurrentPage = 1
      this.fetchVoters()
    },
    async fetchVoters() {
      this.voterLoading = true
      try {
        const params = {
          page: this.voterCurrentPage - 1,
          size: this.voterPageSize
        }
        if (this.voterKeyword) params.keyword = this.voterKeyword
        if (this.voterSelectedOptionId) params.optionId = this.voterSelectedOptionId

        const response = await pollService.getVotes(this.poll.id, params)
        if (response.data) {
          this.voters = response.data.content || []
          this.voterTotal = response.data.totalElements || 0
        }
      } catch (error) {
        console.error('Lỗi khi tải danh sách người vote:', error)
      } finally {
        this.voterLoading = false
      }
    }
  }
}
</script>

<style scoped>
.poll-display {
  background: #fdfdfd;
  border: 1px solid #dcdcdc;
  border-radius: 4px;
  margin-bottom: 20px;
  overflow: hidden;
}

.poll-header {
  background: #f0f8ff;
  padding: 12px 16px;
  border-bottom: 1px solid #dcdcdc;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #2980b9;
}

.poll-header h2 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.poll-body {
  padding: 16px;
}

/* Form bầu chọn */
.poll-vote-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.vote-option-label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1rem;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background 0.2s;
}

.vote-option-label:hover {
  background: #f5f5f5;
}

.vote-option-label input[type="radio"],
.vote-option-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #3498db;
}

.max-choice-hint {
  font-size: 0.85rem;
  color: #e67e22;
  margin-left: 12px;
  font-style: italic;
}

/* Kết quả bầu chọn */
.poll-results {
  display: flex;
  flex-direction: column;
}

.public-voting-link {
  margin-top: 15px;
  font-size: 0.9rem;
  text-align: right;
}

.public-voting-link a {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #3498db;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.public-voting-link a:hover {
  color: #1a507a;
  text-decoration: underline;
}

:deep(.voters-modal-card) {
  width: 800px;
  max-width: 95vw;
}

.filter-group-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-select-sm {
  padding: 8px 12px;
  border-radius: 4px;
  border: 1px solid #ddd;
  outline: none;
}
.form-select-sm:focus {
  border-color: var(--primary);
}

.result-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f1f1f1;
}

.result-item:last-child {
  border-bottom: none;
}

.option-name {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1rem;
}

.result-details {
  display: flex;
  align-items: center;
}

.voted-icon {
  color: #f39c12;
}

.voted-text {
  font-weight: bold;
}

.vote-stats {
  display: flex;
  gap: 20px;
  font-size: 0.95rem;
  color: #555;
  min-width: 150px;
  justify-content: flex-end;
}

.vote-count {
  color: #666;
}

.vote-percent {
  min-width: 45px;
  text-align: right;
}

.progress-col {
  width: 250px;
  height: 14px;
  margin-left: 15px;
  background: #f0f0f0;
  border-radius: 2px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: #f39c12;
  border-radius: 2px;
  transition: width 0.5s ease;
}

.login-prompt {
  margin-top: 10px;
  padding: 10px;
  background: #fff3cd;
  color: #856404;
  border: 1px solid #ffeeba;
  border-radius: 4px;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 8px;
}
.login-prompt a {
  color: #1a507a;
  font-weight: bold;
}
.disabled-label {
  opacity: 0.7;
  cursor: not-allowed;
}

/* Footer */
.poll-footer {
  padding: 12px 16px;
  border-top: 1px solid #dcdcdc;
  background: #f9f9f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
  color: #666;
}

.poll-meta {
  display: flex;
  gap: 6px;
  align-items: center;
}

.meta-dot {
  font-weight: bold;
}

.poll-closed {
  color: #e74c3c;
}

.poll-actions {
  display: flex;
  gap: 10px;
}

.btn-vote {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #3498db;
  color: white;
  border: none;
  padding: 6px 16px;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-vote:hover:not(:disabled) {
  background: #2980b9;
}

.btn-vote:disabled {
  background: #95a5a6;
  cursor: not-allowed;
}

.btn-view-results {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #34495e;
  color: white;
  border: none;
  padding: 6px 16px;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-view-results:hover {
  background: #2c3e50;
}

.btn-change-vote, .btn-back-vote {
  background: #2980b9;
  color: white;
  border: none;
  padding: 6px 16px;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
}

.btn-change-vote:hover, .btn-back-vote:hover {
  background: #1f618d;
}

@media (max-width: 768px) {
  .result-item {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
    padding: 12px 0;
  }

  .option-name {
    width: 100%;
    font-size: 0.95rem;
    line-height: 1.4;
  }

  .result-details {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .vote-stats {
    min-width: auto;
    gap: 10px;
    font-size: 0.88rem;
    justify-content: flex-start;
    flex-shrink: 0;
  }

  .vote-percent {
    min-width: auto;
  }

  .progress-col {
    flex: 1;
    width: auto;
    margin-left: 0;
    min-width: 60px;
  }

  .poll-footer {
    flex-wrap: wrap;
    gap: 10px;
  }

  .poll-meta {
    flex-wrap: wrap;
  }
}
</style>
