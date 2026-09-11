import api from '@/shared/services/api.service'

class ThreadService {
  getAll(params) {
    return api.get('/threads', { params })
  }

  getById(id) {
    return api.get(`/threads/${id}`)
  }

  getLatest() {
    return api.get('/threads/latest')
  }

  create(payload) {
    return api.post('/threads', payload)
  }

  update(id, payload) {
    return api.put(`/threads/${id}`, payload)
  }

  delete(id) {
    return api.delete(`/threads/${id}`)
  }

  deleteAll() {
    return api.delete('/threads/delete-all')
  }

  pin(id) {
    return api.patch(`/threads/${id}/pin`)
  }

  getFollowStatus(id) {
    return api.get(`/threads/${id}/follow-status`)
  }

  follow(id, following) {
    return api.post(`/threads/${id}/follow?following=${following}`)
  }
}

export default new ThreadService()
