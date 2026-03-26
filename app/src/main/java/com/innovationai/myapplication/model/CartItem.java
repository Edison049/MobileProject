package com.innovationai.myapplication.model;

/**
 * 购物车项目模型类
 * 用于表示用户购物车中的电影项目
 */
public class CartItem {
    private Movie movie;    // 电影对象
    private int quantity;   // 数量（通常为1）

    // 无参构造函数
    public CartItem() {
    }

    // 构造函数
    public CartItem(Movie movie, int quantity) {
        this.movie = movie;
        this.quantity = quantity;
    }

    // Getter和Setter方法
    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // 计算单项总价
    public int getTotalPrice() {
        return movie.getPrice() * quantity;
    }

    @Override
    public String toString() {
        return "CartItem{" +
                "movie=" + movie.getTitle() +
                ", quantity=" + quantity +
                ", totalPrice=" + getTotalPrice() +
                '}';
    }
}