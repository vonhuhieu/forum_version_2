import api from '@/shared/services/api.service'

class TitleService {
  getAllTitles() {
    return api.get('/admin/titles')
  }

  createTitle(data) {
    return api.post('/admin/titles', data)
  }

  updateTitle(id, data) {
    return api.put(`/admin/titles/${id}`, data)
  }

  deleteTitle(id) {
    return api.delete(`/admin/titles/${id}`)
  }

  assignTitleToUser(userId, titleId) {
    return api.post(`/admin/titles/users/${userId}/assign`, { titleId })
  }
}

export default new TitleService()
