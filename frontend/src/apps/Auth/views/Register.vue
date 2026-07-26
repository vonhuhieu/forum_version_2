<template>
  <div class="login-page">
    <Loading :visible="loading" />
    <div class="card login-card">
      <div class="card-header">ĐĂNG KÝ THÀNH VIÊN</div>
      <form @submit.prevent="handleRegister" class="login-form">
        <div class="form-group">
          <label>Tên đăng nhập <span class="required">*</span></label>
          <input v-model="username" required pattern="^[a-zA-Z0-9_]{3,20}$" title="Chỉ cho phép chữ cái không dấu, số và dấu gạch dưới (3-20 ký tự)" placeholder="Chỉ dùng chữ, số, gạch dưới (3-20 ký tự)">
          <small class="hint">Tên này dùng để đăng nhập và không đổi được.</small>
        </div>
        <div class="form-group">
          <label>Tên hiển thị</label>
          <input v-model="displayName" placeholder="Nhập tên sẽ hiện trên diễn đàn (có thể đổi)">
          <small class="hint">Để trống nếu muốn lấy Tên đăng nhập làm Tên hiển thị.</small>
        </div>
        <div class="form-group">
          <label>Email <span class="required">*</span></label>
          <input type="email" v-model="email" required placeholder="example@domain.com">
        </div>
        <div class="form-group">
          <label>Mật khẩu <span class="required">*</span></label>
          <div class="password-wrapper">
            <input :type="showPassword ? 'text' : 'password'" v-model="password" required placeholder="Nhập mật khẩu">
            <span class="toggle-icon" @click="showPassword = !showPassword">
              <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
            </span>
          </div>
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
  </div>
</template>

<script>
import AuthService from '@/apps/Auth/services/auth.service'
import Loading from '@/shared/components/Loading.vue'

export default {
  name: 'Register',
  components: {
    Loading
  },
  data() {
    return {
      username: '',
      displayName: '',
      email: '',
      password: '',
      showPassword: false,
      error: '',
      success: '',
      loading: false,
      turnstileWidgetId: null,
      turnstileToken: ''
    }
  },
  mounted() {
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

      const usernameRegex = /^[a-zA-Z0-9_]{3,20}$/;
      if (!usernameRegex.test(this.username)) {
        this.error = 'Tên đăng nhập không hợp lệ. Chỉ được phép chứa chữ không dấu, số, gạch dưới (3-20 kí tự) và không có dấu cách.'
        return
      }

      this.loading = true
      this.error = ''
      this.success = ''
      try {
        const res = await AuthService.register({
          username: this.username,
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
.login-form { padding: 2rem; }
.form-group { margin-bottom: 1.5rem; }
.form-group label { display: block; margin-bottom: 0.5rem; font-weight: bold; color: #1a507a; }
.required { color: #e74c3c; margin-left: 2px; }
.form-group input { width: 100%; padding: 0.75rem; border: 1px solid #ddd; border-radius: 4px; outline: none; margin-bottom: 4px; }
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
</style>
