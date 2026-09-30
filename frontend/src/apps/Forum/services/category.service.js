import api from '@/shared/services/api.service'

class CategoryService {
  getAll() {
    return api.get('/categories')
  }

  getById(id) {
    return api.get(`/categories/${id}`)
  }

  getGroups() {
    return api.get('/category-groups')
  }

  getLabels(categoryId) {
    return api.get(`/categories/${categoryId}/labels`)
  }
}

export default new CategoryService()
