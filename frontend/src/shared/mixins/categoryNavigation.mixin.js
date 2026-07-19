export default {
  methods: {
    handleCategoryRowClick(event, cat) {
      if (event.target.closest('a, button, .last-thread-avatar, .sub-categories-dropdown, [role="button"]')) {
        return
      }
      
      const row = event.currentTarget
      let isRightSide = false
      if (row) {
        const rect = row.getBoundingClientRect()
        const clickX = event.clientX - rect.left
        const relativeX = clickX / rect.width
        isRightSide = relativeX >= 0.62
      }

      const lastThreadBlock = event.target.closest('.category-last-thread')
      const thread = this.lastThreadByCat[cat.id]

      const isMobile = typeof window !== 'undefined' && window.innerWidth < 768

      if (!isMobile && (lastThreadBlock || isRightSide) && thread) {
        const route = {
          name: 'ThreadDetail',
          params: { id: thread.id }
        }
        if (thread.lastPostId) {
          route.query = { postId: thread.lastPostId }
        }
        this.$router.push(route)
      } else {
        this.$router.push({ name: 'CategoryDetail', params: { id: cat.id } })
      }
    }
  }
}
