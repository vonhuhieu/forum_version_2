import api from '@/shared/services/api.service'

class BookmarkService {
  create(data) {
    return api.post('/bookmarks', data)
  }

  update(id, data) {
    return api.put(`/bookmarks/${id}`, data)
  }

  delete(id) {
    return api.delete(`/bookmarks/${id}`)
  }

  getPage(params) {
    return api.get('/bookmarks', { params })
  }

  getById(id) {
    return api.get(`/bookmarks/${id}`)
  }

  getLabels() {
    return api.get('/bookmarks/labels')
  }

  checkStatus(threadIds, postIds) {
    return api.post('/bookmarks/check-status', { threadIds, postIds })
  }
}

export default new BookmarkService()
