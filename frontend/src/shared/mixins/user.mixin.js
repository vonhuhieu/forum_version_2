export default {
  computed: {
    currentUserProfileLink() {
      try {
        const userStr = localStorage.getItem('user')
        if (userStr) {
          const loggedInUser = JSON.parse(userStr)
          return {
            name: 'UserProfile',
            query: { username: loggedInUser.username }
          }
        }
      } catch (e) {
        console.error('Error in currentUserProfileLink mixin:', e)
      }
      return '#'
    }
  },
  methods: {
    checkIsCurrentUser(user) {
      if (!user) return false
      try {
        const userStr = localStorage.getItem('user')
        if (userStr) {
          const loggedInUser = JSON.parse(userStr)
          if (typeof user === 'string') {
            return loggedInUser.username === user
          }
          return loggedInUser.id === user.id || loggedInUser.username === user.username
        }
      } catch (e) {
        console.error('Error checking isCurrentUser in mixin:', e)
      }
      return false
    }
  }
}
