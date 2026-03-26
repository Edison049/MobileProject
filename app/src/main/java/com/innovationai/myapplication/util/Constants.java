package com.innovationai.myapplication.util;

/**
 * 常量类
 * 存储应用中使用的各种常量
 */
public class Constants {
    // Firebase Firestore集合名称
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_MOVIES = "movies";
    public static final String COLLECTION_ORDERS = "orders";

    // SharedPreferences键名
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_CREDITS = "user_credits";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";

    // Intent传递数据的键名
    public static final String EXTRA_MOVIE_ID = "movie_id";
    public static final String EXTRA_MOVIE_TITLE = "movie_title";
    public static final String EXTRA_USER_ID = "user_id";

    // 默认用户积分
    public static final int DEFAULT_USER_CREDITS = 1000;

    // 测试账户信息（学校要求的固定账户）
    public static final String TEST_USER_MARY_EMAIL = "mary@example.com";
    public static final String TEST_USER_MARY_PASSWORD = "mary123";
    public static final String TEST_USER_MARY_NAME = "mary";
    
    public static final String TEST_USER_JOHN_EMAIL = "john@example.com";
    public static final String TEST_USER_JOHN_PASSWORD = "john123";
    public static final String TEST_USER_JOHN_NAME = "john";

    // 网络请求超时时间（毫秒）
    public static final int NETWORK_TIMEOUT = 10000;

    // 分页大小
    public static final int PAGE_SIZE = 20;

    // 视频预览相关常量
    public static final String VIDEO_PREVIEW_CONTROLLER_SHOW_TIMEOUT_MS = "video_preview_show_timeout_ms";
}