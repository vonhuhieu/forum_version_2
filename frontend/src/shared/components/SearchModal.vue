<template>
  <div v-if="show" class="search-modal-overlay" @click.self="close">
    <div class="search-modal-card">
      <!-- Header Area -->
      <div class="search-modal-header">
        <h2>Tìm kiếm diễn đàn</h2>
        <button class="close-btn" @click="close" aria-label="Đóng modal">&times;</button>
      </div>

      <!-- Search Input Section -->
      <div class="search-modal-body">
        <div class="search-form-wrapper">
          <div class="search-input-container" ref="searchContainer">
            <div class="search-input-box">
              <input
                type="text"
                v-model="searchQuery"
                placeholder="Nhập từ khóa cần tìm..."
                @keydown.enter="confirmSelection"
                @keydown.down.prevent="navigateSearchDropdown('down')"
                @keydown.up.prevent="navigateSearchDropdown('up')"
                @keydown.esc="closeSearchDropdown"
                @click="handleSearchFocus"
                @input="handleSearchInput"
                ref="searchInput"
                :class="['modal-search-input', { 'preview-selected': isPreviewSelected }]"
              />
              <button class="btn-modal-search" @click="confirmSelection" :disabled="loading" aria-label="Tìm kiếm">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
              </button>
            </div>

            <!-- Dropdown lịch sử tìm kiếm -->
            <div 
              v-show="showHistoryDropdown && filteredHistory.length > 0" 
              class="search-history-dropdown"
              @mouseleave="resetSearchHover"
            >
              <div
                v-for="(keyword, idx) in filteredHistory"
                :key="keyword"
                :class="['history-item', { active: idx === selectedIndex }]"
                @click="selectSearchKeyword(keyword)"
                @mouseenter="hoverSearchKeyword(keyword, idx)"
              >
                <span class="history-keyword">{{ keyword }}</span>
              </div>
            </div>
          </div>

          <div class="search-filters">
            <div class="sort-by">
              <label for="search-sort-select">Sắp xếp theo:</label>
              <select id="search-sort-select" v-model="sortBy" @change="handleSearch" class="sort-select">
                <option value="relevance">Độ liên quan</option>
                <option value="date">Thời gian (Mới nhất)</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Result Stats -->
        <div class="search-stats" v-if="searched">
          <div v-if="loading" class="searching-status">
            Đang tìm kiếm...
          </div>
          <div v-else class="stats-text">
            Tìm thấy {{ totalElements }} kết quả ({{ searchTimeSeconds }} giây)
          </div>
        </div>

        <!-- Results List -->
        <div class="search-results-list" v-if="!loading && results.length > 0">
          <div v-for="item in results" :key="item.type + '-' + item.id" class="search-result-item" @click="goToResult($event, item)">
            <!-- Title Link -->
            <h3 class="result-title">
              <a href="#" @click.prevent="navigateToResult(item)" class="result-link">
                <span v-html="item.threadTitle"></span>
              </a>
            </h3>

            <!-- Breadcrumbs -->
            <div class="result-breadcrumb-wrapper">
              <Breadcrumb :items="buildBreadcrumbItems(item.categoryId)" />
            </div>

            <!-- Content Snippet -->
            <div class="result-snippet">
              <span class="result-date">{{ formatTime(item.createdAt) }}</span>
              <span class="result-date-separator"> — </span>
              <span v-html="item.snippet"></span>
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div class="search-empty-state" v-else-if="!loading && searched">
          Không tìm thấy bài viết hoặc bình luận nào khớp với từ khóa "{{ lastSearchedQuery }}".
        </div>

        <div class="search-intro-state" v-else-if="!loading">
          Nhập từ khóa và nhấn Enter để bắt đầu tìm kiếm.
        </div>
      </div>

      <!-- Footer / Pagination -->
      <div class="search-modal-footer" v-if="totalPages > 1 && !loading">
        <ForumPagination
          :currentPage="currentPage + 1"
          :totalPages="totalPages"
          @page-changed="handlePageChange"
        />
      </div>
    </div>
    <Loading :visible="loading" />
  </div>
</template>

<script>
import searchService from '@/apps/Forum/services/search.service'
import categoryService from '@/apps/Forum/services/category.service'
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import Loading from '@/shared/components/Loading.vue'
import { formatForumDate } from '@/shared/utils/date'
import searchHistoryMixin from '@/shared/mixins/searchHistory.mixin.js'

export default {
  name: 'SearchModal',
  mixins: [searchHistoryMixin],
  components: {
    Breadcrumb,
    ForumPagination,
    Loading
  },
  props: {
    show: {
      type: Boolean,
      default: false
    },
    initialQuery: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      lastSearchedQuery: '',
      sortBy: 'relevance',
      currentPage: 0,
      pageSize: 10,
      totalPages: 0,
      totalElements: 0,
      results: [],
      categories: [],
      groups: [],
      loading: false,
      searched: false,
      searchTimeSeconds: '0.000'
    }
  },
  watch: {
    show(newVal) {
      if (newVal) {
        this.searchQuery = this.initialQuery
        this.lastSearchedQuery = ''
        this.results = []
        this.searched = false
        this.currentPage = 0
        this.showHistoryDropdown = false
        this.selectedIndex = -1
        this.isPreviewSelected = false
        this.loadCategoriesAndGroups()
        this.loadSearchHistory()
        this.$nextTick(() => {
          if (this.$refs.searchInput) {
            this.$refs.searchInput.focus()
          }
          if (this.searchQuery.trim()) {
            this.handleSearch()
          }
        })

        // Trì hoãn việc đăng ký sự kiện click outside để tránh bắt ngay sự kiện click mở modal đang bubble
        setTimeout(() => {
          document.addEventListener('click', this.handleDocumentClick)
        }, 0)
      } else {
        document.removeEventListener('click', this.handleDocumentClick)
      }
    }
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleDocumentClick)
  },
  methods: {
    close() {
      this.$emit('update:show', false)
      this.$emit('close')
    },
    confirmSelection() {
      this.confirmSearchSelection(this.handleSearch)
    },
    handleDocumentClick(e) {
      if (!this.show) return
      this.handleSearchClickOutside(e)
    },
    async loadCategoriesAndGroups() {
      if (this.categories.length > 0) return
      try {
        const [catRes, groupRes] = await Promise.all([
          categoryService.getAll(),
          categoryService.getGroups()
        ])
        this.categories = catRes.data || []
        this.groups = groupRes.data || []
      } catch (e) {
        console.error('Error loading metadata for search breadcrumbs:', e)
      }
    },
    async handleSearch() {
      if (!this.searchQuery.trim()) return
      this.saveToHistory(this.searchQuery.trim())
      this.showHistoryDropdown = false
      this.currentPage = 0
      await this.executeSearch()
    },
    async executeSearch() {
      this.loading = true
      this.searched = true
      const startTime = performance.now()
      this.lastSearchedQuery = this.searchQuery.trim()

      try {
        const params = {
          q: this.lastSearchedQuery,
          sortBy: this.sortBy,
          page: this.currentPage,
          size: this.pageSize
        }
        const res = await searchService.search(params)
        if (res.data) {
          this.results = res.data.content || []
          this.totalPages = res.data.totalPages || 0
          this.totalElements = res.data.totalElements || 0
        }
      } catch (e) {
        console.error('Search failed:', e)
        this.results = []
        this.totalPages = 0
        this.totalElements = 0
      } finally {
        const endTime = performance.now()
        this.searchTimeSeconds = ((endTime - startTime) / 1000).toFixed(3)
        this.loading = false
      }
    },
    handlePageChange(oneBasedPage) {
      this.currentPage = oneBasedPage - 1
      this.executeSearch()
    },
    buildBreadcrumbItems(categoryId) {
      const items = [{ title: 'Trang chủ', to: '/' }]
      if (!categoryId || this.categories.length === 0) return items

      const cat = this.categories.find(c => c.id === categoryId)
      if (!cat) return items

      // Find top group
      if (cat.categoryGroupId && this.groups.length > 0) {
        const group = this.groups.find(g => g.id === cat.categoryGroupId)
        if (group) {
          items.push({
            title: group.name,
            to: { name: 'Home', hash: `#group-${group.id}` }
          })
        }
      }

      // Find parents recursively
      let parents = []
      let parentId = cat.parentCategoryId
      while (parentId) {
        const parent = this.categories.find(c => c.id === parentId)
        if (parent) {
          parents.unshift(parent)
          parentId = parent.parentCategoryId
        } else {
          break
        }
      }

      parents.forEach(p => {
        items.push({
          title: p.name,
          to: { name: 'CategoryDetail', params: { id: p.id } }
        })
      })

      // Add category itself
      items.push({
        title: cat.name,
        to: { name: 'CategoryDetail', params: { id: cat.id } }
      })

      return items
    },
    highlightKeyword(text, keyword) {
      if (!text) return ''
      if (!keyword || !keyword.trim()) return text

      // Strip HTML tags if any to be safe
      const cleanText = text.replace(/<[^>]*>/g, '')

      // Escape special characters for regex
      const escaped = keyword.trim().replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&')
      const regex = new RegExp(`(${escaped})`, 'gi')
      return cleanText.replace(regex, '<mark class="search-highlight">$1</mark>')
    },
    formatTime(dateStr) {
      return formatForumDate(dateStr)
    },
    navigateToResult(item) {
      this.close()
      if (item.type === 'post') {
        this.$router.push({
          name: 'ThreadDetail',
          params: { id: item.threadId },
          query: { postId: item.id, page: item.pageNumber, highlight: this.lastSearchedQuery },
          hash: `#post-${item.id}`
        })
      } else {
        // thread
        if (item.contentMatched) {
          // Từ khóa khớp ở content -> target vào nội dung bài đăng gốc
          this.$router.push({
            name: 'ThreadDetail',
            params: { id: item.threadId },
            query: { postId: 'main_thread_entry', highlight: this.lastSearchedQuery },
            hash: '#post-main_thread_entry'
          })
        } else {
          // Từ khóa chỉ khớp ở tiêu đề -> scroll lên đầu trang
          this.$router.push({
            name: 'ThreadDetail',
            params: { id: item.threadId },
            query: { highlight: this.lastSearchedQuery }
          })
        }
      }
    },
    goToResult(event, item) {
      if (event.target.closest('a, button, [role="button"]')) {
        return
      }
      this.navigateToResult(item)
    }
  }
}
</script>

<style scoped>
.search-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.search-modal-card {
  background: white;
  width: 90%;
  max-width: 950px;
  height: 85vh;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalFadeIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalFadeIn {
  from { opacity: 0; transform: scale(0.95) translateY(-10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.search-modal-header {
  background: #1a507a;
  color: white;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #123a59;
}

.search-modal-header h2 {
  font-size: 1.4rem;
  font-weight: 600;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 2rem;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  transition: opacity 0.2s;
  outline: none;
}

.close-btn:hover {
  opacity: 0.8;
}

.search-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f8f9fa;
  display: flex;
  flex-direction: column;
}

.search-form-wrapper {
  background: white;
  padding: 15px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-input-box {
  display: flex;
  align-items: center;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
  transition: border-color 0.2s;
}

.search-input-box:focus-within {
  border-color: #1a507a;
}

.modal-search-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 12px 16px;
  font-size: 1rem;
  font-family: inherit;
  transition: font-size 0.15s ease;
}

.modal-search-input.preview-selected {
  font-size: 0.82rem;
}

.btn-modal-search {
  background: #1a507a;
  color: white;
  border: none;
  height: 48px;
  width: 48px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.btn-modal-search:hover {
  background: #236395;
}

/* Lịch sử tìm kiếm & Gợi ý từ khóa */
.search-input-container {
  position: relative;
  width: 100%;
}

.search-history-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 16px -6px rgba(0, 0, 0, 0.05);
  z-index: 999;
  max-height: 280px;
  overflow-y: auto;
  animation: historySlideDown 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  padding: 6px 0;
}

@keyframes historySlideDown {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 18px;
  cursor: pointer;
  transition: background-color 0.15s, color 0.15s;
  font-size: 0.95rem;
  color: #334155;
  text-align: left;
}

.history-item.active {
  background-color: rgba(26, 80, 122, 0.08);
  color: #1a507a;
  font-weight: 500;
}

.history-keyword {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 12px;
}

.search-filters {
  display: flex;
  justify-content: flex-end;
}

.sort-by {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  color: #475569;
}

.sort-select {
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  padding: 6px 12px;
  outline: none;
  background: white;
  cursor: pointer;
}

.sort-select:focus {
  border-color: #1a507a;
}

.search-stats {
  margin-bottom: 15px;
  font-size: 0.9rem;
  color: #64748b;
  font-weight: 500;
}

.search-results-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.search-result-item {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
  transition: box-shadow 0.2s, border-color 0.2s;
}

.search-result-item:hover {
  background-color: #f8f9fa;
  border-color: #cbd5e1;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  cursor: pointer;
}

.result-breadcrumb-wrapper {
  margin-bottom: 8px;
  user-select: text;          /* Cho phép chọn text */
}

/* Override Breadcrumb component's margin-bottom & padding inside search results list */
.result-breadcrumb-wrapper :deep(.breadcrumb) {
  margin-bottom: 0;
  padding: 0;
  background-color: transparent;
}

.result-breadcrumb-wrapper :deep(.breadcrumb-item) {
  font-size: 14px !important;
}

.result-breadcrumb-wrapper :deep(.breadcrumb ol) {
  font-size: 14px;
}

.result-breadcrumb-wrapper :deep(.breadcrumb a),
.result-breadcrumb-wrapper :deep(.breadcrumb a svg) {
  color: #6c757d !important;   /* Đổi màu link thành màu text thường */
  stroke: #6c757d !important;
  text-decoration: none !important;
  pointer-events: none;
}

.result-breadcrumb-wrapper :deep(.breadcrumb a:hover) {
  color: #6c757d !important;
  text-decoration: none !important;
}

.result-title {
  margin: 0 0 6px 0;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.3;
}

.result-link {
  color: #1a507a;
  text-decoration: none;
  transition: color 0.15s;
}

.result-link:hover {
  color: #c0392b;
  text-decoration: underline;
}

.result-page-tag {
  color: #64748b;
  font-size: 14px;
  font-weight: normal;
}

.result-snippet {
  font-size: 14px;
  color: #334155;
  line-height: 1.5;
  margin-bottom: 0;
  word-break: break-word;
}

.result-snippet :deep(p),
.result-snippet :deep(div) {
  display: inline !important;
  margin: 0 !important;
  padding: 0 !important;
}

.result-date {
  color: #64748b;
  font-size: 13.5px;
}

.result-date-separator {
  color: #94a3b8;
}

/* Highlight style */
:deep(.search-highlight) {
  background-color: #fef08a !important;
  color: #000 !important;
  font-weight: 500;
  padding: 0 2px;
  border-radius: 2px;
}

.search-empty-state, .search-intro-state {
  text-align: center;
  padding: 40px;
  color: #64748b;
  font-size: 1.05rem;
  background: white;
  border-radius: 6px;
  border: 1px dashed #cbd5e1;
}

.search-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: white;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1a507a;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.search-modal-footer {
  padding: 15px 20px;
  background: white;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .search-modal-card {
    height: 95vh;
    width: 95%;
  }
  
  .result-title {
    font-size: 18px;
  }
  
  .result-snippet {
    font-size: 14px;
  }
}
</style>
