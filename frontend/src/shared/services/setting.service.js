import api from '@/shared/services/api.service'

class SettingService {
  getPublicSettings() {
    return api.get('/settings/public')
  }

  // Admin APIs
  getSettings() {
    return api.get('/settings')
  }

  updateSettings(payload) {
    return api.put('/settings', payload)
  }
}

export default new SettingService()
