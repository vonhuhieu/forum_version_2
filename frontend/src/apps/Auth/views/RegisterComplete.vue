<template>
  <div class="register-complete-page">
    <Loading :visible="loading" />

    <main class="container" style="padding-top: 1.5rem; padding-bottom: 4rem;">
      <Breadcrumb :items="breadcrumbItems" />

      <div class="complete-wrapper" style="margin-top: 1rem;">
        <!-- Banner 1: Trạng thái chờ xác nhận & nút gửi lại mail -->
        <div class="card status-card" :class="{ 'warning-card': !isEmailSentSuccess }">
          <div class="status-card-body">
            <div class="status-text" v-if="isEmailSentSuccess">
              Tài khoản của bạn hiện đang chờ xác nhận. Email xác nhận đã được gửi đến <strong>{{ email || 'email của bạn' }}</strong>.
            </div>
            <div class="status-text warning-text" v-else>
              Đã xảy ra lỗi trong quá trình gửi email xác thực tài khoản. Vui lòng click button <strong>"Gửi lại email xác nhận"</strong> bên dưới để thử lại.
            </div>
            <div class="resend-action">
              <button class="btn-resend" :class="{ 'btn-resend-warning': !isEmailSentSuccess }" @click="handleResendEmail" :disabled="loading">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-mail">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                  <polyline points="22,6 12,13 2,6"></polyline>
                </svg>
                Gửi lại email xác nhận
              </button>
            </div>
          </div>
        </div>

        <!-- Card 2: Hướng dẫn hoàn tất đăng ký & quay về trang chủ -->
        <div class="card info-card" style="margin-top: 1.5rem;">
          <div class="card-header main-header">Đăng ký thành viên</div>
          <div class="card-body main-body">
            <p class="thank-msg">
              Cảm ơn bạn đã đăng ký. Để hoàn tất đăng ký, bạn cần làm theo liên kết trong email đã được gửi cho bạn.
            </p>
            <div class="navigation-links">
              <router-link to="/" class="link-home">
                <span class="bullet">›</span> Quay lại trang chủ diễn đàn
              </router-link>
            </div>
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
  name: 'RegisterComplete',
  components: {
    Breadcrumb,
    Loading
  },
  data() {
    return {
      loading: false,
      isEmailSentSuccess: true
    }
  },
  computed: {
    breadcrumbItems() {
      return [
        { title: 'Tài khoản của bạn', to: '#' },
        { title: 'Đăng ký thành viên', active: true }
      ]
    },
    email() {
      return this.$route.query.email || ''
    }
  },
  mounted() {
    if (this.$route.query.emailSent === 'false') {
      this.isEmailSentSuccess = false
    }
  },
  methods: {
    async handleResendEmail() {
      if (!this.email) {
        toastError('Không tìm thấy thông tin email. Vui lòng thử lại.')
        return
      }

      this.loading = true
      try {
        const res = await AuthService.resendConfirmationEmail(this.email)
        toastSuccess(res.data?.message || 'Email xác nhận đã được gửi lại thành công!')
        this.isEmailSentSuccess = true
      } catch (err) {
        const msg = err.response?.data?.message || 'Đã có lỗi xảy ra khi gửi lại email.'
        toastError(msg)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.register-complete-page {
  min-height: 80vh;
  background-color: #f4f6f9;
}

.complete-wrapper {
  max-width: 800px;
  margin: 0 auto;
}

/* Status Card (Top Banner) */
.status-card {
  background: #f0f7ff;
  border: 1px solid #cce3f5;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  transition: all 0.3s ease;
}

.warning-card {
  background-color: #fff9e6 !important;
  border-color: #ffe58f !important;
}

.status-card-body {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-text {
  color: #1a507a;
  font-size: 0.98rem;
  line-height: 1.5;
}

.warning-text {
  color: #d46b08 !important;
}

.status-text strong {
  color: #0d3b66;
}

.resend-action {
  display: flex;
  justify-content: flex-start;
}

.btn-resend {
  background-color: #ffffff;
  border: 1px solid #1a507a;
  color: #1a507a;
  padding: 6px 14px;
  border-radius: 4px;
  font-size: 0.88rem;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.btn-resend:hover {
  background-color: #1a507a;
  color: #ffffff;
}

.btn-resend-warning {
  border-color: #d46b08 !important;
  color: #d46b08 !important;
}

.btn-resend-warning:hover {
  background-color: #d46b08 !important;
  color: #ffffff !important;
}

.btn-resend:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Info Card (Main Box) */
.info-card {
  background: #ffffff;
  border: 1px solid #d8dbe0;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.main-header {
  font-size: 1.2rem;
  font-weight: bold;
  color: #1a507a;
  background-color: #f8f9fa;
  padding: 14px 20px;
  border-bottom: 1px solid #d8dbe0;
}

.main-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.thank-msg {
  font-size: 0.98rem;
  color: #333333;
  margin: 0;
  line-height: 1.6;
}

.navigation-links {
  margin-top: 8px;
}

.link-home {
  color: #1a507a;
  text-decoration: none;
  font-size: 0.95rem;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;

}

.link-home:hover {
  color: #d13838;
  text-decoration: underline;
}

.bullet {
  font-size: 1.1rem;
  font-weight: bold;
}
</style>
