package com.forum.lab.utils;

public class Constants {

    // Roles (matching forum backend)
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String ROLE_NON_OFFICIAL_USER = "ROLE_NON_OFFICIAL_USER";

    // Spring Security PreAuthorize Expressions
    public static final String PRE_AUTH_AUTHENTICATED = "isAuthenticated()";
    public static final String PRE_AUTH_USER_OR_ADMIN = "hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')";
    public static final String PRE_AUTH_ADMIN_OR_SUPER_ADMIN = "hasAnyRole('ADMIN', 'SUPER_ADMIN')";

    // Templates
    public static final String TEMPLATE_LUCKY_WHEEL = "LUCKY_WHEEL";
    public static final String TEMPLATE_FLASHCARD = "FLASHCARD";
    public static final String TEMPLATE_MILLIONAIRE = "MILLIONAIRE";

    // Chat Message Roles
    public static final String MESSAGE_ROLE_USER = "user";
    public static final String MESSAGE_ROLE_ASSISTANT = "assistant";
    public static final String MESSAGE_ROLE_SYSTEM = "system";

    // SSE Event Names
    public static final String SSE_EVENT_MESSAGE = "message";
    public static final String SSE_EVENT_SESSION = "session";
    public static final String SSE_EVENT_PRODUCT = "product";
    public static final String SSE_EVENT_ERROR = "error";
    public static final String SSE_EVENT_DONE = "done";
    public static final String SSE_EVENT_HEARTBEAT = "heartbeat";

    // Column Data Types
    public static final String COLUMN_TYPE_TEXT = "text";
    public static final String COLUMN_TYPE_NUMBER = "number";
    public static final String COLUMN_TYPE_IMAGE = "image";
    public static final String COLUMN_TYPE_BOOLEAN = "boolean";

    // Column Alignments
    public static final String COLUMN_ALIGN_LEFT = "left";
    public static final String COLUMN_ALIGN_CENTER = "center";
    public static final String COLUMN_ALIGN_RIGHT = "right";

    // Default Quotas
    public static final int DEFAULT_GUEST_DAILY_LIMIT = 5;
    public static final int DEFAULT_USER_DAILY_LIMIT = 30;
    public static final int DEFAULT_MAX_PRODUCTS_PER_USER = 30;
    public static final int DEFAULT_MAX_ROWS_PER_PRODUCT = 500;

    // Bot Identity & Labels
    public static final String BOT_NAME = "Nhà thông thái";
    public static final String GUEST_USERNAME_PREFIX = "guest_";

    // Menu and Paths
    public static final String MENU_LAB_TITLE = "Phòng thí nghiệm";
    public static final String MENU_LAB_URL = "/phong-thi-nghiem";
    public static final String MENU_LAB_ICON = "bi-boxes";
}
