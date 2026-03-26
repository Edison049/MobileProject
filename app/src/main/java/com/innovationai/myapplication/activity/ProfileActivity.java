package com.innovationai.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.util.CartManager;
import com.innovationai.myapplication.util.TempAuthUtil;
import com.innovationai.myapplication.util.Utils;
import com.google.android.material.button.MaterialButton;

/**
 * 个人资料Activity
 * 显示用户详细信息和统计
 */
public class ProfileActivity extends AppCompatActivity {

    // UI组件
    private ImageButton backButton;
    private ImageView userAvatar;
    private TextView userNameDisplay;
    private TextView userEmailDisplay;
    private TextView creditsBalance;
    private TextView cartCount;
    private TextView ordersCount;
    private MaterialButton logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 初始化视图
        initViews();
        
        // 设置点击监听器
        setupClickListeners();
        
        // 加载用户信息
        loadUserInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到页面时刷新数据
        loadUserInfo();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        backButton = findViewById(R.id.back_button);
        userAvatar = findViewById(R.id.user_avatar);
        userNameDisplay = findViewById(R.id.user_name_display);
        userEmailDisplay = findViewById(R.id.user_email_display);
        creditsBalance = findViewById(R.id.credits_balance);
        cartCount = findViewById(R.id.cart_count);
        ordersCount = findViewById(R.id.orders_count);
        logoutButton = findViewById(R.id.logout_button);
    }

    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        logoutButton.setOnClickListener(v -> confirmLogout());
    }

    /**
     * 加载用户信息
     */
    private void loadUserInfo() {
        String userName = TempAuthUtil.getCurrentUserName(this);
        String userEmail = TempAuthUtil.getCurrentUserEmail(this); // 假设有这个方法
        int credits = TempAuthUtil.getCurrentUserCredits(this);
        
        // 显示用户基本信息
        userNameDisplay.setText(userName);
        userEmailDisplay.setText(userEmail != null ? userEmail : userName + "@example.com");
        creditsBalance.setText(String.valueOf(credits));
        
        // 显示统计数据
        CartManager cartManager = CartManager.getInstance();
        cartCount.setText(String.valueOf(cartManager.getItemCount()));
        
        // 订单数量（简单计数）
        ordersCount.setText("0"); // TODO: 实际订单数量统计
        
        // 设置用户头像（简单处理）
        // 实际应用中可以从Firebase Storage加载用户头像
    }

    /**
     * 确认退出登录
     */
    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("确认退出")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("退出", (dialog, which) -> performLogout())
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 执行退出登录
     */
    private void performLogout() {
        // 清除用户数据
        TempAuthUtil.logout(this);
        
        // 清空购物车
        CartManager.getInstance().clearCart();
        
        // 显示退出成功提示
        Utils.showToast(this, "已退出登录");
        
        // 跳转到登录页面
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}