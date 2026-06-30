<template>
  <div class="login-page">
    <Loading :visible="isLoading" />
    <div class="card login-card">
      <div class="card-header">QUÊN MẬT KHẨU</div>
      
      <!-- Bước 1: Nhập Email -->
      <form v-if="step === STEPS.ENTER_CREDENTIALS" @submit.prevent="handleSendCode" class="login-form">
        <div class="form-group">
          <label>Tên đăng nhập</label>
          <input type="text" v-model="username" required placeholder="Nhập tên đăng nhập">
        </div>
        <div class="form-group">
          <label>Nhập Email đã đăng ký</label>
          <input type="email" v-model="email" required placeholder="example@domain.com">
        </div>
        <div v-if="success" class="success-msg">{{ success }}</div>
        <button type="submit" class="btn-login" :disabled="isLoading">
          {{ isLoading ? 'Đang gửi...' : 'GỬI MÃ XÁC NHẬN' }}
        </button>
        <div style="margin-top: 1rem; text-align: center;">
          <router-link :to="{ name: 'Login' }">Quay lại đăng nhập</router-link>
        </div>
      </form>

      <!-- Bước 2: Đổi mật khẩu -->
      <form v-else-if="step === STEPS.RESET_PASSWORD" @submit.prevent="handleResetPassword" class="login-form">
        <div class="form-group">
          <label>Mã xác nhận (Gửi vào email)</label>
          <input type="text" v-model="code" required placeholder="Nhập mã 6 số">
        </div>
        <div class="form-group">
          <label>Mật khẩu mới</label>
          <div class="password-wrapper">
            <input :type="showPassword ? 'text' : 'password'" v-model="newPassword" required>
            <span class="toggle-icon" @click="showPassword = !showPassword">
              <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
            </span>
          </div>
        </div>
        <div class="form-group">
          <label>Nhập lại mật khẩu mới</label>
          <div class="password-wrapper">
            <input :type="showConfirmPassword ? 'text' : 'password'" v-model="confirmPassword" required>
            <span class="toggle-icon" @click="showConfirmPassword = !showConfirmPassword">
              <svg v-if="showConfirmPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
            </span>
          </div>
        </div>
        <div v-if="error" class="error-msg">{{ error }}</div>
        <div v-if="success" class="success-msg">{{ success }}</div>
        <button type="submit" class="btn-login" :disabled="isLoading">
          {{ isLoading ? 'Đang đổi...' : 'ĐỔI MẬT KHẨU' }}
        </button>
        <div style="margin-top: 1rem; text-align: center;">
          <a href="#" @click.prevent="step = STEPS.ENTER_CREDENTIALS">Gửi lại mã xác nhận</a>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import AuthService from '@/apps/Auth/services/auth.service'
import { FORGOT_PASSWORD_STEPS } from '@/shared/utils/constants'
import Swal from 'sweetalert2'
import Loading from '@/shared/components/Loading.vue'

export default {
  name: 'ForgotPassword',
  components: { Loading },
  data() {
    return {
      STEPS: FORGOT_PASSWORD_STEPS,
      step: FORGOT_PASSWORD_STEPS.ENTER_CREDENTIALS,
      username: '',
      email: '',
      code: '',
      newPassword: '',
      confirmPassword: '',
      showPassword: false,
      showConfirmPassword: false,
      error: '',
      success: '',
      isLoading: false
    }
  },
  methods: {
    async handleSendCode() {
      this.success = ''
      this.isLoading = true
      try {
        const response = await AuthService.forgotPassword(this.username, this.email)
        this.success = response.data.message || 'Đã gửi mã xác nhận'
        setTimeout(() => {
          this.step = FORGOT_PASSWORD_STEPS.RESET_PASSWORD
          this.success = ''
        }, 1500)
      } catch (err) {
        const msg = err.response?.data?.message || 'Tên đăng nhập hoặc email không đúng. Vui lòng kiểm tra lại.'
        Swal.fire({
          icon: 'error',
          title: 'Thông tin không chính xác',
          text: msg,
          confirmButtonText: 'Thử lại',
          confirmButtonColor: '#1a507a'
        })
      } finally {
        this.isLoading = false
      }
    },
    async handleResetPassword() {
      this.error = ''
      this.success = ''
      if (this.newPassword !== this.confirmPassword) {
        this.error = 'Mật khẩu nhập lại không khớp'
        return
      }
      this.isLoading = true
      try {
        const response = await AuthService.resetPassword({
          email: this.email,
          code: this.code,
          newPassword: this.newPassword
        })
        this.success = response.data.message || 'Đổi mật khẩu thành công!'
        setTimeout(() => {
          this.$router.push({ name: 'Login' })
        }, 2000)
      } catch (err) {
        this.error = err.response?.data?.message || 'Mã xác nhận không đúng hoặc đã hết hạn'
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
.btn-login:hover:not(:disabled) { background: #154267; }
.btn-login:disabled { background: #95a5a6; cursor: not-allowed; }
.error-msg { color: #e74c3c; margin-bottom: 1rem; text-align: center; font-size: 0.9rem; }
.success-msg { color: #2ecc71; margin-bottom: 1rem; text-align: center; font-size: 0.9rem; }
</style>
