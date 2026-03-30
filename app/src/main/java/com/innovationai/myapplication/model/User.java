package com.innovationai.myapplication.model;

import com.google.firebase.Timestamp;
import com.innovationai.myapplication.util.Constants;

/**
 * 用户模型类
 * 用于表示系统中的用户信息
 * 对应Firebase Firestore中的users集合
 */
public class User {
    private String uid;           // Firebase Auth生成的用户ID
    private String name;          // 用户姓名
    private int age;              // 用户年龄
    private String email;         // 用户邮箱
    private String passwordHash;  // 密码哈希
    private int credits;          // 用户积分余额
    private String role;          // 角色：admin / user
    private boolean active = true;// 是否有效
    private Timestamp createdAt;  // 账户创建时间

    // 无参构造函数（Firebase序列化需要）
    public User() {
    }

    // 完整构造函数
    public User(String uid, String name, int age, String email, String passwordHash,
                int credits, String role, boolean active, Timestamp createdAt) {
        this.uid = uid;
        this.name = name;
        this.age = age;
        this.email = email;
        this.passwordHash = passwordHash;
        this.credits = credits;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    // Getter和Setter方法
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equalsIgnoreCase(role);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", active=" + active +
                ", credits=" + credits +
                ", createdAt=" + createdAt +
                '}';
    }
}
