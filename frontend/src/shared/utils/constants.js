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

export const BREADCRUMB_HOME_TITLES = ['Trang chủ', 'Home'];

export const DISPLAY_NAME_MAX_LENGTH = 25;

export const LAB_ROUTES = {
  HOME: '/phong-thi-nghiem',
  MY_PRODUCTS: '/lab/san-pham',
  PLAYER_PREFIX: '/lab/p/',
  LAB_PREFIX: '/lab'
};

export const LAB_TEMPLATES = {
  LUCKY_WHEEL: 'LUCKY_WHEEL',
  FLASHCARD: 'FLASHCARD',
  MILLIONAIRE: 'MILLIONAIRE'
};

export const LAB_MESSAGE_ROLES = {
  USER: 'user',
  ASSISTANT: 'assistant'
};

export const LAB_SSE_EVENTS = {
  MESSAGE: 'message',
  SESSION: 'session',
  PRODUCT: 'product',
  ERROR: 'error',
  DONE: 'done',
  HEARTBEAT: 'heartbeat'
};

export const LAB_BOT = {
  NAME: 'Nhà thông thái',
  AVATAR: '🧙‍♂️',
  GREETING: 'Chào bạn! Ta là Nhà thông thái, hãy kể cho ta nghe bạn muốn tạo sản phẩm gì nhé.'
};

export const LAB_QUICK_PROMPTS = [
  { icon: '🏆', label: 'Ai là triệu phú', prompt: 'Làm cho tôi game Ai là triệu phú về kiến thức chung' },
  { icon: '🎡', label: 'Vòng quay may mắn', prompt: 'Tạo vòng quay may mắn bốc thăm phần thưởng' },
  { icon: '🗂', label: 'Flashcard', prompt: 'Tạo bộ thẻ flashcard ôn tập kiến thức' }
];

export const LAB_TEMPLATE_META = {
  [LAB_TEMPLATES.LUCKY_WHEEL]: { name: 'Vòng quay may mắn', icon: '🎡' },
  [LAB_TEMPLATES.FLASHCARD]: { name: 'Thẻ ghi nhớ', icon: '🗂' },
  [LAB_TEMPLATES.MILLIONAIRE]: { name: 'Ai là triệu phú', icon: '🏆' }
};

export const LAB_WHEEL_DEFAULT_COLORS = [
  '#f59e0b', '#3b82f6', '#10b981', '#ec4899', '#8b5cf6', '#ef4444', '#14b8a6', '#64748b'
];

export const LAB_MILLIONAIRE = {
  ANSWER_KEYS: ['A', 'B', 'C', 'D'],
  DEFAULT_TIMER_SECONDS: 30
};

export const LAB_TEXTS = {
  GUEST_LOGIN_HINT: 'Đăng nhập thành viên chính thức để lưu sản phẩm và lịch sử trò chuyện.',
  STREAM_ERROR: 'Đường truyền bị gián đoạn, bạn thử lại sau giây lát nhé.',
  PRODUCT_NOT_FOUND: 'Sản phẩm không tồn tại hoặc đã hết hạn.',
  INPUT_PLACEHOLDER: 'Nhập yêu cầu của bạn, ví dụ: tạo vòng quay chọn người trả bài...'
};

export const CATEGORY_LABEL_MODES = {
  ALL: 'ALL',
  CUSTOM: 'CUSTOM',
  NONE: 'NONE'
};

export const UPLOAD_MODES = {
  AVATAR: 'avatar',
  BANNER: 'banner'
};

export const LIGHTBOX_ZOOM = {
  MIN: 0.5,
  MAX: 4.0,
  DEFAULT: 1.0,
  STEP: 0.15,
  PRESETS: [
    { label: '50%', value: 0.5 },
    { label: '100% (Gốc)', value: 1.0 },
    { label: '150%', value: 1.5 },
    { label: '200%', value: 2.0 },
    { label: '300%', value: 3.0 },
    { label: '400%', value: 4.0 }
  ]
};

