package com.innovationai.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.data.AppRepository;
import com.innovationai.myapplication.model.Order;
import com.innovationai.myapplication.model.User;
import com.innovationai.myapplication.util.CartManager;
import com.innovationai.myapplication.util.Utils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

/**
 * 个人资料Activity
 * 显示用户详细信息和统计
 */
public class ProfileActivity extends AppCompatActivity {
    private final AppRepository repository = AppRepository.getInstance();

    // UI组件
    private ImageButton backButton;
    private ImageView userAvatar;
    private TextView userNameDisplay;
    private TextView userEmailDisplay;
    private TextView creditsBalance;
    private TextView cartCount;
    private TextView ordersCount;
    private TextView accountRoleText;
    private TextView dataModeText;
    private MaterialButton topUpButton;
    private MaterialButton adminPanelButton;
    private MaterialButton logoutButton;
    private User currentUser;

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
        accountRoleText = findViewById(R.id.account_role_text);
        dataModeText = findViewById(R.id.data_mode_text);
        topUpButton = findViewById(R.id.top_up_button);
        adminPanelButton = findViewById(R.id.admin_panel_button);
        logoutButton = findViewById(R.id.logout_button);
    }

    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        topUpButton.setOnClickListener(v -> showTopUpDialog());
        adminPanelButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, AdminPanelActivity.class);
            startActivity(intent);
        });
        logoutButton.setOnClickListener(v -> confirmLogout());
    }

    /**
     * 加载用户信息
     */
    private void loadUserInfo() {
        repository.loadCurrentUser(this, new AppRepository.DataCallback<>() {
            @Override
            public void onSuccess(User data) {
                currentUser = data;
                userNameDisplay.setText(data.getName());
                userEmailDisplay.setText(data.getEmail());
                creditsBalance.setText(String.valueOf(data.getCredits()));
                accountRoleText.setText(data.isAdmin() ? "Administrator" : "Standard user");
                dataModeText.setText("Current data mode: " + repository.getDataModeLabel(ProfileActivity.this));
                adminPanelButton.setVisibility(data.isAdmin() ? android.view.View.VISIBLE : android.view.View.GONE);

                CartManager cartManager = CartManager.getInstance();
                cartCount.setText(String.valueOf(cartManager.getItemCount()));
                loadOrderCount();
            }

            @Override
            public void onError(String errorMessage) {
                Utils.showToast(ProfileActivity.this, errorMessage);
            }
        });
    }

    private void loadOrderCount() {
        repository.loadOrders(this, new AppRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Order> data) {
                ordersCount.setText(String.valueOf(data.size()));
            }

            @Override
            public void onError(String errorMessage) {
                ordersCount.setText("0");
            }
        });
    }

    private void showTopUpDialog() {
        TextInputEditText amountInput = new TextInputEditText(this);
        amountInput.setHint("Enter the number of credits, for example 100");
        amountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("Top Up Credits")
                .setView(amountInput)
                .setMessage("Enter how many credits you want to add. Your balance will update automatically.")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String rawAmount = amountInput.getText() == null
                            ? ""
                            : amountInput.getText().toString().trim();
                    if (rawAmount.isEmpty()) {
                        Utils.showToast(ProfileActivity.this, "Please enter the amount of credits to add");
                        return;
                    }

                    int amount;
                    try {
                        amount = Integer.parseInt(rawAmount);
                    } catch (NumberFormatException e) {
                        Utils.showToast(ProfileActivity.this, "Please enter a valid number");
                        return;
                    }

                    if (amount <= 0) {
                        Utils.showToast(ProfileActivity.this, "Credits to add must be greater than 0");
                        return;
                    }

                    repository.topUpCredits(ProfileActivity.this, amount, new AppRepository.DataCallback<>() {
                        @Override
                        public void onSuccess(User data) {
                            currentUser = data;
                            creditsBalance.setText(String.valueOf(data.getCredits()));
                            Utils.showToast(ProfileActivity.this, "Top-up successful. Current credits: " + data.getCredits());
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Utils.showToast(ProfileActivity.this, errorMessage);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * 确认退出登录
     */
    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * 执行退出登录
     */
    private void performLogout() {
        repository.logout(this);
        
        // 清空购物车
        CartManager.getInstance().clearCart();
        
        // 显示退出成功提示
        Utils.showToast(this, "Signed out successfully");
        
        // 跳转到登录页面
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
