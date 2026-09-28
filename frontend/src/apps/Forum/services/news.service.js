import api from '@/shared/services/api.service'

class NewsService {
  getNewsThreads(params) {
    return api.get('/news/threads', { params })
  }

  getNewsStats() {
    return api.get('/news/stats')
  }

  triggerManualCrawl() {
    return api.post('/news/crawl-now')
  }
}

export default new NewsService()
