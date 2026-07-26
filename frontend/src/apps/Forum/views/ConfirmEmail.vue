<template>
  <div>
    <Loading :visible="isLoading" />

    <main class="container" style="padding-top: 2rem; padding-bottom: 4rem;">
      <!-- Banner Onboarding Kích hoạt -->
      <div class="pending-approval-banner-wrapper" style="margin-top: 0; margin-bottom: 1.5rem;">
        <div class="pending-approval-banner">
          Vui lòng nhập mật khẩu hiện tại của bạn để hoàn tất xác thực email và nâng cấp thành viên chính thức.
        </div>
      </div>

      <!-- Breadcrumb -->
      <Breadcrumb :items="breadcrumbItems" />

      <div class="account-layout">
        <!-- Cột trái: Sidebar -->
        <AccountSidebar activeMenu="password" />

        <!-- Cột phải: Form Onboarding Kích hoạt -->
        <div class="account-content">
          <!-- Trường hợp Token quá hạn 24h -->
          <div v-if="isExpired" class="expired-card card">
            <div class="expired-body">
              <div class="expired-title">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-warning">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="8" x2="12" y2="12"></line>
                  <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                Liên kết xác minh đã hết hạn
              </div>
              <p class="expired-desc">
                Liên kết xác minh email này đã hết hạn (chỉ có hiệu lực trong vòng 24 giờ). Vui lòng điền email của bạn bên dưới để nhận lại email xác nhận mới.
              </p>

              <div class="resend-box">
                <input type="email" v-model="resendEmail" placeholder="Nhập email của bạn..." class="form-input resend-input" />
                <button class="btn-resend-expired" @click="handleResendExpired" :disabled="isLoading">
                  Gửi lại email xác nhận
                </button>
              </div>
            </div>
          </div>

          <!-- Màn hình Form Onboarding Nhập Mật khẩu & Cập nhật Mật khẩu mới -->
          <div v-else class="password-card card">
            <form @submit.prevent="handleConfirm" class="password-form">
              <!-- Hàng 1: Mật khẩu hiện tại -->
              <div class="form-row">
                <div class="form-label-col">
                  <label for="current-password">Mật khẩu hiện tại của bạn:</label>
                </div>
                <div class="form-input-col">
                  <div class="password-input-wrapper">
                    <input 
                      id="current-password"
                      :type="showCurrentPassword ? 'text' : 'password'" 
                      v-model="currentPassword" 
                      required
                      placeholder="Nhập mật khẩu hiện tại"
                      class="form-input"
                    />
                    <button 
                      type="button" 
                      class="btn-toggle-pass" 
                      @click="showCurrentPassword = !showCurrentPassword"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-eye">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                        <circle cx="12" cy="12" r="3"></circle>
                      </svg>
                      <span>{{ showCurrentPassword ? 'Ẩn' : 'Hiện' }}</span>
                    </button>
                  </div>
                  <span class="field-help">Vì lý do an ninh, bạn phải xác minh mật khẩu hiện tại trước khi kích hoạt thành viên.</span>
                </div>
              </div>

              <!-- Hàng 2: Mật khẩu mới -->
              <div class="form-row">
                <div class="form-label-col">
                  <label for="new-password">Mật khẩu mới:</label>
                </div>
                <div class="form-input-col">
                  <div class="password-input-wrapper">
                    <input 
                      id="new-password"
                      :type="showNewPassword ? 'text' : 'password'" 
                      v-model="newPassword" 
                      required
                      placeholder="Nhập mật khẩu mới"
                      class="form-input"
                    />
                    <button 
                      type="button" 
                      class="btn-toggle-pass" 
                      @click="showNewPassword = !showNewPassword"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-eye">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                        <circle cx="12" cy="12" r="3"></circle>
                      </svg>
                      <span>{{ showNewPassword ? 'Ẩn' : 'Hiện' }}</span>
                    </button>
                  </div>
                  <span class="field-help">Bắt buộc nhập mật khẩu mới và phải khác mật khẩu hiện tại.</span>
                </div>
              </div>

              <!-- Hàng 3: Xác nhận mật khẩu mới -->
              <div class="form-row">
                <div class="form-label-col">
                  <label for="confirm-new-password">Xác nhận mật khẩu mới:</label>
                </div>
                <div class="form-input-col">
                  <div class="password-input-wrapper">
                    <input 
                      id="confirm-new-password"
                      :type="showConfirmNewPassword ? 'text' : 'password'" 
                      v-model="confirmNewPassword" 
                      required
                      placeholder="Nhập lại mật khẩu mới"
                      class="form-input"
                    />
                    <button 
                      type="button" 
                      class="btn-toggle-pass" 
                      @click="showConfirmNewPassword = !showConfirmNewPassword"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-eye">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                        <circle cx="12" cy="12" r="3"></circle>
                      </svg>
                      <span>{{ showConfirmNewPassword ? 'Ẩn' : 'Hiện' }}</span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- Thanh lưu dưới cùng -->
              <div class="form-footer">
                <button type="submit" class="btn-save" :disabled="isLoading">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-save">
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                    <polyline points="17 21 17 13 7 13 7 21"></polyline>
                    <polyline points="7 3 7 8 15 8"></polyline>
                  </svg>
                  Lưu &amp; Kích hoạt
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import AccountSidebar from '@/shared/components/AccountSidebar.vue'
import Loading from '@/shared/components/Loading.vue'
import AuthService from '@/apps/Auth/services/auth.service'
import { alertSuccess, toastSuccess, toastError } from '@/shared/utils/swal'

export default {
  name: 'ConfirmEmail',
  components: {
    Breadcrumb,
    AccountSidebar,
    Loading
  },
  data() {
    return {
      isLoading: false,
      token: '',
      currentPassword: '',
      newPassword: '',
      confirmNewPassword: '',
      showCurrentPassword: false,
      showNewPassword: false,
      showConfirmNewPassword: false,
      isExpired: false,
      userEmail: '',
      resendEmail: ''
    }
  },
  computed: {
    breadcrumbItems() {
      return [
        { title: 'Tài khoản của bạn', to: '#' },
        { title: 'Kích hoạt tài khoản', active: true }
      ]
    }
  },
  async mounted() {
    this.token = this.$route.query.token || ''
    if (!this.token) {
      toastError('Không tìm thấy mã xác minh email trong liên kết.')
      this.isExpired = true
      return
    }

    this.isLoading = true
    try {
      const res = await AuthService.verifyConfirmationToken(this.token)
      if (res.data) {
        this.userEmail = res.data.email || ''
        this.resendEmail = this.userEmail
      }
    } catch (err) {
      const msg = err.response?.data?.message || ''
      if (msg.includes('EXPIRED') || msg.includes('hết hạn')) {
        this.isExpired = true
      } else {
        toastError(msg || 'Mã xác minh không hợp lệ.')
      }
    } finally {
      this.isLoading = false
    }
  },
  methods: {
    async handleConfirm() {
      if (!this.currentPassword || !this.newPassword || !this.confirmNewPassword) {
        toastError('Vui lòng điền đầy đủ tất cả các trường mật khẩu.')
        return
      }

      if (this.newPassword !== this.confirmNewPassword) {
        toastError('Mật khẩu mới và xác nhận mật khẩu mới không trùng khớp.')
        return
      }

      if (this.newPassword === this.currentPassword) {
        toastError('Mật khẩu mới phải khác với mật khẩu hiện tại.')
        return
      }

      this.isLoading = true
      try {
        await AuthService.confirmEmail(this.token, this.currentPassword, this.newPassword)
        
        // Logout tài khoản tạm thời
        localStorage.removeItem('token')
        localStorage.removeItem('user')

        // Tắt loading trước khi hiện popup
        this.isLoading = false

        // Hiển thị popup SweetAlert2 thông báo cho user
        await alertSuccess('Xác thực tài khoản thành công. Vui lòng đăng nhập lại để tiếp tục')

        // Chuyển hướng sang trang đăng nhập
        this.$router.push({ name: 'Login' })
      } catch (err) {
        const msg = err.response?.data?.message || 'Có lỗi xảy ra khi xác nhận email.'
        if (msg.includes('EXPIRED') || msg.includes('hết hạn')) {
          this.isExpired = true
        } else {
          toastError(msg)
        }
      } finally {
        this.isLoading = false
      }
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
.pending-approval-banner-wrapper {
  margin-bottom: 1.5rem;
  width: 100%;
}

.pending-approval-banner {
  background-color: #f4f9fc;
  border: 1px solid #d3e6f2;
  color: #1a507a;
  padding: 12px 20px;
  border-radius: 4px;
  font-size: 0.95rem;
  text-align: left;
  line-height: 1.5;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.account-layout {
  display: flex;
  gap: 2rem;
  margin-bottom: 3rem;
}

@media (max-width: 992px) {
  .account-layout {
    flex-direction: column;
    gap: 0;
  }
}

.account-content {
  flex: 1;
  min-width: 0;
}

/* Password Card */
.password-card, .expired-card {
  background: #ffffff;
  border: 1px solid #d8dbe0;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.password-form {
  display: flex;
  flex-direction: column;
}

.form-row {
  display: flex;
  border-bottom: 1px solid #eceef1;
  min-height: 50px;
}

.form-label-col {
  width: 30%;
  padding: 18px 24px;
  background-color: #fcfcfd;
  text-align: right;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  border-right: 1px solid #eceef1;
  font-weight: 500;
  color: #2c3e50;
  flex-shrink: 0;
}

.form-label-col label {
  margin-top: 6px;
  font-size: 0.95rem;
  font-weight: 600;
}

.form-input-col {
  width: 70%;
  padding: 18px 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.password-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  max-width: 450px;
  width: 100%;
}

.form-input {
  width: 100%;
  padding: 8px 80px 8px 12px;
  font-size: 0.95rem;
  border: 1px solid #c8d4e0;
  border-radius: 4px;
  outline: none;
  transition: border-color 0.2s;
  height: 38px;
  font-family: inherit;
}

.form-input:focus {
  border-color: #1a507a;
  box-shadow: 0 0 0 2px rgba(26, 80, 122, 0.1);
}

.btn-toggle-pass {
  position: absolute;
  right: 1px;
  top: 1px;
  bottom: 1px;
  background-color: #f0f4f8;
  border: none;
  border-left: 1px solid #c8d4e0;
  border-top-right-radius: 3px;
  border-bottom-right-radius: 3px;
  padding: 0 15px;
  font-size: 0.85rem;
  color: #1a507a;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: background-color 0.2s;
  user-select: none;
  height: 36px;
}

.btn-toggle-pass:hover {
  background-color: #e2ebf3;
}

.field-help {
  font-size: 0.82rem;
  color: #7f8c8d;
  line-height: 1.4;
}

.form-footer {
  background-color: #f0f4f8;
  padding: 15px;
  display: flex;
  justify-content: center;
  border-top: 1px solid #d8dbe0;
}

.btn-save {
  background-color: #3498db;
  color: white;
  border: 1px solid #2980b9;
  border-radius: 4px;
  padding: 8px 24px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background-color 0.2s;
}

.btn-save:hover {
  background-color: #2980b9;
}

/* Expired Card Styles */
.expired-body {
  padding: 30px;
  display: flex;
  flex-direction: column;
  gap: 15px;
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
  max-width: 500px;
}

.resend-input {
  padding-right: 12px !important;
}

.btn-resend-expired {
  background-color: #1a507a;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 0 18px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.2s;
}

.btn-resend-expired:hover {
  background-color: #154267;
}

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
  }
  
  .form-label-col {
    width: 100%;
    text-align: left;
    justify-content: flex-start;
    padding: 10px 15px;
  }
  
  .form-input-col {
    width: 100%;
    padding: 15px;
  }
  
  .resend-box {
    flex-direction: column;
    max-width: 100%;
  }

  .btn-resend-expired {
    height: 38px;
  }
}
</style>
