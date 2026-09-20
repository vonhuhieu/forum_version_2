<template>
  <div class="google-callback-page">
    <div class="callback-card">
      <div class="spinner-circle"></div>
      <h2 class="callback-title">Đang xác thực tài khoản Google...</h2>
      <p class="callback-desc">Vui lòng chờ trong giây lát, hệ thống đang đồng bộ dữ liệu của bạn.</p>
    </div>
  </div>
</template>

<script>
import AuthService from '@/apps/Auth/services/auth.service'
import { getGoogleRedirectUri } from '@/shared/utils/googleAuth'
import { toastSuccess, toastError, toastInfo } from '@/shared/utils/swal'

export default {
  name: 'GoogleCallback',
  data() {
    return {
      isProcessing: false
    }
  },
  async mounted() {
    await this.handleCallback()
  },
  methods: {
    async handleCallback() {
      if (this.isProcessing) return
      this.isProcessing = true

      const query = this.$route.query
      const source = query.state || sessionStorage.getItem('google_oauth_source') || 'login'
      const targetFallbackRoute = source === 'register' ? 'Register' : 'Login'
      sessionStorage.removeItem('google_oauth_source')

      // 1. Người dùng bấm [Hủy] trên màn hình Consent của Google
      if (query.error === 'access_denied') {
        toastInfo('Bạn đã hủy liên kết tài khoản Google.')
        this.$router.replace({ name: targetFallbackRoute })
        return
      }

      // 2. Các lỗi OAuth khác từ Google
      if (query.error) {
        toastError(query.error_description || 'Đăng nhập Google không thành công.')
        this.$router.replace({ name: targetFallbackRoute })
        return
      }

      const code = query.code
      if (!code) {
        toastError('Không tìm thấy mã xác thực Google hợp lệ.')
        this.$router.replace({ name: targetFallbackRoute })
        return
      }

      // 3. Đổi code lấy token và xác thực tài khoản qua Backend
      try {
        const redirectUri = getGoogleRedirectUri()
        const res = await AuthService.exchangeGoogleCode({
          code,
          redirectUri
        })

        if (res.data.isExistingUser) {
          // Tài khoản đã có trong hệ thống -> Đăng nhập ngay
          localStorage.setItem('token', res.data.token)
          localStorage.setItem('user', JSON.stringify(res.data))
          window.dispatchEvent(new Event('auth-change'))
          toastSuccess('Đăng nhập thành công!')

          const roles = res.data.roles || []
          if (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_SUPER_ADMIN')) {
            this.$router.replace({ name: 'AdminMenu' })
          } else {
            this.$router.replace({ name: 'Home' })
          }
        } else {
          // Tài khoản mới -> Lưu thông tin tạm và điều hướng hoàn tất đăng ký
          sessionStorage.setItem('pending_google_auth', JSON.stringify({
            email: res.data.email,
            suggestedDisplayName: res.data.suggestedDisplayName,
            avatar: res.data.avatar,
            idToken: res.data.idToken
          }))

          this.$router.replace({ name: 'GoogleRegisterComplete' })
        }
      } catch (err) {
        toastError(err.response?.data?.message || 'Xác thực tài khoản Google thất bại.')
        this.$router.replace({ name: targetFallbackRoute })
      }
    }
  }
}
</script>

<style scoped>
.google-callback-page {
  min-height: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3rem 1.5rem;
}

.callback-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  padding: 2.5rem 2rem;
  max-width: 440px;
  width: 100%;
  text-align: center;
  border: 1px solid #e2e8f0;
}

.spinner-circle {
  width: 48px;
  height: 48px;
  margin: 0 auto 1.5rem;
  border: 4px solid #e2e8f0;
  border-top: 4px solid #1a507a;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.callback-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 0.6rem;
}

.callback-desc {
  font-size: 0.92rem;
  color: #64748b;
  line-height: 1.5;
  margin: 0;
}
</style>
