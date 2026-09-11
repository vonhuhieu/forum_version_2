import api from '@/shared/services/api.service'

class UserFollowService {
  getFollowStatus(username) {
    return api.get(`/user-follows/status/${username}`)
  }

  toggleFollow(username, following) {
    return api.post(`/user-follows/${username}?following=${following}`)
  }

  getFollowingUsers(params) {
    return api.get('/user-follows/following', { params })
  }
}

export default new UserFollowService()
