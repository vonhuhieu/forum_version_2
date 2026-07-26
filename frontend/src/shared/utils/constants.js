export const ROLES = {
  NON_OFFICIAL: 'ROLE_NON_OFFICIAL_USER',
  USER: 'ROLE_USER',
  ADMIN: 'ROLE_ADMIN',
  SUPER_ADMIN: 'ROLE_SUPER_ADMIN'
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

