import api from './api.service'

const searchService = {
  getHistory() {
    return api.get('/search/history')
  },
  
  deleteHistory(keyword) {
    return api.delete('/search/history', { params: { keyword } })
  },
  
  clearHistory() {
    return api.delete('/search/history/clear')
  },
  
  syncHistory(keywords) {
    return api.post('/search/history/sync', keywords)
  }
}

export default searchService
