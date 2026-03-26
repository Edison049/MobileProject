package com.innovationai.myapplication.util;

import com.innovationai.myapplication.model.CartItem;
import com.innovationai.myapplication.model.Movie;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车管理类
 * 单例模式管理用户的购物车状态
 */
public class CartManager {
    private static CartManager instance;
    private List<CartItem> cartItems;

    // 私有构造函数
    private CartManager() {
        cartItems = new ArrayList<>();
    }

    /**
     * 获取购物车管理器单例实例
     * @return CartManager实例
     */
    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    /**
     * 向购物车添加电影
     * @param movie 要添加的电影
     * @return 添加成功返回true，如果电影已在购物车中则返回false
     */
    public boolean addToCart(Movie movie) {
        // 检查电影是否已经在购物车中
        for (CartItem item : cartItems) {
            if (item.getMovie().getId().equals(movie.getId())) {
                return false; // 电影已存在
            }
        }
        
        // 添加到购物车
        cartItems.add(new CartItem(movie, 1));
        return true;
    }

    /**
     * 从购物车移除电影
     * @param movieId 要移除的电影ID
     * @return 移除成功返回true，否则返回false
     */
    public boolean removeFromCart(String movieId) {
        for (int i = 0; i < cartItems.size(); i++) {
            if (cartItems.get(i).getMovie().getId().equals(movieId)) {
                cartItems.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * 清空购物车
     */
    public void clearCart() {
        cartItems.clear();
    }

    /**
     * 获取购物车中的所有项目
     * @return 购物车项目列表
     */
    public List<CartItem> getCartItems() {
        return new ArrayList<>(cartItems); // 返回副本以防止外部修改
    }

    /**
     * 获取购物车中商品总数
     * @return 商品数量
     */
    public int getItemCount() {
        return cartItems.size();
    }

    /**
     * 计算购物车总金额
     * @return 总金额（积分）
     */
    public int getTotalAmount() {
        int total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        return total;
    }

    /**
     * 检查购物车是否为空
     * @return true表示购物车为空，false表示不为空
     */
    public boolean isEmpty() {
        return cartItems.isEmpty();
    }

    /**
     * 检查指定电影是否在购物车中
     * @param movieId 电影ID
     * @return true表示在购物车中，false表示不在
     */
    public boolean isInCart(String movieId) {
        for (CartItem item : cartItems) {
            if (item.getMovie().getId().equals(movieId)) {
                return true;
            }
        }
        return false;
    }
}