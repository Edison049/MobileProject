package com.innovationai.myapplication.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 临时认证工具类
 * 在Firebase配置完成前使用本地存储模拟认证功能
 */
public class TempAuthUtil {
    private static final String PREF_NAME = "temp_auth_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_CREDITS = "user_credits";
    
    private static SharedPreferences prefs;
    
    private static void initPrefs(Context context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }
    
    /**
     * 模拟登录
     */
    public static boolean login(Context context, String email, String password) {
        initPrefs(context);
        
        // 简单的测试账户验证
        if ((email.equals("mary@example.com") && password.equals("mary123")) ||
            (email.equals("john@example.com") && password.equals("john123"))) {
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_IS_LOGGED_IN, true);
            editor.putString(KEY_USER_EMAIL, email);
            editor.putString(KEY_USER_NAME, email.contains("mary") ? "mary" : "john");
            editor.putInt(KEY_USER_CREDITS, 1000);
            editor.apply();
            
            return true;
        }
        
        return false;
    }
    
    /**
     * 模拟注册
     */
    public static boolean register(Context context, String name, int age, String email, String password) {
        initPrefs(context);
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putInt(KEY_USER_CREDITS, 1000);
        editor.apply();
        
        return true;
    }
    
    /**
     * 检查是否已登录
     */
    public static boolean isUserLoggedIn(Context context) {
        initPrefs(context);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    /**
     * 获取当前用户名
     */
    public static String getCurrentUserName(Context context) {
        initPrefs(context);
        return prefs.getString(KEY_USER_NAME, "");
    }
    
    /**
     * 获取当前用户积分
     */
    public static int getCurrentUserCredits(Context context) {
        initPrefs(context);
        return prefs.getInt(KEY_USER_CREDITS, 0);
    }
    
    /**
     * 获取当前用户邮箱
     */
    public static String getCurrentUserEmail(Context context) {
        initPrefs(context);
        return prefs.getString(KEY_USER_EMAIL, "");
    }
    
    /**
     * 退出登录
     */
    public static void logout(Context context) {
        initPrefs(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
    
    /**
     * 更新用户积分
     */
    public static void updateUserCredits(Context context, int newCredits) {
        initPrefs(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_USER_CREDITS, newCredits);
        editor.apply();
    }
}