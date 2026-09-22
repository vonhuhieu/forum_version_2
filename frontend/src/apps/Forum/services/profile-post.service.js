import api from '@/shared/services/api.service'

class ProfilePostService {
  getProfilePosts(username, page = 0, size = 10) {
    return api.get(`/profile-posts/user/${username}`, {
      params: { page, size }
    })
  }

  getComments(profilePostId, page = 0, size = 3) {
    return api.get(`/profile-posts/${profilePostId}/comments`, {
      params: { page, size }
    })
  }

  createProfilePost(payload) {
    return api.post('/profile-posts', payload)
  }

  updateProfilePost(id, content) {
    return api.put(`/profile-posts/${id}`, { content })
  }

  deleteProfilePost(id) {
    return api.delete(`/profile-posts/${id}`)
  }

  createComment(profilePostId, content) {
    return api.post(`/profile-posts/${profilePostId}/comments`, { content })
  }

  updateComment(commentId, content) {
    return api.put(`/profile-posts/comments/${commentId}`, { content })
  }

  deleteComment(commentId) {
    return api.delete(`/profile-posts/comments/${commentId}`)
  }
}

export default new ProfilePostService()
