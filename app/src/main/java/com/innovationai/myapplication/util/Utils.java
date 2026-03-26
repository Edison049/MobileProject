package com.innovationai.myapplication.util;

import android.content.Context;
import android.widget.Toast;

/**
 * 通用工具类
 * 包含各种常用的工具方法
 */
public class Utils {
    
    /**
     * 显示短时间Toast消息
     * @param context 上下文
     * @param message 消息内容
     */
    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 显示长时间Toast消息
     * @param context 上下文
     * @param message 消息内容
     */
    public static void showLongToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * 验证邮箱格式
     * @param email 邮箱地址
     * @return true表示格式正确，false表示格式错误
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
    /**
     * 验证密码强度
     * @param password 密码
     * @return true表示密码符合要求，false表示不符合
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        // 密码至少6位
        return password.length() >= 6;
    }
    
    /**
     * 验证姓名
     * @param name 姓名
     * @return true表示姓名有效，false表示无效
     */
    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        // 姓名至少2个字符
        return name.length() >= 2;
    }
    
    /**
     * 验证年龄
     * @param age 年龄
     * @return true表示年龄有效，false表示无效
     */
    public static boolean isValidAge(int age) {
        return age >= 13 && age <= 120; // 最小13岁，最大120岁
    }
}