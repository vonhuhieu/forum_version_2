<template>
  <div>
    <Loading :visible="isLoading" />

    <main class="container" style="padding-top: 2rem;">
      <!-- Breadcrumb -->
      <Breadcrumb :items="breadcrumbItems" />

      <div class="account-layout">
        <!-- Cột trái: Sidebar -->
        <AccountSidebar activeMenu="password" />

        <!-- Cột phải: Form cập nhật -->
        <div class="account-content">
          <div class="password-card card">
            <form @submit.prevent="handleSave" class="password-form">
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
                  <span class="field-help">Vì lý do an ninh, bạn phải xác minh mật khẩu hiện tại trước khi đặt mật khẩu mới.</span>
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
                  <span class="field-help">Bắt buộc nhập mật khẩu.</span>
                </div>
              </div>

              <!-- Hàng 3: Xác nhận mật khẩu mới -->
              <div class="form-row">
                <div class="form-label-col">
                  <label for="confirm-password">Xác nhận mật khẩu mới:</label>
                </div>
                <div class="form-input-col">
                  <div class="password-input-wrapper">
                    <input 
                      id="confirm-password"
                      :type="showConfirmPassword ? 'text' : 'password'" 
                      v-model="confirmPassword" 
                      required
                      class="form-input"
                    />
                    <button 
                      type="button" 
                      class="btn-toggle-pass" 
                      @click="showConfirmPassword = !showConfirmPassword"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-eye">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                        <circle cx="12" cy="12" r="3"></circle>
                      </svg>
                      <span>{{ showConfirmPassword ? 'Ẩn' : 'Hiện' }}</span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- Thanh lưu dưới cùng -->
              <div class="form-footer">
                <button type="submit" class="btn-save">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-save">
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                    <polyline points="17 21 17 13 7 13 7 21"></polyline>
                    <polyline points="7 3 7 8 15 8"></polyline>
                  </svg>
                  Lưu
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
import api from '@/shared/services/api.service'
import { toastSuccess, toastError } from '@/shared/utils/swal'

export default {
  name: 'ChangePassword',
  components: {
    Breadcrumb,
    AccountSidebar,
    Loading
  },
  data() {
    return {
      isLoading: false,
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
      showCurrentPassword: false,
      showNewPassword: false,
      showConfirmPassword: false
    }
  },
  computed: {
    breadcrumbItems() {
      return [
        { title: 'Tài khoản của bạn', to: '/account/profile' },
        { title: 'Mật khẩu', active: true }
      ]
    }
  },
  methods: {
    async handleSave() {
      if (!this.currentPassword || !this.newPassword || !this.confirmPassword) {
        toastError('Vui lòng nhập đầy đủ các trường thông tin.')
        return
      }

      if (this.newPassword !== this.confirmPassword) {
        toastError('Mật khẩu mới và xác nhận mật khẩu mới không trùng khớp.')
        return
      }

      this.isLoading = true
      try {
        await api.put('/users/me/change-password', {
          currentPassword: this.currentPassword,
          newPassword: this.newPassword,
          confirmPassword: this.confirmPassword
        })
        
        toastSuccess('Cập nhật mật khẩu thành công!')
        
        // Reset form
        this.currentPassword = ''
        this.newPassword = ''
        this.confirmPassword = ''
        this.showCurrentPassword = false
        this.showNewPassword = false
        this.showConfirmPassword = false
      } catch (err) {
        let msg = 'Có lỗi xảy ra khi cập nhật mật khẩu.'
        if (err.response && err.response.data && err.response.data.message) {
          msg = err.response.data.message
        }
        toastError(msg)
      } finally {
        this.isLoading = false
      }
    }
  }
}
</script>

<style scoped>
.page-title {
  font-size: 1.75rem;
  color: #1a507a;
  margin-top: 0;
  margin-bottom: 1.5rem;
  font-weight: 500;
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

/* Card layout */
.password-card {
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

/* Row structure */
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

/* Password input field & eye toggle */
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

.icon-eye {
  flex-shrink: 0;
}

.field-help {
  font-size: 0.82rem;
  color: #7f8c8d;
  line-height: 1.4;
}

/* Save section style */
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

.icon-save {
  flex-shrink: 0;
}

/* Responsive queries */
@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
  }
  
  .form-label-col {
    width: 100%;
    text-align: left;
    justify-content: flex-start;
    background-color: #fcfcfd;
    border-right: none;
    border-bottom: 1px solid #eceef1;
    padding: 10px 15px;
  }
  
  .form-label-col label {
    margin-top: 0;
  }
  
  .form-input-col {
    width: 100%;
    padding: 15px;
  }
  
  .password-input-wrapper {
    max-width: 100%;
  }
}
</style>
