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
          <label>Xác nhận mật khẩu <span class="required">*</span></label>
          <div class="password-wrapper">
            <input :type="showConfirmPassword ? 'text' : 'password'" v-model="confirmPassword" required placeholder="Nhập lại mật khẩu">
            <span class="toggle-icon" @click="showConfirmPassword = !showConfirmPassword">
              <svg v-if="showConfirmPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
            </span>
          </div>
        </div>
        
        <div class="form-group">
          <label>Xác nhận <span class="required">*</span></label>
          <div id="recaptcha-main" class="g-recaptcha" data-sitekey="6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI"></div>
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
      confirmPassword: '',
      showPassword: false,
      showConfirmPassword: false,
      error: '',
      success: '',
      loading: false
    }
  },
  mounted() {
    // If recaptcha is already loaded, render it
    if (window.grecaptcha) {
      this.renderRecaptcha()
    } else {
      // Check every 500ms if recaptcha is loaded
      const interval = setInterval(() => {
        if (window.grecaptcha) {
          this.renderRecaptcha()
          clearInterval(interval)
        }
      }, 500)
    }
  },
  methods: {
    renderRecaptcha() {
      if (window.grecaptcha && document.getElementById('recaptcha-main')) {
        window.grecaptcha.render('recaptcha-main')
      }
    },
    async handleRegister() {
      const recaptchaResponse = window.grecaptcha ? window.grecaptcha.getResponse() : ''
      if (!recaptchaResponse) {
        this.error = 'Vui lòng xác nhận mã reCAPTCHA'
        return
      }

      const usernameRegex = /^[a-zA-Z0-9_]{3,20}$/;
      if (!usernameRegex.test(this.username)) {
        this.error = 'Tên đăng nhập không hợp lệ. Chỉ được phép chứa chữ không dấu, số, gạch dưới (3-20 kí tự) và không có dấu cách.'
        return
      }

      if (this.password !== this.confirmPassword) {
        this.error = 'Mật khẩu xác nhận không khớp'
        return
      }
      this.loading = true
      this.error = ''
      this.success = ''
      try {
        await AuthService.register({
          username: this.username,
          displayName: this.displayName,
          password: this.password,
          email: this.email,
          recaptcha: recaptchaResponse
        })
        this.success = 'Đăng ký thành công! Đang chuyển hướng đến trang đăng nhập...'
        setTimeout(() => {
          this.$router.push({ name: 'Login' })
        }, 2000)
      } catch (err) {
        this.error = err.response?.data?.message || 'Đã có lỗi xảy ra'
        if (window.grecaptcha) {
          window.grecaptcha.reset()
        }
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
</style>
