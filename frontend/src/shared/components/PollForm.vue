<template>
  <div class="poll-form">
    <!-- Câu hỏi -->
    <div class="poll-row">
      <label class="poll-label">Câu hỏi:</label>
      <div class="poll-field">
        <input
          v-model="poll.question"
          class="poll-input"
          placeholder="Nhập câu hỏi bình chọn..."
          :disabled="disabled"
        />
      </div>
    </div>

    <!-- Danh sách đáp án -->
    <div class="poll-row">
      <label class="poll-label">Có thể trả lời:</label>
      <div class="poll-field">
        <div
          v-for="(option, index) in poll.options"
          :key="index"
          class="option-item"
        >
          <input
            v-model="poll.options[index].text"
            class="poll-input"
            :placeholder="index < 2 ? `Lựa chọn ${index + 1}` : 'Lựa chọn...'"
            @keydown.enter.prevent="addOption"
            :disabled="disabled"
          />
          <button
            v-if="poll.options.length > 2 && !disabled"
            class="btn-remove-option"
            @click="removeOption(index)"
            title="Xóa lựa chọn"
          >×</button>
        </div>
        <button v-if="!disabled" class="btn-add-option" @click="addOption">+ Thêm lựa chọn</button>
      </div>
    </div>

    <!-- Số lượng đáp án tối đa -->
    <div class="poll-row">
      <label class="poll-label">Số lượng đáp án tối đa:</label>
      <div class="poll-field">
        <div class="max-choice-options">
          <label class="radio-label">
            <input type="radio" v-model="poll.maxChoiceType" value="one" :disabled="disabled" />
            <span>Một lựa chọn</span>
          </label>
          <label class="radio-label">
            <input type="radio" v-model="poll.maxChoiceType" value="unlimited" :disabled="disabled" />
            <span>Vô số</span>
          </label>
          <label class="radio-label custom-count-label">
            <input type="radio" v-model="poll.maxChoiceType" value="custom" :disabled="disabled" />
            <div class="count-control" @click.stop>
              <input
                v-model.number="poll.maxChoiceCustom"
                type="number"
                class="count-input"
                min="2"
                :max="poll.options.length"
                :disabled="poll.maxChoiceType !== 'custom' || disabled"
              />
              <button v-if="!disabled" class="btn-count" @click.prevent="incrementMax" :disabled="poll.maxChoiceType !== 'custom'">+</button>
              <button v-if="!disabled" class="btn-count" @click.prevent="decrementMax" :disabled="poll.maxChoiceType !== 'custom'">−</button>
            </div>
          </label>
        </div>
        <p class="hint-text">Đây là số lượng đáp án tối đa mà một người có thể lựa chọn.</p>
      </div>
    </div>

    <!-- Tùy chọn -->
    <div class="poll-row">
      <label class="poll-label">Tùy chọn:</label>
      <div class="poll-field">
        <label class="checkbox-label">
          <input type="checkbox" v-model="poll.allowChangeVote" :disabled="disabled" />
          <span>Cho phép người dùng thay đổi đáp án</span>
        </label>
        <label class="checkbox-label">
          <input type="checkbox" v-model="poll.publicVoting" :disabled="disabled" />
          <span>Hiển thị bình chọn công cộng</span>
        </label>
        <label class="checkbox-label">
          <input type="checkbox" v-model="poll.showResultWithoutVote" :disabled="disabled" />
          <span>Cho phép hiển thị kết quả mà không cần bầu chọn</span>
        </label>
        <label class="checkbox-label">
          <input type="checkbox" v-model="poll.hasExpiry" :disabled="disabled" />
          <span>Đóng bình chọn này sau:</span>
        </label>
        <div v-if="poll.hasExpiry" class="expiry-control">
          <input
            v-model.number="poll.expiryValue"
            type="number"
            class="count-input"
            min="1"
            :disabled="disabled"
            @input="poll.expiryChanged = true"
          />
          <button v-if="!disabled" class="btn-count" @click.prevent="poll.expiryValue++; poll.expiryChanged = true">+</button>
          <button v-if="!disabled" class="btn-count" @click.prevent="decrementExpiry">−</button>
          <select v-model="poll.expiryUnit" class="expiry-select" :disabled="disabled" @change="poll.expiryChanged = true">
            <option value="day">Ngày</option>
            <option value="hour">Giờ</option>
          </select>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PollForm',
  emits: ['update:modelValue'],
  props: {
    modelValue: {
      type: Object,
      default: null
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      poll: {
        question: '',
        options: [{id: null, text: ''}, {id: null, text: ''}, {id: null, text: ''}],
        maxChoiceType: 'one',
        maxChoiceCustom: 2,
        allowChangeVote: true,
        publicVoting: false,
        showResultWithoutVote: true,
        hasExpiry: false,
        expiryValue: 7,
        expiryUnit: 'day',
        originalClosedAt: null,
        expiryChanged: false
      },
      isInitialized: false,
      isPopulating: false
    }
  },
  watch: {
    poll: {
      deep: true,
      handler(val) {
        if (!this.isPopulating) {
          this.$emit('update:modelValue', this.getPollData())
        }
      }
    },
    modelValue: {
      immediate: true,
      handler(val) {
        this.initFromProps(val)
      }
    }
  },
  methods: {
    initFromProps(val) {
      if (val && Object.keys(val).length > 0 && !this.isInitialized) {
        this.isPopulating = true
        
        const options = val.options && val.options.length > 0 
          ? val.options.map(o => ({ id: o.id || null, text: o.optionText || o })) 
          : [{id: null, text: ''}, {id: null, text: ''}, {id: null, text: ''}]
        
        let maxChoiceType = 'one'
        let maxChoiceCustom = 2
        if (val.maxChoices === -1) {
          maxChoiceType = 'unlimited'
        } else if (val.maxChoices > 1) {
          maxChoiceType = 'custom'
          maxChoiceCustom = val.maxChoices
        }

        let hasExpiry = false
        let expiryValue = 7
        let expiryUnit = 'day'
        
        if (val.closedAt) {
          hasExpiry = true
          // Calculate roughly how many days/hours left or just use a default display for edit
          const diff = new Date(val.closedAt) - new Date()
          if (diff > 0) {
            const diffDays = Math.round(diff / (1000 * 60 * 60 * 24))
            if (diffDays >= 1) {
              expiryValue = diffDays
              expiryUnit = 'day'
            } else {
              expiryValue = Math.max(1, Math.round(diff / (1000 * 60 * 60)))
              expiryUnit = 'hour'
            }
          }
        }

        this.poll = {
          question: val.question || '',
          options: options,
          maxChoiceType,
          maxChoiceCustom,
          allowChangeVote: val.allowChangeVote !== undefined ? val.allowChangeVote : true,
          publicVoting: val.publicVoting !== undefined ? val.publicVoting : false,
          showResultWithoutVote: val.showResultWithoutVote !== undefined ? val.showResultWithoutVote : true,
          hasExpiry,
          expiryValue,
          expiryUnit,
          originalClosedAt: val.closedAt || null,
          expiryChanged: false
        }
        
        this.isInitialized = true
        
        this.$nextTick(() => {
          this.isPopulating = false
        })
      }
    },
    addOption() {
      if (!this.disabled) {
        this.poll.options.push({id: null, text: ''})
      }
    },
    removeOption(index) {
      if (!this.disabled) {
        this.poll.options.splice(index, 1)
      }
    },
    incrementMax() {
      if (!this.disabled) {
        const max = this.poll.options.filter(o => o.text.trim()).length
        if (this.poll.maxChoiceCustom < max) this.poll.maxChoiceCustom++
      }
    },
    decrementMax() {
      if (!this.disabled && this.poll.maxChoiceCustom > 2) this.poll.maxChoiceCustom--
    },
    decrementExpiry() {
      if (!this.disabled && this.poll.expiryValue > 1) {
        this.poll.expiryValue--
        this.poll.expiryChanged = true
      }
    },
    getPollData() {
      const options = this.poll.options.filter(o => o.text.trim()).map(o => ({ id: o.id, optionText: o.text }))
      let maxChoices = 1
      if (this.poll.maxChoiceType === 'unlimited') maxChoices = -1
      else if (this.poll.maxChoiceType === 'custom') maxChoices = this.poll.maxChoiceCustom

      let closedAt = null
      if (this.poll.hasExpiry) {
        if (this.poll.originalClosedAt && !this.poll.expiryChanged) {
          closedAt = this.poll.originalClosedAt
        } else {
          const date = new Date()
          if (this.poll.expiryUnit === 'day') {
            date.setDate(date.getDate() + this.poll.expiryValue)
          } else if (this.poll.expiryUnit === 'hour') {
            date.setHours(date.getHours() + this.poll.expiryValue)
          }
          // Chuyển đổi thành local time string thay vì UTC toISOString để SpringBoot nhận đúng giờ
          const tzOffset = date.getTimezoneOffset() * 60000;
          closedAt = (new Date(date.getTime() - tzOffset)).toISOString().slice(0, -1);
        }
      }

      return {
        question: this.poll.question,
        options: options,
        maxChoices,
        allowChangeVote: this.poll.allowChangeVote,
        publicVoting: this.poll.publicVoting,
        showResultWithoutVote: this.poll.showResultWithoutVote,
        closedAt: closedAt
      }
    },
    reset() {
      this.poll = {
        question: '',
        options: [{id: null, text: ''}, {id: null, text: ''}, {id: null, text: ''}],
        maxChoiceType: 'one',
        maxChoiceCustom: 2,
        allowChangeVote: true,
        publicVoting: false,
        showResultWithoutVote: true,
        hasExpiry: false,
        expiryValue: 7,
        expiryUnit: 'day',
        originalClosedAt: null,
        expiryChanged: false
      }
    }
  }
}
</script>

<style scoped>
.poll-form {
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background: #fafafa;
  padding: 0;
  overflow: hidden;
}

.poll-row {
  display: flex;
  align-items: flex-start;
  border-bottom: 1px solid #eee;
  min-height: 48px;
}

.poll-row:last-child {
  border-bottom: none;
}

.poll-label {
  width: 200px;
  min-width: 200px;
  padding: 12px 16px;
  font-size: 13px;
  color: #555;
  background: #f5f5f5;
  border-right: 1px solid #e0e0e0;
  display: flex;
  align-items: flex-start;
  padding-top: 14px;
}

.poll-field {
  flex: 1;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.poll-input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #dce0e5;
  border-radius: 3px;
  font-size: 13px;
  background: white;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.poll-input:focus {
  border-color: #3498db;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-remove-option {
  background: none;
  border: none;
  font-size: 18px;
  color: #aaa;
  cursor: pointer;
  line-height: 1;
  padding: 0 4px;
  flex-shrink: 0;
  transition: color 0.2s;
}

.btn-remove-option:hover {
  color: #e74c3c;
}

.btn-add-option {
  background: none;
  border: none;
  color: #3498db;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 0;
  text-align: left;
  width: fit-content;
}

.btn-add-option:hover {
  text-decoration: underline;
}

/* Radio options */
.max-choice-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  cursor: pointer;
}

.custom-count-label {
  align-items: center;
}

.count-control {
  display: flex;
  align-items: center;
  gap: 0;
  border: 1px solid #dce0e5;
  border-radius: 3px;
  overflow: hidden;
}

.count-input {
  width: 50px;
  text-align: center;
  border: none;
  padding: 4px 6px;
  font-size: 13px;
  outline: none;
  background: white;
  -moz-appearance: textfield;
}

.count-input::-webkit-outer-spin-button,
.count-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
}

.count-input:disabled {
  background: #f5f5f5;
  color: #aaa;
}

.btn-count {
  background: #f5f5f5;
  border: none;
  border-left: 1px solid #dce0e5;
  padding: 4px 8px;
  cursor: pointer;
  font-size: 13px;
  color: #555;
  transition: background 0.2s;
}

.btn-count:hover:not(:disabled) {
  background: #e8e8e8;
}

.btn-count:disabled {
  cursor: not-allowed;
  color: #ccc;
}

.hint-text {
  font-size: 11px;
  color: #999;
  margin: 0;
}

/* Checkboxes */
.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  cursor: pointer;
  padding: 2px 0;
}

.checkbox-label input[type="checkbox"] {
  accent-color: #3498db;
  cursor: pointer;
}

/* Expiry control */
.expiry-control {
  display: flex;
  align-items: center;
  gap: 0;
  margin-left: 20px;
  border: 1px solid #dce0e5;
  border-radius: 3px;
  overflow: hidden;
  width: fit-content;
}

.expiry-select {
  border: none;
  border-left: 1px solid #dce0e5;
  padding: 4px 6px;
  font-size: 13px;
  background: #f5f5f5;
  cursor: pointer;
  outline: none;
}

@media (max-width: 767px) {
  .poll-row {
    flex-direction: column;
    align-items: stretch;
  }

  .poll-label {
    width: 100%;
    min-width: 0;
    background: transparent;
    border-right: none;
    padding: 12px 12px 4px 12px;
  }

  .poll-field {
    padding: 4px 12px 12px 12px;
  }

  .expiry-control {
    margin-left: 0;
    margin-top: 6px;
  }
}
</style>
