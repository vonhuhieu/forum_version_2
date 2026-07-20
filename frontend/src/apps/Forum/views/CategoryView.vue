<template>
  <div>
    <main class="container" style="padding-top: 2rem;">
      <div v-if="loading" style="text-align: center; padding: 3rem;">Đang tải...</div>
      
      <div v-else>
        <!-- Block 1: Breadcrumb -->
        <Breadcrumb :items="breadcrumbItems" />

        <!-- Block 2: Tiêu đề chuyên mục hiện tại (Card header chuyển lên vị trí trên cùng dưới breadcrumb) -->
        <div class="card" style="margin-bottom: 2rem;">
          <div class="card-header" style="display: flex; justify-content: space-between; align-items: center; border-bottom: none;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <span>{{ category ? category.name : 'Chuyên mục' }}</span>
              <span v-if="category" style="font-size: 0.8rem; font-weight: normal; opacity: 0.8;">{{ category.description }}</span>
            </div>
            <button v-if="isLoggedIn && !isNonOfficial" class="btn-post-thread" @click="goToCreateThread">Đăng bài...</button>
          </div>
        </div>

        <!-- Block 3: Chuyên mục con (Tách riêng thành 1 Card độc lập và nằm dưới Tiêu đề chuyên mục) -->
        <div v-if="category && category.subCategories && category.subCategories.length > 0" class="card" style="margin-bottom: 2rem; border: 1px solid #dee2e6; border-radius: 4px; overflow: hidden;">
          <div class="sub-categories-block" style="border-bottom: none;">
             <div class="sub-categories-list">
              <div v-for="sub in category.subCategories" :key="sub.id" class="category-row home-category-row min-height-100-on-pc" @click="handleCategoryRowClick($event, sub)">
                <div class="category-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#f39c12" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
                </div>
                <div class="category-info">
                  <div class="cat-name-row">
                    <router-link :to="{ name: 'CategoryDetail', params: { id: sub.id } }" class="category-name">
                      {{ sub.name }}
                    </router-link>
                  </div>
                  <div v-if="sub.description" class="cat-desc">{{ sub.description }}</div>
                </div>
                <div class="category-stats">
                  <div class="stat-item">
                    <span class="stat-label">Chủ đề</span>
                    <span class="stat-value">{{ formatNumber(sub.threadCount || 0) }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">Bài viết</span>
                    <span class="stat-value">{{ formatNumber(sub.postCount || 0) }}</span>
                  </div>
                </div>
                <div class="category-last-thread">
                  <div v-if="lastThreadByCat[sub.id]" class="last-thread-box">
                    <user-profile-popup :user="lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author" v-if="lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author">
                      <div class="last-thread-avatar" :style="!isAvatarUrl((lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.avatar) ? { backgroundColor: (lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.avatar || '#ccc', color: '#fff' } : {}">
                        <img v-if="isAvatarUrl((lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.avatar)" :src="(lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.avatar" />
                        <template v-else>
                          {{ ((lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.displayName || (lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.username || 'A').charAt(0).toUpperCase() }}
                        </template>
                      </div>
                    </user-profile-popup>
                    <div v-else class="last-thread-avatar" style="background-color: #ccc; color: #fff;">A</div>
                    <div class="last-thread-info">
                      <router-link :to="lastThreadByCat[sub.id].lastPostId ? { name: 'ThreadDetail', params: { id: lastThreadByCat[sub.id].id }, query: { postId: lastThreadByCat[sub.id].lastPostId } } : { name: 'ThreadDetail', params: { id: lastThreadByCat[sub.id].id } }" class="last-thread-title" :title="lastThreadByCat[sub.id].title">
                        <span v-if="lastThreadByCat[sub.id].label" class="label-tag-mini" :style="{ backgroundColor: lastThreadByCat[sub.id].label.colorCode, color: lastThreadByCat[sub.id].label.textColor, borderColor: lastThreadByCat[sub.id].label.borderColor || 'transparent' }">
                          {{ lastThreadByCat[sub.id].label.name }}
                        </span>
                        <span class="title-txt">{{ lastThreadByCat[sub.id].title }}</span>
                        <span v-if="lastThreadByCat[sub.id].pinned" title="Đã ghim" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 4px;">
                          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="icon-pin" style="display: block; pointer-events: none;"><line x1="12" y1="17" x2="12" y2="22"></line><path d="M5 17h14v-1.76a2 2 0 0 0-.44-1.24l-2.78-3.5A2 2 0 0 1 15 9.26V5a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4.26a2 2 0 0 1-.78 1.24l-2.78 3.5a2 2 0 0 0-.44 1.24z"></path></svg>
                        </span>
                        <span v-if="lastThreadByCat[sub.id].locked" title="Đã khóa" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 4px;">
                          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: block; pointer-events: none;"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                        </span>
                      </router-link>
                      <div class="last-thread-meta">
                        <span>{{ formatDate(lastThreadByCat[sub.id].lastPostAt || lastThreadByCat[sub.id].createdAt) }}</span>
                        <span class="dot">•</span>
                        <span class="author">{{ (lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.displayName || (lastThreadByCat[sub.id].lastPostAuthor || lastThreadByCat[sub.id].author)?.username || 'Ẩn danh' }}</span>
                      </div>
                    </div>
                  </div>
                  <div v-else class="no-thread" style="color: #999; font-size: 0.85rem;">Chưa có bài viết</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Block 4: Danh sách bài viết -->
        <div class="card" style="margin-bottom: 2rem;">
          <!-- Chỉ hiển thị pagination-wrapper trên khi tổng số trang > 1 -->
          <div v-if="totalPages > 1" class="pagination-wrapper" style="padding: 1rem; border-bottom: 1px solid #eee;">
            <ForumPagination 
              :current-page="currentPage" 
              :total-pages="totalPages" 
              @page-changed="currentPage = $event"
            />
          </div>

          <!-- Thanh bộ lọc (Filter bar) -->
          <div class="thread-filter-bar">
            <div class="active-filters">
              <div 
                v-if="appliedLabel" 
                class="active-filter-badge" 
                :style="{ backgroundColor: appliedLabel.colorCode, color: appliedLabel.textColor, borderColor: appliedLabel.borderColor || 'transparent' }"
              >
                Nhãn: {{ appliedLabel.name }}
                <span class="remove-filter-btn" @click="removeLabelFilter">&times;</span>
              </div>
              <div 
                v-if="appliedUser" 
                class="active-filter-badge"
                style="background-color: #eef4f9; color: #1a507a; border-color: #c5d5e2;"
              >
                Bắt đầu bởi: {{ appliedUser.displayName || appliedUser.username }}
                <span class="remove-filter-btn" @click="removeUserFilter">&times;</span>
              </div>
              <div 
                v-if="appliedThreadType" 
                class="active-filter-badge"
                style="background-color: #f5f5f5; color: #444; border-color: #ddd;"
              >
                Thread type: {{ appliedThreadType === 'discussion' ? 'Thảo luận' : 'Bình chọn' }}
                <span class="remove-filter-btn" @click="removeThreadTypeFilter">&times;</span>
              </div>
              <div 
                v-if="!isDefaultSort" 
                class="active-filter-badge"
                style="background-color: #f5f5f5; color: #444; border-color: #ddd;"
              >
                Sắp xếp theo: {{ getSortByTextApplied() }}<img :src="appliedSortOrder === 'asc' ? iconGoUp : iconGoDown" class="sort-direction-icon" alt="sort direction" />
                <span class="remove-filter-btn" @click="removeSortFilter">&times;</span>
              </div>
            </div>

            <div class="filter-trigger-wrapper" style="position: relative;">
              <div class="filter-trigger" @click="filterDropdownOpen = !filterDropdownOpen">
                <span>Lọc</span>
                <span class="arrow-down">▼</span>
              </div>

              <!-- Filter Dropdown overlay -->
              <div class="filter-dropdown" v-if="filterDropdownOpen" @click.stop>
                <div class="filter-dropdown-header">Chỉ hiện:</div>
                <div class="filter-dropdown-body">
                  <div class="filter-field-group">
                    <label class="filter-field-label">Nhãn:</label>
                    <div class="custom-select filter-label-select">
                      <div 
                        class="select-selected-container"
                        :style="selectedLabel ? { backgroundColor: selectedLabel.colorCode, color: selectedLabel.textColor, borderColor: selectedLabel.borderColor || 'transparent' } : {}"
                      >
                        <input 
                          type="text" 
                          v-model="labelSearchKeyword" 
                          @focus="onLabelInputFocus"
                          placeholder="(Mọi)"
                          class="select-search-input"
                          :style="selectedLabel ? { color: selectedLabel.textColor, fontWeight: '600' } : {}"
                        />
                        <span 
                          v-if="selectedLabel" 
                          class="select-clear-btn" 
                          @click.stop="clearLabelSelection"
                          :style="selectedLabel ? { color: selectedLabel.textColor } : {}"
                        >
                          &times;
                        </span>
                        <span class="select-arrow-icon" @click.stop="toggleLabelDropdown" :style="selectedLabel ? { color: selectedLabel.textColor } : {}">▼</span>
                      </div>
                      
                      <div class="select-items" v-if="labelDropdownOpen">
                        <div 
                          class="select-item" 
                          @click="selectLabel(null)"
                        >
                          (Mọi)
                        </div>
                        <div 
                          v-for="label in filteredLabels" 
                          :key="label.id" 
                          class="select-item"
                          :style="{ backgroundColor: label.colorCode, color: label.textColor, borderColor: label.borderColor || 'transparent' }"
                          @click="selectLabel(label)"
                        >
                          {{ label.name }}
                        </div>
                        <div v-if="filteredLabels.length === 0" class="select-item no-result" style="color: #999; text-align: center; padding: 8px;">
                          Không tìm thấy nhãn nào
                        </div>
                      </div>
                    </div>
                  </div>

                  <div class="filter-field-group filter-user-search" style="position: relative;">
                    <label class="filter-field-label">Bắt đầu bởi:</label>
                    <div class="select-selected-container">
                      <input 
                        type="text" 
                        :value="userSearchKeyword" 
                        @focus="onUserSearchFocus"
                        @input="handleUserSearchInput"
                        placeholder="Tên người dùng..."
                        class="select-search-input"
                      />
                      <span 
                        v-if="selectedUser" 
                        class="select-clear-btn" 
                        @click.stop="clearUserSelection"
                      >
                        &times;
                      </span>
                    </div>

                    <!-- Dropdown autocomplete search results -->
                    <div v-if="userDropdownOpen && userSearchResults.length > 0" class="autocomplete-dropdown-filter">
                      <div 
                        v-for="user in userSearchResults" 
                        :key="user.id" 
                        class="autocomplete-item-filter" 
                        @click="selectUser(user)"
                      >
                        <div class="user-avatar-mini" :style="!isAvatarUrl(user.avatar) ? { backgroundColor: user.avatar || '#ccc', color: '#fff' } : {}">
                          <img v-if="isAvatarUrl(user.avatar)" :src="user.avatar" />
                          <template v-else>
                            {{ (user.displayName || user.username || 'A').charAt(0).toUpperCase() }}
                          </template>
                        </div>
                        <span class="user-name-text">
                          <strong>{{ user.displayName || user.username }}</strong>
                        </span>
                      </div>
                    </div>
                  </div>

                  <div class="filter-field-group filter-thread-type" style="position: relative;">
                    <label class="filter-field-label">Thread type:</label>
                    <div class="custom-select">
                      <div 
                        class="select-selected-container"
                        @click.stop="toggleTypeDropdown"
                        style="cursor: pointer; justify-content: space-between;"
                      >
                        <div style="font-size: 0.85rem; color: #333; font-weight: 500;">
                          {{ getSelectedTypeText() }}
                        </div>
                        <span class="select-arrow-icon" style="position: static; font-size: 0.6rem; color: #8c8c8c; cursor: pointer;">▼</span>
                      </div>
                      
                      <div class="select-items" v-if="typeDropdownOpen">
                        <div 
                          class="select-item" 
                          @click="selectThreadType(null)"
                        >
                          (Mọi)
                        </div>
                        <div 
                          class="select-item" 
                          @click="selectThreadType('discussion')"
                        >
                          Thảo luận
                        </div>
                        <div 
                          class="select-item" 
                          @click="selectThreadType('poll')"
                        >
                          Bình chọn
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <div class="filter-field-group filter-sort-group">
                    <label class="filter-field-label">Sắp xếp theo:</label>
                    <div class="sort-two-col">
                      <!-- Dropdown trái: tiêu chí sắp xếp -->
                      <div class="custom-select filter-sort-by">
                        <div 
                          class="select-selected-container"
                          @click.stop="toggleSortDropdown"
                          style="cursor: pointer; justify-content: space-between;"
                        >
                          <div style="font-size: 0.85rem; color: #333; font-weight: 500;">
                            {{ getSortByText() }}
                          </div>
                          <span class="select-arrow-icon" style="position: static; font-size: 0.6rem; color: #8c8c8c; cursor: pointer;">&#9660;</span>
                        </div>
                        <div class="select-items" v-if="sortDropdownOpen">
                          <div class="select-item" @click="selectSortBy('lastPostAt')">Last message</div>
                          <div class="select-item" @click="selectSortBy('createdAt')">First message</div>
                          <div class="select-item" @click="selectSortBy('replyCount')">Replies</div>
                          <div class="select-item" @click="selectSortBy('viewCount')">Views</div>
                          <div class="select-item" @click="selectSortBy('reactionCount')">First message reaction score</div>
                        </div>
                      </div>

                      <!-- Dropdown phải: chiều sắp xếp -->
                      <div class="custom-select filter-sort-order">
                        <div 
                          class="select-selected-container"
                          @click.stop="toggleSortOrderDropdown"
                          style="cursor: pointer; justify-content: space-between;"
                        >
                          <div style="font-size: 0.85rem; color: #333; font-weight: 500;">
                            {{ getSortOrderText() }}
                          </div>
                          <span class="select-arrow-icon" style="position: static; font-size: 0.6rem; color: #8c8c8c; cursor: pointer;">&#9660;</span>
                        </div>
                        <div class="select-items" v-if="sortOrderDropdownOpen">
                          <div class="select-item" @click="selectSortOrder('desc')">Trên xuống</div>
                          <div class="select-item" @click="selectSortOrder('asc')">Dưới lên</div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div class="filter-dropdown-footer">
                    <button class="btn-submit-filter" @click="submitFilter">Lọc</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div class="thread-list">
            <div v-for="thread in paginatedThreads" :key="thread.id" class="thread-row thread-row-center min-height-100-on-pc" @click="goToThread($event, thread)">
              <user-profile-popup :user="thread.author" v-if="thread.author">
                <div class="thread-avatar" :style="!isAvatarUrl(thread.author?.avatar) ? { backgroundColor: thread.author?.avatar || '#ccc', color: '#fff' } : {}">
                  <img v-if="isAvatarUrl(thread.author?.avatar)" :src="thread.author.avatar" />
                  <template v-else>
                    {{ thread.author ? (thread.author.displayName || thread.author.username).charAt(0).toUpperCase() : 'A' }}
                  </template>
                </div>
              </user-profile-popup>
              <div v-else class="thread-avatar" style="background-color: #ccc; color: #fff;">A</div>
              <div class="thread-main">
                <div class="thread-title">
                  <span v-if="thread.label" class="label-tag" :style="{ backgroundColor: thread.label.colorCode, color: thread.label.textColor, borderColor: thread.label.borderColor || 'transparent' }">
                    {{ thread.label.name }}
                  </span>
                  <router-link :to="{ name: 'ThreadDetail', params: { id: thread.id }, query: getThreadDetailQuery() }">{{ thread.title }}</router-link>
                  <span v-if="thread.pinned" title="Đã ghim" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 6px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="icon-pin" style="display: block; pointer-events: none;"><line x1="12" y1="17" x2="12" y2="22"></line><path d="M5 17h14v-1.76a2 2 0 0 0-.44-1.24l-2.78-3.5A2 2 0 0 1 15 9.26V5a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4.26a2 2 0 0 1-.78 1.24l-2.78 3.5a2 2 0 0 0-.44 1.24z"></path></svg>
                  </span>
                  <span v-if="thread.locked" title="Đã khóa" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 6px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-lock" style="display: block; pointer-events: none;"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                  </span>
                </div>
                <div class="thread-meta">
                  <span class="author-name white-space-nowrap">{{ thread.author ? (thread.author.displayName || thread.author.username) : 'Ẩn danh' }}</span>
                  <span class="dot-divider">•</span>
                  <router-link :to="{ name: 'ThreadDetail', params: { id: thread.id }, query: getThreadDetailQuery() }" class="meta-link">{{ formatDate(thread.createdAt) }}</router-link>
                  
                  <span class="quick-pages" v-if="getThreadPages(thread.replyCount).length > 0">
                    <router-link 
                      v-for="p in getThreadPages(thread.replyCount)" 
                      :key="p" 
                      :to="{ name: 'ThreadDetail', params: { id: thread.id }, query: getThreadDetailQuery({ page: p }) }"
                      class="page-badge"
                    >
                      {{ p }}
                    </router-link>
                  </span>
                </div>
                <div class="thread-author-mobile">
                  {{ thread.author ? (thread.author.displayName || thread.author.username) : 'Ẩn danh' }}
                </div>
                <div class="thread-stats-mobile">
                  Trả lời: {{ thread.replyCount || 0 }} <span class="dot-divider">•</span> {{ formatDate(thread.lastPostAt || thread.createdAt) }}
                </div>
              </div>
              <div class="thread-stats">
                <div class="stat-block">
                  <span class="stat-label">Trả lời:</span>
                  <span class="stat-value">{{ thread.replyCount || 0 }}</span>
                </div>
                <div class="stat-block">
                  <span class="stat-label">Xem:</span>
                  <span class="stat-value">{{ thread.viewCount || 0 }}</span>
                </div>
              </div>
              <div class="thread-last-post">
                <div class="last-post-info">
                  <router-link 
                    :to="{ name: 'ThreadDetail', params: { id: thread.id }, query: getThreadDetailQuery(thread.lastPostId ? { postId: thread.lastPostId } : {}) }" 
                    class="last-post-time-link">
                    {{ formatDate(thread.lastPostAt || thread.createdAt) }}
                  </router-link>
                  <span class="last-post-author">{{ (thread.lastPostAuthor || thread.author)?.displayName || (thread.lastPostAuthor || thread.author)?.username || 'Ẩn danh' }}</span>
                </div>
                <user-profile-popup :user="thread.lastPostAuthor || thread.author" v-if="thread.lastPostAuthor || thread.author">
                  <div class="last-post-avatar" :style="!isAvatarUrl((thread.lastPostAuthor || thread.author)?.avatar) ? { backgroundColor: (thread.lastPostAuthor || thread.author)?.avatar || '#ccc', color: '#fff' } : {}">
                    <img v-if="isAvatarUrl((thread.lastPostAuthor || thread.author)?.avatar)" :src="(thread.lastPostAuthor || thread.author)?.avatar" />
                    <template v-else>
                      {{ ((thread.lastPostAuthor || thread.author)?.displayName || (thread.lastPostAuthor || thread.author)?.username || 'A').charAt(0).toUpperCase() }}
                    </template>
                  </div>
                </user-profile-popup>
                <div v-else class="last-post-avatar" style="background-color: #ccc; color: #fff;">A</div>
              </div>
            </div>

            <div v-if="!threads || threads.length === 0"
              style="padding: 2rem; text-align: center; color: #999;">
              Chưa có bài viết nào trong mục này.
            </div>
          </div>
          
          <!-- Chỉ hiển thị pagination-wrapper dưới khi tổng số trang > 1 -->
          <div v-if="totalPages > 1" class="pagination-wrapper" style="padding: 1rem; border-top: 1px solid #eee;">
            <ForumPagination 
              :current-page="currentPage" 
              :total-pages="totalPages" 
              @page-changed="currentPage = $event"
            />
          </div>
        </div>

        <Breadcrumb :items="breadcrumbItems" />
      </div>
    </main>
  </div>
</template>

<script>
import threadService from '@/apps/Forum/services/thread.service'
import categoryService from '@/apps/Forum/services/category.service'
import labelService from '@/apps/Forum/services/label.service'
import userService from '@/apps/Forum/services/user.service'
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import UserProfilePopup from '@/shared/components/UserProfilePopup.vue'
import { formatForumDate } from '@/shared/utils/date'
import { isNonOfficialUser, isAvatarUrl, getImeValue } from '@/shared/utils/utils'
import categoryNavigationMixin from '@/shared/mixins/categoryNavigation.mixin.js'

export default {
  name: 'CategoryView',
  mixins: [categoryNavigationMixin],
  components: {
    Breadcrumb,
    ForumPagination,
    UserProfilePopup
  },
  data() {
    return {
      iconGoUp: '/icon-go-up.svg',
      iconGoDown: '/icon-go-down.svg',
      category: null,
      categoryGroup: null,
      allCategories: [],
      lastThreadByCat: {},
      threads: [],
      loading: true,
      currentPage: 1,
      itemsPerPage: 10,
      isLoggedIn: false,
      totalPagesCount: 1,
      totalElements: 0,
      isChangingCategory: false,
      allLabels: [],
      selectedLabel: null,
      appliedLabel: null,
      labelSearchKeyword: '',
      filterDropdownOpen: false,
      labelDropdownOpen: false,
      selectedUser: null,
      appliedUser: null,
      userSearchKeyword: '',
      userDropdownOpen: false,
      userSearchResults: [],
      loadingUsers: false,
      userSearchTimeout: null,
      userSearchRequestId: 0,
      selectedThreadType: null,
      appliedThreadType: null,
      typeDropdownOpen: false,
      selectedSortBy: 'lastPostAt',
      selectedSortOrder: 'desc',
      appliedSortBy: 'lastPostAt',
      appliedSortOrder: 'desc',
      sortDropdownOpen: false,
      sortOrderDropdownOpen: false
    }
  },
  watch: {
    '$route.params.id': {
      async handler(newId, oldId) {
        if (newId && newId !== oldId) {
          this.isChangingCategory = true
          this.currentPage = 1
          await this.fetchData()
          this.isChangingCategory = false
        }
      }
    },
    currentPage: {
      async handler(newPage, oldPage) {
        if (this.isChangingCategory) return
        if (newPage !== oldPage) {
          this.loading = true
          try {
            await this.fetchThreadsPaged()
          } finally {
            this.loading = false
          }
        }
      }
    },
    '$route.query.labelId': {
      async handler(newVal, oldVal) {
        if (this.isChangingCategory) return
        this.currentPage = 1
        this.loading = true
        try {
          this.syncLabelFromQuery()
          await this.fetchThreadsPaged()
        } finally {
          this.loading = false
        }
      }
    },
    '$route.query.displayName': {
      async handler(newVal, oldVal) {
        if (this.isChangingCategory) return
        this.currentPage = 1
        this.loading = true
        try {
          await this.syncUserFromQuery()
          await this.fetchThreadsPaged()
        } finally {
          this.loading = false
        }
      }
    },
    '$route.query.threadType': {
      async handler(newVal, oldVal) {
        if (this.isChangingCategory) return
        this.currentPage = 1
        this.loading = true
        try {
          this.syncThreadTypeFromQuery()
          await this.fetchThreadsPaged()
        } finally {
          this.loading = false
        }
      }
    },
    '$route.query.sortBy': {
      async handler(newVal, oldVal) {
        if (this.isChangingCategory) return
        this.currentPage = 1
        this.loading = true
        try {
          this.syncSortFromQuery()
          await this.fetchThreadsPaged()
        } finally {
          this.loading = false
        }
      }
    },
    '$route.query.sortOrder': {
      async handler(newVal, oldVal) {
        if (this.isChangingCategory) return
        this.currentPage = 1
        this.loading = true
        try {
          this.syncSortFromQuery()
          await this.fetchThreadsPaged()
        } finally {
          this.loading = false
        }
      }
    },
    userSearchKeyword(newVal) {
      // Watcher phản ứng tức thì: đóng dropdown ngay khi keyword trống
      if (!newVal || !newVal.trim()) {
        clearTimeout(this.userSearchTimeout)
        this.userSearchRequestId++ // Vô hiệu hóa mọi response đang bay
        this.userSearchResults = []
        this.userDropdownOpen = false
      }
    }
  },
  computed: {
    isNonOfficial() {
      return isNonOfficialUser()
    },
    breadcrumbItems() {
      const items = [{ title: 'Trang chủ', to: { name: 'Home' } }]
      
      if (this.categoryGroup) {
        items.push({ 
          title: this.categoryGroup.name, 
          to: { name: 'Home', hash: `#group-${this.categoryGroup.id}` } 
        })
      }

      if (this.category && this.allCategories && this.allCategories.length > 0) {
         let parents = [];
         let currentParentId = this.category.parentCategoryId;
         while (currentParentId) {
             const parent = this.allCategories.find(c => c.id === currentParentId);
             if (parent) {
                 parents.unshift(parent);
                 currentParentId = parent.parentCategoryId;
             } else {
                 break;
             }
         }
         parents.forEach(p => {
             items.push({
                 title: p.name,
                 to: { name: 'CategoryDetail', params: { id: p.id } }
             })
         });
      }
      
      const currentQuery = {}
      if (this.$route.query.labelId) currentQuery.labelId = this.$route.query.labelId
      if (this.$route.query.displayName) currentQuery.displayName = this.$route.query.displayName
      if (this.$route.query.threadType) currentQuery.threadType = this.$route.query.threadType
      if (this.$route.query.sortBy) currentQuery.sortBy = this.$route.query.sortBy
      if (this.$route.query.sortOrder) currentQuery.sortOrder = this.$route.query.sortOrder

      const hasQuery = Object.keys(currentQuery).length > 0
      items.push({
        title: this.category ? this.category.name : 'Chuyên mục',
        ...(hasQuery && this.category
          ? { to: { name: 'CategoryDetail', params: { id: this.category.id }, query: currentQuery } }
          : {})
      })
      return items
    },
    totalPages() {
      return this.totalPagesCount
    },
    paginatedThreads() {
      return this.threads
    },
    filteredLabels() {
      if (!this.labelSearchKeyword) {
        return this.allLabels
      }
      if (this.selectedLabel && this.labelSearchKeyword === this.selectedLabel.name) {
        return this.allLabels
      }
      return this.allLabels.filter(label => 
        label.name.toLowerCase().includes(this.labelSearchKeyword.toLowerCase())
      )
    },
    isDefaultSort() {
      return this.appliedSortBy === 'lastPostAt' && this.appliedSortOrder === 'desc'
    }
  },
  async mounted() {
    this.checkAuth()
    await this.fetchData()
    window.addEventListener('user-avatar-updated', this.handleAvatarUpdated)
    document.addEventListener('click', this.handleDocumentClick)
  },
  beforeUnmount() {
    window.removeEventListener('user-avatar-updated', this.handleAvatarUpdated)
    document.removeEventListener('click', this.handleDocumentClick)
  },
  methods: {
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    handleAvatarUpdated(event) {
      const { username, avatar } = event.detail
      this.threads = this.threads.map(t => {
        const updated = { ...t }
        if (t.author && t.author.username === username) {
          updated.author = { ...t.author, avatar }
        }
        if (t.lastPostAuthor && t.lastPostAuthor.username === username) {
          updated.lastPostAuthor = { ...t.lastPostAuthor, avatar }
        }
        return updated
      })
      const updatedLastThreadByCat = { ...this.lastThreadByCat }
      Object.keys(updatedLastThreadByCat).forEach(subId => {
        const thread = updatedLastThreadByCat[subId]
        if (thread) {
          const updatedThread = { ...thread }
          let changed = false
          if (thread.author && thread.author.username === username) {
            updatedThread.author = { ...thread.author, avatar }
            changed = true
          }
          if (thread.lastPostAuthor && thread.lastPostAuthor.username === username) {
            updatedThread.lastPostAuthor = { ...thread.lastPostAuthor, avatar }
            changed = true
          }
          if (changed) {
            updatedLastThreadByCat[subId] = updatedThread
          }
        }
      })
      this.lastThreadByCat = updatedLastThreadByCat
    },
    checkAuth() {
      const user = localStorage.getItem('user')
      this.isLoggedIn = !!user
    },
    goToCreateThread() {
      if (this.category) {
        this.$router.push({ name: 'CreateThread', query: { catId: this.category.id } })
      }
    },
    async fetchData() {
      this.loading = true
      const categoryId = this.$route.params.id
      try {
        // Fetch tất cả chuyên mục, nhóm và nhãn
        const [catRes, groupRes, labelRes] = await Promise.all([
          categoryService.getAll(),
          categoryService.getGroups(),
          labelService.getAll()
        ])
        
        const categories = catRes.data
        this.allCategories = categories
        this.category = categories.find(c => c.id == categoryId)
        this.allLabels = labelRes.data || []

        if (this.category && this.category.categoryGroupId) {
          this.categoryGroup = groupRes.data.find(g => g.id === this.category.categoryGroupId)
        }

        // Sync label from URL query parameters
        this.syncLabelFromQuery()

        // Sync user from URL query parameters
        await this.syncUserFromQuery()

        // Sync thread type from URL query parameters
        this.syncThreadTypeFromQuery()

        // Sync sort from URL query parameters
        this.syncSortFromQuery()

        // Fetch danh sách bài viết trang hiện tại
        await this.fetchThreadsPaged()

        // Fetch last thread for subcategories
        if (this.category && this.category.subCategories) {
          for (const sub of this.category.subCategories) {
            this.fetchLastThread(sub.id)
          }
        }
      } catch (error) {
        console.error('Lỗi khi tải dữ liệu chuyên mục:', error)
      } finally {
        this.loading = false
      }
    },
    async fetchThreadsPaged() {
      const categoryId = this.$route.params.id
      const page = this.currentPage - 1
      const size = this.itemsPerPage
      const labelId = this.$route.query.labelId || null
      const displayName = this.$route.query.displayName || null
      const threadType = this.$route.query.threadType || null
      const sortBy = this.$route.query.sortBy || null
      const sortOrder = this.$route.query.sortOrder || null
      
      const threadRes = await threadService.getAll({ categoryId, labelId, displayName, threadType, sortBy, sortOrder, page, size })
      if (threadRes.data && threadRes.data.content) {
        this.threads = threadRes.data.content
        this.totalPagesCount = threadRes.data.totalPages || 1
        this.totalElements = threadRes.data.totalElements || 0
      } else {
        this.threads = []
        this.totalPagesCount = 1
        this.totalElements = 0
      }
    },
    async fetchLastThread(catId) {
      try {
        const res = await threadService.getAll({ categoryId: catId, limit: 1 })
        if (res.data && res.data.length > 0) {
          this.lastThreadByCat = { ...this.lastThreadByCat, [catId]: res.data[0] }
        }
      } catch (e) {
        console.error(e)
      }
    },

    formatDate(dateStr) {
      return formatForumDate(dateStr)
    },
    formatNumber(num) {
      if (!num) return 0
      if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
      if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
      return num
    },
    getThreadPages(replyCount) {
      const itemsPerPage = 10; 
      const totalItems = 1 + (replyCount || 0);
      const totalPages = Math.ceil(totalItems / itemsPerPage);
      
      if (totalPages <= 1) return [];
      if (totalPages === 2) return [2];
      if (totalPages === 3) return [2, 3];
      
      return [totalPages - 2, totalPages - 1, totalPages];
    },
    syncLabelFromQuery() {
      const labelId = this.$route.query.labelId
      if (labelId && this.allLabels && this.allLabels.length > 0) {
        const found = this.allLabels.find(l => String(l.id) === String(labelId))
        if (found) {
          this.appliedLabel = found
          this.selectedLabel = found
          this.labelSearchKeyword = found.name
          return
        }
      }
      this.appliedLabel = null
      this.selectedLabel = null
      this.labelSearchKeyword = ''
    },
    onLabelInputFocus() {
      this.labelDropdownOpen = true
    },
    selectLabel(label) {
      this.selectedLabel = label
      this.labelSearchKeyword = label ? label.name : ''
      this.labelDropdownOpen = false
    },
    clearLabelSelection() {
      this.selectedLabel = null
      this.labelSearchKeyword = ''
      this.labelDropdownOpen = false
    },
    toggleLabelDropdown() {
      this.labelDropdownOpen = !this.labelDropdownOpen
    },
    async syncUserFromQuery() {
      const displayName = this.$route.query.displayName
      if (displayName) {
        try {
          const res = await userService.getPublicByName(displayName)
          if (res.data) {
            this.appliedUser = res.data
            this.selectedUser = res.data
            this.userSearchKeyword = res.data.displayName || res.data.username
            return
          }
        } catch (e) {
          console.error('Lỗi khi sync user từ query:', e)
        }
      }
      this.appliedUser = null
      this.selectedUser = null
      this.userSearchKeyword = ''
    },
    onUserSearchFocus() {
      if (this.userSearchKeyword && this.userSearchKeyword.trim()) {
        this.userDropdownOpen = true
        this.handleUserSearchInput()
      }
    },
    handleUserSearchInput(e) {
      this.userSearchKeyword = getImeValue(e)
      // Luôn hủy timer cũ TRƯỚC, tránh race condition khi xóa nhanh
      clearTimeout(this.userSearchTimeout)
      if (!this.userSearchKeyword || !this.userSearchKeyword.trim()) {
        this.userSearchResults = []
        this.userDropdownOpen = false
        this.selectedUser = null
        return
      }
      this.userDropdownOpen = true
      this.userSearchTimeout = setTimeout(() => {
        this.fetchUsers()
      }, 300)
    },
    async fetchUsers() {
      // Guard: không bao giờ gọi API khi keyword rỗng
      if (!this.userSearchKeyword || !this.userSearchKeyword.trim()) {
        this.userSearchResults = []
        this.userDropdownOpen = false
        return
      }
      // Gán request ID để bỏ qua response cũ (stale response)
      const requestId = ++this.userSearchRequestId
      this.loadingUsers = true
      try {
        const response = await userService.searchPublic({
          keyword: this.userSearchKeyword,
          page: 0,
          size: 10
        })
        // Chỉ cập nhật UI nếu đây vẫn là request mới nhất
        if (requestId === this.userSearchRequestId && response.data) {
          this.userSearchResults = response.data.content || []
        }
      } catch (error) {
        console.error('Lỗi khi tìm kiếm người dùng:', error)
      } finally {
        if (requestId === this.userSearchRequestId) {
          this.loadingUsers = false
        }
      }
    },
    selectUser(user) {
      this.selectedUser = user
      this.userSearchKeyword = user.displayName || user.username
      this.userDropdownOpen = false
    },
    clearUserSelection() {
      this.selectedUser = null
      this.userSearchKeyword = ''
      this.userDropdownOpen = false
      this.userSearchResults = []
    },
    submitFilter() {
      const query = { ...this.$route.query }
      if (this.selectedLabel) {
        query.labelId = this.selectedLabel.id
      } else {
        delete query.labelId
      }
      
      if (this.selectedUser) {
        query.displayName = this.selectedUser.displayName || this.selectedUser.username
      } else {
        delete query.displayName
      }

      if (this.selectedThreadType) {
        query.threadType = this.selectedThreadType
      } else {
        delete query.threadType
      }

      // Xử lý sort: chỉ đưa vào query khi khác mặc định
      const isDefault = this.selectedSortBy === 'lastPostAt' && this.selectedSortOrder === 'desc'
      if (!isDefault) {
        query.sortBy = this.selectedSortBy
        query.sortOrder = this.selectedSortOrder
      } else {
        delete query.sortBy
        delete query.sortOrder
      }
      
      this.filterDropdownOpen = false
      this.$router.push({ name: 'CategoryDetail', params: { id: this.category.id }, query })
    },
    removeLabelFilter() {
      const query = { ...this.$route.query }
      delete query.labelId
      this.selectedLabel = null
      this.labelSearchKeyword = ''
      this.$router.push({ name: 'CategoryDetail', params: { id: this.category.id }, query })
    },
    removeUserFilter() {
      const query = { ...this.$route.query }
      delete query.displayName
      this.selectedUser = null
      this.appliedUser = null
      this.userSearchKeyword = ''
      this.userSearchResults = []
      this.$router.push({ name: 'CategoryDetail', params: { id: this.category.id }, query })
    },
    handleDocumentClick(e) {
      const trigger = this.$el.querySelector('.filter-trigger')
      const dropdown = this.$el.querySelector('.filter-dropdown')
      if (trigger && !trigger.contains(e.target) && dropdown && !dropdown.contains(e.target)) {
        this.filterDropdownOpen = false
      }
      
      const labelSelect = this.$el.querySelector('.filter-label-select')
      if (labelSelect && !labelSelect.contains(e.target)) {
        this.labelDropdownOpen = false
        this.labelSearchKeyword = this.selectedLabel ? this.selectedLabel.name : ''
      }

      const userSearchGroup = this.$el.querySelector('.filter-user-search')
      if (userSearchGroup && !userSearchGroup.contains(e.target)) {
        this.userDropdownOpen = false
        this.userSearchKeyword = this.selectedUser ? (this.selectedUser.displayName || this.selectedUser.username) : ''
      }

      const typeSelect = this.$el.querySelector('.filter-thread-type')
      if (typeSelect && !typeSelect.contains(e.target)) {
        this.typeDropdownOpen = false
      }

      const sortBySelect = this.$el.querySelector('.filter-sort-by')
      if (sortBySelect && !sortBySelect.contains(e.target)) {
        this.sortDropdownOpen = false
      }

      const sortOrderSelect = this.$el.querySelector('.filter-sort-order')
      if (sortOrderSelect && !sortOrderSelect.contains(e.target)) {
        this.sortOrderDropdownOpen = false
      }
    },
    toggleTypeDropdown() {
      this.typeDropdownOpen = !this.typeDropdownOpen
    },
    selectThreadType(type) {
      this.selectedThreadType = type
      this.typeDropdownOpen = false
    },
    getSelectedTypeText() {
      if (this.selectedThreadType === 'discussion') {
        return 'Thảo luận'
      }
      if (this.selectedThreadType === 'poll') {
        return 'Bình chọn'
      }
      return '(Mọi)'
    },
    syncThreadTypeFromQuery() {
      const type = this.$route.query.threadType || null
      this.appliedThreadType = type
      this.selectedThreadType = type
    },
    removeThreadTypeFilter() {
      const query = { ...this.$route.query }
      delete query.threadType
      this.selectedThreadType = null
      this.appliedThreadType = null
      this.$router.push({ name: 'CategoryDetail', params: { id: this.category.id }, query })
    },
    getThreadDetailQuery(extraParams = {}) {
      const query = { ...extraParams }
      if (this.$route.query.labelId) {
        query.labelId = this.$route.query.labelId
      }
      if (this.$route.query.displayName) {
        query.displayName = this.$route.query.displayName
      }
      if (this.$route.query.threadType) {
        query.threadType = this.$route.query.threadType
      }
      if (this.$route.query.sortBy) {
        query.sortBy = this.$route.query.sortBy
      }
      if (this.$route.query.sortOrder) {
        query.sortOrder = this.$route.query.sortOrder
      }
      return query
    },
    goToThread(event, thread) {
      if (event.target.closest('a, button, .thread-avatar, .last-post-avatar, [role="button"]')) {
        return
      }
      this.$router.push({
        name: 'ThreadDetail',
        params: { id: thread.id },
        query: this.getThreadDetailQuery()
      })
    },
    toggleSortDropdown() {
      this.sortDropdownOpen = !this.sortDropdownOpen
    },
    toggleSortOrderDropdown() {
      this.sortOrderDropdownOpen = !this.sortOrderDropdownOpen
    },
    selectSortBy(val) {
      this.selectedSortBy = val
      this.sortDropdownOpen = false
    },
    selectSortOrder(val) {
      this.selectedSortOrder = val
      this.sortOrderDropdownOpen = false
    },
    getSortByText() {
      const map = { lastPostAt: 'Last message', createdAt: 'First message', replyCount: 'Replies', viewCount: 'Views', reactionCount: 'First message reaction score' }
      return map[this.selectedSortBy] || 'Last message'
    },
    getSortByTextApplied() {
      const map = { lastPostAt: 'Last message', createdAt: 'First message', replyCount: 'Replies', viewCount: 'Views', reactionCount: 'First message reaction score' }
      return map[this.appliedSortBy] || 'Last message'
    },
    getSortOrderText() {
      return this.selectedSortOrder === 'asc' ? 'Dưới lên' : 'Trên xuống'
    },
    syncSortFromQuery() {
      const sortBy = this.$route.query.sortBy || 'lastPostAt'
      const sortOrder = this.$route.query.sortOrder || 'desc'
      this.selectedSortBy = sortBy
      this.appliedSortBy = sortBy
      this.selectedSortOrder = sortOrder
      this.appliedSortOrder = sortOrder
    },
    removeSortFilter() {
      const query = { ...this.$route.query }
      delete query.sortBy
      delete query.sortOrder
      this.selectedSortBy = 'lastPostAt'
      this.selectedSortOrder = 'desc'
      this.appliedSortBy = 'lastPostAt'
      this.appliedSortOrder = 'desc'
      this.$router.push({ name: 'CategoryDetail', params: { id: this.category.id }, query })
    }
  }
}
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-start;
  align-items: center;
}

.sub-categories-block {
  background-color: #f8f9fa;
  border-bottom: 1px solid #eee;
}

.sub-categories-list {
  display: flex;
  flex-direction: column;
}

.category-row {
  display: flex;
  padding: 12px 15px;
  border-bottom: 1px solid #f0f2f5;
  align-items: center;
  background: white;
  transition: background-color 0.2s;
}

.category-row:last-child {
  border-bottom: none;
}

.category-row:hover {
  background: #f9fbfc;
}

.category-icon {
  width: 40px;
  display: flex;
  align-items: center;
}

.category-info {
  flex: 1;
  min-width: 0;
}

.category-name {
  font-weight: 600;
  color: #1a507a;
  text-decoration: none;
  font-size: 1.05rem;
}

.category-name:hover {
  text-decoration: underline;
}

.cat-desc {
  font-size: 0.8rem;
  color: #888;
  margin-top: 3px;
}

.category-stats {
  display: flex;
  width: 150px;
  text-align: center;
  gap: 15px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 0.7rem;
  text-transform: uppercase;
  color: #999;
}

.stat-value {
  font-size: 0.95rem;
  font-weight: 500;
  margin-top: 2px;
}

.category-last-thread {
  width: 320px;
  padding-left: 15px;
  border-left: 1px solid #eee;
}

.last-thread-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.last-thread-avatar {
  width: 36px;
  height: 36px;
  background: #5c6bc0;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 1rem;
  flex-shrink: 0;
}

.last-thread-info {
  flex: 1;
  min-width: 0;
}

.last-thread-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.95rem;
  font-weight: 500;
  color: #1a507a;
  text-decoration: none;
  margin-bottom: 2px;
  min-width: 0;
}

.last-thread-title .title-txt {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 0 1 auto;
}

.last-thread-title:hover .title-txt {
  text-decoration: underline;
}

.label-tag-mini {
  padding: 1px 5px;
  font-size: 0.7rem;
  border-radius: 3px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  white-space: nowrap;
  line-height: 1.2;
  flex-shrink: 0;
}

.last-thread-meta {
  font-size: 0.8rem;
  color: #888;
}

.label-tag {
  padding: 2px 6px;
  font-size: 0.75rem;
  border-radius: 3px;
  font-weight: 600;
  display: inline-block;
  border: 1px solid transparent;
  margin-right: 8px;
  white-space: nowrap;
  vertical-align: middle;
  line-height: 1;
}

.thread-title {
  margin-bottom: 4px;
  display: block;
  min-width: 0;
  max-width: 100%;
}

.thread-title span {
  display: inline-block;
  vertical-align: middle;
}

.thread-title a {
  text-decoration: none;
  color: #1a507a;
  font-weight: 500;
  font-size: 1.05rem;
  line-height: 1.5;
  display: inline;
  white-space: normal;
  word-break: break-word;
}

.thread-meta {
  font-size: 0.85rem;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  gap: 5px;
}

.dot-divider {
  font-size: 0.85rem;
  color: #bbb;
}

.meta-link {
  color: #8c8c8c;
  text-decoration: none;
  cursor: pointer;
}

.meta-link:hover {
  text-decoration: underline;
}

.quick-pages {
  display: inline-flex;
  gap: 4px;
  margin-left: 8px;
}

.page-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  border: 1px solid #e0e0e0;
  background-color: #f8f9fa;
  color: #666;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 3px;
  text-decoration: none;
  font-weight: normal;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-badge:hover {
  background-color: #1a507a;
  border-color: #1a507a;
  color: white;
  font-weight: bold;
}

.thread-last-post {
  width: 180px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 10px;
  text-align: right;
}

.last-post-info {
  display: flex;
  flex-direction: column;
}

.last-post-time-link {
  font-size: 0.85rem;
  color: #2980b9;
  text-decoration: none;
  cursor: pointer;
}

.last-post-time-link:hover {
  text-decoration: underline;
}

.last-post-author {
  font-size: 0.8rem;
  color: #444;
}

.last-post-avatar {
  width: 32px;
  height: 32px;
  background-color: #5c6bc0;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.85rem;
  flex-shrink: 0;
  border: 1px solid #dee2e6;
  margin-top: 3px;
}

@media (max-width: 767px) {
  .sub-categories-block .category-row {
    display: grid !important;
    grid-template-columns: 40px 1fr !important;
    grid-template-areas: 
      "icon info"
      "icon stats"
      "icon last-thread" !important;
    align-items: center !important;
    padding: 12px 15px !important;
    gap: 4px 0px !important;
  }

  .sub-categories-block .category-icon {
    grid-area: icon !important;
    margin-top: 0 !important;
  }

  .sub-categories-block .category-info {
    grid-area: info !important;
    display: flex !important;
    flex-direction: column !important;
    align-items: flex-start !important;
  }

  .sub-categories-block .category-stats {
    grid-area: stats !important;
    display: flex !important;
    flex-direction: row !important;
    width: auto !important;
    gap: 15px !important;
    text-align: left !important;
    font-size: 0.8rem !important;
    color: #666 !important;
    padding-left: 0 !important;
    border-left: none !important;
  }

  .sub-categories-block .category-stats .stat-item {
    display: flex !important;
    flex-direction: row !important;
    align-items: center !important;
    gap: 4px !important;
  }

  .sub-categories-block .category-stats .stat-item .stat-label {
    font-size: 0.8rem !important;
    color: #666 !important;
    text-transform: none !important;
  }

  .sub-categories-block .category-stats .stat-item .stat-label::after {
    content: ":" !important;
  }

  .sub-categories-block .category-stats .stat-item .stat-value {
    font-size: 0.8rem !important;
    font-weight: 500 !important;
    margin-top: 0 !important;
  }

  .sub-categories-block .category-last-thread {
    grid-area: last-thread !important;
    width: 100% !important;
    min-width: 0 !important;
    padding-left: 0 !important;
    border-left: none !important;
  }

  .sub-categories-block .last-thread-box {
    display: flex !important;
    align-items: flex-start !important;
    gap: 0 !important;
    width: 100% !important;
    min-width: 0 !important;
  }

  .sub-categories-block .category-last-thread .last-thread-avatar {
    display: none !important;
  }

  .sub-categories-block .last-thread-info {
    width: 100% !important;
    min-width: 0 !important;
    display: flex !important;
    flex-direction: column !important;
    align-items: flex-start !important;
    gap: 2px !important;
  }

  .sub-categories-block .last-thread-title {
    display: inline-flex !important;
    align-items: center !important;
    max-width: 100% !important;
    min-width: 0 !important;
    gap: 6px !important;
    font-size: 0.85rem !important;
    margin-bottom: 0 !important;
  }

  .sub-categories-block .last-thread-title .title-txt {
    white-space: nowrap !important;
    overflow: hidden !important;
    text-overflow: ellipsis !important;
    flex: 0 1 auto !important;
    display: block !important;
    min-width: 0 !important;
    max-width: 100% !important;
  }

  .sub-categories-block .last-thread-meta {
    display: flex !important;
    align-items: center !important;
    gap: 4px !important;
    font-size: 0.8rem !important;
    color: #888 !important;
  }

  .sub-categories-block .last-thread-meta .dot {
    margin: 0 2px !important;
  }

  .sub-categories-block .last-thread-meta .author {
    color: #1a507a !important;
  }

  .sub-categories-block .no-thread {
    font-size: 0.85rem !important;
    color: #888 !important;
    margin-top: 2px !important;
  }
}

.thread-filter-bar {
  background-color: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
  padding: 8px 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
}

.filter-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #1a507a;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  user-select: none;
  padding: 4px 8px;
  border-radius: 3px;
  transition: background-color 0.2s;
}

.filter-trigger:hover {
  background-color: rgba(26, 80, 122, 0.08);
}

.filter-trigger .arrow-down {
  font-size: 0.7rem;
  color: #1a507a;
}

/* Custom Dropdown Filters styling */
.filter-dropdown {
  position: absolute;
  top: 100%;
  right: -8px;
  background-color: #fff;
  border: 1px solid #c5d5e2;
  border-top: 3px solid #3498db;
  border-radius: 0 0 4px 4px;
  width: 327px;
  z-index: 100;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  overflow: visible; /* Allow option dropdown to spill over bottom */
}

.filter-dropdown-header {
  background-color: #eef4f9;
  border-bottom: 1px solid #dee2e6;
  color: #1a507a;
  padding: 8px 12px;
  font-weight: bold;
  font-size: 0.9rem;
}

.filter-dropdown-body {
  padding: 12px;
}

.filter-field-group {
  margin-bottom: 12px;
}

.filter-field-label {
  display: block;
  font-size: 0.8rem;
  color: #333;
  margin-bottom: 6px;
  font-weight: 600;
}

.select-selected-container {
  display: flex;
  align-items: center;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  padding: 0 5px;
  background-color: #fff;
  position: relative;
  height: 34px;
}

.select-search-input {
  border: none;
  background: transparent;
  outline: none;
  font-size: 0.85rem;
  color: #333;
  width: 100%;
  padding-right: 35px;
  height: 100%;
}

.select-search-input::placeholder {
  color: #8c8c8c;
}

.select-clear-btn {
  position: absolute;
  right: 22px;
  cursor: pointer;
  font-size: 1.1rem;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 0 4px;
}

.select-clear-btn:hover {
  filter: brightness(0.8);
}

.select-arrow-icon {
  position: absolute;
  right: 8px;
  font-size: 0.6rem;
  color: #8c8c8c;
  cursor: pointer;
}

.filter-label-select,
.filter-thread-type {
  position: relative;
}

.filter-label-select .select-items,
.filter-thread-type .select-items {
  position: absolute;
  background-color: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 101;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  max-height: 200px;
  overflow-y: auto;
  margin-top: 5px;
  padding: 5px;
}

.filter-label-select .select-item,
.filter-thread-type .select-item {
  padding: 8px 10px;
  cursor: pointer;
  border-radius: 3px;
  margin-bottom: 2px;
  font-weight: 500;
  color: #333;
  border: 1px solid transparent;
}

.filter-label-select .select-item:hover,
.filter-thread-type .select-item:hover {
  filter: brightness(0.9);
  background-color: #f8f9fa;
}

.filter-dropdown-footer {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #dee2e6;
  padding-top: 10px;
  margin-top: 15px;
}

.btn-submit-filter {
  background-color: #3498db;
  color: white;
  border: none;
  padding: 6px 16px;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
  font-size: 0.85rem;
  transition: background-color 0.2s;
}

.btn-submit-filter:hover {
  background-color: #2980b9;
}

.active-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.active-filter-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  font-size: 0.75rem;
  font-weight: bold;
  border-radius: 4px;
  border: 1px solid transparent;
}

.remove-filter-btn {
  cursor: pointer;
  font-size: 1rem;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 2px;
  margin-left: 2px;
}

.remove-filter-btn:hover {
  filter: brightness(0.8);
}

.autocomplete-dropdown-filter {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #c5d5e2;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  max-height: 200px;
  overflow-y: auto;
  margin-top: 5px;
}

.autocomplete-item-filter {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.autocomplete-item-filter:hover {
  background-color: #f5f7fa;
}

.user-avatar-mini {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 11px;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid #dee2e6;
}

.user-avatar-mini img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}


@import "@/shared/assets/styles/custom.css";

.sort-two-col {
  display: flex;
  gap: 8px;
}

.filter-sort-by {
  flex: 1.5;
  position: relative;
}

.filter-sort-order {
  flex: 1;
  position: relative;
}

.filter-sort-by .select-items,
.filter-sort-order .select-items {
  position: absolute;
  background-color: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 101;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  margin-top: 5px;
  padding: 5px;
}

.filter-sort-by .select-item,
.filter-sort-order .select-item {
  padding: 8px 0px;
  cursor: pointer;
  border-radius: 3px;
  margin-bottom: 2px;
  font-weight: 500;
  color: #333;
  border: 1px solid transparent;
  font-size: 0.85rem;
}

.filter-sort-by .select-item:hover,
.filter-sort-order .select-item:hover {
  background-color: #f8f9fa;
}

.sort-direction-icon {
  width: 12px;
  height: 12px;
  vertical-align: middle;
  margin-left: 4px;
  display: inline-block;
  opacity: 0.8;
}

.sub-categories-list .category-row {
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.sub-categories-list .category-row:hover {
  background-color: #f8f9fa;
}

</style>
