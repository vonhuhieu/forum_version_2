import api from '@/shared/services/api.service'

class UserService {
  search(params) {
    return api.get('/users/search', { params })
  }

  searchPublic(params) {
    return api.get('/users/search/public', { params })
  }

  getByName(name) {
    return api.get('/users/by-name', { params: { name } })
  }

  getPublicByName(name) {
    return api.get('/users/by-name/public', { params: { name } })
  }
}

export default new UserService()
