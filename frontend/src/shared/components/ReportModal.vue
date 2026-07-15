<template>
  <BaseModal
    :show="show"
    @update:show="$emit('update:show', $event)"
    @close="reset"
    title="Report content"
    :showCloseButton="true"
    :closeOnOverlay="true"
    :cardStyle="{ width: '650px', 'max-width': '95vw' }"
    cardClass="custom-report-modal"
  >
    <div class="report-modal-body">
      <div class="report-row">
        <div class="report-label-col">
          <span>Lý do báo cáo:</span>
        </div>
        <div class="report-input-col">
          <textarea 
            ref="reasonTextarea"
            v-model="reason" 
            class="report-textarea"
            rows="3"
            placeholder="Vui lòng nhập lý do báo cáo vi phạm..."
          ></textarea>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="report-modal-footer">
        <button class="btn-submit-report" :disabled="submitting || !reason.trim()" @click="submit">
          {{ submitting ? 'Đang gửi...' : 'Báo cáo' }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>

<script>
import BaseModal from './BaseModal.vue'

export default {
  name: 'ReportModal',
  components: {
    BaseModal
  },
  props: {
    show: {
      type: Boolean,
      default: false
    },
    submitting: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      reason: ''
    }
  },
  watch: {
    show(newVal) {
      if (newVal) {
        this.reason = '';
        this.$nextTick(() => {
          if (this.$refs.reasonTextarea) {
            this.$refs.reasonTextarea.focus();
          }
        });
      }
    }
  },
  methods: {
    close() {
      this.$emit('update:show', false);
      this.$emit('close');
    },
    reset() {
      this.reason = '';
    },
    submit() {
      if (!this.reason.trim()) return;
      this.$emit('submit', this.reason.trim());
    }
  }
}
</script>

<style scoped>
::v-deep(.custom-report-modal) {
  border-radius: 4px;
}
::v-deep(.custom-report-modal .card-header) {
  background: #f0f7fd;
  color: #1a507a;
  padding: 12px 16px;
  font-weight: 500;
  text-align: left;
  font-size: 1.15rem;
  border-bottom: 1px solid #d8e6f3;
}
::v-deep(.custom-report-modal .close-btn) {
  color: #7cb3db;
  font-size: 1.5rem;
  top: 8px;
  right: 12px;
}
::v-deep(.custom-report-modal .close-btn:hover) {
  color: #1a507a;
}
::v-deep(.custom-report-modal .modal-footer) {
  padding: 12px;
  background: #f8f9fa;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: center;
  width: 100%;
  box-sizing: border-box;
}

.report-modal-body {
  padding: 0;
}
.report-row {
  display: flex;
  background: #fff;
  min-height: 120px;
}
.report-label-col {
  width: 150px;
  background: #f5f5f5;
  border-right: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 16px;
  font-weight: 500;
  color: #333;
}
.report-input-col {
  flex: 1;
  padding: 16px;
  display: flex;
  align-items: center;
}
.report-textarea {
  width: 100%;
  border: 1px solid #a5cae4;
  border-radius: 4px;
  padding: 8px;
  outline: none;
  font-size: 0.95rem;
  resize: vertical;
  box-sizing: border-box;
}
.report-textarea:focus {
  border-color: #3498db;
  box-shadow: 0 0 4px rgba(52, 152, 219, 0.3);
}
.report-modal-footer {
  width: 100%;
  display: flex;
  justify-content: center;
}
.btn-submit-report {
  background-color: #3498db;
  color: #fff;
  border: none;
  padding: 6px 24px;
  border-radius: 4px;
  font-size: 0.9rem;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.2s;
}
.btn-submit-report:hover:not(:disabled) {
  background-color: #2980b9;
}
.btn-submit-report:disabled {
  background-color: #bdc3c7;
  cursor: not-allowed;
}
</style>
