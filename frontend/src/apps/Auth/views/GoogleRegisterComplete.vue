<template>
  <div class="google-register-page">
    <Loading :visible="loading" />
    
    <main class="container" style="padding-top: 1.5rem; padding-bottom: 4rem;">
      <Breadcrumb :items="breadcrumbItems" />

      <h1 class="page-title">Đăng ký bằng Google</h1>

      <div class="card register-card">
        <div class="card-tabs">
          <div class="tab-item">Tạo tài khoản mới</div>
        </div>

        <form @submit.prevent="handleCompleteRegister" class="google-form">
          <!-- Tên thành viên / Tên hiển thị -->
          <div class="form-row">
            <div class="row-label">
              <label for="displayName">Tên thành viên:</label>
              <span class="required-badge">Bắt buộc</span>
            </div>
            <div class="row-input">
              <input 
                id="displayName"
                type="text" 
                v-model="displayName" 
                required 
                maxlength="30"
                placeholder="Nhập tên hiển thị bạn mong muốn..."
                class="form-control"
              />
              <p class="field-hint">
                Đây là tên sẽ hiển thị cùng các bài viết của bạn. Bạn có thể dùng bất cứ tên nào tùy thích.
              </p>
            </div>
          </div>

          <!-- Email (Cố định từ Google) -->
          <div class="form-row">
            <div class="row-label">
              <label>Email:</label>
            </div>
            <div class="row-input">
              <div class="email-locked-wrapper">
                <input 
                  type="email" 
                  :value="email" 
                  disabled 
                  class="form-control email-locked-input" 
                />
                <span class="google-verified-badge" title="Email đã được Google xác thực chính chủ">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
                  Đã xác thực
                </span>
              </div>
              <p class="field-hint">
                Địa chỉ email được liên kết cố định từ tài khoản Google của bạn và không thể sửa đổi.
              </p>
            </div>
          </div>

          <!-- Checkbox điều khoản -->
          <div class="form-row agreement-row">
            <div class="row-label"></div>
            <div class="row-input">
              <label class="checkbox-label">
                <input type="checkbox" v-model="agreeTerms" required />
                <span>Tôi đồng ý với <a href="#" @click.prevent>điều khoản</a> và <a href="#" @click.prevent>chính sách quyền riêng tư</a>.</span>
              </label>
            </div>
          </div>

          <!-- Thông báo lỗi -->
          <div v-if="error" class="error-banner">
            {{ error }}
          </div>

          <!-- Nút Đăng ký -->
          <div class="form-row submit-row">
            <div class="row-label"></div>
            <div class="row-input">
              <button type="submit" class="btn-submit-google" :disabled="loading || !agreeTerms">
                Đăng ký
              </button>
            </div>
          </div>
        </form>
      </div>
    </main>
  </div>
</template>

<script>
import AuthService from '@/apps/Auth/services/auth.service'
import Loading from '@/shared/components/Loading.vue'
import Breadcrumb from '@/shared/components/Breadcrumb.vue'

export default {
  name: 'GoogleRegisterComplete',
  components: {
    Loading,
    Breadcrumb
  },
  data() {
    return {
      displayName: '',
      email: '',
      idToken: '',
      avatar: '',
      agreeTerms: true,
      error: '',
      loading: false
    }
  },
  computed: {
    breadcrumbItems() {
      return [
        { title: 'Tài khoản của bạn', to: '#' },
        { title: 'Đăng ký bằng Google', active: true }
      ]
    }
  },
  mounted() {
    // Lấy thông tin Google đã xác thực từ router state hoặc sessionStorage
    const stateData = history.state?.googleData || JSON.parse(sessionStorage.getItem('pending_google_auth') || 'null')
    
    if (!stateData || !stateData.idToken || !stateData.email) {
      // Nếu không có thông tin hợp lệ, quay về trang đăng ký
      this.$router.replace({ name: 'Register' })
      return
    }

    this.email = stateData.email
    this.displayName = stateData.suggestedDisplayName || ''
    this.idToken = stateData.idToken
    this.avatar = stateData.avatar || ''
  },
  methods: {
    async handleCompleteRegister() {
      if (!this.displayName || !this.displayName.trim()) {
        this.error = 'Vui lòng nhập tên thành viên của bạn.'
        return
      }

      if (this.displayName.trim().length < 2 || this.displayName.trim().length > 30) {
        this.error = 'Tên thành viên phải có từ 2 đến 30 ký tự.'
        return
      }

      if (!this.agreeTerms) {
        this.error = 'Bạn cần đồng ý với điều khoản và chính sách quyền riêng tư để tiếp tục.'
        return
      }

      this.loading = true
      this.error = ''

      try {
        const response = await AuthService.completeGoogleRegister({
          idToken: this.idToken,
          displayName: this.displayName.trim()
        })

        // Xóa dữ liệu tạm trong sessionStorage
        sessionStorage.removeItem('pending_google_auth')

        // Lưu thông tin đăng nhập vào localStorage
        localStorage.setItem('token', response.data.token)
        localStorage.setItem('user', JSON.stringify(response.data))

        // Bắn sự kiện cập nhật trạng thái đăng nhập
        window.dispatchEvent(new Event('auth-change'))

        // Điều hướng thẳng về trang chủ với quyền thành viên chính thức
        this.$router.push({ name: 'Home' })
      } catch (err) {
        this.error = err.response?.data?.message || 'Đăng ký tài khoản Google thất bại. Vui lòng thử lại.'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.google-register-page {
  min-height: 80vh;
}

.page-title {
  font-size: 1.45rem;
  font-weight: 700;
  color: #0f172a;
  margin: 1.25rem 0 1.25rem 0;
}

.register-card {
  background: #ffffff;
  border: 1px solid #dbe2ea;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.card-tabs {
  background: #1a507a;
  display: flex;
  padding: 0 16px;
}

.tab-item {
  color: #ffffff;
  font-weight: 600;
  font-size: 0.95rem;
  padding: 12px 18px;
  border-bottom: none;
  background: none;
}

.google-form {
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-row {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
    gap: 6px;
  }
}

.row-label {
  width: 180px;
  flex-shrink: 0;
  text-align: right;
  padding-top: 8px;
}

@media (max-width: 768px) {
  .row-label {
    width: 100%;
    text-align: left;
    padding-top: 0;
  }
}

.row-label label {
  font-weight: 600;
  font-size: 0.95rem;
  color: #1e293b;
}

.required-badge {
  display: block;
  font-size: 0.78rem;
  color: #64748b;
  margin-top: 2px;
}

.row-input {
  flex: 1;
  max-width: 520px;
}

.form-control {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 5px;
  font-size: 0.95rem;
  color: #1e293b;
  background: #ffffff;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-control:focus {
  border-color: #0284c7;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15);
}

.email-locked-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.email-locked-input {
  background: #f8fafc !important;
  color: #64748b !important;
  border-color: #e2e8f0 !important;
  cursor: not-allowed;
  padding-right: 120px;
}

.google-verified-badge {
  position: absolute;
  right: 10px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.78rem;
  font-weight: 600;
  color: #15803d;
  background: #dcfce7;
  padding: 3px 8px;
  border-radius: 12px;
  pointer-events: none;
}

.field-hint {
  font-size: 0.82rem;
  color: #64748b;
  margin: 6px 0 0 0;
  line-height: 1.45;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  color: #334155;
  cursor: pointer;
}

.checkbox-label a {
  color: #0284c7;
  text-decoration: none;
}

.checkbox-label a:hover {
  text-decoration: underline;
}

.error-banner {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-size: 0.9rem;
  padding: 10px 16px;
  border-radius: 5px;
  max-width: 520px;
  margin-left: 200px;
}

@media (max-width: 768px) {
  .error-banner {
    margin-left: 0;
  }
}

.btn-submit-google {
  background: #2563eb;
  color: #ffffff;
  border: none;
  font-size: 0.95rem;
  font-weight: 600;
  padding: 10px 32px;
  border-radius: 5px;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(37, 99, 235, 0.3);
  transition: all 0.2s;
}

.btn-submit-google:hover:not(:disabled) {
  background: #1d4ed8;
  box-shadow: 0 4px 8px rgba(37, 99, 235, 0.4);
  transform: translateY(-1px);
}

.btn-submit-google:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
