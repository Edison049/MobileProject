package com.innovationai.myapplication.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.adapter.CartAdapter;
import com.innovationai.myapplication.model.CartItem;
import com.innovationai.myapplication.model.User;
import com.innovationai.myapplication.util.CartManager;
import com.innovationai.myapplication.util.TempAuthUtil;
import com.innovationai.myapplication.util.Utils;
import java.util.List;

/**
 * 购物车Activity
 * 管理用户购物车中的电影项目
 */
public class CartActivity extends AppCompatActivity {

    // UI组件
    private ImageButton backButton;
    private RecyclerView cartItemsRecycler;
    private LinearLayout emptyCartLayout;
    private LinearLayout checkoutLayout;
    private TextView totalAmountText;
    private com.google.android.material.button.MaterialButton checkoutButton;

    // 适配器和数据
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // 初始化视图
        initViews();
        
        // 设置点击监听器
        setupClickListeners();
        
        // 加载购物车数据
        loadCartData();
        
        // 加载用户信息
        loadUserInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到页面时刷新购物车数据
        loadCartData();
        loadUserInfo();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        backButton = findViewById(R.id.back_button);
        cartItemsRecycler = findViewById(R.id.cart_items_recycler);
        emptyCartLayout = findViewById(R.id.empty_cart_layout);
        checkoutLayout = findViewById(R.id.checkout_layout);
        totalAmountText = findViewById(R.id.total_amount_text);
        checkoutButton = findViewById(R.id.checkout_button);

        // 设置RecyclerView
        cartItemsRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        checkoutButton.setOnClickListener(v -> processCheckout());
    }

    /**
     * 加载购物车数据
     */
    private void loadCartData() {
        CartManager cartManager = CartManager.getInstance();
        cartItems = cartManager.getCartItems();
        
        // 更新适配器
        if (cartAdapter == null) {
            cartAdapter = new CartAdapter(this, cartItems);
            cartAdapter.setOnCartItemClickListener(new CartAdapter.OnCartItemClickListener() {
                @Override
                public void onRemoveItemClick(CartItem item) {
                    // 项目被移除时的回调
                }

                @Override
                public void onCartUpdated() {
                    updateCartDisplay();
                }
            });
            cartItemsRecycler.setAdapter(cartAdapter);
        } else {
            cartAdapter.updateCartItems(cartItems);
        }
        
        // 更新UI显示
        updateCartDisplay();
    }

    /**
     * 更新购物车显示
     */
    private void updateCartDisplay() {
        CartManager cartManager = CartManager.getInstance();
        
        if (cartManager.isEmpty()) {
            // 购物车为空
            emptyCartLayout.setVisibility(View.VISIBLE);
            checkoutLayout.setVisibility(View.GONE);
            cartItemsRecycler.setVisibility(View.GONE);
        } else {
            // 购物车有项目
            emptyCartLayout.setVisibility(View.GONE);
            checkoutLayout.setVisibility(View.VISIBLE);
            cartItemsRecycler.setVisibility(View.VISIBLE);
            
            // 更新总金额显示
            int totalAmount = cartManager.getTotalAmount();
            totalAmountText.setText(totalAmount + "积分");
        }
    }

    /**
     * 加载用户信息
     */
    private void loadUserInfo() {
        String userName = TempAuthUtil.getCurrentUserName(this);
        int credits = TempAuthUtil.getCurrentUserCredits(this);
        
        if (!userName.isEmpty()) {
            currentUser = new User();
            currentUser.setName(userName);
            currentUser.setCredits(credits);
        }
    }

    /**
     * 处理结算逻辑
     */
    private void processCheckout() {
        if (cartItems == null || cartItems.isEmpty()) {
            Utils.showToast(this, "购物车为空");
            return;
        }

        if (currentUser == null) {
            Utils.showToast(this, "请先登录");
            return;
        }

        int totalAmount = CartManager.getInstance().getTotalAmount();
        
        // 检查积分是否足够
        if (currentUser.getCredits() < totalAmount) {
            Utils.showToast(this, "积分不足，无法完成购买");
            return;
        }

        // 显示确认对话框
        new AlertDialog.Builder(this)
                .setTitle("确认结算")
                .setMessage("确定要花费 " + totalAmount + " 积分购买这 " + 
                           cartItems.size() + " 部电影吗？")
                .setPositiveButton("确认购买", (dialog, which) -> completePurchase())
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 完成购买
     */
    private void completePurchase() {
        if (currentUser == null || cartItems == null) return;

        int totalAmount = CartManager.getInstance().getTotalAmount();
        
        // 扣除积分
        int newCredits = currentUser.getCredits() - totalAmount;
        TempAuthUtil.updateUserCredits(this, newCredits);
        
        // 清空购物车
        CartManager.getInstance().clearCart();
        
        // 更新用户对象
        currentUser.setCredits(newCredits);
        
        // 显示成功消息
        Utils.showToast(this, "购买成功！剩余积分: " + newCredits);
        
        // 刷新显示
        loadCartData();
        
        // 返回主菜单
        finish();
    }
}