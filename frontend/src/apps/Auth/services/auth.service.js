import api from '@/shared/services/api.service'

class AuthService {
  login(credentials) {
    return api.post('/auth/login', credentials)
  }
  register(userData) {
    return api.post('/auth/register', userData)
  }
  forgotPassword(username, email) {
    return api.post('/auth/forgot-password', { username, email })
  }
  resetPassword(payload) {
    return api.post('/auth/reset-password', payload)
  }
}

export default new AuthService()
