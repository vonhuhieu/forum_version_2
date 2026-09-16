<template>
  <div class="bookmark-page-wrapper">
    <main class="container" style="padding-top: 2rem; padding-bottom: 2.5rem;">
      <!-- Breadcrumb -->
      <Breadcrumb :items="breadcrumbItems" />

      <div class="account-layout">
        <!-- Sidebar -->
        <AccountSidebar activeMenu="bookmarks" />

        <!-- Main Content -->
        <div class="account-content">
          <div class="bookmark-card card">
            <!-- Header & Filter Bar -->
            <div class="bookmark-header-row">
              <h2 class="bookmark-page-title">Dấu trang</h2>

              <div class="bookmark-filter-controls">
                <!-- Active label badge -->
                <div v-if="appliedLabel" class="active-label-badge">
                  <span>Nhãn: <strong>{{ appliedLabel }}</strong></span>
                  <span class="remove-label-btn" @click="clearLabelFilter">&times;</span>
                </div>

                <!-- Filter trigger button with dropdown -->
                <div class="filter-trigger-wrapper" ref="filterDropdownWrapper">
                  <button class="btn-filter-trigger" @click.stop="filterDropdownOpen = !filterDropdownOpen">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon></svg>
                    <span>Lọc theo nhãn</span>
                    <span class="filter-arrow">▼</span>
                  </button>

                  <!-- Filter Dropdown Popup -->
                  <div class="filter-dropdown-menu" v-if="filterDropdownOpen" @click.stop>
                    <div class="filter-dropdown-title">Lọc theo nhãn</div>
                    <div class="filter-input-wrap">
                      <input
                        type="text"
                        v-model="labelFilterInput"
                        class="filter-search-input"
                        placeholder="Tìm hoặc chọn nhãn..."
                        @keydown.down.prevent="navigateDropdown('down')"
                        @keydown.up.prevent="navigateDropdown('up')"
                        @keydown.enter.prevent="confirmDropdownSelection"
                      />
                    </div>
                    <div class="filter-options-list" v-if="filteredAvailableLabels.length > 0">
                      <div
                        v-for="(label, idx) in filteredAvailableLabels"
                        :key="idx"
                        :class="['filter-option-item', { 'active': idx === selectedDropdownIndex, 'selected': appliedLabel === label }]"
                        @click="selectLabel(label)"
                        @mouseenter="selectedDropdownIndex = idx"
                      >
                        {{ label }}
                      </div>
                    </div>
                    <div class="filter-no-options" v-else>
                      Không có nhãn phù hợp
                    </div>
                    <div class="filter-dropdown-footer" v-if="appliedLabel">
                      <button class="btn-clear-filter" @click="clearLabelFilter">Bỏ lọc</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Loading state -->
            <div v-if="loading" class="state-container loading-state">
              <div class="spinner"></div>
              <span>Đang tải danh sách dấu trang...</span>
            </div>

            <!-- Empty state -->
            <div v-else-if="bookmarks.length === 0" class="state-container empty-state">
              <div class="empty-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path></svg>
              </div>
              <div class="empty-text">
                {{ appliedLabel ? 'Không tìm thấy dấu trang nào với nhãn này.' : 'Bạn chưa lưu dấu trang nào.' }}
              </div>
            </div>

            <!-- Bookmark list -->
            <div v-else class="bookmark-list">
              <div
                v-for="b in bookmarks"
                :key="b.id"
                class="bookmark-list-item cursor-pointer"
                @click="goToBookmark(b)"
              >
                <!-- Avatar -->
                <div class="bookmark-avatar-col" @click.stop>
                  <UserProfilePopup :user="getAuthorObj(b)" v-if="getAuthorObj(b)">
                    <div class="bookmark-avatar" :style="!isAvatarUrl(b.authorAvatar) ? { backgroundColor: b.authorAvatar || '#3498db' } : {}">
                      <img v-if="isAvatarUrl(b.authorAvatar)" :src="formatAvatarUrl(b.authorAvatar)" alt="avatar" />
                      <template v-else>
                        {{ (b.authorDisplayName || b.authorUsername || '?').charAt(0).toUpperCase() }}
                      </template>
                    </div>
                  </UserProfilePopup>
                  <div v-else class="bookmark-avatar" style="background-color: #ccc; color: #fff;">?</div>
                </div>

                <!-- Info -->
                <div class="bookmark-info-col">
                  <!-- Line 1: Title -->
                  <a :href="getBookmarkUrl(b)" @click.prevent="goToBookmark(b)" class="bookmark-title">
                    {{ b.threadTitle }}
                  </a>

                  <!-- Line 2: Note or snippet preview -->
                  <div class="bookmark-snippet" v-if="b.contentPreview">
                    {{ b.contentPreview }}
                  </div>

                  <!-- Line 3: Meta line -->
                  <div class="bookmark-meta">
                    <UserProfilePopup :user="getAuthorObj(b)" v-if="getAuthorObj(b)">
                      <span class="bookmark-author cursor-pointer" @click.stop>
                        <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                        {{ b.authorDisplayName || b.authorUsername }}
                        <VerifiedBadge :user="getAuthorObj(b)" size="12px" />
                      </span>
                    </UserProfilePopup>
                    <span v-else class="bookmark-author">
                      <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                      {{ b.authorDisplayName || b.authorUsername }}
                    </span>

                    <span class="meta-separator">·</span>

                    <span class="bookmark-date">
                      <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                      {{ formatDate(b.createdAt) }}
                    </span>

                    <template v-if="b.labels && b.labels.length > 0">
                      <span class="meta-separator">·</span>
                      <span class="bookmark-labels-tags" @click.stop>
                        <span
                          v-for="(lbl, lidx) in b.labels"
                          :key="lidx"
                          class="bookmark-tag-pill"
                          @click.stop="selectLabel(lbl)"
                        >
                          {{ lbl }}
                        </span>
                      </span>
                    </template>
                  </div>
                </div>

                <!-- Tools Dropdown -->
                <div class="bookmark-tools-col" @click.stop>
                  <div class="tools-btn-wrapper" ref="toolsWrapper">
                    <button class="btn-tools" @click.stop="toggleTools(b.id)" title="Bookmark tools">
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
                      <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-left: 2px;"><polyline points="6 9 12 15 18 9"></polyline></svg>
                    </button>

                    <div class="tools-menu" v-if="activeToolsId === b.id" @click.stop>
                      <button class="tools-menu-item" @click="handleCopyLink(b)">
                        <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path></svg>
                        Sao chép liên kết
                      </button>
                      <button class="tools-menu-item" @click="openEditPopup(b)">
                        <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path></svg>
                        Sửa
                      </button>
                      <button class="tools-menu-item text-danger" @click="handleDelete(b)">
                        <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                        Xóa
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Pagination (left aligned) -->
            <div class="bookmark-pagination-wrapper" v-if="totalPages > 1">
              <ForumPagination
                :current-page="currentPage"
                :total-pages="totalPages"
                @page-change="onPageChange"
              />
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Bookmark Edit Popup -->
    <BookmarkPopup
      :show="showEditPopup"
      mode="edit"
      :bookmark-id="selectedBookmark ? selectedBookmark.id : null"
      :thread-id="selectedBookmark ? selectedBookmark.threadId : null"
      :post-id="selectedBookmark ? selectedBookmark.postId : null"
      :initial-note="selectedBookmark ? selectedBookmark.note : ''"
      :initial-labels="selectedBookmark ? selectedBookmark.labels : []"
      @close="showEditPopup = false"
      @saved="onBookmarkSaved"
      @deleted="onBookmarkDeleted"
    />
  </div>
</template>

<script>
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import AccountSidebar from '@/shared/components/AccountSidebar.vue'
import UserProfilePopup from '@/shared/components/UserProfilePopup.vue'
import VerifiedBadge from '@/shared/components/VerifiedBadge.vue'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import BookmarkPopup from '@/shared/components/BookmarkPopup.vue'
import bookmarkService from '@/apps/Forum/services/bookmark.service'
import { toastSuccess, toastError, alertConfirm } from '@/shared/utils/swal'
import { formatForumDate } from '@/shared/utils/date'
import { isAvatarUrl, formatAvatarUrl } from '@/shared/utils/utils'

export default {
  name: 'BookmarkList',
  components: {
    Breadcrumb,
    AccountSidebar,
    UserProfilePopup,
    VerifiedBadge,
    ForumPagination,
    BookmarkPopup
  },
  data() {
    return {
      bookmarks: [],
      currentPage: 1,
      totalPages: 1,
      totalElements: 0,
      itemsPerPage: 10,
      loading: false,

      // Filter
      allLabels: [],
      appliedLabel: '',
      labelFilterInput: '',
      filterDropdownOpen: false,
      selectedDropdownIndex: -1,

      // Tools dropdown
      activeToolsId: null,

      // Edit popup
      showEditPopup: false,
      selectedBookmark: null,

      breadcrumbItems: [
        { title: 'Trang chủ', to: '/' },
        { title: 'Tài khoản', to: '/account/profile' },
        { title: 'Dấu trang' }
      ]
    }
  },
  computed: {
    filteredAvailableLabels() {
      const q = this.labelFilterInput.trim().toLowerCase()
      if (!q) return this.allLabels
      return this.allLabels.filter(l => l.toLowerCase().includes(q))
    }
  },
  watch: {
    '$route.query'(newQuery) {
      const page = parseInt(newQuery.page) || 1
      const label = newQuery.label || ''
      this.currentPage = page
      this.appliedLabel = label
      this.fetchBookmarks()
    }
  },
  async mounted() {
    const page = parseInt(this.$route.query.page) || 1
    const label = this.$route.query.label || ''
    this.currentPage = page
    this.appliedLabel = label

    await Promise.all([
      this.fetchBookmarks(),
      this.fetchLabels()
    ])

    document.addEventListener('click', this._handleGlobalClick)
  },
  beforeUnmount() {
    document.removeEventListener('click', this._handleGlobalClick)
  },
  methods: {
    isAvatarUrl,
    formatAvatarUrl,
    formatDate(d) {
      if (!d) return ''
      return formatForumDate(d)
    },
    getAuthorObj(b) {
      if (!b || !b.authorId) return null
      return {
        id: b.authorId,
        username: b.authorUsername,
        displayName: b.authorDisplayName,
        avatar: b.authorAvatar,
        verifiedBadge: b.authorIsVerifiedBadge
      }
    },
    getBookmarkUrl(b) {
      if (!b) return '#'
      const targetPost = b.postId || 'main_thread_entry'
      return `/thread/${b.threadId}?postId=${targetPost}`
    },
    goToBookmark(b) {
      if (!b) return
      const targetPost = b.postId || 'main_thread_entry'
      this.$router.push({ name: 'ThreadDetail', params: { id: b.threadId }, query: { postId: targetPost } })
    },
    async fetchLabels() {
      try {
        const res = await bookmarkService.getLabels()
        this.allLabels = res.data.data || res.data || []
      } catch (e) {
        console.error('Error fetching bookmark labels:', e)
      }
    },
    async fetchBookmarks() {
      this.loading = true
      try {
        const params = {
          page: this.currentPage - 1,
          size: this.itemsPerPage
        }
        if (this.appliedLabel) {
          params.label = this.appliedLabel
        }
        const res = await bookmarkService.getPage(params)
        const pageData = (res.data && res.data.content !== undefined) ? res.data : (res.data?.data || res.data || {})
        this.bookmarks = pageData.content || []
        this.totalPages = pageData.totalPages || 1
        this.totalElements = pageData.totalElements || 0
      } catch (e) {
        console.error('Error fetching bookmarks:', e)
        this.bookmarks = []
        this.totalPages = 1
        this.totalElements = 0
      } finally {
        this.loading = false
      }
    },
    onPageChange(page) {
      if (page === this.currentPage) return
      const query = { ...this.$route.query, page }
      this.$router.push({ query })
    },
    selectLabel(label) {
      this.filterDropdownOpen = false
      this.labelFilterInput = ''
      const query = { ...this.$route.query, label, page: 1 }
      this.$router.push({ query })
    },
    clearLabelFilter() {
      this.filterDropdownOpen = false
      this.labelFilterInput = ''
      const query = { ...this.$route.query }
      delete query.label
      query.page = 1
      this.$router.push({ query })
    },
    navigateDropdown(direction) {
      const list = this.filteredAvailableLabels
      if (list.length === 0) return
      if (direction === 'down') {
        this.selectedDropdownIndex = (this.selectedDropdownIndex + 1) % list.length
      } else {
        this.selectedDropdownIndex = this.selectedDropdownIndex <= 0 ? list.length - 1 : this.selectedDropdownIndex - 1
      }
    },
    confirmDropdownSelection() {
      const list = this.filteredAvailableLabels
      if (this.selectedDropdownIndex >= 0 && this.selectedDropdownIndex < list.length) {
        this.selectLabel(list[this.selectedDropdownIndex])
      } else if (this.labelFilterInput.trim()) {
        this.selectLabel(this.labelFilterInput.trim())
      }
    },
    toggleTools(id) {
      this.activeToolsId = this.activeToolsId === id ? null : id
    },
    async handleCopyLink(b) {
      this.activeToolsId = null
      const fullUrl = window.location.origin + this.getBookmarkUrl(b)
      try {
        await navigator.clipboard.writeText(fullUrl)
        toastSuccess('Đã sao chép liên kết vào bộ nhớ tạm')
      } catch (e) {
        console.error('Error copying link:', e)
        toastError('Không thể sao chép liên kết')
      }
    },
    openEditPopup(b) {
      this.activeToolsId = null
      this.selectedBookmark = b
      this.showEditPopup = true
    },
    async handleDelete(b) {
      this.activeToolsId = null
      const confirm = await alertConfirm('Xóa bookmark', 'Bạn có chắc chắn muốn xóa bookmark này?')
      if (!confirm.isConfirmed) return

      try {
        await bookmarkService.delete(b.id)
        toastSuccess('Xóa bookmark thành công')
        await this.fetchBookmarks()
        await this.fetchLabels()
      } catch (e) {
        console.error('Error deleting bookmark:', e)
        toastError('Không thể xóa bookmark')
      }
    },
    onBookmarkSaved() {
      this.fetchBookmarks()
      this.fetchLabels()
    },
    onBookmarkDeleted() {
      this.fetchBookmarks()
      this.fetchLabels()
    },
    _handleGlobalClick(e) {
      if (this.$refs.filterDropdownWrapper && !this.$refs.filterDropdownWrapper.contains(e.target)) {
        this.filterDropdownOpen = false
      }
      this.activeToolsId = null
    }
  }
}
</script>

<style scoped>
.bookmark-card {
  background: #fff;
  border: 1px solid #d8d8d8;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.bookmark-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #e9ecef;
  flex-wrap: wrap;
  gap: 10px;
}

.bookmark-page-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a507a;
}

.bookmark-filter-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.active-label-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background-color: #eef4f9;
  color: #1a507a;
  border: 1px solid #c5d5e2;
  border-radius: 3px;
  padding: 4px 10px;
  font-size: 0.85rem;
}

.remove-label-btn {
  cursor: pointer;
  font-weight: bold;
  font-size: 1rem;
  line-height: 1;
  color: #7f8c8d;
}

.remove-label-btn:hover {
  color: #e74c3c;
}

.filter-trigger-wrapper {
  position: relative;
}

.btn-filter-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  background: #f8f9fa;
  border: 1px solid #ccc;
  border-radius: 3px;
  color: #495057;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-filter-trigger:hover {
  background: #e9ecef;
  border-color: #adb5bd;
}

.filter-arrow {
  font-size: 0.65rem;
  color: #6c757d;
}

.filter-dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  width: 250px;
  background: #fff;
  border: 1px solid #d8d8d8;
  border-radius: 4px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.15);
  z-index: 100;
  padding: 8px 0;
}

.filter-dropdown-title {
  padding: 4px 12px 8px;
  font-size: 0.8rem;
  font-weight: 600;
  color: #6c757d;
  border-bottom: 1px solid #eee;
  text-transform: uppercase;
}

.filter-input-wrap {
  padding: 8px 10px;
}

.filter-search-input {
  width: 100%;
  padding: 6px 10px;
  font-size: 0.85rem;
  border: 1px solid #ced4da;
  border-radius: 3px;
  outline: none;
  box-sizing: border-box;
}

.filter-search-input:focus {
  border-color: #1a507a;
}

.filter-options-list {
  max-height: 180px;
  overflow-y: auto;
}

.filter-option-item {
  padding: 6px 12px;
  font-size: 0.85rem;
  color: #2c3e50;
  cursor: pointer;
  transition: background 0.1s ease;
}

.filter-option-item:hover,
.filter-option-item.active {
  background: #eef4f9;
  color: #1a507a;
}

.filter-option-item.selected {
  font-weight: 600;
  color: #1a507a;
  background: #e3effd;
}

.filter-no-options {
  padding: 10px 12px;
  font-size: 0.82rem;
  color: #999;
  text-align: center;
}

.filter-dropdown-footer {
  padding: 6px 10px 2px;
  border-top: 1px solid #eee;
  text-align: right;
}

.btn-clear-filter {
  background: none;
  border: none;
  color: #e74c3c;
  font-size: 0.8rem;
  cursor: pointer;
}

/* List Items */
.bookmark-list {
  display: flex;
  flex-direction: column;
}

.bookmark-list-item {
  display: flex;
  align-items: flex-start;
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.15s ease;
  position: relative;
}

.bookmark-list-item:hover {
  background: #fbfbfb;
}

.bookmark-avatar-col {
  flex-shrink: 0;
  margin-right: 14px;
}

.bookmark-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  font-size: 1.1rem;
}

.bookmark-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.bookmark-info-col {
  flex: 1;
  min-width: 0;
}

.bookmark-title {
  display: inline-block;
  font-size: 0.95rem;
  font-weight: 600;
  color: #1a507a;
  text-decoration: none;
  margin-bottom: 4px;
  word-break: break-word;
  transition: color 0.15s ease;
}

.bookmark-title:hover {
  color: #d35400;
  text-decoration: underline;
}

.bookmark-snippet {
  font-size: 0.85rem;
  color: #555;
  line-height: 1.45;
  margin-bottom: 6px;
  word-break: break-word;
}

.bookmark-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 0.78rem;
  color: #7f8c8d;
}

.bookmark-author {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #2c3e50;
  font-weight: 500;
}

.bookmark-date {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.meta-separator {
  color: #ccc;
}

.bookmark-labels-tags {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
}

.bookmark-tag-pill {
  display: inline-block;
  background: #edf2f7;
  color: #4a5568;
  padding: 1px 7px;
  border-radius: 10px;
  font-size: 0.72rem;
  cursor: pointer;
  transition: all 0.12s ease;
}

.bookmark-tag-pill:hover {
  background: #cbd5e0;
  color: #1a202c;
}

/* Tools Col */
.bookmark-tools-col {
  flex-shrink: 0;
  margin-left: 12px;
  position: relative;
}

.tools-btn-wrapper {
  position: relative;
}

.btn-tools {
  display: inline-flex;
  align-items: center;
  padding: 4px 7px;
  background: #f8f9fa;
  border: 1px solid #dcdcdc;
  border-radius: 3px;
  color: #666;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-tools:hover {
  background: #eef4f9;
  border-color: #1a507a;
  color: #1a507a;
}

.tools-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  width: 160px;
  background: #fff;
  border: 1px solid #d8d8d8;
  border-radius: 4px;
  box-shadow: 0 3px 12px rgba(0,0,0,0.15);
  z-index: 50;
  padding: 4px 0;
}

.tools-menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 12px;
  background: none;
  border: none;
  font-size: 0.82rem;
  color: #333;
  text-align: left;
  cursor: pointer;
  transition: background 0.1s ease;
}

.tools-menu-item:hover {
  background: #f1f4f8;
}

.tools-menu-item.text-danger {
  color: #e74c3c;
}

.tools-menu-item.text-danger:hover {
  background: #fdf2f2;
}

/* State containers */
.state-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #7f8c8d;
}

.loading-state .spinner {
  width: 28px;
  height: 28px;
  border: 3px solid #e0e0e0;
  border-top-color: #1a507a;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state .empty-icon {
  margin-bottom: 12px;
}

.empty-state .empty-text {
  font-size: 0.95rem;
}

.bookmark-pagination-wrapper {
  padding: 15px 20px;
  border-top: 1px solid #e9ecef;
  display: flex;
  justify-content: flex-start;
}

/* Layout đồng bộ với toàn bộ hệ thống Account */
.account-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  margin-top: 15px;
  width: 100%;
}

.account-content {
  flex: 1;
  min-width: 0;
  width: 100%;
}

.bookmark-list-item {
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.bookmark-list-item:hover {
  background-color: #f8fafc;
}

@media (max-width: 768px) {
  .account-layout {
    flex-direction: column;
  }
}
</style>
