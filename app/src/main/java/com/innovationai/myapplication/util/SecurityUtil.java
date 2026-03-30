package com.innovationai.myapplication.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 简单密码哈希工具
 * 项目演示中避免把原始密码直接写入数据库
 */
public final class SecurityUtil {
    private SecurityUtil() {
    }

    public static String hashPassword(String password) {
        if (password == null) {
            return "";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashBytes.length * 2);
            for (byte hashByte : hashBytes) {
                builder.append(String.format("%02x", hashByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static boolean matches(String rawPassword, String hashedPassword) {
        return hashPassword(rawPassword).equals(hashedPassword);
    }
}
