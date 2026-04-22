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
 * 注册Activity
 * 处理新用户注册逻辑
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText nameEditText;
    private TextInputEditText ageEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputLayout nameInputLayout;
    private TextInputLayout ageInputLayout;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private Button registerButton;
    private TextView loginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 初始化视图组件
        initViews();
        
        // 设置点击事件监听器
        setupClickListeners();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        nameEditText = findViewById(R.id.name_edit_text);
        ageEditText = findViewById(R.id.age_edit_text);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        nameInputLayout = findViewById(R.id.name_input_layout);
        ageInputLayout = findViewById(R.id.age_input_layout);
        emailInputLayout = findViewById(R.id.email_input_layout);
        passwordInputLayout = findViewById(R.id.password_input_layout);
        registerButton = findViewById(R.id.register_button);
        loginLink = findViewById(R.id.login_link);
    }

    /**
     * 设置点击事件监听器
     */
    private void setupClickListeners() {
        // 注册按钮点击事件
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegister();
            }
        });

        // 登录链接点击事件
        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到登录页面
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // 关闭当前Activity
            }
        });
    }

    /**
     * 尝试注册新用户
     */
    private void attemptRegister() {
        // 获取输入的数据
        String name = nameEditText.getText().toString().trim();
        String ageStr = ageEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // 验证输入
        if (!validateInput(name, ageStr, email, password)) {
            return;
        }

        int age = Integer.parseInt(ageStr);

        // 显示加载状态
        registerButton.setEnabled(false);
        registerButton.setText("Creating account...");

        AppRepository.getInstance().register(this, name, age, email, password,
                new AppRepository.DataCallback<>() {
                    @Override
                    public void onSuccess(com.innovationai.myapplication.model.User data) {
                        Utils.showToast(RegisterActivity.this, "Account created successfully. Welcome aboard!");
                        navigateToMainMenu();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Utils.showToast(RegisterActivity.this, errorMessage);
                        resetRegisterButton();
                    }
                });
    }

    /**
     * 验证用户输入
     * @param name 姓名
     * @param ageStr 年龄字符串
     * @param email 邮箱
     * @param password 密码
     * @return true表示验证通过，false表示验证失败
     */
    private boolean validateInput(String name, String ageStr, String email, String password) {
        boolean isValid = true;

        // 验证姓名
        if (TextUtils.isEmpty(name)) {
            nameInputLayout.setError("Please enter your name");
            isValid = false;
        } else if (!Utils.isValidName(name)) {
            nameInputLayout.setError("Name must be at least 2 characters");
            isValid = false;
        } else {
            nameInputLayout.setError(null);
        }

        // 验证年龄
        if (TextUtils.isEmpty(ageStr)) {
            ageInputLayout.setError("Please enter your age");
            isValid = false;
        } else {
            try {
                int age = Integer.parseInt(ageStr);
                if (!Utils.isValidAge(age)) {
                    ageInputLayout.setError("Age must be between 13 and 120");
                    isValid = false;
                } else {
                    ageInputLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                ageInputLayout.setError("Please enter a valid age");
                isValid = false;
            }
        }

        // 验证邮箱
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Please enter your email address");
            isValid = false;
        } else if (!Utils.isValidEmail(email)) {
            emailInputLayout.setError("Please enter a valid email address");
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
     * 重置注册按钮状态
     */
    private void resetRegisterButton() {
        registerButton.setEnabled(true);
        registerButton.setText("Create Account");
    }

    /**
     * 跳转到主菜单
     */
    private void navigateToMainMenu() {
        Intent intent = new Intent(RegisterActivity.this, MainMenuActivity.class);
        startActivity(intent);
        finish(); // 关闭注册Activity
    }
}
