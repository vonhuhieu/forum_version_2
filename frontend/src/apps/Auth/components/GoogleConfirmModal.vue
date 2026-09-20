<template>
  <div class="google-modal-overlay" v-if="visible" @click.self="$emit('close')">
    <div class="google-modal-card">
      <div class="google-modal-header">
        <span class="google-modal-title">Tiếp tục với Google</span>
        <button class="google-modal-close" @click="$emit('close')" aria-label="Đóng">&times;</button>
      </div>
      <div class="google-modal-body">
        <p class="google-modal-prompt">Bạn có chắc chắn muốn tiếp tục liên kết tài khoản này?</p>
        <div class="google-brand-display">
          <svg class="google-icon-svg" viewBox="0 0 24 24" width="28" height="28">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
          </svg>
          <span class="google-brand-name">Google</span>
        </div>
        <div v-if="!hasGoogleClientId" class="dev-mock-box">
          <label class="dev-mock-label">Tài khoản Google (Thử nghiệm Local):</label>
          <input 
            type="email" 
            v-model="mockEmail" 
            class="dev-mock-input" 
            placeholder="vd: test.thanhvien@gmail.com"
            @keyup.enter="handleConfirm"
          />
        </div>
        <div class="google-modal-actions">
          <button class="btn-google-confirm" @click="handleConfirm">
            <span class="check-icon">✓</span> Xác nhận
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'GoogleConfirmModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  emits: ['confirm', 'close'],
  data() {
    const clientId = process.env.VUE_APP_GOOGLE_CLIENT_ID
    const hasClient = !!(clientId && !clientId.startsWith('${'))
    return {
      hasGoogleClientId: hasClient,
      mockEmail: 'hoptacxavuive.member@gmail.com'
    }
  },
  methods: {
    handleConfirm() {
      if (this.hasGoogleClientId) {
        this.$emit('confirm')
      } else {
        const email = this.mockEmail?.trim() || 'hoptacxavuive.member@gmail.com'
        this.$emit('confirm', email)
      }
    }
  }
}
</script>

<style scoped>
.google-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  padding: 16px;
  animation: fadeIn 0.15s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.google-modal-card {
  background: #ffffff;
  width: 100%;
  max-width: 480px;
  border-radius: 8px;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.22);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: scaleUp 0.18s ease-out;
}

@keyframes scaleUp {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}

.google-modal-header {
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.google-modal-title {
  font-size: 1.15rem;
  font-weight: 600;
  color: #1e3a8a;
}

.google-modal-close {
  background: transparent;
  border: none;
  font-size: 24px;
  color: #64748b;
  cursor: pointer;
  line-height: 1;
  padding: 0 4px;
  transition: color 0.15s;
}

.google-modal-close:hover {
  color: #0f172a;
}

.google-modal-body {
  padding: 24px 24px 28px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.google-modal-prompt {
  font-size: 0.96rem;
  color: #334155;
  margin: 0 0 16px 0;
  line-height: 1.5;
}

.google-brand-display {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 24px;
}

.google-brand-name {
  font-size: 1.4rem;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.3px;
}

.dev-mock-box {
  width: 100%;
  margin-bottom: 20px;
  text-align: left;
  background: #f8fafc;
  padding: 10px 14px;
  border-radius: 6px;
  border: 1px dashed #94a3b8;
}

.dev-mock-label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
}

.dev-mock-input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  font-size: 0.9rem;
  outline: none;
  box-sizing: border-box;
}

.dev-mock-input:focus {
  border-color: #2563eb;
}

.google-modal-actions {
  width: 100%;
  display: flex;
  justify-content: center;
}

.btn-google-confirm {
  background: #2563eb;
  color: #ffffff;
  border: none;
  font-size: 0.95rem;
  font-weight: 600;
  padding: 10px 32px;
  border-radius: 6px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.35);
  transition: all 0.2s ease;
}

.btn-google-confirm:hover {
  background: #1d4ed8;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.45);
  transform: translateY(-1px);
}

.check-icon {
  font-size: 1.05rem;
  font-weight: bold;
}
</style>
