package com.forum.utils;

public class Constants {
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_NON_OFFICIAL_USER = "ROLE_NON_OFFICIAL_USER";
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    public static final String THREAD_SCOPE_PUBLIC = "PUBLIC";
    public static final String THREAD_SCOPE_INTERNAL = "INTERNAL";

    public static final String THREAD_TYPE_DISCUSSION = "discussion";
    public static final String THREAD_TYPE_POLL = "poll";

    public static final String SETTING_THREAD_EDIT_LIMIT_MINUTES = "thread_edit_limit_minutes";
    public static final String DEFAULT_THREAD_EDIT_LIMIT_MINUTES = "15";

    public static final String SETTING_POST_EDIT_LIMIT_MINUTES = "post_edit_limit_minutes";
    public static final String DEFAULT_POST_EDIT_LIMIT_MINUTES = "15";

    public static final String SETTING_CONVERSATION_EDIT_LIMIT_MINUTES = "conversation_edit_limit_minutes";
    public static final String DEFAULT_CONVERSATION_EDIT_LIMIT_MINUTES = "15";
    public static final String SETTING_CONVERSATION_REPLY_EDIT_LIMIT_MINUTES = "conversation_reply_edit_limit_minutes";
    public static final String DEFAULT_CONVERSATION_REPLY_EDIT_LIMIT_MINUTES = "15";

    public static final String MEMBER_KEY_MOST_MESSAGES = "most_messages";
    public static final String MEMBER_KEY_MOST_REACTIONS = "most_reactions";
    public static final String MEMBER_KEY_MOST_POINTS = "most_points";

    public static final String SETTING_POST_BUTTON_HOME = "post_button_home";
    public static final String SETTING_POST_BUTTON_LATEST = "post_button_latest";
    public static final String SETTING_POST_BUTTON_PINNED = "post_button_pinned";
    public static final String SETTING_POST_BUTTON_CATEGORY = "post_button_category";

    public static final String DEFAULT_POST_BUTTON_HOME = "ALL";
    public static final String DEFAULT_POST_BUTTON_LATEST = "ALL";
    public static final String DEFAULT_POST_BUTTON_PINNED = "ADMIN_ONLY";
    public static final String DEFAULT_POST_BUTTON_CATEGORY = "ALL";

    // Spring Security PreAuthorize Expressions
    public static final String PRE_AUTH_ADMIN_OR_SUPER_ADMIN = "hasAnyRole('ADMIN', 'SUPER_ADMIN')";

    // News Hub & Bot Constants
    public static final String NEWS_LABEL_NAME = "Điểm Tin";
    public static final String NEWS_LABEL_COLOR = "#1a73e8";
    public static final String NEWS_LABEL_TEXT_COLOR = "#ffffff";
    public static final String NEWS_LABEL_BORDER_COLOR = "transparent";

    public static final String NEWS_BOT_USERNAME = "diemtinbot";
    public static final String NEWS_BOT_DISPLAY_NAME = "Điểm Tin Bot 🤖";
    public static final String NEWS_BOT_EMAIL = "diemtinbot@hoptacxavuive.com";
    public static final String NEWS_BOT_TITLE_NAME = "Biên Tập Viên Tin Tức";
    public static final String NEWS_BOT_TITLE_DESCRIPTION = "Biên tập viên tổng hợp tin tức chính thống";
    public static final String NEWS_BOT_AVATAR_BASE_URL = "https://api.dicebear.com/7.x/bottts/svg?seed=";

    public static final String MENU_NEWS_TITLE = "Điểm tin";
    public static final String MENU_NEWS_URL = "/diem-tin";
    public static final String MENU_NEWS_OLD_URL = "/leu-bao";
    public static final Long MENU_NEWS_ID = 2L;
    public static final int MENU_NEWS_ORDER = 2;

    // News Tab Keys
    public static final String NEWS_TAB_ALL = "all";
    public static final String NEWS_TAB_TECH = "tech";
    public static final String NEWS_TAB_FINANCE = "finance";
    public static final String NEWS_TAB_SOCIETY = "society";

    // Category IDs for News
    public static final Long CATEGORY_AI_PROGRAMMING_ID = 36L;      // Trí Tuệ Nhân Tạo & Lập Trình
    public static final Long CATEGORY_INVESTMENT_FINANCE_ID = 32L;  // Đầu Tư & Dòng Tiền
    public static final Long CATEGORY_SIDEWALK_TEA_ID = 29L;        // Trà Đá Vỉa Hè
    public static final Long CATEGORY_DIGITAL_DEVICES_ID = 35L;     // Thiết Bị Số

    // System Settings for News Bot & Moderation
    public static final String SETTING_NEWS_BOT_ENABLED = "news_bot_enabled";
    public static final String DEFAULT_NEWS_BOT_ENABLED = "true";
    public static final String SETTING_NEWS_BOT_CRON = "news_bot_cron";
    public static final String DEFAULT_NEWS_BOT_CRON = "0 0 7,12,18 * * *";
    public static final String SETTING_NEWS_BOT_MAX_DAILY_POSTS = "news_bot_max_daily_posts";
    public static final String DEFAULT_NEWS_BOT_MAX_DAILY_POSTS = "8";

    public static final String SETTING_PROFANITY_FILTER_ENABLED = "profanity_filter_enabled";
    public static final String DEFAULT_PROFANITY_FILTER_ENABLED = "true";
    public static final String SETTING_PROFANITY_CUSTOM_KEYWORDS = "profanity_custom_keywords";
    public static final String DEFAULT_PROFANITY_CUSTOM_KEYWORDS = "";
}
