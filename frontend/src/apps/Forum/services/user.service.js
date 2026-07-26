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

  getNewestMembers(limit = 12) {
    return api.get('/users/members/newest', { params: { limit } })
  }

  getTopPosters(limit = 5) {
    return api.get('/users/members/top-posters', { params: { limit } })
  }

  getTopInteractions(limit = 5) {
    return api.get('/users/members/top-interactions', { params: { limit } })
  }

  getTopTrophyPoints(limit = 5) {
    return api.get('/users/members/top-trophy-points', { params: { limit } })
  }

  getMembersPaged(key, page = 0, size = 20) {
    return api.get('/users/members/paged', { params: { key, page, size } })
  }
}

export default new UserService()
