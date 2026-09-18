<template>
  <div>
    <Loading :visible="isLoading" text="Đang xác thực tài khoản..." />

    <main class="container" style="padding-top: 2rem; padding-bottom: 4rem;">
      <!-- Breadcrumb -->
      <Breadcrumb :items="breadcrumbItems" />

      <div class="confirm-container">
        <!-- Trạng thái Xác thực Thành công -->
        <div v-if="isSuccess" class="status-card card success-card">
          <div class="status-body">
            <div class="status-icon success-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#27ae60" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
              </svg>
            </div>
            <h3 class="status-title success-title">Xác thực tài khoản thành công!</h3>
            <p class="status-desc">
              Tài khoản của bạn đã được kích hoạt và nâng cấp thành viên chính thức.<br>
              <span class="auto-redirect-text">Tự động chuyển về Trang chủ sau {{ countdown }} giây...</span>
            </p>
            <div class="action-buttons" style="margin-top: 15px;">
              <button class="btn-primary" @click="goToHome">Về Trang chủ ngay</button>
            </div>
          </div>
        </div>

        <!-- Trường hợp Token quá hạn 24h -->
        <div v-else-if="isExpired" class="expired-card card">
          <div class="expired-body">
            <div class="expired-title">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-warning">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              Liên kết xác thực đã hết hạn
            </div>
            <p class="expired-desc">
              Liên kết xác thực email này đã hết hạn (chỉ có hiệu lực trong vòng 24 giờ). Vui lòng điền email của bạn bên dưới để nhận lại liên kết xác nhận mới.
            </p>

            <div class="resend-box">
              <input type="email" v-model="resendEmail" placeholder="Nhập email của bạn..." class="form-input resend-input" />
              <button class="btn-resend-expired" @click="handleResendExpired" :disabled="isLoading">
                Gửi lại email xác nhận
              </button>
            </div>
          </div>
        </div>

        <!-- Trường hợp Lỗi không hợp lệ hoặc đã xác thực -->
        <div v-else-if="isError" class="expired-card card">
          <div class="expired-body">
            <div class="expired-title" style="color: #e67e22;">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              Thông báo xác thực
            </div>
            <p class="expired-desc">
              {{ errorMessage || 'Mã xác thực email không hợp lệ hoặc tài khoản đã được kích hoạt trước đó.' }}
            </p>
            <div class="action-buttons">
              <button class="btn-primary" @click="$router.push({ name: 'Home' })">Về Trang chủ</button>
              <button class="btn-secondary" @click="$router.push({ name: 'Login' })">Đăng nhập</button>
            </div>
          </div>
        </div>

        <!-- Trạng thái chờ / Đang xác thực -->
        <div v-else class="status-card card">
          <div class="status-body">
            <div class="status-icon">
              <div class="spinner"></div>
            </div>
            <h3 class="status-title">Đang kích hoạt tài khoản...</h3>
            <p class="status-desc">Hệ thống đang tự động xác thực và nâng cấp quyền thành viên cho bạn. Vui lòng chờ trong giây lát.</p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import Loading from '@/shared/components/Loading.vue'
import AuthService from '@/apps/Auth/services/auth.service'
import { toastSuccess, toastError } from '@/shared/utils/swal'

export default {
  name: 'ConfirmEmail',
  components: {
    Breadcrumb,
    Loading
  },
  data() {
    return {
      isLoading: false,
      token: '',
      isSuccess: false,
      countdown: 5,
      countdownTimer: null,
      isExpired: false,
      isError: false,
      errorMessage: '',
      resendEmail: ''
    }
  },
  computed: {
    breadcrumbItems() {
      return [
        { title: 'Trang chủ', to: '/' },
        { title: 'Kích hoạt tài khoản', active: true }
      ]
    }
  },
  async mounted() {
    this.token = this.$route.query.token || ''
    if (!this.token) {
      this.isError = true
      this.errorMessage = 'Không tìm thấy mã xác minh email trong liên kết.'
      toastError(this.errorMessage)
      return
    }

    this.isLoading = true
    try {
      const res = await AuthService.confirmEmail(this.token)
      
      // Tự động lưu JWT Token và thông tin user mới (vai trò ROLE_USER)
      if (res.data && res.data.token) {
        localStorage.setItem('token', res.data.token)
        localStorage.setItem('user', JSON.stringify(res.data))
        window.dispatchEvent(new Event('auth-changed'))
        window.dispatchEvent(new Event('storage'))
      }

      this.isLoading = false
      this.isSuccess = true

      // Đếm ngược 3 giây tự động chuyển về Trang chủ
      this.startCountdown()
    } catch (err) {
      this.isLoading = false
      const msg = err.response?.data?.message || 'Mã xác minh không hợp lệ hoặc tài khoản đã được kích hoạt trước đó.'
      if (msg.includes('EXPIRED') || msg.includes('hết hạn')) {
        this.isExpired = true
      } else {
        this.isError = true
        this.errorMessage = msg
      }
    }
  },
  beforeUnmount() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer)
    }
  },
  methods: {
    startCountdown() {
      this.countdown = 3
      this.countdownTimer = setInterval(() => {
        this.countdown--
        if (this.countdown <= 0) {
          clearInterval(this.countdownTimer)
          this.goToHome()
        }
      }, 1000)
    },
    goToHome() {
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer)
      }
      window.location.href = '/'
    },
    async handleResendExpired() {
      if (!this.resendEmail) {
        toastError('Vui lòng nhập địa chỉ email.')
        return
      }

      this.isLoading = true
      try {
        const res = await AuthService.resendConfirmationEmail(this.resendEmail)
        toastSuccess(res.data?.message || 'Đã gửi lại email xác nhận mới!')
      } catch (err) {
        toastError(err.response?.data?.message || 'Có lỗi xảy ra khi gửi lại email.')
      } finally {
        this.isLoading = false
      }
    }
  }
}
</script>

<style scoped>
.confirm-container {
  max-width: 680px;
  margin: 1.5rem auto 3rem auto;
}

.card {
  background: #ffffff;
  border: 1px solid #d8dbe0;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

/* Status Card */
.status-card {
  padding: 40px 20px;
  text-align: center;
}

.status-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.status-icon {
  margin-bottom: 8px;
}

.spinner {
  width: 44px;
  height: 44px;
  border: 4px solid #e2ebf3;
  border-top-color: #1a507a;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.status-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a507a;
  margin: 0;
}

.success-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background-color: #eafaf1;
}

.success-title {
  color: #27ae60 !important;
  font-size: 1.35rem;
  font-weight: 600;
}

.status-desc {
  font-size: 0.95rem;
  color: #666;
  max-width: 480px;
  line-height: 1.5;
  margin: 0;
}

.auto-redirect-text {
  display: inline-block;
  margin-top: 8px;
  font-size: 0.88rem;
  color: #7f8c8d;
}

/* Expired Card Styles */
.expired-body {
  padding: 30px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.expired-title {
  color: #e74c3c;
  font-size: 1.15rem;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 8px;
}

.expired-desc {
  color: #555555;
  font-size: 0.95rem;
  line-height: 1.5;
  margin: 0;
}

.resend-box {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.form-input {
  flex: 1;
  padding: 9px 12px;
  font-size: 0.95rem;
  border: 1px solid #c8d4e0;
  border-radius: 4px;
  outline: none;
  transition: border-color 0.2s;
  height: 38px;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #1a507a;
  box-shadow: 0 0 0 2px rgba(26, 80, 122, 0.1);
}

.btn-resend-expired {
  background-color: #1a507a;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 0 20px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.2s;
  height: 38px;
}

.btn-resend-expired:hover {
  background-color: #154267;
}

.btn-resend-expired:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 10px;
}

.btn-primary {
  background-color: #1a507a;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 18px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-primary:hover {
  background-color: #154267;
}

.btn-secondary {
  background-color: #e2ebf3;
  color: #1a507a;
  border: none;
  border-radius: 4px;
  padding: 8px 18px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-secondary:hover {
  background-color: #d0e0ed;
}

@media (max-width: 768px) {
  .confirm-container {
    margin: 1rem auto 2rem auto;
    padding: 0 10px;
  }

  .resend-box {
    flex-direction: column;
  }

  .btn-resend-expired {
    width: 100%;
  }

  .action-buttons {
    flex-direction: column;
  }

  .action-buttons button {
    width: 100%;
  }
}
</style>
