import api from '@/shared/services/api.service'

class SearchService {
  search(params) {
    return api.get('/search', { params })
  }
}

export default new SearchService()
