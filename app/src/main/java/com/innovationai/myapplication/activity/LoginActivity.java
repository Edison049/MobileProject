package com.innovationai.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
// import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
// import com.google.firebase.auth.FirebaseAuthInvalidUserException;
// import com.google.firebase.firestore.DocumentSnapshot;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.model.User;
import com.innovationai.myapplication.util.Constants;
import com.innovationai.myapplication.util.FirebaseUtil;
import com.innovationai.myapplication.util.TempAuthUtil;
import com.innovationai.myapplication.util.Utils;

/**
 * 登录Activity
 * 处理用户登录逻辑，支持Firebase认证
 * 特别支持学校要求的两个测试账户：mary和john
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
        if (FirebaseUtil.isUserLoggedIn()) {
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
        loginButton.setText("登录中...");

        // 特殊处理测试账户 - 学校硬性要求
        if (handleTestAccounts(email, password)) {
            return;
        }

        // 使用Firebase认证登录
        performFirebaseLogin(email, password);
    }

    /**
     * 验证用户输入
     * @param email 邮箱
     * @param password 密码
     * @return true表示验证通过，false表示验证失败
     */
    private boolean validateInput(String email, String password) {
        boolean isValid = true;

        // 验证邮箱
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("请输入邮箱地址");
            isValid = false;
        } else if (!Utils.isValidEmail(email)) {
            emailInputLayout.setError("请输入有效的邮箱地址");
            isValid = false;
        } else {
            emailInputLayout.setError(null);
        }

        // 验证密码
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("请输入密码");
            isValid = false;
        } else if (!Utils.isValidPassword(password)) {
            passwordInputLayout.setError("密码至少6位");
            isValid = false;
        } else {
            passwordInputLayout.setError(null);
        }

        return isValid;
    }

    /**
     * 处理测试账户登录（学校要求的特殊逻辑）
     * @param email 邮箱
     * @param password 密码
     * @return true表示是测试账户并已处理，false表示不是测试账户
     */
    private boolean handleTestAccounts(String email, String password) {
        // 使用临时认证工具
        if (TempAuthUtil.login(this, email, password)) {
            Utils.showToast(this, "登录成功！欢迎 " + 
                (email.contains("mary") ? "Mary" : "John") + "!");
            navigateToMainMenu();
            return true;
        }
        return false;
    }

    /*
    private void createOrGetTestUser(String email, String password, String name, int age) {
        // Firebase相关代码暂时注释
    }
    */

    /*
    private void createUserAccount(String email, String password, String name, int age) {
        // Firebase相关代码暂时注释
    }
    */

    /*
    private void saveUserDetails(String name, int age, String email) {
        // Firebase相关代码暂时注释
    }
    */

    /**
     * 执行登录（使用临时认证）
     * @param email 邮箱
     * @param password 密码
     */
    private void performFirebaseLogin(String email, String password) {
        // 使用临时认证
        if (TempAuthUtil.login(this, email, password)) {
            Utils.showToast(this, "登录成功！");
            navigateToMainMenu();
        } else {
            Utils.showToast(this, "邮箱或密码错误");
            resetLoginButton();
        }
    }

    /*
    private void fetchUserInfo() {
        // Firebase相关代码暂时注释
    }
    */

    /*
    private void createDefaultUserProfile() {
        // Firebase相关代码暂时注释
    }
    */

    /**
     * 处理登录错误
     * @param exception 异常信息
     */
    private void handleLoginError(Exception exception) {
        String errorMessage = "登录失败";
        
        if (exception != null) {
            errorMessage = exception.getMessage();
        }
        
        Utils.showToast(this, errorMessage);
    }

    /**
     * 重置登录按钮状态
     */
    private void resetLoginButton() {
        loginButton.setEnabled(true);
        loginButton.setText("登录");
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