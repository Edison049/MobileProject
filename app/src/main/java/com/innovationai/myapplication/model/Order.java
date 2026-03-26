package com.innovationai.myapplication.model;

import com.google.firebase.Timestamp;
import java.util.List;

/**
 * 订单模型类
 * 用于表示用户的购买订单
 * 对应Firebase Firestore中的orders集合
 */
public class Order {
    private String orderId;             // 订单ID (Firestore文档ID)
    private String userId;              // 用户ID
    private List<CartItem> movies;      // 购买的电影列表
    private int totalAmount;            // 总金额
    private Timestamp timestamp;        // 订单时间
    private String status;              // 订单状态

    // 无参构造函数（Firebase序列化需要）
    public Order() {
    }

    // 完整构造函数
    public Order(String orderId, String userId, List<CartItem> movies, 
                 int totalAmount, Timestamp timestamp, String status) {
        this.orderId = orderId;
        this.userId = userId;
        this.movies = movies;
        this.totalAmount = totalAmount;
        this.timestamp = timestamp;
        this.status = status;
    }

    // Getter和Setter方法
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<CartItem> getMovies() {
        return movies;
    }

    public void setMovies(List<CartItem> movies) {
        this.movies = movies;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", moviesCount=" + (movies != null ? movies.size() : 0) +
                ", totalAmount=" + totalAmount +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                '}';
    }
}