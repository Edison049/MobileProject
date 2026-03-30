package com.innovationai.myapplication.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 当前登录会话管理器
 * 只负责保存当前登录用户ID，不负责业务数据存储
 */
public final class SessionManager {
    private static final String PREF_NAME = "movie_app_session";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";

    private SessionManager() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveCurrentUserId(Context context, String userId) {
        getPrefs(context).edit().putString(KEY_CURRENT_USER_ID, userId).apply();
    }

    public static String getCurrentUserId(Context context) {
        return getPrefs(context).getString(KEY_CURRENT_USER_ID, null);
    }

    public static boolean isLoggedIn(Context context) {
        return getCurrentUserId(context) != null;
    }

    public static void clearSession(Context context) {
        getPrefs(context).edit().remove(KEY_CURRENT_USER_ID).apply();
    }
}
