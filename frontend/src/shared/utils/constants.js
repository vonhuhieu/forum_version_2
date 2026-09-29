export const ROLES = {
  NON_OFFICIAL: 'ROLE_NON_OFFICIAL_USER',
  USER: 'ROLE_USER',
  ADMIN: 'ROLE_ADMIN',
  SUPER_ADMIN: 'ROLE_SUPER_ADMIN'
};

export const ROLE_BADGE_CLASSES = {
  [ROLES.SUPER_ADMIN]: 'badge-danger',
  [ROLES.ADMIN]: 'badge-warning',
  [ROLES.USER]: 'badge-success',
  [ROLES.NON_OFFICIAL]: 'badge-secondary',
  DEFAULT: 'badge-light'
};

export const ROLE_NAMES = {
  [ROLES.SUPER_ADMIN]: 'Super Admin',
  [ROLES.ADMIN]: 'Admin',
  [ROLES.USER]: 'Thành viên',
  [ROLES.NON_OFFICIAL]: 'Chưa chính thức'
};

export const THREAD_SCOPES = {
  PUBLIC: 'PUBLIC',
  INTERNAL: 'INTERNAL'
};

export const FORGOT_PASSWORD_STEPS = {
  ENTER_CREDENTIALS: 1, // Bước 1: Nhập tên đăng nhập + email
  RESET_PASSWORD: 2     // Bước 2: Nhập mã xác nhận + mật khẩu mới
};

export const SETTINGS = {
  THREAD_EDIT_LIMIT_MINUTES_KEY: 'thread_edit_limit_minutes',
  DEFAULT_THREAD_EDIT_LIMIT_MINUTES: 15,
  POST_EDIT_LIMIT_MINUTES_KEY: 'post_edit_limit_minutes',
  DEFAULT_POST_EDIT_LIMIT_MINUTES: 15,
  CONVERSATION_EDIT_LIMIT_MINUTES_KEY: 'conversation_edit_limit_minutes',
  DEFAULT_CONVERSATION_EDIT_LIMIT_MINUTES: 15,
  CONVERSATION_REPLY_EDIT_LIMIT_MINUTES_KEY: 'conversation_reply_edit_limit_minutes',
  DEFAULT_CONVERSATION_REPLY_EDIT_LIMIT_MINUTES: 15,
  NO_LIMIT_VALUE: -1
};

export const MEMBER_KEYS = {
  MOST_MESSAGES: 'most_messages',
  MOST_REACTIONS: 'most_reactions',
  MOST_POINTS: 'most_points'
};

export const TITLE_TYPES = {
  UNVERIFIED_DEFAULT: 'UNVERIFIED_DEFAULT',
  POINT_BASED: 'POINT_BASED',
  CUSTOM_ASSIGNABLE: 'CUSTOM_ASSIGNABLE'
};

export const NEWS_TABS = {
  ALL: 'all',
  TECH: 'tech',
  FINANCE: 'finance',
  SOCIETY: 'society'
};

export const NEWS_LABEL_NAME = 'Điểm Tin';

export const NOTIFICATION_TYPES = {
  REACTION: 'REACTION',
  QUOTE: 'QUOTE',
  MENTION: 'MENTION',
  FOLLOWED_USER_THREAD: 'FOLLOWED_USER_THREAD',
  FOLLOWED_USER_POST: 'FOLLOWED_USER_POST',
  SYSTEM: 'SYSTEM'
};

export const NOTIFICATION_TAB_KEYS = {
  ALL: 'all',
  UNREAD: 'unread'
};

export const NOTIFICATION_LABEL_STYLES = {
  MENTION: {
    backgroundColor: '#2577b1',
    color: '#ffffff',
    borderColor: 'transparent'
  },
  DEFAULT: {
    backgroundColor: '#95a5a6',
    color: '#ffffff',
    borderColor: 'transparent'
  },
  DEFAULT_REACTION_COLOR: '#2c3e50'
};

export const NOTIFICATION_TEXTS = {
  EXTRA_THREAD_POSTS: 'Có thể có bài viết thêm trong chủ đề',
  NO_UNREAD: 'Không có thông báo chưa đọc nào.',
  NO_NEW: 'Không có thông báo nào mới.'
};
