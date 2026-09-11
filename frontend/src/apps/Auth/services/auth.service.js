import api from '@/shared/services/api.service'
import { getCurrentOrigin } from '@/shared/utils/utils'

class AuthService {
  login(credentials) {
    return api.post('/auth/login', credentials)
  }
  register(userData) {
    return api.post('/auth/register', {
      ...userData,
      baseUrl: getCurrentOrigin()
    })
  }
  forgotPassword(email) {
    return api.post('/auth/forgot-password', { email })
  }
  resetPassword(payload) {
    return api.post('/auth/reset-password', payload)
  }
  resendConfirmationEmail(email) {
    return api.post('/auth/resend-confirmation', {
      email,
      baseUrl: getCurrentOrigin()
    })
  }
  verifyConfirmationToken(token) {
    return api.get('/auth/verify-token', { params: { token } })
  }
  confirmEmail(token, password, newPassword = null) {
    return api.post('/auth/confirm-email', { 
      token, 
      password, 
      currentPassword: password, 
      newPassword 
    })
  }
}

export default new AuthService()
