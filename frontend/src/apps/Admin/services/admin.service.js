import api from '@/shared/services/api.service'

class AdminService {
  // Categories
  getCategories() {
    return api.get('/categories')
  }
  createCategory(data) {
    return api.post('/categories', data)
  }
  updateCategory(id, data) {
    return api.put(`/categories/${id}`, data)
  }
  deleteCategory(id) {
    return api.delete(`/categories/${id}`)
  }

  // Category Groups
  getCategoryGroups() {
    return api.get('/category-groups')
  }
  createCategoryGroup(data) {
    return api.post('/category-groups', data)
  }
  updateCategoryGroup(id, data) {
    return api.put(`/category-groups/${id}`, data)
  }
  deleteCategoryGroup(id) {
    return api.delete(`/category-groups/${id}`)
  }

  // Menus
  getMenus() {
    return api.get('/menus')
  }
  createMenu(data) {
    return api.post('/menus', data)
  }
  updateMenu(id, data) {
    return api.put(`/menus/${id}`, data)
  }
  deleteMenu(id) {
    return api.delete(`/menus/${id}`)
  }

  // Labels
  getLabels() {
    return api.get('/labels')
  }
  createLabel(data) {
    return api.post('/labels', data)
  }
  updateLabel(id, data) {
    return api.put(`/labels/${id}`, data)
  }
  deleteLabel(id) {
    return api.delete(`/labels/${id}`)
  }

  // Reaction Icons
  getReactionIcons() {
    return api.get('/reaction-icons')
  }
  createReactionIcon(data) {
    return api.post('/reaction-icons', data)
  }
  updateReactionIcon(id, data) {
    return api.put(`/reaction-icons/${id}`, data)
  }
  deleteReactionIcon(id) {
    return api.delete(`/reaction-icons/${id}`)
  }

  // Admin Users
  getAdminUsers(params) {
    return api.get('/users/admin', { params })
  }
  createAdminUser(data) {
    return api.post('/users/admin', data)
  }
  updateAdminUser(id, data) {
    return api.put(`/users/admin/${id}`, data)
  }
  deleteAdminUser(id) {
    return api.delete(`/users/admin/${id}`)
  }
}

export default new AdminService()
