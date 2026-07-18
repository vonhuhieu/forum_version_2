export default {
  data() {
    return {
      searchQuery: '',
      searchHistory: [],
      showHistoryDropdown: false,
      selectedIndex: -1,
      originalQuery: '',
      filterQuery: '',
      isPreviewSelected: false,
      isDeleteFocused: false
    }
  },
  computed: {
    filteredHistory() {
      if (typeof this.filterQuery !== 'string') {
        return this.searchHistory.slice(0, 10)
      }
      const q = this.filterQuery.trim().toLowerCase()
      if (!q) {
        return this.searchHistory.slice(0, 10)
      }
      return this.searchHistory
        .filter(item => item.toLowerCase().startsWith(q))
        .slice(0, 10)
    }
  },
  mounted() {
    this.loadSearchHistory()
  },
  methods: {
    loadSearchHistory() {
      try {
        const historyStr = localStorage.getItem('forum_search_history')
        this.searchHistory = historyStr ? JSON.parse(historyStr) : []
      } catch (e) {
        console.error('Error loading search history:', e)
        this.searchHistory = []
      }
    },
    saveToHistory(query) {
      if (!query || !query.trim()) return
      const cleaned = query.trim()
      let history = [...this.searchHistory]
      history = history.filter(item => item.toLowerCase() !== cleaned.toLowerCase())
      history.unshift(cleaned)
      this.searchHistory = history
      localStorage.setItem('forum_search_history', JSON.stringify(this.searchHistory))
    },
    removeFromHistory(keyword) {
      if (!keyword) return
      const cleaned = keyword.trim().toLowerCase()
      this.searchHistory = this.searchHistory.filter(
        item => item.trim().toLowerCase() !== cleaned
      )
      try {
        localStorage.setItem('forum_search_history', JSON.stringify(this.searchHistory))
      } catch (e) {
        console.error('Error saving search history after removal:', e)
      }
      this.selectedIndex = -1
      this.isPreviewSelected = false
      this.isDeleteFocused = false
    },
    navigateSearchDropdown(direction) {
      if (!this.showHistoryDropdown || this.filteredHistory.length === 0) return
      this.isDeleteFocused = false
      const len = this.filteredHistory.length
      if (direction === 'down') {
        if (this.selectedIndex === -1) {
          this.originalQuery = this.searchQuery
        }
        this.selectedIndex = (this.selectedIndex + 1) % (len + 1)
        if (this.selectedIndex === len) {
          this.selectedIndex = -1
        }
      } else if (direction === 'up') {
        if (this.selectedIndex === -1) {
          this.originalQuery = this.searchQuery
          this.selectedIndex = len - 1
        } else {
          this.selectedIndex--
        }
      }

      if (this.selectedIndex !== -1) {
        this.searchQuery = this.filteredHistory[this.selectedIndex]
        this.isPreviewSelected = true
      } else {
        this.searchQuery = this.originalQuery
        this.isPreviewSelected = false
      }
      this.scrollSearchHistoryDropdown()
    },
    handleArrowRight(e) {
      if (this.showHistoryDropdown && this.selectedIndex !== -1 && !this.isDeleteFocused) {
        e.preventDefault()
        this.isDeleteFocused = true
        this.searchQuery = this.originalQuery
      }
    },
    handleArrowLeft(e) {
      if (this.showHistoryDropdown && this.selectedIndex !== -1 && this.isDeleteFocused) {
        e.preventDefault()
        this.isDeleteFocused = false
        this.searchQuery = this.filteredHistory[this.selectedIndex]
      }
    },
    hoverSearchKeyword(keyword, idx) {
      if (this.selectedIndex === -1) {
        this.originalQuery = this.searchQuery
      }
      this.selectedIndex = idx
      this.searchQuery = keyword
      this.isPreviewSelected = true
      this.isDeleteFocused = false
    },
    resetSearchHover() {
      if (!this.showHistoryDropdown) return
      this.selectedIndex = -1
      this.searchQuery = this.originalQuery
      this.isPreviewSelected = false
      this.isDeleteFocused = false
    },
    selectSearchKeyword(keyword) {
      this.searchQuery = keyword
      this.filterQuery = keyword
      this.showHistoryDropdown = false
      this.selectedIndex = -1
      this.isPreviewSelected = false
      this.isDeleteFocused = false
      this.$nextTick(() => {
        const input = this.$refs.searchInput
        if (input) {
          input.focus()
          const len = input.value.length
          input.setSelectionRange(len, len)
        }
      })
    },
    confirmSearchSelection(onConfirmSearch) {
      if (this.showHistoryDropdown && this.selectedIndex !== -1) {
        if (this.isDeleteFocused) {
          const targetKeyword = this.filteredHistory[this.selectedIndex]
          this.removeFromHistory(targetKeyword)
          this.isDeleteFocused = false
          return
        }
        this.searchQuery = this.filteredHistory[this.selectedIndex]
        this.filterQuery = this.searchQuery
        this.showHistoryDropdown = false
        this.isPreviewSelected = false
        this.selectedIndex = -1
        this.$nextTick(() => {
          const input = this.$refs.searchInput
          if (input) {
            input.focus()
            const len = input.value.length
            input.setSelectionRange(len, len)
          }
        })
        return
      }
      this.isPreviewSelected = false
      if (typeof onConfirmSearch === 'function') {
        onConfirmSearch()
      }
    },
    closeSearchDropdown() {
      this.showHistoryDropdown = false
      this.selectedIndex = -1
      this.isPreviewSelected = false
      this.isDeleteFocused = false
    },
    handleSearchFocus() {
      this.showHistoryDropdown = true
      this.selectedIndex = -1
      this.originalQuery = this.searchQuery
      this.filterQuery = this.searchQuery
      this.isPreviewSelected = false
      this.isDeleteFocused = false
    },
    handleSearchInput() {
      this.showHistoryDropdown = true
      this.selectedIndex = -1
      this.originalQuery = this.searchQuery
      this.filterQuery = this.searchQuery
      this.isPreviewSelected = false
      this.isDeleteFocused = false
    },
    handleSearchClickOutside(e) {
      if (this.$refs.searchContainer && !this.$refs.searchContainer.contains(e.target)) {
        this.showHistoryDropdown = false
        this.selectedIndex = -1
        this.isPreviewSelected = false
        this.isDeleteFocused = false
      }
    },
    scrollSearchHistoryDropdown() {
      this.$nextTick(() => {
        const dropdown = this.$refs.searchContainer?.querySelector('.search-history-dropdown')
        if (!dropdown) return
        const activeItem = dropdown.querySelector('.history-item.active')
        if (!activeItem) return

        const dropdownTop = dropdown.scrollTop
        const dropdownBottom = dropdownTop + dropdown.clientHeight
        const itemTop = activeItem.offsetTop
        const itemBottom = itemTop + activeItem.clientHeight

        if (itemTop < dropdownTop) {
          dropdown.scrollTop = itemTop
        } else if (itemBottom > dropdownBottom) {
          dropdown.scrollTop = itemBottom - dropdown.clientHeight
        }
      })
    }
  }
}
