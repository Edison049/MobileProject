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
// import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.model.User;
import com.innovationai.myapplication.util.Constants;
import com.innovationai.myapplication.util.FirebaseUtil;
import com.innovationai.myapplication.util.TempAuthUtil;
import com.innovationai.myapplication.util.Utils;

/**
 * 注册Activity
 * 处理新用户注册逻辑，创建Firebase账户并在Firestore中存储用户详细信息
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
        registerButton.setText("注册中...");

        // 创建Firebase账户
        createFirebaseAccount(email, password, name, age);
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
            nameInputLayout.setError("请输入姓名");
            isValid = false;
        } else if (!Utils.isValidName(name)) {
            nameInputLayout.setError("姓名至少2个字符");
            isValid = false;
        } else {
            nameInputLayout.setError(null);
        }

        // 验证年龄
        if (TextUtils.isEmpty(ageStr)) {
            ageInputLayout.setError("请输入年龄");
            isValid = false;
        } else {
            try {
                int age = Integer.parseInt(ageStr);
                if (!Utils.isValidAge(age)) {
                    ageInputLayout.setError("年龄必须在13-120之间");
                    isValid = false;
                } else {
                    ageInputLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                ageInputLayout.setError("请输入有效的年龄");
                isValid = false;
            }
        }

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
     * 创建账户（使用临时认证）
     * @param email 邮箱
     * @param password 密码
     * @param name 姓名
     * @param age 年龄
     */
    private void createFirebaseAccount(String email, String password, String name, int age) {
        // 使用临时认证
        if (TempAuthUtil.register(this, name, age, email, password)) {
            Utils.showToast(RegisterActivity.this, "注册成功！欢迎加入电影世界！");
            navigateToMainMenu();
        } else {
            Utils.showToast(RegisterActivity.this, "注册失败");
            resetRegisterButton();
        }
    }

    /*
    private void saveUserDetails(String name, int age, String email) {
        // Firebase相关代码暂时注释
    }
    */

    /**
     * 处理注册错误
     * @param exception 异常信息
     */
    private void handleRegistrationError(Exception exception) {
        String errorMessage = "注册失败";
        
        if (exception != null) {
            errorMessage = exception.getMessage();
        }
        
        Utils.showToast(this, errorMessage);
    }

    /**
     * 重置注册按钮状态
     */
    private void resetRegisterButton() {
        registerButton.setEnabled(true);
        registerButton.setText("创建账户");
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