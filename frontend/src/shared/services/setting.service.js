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

  getModerationConfig() {
    return api.get('/settings/moderation')
  }

  updateModerationConfig(payload) {
    return api.put('/settings/moderation', payload)
  }

  testModeration(content) {
    return api.post('/settings/moderation/test', { content })
  }
}

export default new SettingService()
