<template>
  <header>
    <div class="header-top">
      <div class="container">
        <div class="logo" style="cursor: pointer;" @click="goToHome">HTXSL</div>
      </div>
    </div>
    <div class="header-nav">
      <div class="container nav-container">
        <!-- Mobile hamburger and logo -->
        <div class="nav-left-mobile">
          <button class="mobile-hamburger" @click="isSidebarOpen = true" aria-label="Menu">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="3" y1="12" x2="21" y2="12"></line>
              <line x1="3" y1="6" x2="21" y2="6"></line>
              <line x1="3" y1="18" x2="21" y2="18"></line>
            </svg>
          </button>
          <div class="mobile-logo logo" @click="goToHome">HTXSL</div>
        </div>

        <div class="nav-scroll-wrapper">
          <button 
            class="nav-scroll-btn btn-left" 
            v-show="canScrollLeft" 
            @click="scrollNav('left')" 
            aria-label="Cuộn sang trái"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
          </button>

          <nav class="nav-links" ref="navLinks">
            <router-link class="fs-18"
              v-for="menu in activeMenus"
              :key="menu.id"
              :to="menu.url"
              :class="{ 'active': isMenuActive(menu) }"
              @click="handleMenuClick($event, menu.url)"
            >
              {{ menu.title }}
            </router-link>
          </nav>

          <button 
            class="nav-scroll-btn btn-right" 
            v-show="canScrollRight" 
            @click="scrollNav('right')" 
            aria-label="Cuộn sang phải"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </button>
        </div>
        <div class="nav-right">
          <div class="nav-group-user" :class="{ 'active': showUserDropdown }" v-if="isLoggedIn" ref="userContainer" @click="toggleUserDropdown" style="cursor: pointer;">
            <div class="user-info-header">
              <img v-if="isAvatarUrl(currentUser.avatar)" :src="currentUser.avatar" class="user-avatar-small user-avatar-img" />
              <span v-else class="user-avatar-small" :style="{ backgroundColor: currentUser.avatar || '#fff', color: currentUser.avatar ? '#fff' : '#1a507a' }">
                {{ (currentUser.displayName || currentUser.username).charAt(0).toUpperCase() }}
              </span>
              <span class="user-greeting fs-18 color-c9d6e0">{{ truncatedDisplayName }}</span>
              
              <!-- User Dropdown Menu (XenForo style) -->
              <div class="user-dropdown xamvn-dropdown" v-show="showUserDropdown" @click.stop>
                <!-- Tab headers -->
                <div class="xamvn-dropdown-tabs">
                  <button 
                    class="xamvn-tab-btn" 
                    :class="{ 'active': activeUserTab === 'account' }" 
                    @click="activeUserTab = 'account'"
                  >
                    Tài khoản của bạn
                  </button>
                  <button 
                    class="xamvn-tab-btn" 
                    :class="{ 'active': activeUserTab === 'bookmarks' }" 
                    @click="activeUserTab = 'bookmarks'"
                  >
                    Dấu trang
                  </button>
                </div>

                <!-- Tab: Tài khoản của bạn -->
                <div v-show="activeUserTab === 'account'" class="xamvn-tab-content">
                  <!-- User Brief Info -->
                  <div class="xamvn-user-brief">
                    <div class="xamvn-avatar-wrapper" @click.stop="openAvatarModal">
                      <img v-if="isAvatarUrl(currentUser.avatar)" :src="currentUser.avatar" class="xamvn-avatar-large xamvn-avatar-img" />
                      <div v-else class="xamvn-avatar-large" :style="{ backgroundColor: currentUser.avatar || '#fff', color: currentUser.avatar ? '#fff' : '#1a507a' }">
                        {{ (currentUser.displayName || currentUser.username).charAt(0).toUpperCase() }}
                      </div>
                      <div class="xamvn-avatar-edit-overlay">
                        <span>Sửa</span>
                      </div>
                    </div>
                    <div class="xamvn-user-details">
                      <div class="xamvn-username">{{ currentUser.displayName || currentUser.username }}</div>
                      <div class="xamvn-title">Yếu sinh lý</div>
                      <div class="xamvn-stats">
                        <div class="xamvn-stat-row">
                          <span class="xamvn-stat-label">Bài viết:</span>
                          <span class="xamvn-stat-value">{{ currentUser.postCount || 0 }}</span>
                        </div>
                        <div class="xamvn-stat-row">
                          <span class="xamvn-stat-label">Điểm tương tác:</span>
                          <span class="xamvn-stat-value">{{ currentUser.interactionPoints || 0 }}</span>
                        </div>
                        <div class="xamvn-stat-row">
                          <span class="xamvn-stat-label">Điểm thành tích:</span>
                          <span class="xamvn-stat-value">{{ currentUser.trophyPoints || 0 }}</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Double Column Links -->
                  <div class="xamvn-links-grid" @click="showUserDropdown = false">
                    <router-link :to="{ name: 'UserProfile' }" class="xamvn-link-item">Trang cá nhân</router-link>
                    <a href="#" class="xamvn-link-item" @click.prevent="goToReceivedReactions">Điểm tương tác nhận được</a>
                    <a href="#" class="xamvn-link-item" @click.prevent>Chi tiết tài khoản</a>
                    <a href="#" class="xamvn-link-item" @click.prevent>Tùy chọn</a>
                    <router-link :to="{ name: 'ChangePassword' }" class="xamvn-link-item">Mật khẩu</router-link>
                    <a href="#" class="xamvn-link-item" @click.prevent>Đang theo dõi</a>
                    <a href="#" class="xamvn-link-item" @click.prevent>Bảo mật cá nhân</a>
                    <a href="#" class="xamvn-link-item" @click.prevent>Phớt lờ</a>
                  </div>

                </div>

                <!-- Tab: Dấu trang -->
                <div v-show="activeUserTab === 'bookmarks'" class="xamvn-tab-content xamvn-bookmarks-tab">
                  <div class="xamvn-empty-message">
                    Không có dấu trang nào được lưu.
                  </div>
                </div>

                <hr class="xamvn-divider" />

                <!-- Bottom Logout Button -->
                <div class="xamvn-footer">
                  <button @click.stop="handleLogout" class="xamvn-logout-btn">Thoát</button>
                </div>
              </div>
            </div>
          </div>
          <template v-else>
            <router-link :to="{ name: 'Register' }" class="nav-group-user fs-18">Đăng ký</router-link>
            <router-link :to="{ name: 'Login' }" class="nav-group-user fs-18">Đăng nhập</router-link>
          </template>

          <!-- Mailbox Container -->
          <div class="mailbox-container" :class="{ 'active': showMailDropdown }" ref="mailContainer" v-if="isLoggedIn">
             <button class="btn-icon-mail" :class="{ 'shake-animation': isMailShaking }" @click.stop="toggleMailDropdown" aria-label="Inbox">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                  <polyline points="22,6 12,13 2,6"></polyline>
                </svg>
                <span class="notif-badge" :class="{ 'pulse-animation': isMailShaking }" v-if="unreadMailCount > 0">{{ unreadMailCount > 99 ? '99+' : unreadMailCount }}</span>
             </button>

             <!-- Mailbox Dropdown -->
             <div class="mail-dropdown" v-show="showMailDropdown">
                <div class="notif-header">
                   <span class="notif-title">Hộp thư</span>
                </div>
                
                 <!-- Mail Tabs -->
                 <div class="mail-tabs">
                    <div class="mail-tabs-left">
                       <button class="mail-tab-btn" :class="{ 'active': activeMailTab === 'all' }" @click="activeMailTab = 'all'">Tất cả</button>
                       <button class="mail-tab-btn" :class="{ 'active': activeMailTab === 'unread' }" @click="activeMailTab = 'unread'">Chưa đọc</button>
                    </div>
                    <div class="mail-tabs-actions">
                      <button class="btn-mark-all-read" @click.stop="markAllMailRead">Đánh dấu đã xem</button>
                      <button class="btn-mark-all-read btn-clear-mail-header" @click.stop="clearAllMail">Xóa</button>
                    </div>
                 </div>
                
                <div class="notif-list" v-if="paginatedConversations.length > 0">
                   <div 
                     v-for="convo in paginatedConversations" 
                     :key="convo.id" 
                     class="notif-item" 
                     :class="{ 'unread': !convo.isRead }"
                     @click="goToConversation(convo)"
                   >
                      <div class="notif-avatar-wrapper">
                         <user-profile-popup :user="getConvoUser(convo)" v-if="getConvoUser(convo)">
                            <div class="notif-avatar" :style="!isAvatarUrl(getConvoAvatarBg(convo)) ? { backgroundColor: getConvoAvatarBg(convo) } : {}">
                               <img v-if="isAvatarUrl(getConvoAvatarBg(convo))" :src="getConvoAvatarBg(convo)" />
                               <template v-else>
                                  {{ getConvoAvatarText(convo) }}
                               </template>
                            </div>
                         </user-profile-popup>
                         <div v-else class="notif-avatar" style="background-color: #ccc; color: #fff;">C</div>
                      </div>
                      <div class="notif-body">
                         <div class="notif-text">
                            <template v-if="convo.isReaction">
                               <strong>{{ convo.creatorDisplayName || convo.creatorUsername }}</strong> đã tương tác 
                               <ReactionIcon :code="convo.reactionIcon" :color="convo.reactionColor" size="18px" style="display:inline-flex;vertical-align:middle;" />
                               <strong :style="{ color: convo.reactionColor || '#2c3e50' }">{{ convo.reactionName }}</strong>
                               với trả lời của bạn trong hội thoại 
                               <span class="convo-title-link" style="display: inline;">{{ convo.title }}</span>
                            </template>
                            <template v-else-if="convo.isReply">
                               <template v-if="currentUser && convo.lastMessageSenderUsername === currentUser.username">
                                  Bạn đã trả lời vào cuộc đối thoại 
                                  <span class="convo-title-link" style="display: inline;">{{ convo.title }}</span>
                               </template>
                               <template v-else>
                                  <strong>{{ convo.lastMessageSenderDisplayName || convo.lastMessageSenderUsername }}</strong> đã trả lời vào cuộc đối thoại 
                                  <span class="convo-title-link" style="display: inline;">{{ convo.title }}</span>
                               </template>
                            </template>
                            <template v-else-if="convo.isQuote">
                               <strong>{{ convo.lastMessageSenderDisplayName || convo.lastMessageSenderUsername }}</strong> đã trích tin nhắn của bạn trong cuộc đối thoại 
                               <span class="convo-title-link" style="display: inline;">{{ convo.title }}</span>
                            </template>
                            <template v-else-if="convo.isMention">
                               <strong>{{ convo.lastMessageSenderDisplayName || convo.lastMessageSenderUsername }}</strong> đã tag bạn trong cuộc hội thoại 
                               <span class="convo-title-link" style="display: inline;">{{ convo.title }}</span>
                            </template>
                            <template v-else-if="currentUser && convo.creatorUsername === currentUser.username">
                               Bạn đã mở cuộc hội thoại 
                               <span class="convo-title-link" style="display: inline;">{{ convo.title }}</span>
                               với {{ getRecipients(convo) }}
                            </template>
                            <template v-else>
                               {{ convo.creatorDisplayName || convo.creatorUsername }} đã bắt đầu cuộc hội thoại 
                               <span class="convo-title-link" style="display: inline;">{{ convo.title }}</span>
                               với bạn
                            </template>
                         </div>
                         <!-- Line 3: Dynamic Time -->
                         <div class="notif-time">{{ formatTime(convo.updatedAt || convo.createdAt) }}</div>
                      </div>
                     <div class="notif-status-dot" v-if="!convo.isRead"></div>
                   </div>
                </div>
                
                <div class="notif-empty" v-else>
                   <span v-if="activeMailTab === 'unread'">Không có cuộc đối thoại chưa đọc nào.</span>
                   <span v-else>Không có cuộc đối thoại nào mới.</span>
                </div>
                
                <div class="notif-footer">
                   <a href="#" class="btn-load-more" :class="{ 'disabled': !hasMoreMail }" @click.prevent="loadMoreMail">Xem thêm</a>
                    <span style="color: #ccc;">·</span>
                    <router-link :to="{ name: 'ConversationList' }" @click="showMailDropdown = false">Xem tất cả</router-link>
                    <span style="color: #ccc;">·</span>
                    <a href="#" @click.prevent="goToAddConvo" v-if="!isNonOfficial">Bắt đầu đối thoại mới</a>
                 </div>
             </div>
          </div>

          <!-- Notification Bell Container -->
          <div class="notification-bell-container" :class="{ 'active': showNotifDropdown }" ref="notifContainer" v-if="isLoggedIn">
             <button class="btn-icon-bell" :class="{ 'shake-animation': isShaking }" @click.stop="toggleNotifDropdown" aria-label="Notifications">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                  <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                </svg>
                <span class="notif-badge" :class="{ 'pulse-animation': isShaking }" v-if="unreadCount > 0">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
             </button>

             <!-- Notification Dropdown -->
             <div class="notif-dropdown" v-show="showNotifDropdown">
                 <div class="notif-header">
                    <span class="notif-title">Thông báo</span>
                 </div>
                 
                 <!-- Notification Tabs -->
                 <div class="mail-tabs">
                    <div class="mail-tabs-left">
                       <button class="mail-tab-btn" :class="{ 'active': activeNotifTab === 'all' }" @click="activeNotifTab = 'all'">Tất cả</button>
                       <button class="mail-tab-btn" :class="{ 'active': activeNotifTab === 'unread' }" @click="activeNotifTab = 'unread'">Chưa đọc</button>
                    </div>
                    <div class="mail-tabs-actions">
                      <button class="btn-mark-all-read" @click.stop="markAllRead">Đánh dấu đã xem</button>
                      <button class="btn-mark-all-read btn-clear-mail-header" @click.stop="clearAllNotifications">Xóa</button>
                    </div>
                 </div>
                 
                 <div class="notif-list" v-if="paginatedNotifications.length > 0">
                    <div 
                      v-for="notif in paginatedNotifications" 
                      :key="notif.id" 
                     class="notif-item" 
                     :class="{ 'unread': !notif.isRead }"
                     @click="handleNotifClick(notif)"
                   >
                      <div class="notif-avatar-wrapper">
                         <user-profile-popup :user="getNotifUser(notif)" v-if="getNotifUser(notif)">
                            <div class="notif-avatar" :style="!isAvatarUrl(notif.actorAvatar) ? { backgroundColor: notif.actorAvatar || '#3498db' } : {}">
                               <img v-if="isAvatarUrl(notif.actorAvatar)" :src="notif.actorAvatar" />
                               <template v-else>
                                  {{ (notif.actorDisplayName || notif.actorUsername || '?').charAt(0).toUpperCase() }}
                               </template>
                            </div>
                         </user-profile-popup>
                         <div v-else class="notif-avatar" style="background-color: #ccc; color: #fff;">?</div>
                      </div>
                     <div class="notif-body">
                        <div class="notif-text">
                           <strong>{{ notif.actorDisplayName || notif.actorUsername }}</strong>
                           <template v-if="notif.type === 'REACTION'">
                              đã tương tác <ReactionIcon :code="notif.reactionIcon" :color="notif.reactionColor" size="18px" style="display:inline-flex;vertical-align:middle;" /> 
                              <strong :style="{ color: notif.reactionColor || '#2c3e50' }">{{ notif.reactionName }}</strong>
                              với bài viết của bạn trong chủ đề
                           </template>
                           <template v-else-if="notif.type === 'QUOTE'">
                              đã trích bài viết của bạn trong chủ đề
                           </template>
                           <template v-else-if="notif.type === 'MENTION'">
                              đã tag bạn trong chủ đề
                           </template>
                           <template v-else>
                              đã trả lời vào chủ đề
                           </template>
                           <span class="notif-link-block" @click.stop="handleNotifClick(notif)">
                              <span v-if="notif.threadLabelName" class="notif-label-tag" :style="{ backgroundColor: notif.type === 'MENTION' ? '#2577b1' : (notif.threadLabelColor || '#95a5a6'), color: notif.type === 'MENTION' ? '#fff' : (notif.threadLabelTextColor || '#fff'), borderColor: notif.type === 'MENTION' ? 'transparent' : (notif.threadLabelBorderColor || 'transparent') }">{{ notif.threadLabelName }}</span>
                              <span class="highlight-thread">{{ notif.threadTitle }}</span>
                           </span>.
                           <span v-if="notif.type !== 'QUOTE' && notif.type !== 'REACTION' && notif.type !== 'MENTION'" class="notif-extra">Có thể có bài viết thêm trong chủ đề</span>
                        </div>
                        <div class="notif-time">{{ formatTime(notif.createdAt) }}</div>
                     </div>
                     <div class="notif-status-dot" v-if="!notif.isRead"></div>
                   </div>
                </div>
                
                <div class="notif-empty" v-else>
                   <span v-if="activeNotifTab === 'unread'">Không có thông báo chưa đọc nào.</span>
                   <span v-else>Không có thông báo nào mới.</span>
                </div>
                
                <div class="notif-footer">
                    <a href="#" class="btn-load-more" :class="{ 'disabled': !hasMoreNotif }" @click.prevent="loadMoreNotif">Xem thêm</a>
                    <span style="color: #ccc;">·</span>
                    <router-link :to="{ name: 'NotificationsList' }" @click="showNotifDropdown = false">Xem tất cả</router-link>
                 </div>
             </div>
          </div>

          <!-- Search Dropdown Container -->
          <div class="search-container" :class="{ 'active': showSearchDropdown }" ref="searchContainer">
            <div class="btn-search" @click.stop="toggleSearchDropdown">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
              <span class="search-text color-c9d6e0">Tìm kiếm</span>
            </div>
            
            <!-- Search Dropdown Popup -->
            <div class="search-dropdown" v-show="showSearchDropdown" @click.stop>
              <form @submit.prevent="confirmHeaderSearch" action="" style="width: 100%;">
                <div class="search-input-wrapper">
                  <input 
                    type="search" 
                    :value="searchQuery" 
                    name="search"
                    placeholder="Tìm kiếm..." 
                    autocomplete="off"
                    autocorrect="off"
                    autocapitalize="off"
                    spellcheck="false"
                    @keydown.enter="confirmHeaderSearch" 
                    @keydown.down.prevent="navigateSearchDropdown('down')"
                    @keydown.up.prevent="navigateSearchDropdown('up')"
                    @keydown.right="handleArrowRight"
                    @keydown.left="handleArrowLeft"
                    @keydown.esc="closeSearchDropdown"
                    @click="handleSearchFocus"
                    @input="handleSearchInput"
                    ref="searchInput"
                    :class="['search-input', { 'preview-selected': isPreviewSelected }]"
                    enterkeyhint="search"
                    inputmode="search"
                  />
                  <button type="submit" class="btn-search-submit" aria-label="Tìm kiếm">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                  </button>
                </div>
              </form>

              <!-- Dropdown lịch sử tìm kiếm (desktop only) -->
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
                  <button 
                    type="button" 
                    :class="['delete-history-btn', { 'focused-delete': isDeleteFocused && idx === selectedIndex }]"
                    @click.stop="removeFromHistory(keyword)"
                    aria-label="Xóa từ khóa"
                  >
                    &times;
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Mobile Sidebar -->
    <div class="mobile-sidebar" :class="{ 'open': isSidebarOpen }">
      <div class="sidebar-backdrop" @click="isSidebarOpen = false"></div>
      <div class="sidebar-content">
        <div class="sidebar-header">
          <div class="sidebar-logo" @click="goToHome(); isSidebarOpen = false">HTXSL</div>
          <button class="btn-close-sidebar" @click="isSidebarOpen = false" aria-label="Đóng menu">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <nav class="sidebar-nav">
          <router-link
            v-for="menu in activeMenus"
            :key="menu.id"
            :to="menu.url"
            :class="{ 'active': isMenuActive(menu) }"
            @click="handleMenuClick($event, menu.url, true)"
          >
            {{ menu.title }}
          </router-link>
        </nav>
      </div>
    </div>
  </header>

  <div class="container header-banner-container">
    <div class="banner-box" style="margin-top: 1rem;">
      <img src="/675456323_122106804740812631_4737388993277477397_n.jpg" alt="HTXHS Banner">
    </div>
  </div>

  <PendingApprovalBanner v-if="isNonOfficial" />
  <SearchModal v-model:show="showSearchModal" :initial-query="searchQuery" />
  <AvatarUploadModal :show="showAvatarModal" :current-user="currentUser" @close="showAvatarModal = false" @avatar-updated="onAvatarUpdated" />
</template>

<script>
import api from '@/shared/services/api.service'
import webSocketService from '@/shared/services/websocket.service'
import conversationService from '@/apps/Forum/services/conversation.service'
import menuService from '@/apps/Forum/services/menu.service'
import notificationService from '@/apps/Forum/services/notification.service'
import { formatForumDate } from '@/shared/utils/date'
import { alertSuccess, alertWarning } from '@/shared/utils/swal'
import { isNonOfficialUser, truncateString } from '@/shared/utils/utils'
import PendingApprovalBanner from '@/shared/components/PendingApprovalBanner.vue'
import SearchModal from '@/shared/components/SearchModal.vue'
import ReactionIcon from '@/shared/components/ReactionIcon.vue'
import AvatarUploadModal from '@/shared/components/AvatarUploadModal.vue'
import UserProfilePopup from '@/shared/components/UserProfilePopup.vue'
import searchHistoryMixin from '@/shared/mixins/searchHistory.mixin.js'

export default {
  name: 'ForumHeader',
  mixins: [searchHistoryMixin],
  components: {
    PendingApprovalBanner,
    SearchModal,
    ReactionIcon,
    AvatarUploadModal,
    UserProfilePopup
  },
  data() {
    return {
      windowWidth: window.innerWidth,
      menus: [],
      isLoggedIn: false,
      currentUser: null,
      showNotifDropdown: false,
      showUserDropdown: false,
      showMailDropdown: false,
      isSidebarOpen: false,
      notifications: [],
      unreadCount: 0,
      isShaking: false,
      conversations: [],
      unreadMailCount: 0,
      isMailShaking: false,
      activeMailTab: 'all',
      mailLimitAll: 10,
      mailLimitUnread: 10,
      activeNotifTab: 'all',
      notifLimitAll: 10,
      notifLimitUnread: 10,
      canScrollLeft: false,
      canScrollRight: false,
      showSearchDropdown: false,
      showSearchModal: false,
      activeUserTab: 'account',
      showAvatarModal: false
    }
  },
  computed: {
    isMobile() {
      return this.windowWidth < 768
    },
    activeMenus() {
      return this.menus.filter(menu => menu.active)
    },
    isNonOfficial() {
      return isNonOfficialUser()
    },
    truncatedDisplayName() {
      if (!this.currentUser) return ''
      const name = this.currentUser.displayName || this.currentUser.username || ''
      return truncateString(name, 8)
    },
    filteredConversations() {
      if (this.activeMailTab === 'unread') {
        return this.conversations.filter(c => !c.isRead)
      }
      return this.conversations
    },
    paginatedConversations() {
      const limit = this.activeMailTab === 'all' ? this.mailLimitAll : this.mailLimitUnread
      return this.filteredConversations.slice(0, limit)
    },
    hasMoreMail() {
      const limit = this.activeMailTab === 'all' ? this.mailLimitAll : this.mailLimitUnread
      return this.filteredConversations.length > limit
    },
    filteredNotifications() {
      if (this.activeNotifTab === 'unread') {
        return this.notifications.filter(n => !n.isRead)
      }
      return this.notifications
    },
    paginatedNotifications() {
      const limit = this.activeNotifTab === 'all' ? this.notifLimitAll : this.notifLimitUnread
      return this.filteredNotifications.slice(0, limit)
    },
    hasMoreNotif() {
      const limit = this.activeNotifTab === 'all' ? this.notifLimitAll : this.notifLimitUnread
      return this.filteredNotifications.length > limit
    }
  },
  watch: {
    menus() {
      this.$nextTick(() => {
        setTimeout(this.updateScrollArrows, 300)
      })
    }
  },
  async mounted() {
    this.checkAuth()
    
    if (this.isLoggedIn && this.currentUser) {
      try {
        this.syncUserProfile()
        this.fetchNotifSummary()
        this.fetchMailSummary()
        this.setupSocket()
      } catch (error) {
        console.error('Lỗi khi thiết lập thông tin người dùng đăng nhập:', error)
      }
    }
    
    document.addEventListener('click', this.handleClickOutside)

    const nav = this.$refs.navLinks
    if (nav) {
      nav.addEventListener('scroll', this.updateScrollArrows)
    }
    window.addEventListener('resize', this.handleResize)
    window.addEventListener('user-avatar-updated', this.handleAvatarUpdated)
    window.addEventListener('notifications-updated', this.fetchNotifSummary)

    try {
      const response = await menuService.getAll()
      this.menus = response.data
    } catch (error) {
      console.error('Lỗi khi tải menu:', error)
      this.menus = [
        { id: 1, title: 'Trang nhất', url: '/' }
      ]
    }

    this.$nextTick(() => {
      setTimeout(this.updateScrollArrows, 500)
    })
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleClickOutside)
    if (this.notifUnsubscribe) {
      this.notifUnsubscribe()
    }
    if (this.convoUnsubscribe) {
      this.convoUnsubscribe()
    }
    const nav = this.$refs.navLinks
    if (nav) {
      nav.removeEventListener('scroll', this.updateScrollArrows)
    }
    window.removeEventListener('resize', this.handleResize)
    window.removeEventListener('user-avatar-updated', this.handleAvatarUpdated)
    window.removeEventListener('notifications-updated', this.fetchNotifSummary)
  },
  methods: {
    isMenuActive(menu) {
      if (!menu || !menu.url) return false
      const path = this.$route.path
      if (menu.url === '/' || menu.url === '/trang-chu') {
        return path === '/' || path === '/trang-chu'
      }
      if (menu.url === '/thanh-vien' || menu.url === '/members') {
        return path === '/thanh-vien' || path === '/members' || path === '/account/profile'
      }
      return path === menu.url || path.startsWith(menu.url + '/')
    },
    handleResize() {
      this.windowWidth = window.innerWidth
      this.updateScrollArrows()
    },
    handleAvatarUpdated(event) {
      const { username, avatar } = event.detail
      if (this.currentUser && this.currentUser.username === username) {
        this.currentUser.avatar = avatar
      }
    },
    goToHome() {
      if (this.$route.name === 'Home') {
        window.scrollTo({ top: 0, behavior: 'smooth' })
      } else {
        this.$router.push({ name: 'Home' })
      }
    },
    handleMenuClick(event, url, closeSidebar = false) {
      if (closeSidebar) {
        this.isSidebarOpen = false
      }
      if (this.$route.path === url || (url === '/' && this.$route.name === 'Home')) {
        window.scrollTo({ top: 0, behavior: 'smooth' })
      }
    },
    async syncUserProfile() {
      if (!this.isLoggedIn || !this.currentUser || !this.currentUser.username) return
      try {
        const res = await api.get(`/users/by-name?name=${this.currentUser.username}`)
        if (res.data) {
          const dbUser = res.data
          const updatedUser = { 
            ...this.currentUser, 
            displayName: dbUser.displayName,
            avatar: dbUser.avatar,
            roles: dbUser.roles,
            postCount: dbUser.postCount,
            interactionPoints: dbUser.interactionPoints,
            trophyPoints: dbUser.trophyPoints
          }
          
          const currentRoles = this.currentUser.roles || []
          const isRolesChanged = dbUser.roles.length !== currentRoles.length || 
                                 !dbUser.roles.every(r => currentRoles.includes(r))
          
          localStorage.setItem('user', JSON.stringify(updatedUser))
          this.currentUser = updatedUser
          
          if (isRolesChanged) {
            window.location.reload()
          }
        }
      } catch (e) {
        console.error('Lỗi khi đồng bộ vai trò người dùng:', e)
      }
    },
    scrollNav(direction) {
      const nav = this.$refs.navLinks
      if (!nav) return
      const scrollAmount = 150
      const newScrollLeft = direction === 'left' 
        ? nav.scrollLeft - scrollAmount 
        : nav.scrollLeft + scrollAmount
      
      nav.scrollTo({
        left: newScrollLeft,
        behavior: 'smooth'
      })
    },
    updateScrollArrows() {
      const nav = this.$refs.navLinks
      if (!nav) return
      this.canScrollLeft = nav.scrollLeft > 5
      this.canScrollRight = nav.scrollLeft < (nav.scrollWidth - nav.clientWidth - 5)
    },
    checkAuth() {
      const user = localStorage.getItem('user')
      if (user) {
        this.isLoggedIn = true
        this.currentUser = JSON.parse(user)
      }
    },
    handleLogout() {
      webSocketService.disconnect()
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      this.isLoggedIn = false
      this.currentUser = null
      this.$emit('logout')
      if (this.$route.meta.requiresAuth) {
        this.$router.push({ name: 'Home' })
      } else {
        window.location.reload() // Reload to update UI across components
      }
    },
    
    // --- Notification System Logic ---
    
    setupSocket() {
      if (!this.currentUser || !this.currentUser.id) return
      
      // Connect (using username for tracking connection identifier internally in service)
      webSocketService.connect(this.currentUser.username)
      
      // Clean up existing subscription if present
      if (this.notifUnsubscribe) {
        this.notifUnsubscribe()
      }
      if (this.convoUnsubscribe) {
        this.convoUnsubscribe()
      }

      // Register callback for live push notifications - USING NUMERICAL USER ID FOR TOPIC
      this.notifUnsubscribe = webSocketService.subscribeToNotifications(this.currentUser.id, (newNotif) => {
        // Add to top of list
        this.notifications.unshift(newNotif)
        this.unreadCount++
        
        // Hiệu ứng âm thanh và rung chuông
        this.playNotifSound()
        this.triggerShake()
      })

      // Register callback for live conversations - USING NUMERICAL USER ID FOR TOPIC
      this.convoUnsubscribe = webSocketService.subscribe(`/topic/conversations/${this.currentUser.id}`, (newConvo) => {
        // Add to top of list and avoid duplicates for same convo
        if (newConvo.isReaction || newConvo.isReply || newConvo.isQuote || newConvo.isMention) {
          this.conversations.unshift(newConvo)
        } else {
          this.conversations = this.conversations.filter(c => c.id !== newConvo.id || c.isReaction || c.isReply || c.isQuote || c.isMention)
          this.conversations.unshift(newConvo)
        }
        
        // If not read (for recipient), trigger visual/audio notifications
        if (!newConvo.isRead) {
          this.unreadMailCount++
          this.playNotifSound()
          this.triggerMailShake()
        }
      })
    },
    
    playNotifSound() {
      // Bell sound URL (Mixkit generic notification)
      const audio = new Audio('https://assets.mixkit.co/active_storage/sfx/2550/2550-preview.mp3');
      audio.volume = 0.5;
      audio.play().catch(e => {
        // Many browsers block autoplay without interaction
        console.log('Autoplay sound blocked or audio error:', e);
      });
    },
    
    triggerShake() {
       this.isShaking = false;
       this.$nextTick(() => {
         this.isShaking = true;
         setTimeout(() => {
            this.isShaking = false;
         }, 3000);
       });
    },
    
    async fetchNotifSummary() {
      try {
        const [listRes, countRes] = await Promise.all([
          notificationService.getAll(),
          notificationService.getUnreadCount()
        ])
        this.notifications = listRes.data
        this.unreadCount = countRes.data
      } catch (error) {
        console.error('Lỗi khi tải thông báo:', error)
      }
    },
    
    toggleNotifDropdown() {
      this.showNotifDropdown = !this.showNotifDropdown
      this.showUserDropdown = false
      this.showMailDropdown = false
      if (this.showNotifDropdown) {
        this.activeNotifTab = 'all'
        this.notifLimitAll = 10
        this.notifLimitUnread = 10
      }
    },
    loadMoreNotif() {
      if (this.hasMoreNotif) {
        if (this.activeNotifTab === 'all') {
          this.notifLimitAll += 10
        } else {
          this.notifLimitUnread += 10
        }
      } else {
        alertWarning('Đã tải toàn bộ thông báo')
      }
    },
    toggleUserDropdown() {
      this.showUserDropdown = !this.showUserDropdown
      this.showNotifDropdown = false
      this.showMailDropdown = false
      if (this.showUserDropdown) {
        this.activeUserTab = 'account'
        this.syncUserProfile()
      }
    },
    viewYourContent() {
      this.showUserDropdown = false
      this.searchQuery = this.currentUser.username
      this.showSearchModal = true
    },
    toggleMailDropdown() {
      this.showMailDropdown = !this.showMailDropdown
      this.showNotifDropdown = false
      this.showUserDropdown = false
      if (this.showMailDropdown) {
        this.activeMailTab = 'all'
        this.mailLimitAll = 10
        this.mailLimitUnread = 10
      }
    },
    loadMoreMail() {
      if (this.hasMoreMail) {
        if (this.activeMailTab === 'all') {
          this.mailLimitAll += 10
        } else {
          this.mailLimitUnread += 10
        }
      } else {
        alertWarning('Đã tải toàn bộ thông báo')
      }
    },
    async markAllMailRead() {
      try {
        await conversationService.markAllAsRead()
        this.unreadMailCount = 0
        this.conversations.forEach(c => c.isRead = true)
      } catch (e) {
        console.error('Lỗi khi đánh dấu đã đọc tất cả tin nhắn đối thoại:', e)
      }
    },
    async clearAllMail() {
      try {
        await conversationService.clearAll()
        this.conversations = []
        this.unreadMailCount = 0
        alertSuccess('Đã xóa toàn bộ thông báo trong hộp thư.')
      } catch (e) {
        console.error('Lỗi khi xóa toàn bộ thông báo hộp thư:', e)
      }
    },
    triggerMailShake() {
       this.isMailShaking = false;
       this.$nextTick(() => {
         this.isMailShaking = true;
         setTimeout(() => {
            this.isMailShaking = false;
         }, 3000);
       });
    },
    async fetchMailSummary() {
      try {
        const [listRes, countRes] = await Promise.all([
          conversationService.getAll(),
          conversationService.getUnreadCount()
        ])
        this.conversations = listRes.data || []
        this.unreadMailCount = countRes.data || 0
      } catch (error) {
        console.error('Lỗi khi tải hộp thư:', error)
      }
    },
    goToAddConvo() {
      this.showMailDropdown = false
      this.$router.push({ name: 'AddConversation' })
    },
    goToReceivedReactions() {
      this.showUserDropdown = false
      this.$router.push({ name: 'ReceivedReactions' })
    },
    async goToConversation(convo) {
      this.showMailDropdown = false
      
      if (!convo.isRead) {
        convo.isRead = true
        this.unreadMailCount = Math.max(0, this.unreadMailCount - 1)
        try {
          if (convo.isReaction || convo.isReply || convo.isQuote || convo.isMention) {
            await notificationService.markAsRead(convo.notificationId)
            await conversationService.markAsRead(convo.id)
          } else {
            await conversationService.markAsRead(convo.id)
          }
        } catch (e) {
          console.error('Lỗi khi đánh dấu đã đọc:', e)
        }
      }

      const targetMsgId = convo.isReaction ? convo.firstMessageId : ((convo.isReply || convo.isQuote || convo.isMention) ? convo.lastMessageId : convo.firstMessageId)

      // Dispatch global custom event for ConversationDetail to react if we are already on this conversation detail page
      window.dispatchEvent(new CustomEvent('conversation-clicked', {
        detail: {
          conversationId: convo.id,
          messageId: targetMsgId
        }
      }))

      const query = targetMsgId ? { messageId: targetMsgId } : {}
      this.$router.push({ 
        name: 'ConversationDetail', 
        params: { id: convo.id },
        query
      })
    },
    getRecipients(convo) {
      if (!convo || !convo.participants) return ''
      const myName = this.currentUser ? (this.currentUser.displayName || this.currentUser.username) : ''
      const myUsername = this.currentUser ? this.currentUser.username : ''
      const filtered = convo.participants.filter(p => p !== myName && p !== myUsername)
      return filtered.join(', ')
    },
    getConvoAvatarBg(convo) {
      if (convo.isReaction) return convo.creatorAvatar || '#3498db'
      if (convo.isReply || convo.isQuote || convo.isMention) return convo.lastMessageSenderAvatar || '#3498db'
      return convo.creatorAvatar || '#3498db'
    },
    getConvoAvatarText(convo) {
      let name = 'C'
      if (convo.isReaction) {
        name = convo.creatorDisplayName || convo.creatorUsername || 'C'
      } else if (convo.isReply || convo.isQuote || convo.isMention) {
        name = convo.lastMessageSenderDisplayName || convo.lastMessageSenderUsername || 'C'
      } else {
        name = convo.creatorDisplayName || convo.creatorUsername || 'C'
      }
      return name.charAt(0).toUpperCase()
    },
    
    handleClickOutside(e) {
      const container = this.$refs.notifContainer
      if (container && !container.contains(e.target)) {
        this.showNotifDropdown = false
      }
      const userContainer = this.$refs.userContainer
      if (userContainer && !userContainer.contains(e.target)) {
        this.showUserDropdown = false
      }
      const mailContainer = this.$refs.mailContainer
      if (mailContainer && !mailContainer.contains(e.target)) {
        this.showMailDropdown = false
      }
      const searchContainer = this.$refs.searchContainer
      if (searchContainer && !searchContainer.contains(e.target)) {
        this.showSearchDropdown = false
        this.showHistoryDropdown = false
        this.showMobileSuggestions = false
        this.selectedIndex = -1
      }
      const headerSearchContainer = this.$refs.headerSearchContainer
      if (headerSearchContainer && !headerSearchContainer.contains(e.target)) {
        this.showHistoryDropdown = false
        this.selectedIndex = -1
      }
    },
    
    getReactionIconUrl(code) {
      if (!code) return ''
      try {
        return require(`@/assets/reactions/${code}.svg`)
      } catch (e) {
        return ''
      }
    },
    getConvoUser(convo) {
      if (!convo) return null
      if (convo.isReaction) {
        return {
          username: convo.creatorUsername,
          displayName: convo.creatorDisplayName,
          avatar: convo.creatorAvatar
        }
      }
      if (convo.isReply || convo.isQuote || convo.isMention) {
        return {
          username: convo.lastMessageSenderUsername,
          displayName: convo.lastMessageSenderDisplayName,
          avatar: convo.lastMessageSenderAvatar
        }
      }
      return {
        username: convo.creatorUsername,
        displayName: convo.creatorDisplayName,
        avatar: convo.creatorAvatar
      }
    },
    getNotifUser(notif) {
      if (!notif) return null
      return {
        username: notif.actorUsername,
        displayName: notif.actorDisplayName,
        avatar: notif.actorAvatar
      }
    },
    isAvatarUrl(avatar) {
      if (!avatar) return false
      return avatar.startsWith('http://') || avatar.startsWith('https://') || avatar.startsWith('/')
    },
    openAvatarModal() {
      this.showAvatarModal = true
    },
    onAvatarUpdated(newAvatarUrl) {
      // Update current component state
      this.currentUser.avatar = newAvatarUrl
      // Persist to localStorage so it survives page refresh
      const stored = localStorage.getItem('user')
      if (stored) {
        const userData = JSON.parse(stored)
        userData.avatar = newAvatarUrl
        localStorage.setItem('user', JSON.stringify(userData))
      }
      
      // Dispatch a window event to update avatar in other active components
      window.dispatchEvent(new CustomEvent('user-avatar-updated', {
        detail: {
          username: this.currentUser.username,
          avatar: newAvatarUrl
        }
      }))
    },
    formatTime(dateStr) {
      return formatForumDate(dateStr)
    },
    
    async markAllRead() {
      try {
        await notificationService.markAllRead()
        this.unreadCount = 0
        this.notifications.forEach(n => n.isRead = true)
      } catch (e) {
        console.error(e)
      }
    },
    async clearAllNotifications() {
      try {
        await notificationService.clearAll()
        this.notifications = []
        this.unreadCount = 0
        alertSuccess('Đã xóa toàn bộ thông báo.')
      } catch (e) {
        console.error('Lỗi khi xóa toàn bộ thông báo:', e)
      }
    },
    
    async handleNotifClick(notif) {
      this.showNotifDropdown = false
      
      // 1. Mark this specific notification as read instantly locally
      if (!notif.isRead) {
        notif.isRead = true
        this.unreadCount = Math.max(0, this.unreadCount - 1)
        
        // Fire off the API async background (no wait)
        try {
          notificationService.markAsRead(notif.id)
        } catch (e) {
          console.error(e)
        }
      }
      
      // 2. Dispatch global custom event for ThreadDetail to react (e.g. if we are already on this thread)
      window.dispatchEvent(new CustomEvent('notification-clicked', {
        detail: {
          threadId: notif.threadId,
          postId: notif.postId
        }
      }))

      // 3. Route logic: navigate to exact post
      const routeTarget = {
         name: 'ThreadDetail',
         params: { id: notif.threadId }
      }
      
      if (notif.postId) {
         routeTarget.query = { postId: notif.postId }
      } else {
         // Default to main post for thread-level actions (like reactions to thread)
         routeTarget.query = { postId: 'main_thread_entry' }
      }
      
      this.$router.push(routeTarget)
    },
    toggleSearchDropdown() {
      this.showSearchDropdown = !this.showSearchDropdown
      this.showNotifDropdown = false
      this.showUserDropdown = false
      this.showMailDropdown = false
      this.showHistoryDropdown = false
      this.selectedIndex = -1
      this.isPreviewSelected = false
      if (this.showSearchDropdown) {
        this.searchQuery = ''
        this.originalQuery = ''
        this.filterQuery = ''
        this.loadSearchHistory()
        this.$nextTick(() => {
          if (this.$refs.searchInput) {
            this.$refs.searchInput.focus()
          }
        })
      }
    },
    triggerSearch() {
      if (!this.searchQuery.trim()) return
      this.saveToHistory(this.searchQuery.trim())
      // Chỉ blur trên mobile để đóng bàn phím ảo
      if (this.isMobile && this.$refs.searchInput) {
        this.$refs.searchInput.blur()
      }
      this.showSearchDropdown = false
      this.showSearchModal = true
      this.isPreviewSelected = false
    },
    confirmHeaderSearch() {
      this.confirmSearchSelection(this.triggerSearch)
    }
  }
}
</script>

<style scoped>
.notification-bell-container {
  position: relative;
}

.btn-icon-bell, .btn-icon-mail {
  background: transparent;
  border: none;
  color: #fff;
  cursor: pointer;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  outline: none;
  height: 100%;
  width: 100%;
}

.btn-icon-bell:hover, .btn-icon-mail:hover {
  background: transparent;
}

/* Bell Shake Animation */
.shake-animation {
  animation: bell-shake 3s cubic-bezier(.36,.07,.19,.97) both;
  transform-origin: center top;
}

@keyframes bell-shake {
  0% { transform: rotate(0); }
  10% { transform: rotate(15deg); }
  20% { transform: rotate(-15deg); }
  30% { transform: rotate(12deg); }
  40% { transform: rotate(-12deg); }
  50% { transform: rotate(8deg); }
  60% { transform: rotate(-8deg); }
  70% { transform: rotate(4deg); }
  80% { transform: rotate(-4deg); }
  90% { transform: rotate(2deg); }
  100% { transform: rotate(0); }
}

.notif-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  background-color: #e74c3c;
  color: white;
  font-size: 10px;
  font-weight: bold;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  border: 2px solid #1a507a;
  z-index: 2;
}

.pulse-animation {
  animation: badge-pulse 1s infinite;
}

@keyframes badge-pulse {
  0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(231, 76, 60, 0.7); }
  70% { transform: scale(1.1); box-shadow: 0 0 0 10px rgba(231, 76, 60, 0); }
  100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(231, 76, 60, 0); }
}

/* Dropdown Shell */
.notif-dropdown, .mail-dropdown {
  position: absolute;
  top: 50px;
  right: -100px;
  width: 360px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  z-index: 1000;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* Standard Arrow pointer at top */
.notif-dropdown::before, .mail-dropdown::before {
  content: '';
  position: absolute;
  top: -6px;
  right: 112px;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-bottom: 6px solid #f8f9fa;
}

.notif-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  background: #f8f9fa;
  border-bottom: 1px solid #eee;
}

.mail-tabs {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 15px;
}

.mail-tabs-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.notif-title {
  color: #2c3e50;
  font-weight: bold;
  font-size: 0.95rem;
}

.btn-read-all {
  background: none;
  border: none;
  color: #3498db;
  font-size: 0.8rem;
  cursor: pointer;
  padding: 0;
}

.btn-read-all:hover {
  text-decoration: underline;
}

.notif-list {
  max-height: 400px;
  overflow-y: auto;
}

.notif-item {
  display: flex;
  gap: 12px;
  padding: 12px 15px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}

.notif-item:hover {
  background: #f5f8fa;
}

.notif-item.unread {
  background: #f0f7fb;
}

.notif-avatar-wrapper {
  flex-shrink: 0;
}

.notif-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
  font-size: 0.9rem;
}

.notif-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 0.88rem;
  color: #555;
  line-height: 1.4;
}

.notif-text strong {
  color: #2c3e50;
}

.highlight-thread {
  color: #2577b1;
  font-weight: 500;
  cursor: pointer;
}

.highlight-thread:hover {
  text-decoration: underline;
}

.notif-link-block {
  display: inline;
  cursor: pointer;
}

.notif-link-block:hover .highlight-thread {
  text-decoration: underline !important;
}

.notif-label-tag {
  display: inline-block;
  padding: 1px 6px;
  font-size: 0.75rem;
  border-radius: 3px;
  color: #fff;
  margin: 0 4px;
  vertical-align: middle;
  line-height: 1.4;
  border: 1px solid transparent;
}

.notif-reaction-icon {
  width: 16px;
  height: 16px;
  vertical-align: middle;
  margin: 0 2px;
}

.notif-extra {
  display: block;
  font-size: 0.8rem;
  color: #888;
  margin-top: 2px;
}

.notif-time {
  font-size: 0.75rem;
  color: #888;
}

.notif-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #3498db;
  position: absolute;
  right: 15px;
  top: calc(50% - 4px);
}

.notif-empty {
  padding: 30px;
  text-align: center;
  color: #7f8c8d;
  font-size: 0.9rem;
}

.notif-footer {
  padding: 10px 15px;
  background: #f8f9fa;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-start;
  gap: 15px;
  align-items: center;
  font-size: 0.85rem;
}

.notif-footer a {
  color: #3498db;
  text-decoration: none;
  font-weight: 500;
}

.notif-footer a:hover {
  text-decoration: underline;
}

.btn-mark-all-read {
  background: none;
  border: none;
  color: #3498db;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
  outline: none;
}

.btn-mark-all-read:hover {
  text-decoration: underline;
}
.btn-clear-mail-header {
  margin-left: 10px;
  color: #e74c3c;
}
.btn-clear-mail-header:hover {
  text-decoration: underline;
}
.user-info-header {
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
  height: 100%;
}

.user-dropdown {
  position: absolute;
  top: 50px;
  right: 0;
  width: 150px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  z-index: 1000;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 5px 0;
}

.user-dropdown::before {
  content: '';
  position: absolute;
  top: -6px;
  right: 15px;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-bottom: 6px solid #fff;
}

/* Custom XenForo dropdown styling */
.user-dropdown.xamvn-dropdown {
  width: 320px;
  padding: 0;
  border: 1px solid #d8d8d8;
  border-top: 3px solid #1a507a;
  border-radius: 4px;
  box-shadow: 0 5px 25px rgba(0,0,0,0.2);
  display: flex;
  flex-direction: column;
  background: #fff;
}

.user-dropdown.xamvn-dropdown::before {
  border-bottom-color: #1a507a;
  right: 25px;
}

.xamvn-dropdown-tabs {
  display: flex;
  background-color: #fff;
  border-bottom: 1px solid #d8d8d8;
}

.xamvn-tab-btn {
  flex: 1;
  background: none;
  border: none;
  padding: 10px 0;
  font-size: 0.88rem;
  color: #555;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s ease;
  font-weight: 500;
  outline: none;
  font-family: inherit;
}

.xamvn-tab-btn:hover {
  color: #1a507a;
  background: #eef4f8;
}

.xamvn-tab-btn.active {
  color: #1a507a;
  background: linear-gradient(0deg, #edf6fd, #f6fafe);
  border-bottom: 2px solid #1a507a;
  font-weight: bold;
}

.xamvn-tab-content {
  padding: 15px;
  display: flex;
  flex-direction: column;
  padding-bottom: 0 !important;
}

.xamvn-user-brief {
  display: flex;
  gap: 15px;
  margin-top: -15px;
  margin-left: -15px;
  margin-right: -15px;
  padding: 15px;
  background-color: #f5f5f5;
  border-bottom: 1px solid #e5e5e5;
}

.xamvn-avatar-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
}

.xamvn-avatar-wrapper:hover .xamvn-avatar-edit-overlay {
  opacity: 1;
}

.xamvn-avatar-edit-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 28px;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}

.xamvn-avatar-edit-overlay span {
  color: #fff;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.xamvn-avatar-large {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.2rem;
  font-weight: bold;
  flex-shrink: 0;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.1);
  border: 1px solid rgba(0,0,0,0.05);
}

.xamvn-avatar-img {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
}

.user-avatar-img {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.xamvn-user-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  min-width: 0;
}

.xamvn-username {
  font-size: 1.15rem;
  font-weight: 700;
  color: #1a507a;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.xamvn-title {
  font-size: 14px;
  color: #141414;
  margin-bottom: 10px;
}

.xamvn-stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.xamvn-stat-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  line-height: 1.4;
}

.xamvn-stat-label, .xamvn-stat-value {
  color: #8c8c8c;
  font-size: 14px;
}

.xamvn-divider {
  border: 0;
  border-top: 1px solid #e5e5e5;
  margin: 0;
  width: 100%;
}

.xamvn-links-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  align-items: start;
}

.xamvn-link-item {
  font-size: 14px;
  color: #141414;
  text-decoration: none;
  padding: 4px 8px;
  border-radius: 3px;
  transition: all 0.15s;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-left: 0px !important;
}

.xamvn-link-item:hover {
  background: #f5f8fa;
  color: #e74c3c;
}

.xamvn-bookmarks-tab {
  padding: 30px 15px;
  align-items: center;
  justify-content: center;
}

.xamvn-empty-message {
  font-size: 0.85rem;
  color: #7f8c8d;
  text-align: center;
}

.xamvn-footer {
  padding: 6px 15px;
  display: flex;
  justify-content: flex-start;
}

.xamvn-logout-btn {
  background: none;
  border: none;
  color: #141414;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 0px;
  border-radius: 3px;
  transition: all 0.15s;
  font-family: inherit;
  width: 100%;
  text-align: left;
}

.xamvn-logout-btn:hover {
  background: #f5f8fa;
  color: #e74c3c;
}

.user-dropdown-item {
  background: none;
  border: none;
  color: #2c3e50;
  padding: 10px 15px;
  text-align: left;
  width: 100%;
  cursor: pointer;
  font-size: 0.90rem;
  transition: background 0.15s, color 0.15s;
  font-family: inherit;
}

.user-dropdown-item:hover {
  background: #f5f8fa;
  color: #e74c3c;
}

@media (max-width: 767px) {
  .user-dropdown.xamvn-dropdown {
    position: fixed !important;
    top: 50px !important;
    left: 16px !important;
    right: 16px !important;
    width: auto !important;
    z-index: 1001;
  }
}

.user-avatar-small {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: bold;
}

.user-greeting {
  color: white;
  font-size: 0.9rem;
}

.convo-title-link {
  display: block;
  color: #2577b1;
  font-weight: 500;
  cursor: pointer;
  font-size: 0.95rem;
  line-height: 1.3;
}

.convo-title-link:hover {
  text-decoration: underline;
}

.convo-participants {
  display: block;
  font-size: 0.85rem;
  color: #666;
  margin-top: 2px;
}

/* Mail Tabs and Pagination styling */
.mail-tabs {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fdfdfd;
  border-bottom: 1px solid #eee;
  padding: 0 15px;
}

.mail-tabs-left {
  display: flex;
}

.btn-mark-mail-read-header {
  background: none;
  border: none;
  color: #3498db;
  font-size: 0.8rem;
  cursor: pointer;
  padding: 0;
  font-weight: 500;
  transition: color 0.2s;
  font-family: inherit;
}

.btn-mark-mail-read-header:hover {
  color: #2980b9;
  text-decoration: underline;
}

.mail-tab-btn {
  background: none;
  border: none;
  padding: 10px 15px;
  font-size: 0.85rem;
  color: #7f8c8d;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  outline: none;
  font-weight: 500;
  font-family: inherit;
}

.mail-tab-btn:hover {
  color: #2980b9;
}

.mail-tab-btn.active {
  color: #2980b9;
  border-bottom-color: #3498db;
  font-weight: bold;
}

.btn-load-more.disabled {
  opacity: 0.5;
  cursor: default;
}

.notif-footer a.btn-load-more.disabled:hover {
  text-decoration: none;
}

@import "@/shared/assets/styles/custom.css";
/* Import mobile responsive styles */
@import "@/shared/assets/styles/responsive/mobile/header/header_mobile.css";
@import "@/shared/assets/styles/responsive/tablet/header/header_tablet.css";

/* Dropdown responsive positions */
@media (max-width: 1024px) {
  .notif-dropdown, .mail-dropdown {
    right: -60px !important;
  }
  .notif-dropdown::before {
    right: 72px !important;
  }
  .mail-dropdown::before {
    right: 120px !important;
  }
}

@media (max-width: 767px) {
  .notif-dropdown, .mail-dropdown {
    position: fixed !important;
    top: 50px !important;
    left: 16px !important;
    right: 16px !important;
    width: auto !important;
  }
  .notif-dropdown::before {
    right: 62px !important;
  }
  .mail-dropdown::before {
    right: 110px !important;
  }

  /* Prevent text wrapping and optimize spacing on mobile */
  .mail-tabs {
    padding: 0 8px !important;
    gap: 8px !important;
  }
  .mail-tab-btn {
    padding: 10px 8px !important;
    font-size: 0.8rem !important;
    white-space: nowrap !important;
  }
  .mail-tabs-actions {
    gap: 6px !important;
  }
  .btn-mark-all-read, .btn-mark-mail-read-header {
    font-size: 0.8rem !important;
    white-space: nowrap !important;
  }
  .btn-clear-mail-header {
    margin-left: 6px !important;
  }
  .notif-footer {
    padding: 10px 8px !important;
    gap: 8px !important;
    font-size: 0.78rem !important;
  }
  .notif-footer a, .notif-footer span {
    white-space: nowrap !important;
  }
}

/* Search feature styles */
.search-container {
  position: relative;
  display: flex;
  align-items: stretch;
}

.search-dropdown {
  position: absolute;
  top: 50px;
  right: 0;
  width: 280px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  z-index: 1000;
  padding: 10px;
  box-sizing: border-box;
}

/* Lịch sử tìm kiếm & Gợi ý từ khóa */
.search-history-dropdown {
  position: relative;
  margin-top: 8px;
  border-top: 1px solid #f1f5f9;
  padding-top: 6px;
  max-height: 240px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  cursor: pointer;
  border-radius: 4px;
  margin: 2px 0;
  transition: background-color 0.15s, color 0.15s;
  font-size: 0.9rem;
  color: #475569;
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
  margin-right: 8px;
}

.delete-history-btn {
  background: transparent;
  border: none;
  color: #94a3b8;
  font-size: 1.1rem;
  line-height: 1;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s, background-color 0.2s, color 0.2s;
}

@media (min-width: 768px) {
  .history-item:hover .delete-history-btn,
  .history-item.active .delete-history-btn {
    opacity: 1;
  }
}

.delete-history-btn:hover,
.delete-history-btn.focused-delete {
  background-color: #f1f5f9;
  color: #ef4444;
}


.search-dropdown::before {
  content: '';
  position: absolute;
  top: -6px;
  right: 25px;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-bottom: 6px solid #fff;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  padding: 4px 8px;
  background: #fdfdfd;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 0.88rem;
  background: transparent;
  color: #333;
  width: 100%;
  transition: font-size 0.15s ease;
}

.search-input.preview-selected {
  font-size: 0.74rem;
}

.btn-search-submit {
  background: none;
  border: none;
  color: #1a507a;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  outline: none;
  transition: color 0.2s;
}

.btn-search-submit:hover {
  color: #236395;
}

@media (max-width: 767px) {
  .search-dropdown {
    position: fixed !important;
    top: 50px !important;
    left: 16px !important;
    right: 16px !important;
    width: auto !important;
  }
  .search-dropdown::before {
    right: 20px !important;
  }
  /* Cho phép hiển thị dropdown lịch sử trên mobile */
  .search-history-dropdown {
    display: block !important;
  }
  /* Đảm bảo nút xóa lịch sử luôn hiển thị trên di động để chạm bấm */
  .delete-history-btn {
    opacity: 1 !important;
  }
}

</style>
