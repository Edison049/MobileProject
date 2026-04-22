package com.innovationai.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.data.AppRepository;
import com.innovationai.myapplication.util.Utils;

/**
 * 登录Activity
 * 处理用户登录逻辑
 * 登录输入框同时支持用户名和邮箱
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private Button loginButton;
    private TextView registerLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化视图组件
        initViews();
        
        // 设置点击事件监听器
        setupClickListeners();
        
        // 检查是否已经登录
        checkIfAlreadyLoggedIn();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        emailInputLayout = findViewById(R.id.email_input_layout);
        passwordInputLayout = findViewById(R.id.password_input_layout);
        loginButton = findViewById(R.id.login_button);
        registerLink = findViewById(R.id.register_link);
    }

    /**
     * 设置点击事件监听器
     */
    private void setupClickListeners() {
        // 登录按钮点击事件
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        // 注册链接点击事件
        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到注册页面
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish(); // 关闭当前Activity
            }
        });
    }

    /**
     * 检查用户是否已经登录
     */
    private void checkIfAlreadyLoggedIn() {
        if (AppRepository.getInstance().isLoggedIn(this)) {
            // 用户已登录，直接跳转到主菜单
            navigateToMainMenu();
        }
    }

    /**
     * 尝试登录
     */
    private void attemptLogin() {
        // 获取输入的邮箱和密码
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // 验证输入
        if (!validateInput(email, password)) {
            return;
        }

        // 显示加载状态
        loginButton.setEnabled(false);
        loginButton.setText("Signing in...");

        AppRepository.getInstance().login(this, email, password, new AppRepository.DataCallback<>() {
            @Override
            public void onSuccess(com.innovationai.myapplication.model.User data) {
                Utils.showToast(LoginActivity.this, "Signed in successfully. Welcome, " + data.getName());
                navigateToMainMenu();
            }

            @Override
            public void onError(String errorMessage) {
                Utils.showToast(LoginActivity.this, errorMessage);
                resetLoginButton();
            }
        });
    }

    /**
     * 验证用户输入
     * @param email 邮箱
     * @param password 密码
     * @return true表示验证通过，false表示验证失败
     */
    private boolean validateInput(String email, String password) {
        boolean isValid = true;

        // 验证账号
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Please enter your username or email");
            isValid = false;
        } else {
            emailInputLayout.setError(null);
        }

        // 验证密码
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Please enter your password");
            isValid = false;
        } else if (!Utils.isValidPassword(password)) {
            passwordInputLayout.setError("Password must be at least 6 characters");
            isValid = false;
        } else {
            passwordInputLayout.setError(null);
        }

        return isValid;
    }

    /**
     * 重置登录按钮状态
     */
    private void resetLoginButton() {
        loginButton.setEnabled(true);
        loginButton.setText("Sign In");
    }

    /**
     * 跳转到主菜单
     */
    private void navigateToMainMenu() {
        Intent intent = new Intent(LoginActivity.this, MainMenuActivity.class);
        startActivity(intent);
        finish(); // 关闭登录Activity
    }
}
