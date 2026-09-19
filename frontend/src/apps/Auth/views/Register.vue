<template>
  <div class="login-page">
    <Loading :visible="loading" />
    <div class="card login-card">
      <div class="card-header">ĐĂNG KÝ THÀNH VIÊN</div>

      <!-- Đăng ký nhanh hơn bằng Google (voz.vn style) -->
      <div class="quick-register-header">
        <span class="quick-title">Đăng ký nhanh hơn bằng:</span>
        <button type="button" class="btn-google-login" @click="openGoogleConfirm">
          <svg class="google-icon" viewBox="0 0 24 24" width="18" height="18">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
          </svg>
          <span>Google</span>
        </button>
      </div>

      <div class="auth-divider">
        <span>hoặc điền thủ công</span>
      </div>

      <form @submit.prevent="handleRegister" class="login-form">
        <div class="form-group">
          <label>Tên hiển thị <span class="required">*</span></label>
          <input v-model="displayName" required>
          <small class="hint">Đây là tên hiển thị ở mỗi bài viết của bạn. Bạn có thể dùng bất cứ tên nào mình muốn. Một khi đã đặt thì không thể đổi.</small>
        </div>
        <div class="form-group">
          <label>Email <span class="required">*</span></label>
          <input type="email" v-model="email" required>
        </div>
        <div class="form-group">
          <label>Mật khẩu <span class="required">*</span></label>
          <div class="password-wrapper">
            <input :type="showPassword ? 'text' : 'password'" v-model="password" required>
            <span class="toggle-icon" @click="showPassword = !showPassword">
              <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
            </span>
          </div>
          <PasswordStrengthMeter :password="password" />
        </div>
        <div class="form-group">
          <label>Xác nhận <span class="required">*</span></label>
          <div id="turnstile-container"></div>
        </div>
        <div v-if="error" class="error-msg">{{ error }}</div>
        <div v-if="success" class="success-msg">{{ success }}</div>
        <button type="submit" class="btn-login">ĐĂNG KÝ NGAY</button>
        <div style="margin-top: 1rem; text-align: center;">
          Đã có tài khoản? <router-link :to="{ name: 'Login' }">Đăng nhập</router-link>
        </div>
      </form>
    </div>

    <!-- Popup Xác nhận Tiếp tục với Google (Hình 1) -->
    <GoogleConfirmModal 
      :visible="showGoogleModal" 
      @confirm="handleGoogleConfirmed" 
      @close="showGoogleModal = false" 
    />
  </div>
</template>

<script>
import AuthService from '@/apps/Auth/services/auth.service'
import Loading from '@/shared/components/Loading.vue'
import PasswordStrengthMeter from '@/shared/components/PasswordStrengthMeter.vue'
import GoogleConfirmModal from '@/apps/Auth/components/GoogleConfirmModal.vue'
import { initGoogleAuth, promptGoogleLogin } from '@/shared/utils/googleAuth'

export default {
  name: 'Register',
  components: {
    Loading,
    PasswordStrengthMeter,
    GoogleConfirmModal
  },
  data() {
    return {
      displayName: '',
      email: '',
      password: '',
      showPassword: false,
      error: '',
      success: '',
      loading: false,
      turnstileWidgetId: null,
      turnstileToken: '',
      showGoogleModal: false
    }
  },
  mounted() {
    initGoogleAuth(this.handleGoogleCredentialResponse)
    this.initTurnstile()
  },
  methods: {
    initTurnstile() {
      if (window.turnstile) {
        this.renderTurnstile()
      } else {
        const interval = setInterval(() => {
          if (window.turnstile) {
            this.renderTurnstile()
            clearInterval(interval)
          }
        }, 300)
      }
    },
    renderTurnstile() {
      const siteKey = process.env.VUE_APP_TURNSTILE_SITE_KEY || '1x00000000000000000000AA'
      const container = document.getElementById('turnstile-container')
      if (window.turnstile && container && this.turnstileWidgetId === null) {
        try {
          container.innerHTML = ''
          this.turnstileWidgetId = window.turnstile.render('#turnstile-container', {
            sitekey: siteKey,
            size: 'flexible',
            callback: (token) => {
              this.turnstileToken = token
              this.error = ''
            },
            'expired-callback': () => {
              this.turnstileToken = ''
            },
            'error-callback': () => {
              this.turnstileToken = ''
            }
          })
        } catch (e) {
          console.error('Lỗi khởi tạo Cloudflare Turnstile:', e)
        }
      }
    },
    resetTurnstile() {
      if (window.turnstile && this.turnstileWidgetId !== null) {
        try {
          window.turnstile.reset(this.turnstileWidgetId)
        } catch (e) {
          // ignore
        }
        this.turnstileToken = ''
      }
    },
    async handleRegister() {
      const token = this.turnstileToken || (window.turnstile && this.turnstileWidgetId !== null ? window.turnstile.getResponse(this.turnstileWidgetId) : '')
      if (!token) {
        this.error = 'Vui lòng xác nhận mã chống Bot (Cloudflare Turnstile)'
        return
      }

      this.loading = true
      this.error = ''
      this.success = ''
      try {
        const res = await AuthService.register({
          displayName: this.displayName,
          password: this.password,
          email: this.email,
          turnstileToken: token
        })
        
        // Tự động lưu session đăng nhập vai trò ROLE_NON_OFFICIAL_USER
        if (res.data && res.data.token) {
          localStorage.setItem('token', res.data.token)
          localStorage.setItem('user', JSON.stringify(res.data))
          window.dispatchEvent(new Event('storage'))
        }

        // Điều hướng sang màn hình thông báo chờ xác nhận email
        await this.$router.push({
          name: 'RegisterComplete',
          query: {
            email: this.email,
            emailSent: res.data?.emailSent ? 'true' : 'false'
          }
        })
      } catch (err) {
        this.error = err.response?.data?.message || 'Đã có lỗi xảy ra'
        this.resetTurnstile()
      } finally {
        this.loading = false
      }
    },
    openGoogleConfirm() {
      this.error = ''
      this.showGoogleModal = true
    },
    handleGoogleConfirmed(mockEmail) {
      this.showGoogleModal = false
      promptGoogleLogin(this.handleGoogleCredentialResponse, mockEmail)
    },
    async handleGoogleCredentialResponse(googleResponse) {
      if (!googleResponse || !googleResponse.credential) return

      this.loading = true
      this.error = ''

      try {
        const res = await AuthService.verifyGoogle(googleResponse.credential)

        if (res.data.isExistingUser) {
          // Tài khoản đã có -> Đăng nhập thành công
          localStorage.setItem('token', res.data.token)
          localStorage.setItem('user', JSON.stringify(res.data))
          window.dispatchEvent(new Event('auth-change'))
          this.$router.push({ name: 'Home' })
        } else {
          // Tài khoản mới -> Chuyển sang màn hình Đăng ký bằng Google để tự nhập Tên hiển thị (Hình 3)
          sessionStorage.setItem('pending_google_auth', JSON.stringify({
            email: res.data.email,
            suggestedDisplayName: res.data.suggestedDisplayName,
            avatar: res.data.avatar,
            idToken: res.data.idToken
          }))

          this.$router.push({ name: 'GoogleRegisterComplete' })
        }
      } catch (err) {
        this.error = err.response?.data?.message || 'Xác thực tài khoản Google thất bại.'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #004a7c;
  padding: 2rem 1rem;
}
.login-card { width: 400px; }
.login-form { padding: 1.5rem 2rem 2rem 2rem; }
.form-group { margin-bottom: 1.5rem; }
.form-group label { display: block; margin-bottom: 0.5rem; font-weight: bold; color: #1a507a; }
.required { color: #e74c3c; margin-left: 2px; }
.form-group input { width: 100%; padding: 0.75rem; border: 1px solid #ddd; border-radius: 4px; outline: none; margin-bottom: 4px; box-sizing: border-box; }
.form-group input:focus { border-color: #1a507a; }
.hint { font-size: 0.75rem; color: #777; display: block; margin-bottom: 0.25rem; }

.password-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.toggle-icon {
  position: absolute;
  right: 12px;
  cursor: pointer;
  color: #666;
  display: flex;
  align-items: center;
  user-select: none;
}

.toggle-icon:hover {
  color: #1a507a;
}

.btn-login { width: 100%; background: #1a507a; color: white; border: none; padding: 1rem; border-radius: 4px; font-weight: bold; cursor: pointer; margin-top: 0.5rem; transition: background 0.3s; }
.btn-login:hover { background: #154267; }
.error-msg { color: #e74c3c; margin-bottom: 1rem; text-align: center; font-size: 0.9rem; }
.success-msg { color: #27ae60; margin-bottom: 1rem; text-align: center; font-size: 0.9rem; }

#turnstile-container {
  width: 100%;
  min-height: 65px;
}

#turnstile-container :deep(iframe),
#turnstile-container iframe {
  width: 100% !important;
  min-width: 100% !important;
  max-width: 100% !important;
  display: block !important;
}

/* Đăng ký nhanh bằng Google (voz style) */
.quick-register-header {
  background: #f8fafc;
  padding: 12px 20px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.quick-title {
  font-size: 0.88rem;
  font-weight: 600;
  color: #334155;
}
.btn-google-login {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  padding: 6px 14px;
  font-size: 0.9rem;
  font-weight: 600;
  color: #1e293b;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: all 0.2s ease;
}
.btn-google-login:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.auth-divider {
  position: relative;
  text-align: center;
  margin: 1.2rem 2rem 0.2rem 2rem;
}
.auth-divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #e2e8f0;
}
.auth-divider span {
  position: relative;
  background: #ffffff;
  padding: 0 10px;
  font-size: 0.8rem;
  color: #94a3b8;
  text-transform: lowercase;
}
</style>
