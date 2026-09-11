<template>
  <div class="login-page">
    <Loading :visible="isLoading" />
    <div class="card login-card">
      <div class="card-header">ĐĂNG NHẬP HỆ THỐNG</div>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label>Email</label>
          <input v-model="email" required placeholder="example@domain.com">
        </div>
        <div class="form-group">
          <label>Mật khẩu</label>
          <div class="password-wrapper">
            <input :type="showPassword ? 'text' : 'password'" v-model="password" required placeholder="Nhập mật khẩu">
            <span class="toggle-icon" @click="showPassword = !showPassword">
              <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
            </span>
          </div>
        </div>
        <div class="remember-group">
          <input type="checkbox" id="remember" v-model="rememberMe">
          <label for="remember">Nhớ mật khẩu</label>
        </div>
        <div v-if="error" class="error-msg">{{ error }}</div>
        <button type="submit" class="btn-login">VÀO HỆ THỐNG</button>
        <div style="margin-top: 1rem; text-align: center; display: flex; justify-content: space-between;">
          <router-link :to="{ name: 'Home' }">Quay lại trang chủ</router-link>
          <router-link :to="{ name: 'ForgotPassword' }" style="color: #e74c3c;">Quên mật khẩu?</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import AuthService from '@/apps/Auth/services/auth.service'
import Loading from '@/shared/components/Loading.vue'

export default {
  name: 'Login',
  components: {
    Loading
  },
  data() {
    return {
      email: '',
      password: '',
      showPassword: false,
      rememberMe: false,
      error: '',
      isLoading: false
    }
  },
  mounted() {
    const savedEmail = localStorage.getItem('remembered_email') || localStorage.getItem('remembered_username')
    const savedPass = localStorage.getItem('remembered_password')
    if (savedEmail && savedPass) {
      this.email = savedEmail
      this.password = savedPass
      this.rememberMe = true
    }
  },
  methods: {
    async handleLogin() {
      this.isLoading = true
      this.error = ''
      try {
        const response = await AuthService.login({
          email: this.email,
          username: this.email,
          password: this.password
        })

        if (this.rememberMe) {
          localStorage.setItem('remembered_email', this.email)
          localStorage.setItem('remembered_password', this.password)
        } else {
          localStorage.removeItem('remembered_email')
          localStorage.removeItem('remembered_username')
          localStorage.removeItem('remembered_password')
        }

        localStorage.setItem('token', response.data.token)
        localStorage.setItem('user', JSON.stringify(response.data))
        const roles = response.data.roles || []
        if (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_SUPER_ADMIN')) {
          this.$router.push({ name: 'AdminMenu' })
        } else {
          this.$router.push({ name: 'Home' })
        }
      } catch (err) {
        this.error = 'Email hoặc mật khẩu không chính xác'
      } finally {
        this.isLoading = false
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
.form-group input { width: 100%; padding: 0.75rem; border: 1px solid #ddd; border-radius: 4px; outline: none; }
.form-group input:focus { border-color: #1a507a; }

.remember-group {
  display: flex;
  align-items: center;
  margin-bottom: 1.5rem;
}
.remember-group input {
  margin-right: 0.5rem;
  width: auto;
  cursor: pointer;
}
.remember-group label {
  color: #1a507a;
  font-size: 0.95rem;
  cursor: pointer;
}

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

.btn-login { width: 100%; background: #1a507a; color: white; border: none; padding: 1rem; border-radius: 4px; font-weight: bold; cursor: pointer; transition: background 0.3s; }
.btn-login:hover { background: #154267; }
.error-msg { color: #e74c3c; margin-bottom: 1rem; text-align: center; font-size: 0.9rem; }
</style>
