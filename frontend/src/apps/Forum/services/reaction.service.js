import api from '@/shared/services/api.service'

class ReactionService {
  getIcons() {
    return api.get('/reaction-icons')
  }

  addReaction(type, targetId, iconId) {
    let endpoint
    if (type === 'thread') {
      endpoint = `/reactions/threads/${targetId}`
    } else if (type === 'post') {
      endpoint = `/reactions/posts/${targetId}`
    } else if (type === 'message') {
      endpoint = `/reactions/messages/${targetId}`
    }
    return api.post(`${endpoint}?iconId=${iconId}`)
  }

  removeReaction(type, targetId) {
    let endpoint
    if (type === 'thread') {
      endpoint = `/reactions/threads/${targetId}`
    } else if (type === 'post') {
      endpoint = `/reactions/posts/${targetId}`
    } else if (type === 'message') {
      endpoint = `/reactions/messages/${targetId}`
    }
    return api.delete(endpoint)
  }

  getParticipants(type, targetId, params) {
    // type must be 'threads', 'posts', or 'messages'
    return api.get(`/reactions/${type}/${targetId}/participants`, { params })
  }

  getReceivedReactionsSummary() {
    return api.get('/reactions/received/summary')
  }

  getReceivedReactions(params) {
    return api.get('/reactions/received', { params })
  }
}

export default new ReactionService()
