import api from '@/shared/services/api.service'

class NotificationService {
  getAll() {
    return api.get('/notifications')
  }

  getUnreadCount() {
    return api.get('/notifications/unread-count')
  }

  markAllRead() {
    return api.put('/notifications/read-all')
  }

  markAsRead(id) {
    return api.put(`/notifications/${id}/read`)
  }

  clearAll() {
    return api.delete('/notifications/clear-all')
  }
}

export default new NotificationService()
